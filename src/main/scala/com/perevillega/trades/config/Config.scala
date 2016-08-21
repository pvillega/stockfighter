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

package com.perevillega.trades.config

import pureconfig._

import scala.util.{Failure, Success}

final case class Config(endpoints: EndpointsConfig, api: ApiConfig, testAccount: TestAccountConfig)
final case class EndpointsConfig(baseUrl: String, webSocketUrl: String)
final case class ApiConfig(apiKey: String)
final case class TestAccountConfig(account: String, venue: String, stock: String)

// Uses Pureconfig to load the configuration from application.conf
object Config {
  private val config = loadConfig[Config] match {
    case Success(cfg) => cfg
    case Failure(t) => throw t
  }

  val mockAccount = config.testAccount.account
  val mockVenue = config.testAccount.venue
  val mockStockSymbol = config.testAccount.stock

  val apiEndpoint = config.endpoints.baseUrl
  val wsEndpoint = config.endpoints.webSocketUrl
  val apiKey = config.api.apiKey
}
