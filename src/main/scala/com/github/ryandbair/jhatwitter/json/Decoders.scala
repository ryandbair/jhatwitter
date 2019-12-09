package com.github.ryandbair.jhatwitter.json

import java.net.URI

import io.circe.Decoder

object Decoders {
  implicit val URIDecoder: Decoder[URI] = Decoder.decodeString.map(URI.create)
}
