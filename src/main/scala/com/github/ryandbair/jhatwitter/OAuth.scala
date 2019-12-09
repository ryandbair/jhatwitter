package com.github.ryandbair.jhatwitter

import akka.http.scaladsl.model.headers.{Authorization, RawHeader}
import akka.http.scaladsl.model.{HttpEntity, HttpRequest}
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService

object OAuth {
  case class Params(consumerKey: String, consumerSecret: String, requestToken: String, requestTokenSecret: String)

  implicit class OAuthSupport(request: HttpRequest) {
    def withOAuthHeader(params: Params): HttpRequest = {
      val bodyParams = request.entity match {
        case e: HttpEntity.Strict => Some(e.data.utf8String)
        case _ => None
      }
      val koauthRequest = KoauthRequest(request.method.value, request.uri.toString(), bodyParams)
      val requestWithInfo = DefaultConsumerService.createOauthenticatedRequest(
        koauthRequest,
        params.consumerKey,
        params.consumerSecret,
        params.requestToken,
        params.requestTokenSecret,
      )
      val oauthHeader = RawHeader(Authorization.name, requestWithInfo.header)
      request.mapHeaders(_ :+ oauthHeader)
    }
  }
}
