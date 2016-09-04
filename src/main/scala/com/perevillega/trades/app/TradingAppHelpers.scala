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

package com.perevillega.trades.app

import cats.free._
import cats.instances.all._
import com.ning.http.client.ws.WebSocket
import com.perevillega.trades.model.json.CirceImplicits._
import com.perevillega.trades._
import com.perevillega.trades.config.Config
import com.perevillega.trades.model.Execution
import com.perevillega.trades.model.internal.InternalApiModel.TickerTape
import com.perevillega.trades.repository.Repository
import com.perevillega.trades.service.TradeApi._
import com.perevillega.trades.service.LogApi._
import com.perevillega.trades.service._
import com.perevillega.trades.support.{GigahorseHttpClientManager, HttpClientManager}
import freek._
import gigahorse.Request
import io.circe.parser._
import monix.cats._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.{higherKinds, implicitConversions, postfixOps}

trait TradingAppHelpers extends App {
  val testAccount = Config.mockVenue
  val testVenue = Config.mockVenue
  val testStock = Config.mockStockSymbol

  private val repository = Repository()

  // Below is some more Freek magic to unify DSL of several languages
  // Note that we need the Onion type (O) to handle the result return types from our programs,
  // this way we can chain values inside the for-comprehension
  // Even with the onion a `left` in the for-comprehension will short-circuit correctly and exit the program, so all is good
  type API = LogApi.PRG :||: TradeApi.PRG
  val API = DSL.Make[API]
  type O = Result[?] :&: List :&: Bulb

  // More Freek magic, unifying interpreters from different languages
  val accumulatingInterpreter = LogApiAccumulatingInterpreter :&: TradeApiAccumulatingInterpreter

  // We create the task interpreter on demand to have better control over http client and avoid dangling sockets
  def taskInterpreter(httpClient: HttpClientManager[Request, String, Result[String], Result[WebSocket]]) = LogApiTaskInterpreter :&: new TradeApiTaskInterpreter(Config.apiKey, Config.apiEndpoint, Config.wsEndpoint, httpClient)

  // Helper method. Receives an Onion (a program you defined) and executes it with the task interpreter
  // The method makes sure all sockets are closed after execution finishes, to avoid dangling connections to the server
  def run[A](program: OnionT[Free, API.Cop, O, A], waitTime: Duration = 10 seconds) = {
    val client = new GigahorseHttpClientManager()
    try {
      val interpreter = taskInterpreter(client)
      val taskResult = program.value.interpret(interpreter)
      Await.result(taskResult.runAsync, waitTime)
    } finally {
      client.close()
    }
  }

  // Helper method. Receives an Onion (a program you defined) and executes it with the task interpreter
  // It returns a tuple (HttpClient, Result) and DOES NOT close the http client sockets on return
  // The main use is when you want to call some program that uses websockets and don't want to close
  // the connexion when it returns.
  // Yes, it's a dirty hack, but saves some boilerplate
  def runAndGetHttpClient[A](program: OnionT[Free, API.Cop, O, A], waitTime: Duration = 10 seconds) = {
    val client = new GigahorseHttpClientManager()
    val interpreter = taskInterpreter(client)
    val taskResult = program.value.interpret(interpreter)
    (client, Await.result(taskResult.runAsync, waitTime))
  }

  // Helper method. Runs the program provided with the accumulating interpreter, which will return a list of all steps
  // in the program (happy path).
  def inspectAsString[A](program: OnionT[Free, API.Cop, O, A]) = {
    val stateResult = program.value.interpret(accumulatingInterpreter)
    stateResult.runS(Nil).value
  }

  def initialiseRepository(venue: String, account: String): (HttpClientManager[Request, String, Result[String], Result[WebSocket]], Repository) = {
    repository.clear()
    val pair = runAndGetHttpClient(trackOrderBooksWithWS(venue, account))
    (pair._1, repository)
  }

  private def trackOrderBooksWithWS(venue: String, account: String) = for {
    _ <- setOrderBook(venue)
    _ <- info(s"Starting Websockets to populate Repository for venue $venue").freeko[API, O]
    _ <- tickerTape(venue, account, onQuoteMessage).freeko[API, O]
    _ <- info(s"Ticker Tape Websockets running").freeko[API, O]
    _ <- executions(venue, account, onExecutionMessage).freeko[API, O]
    _ <- info(s"Execution Websockets running").freeko[API, O]
  } yield ()

  private def setOrderBook(venue: String) = for {
    _ <- info(s"Obtaining all existing stocks in venue $venue").freeko[API, O]
    stock <- venueStocks(venue).freeko[API, O]
    _ <- info(s"Storing Order Book for $stock in venue $venue").freeko[API, O]
    ob <- stockOrderBook(venue, stock).freeko[API, O]
  } yield repository.addOrderBook(ob)

  private def onQuoteMessage(s: String): Unit =
    decode[TickerTape](s).leftMap(throw _).foreach(tt => repository.addQuote(tt.quote))

  private def onExecutionMessage(s: String) =
    decode[Execution](s).leftMap(throw _).foreach(ex => repository.addExecution(ex))

}
