/*
 * Copyright 2016 Pere Villega
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.perevillega.trades.support

import cats.data.Xor
import com.ning.http.client.ws.{WebSocket, WebSocketTextListener, WebSocketUpgradeHandler}
import com.perevillega.trades._
import gigahorse._
import io.circe.Decoder
import io.circe.parser._
import monix.eval.Task
import cats.syntax.xor._
import com.ning.http.client.AsyncHttpClient
import com.perevillega.trades.support.GigahorseHttpClientManager.OKCheck
import io.circe.generic.semiauto._

import scala.collection.mutable

// Trait so we can swap client managers with other libraries. Fancy http4s? Just create it!
trait HttpClientManager[Req, WsReq, Resp, WsResp] {
  def execute(req: Req): Task[Resp]

  def executeWs(req: WsReq, onMessage: (String) => Unit, onError: (Throwable) => Unit): Task[WsResp]

  def close(): Unit
}

// Our main client manager, using GigaHorse (http://eed3si9n.com/gigahorse/)
class GigahorseHttpClientManager(config: Config = Gigahorse.config) extends HttpClientManager[Request, String, Result[String], Result[WebSocket]] {

  // The client is pooled by default, it can manage all the connections required (including web services)
  private val client = Gigahorse.http(config)
  // We keep a list of websockets created so we can clean connections at the end. Otherwise the jvm process may not terminate
  private val wsList = mutable.ListBuffer[WebSocket]()

  // Standard request execution using the pooled client
  def execute(req: Request): Task[Result[String]] =
    Task.fromFuture(client.process(req)).map { rsp =>
      // The server we are hitting has a curious notion of error. It may return code 200 but {ok: false} in body, that means an error has happened...
      decode[OKCheck](rsp.body).fold(err => rsp.body.left, check => if(check.ok) rsp.body.right else rsp.body.left)
    }

  // For websockets, we create a custom handler with the methods provided to manage messages received.
  // We don't need to send messages for Stockfighter, so no need to allow that in here, although we return the WebSocket anyway
  def executeWs(url: String, onMessage: (String) => Unit, onError: (Throwable) => Unit): Task[Result[WebSocket]] = Task {
    Xor.catchNonFatal {
      // can't convert Java future to Scala future easily, but we wrap this in Task so we just pull response here
      val ws = client
        .underlying[AsyncHttpClient]
        .prepareGet(url)
        .execute(handler(onMessage, onError))
        .get()

      wsList += ws
      ws
    }.leftMap(_.getMessage)
  }

  def handler(onMessage: (String) => Unit, onError: (Throwable) => Unit) =
    new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketTextListener() {
      override def onOpen(webSocket: WebSocket): Unit = ()
      override def onClose(webSocket: WebSocket): Unit = ()
      override def onError(t: Throwable): Unit = onError(t)
      override def onMessage(message: String): Unit = onMessage(message)
    }).build()

  // If not called it will leave connections open and the jvm running. Used in helper methods
  def close(): Unit = {
    wsList.foreach(_.close())
    client.close()
  }
}

object GigahorseHttpClientManager {
  // Support class for our http client, required due to how the server notifies of errors, sometimes (Http 200 with ok: false in body)
  final case class OKCheck(ok: Boolean)
  implicit val okCheckDecoder: Decoder[OKCheck] = deriveDecoder[OKCheck]
}