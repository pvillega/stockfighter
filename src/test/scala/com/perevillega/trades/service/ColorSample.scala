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
import monix.cats._
import monix.execution.Scheduler.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps

// verifying colors work in terminal
object ColorSample extends App {
  import LogApi._

  val program = for {
    _ <- debug("debug").freek[PRG]
    _ <- warn("warn").freek[PRG]
    _ <- info("info").freek[PRG]
    _ <- error("error").freek[PRG]
    _ <- debug("debug").freek[PRG]
    _ <- warn("warn").freek[PRG]
    _ <- info("info").freek[PRG]
    _ <- error("error").freek[PRG]
  } yield ()

  Await.result(program.interpret(LogApiTaskInterpreter).runAsync, 10 seconds)
}
