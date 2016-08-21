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

package com.perevillega.trades.model.internal

import java.io.Serializable

import com.perevillega.trades.model._

// These classes are used to deserialise Json from Stockfighter.
// Not intended for human consumption, we provide 'nicer' responses in the API when possible
object InternalApiModel {

  final case class TradeError(error: String)
  final case class Ok()

  sealed trait OrderDirection extends Product with Serializable
  object OrderDirection {
    case object BuyOrder extends OrderDirection {
      override def toString = "buy"
    }
    case object SellOrder extends OrderDirection{
      override def toString = "sell"
    }
  }

  final case class NewOrder(venue: String, account: String, stock: String, price: Int, qty: Int, direction: String, orderType: String)
  object NewOrder {
    def apply(venue: String, account: String, order: Order, direction: OrderDirection): NewOrder =
      NewOrder(venue, account, order.stock, order.price, order.qty, direction.toString, order.orderType.toString)
  }

  final case class Stock(name: String, symbol: String)
  final case class StocksOnVenueResponse(symbols: List[Stock])

  final case class AllOrdersStatus(venue: String, orders: List[OrderStatus])

  final case class TickerTape(quote: Quote)
}
