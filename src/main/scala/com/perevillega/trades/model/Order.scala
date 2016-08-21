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

import java.io.Serializable

import com.perevillega.trades.model.OrderType.Limit

final case class Order(stock: String, price: Int, qty: Int, orderType: OrderType = Limit)

sealed trait OrderType extends Product with Serializable
object OrderType {
  case object Limit extends OrderType {
    override def toString = "limit"
  }
  case object Market extends OrderType {
    override def toString = "market"
  }
  case object FillOrKill extends OrderType {
    override def toString = "fill-or-kill"
  }
  case object ImmediateOrCancel extends OrderType {
    override def toString = "immediate-or-cancel"
  }

  val values = List[OrderType](Limit, Market, FillOrKill, ImmediateOrCancel)

  def apply(s: String): Option[OrderType] = values.find(_.toString == s)
}
