package com.github.ryandbair.jhatwitter

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import akka.util.ByteString

object Stream {
  def source(entitySource: Source[ByteString, Any])(implicit mat: Materializer): Source[Tweet, NotUsed] = ???
}
