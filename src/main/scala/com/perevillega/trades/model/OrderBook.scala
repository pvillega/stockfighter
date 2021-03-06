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

package com.perevillega.trades.model

import com.perevillega.trades._

final case class Request(price: Int, qty: Int, isBuy: Boolean)

final case class OrderBook(venue: String, symbol: String, bids: Option[List[Request]], asks: Option[List[Request]], ts: String) {
  val requestsToBuy = bids.getOrElse(Nil)
  val requestsToSell = asks.getOrElse(Nil)
  lazy val time = dateFormatter.parseDateTime(ts)
}
