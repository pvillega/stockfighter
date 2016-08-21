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

package com.perevillega.trades.model.json

import com.perevillega.trades.model._
import com.perevillega.trades.model.internal.InternalApiModel._
import io.circe._
import io.circe.generic.semiauto._

// Implicits to encode/decode json and our case classes
object CirceImplicits {
  // encoders
  implicit val newOrderDecoder: Encoder[NewOrder] = deriveEncoder[NewOrder]

  // decoders
  implicit val allOrderStatusDecoder: Decoder[AllOrdersStatus] = deriveDecoder[AllOrdersStatus]
  implicit val executionDecoder: Decoder[Execution] = deriveDecoder[Execution]
  implicit val fillDecoder: Decoder[Fill] = deriveDecoder[Fill]
  implicit val orderDecoder: Decoder[Order] = deriveDecoder[Order]
  implicit val orderBookDecoder: Decoder[OrderBook] = deriveDecoder[OrderBook]
  implicit val orderDirectionDecoder: Decoder[OrderDirection] = deriveDecoder[OrderDirection]
  implicit val orderEntryDecoder: Decoder[Request] = deriveDecoder[Request]
  implicit val orderStatusDecoder: Decoder[OrderStatus] = deriveDecoder[OrderStatus]
  implicit val orderTypeDecoder: Decoder[OrderType] = deriveDecoder[OrderType]
  implicit val quoteDecoder: Decoder[Quote] = deriveDecoder[Quote]
  implicit val statusOkDecoder: Decoder[Ok] = deriveDecoder[Ok]
  implicit val stockDecoder: Decoder[Stock] = deriveDecoder[Stock]
  implicit val stockOnVenueResponseDecoder: Decoder[StocksOnVenueResponse] = deriveDecoder[StocksOnVenueResponse]
  implicit val tickerTapeDecoder: Decoder[TickerTape] = deriveDecoder[TickerTape]
  implicit val tradesApiErrorDecoder: Decoder[TradeError] = deriveDecoder[TradeError]

}
