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
import com.perevillega.trades.model.Order
import com.perevillega.trades.model.OrderType.Market
import freek._
import monix.cats._

object Chapter1 extends TradingAppHelpers {
  // This is the only exception regarding Solutions provided
  // Level 1 just requires you to buy some stock, so let's try this. Our strategy:
  // - get the latest quote for the stock
  // - send a 'buy' order with 'ask' (current sell value) + 50 points (50 cents) or, if
  //   no ask value set (happens!) then offer $10 (1000, remember decimals).
  // - See happy results in the output!

  val venue = "SGJEX"
  val account = "BP12793249"
  val stock = "FOC"

  val program = for {
    q <- stockQuote(venue, stock).freeko[API, O]
    b <- buy(venue, account, Order(stock, q.ask.map(_+ 50).getOrElse(1000), 100, Market)).freeko[API, O]
  } yield b

  val result = run(program)
  println(s"Chapter 1: $result")
}
