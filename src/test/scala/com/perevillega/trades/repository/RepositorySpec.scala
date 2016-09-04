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

package com.perevillega.trades.repository

import com.perevillega.trades.model._
import org.scalatest.{FlatSpec, Matchers}

class RepositorySpec extends FlatSpec with Matchers {

  "A repository" should "be created only once, calls to apply return same object always" in fixture { rep =>
    val rep = Repository()
    val rep2 = Repository()
    rep eq rep2 should be(true)
  }

  it should "be created without any venue in it" in fixture { rep =>
    val rep = Repository()
    rep.getVenues.isEmpty should be(true)
  }

  it should "be created with no quotes" in fixture { rep =>
    val rep = Repository()
    rep.getAllQuotes.isEmpty should be(true)
  }

  it should "be able to be add quotes to its corresponding venue" in fixture { rep =>
    val quote = Quote("v1", "s1", Some(1), Some(1), 10, 10, 1, 1, 2, 3, "", "")
    rep.addQuote(quote)
    rep.getQuote("v1", "s1") should be(Some(quote))
  }

  it should "be able to be add quotes for same stock on different venues without conflict" in fixture { rep =>
    val quote1 = Quote("v1", "s1", Some(1), Some(1), 10, 10, 1, 1, 2, 3, "", "")
    val quote2 = Quote("v2", "s1", Some(2), Some(2), 20, 20, 2, 2, 4, 6, "", "")
    rep.addQuote(quote1)
    rep.addQuote(quote2)
    rep.getQuote("v1", "s1") should be(Some(quote1))
    rep.getQuote("v2", "s1") should be(Some(quote2))
  }

  it should "keep only the latest quote for a stock in a venue" in fixture { rep =>
    val quote1 = Quote("v1", "s1", Some(1), Some(1), 10, 10, 1, 1, 2, 3, "", "")
    val quote2 = Quote("v1", "s1", Some(2), Some(2), 20, 20, 2, 2, 4, 6, "", "")
    rep.addQuote(quote1)
    rep.addQuote(quote2)
    rep.getQuote("v1", "s1") should be(Some(quote2))
  }

  it should "retrieve all quotes in a venue upon request" in fixture { rep =>
    val quote1 = Quote("v1", "s1", Some(1), Some(1), 10, 10, 1, 1, 2, 3, "", "")
    val quote2 = Quote("v1", "s2", Some(2), Some(2), 20, 20, 2, 2, 4, 6, "", "")
    rep.addQuote(quote1)
    rep.addQuote(quote2)
    rep.getAllQuotes("v1").toSet should be(Set(quote1, quote2))
  }

  it should "retrieve all quotes in all venues upon request" in fixture { rep =>
    val quote1 = Quote("v1", "s1", Some(1), Some(1), 10, 10, 1, 1, 2, 3, "", "")
    val quote2 = Quote("v2", "s2", Some(2), Some(2), 20, 20, 2, 2, 4, 6, "", "")
    rep.addQuote(quote1)
    rep.addQuote(quote2)
    rep.getAllQuotes.toSet should be(Set(quote1, quote2))
  }

  it should "be able to use an order book to bootstrap the internal order book for a stock" in fixture { rep =>
    val orderBook = OrderBook("v1", "s1", None, None, "")
    rep.addOrderBook(orderBook)
    rep.getOrderBook("v1", "s1") should be(Some(orderBook))
  }

  it should "be able to have order book for several different stocks in same venue" in fixture { rep =>
    val orderBook1 = OrderBook("v1", "s1", None, None, "1")
    val orderBook2 = OrderBook("v1", "s2", None, None, "2")
    rep.addOrderBook(orderBook1)
    rep.addOrderBook(orderBook2)
    rep.getOrderBook("v1", "s1") should be(Some(orderBook1))
    rep.getOrderBook("v1", "s2") should be(Some(orderBook2))
  }

  it should "be able to have order book for same stock in different venues" in fixture { rep =>
    val orderBook1 = OrderBook("v1", "s1", None, None, "1")
    val orderBook2 = OrderBook("v2", "s1", None, None, "2")
    rep.addOrderBook(orderBook1)
    rep.addOrderBook(orderBook2)
    rep.getOrderBook("v1", "s1") should be(Some(orderBook1))
    rep.getOrderBook("v2", "s1") should be(Some(orderBook2))
  }

