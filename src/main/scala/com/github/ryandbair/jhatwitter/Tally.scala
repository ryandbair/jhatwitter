package com.github.ryandbair.jhatwitter

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}
import com.vdurmont.emoji.Emoji

object Tally {
  // TODO: rate counter
  sealed trait Protocol
  case class Total(value: Int) extends Protocol
  case class Emojis(emojis: Map[Emoji, Int]) extends Protocol
  case class HashTags(hashtags: Map[HashTag, Int]) extends Protocol
  case class Domains(domains: Map[String, Int]) extends Protocol
  case class Stats(category: StatCategory, countWith: Int = 0, countWithOut: Int = 0) extends Protocol {
    def count(condition: => Boolean): Stats = {
      if (condition)
        this.copy(countWith = this.countWith + 1)
      else
        this.copy(countWith = this.countWithOut + 1)
    }
  }
  case object Complete extends Protocol
  case class Failure(f: Throwable) extends Protocol
  case class ReadResults(sender: ActorRef[State]) extends Protocol

  sealed trait StatCategory
  object StatCategory {
    final val Emoji = new StatCategory {}
    final val Url = new StatCategory {}
    final val PhotoUrl = new StatCategory {}
  }

  case class State(total: Int = 0, emojis: Map[Emoji, Int] = Map.empty,
                   hashtags: Map[HashTag, Int] = Map.empty, domains: Map[String, Int] = Map.empty,
                   stats: Map[StatCategory, Stats] = Map.empty)

  def apply(): Behavior[Protocol] = tally(State())

  private def tally(state: State): Behavior[Protocol] =
    Behaviors.receiveMessage {
      case Total(value) =>
        tally(state.copy(total = value))
      case Emojis(value) =>
        tally(state.copy(emojis = value))
      case s: Stats =>
        tally(state.copy(stats = state.stats + (s.category -> s)))
      case HashTags(value) =>
        tally(state.copy(hashtags = value))
      case Domains(value) =>
        tally(state.copy(domains = value))
      case ReadResults(sender) =>
        sender ! state
        Behaviors.same
      case Complete =>
        Behaviors.stopped
    }
}
