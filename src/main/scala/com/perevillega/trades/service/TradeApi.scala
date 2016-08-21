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

import cats.data.{Xor, XorT}
import cats.syntax.xor._
import cats.~>
import com.ning.http.client.providers.netty.NettyAsyncHttpProviderConfig
import com.ning.http.client.providers.netty.ws.NettyWebSocket
import com.ning.http.client.ws.WebSocket
import com.perevillega.trades
import com.perevillega.trades._
import com.perevillega.trades.model._
import com.perevillega.trades.model.internal.InternalApiModel.OrderDirection.{BuyOrder, SellOrder}
import com.perevillega.trades.model.internal.InternalApiModel._
import com.perevillega.trades.model.json.CirceImplicits
import com.perevillega.trades.support.{HttpClientManager, TestInterpreter}
import freek.{:|:, FXNil, Program}
import gigahorse.{Gigahorse, Request}
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import monix.cats._
import monix.eval.Task

// Defines the API for the Trade endpoint of Stockfighter
object TradeApi {
  // The DSL itself. It can be thought about as a mapping of a method call to a case class. For example, we want
  // to turn the method 'buy(venue, account, order) : Result[OrderStatus]' into a Free Monad. We create the below
  // case class `Buy` that takes as parameters the params of the method and sets the return type as the `A` in `DSL`
  //
  // Note that, where possible, we have simplified the return types. So you won't get exactly what the endpoint
  // returns, but a curated version . For example, IsApiUp returns a Boolean, instead of the ok-error json object in the api
  // We have also reworked some stuff. For example, Buy/Sell are specifically different methods (we make the right request under
  // the hood) to make your life easier. And they return the OrderStatus, as Cancel or GetStatus, instead of a different object type.
  // Convenient. This should make using this api more enjoyable
  sealed trait DSL[A]
  case object IsApiUp extends DSL[Result[Boolean]]
  final case class IsVenueUp(venue: String) extends DSL[Result[Boolean]]

  final case class Buy(venue: String, account: String, order: Order) extends DSL[Result[OrderStatus]]
  final case class Sell(venue: String, account: String, order: Order) extends DSL[Result[OrderStatus]]
  final case class Cancel(venue: String, symbol: String, orderId: Int) extends DSL[Result[OrderStatus]]

  final case class GetStatus(venue: String, symbol: String, orderId: Int) extends DSL[Result[OrderStatus]]
  final case class AllMyOrders(venue: String, account: String) extends DSL[Result[List[OrderStatus]]]
  final case class AllMyOrdersForStock(venue: String, account: String, stock: String) extends DSL[Result[List[OrderStatus]]]

  final case class VenueStocks(venue: String) extends DSL[Result[List[String]]]
  final case class StockOrderBook(venue: String, stock: String) extends DSL[Result[OrderBook]]
  final case class StockQuote(venue: String, stock: String) extends DSL[Result[Quote]]

  final case class TickerTapeAll(venue: String, account: String, onMessage: (String) => Unit, onError: (Throwable) => Unit) extends DSL[Result[WebSocket]]
  final case class TickerTapeStock(venue: String, account: String, stock: String, onMessage: (String) => Unit, onError: (Throwable) => Unit) extends DSL[Result[WebSocket]]
  final case class ExecutionsAll(venue: String, account: String, onMessage: (String) => Unit, onError: (Throwable) => Unit) extends DSL[Result[WebSocket]]
  final case class ExecutionsStock(venue: String, account: String, stock: String, onMessage: (String) => Unit, onError: (Throwable) => Unit) extends DSL[Result[WebSocket]]

  // Freek magic to to get a Free Monad from the DSL
  type PRG = DSL :|: FXNil
  val PRG = Program[PRG]

  // Support methods, use them when creating your for-comprehensions
  def isStockfighterUp = IsApiUp
  def isVenueUp(venue: String) = IsVenueUp(venue)

  def buy(venue: String, account: String, order: Order) = Buy(venue, account, order)
  def sell(venue: String, account: String, order: Order) = Sell(venue, account, order)
  def cancel(venue: String, symbol: String, orderId: Int) = Cancel(venue, symbol, orderId)

  def orderStatus(venue: String, symbol: String, orderId: Int) = GetStatus(venue, symbol, orderId)
  def allMyOrders(venue: String, account: String) = AllMyOrders(venue, account)
  def allMyOrdersForStock(venue: String, account: String, stock: String) = AllMyOrdersForStock(venue, account, stock)

  def venueStocks(venue: String) = VenueStocks(venue)
  def stockOrderBook(venue: String, stock: String) = StockOrderBook(venue, stock)
  def stockQuote(venue: String, stock: String) = StockQuote(venue, stock)

