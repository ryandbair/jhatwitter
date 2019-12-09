package com.github.ryandbair.jhatwitter

import akka.actor
import akka.actor.typed.scaladsl.ActorContext
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.adapter._
import akka.actor.typed.{ActorRef, Scheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.server.Directives.{complete, get, path}
import akka.http.scaladsl.server.Route
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContextExecutor, Future}

object HttpServer {
  def start(context: ActorContext[_], tally: ActorRef[Tally.Protocol]): Future[Http.ServerBinding] = {
    implicit val scheduler: Scheduler = context.system.scheduler
    implicit val ec: ExecutionContextExecutor = context.system.executionContext
    implicit val classicSystem: actor.ActorSystem = context.system.toClassic
    implicit val timeout: Timeout = 1.second

    val route =
      path("stats") {
        get {
          complete {
            tally.ask[Tally.State](Tally.ReadResults).map { state =>
              import state._

              // TODO: other things to print
              HttpEntity(
                s"""
                   |Tweet stats:
                   |Total: $total
                   |${Templates.popularItem("Emoji", emojis)}
                   |${Templates.popularItem("Hashtag", hashtags)}
                   |${Templates.popularItem("Domains", domains)}
                   |""".stripMargin

              )
            }
          }
        }
      }

    val http = Http(context.system.toClassic)
    http.bindAndHandle(Route.handlerFlow(route), "localhost", 8080)
  }
}

object Templates {
  def popularItem[T](name: String, items: Map[T, Int]): String = {
    val text = items.toSeq.sortBy(_._2).headOption.fold(s"None seen yet") { case (item, count) =>
      s"$item : $count\n"
    }
    s"Most popular $name: $text"
  }
}