  it should "correctly update an existing order book for a stock when we receive a buy execution" in fixture { rep =>
    val bids = List(Request(15, 1000, isBuy = true), Request(10, 2000, isBuy = true))
    val asks = List(Request(20, 200, isBuy = false), Request(25, 300, isBuy = false), Request(45, 10, isBuy = false), Request(555, 20, isBuy = false))
    val orderBook1 = OrderBook("v1", "s1", Some(bids), Some(asks), "1")

    val fills = List(Fill(20, 200, ""), Fill(25, 200, ""))
    val orderStatus = OrderStatus(1, "acc", "v1", "s1", "buy", 100, 100, 10, "limit", "", fills, 100, open = false)
    val execution = Execution("acc", "v1", "s1", orderStatus, 1, 1, 100, 10, "999", standingComplete = false, incomingComplete = false)

    val expected = OrderBook("v1", "s1", Some(bids), Some(List(Request(25, 100, isBuy = false), Request(45, 10, isBuy = false), Request(555, 20, isBuy = false))), "999")

    rep.addOrderBook(orderBook1)
    rep.addExecution(execution)
    rep.getOrderBook("v1", "s1") should be(Some(expected))
  }

  it should "correctly update an existing order book for a stock when we receive a sell execution" in fixture { rep =>
    val bids = List(Request(15, 1000, isBuy = true), Request(10, 2000, isBuy = true), Request(5, 30, isBuy = true), Request(1, 9999, isBuy = true))
    val asks = List(Request(20, 200, isBuy = false), Request(25, 300, isBuy = false), Request(45, 10, isBuy = false), Request(555, 20, isBuy = false))
    val orderBook1 = OrderBook("v1", "s1", Some(bids), Some(asks), "1")

    val fills = List(Fill(15, 200, ""), Fill(15, 800, ""),  Fill(10, 200, ""))
    val orderStatus = OrderStatus(1, "acc", "v1", "s1", "sell", 100, 100, 10, "limit", "", fills, 100, open = false)
    val execution = Execution("acc", "v1", "s1", orderStatus, 1, 1, 100, 10, "999", standingComplete = false, incomingComplete = false)

    val expected = OrderBook("v1", "s1", Some(List(Request(10, 1800, isBuy = true), Request(5, 30, isBuy = true), Request(1, 9999, isBuy = true))), Some(asks), "999")

    rep.addOrderBook(orderBook1)
    rep.addExecution(execution)
    rep.getOrderBook("v1", "s1") should be(Some(expected))
  }

  it should "correctly update an existing order book for a stock when we receive a sell execution and fill is bigger than single request" in fixture { rep =>
    val bids = List(Request(15, 1000, isBuy = true), Request(15, 1000, isBuy = true), Request(15, 1000, isBuy = true), Request(10, 2000, isBuy = true), Request(5, 30, isBuy = true), Request(1, 9999, isBuy = true))
    val asks = List(Request(20, 200, isBuy = false), Request(25, 300, isBuy = false), Request(45, 10, isBuy = false), Request(555, 20, isBuy = false))
    val orderBook1 = OrderBook("v1", "s1", Some(bids), Some(asks), "1")

    val fills = List(Fill(15, 3000, ""), Fill(10, 200, ""))
    val orderStatus = OrderStatus(1, "acc", "v1", "s1", "sell", 100, 100, 10, "limit", "", fills, 100, open = false)
    val execution = Execution("acc", "v1", "s1", orderStatus, 1, 1, 100, 10, "999", standingComplete = false, incomingComplete = false)

    val expected = OrderBook("v1", "s1", Some(List(Request(10, 1800, isBuy = true), Request(5, 30, isBuy = true), Request(1, 9999, isBuy = true))), Some(asks), "999")

    rep.addOrderBook(orderBook1)
    rep.addExecution(execution)
    rep.getOrderBook("v1", "s1") should be(Some(expected))
  }

  private def fixture(f: Repository => Unit) = {
    val rep = Repository()
    f(rep)
    rep.clear()
  }
}