  def tickerTape(venue: String, account: String, onMessage: (String) => Unit = s => println(s), onError: (Throwable) => Unit = t => throw t ) = TickerTapeAll(venue, account, onMessage, onError)
  def tickerTapeStock(venue: String, account: String, stock: String, onMessage: (String) => Unit = s => println(s), onError: (Throwable) => Unit = t => throw t) = TickerTapeStock(venue, account, stock, onMessage, onError)
  def executions(venue: String, account: String, onMessage: (String) => Unit = s => println(s), onError: (Throwable) => Unit = t => throw t) = ExecutionsAll(venue, account, onMessage, onError)
  def executionsStock(venue: String, account: String, stock: String, onMessage: (String) => Unit = s => println(s), onError: (Throwable) => Unit = t => throw t) = ExecutionsStock(venue, account, stock, onMessage, onError)
}

// First interpreter, natural transformation from DSL to a State monad. We used a custom
// type ListState to represent a State[List[String], A]. When run you can get a printout of the program
// and see visually what it will do
object TradeApiAccumulatingInterpreter extends (TradeApi.DSL ~> ListState) {
  import TradeApi._

  // Some default values to avoid repeating the same code over and over again in the implementation
  private val mockOrderStatus = OrderStatus(0, "", "", "", "", 0, 0, "", "", Nil, 0, open = false).right[ErrorMessage]
  private val mockOrderBook = OrderBook("", "", None, None, "").right[ErrorMessage]
  private val mockQuote = Quote("", "", None, None, 0, 0, 0, 0, 0, 0, "", "").right[ErrorMessage]
  private val mockWebsocket = new NettyWebSocket(null, new NettyAsyncHttpProviderConfig()).right[ErrorMessage]

  //BEWARE: if you set an Xor.Left in here as return type in the State Monad, you'll short circuit the for comprehensions and won't print the programs
  def apply[A](a: TradeApi.DSL[A]) = a match {
    case _: IsApiUp.type => addToState (s"$a", false.right[ErrorMessage])
    case _: IsVenueUp => addToState (s"$a", false.right[ErrorMessage])
    case _: Buy => addToState (s"$a", mockOrderStatus)
    case _: Sell => addToState (s"$a", mockOrderStatus)
    case _: Cancel => addToState (s"$a", mockOrderStatus)
    case _: GetStatus => addToState (s"$a", mockOrderStatus)
    case _: AllMyOrders => addToState (s"$a", Nil.right[ErrorMessage])
    case _: AllMyOrdersForStock => addToState (s"$a", Nil.right[ErrorMessage])
    case _: VenueStocks => addToState (s"$a", Nil.right[ErrorMessage])
    case _: StockOrderBook => addToState (s"$a", mockOrderBook)
    case _: StockQuote => addToState (s"$a", mockQuote)
    case _: TickerTapeAll => addToState (s"$a", mockWebsocket)
    case _: TickerTapeStock => addToState (s"$a", mockWebsocket)
    case _: ExecutionsAll => addToState (s"$a", mockWebsocket)
    case _: ExecutionsStock => addToState (s"$a", mockWebsocket)
  }
}

