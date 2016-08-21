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

package com.perevillega.trades.repository

import com.perevillega.trades.model._
import com.perevillega.trades.model.internal.InternalApiModel.OrderDirection.BuyOrder

import scala.annotation.tailrec
import scala.collection.mutable

class VenueRepository {
  private val quotes = mutable.Map[String, Quote]()
  private val stockOrderBooks = mutable.Map[String, OrderBook]()

  def addQuote(q: Quote): Unit = { val ignored = quotes += (q.symbol -> q) }
  def getQuote(stock: String): Option[Quote] = quotes.get(stock)
  def getAllQuotes: Iterable[Quote] = quotes.values

  def addOrderBook(o: OrderBook): Unit = { val ignored = stockOrderBooks += (o.symbol -> o) }
  def getOrderBook(stock: String): Option[OrderBook] = stockOrderBooks.get(stock)
  def addExecution(e: Execution): Unit =
    getOrderBook(e.symbol).map { ob =>

      val (bids, asks) =
        if(e.order.direction == BuyOrder.toString) {
          val sortedFills = e.order.fills.sortBy(_.price) // requests to sell are ordered by price
          (ob.bids, Some(matchFillsAndRequests(sortedFills, ob.requestsToSell)))
        } else {
          val sortedFills = e.order.fills.sortBy(_.price).reverse // requests to buy are ordered in decreasing price
          (Some(matchFillsAndRequests(sortedFills, ob.requestsToBuy)), ob.asks)
        }

      ob.copy(bids = bids, asks = asks, ts = e.filledAt)
    }.foreach(nob => addOrderBook(nob))

  @tailrec
  private def matchFillsAndRequests(fills: List[Fill], requests: List[Request]): List[Request] = (fills, requests) match {
    case (Nil, _) => requests
    case (_, Nil) => Nil
    case (fill :: fillTail, request :: reqTail) if fill.price != request.price =>
      throw new RuntimeException(s"Abort program, this shouldn't happen: Unmatched fill with request\n Fills [$fills]\n Requs [$requests]")
    case (fill :: fillTail, request :: reqTail) if fill.qty > request.qty =>
      val remainingFills = fill.copy(qty = fill.qty - request.qty) :: fillTail
      val remainingRequests = reqTail
      matchFillsAndRequests(remainingFills, remainingRequests)
    case (fill :: fillTail, request :: reqTail) if fill.qty == request.qty =>
      val remainingFills = fillTail
      val remainingRequests = reqTail
      matchFillsAndRequests(remainingFills, remainingRequests)
    case (fill :: fillTail, request :: reqTail) if fill.qty < request.qty =>
      val remainingFills = fillTail
      val remainingRequests = request.copy(qty = request.qty - fill.qty) ::reqTail
      matchFillsAndRequests(remainingFills, remainingRequests)
  }
}

class Repository {
  private val venues = mutable.Map[String, VenueRepository]()
  private val quoteHooks = mutable.ListBuffer[Quote => Unit]()
  private val executionHooks = mutable.ListBuffer[Execution => Unit]()

  def getVenues: Iterable[String] = venues.keys

  def addQuote(q: Quote): Unit = {
    venues.getOrElseUpdate(q.venue, new VenueRepository).addQuote(q)
    quoteHooks.foreach(_(q))
  }
  def getQuote(venue: String, stock: String): Option[Quote] = venues.get(venue).flatMap(_.getQuote(stock))
  def getAllQuotes(venue: String): Iterable[Quote]  = venues.get(venue).map(_.getAllQuotes).getOrElse(Iterable.empty)
  def getAllQuotes: Iterable[Quote]  = venues.keys.flatMap(v => getAllQuotes(v))

  def addOrderBook(o: OrderBook): Unit = venues.getOrElseUpdate(o.venue, new VenueRepository).addOrderBook(o)
  def getOrderBook(venue: String, stock: String): Option[OrderBook] = venues.get(venue).flatMap(_.getOrderBook(stock))
  def addExecution(e: Execution): Unit = {
    venues.getOrElseUpdate(e.venue, new VenueRepository).addExecution(e)
    executionHooks.foreach(_(e))
  }

  def addHookForQuote(f: Quote => Unit): Unit = {
    val ignore = quoteHooks += f
  }
  def addHookForExecution(f: Execution => Unit): Unit = {
    val ignore = executionHooks += f
  }

  def clear(): Unit = {
    venues.clear()
    quoteHooks.clear()
    executionHooks.clear()
  }
}

object Repository {
  private val repo = new Repository

  def apply(): Repository = repo
}
