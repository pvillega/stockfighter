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

final case class Fill(price: Int, qty: Int, ts: String) {
  lazy val time: DateTime = dateFormatter.parseDateTime(ts)
}

final case class OrderStatus(id: Int, account: String, venue: String, symbol: String, direction: String, originalQty: Int,
                             qty: Int, orderType: String, ts: String, fills: List[Fill], totalFilled: Int, open: Boolean) {
  val orderTypeObj = OrderType(orderType)
  lazy val time = dateFormatter.parseDateTime(ts)
}
