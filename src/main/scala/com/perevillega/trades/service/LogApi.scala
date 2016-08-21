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

import cats.~>
import com.perevillega.trades._
import com.perevillega.trades.support.TestInterpreter
import freek._
import monix.eval.Task

// Defines the API for logging.
object LogApi {

  sealed trait LogLevel
  case object ErrorLevel extends LogLevel
  case object WarnLevel extends LogLevel
  case object InfoLevel extends LogLevel
  case object DebugLevel extends LogLevel

  // The DSL can be thought about as a mapping of a method call to a case class. For example, we want
  // to turn the method 'log(level, message) : Unit' into a Free Monad. We create the below case class
  // `LogMsg` that takes as parameters the params of the method and sets the return type as the `A` in `DSL`
  trait DSL[A]
  final case class LogMsg(level: LogLevel, msg: String) extends DSL[Unit]

  // Freek magic to get a Free Monad from the DSL
  type PRG = DSL :|: FXNil
  val PRG = Program[PRG]

  // Helper methods. As usual they just create an instance of the DSL, don't really log anything
  def debug(msg: String) = LogMsg(DebugLevel, msg)
  def warn(msg: String) = LogMsg(WarnLevel, msg)
  def info(msg: String) = LogMsg(InfoLevel, msg)
  def error(msg: String) = LogMsg(ErrorLevel, msg)
}

// First interpreter, natural transformation from DSL to a State monad. We used a custom
// type ListState to represent a State[List[String], A]. When run you can get a printout of the program
// and see visually what it will do
object LogApiAccumulatingInterpreter extends (LogApi.DSL ~> ListState) {
  def apply[A](a: LogApi.DSL[A]) = addToState(s"$a", ().asInstanceOf[A])
}

// Second interpreter, natural transformation from DSL to Task. This is the 'executing'
// interpreter that will, as a result, print things to the console.
// Note that we encapsulate the side effects within the interpreter, so we have full control.
object LogApiTaskInterpreter extends (LogApi.DSL ~> Task) {
  import LogApi._

  private def printWithColor(level: LogLevel, msg: String) =
    println(color(level) + s"[$level] - $msg" + Console.RESET)

  private def color(level: LogLevel) = level match {
    case _@ DebugLevel => Console.RESET
    case _@ InfoLevel => Console.BLUE
    case _@ WarnLevel => Console.YELLOW
    case _@ ErrorLevel => Console.RED
  }

  def apply[A](a: LogApi.DSL[A]) = a match {
    case LogMsg(lvl, msg) => Task(printWithColor(lvl, msg))
  }
}

// Interpreter to be used for test purposes. See TestInterpreter for details
object LogApiTestInterpreter extends (LogApi.DSL ~> ListState) with TestInterpreter[LogApi.DSL] {
  def apply[A](a: LogApi.DSL[A]) = addToState(s"$a", getResult(a, ()).asInstanceOf[A])
}