//TODO: phase 2, split this interpreter into an interpreter of http and log? and trades becomes a for comprehension of http calls and logs
// Second interpreter, natural transformation from DSL to Task. This is the 'executing' interpreter that will, as a result, do requests to
// Stockfighter server.
//
// Note it is a class, not an object as most other interpreters. This is so we can pass configuration as well as the httpClient to be used.
// The client thing is important: there's no easy way to tell the interpreter when to close the connections, as it calls 'apply'
// over and over again. We could use a new client each time but it's expensive, a waste. With the client given as dependency we can control
// its lifecycle externally.
// Or we could also merge the client and this into a single object. But it doesn't look nice :)
class TradeApiTaskInterpreter(apiKey: String, apiEndpoint: String, wsEndpoint: String,
                              httpClient: HttpClientManager[Request, String, Result[String], Result[WebSocket]]) extends (TradeApi.DSL ~> Task) {
  import CirceImplicits._
  import TradeApi._

  def apply[A](a: TradeApi.DSL[A]): Task[A] =
    a match {
      case _: IsApiUp.type => run[Ok](heartbeatRequest).map(_ => true).value
      case IsVenueUp(venue) => run[Ok](venueUpRequest(venue)).map(_ => true).value
      case Buy(venue, account, order) =>  run[OrderStatus](newOrderRequest(venue, account, order, BuyOrder)).value
      case Sell(venue, account, order) => run[OrderStatus](newOrderRequest(venue, account, order, SellOrder)).value
      case Cancel(venue, symbol, id) => run[OrderStatus](cancelRequest(venue, symbol, id)).value
      case GetStatus(venue, symbol, id) => run[OrderStatus](orderStatusRequest(venue, symbol, id)).value
      case AllMyOrders(venue, account) => run[AllOrdersStatus](allOrdersRequest(venue, account)).map(_.orders).value
      case AllMyOrdersForStock(venue, account, stock) => run[AllOrdersStatus](allOrdersStockRequest(venue, account, stock)).map(_.orders).value
      case VenueStocks(venue) => run[StocksOnVenueResponse](venueStocksRequest(venue)).map(_.symbols.map(_.symbol)).value
      case StockOrderBook(venue, stock) => run[OrderBook](orderBookRequest(venue, stock)).value
      case StockQuote(venue, stock) => run[Quote](quoteRequest(venue, stock)).value

      case TickerTapeAll(venue, account, onMessage, onError) => ws(wsTickerTapeEndpoint(venue, account), onMessage, onError)
      case TickerTapeStock(venue, account, stock, onMessage, onError) => ws(wsTickerTapeStockEndpoint(venue, account, stock), onMessage, onError)
      case ExecutionsAll(venue, account, onMessage, onError) => ws(wsExecutionsEndpoint(venue, account), onMessage, onError)
      case ExecutionsStock(venue, account, stock, onMessage, onError) => ws(wsExecutionsStockEndpoint(venue, account, stock), onMessage, onError)
    }

  private def run[A: Decoder](req: Request): ResultT[Task, A] =
    XorT[Task, ErrorMessage, String](httpClient.execute(req.addHeaders(headers)))
      .leftMap { err =>
        decode[TradeError](err).fold(_.getMessage, _.error)
      }.subflatMap { ok =>
        decode[A](ok).leftMap(_.getMessage)
      }

  private def ws(url: String, onMessage: (String) => Unit , onError: (Throwable) => Unit): Task[trades.Result[WebSocket]] =
    httpClient.executeWs(url, onMessage, onError)

  // headers for authentication
  private val headers = Map("X-Starfighter-Authorization" -> List(apiKey), "Content-Type" -> List("application/json"))

  // helper methods that generate the proper Request/Url per each endpoint, based on parameters
  private val heartbeatRequest = Gigahorse.url(apiEndpoint + "/heartbeat").get
  private def venueUpRequest(venue: String) = Gigahorse.url(apiEndpoint + s"/venues/$venue/heartbeat").get

  private def venueStocksRequest(venue: String) = Gigahorse.url(apiEndpoint + s"/venues/$venue/stocks").get
  private def orderBookRequest(venue: String, symbol: String) = Gigahorse.url(apiEndpoint + s"/venues/$venue/stocks/$symbol").get
  private def quoteRequest(venue: String, symbol: String) = Gigahorse.url(apiEndpoint + s"/venues/$venue/stocks/$symbol/quote").get
  private def orderStatusRequest(venue: String, symbol: String, orderId: Int) = Gigahorse.url(apiEndpoint + s"/venues/$venue/stocks/$symbol/orders/$orderId").get
  private def allOrdersRequest(venue: String, account: String) = Gigahorse.url(apiEndpoint + s"/venues/$venue/accounts/$account/orders").get
  private def allOrdersStockRequest(venue: String, account: String, symbol: String) = Gigahorse.url(apiEndpoint + s"/venues/$venue/accounts/$account/stocks/$symbol/orders").get

  private def cancelRequest(venue: String, symbol: String, orderId: Int) = Gigahorse.url(apiEndpoint + s"/venues/$venue/stocks/$symbol/orders/$orderId").delete

  private def newOrderRequest(venue: String, account: String, order: Order, orderDirection: OrderDirection) =
    Gigahorse
      .url(apiEndpoint + s"/venues/$venue/stocks/${order.stock}/orders")
      .post(NewOrder(venue, account, order, orderDirection).asJson.noSpaces)

  private def wsTickerTapeEndpoint(venue: String, account: String) = wsEndpoint + s"/$account/venues/$venue/tickertape"
  private def wsTickerTapeStockEndpoint(venue: String, account: String, symbol: String) = wsEndpoint + s"/$account/venues/$venue/tickertape/stocks/$symbol"
  private def wsExecutionsEndpoint(venue: String, account: String) = wsEndpoint + s"/$account/venues/$venue/executions"
  private def wsExecutionsStockEndpoint(venue: String, account: String, symbol: String) = wsEndpoint + s"/$account/venues/$venue/executions/stocks/$symbol"
}

// Interpreter to be used for test purposes. See TestInterpreter for details
object TradeApiTestInterpreter  extends (TradeApi.DSL ~> ListState) with TestInterpreter[TradeApi.DSL] {
  def apply[A](a: TradeApi.DSL[A]) = addToState(s"$a", getResult(a, Xor.Left("Missing value")).asInstanceOf[A])
}