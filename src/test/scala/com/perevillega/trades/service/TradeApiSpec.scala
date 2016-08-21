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

import cats.data.Xor
import cats.free._
import cats.std.all._
import com.perevillega.trades.model.{Order, OrderStatus}
import org.scalatest.{FlatSpec, Matchers}
import freek._
import cats.syntax.xor._
import com.perevillega.trades.Result

class TradeApiSpec extends FlatSpec with Matchers {
  import TradeApi._
  type O = Result[?] :&: List :&: Bulb

  "TradeApi" should "run a small program as expected" in {
    val interpreter = TradeApiTestInterpreter
    val mockOrderStatus = OrderStatus(1, "account", "venue", "stock", "buy", 100, 100, "limit", "", Nil, 100, false)
    interpreter.mockValue(IsApiUp, true.right[String])
    interpreter.mockValue(IsVenueUp("venue"), true.right[String])
    interpreter.mockValue(Buy("venue", "account", Order("stock", 10, 100)), mockOrderStatus.right[String])

    val program = for {
      _ <- isStockfighterUp.freeko[PRG, O]
      _ <- isVenueUp("venue").freeko[PRG, O]
      b <- buy("venue", "account", Order("stock", 10, 100)).freeko[PRG, O]
    } yield b

    val expectedExecution = List("IsApiUp", "IsVenueUp(venue)", "Buy(venue,account,Order(stock,10,100,limit))")
    val result = program.value.interpret(interpreter)
    result.runS(Nil).value should be(expectedExecution)
    result.runA(Nil).value should be(Xor.Right(List(mockOrderStatus)))

    interpreter.clear()
  }

  it should "exit a program if we get an error during execution" in {
    val interpreter = TradeApiTestInterpreter
    val mockOrderStatus = OrderStatus(1, "account", "venue", "stock", "buy", 100, 100, "limit", "", Nil, 100, false)
    interpreter.mockValue(IsApiUp, true.right[String])
    interpreter.mockValue(IsVenueUp("venue"), Xor.Left("The venue is not up"))
    interpreter.mockValue(Buy("venue", "account", Order("stock", 10, 100)), mockOrderStatus.right[String])

    val program = for {
      _ <- isStockfighterUp.freeko[PRG, O]
      vUp <- isVenueUp("venue").freeko[PRG, O]
      b <- buy("venue", "account", Order("stock", 10, 100)).freeko[PRG, O]
    } yield b

    val expectedExecution = List("IsApiUp", "IsVenueUp(venue)")
    val result = program.value.interpret(interpreter)
    result.runS(Nil).value should be(expectedExecution)
    result.runA(Nil).value should be(Xor.Left("The venue is not up"))

    interpreter.clear()
  }

  it should "iterate over elements of an array" in {
    val interpreter = TradeApiTestInterpreter
    val mockOrderStatus = OrderStatus(1, "account", "venue", "stock", "buy", 100, 100, "limit", "", Nil, 100, false)
    interpreter.mockValue(IsApiUp, true.right[String])
    interpreter.mockValue(IsVenueUp("venue"), true.right[String])
    interpreter.mockValue(VenueStocks("venue"), Xor.Right(List("s1", "s2", "s3")))
    interpreter.mockValue(Buy("venue", "account", Order("s1", 10, 100)), mockOrderStatus.right[String])
    interpreter.mockValue(Buy("venue", "account", Order("s2", 10, 100)), mockOrderStatus.right[String])
    interpreter.mockValue(Buy("venue", "account", Order("s3", 10, 100)), mockOrderStatus.right[String])

    val program = for {
      _ <- isStockfighterUp.freeko[PRG, O]
      _ <- isVenueUp("venue").freeko[PRG, O]
      stock <- venueStocks("venue").freeko[PRG, O]
      b <- buy("venue", "account", Order(stock, 10, 100)).freeko[PRG, O]
    } yield b

    val expectedExecution = List("IsApiUp", "IsVenueUp(venue)", "VenueStocks(venue)", "Buy(venue,account,Order(s1,10,100,limit))", "Buy(venue,account,Order(s2,10,100,limit))", "Buy(venue,account,Order(s3,10,100,limit))")
    val result = program.value.interpret(interpreter)
    result.runS(Nil).value should be(expectedExecution)
    result.runA(Nil).value should be(Xor.Right(List(mockOrderStatus, mockOrderStatus, mockOrderStatus)))

    interpreter.clear()
  }

}
