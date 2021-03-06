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
import org.joda.time.DateTime

final case class Quote(venue: String, symbol: String, bid: Option[Int], ask: Option[Int], bidSize: Int, askSize: Int,
                       bidDepth: Int, askDepth: Int, last: Int, lastSize: Int, lastTrade: String, quoteTime: String) {
  lazy val lastTradeDate: DateTime = dateFormatter.parseDateTime(lastTrade)
  lazy val quoteTimeDate: DateTime = dateFormatter.parseDateTime(quoteTime)
  val salePrice = ask.getOrElse(0)
  val buyPrice = bid.getOrElse(0)
  val saleQty = askSize
  val buyQty = bidSize
}
