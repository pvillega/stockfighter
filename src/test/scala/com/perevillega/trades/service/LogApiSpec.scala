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

import freek._
import org.scalatest.{FlatSpec, Matchers}

class LogApiSpec extends FlatSpec with Matchers {

  import LogApi._

  val interpreter = LogApiTestInterpreter

  val program = for {
    _ <- LogApi.debug("Debug message").freek[PRG]
    _ <- LogApi.info("Info message").freek[PRG]
    _ <- LogApi.warn("Warn message").freek[PRG]
    _ <- LogApi.error("Error message").freek[PRG]
  } yield ()

  "LogAPI" should "accumulate log messages with test interpreter" in {
    val state = program.interpret(interpreter)
    val expected = List("LogMsg(DebugLevel,Debug message)", "LogMsg(InfoLevel,Info message)", "LogMsg(WarnLevel,Warn message)", "LogMsg(ErrorLevel,Error message)")
    state.runS(Nil).value should be(expected)
  }
}
