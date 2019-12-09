package com.github.ryandbair.jhatwitter

import akka.NotUsed
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorSystem, Behavior, Terminated}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, Sink}
import akka.stream.typed.scaladsl.ActorSink
import akka.stream.{ClosedShape, Materializer}
import com.github.ryandbair.jhatwitter.OAuth.OAuthSupport
import com.typesafe.config.ConfigFactory
import com.vdurmont.emoji.{Emoji, EmojiManager, EmojiParser}
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._

import scala.concurrent.ExecutionContextExecutor
import scala.jdk.CollectionConverters.IterableHasAsScala
import scala.util.Success

object TwitterStreamApp {
  val tweetStreamUrl = "https://stream.twitter.com/1.1/statuses/sample.json"

  def main(args: Array[String]): Unit = {
    val config = ConfigFactory.load()
    val oauthParams = config.as[OAuth.Params]("authentication")

    ActorSystem(appBehavior(oauthParams), "TwitterStream")
  }

  def appBehavior(oauthParams: OAuth.Params): Behavior[Unit] =
    Behaviors.setup { context =>
      val request = HttpRequest(uri = tweetStreamUrl).withOAuthHeader(oauthParams)
      implicit val ec: ExecutionContextExecutor = context.system.executionContext
      implicit val materializer: Materializer = Materializer(context)
      val tally = context.spawn(Tally(), "tally")
      context.watch(tally)

      HttpServer.start(context, tally) // TODO: watch this

      Http(context.system.toClassic).singleRequest(request).flatMap {
        import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._
        import io.circe.generic.auto._

        Unmarshal(_).to[SourceOf[Tweet]]
      }.map { tweetSource =>
        GraphDSL.create(tweetSource) { implicit builder => source =>
          import GraphDSL.Implicits._
          val bcast = builder.add(Broadcast[Tweet](7))
          val merge = builder.add(Merge[Tally.Protocol](7))
          val sink: Sink[Tally.Protocol, NotUsed] = ActorSink.actorRef(tally, Tally.Complete, Tally.Failure)

          val count = Flow[Tweet].scan(0) { case (count, _) =>
            count + 1
          }.map(Tally.Total)

          def statFlow[T, M](mkMessage: Map[T, Int] => M)(extract: Tweet => Iterable[T]): Flow[Tweet, M, NotUsed] =
            Flow[Tweet].scan(Map.empty[T, Int]) { case (state, tweet) =>
              val features = extract(tweet)
              val counts = features.groupMapReduce(identity)(_ => 1)(_ + _)
              counts.foldLeft(state) { case (s, (feature, newCount)) =>
                val updatedCount = s.get(feature).fold(newCount)(_ + newCount)
                s + (feature -> updatedCount)
              }
            }.map(mkMessage)

          val emojiStat = statFlow[Emoji, Tally.Emojis](Tally.Emojis) { tweet =>
            EmojiParser.extractEmojis(tweet.text).asScala.map(EmojiManager.getByUnicode)
          }

          val domainStat = statFlow[String, Tally.Domains](Tally.Domains) { tweet =>
            tweet.entities.urls.map { urlData =>
              urlData.expandedUrl.getHost
            }
          }

          val hashtagStat = statFlow[HashTag, Tally.HashTags](Tally.HashTags)(_.entities.hashtags)

          def countFlow(category: Tally.StatCategory, condition: Tweet => Boolean): Flow[Tweet, Tally.Stats, NotUsed] =
            Flow[Tweet].scan(Tally.Stats(category)) {
              case (state, tweet) =>
                state.count(condition(tweet))
            }

          val hasURL = countFlow(Tally.StatCategory.Url, _.entities.urls.nonEmpty)

          val hasPhotoURL = countFlow(Tally.StatCategory.PhotoUrl, _.entities.media.nonEmpty)

          val hasEmoji = countFlow(Tally.StatCategory.Emoji, tweet => !EmojiParser.extractEmojis(tweet.text).isEmpty)

          source ~> bcast ~> count       ~> merge
                    bcast ~> emojiStat   ~> merge
                    bcast ~> domainStat  ~> merge
                    bcast ~> hashtagStat ~> merge
                    bcast ~> hasURL      ~> merge
                    bcast ~> hasPhotoURL ~> merge
                    bcast ~> hasEmoji    ~> merge ~> sink
          ClosedShape
        }
      }

      Behaviors.receiveSignal {
        case (_, Terminated(_)) =>
          Behaviors.stopped
      }
    }
}
