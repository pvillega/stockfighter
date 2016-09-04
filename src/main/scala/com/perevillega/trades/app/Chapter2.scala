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

import com.perevillega.trades.service.LogApi._
import com.perevillega.trades.service.TradeApi._
import cats.instances.all._
import com.perevillega.trades.model.{Execution, Order, OrderType}
import com.perevillega.trades.model.OrderType._
import com.perevillega.trades.repository.Repository
import freek._
import monix.cats._

import scala.annotation.tailrec

object Chapter2 extends TradingAppHelpers {
  val venue = "BRHEX"
  val account = "RFB7900886"
  val stock = "GIFU"
  val TARGET = 100000
  val targetPrice = 5319

  // purchase them 100,000 shares of IPSO Corp. Beware market noticing
  // Suggestion from instructions:
  // - do a few orders each time, they don't remember old orders
  // - control spread
  // - avoid changing price too much

  val (clientRepo, myRepo) = initialiseRepository(venue, account)

  // we want to buy immediately, as many as we can, without trace in the order book
  def buyShares(amount: Int, price: Int, orderType: OrderType = ImmediateOrCancel) = for {
    _ <- info(s"Buying $amount shares at $price price").freeko[API, O]
    b <- buy(venue, account, Order(stock, price, amount, orderType)).freeko[API, O]
  } yield b

  // we want to add a sell order to try to lower prices?
  def sellShares(amount: Int, price: Int) = for {
    _ <- info(s"Selling $amount shares at $price price").freeko[API, O]
    b <- sell(venue, account, Order(stock, price, amount, Limit)).freeko[API, O]
  } yield b


  def cancelOrders(id: Int) = for {
    _ <- info(s"Cancel sell order $id").freeko[API, O]
    b <- cancel(venue, stock, id).freeko[API, O]
  } yield b

  // step for each iteration
  def doIteration: Int = {
    // check quote from websockets
    myRepo.getQuote(venue, stock).map { quote =>
      // can we just buy some at the desired price?
      if(quote.salePrice <= targetPrice) {
        val bought = run(buyShares(quote.saleQty, quote.salePrice))
        bought.fold(_ => 0, os => os.head.fills.map(_.qty).sum)
      } else if(quote.salePrice < targetPrice + 100){
        // work with 5% of sale market
        val qty: Int = (quote.saleQty*.05).toInt
        // let's try to manipulate market. We want to force price down, so we sell cheaper than the current spread but more than our price
        val sold = run(sellShares(qty, quote.salePrice - 5))
        // to avoid triggering any controls from market, we have to buy some, hopefully outset by
        val bought = run(buyShares(qty, quote.buyPrice + 5, Limit))

        val fillsSell = sold.fold(_ => 0, os => os.head.fills.map(_.qty).sum)
        val fillsBuy = bought.fold(_ => 0, os => os.head.fills.map(_.qty).sum)
        fillsBuy - fillsSell
      } else {
        // Do nothing, conditions not right
        println(s">> Decided to wait ${quote.salePrice}")
        0
      }

    }.getOrElse(0)
  }

  var ownedStock = 0
  // Track executions of our sell orders
  def trackExecutions(e: Execution): Unit = {
    if(e.venue == venue && e.account == account && e.symbol == stock) {
      if(e.order.direction == "sell") {
        println(s"!! Sell order filled! $e")
        ownedStock = ownedStock - e.filled
      } else if(e.order.direction == "buy") {
        println(s"!! Buy order filled! $e")
        ownedStock = ownedStock + e.filled
      }
    }
  }
  myRepo.addHookForExecution(trackExecutions)

  //TODO: on exectutions for my sales, consider additioanl fills obtained! changes number of shares we own

  @tailrec
  def executeStrategy(): Unit = {
    val obtained = doIteration
    if(ownedStock + obtained < TARGET) {
      // please wait to avoid waking up market
      Thread.sleep(5000)
      executeStrategy()
    } else {
      println(s"Our calculations say we have ${ownedStock + obtained} shares, which is more than $TARGET. Exiting.")
    }
  }
  executeStrategy()

  println("End of program reached!")
  clientRepo.close()
}
