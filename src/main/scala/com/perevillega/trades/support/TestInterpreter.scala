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

package com.perevillega.trades.support

import scala.collection.mutable
import scala.language.higherKinds

// Trait to be extended by Interpreters (Natural transformations) we will use
// during testing. Provides helper methods to ensure our applications do
// what we expect
trait TestInterpreter[DSL[_]] {

  private val mockedResults = mutable.Map[DSL[_], Any]()

  def mockValue(from: DSL[_], ret: Any): Unit = {
    val ignore = mockedResults += from -> ret
  }

  def getResult(from: DSL[_], default: Any) = mockedResults.getOrElse(from, default)

  def clear(): Unit = mockedResults.clear()

}
