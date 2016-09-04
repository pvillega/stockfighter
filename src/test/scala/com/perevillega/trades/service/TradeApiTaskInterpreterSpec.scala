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

package com.perevillega.trades.service

import org.scalatest.{FlatSpec, Matchers}
import TradeApi._
import cats.data.Xor
import com.ning.http.client.ws.WebSocket
import com.perevillega.trades._
import com.perevillega.trades.model.Order
import com.perevillega.trades.support.HttpClientManager
import gigahorse.Request
import monix.eval.Task
import monix.cats._
import monix.execution.Scheduler.Implicits.global
import freek._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

// We focus on testing the interpreter, as that contains the logic.
// Programs are not relevant for testing, unless we want to ensure a certain command is executed
class TradeApiTaskInterpreterSpec  extends FlatSpec with Matchers {

  def verifyInterpreter(assertions: (Request => Unit)*): TradeApiTaskInterpreter = {
    val httpClient = new HttpClientManager[Request, String, Result[String], Result[WebSocket]] {
      override def execute(req: Request): Task[Result[String]] = {
        assertions.foreach(_(req))
        Task.now(Xor.Left("To be implemented"))
      }

      override def executeWs(req: String, onMessage: (String) => Unit, onError: (Throwable) => Unit): Task[Result[WebSocket]] = Task.apply(Xor.left("To be implemented"))

      override def close(): Unit = ()
    }

    new TradeApiTaskInterpreter("apiKey", "apiEndpoint", "wsEndpoint", httpClient)
  }

  "Interpreter" should "add Stockfighter authentication headers on requests" in {
    val program = for {
      _ <- buy("", "", Order("", 1, 1)).freek[PRG]
    } yield ()

    val interpreter = verifyInterpreter(req => {req.headers.get("X-Starfighter-Authorization") should be(Some(List("apiKey")))})
    Await.result(program.interpret(interpreter).runAsync, 10 seconds)
  }

}
