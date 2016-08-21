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

package com.perevillega

import cats.data._
import org.joda.time.format.ISODateTimeFormat

import scala.language.higherKinds

// Some convenience types and helper methods used across the app
package object trades {
  type ErrorMessage = String

  type Result[A] = ErrorMessage Xor A
  type ResultT[F[_], A] = XorT[F, ErrorMessage, A]

  type ListState[A] = State[List[String], A]

  val dateFormatter = ISODateTimeFormat.dateTime()

  def addToState[A](s: String, elem: A): ListState[A] = State(l => (l ++ List(s), elem))
}
