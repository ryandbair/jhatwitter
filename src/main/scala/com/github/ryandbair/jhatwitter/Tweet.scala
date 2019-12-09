package com.github.ryandbair.jhatwitter

import java.net.URI

case class Tweet(text: String, entities: Entities)

case class Entities(hashtags: Seq[HashTag], urls: Seq[TweetUrlData], media: Seq[Media])

case class HashTag(text: String) extends AnyVal

case class TweetUrlData(expandedUrl: URI)

case class Media(mediaUrl: String)
