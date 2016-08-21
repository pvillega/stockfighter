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

package com.perevillega.trades.app

import com.perevillega.trades.model.{Execution, Order, Quote}
import com.perevillega.trades.service._
import LogApi._
import TradeApi._
import freek._
import monix.cats._
import cats.std.all._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.duration._
import scala.concurrent.Await
import scala.language.{higherKinds, postfixOps}

object Introduction extends TradingAppHelpers {

  // This class shows how to use the API. This API is built using Free Monads, with Freek (https://github.com/ProjectSeptemberInc/freek)
  // to facilitate building staff with Cat's Free.

  // Why Free Monads? Because we can, and also because I wanted to learn about them and Stockfighter was a perfect case study:
  // It's small enough in surface that the resulting API will be small, but it's a real application and showcases some utility.
  // Mind you, some of the power of Free Monads won't be exploited in this implementation, this is just a taste.

  // Want to learn to use this implementation? Yes? Ok! Let's start.
  // Note that this class extends a Trait. This trait should be extended by all your runnable objects (the trait extends 'App')
  // as it provides several important support methods that will make your life easier.

  // The imports below are needed for all the examples to work. The ones at the top of the file are also required, but these ones have
  // special relevance. If you get an error, first of all check if you forgot any of them!
  // Don't forget them!
  import LogApi._
  import TradeApi._
  import freek._
  import monix.cats._
  import cats.std.all._

  // The first step is to define programs that interact with the Stockfighter API. By using Free Monads we separate the definition step
  // from the execution step. Let's see what I mean by creating a simple program:

  def example1(venue: String, account: String) = for {
    _ <- info("Example 1: Is api up?").freeko[API, O]
    b <- isStockfighterUp.freeko[API, O]
    _ <- debug(s"Is api up - $b").freeko[API, O]
  } yield b

  // Although it may seem otherwise, this program does nothing. It's just defining a behaviour, using an API. No execution takes place,
  // no logs are printed, no requests are sent. It just builds an object that contains the steps to run, the Free Monad.
  //
  // Let's analise the example:
  //
  // - We have a method 'example1' with 2 parameters (venue and account)
  // - We use LogApi helper methods to define an 'info' level message and a second message at 'debug' level.
  //   Note we don't care about the return value (Unit) so we ignore it with the '_ <-'
  // - We check if StockFighter api is up via the helper method 'isStockfighterUp'. We will return the response 'b' as result of the program
  // - All the helper methods have '.freeko[API, O]' appended. This is a bit of Freek magic so we can work in a more convenient
  //   way with the Free monads, otherwise order of DSL and different result types cause issues. It's a bit of boilerplate
  //   but worth the cost
  // - Our API is defined to return Result[A], which is a type alias for Xor[String, A]. See http://typelevel.org/cats/tut/xor.html for more detail
  //
  // Understood? You may want to check the code in objects LogApi and TradeApi to read more comments about the DSL, also to see how
  // easy is to define the API
}

object Introduction_1 extends TradingAppHelpers {
  // Ok, so we have defined the program. But as we mentioned before, this is just a definition. If you see the
  // helper method's implementation, they just instantiate case classes, there is no execution logic. Just a series of steps.
  // So, how do we execute it?
  // We interpret it. We run our program through a method that identifies every step defined and does something with it.
  // This is called a Natural Transformation, that converts our monad (Free Monad) to another Monad (Option, Either, Task, State, etc)
  // following certain rules.
  //
  // As an example, if you open the trait 'TradingAppHelpers' and look at method 'inspectAsString' you will see it call an interpreter:
  //
  //    program.value.interpret(accumulatingInterpreter)
  //
  // This step uses an interpreter (accumulatingInterpreter) to transform our program. If you follow its implementation
  // and browse the code of each of its components you'll notice the natural transformation is little more than a
  // pattern match on all the cases of the DSL. This means each case class in the DSL (each method of our API) will be
  // mapped to one action within that natural transformation
  //
  // In the above scenario both LogApi and TradeApi have an accumulating interpreter that adds the case class (as String)
  // to a State monad. The result of the interpretation is that State monad. You can see that the helper method 'inspectAsString'
  // runs the monad, with 'runS', to extract the latest state, which contains a list of all the computation steps defined in our program.

  // Let's run it in reality and print the result:
  // NOTE: remember this file extends trait App so you can execute it as an object with Main method. 'run-main' in sbt, or 'run' in your IDE
  val stateAsString = inspectAsString(Introduction.example1(testVenue, testAccount))
  println(">> Result State monad - status: " + stateAsString)

  // Did you see the result? It should be something like:
  //
  //   List(LogMsg(InfoLevel,Example 1: Is api up?), IsApiUp, LogMsg(DebugLevel,Is api up - false))
  //
  // That is, a list of the steps in our program, as defined. Info log message, IsApiUpCheck, Debug log message.
  // Note that the debug message has a value of 'false', because our interpreter is returning that value for 'IsApiUpCheck'.
  // Feel free to edit the interpreters (LogApiAccumulatingInterpreter and TradeApiAccumulatingInterpreter)
  // to see how the changes affect the output of the program.

  // This is giving us a hint to the power beneath a Free Monad:
  // - You separate declaration from execution. This works well in many environments, especially DDD where we first define a language
  //   and business rules. You don't have to tangle yourself on technical details (http, database, etc) when defining the business steps
  // - You can use many interpreters for the same program. This means you define the logic once and then you can run it with an
  //   execution interpreter, and auditing one, and any other you may want/need to.
  // - Isolation of side effects. Your logic is pure, a series of steps. Your interpreters are the ones in charge of the side effects,
  //   and manage them internally, isolated from the rest of the code, like black boxes
  // - Passing interpreters to your program can be considered compile-time dependency injection, where you provide a context for execution.
  //   In the same way you can easily swap interpreters with minimal impact to code. For example, replace an Http interpreter by one
  //   that uses Pub/Sub and messages. This is transparent to all your defined programs, you only replace the interpreter.
  //
  // Yes, there is a small penalty in performance (less than you would expect thanks to some smart tricks in Cats' implementation)
  // but usually that's not your bottleneck, you can measure it, and having good code which is easy to maintain and refactor is worth it.
}

object Introduction_2 extends TradingAppHelpers {

  // Ok, so we have defined a program and executed it using a interpreter to State to see what was defined within that program.
  // Should we run it against Stockfighter itself?
  // To do so, first edit 'application.conf' and set your Stockfighter API keys, so you can later play around with the Free Monads
  // without any error. We provide values 'testAccount', 'testVenue', and 'testStock' that match the account, venue, and stock of
  // the test playground of Stockfighter. They are defined in trait 'TradingAppHelpers'

  // A helper function 'run' takes a program (Free Monad) and executes it using an http client (GigaHorse - http://eed3si9n.com/gigahorse/)
  // To use it, run:
  val result = run(Introduction.example1(testVenue, testAccount))
  println(">> Result: " + result)

  // Note that the method uses an interpreter that returns a Task, and executes the task within 'run'. We do this to avoid having dangling
  // 'httpClient' sockets after execution finishes. Feel free to tweak as desired to obtain the Task inside, but if you do so remember
  // to close the 'httpClient' after you complete your tasks, otherwise you'll process won't exit.

  // We have run the example twice. In both cases the principle is the same as when we used the State Monad interpreter.
  // We use an interpreter, a natural transformation, to perform actions on each step of our defined program. In this case you'll notice
  // that 'LogApiTaskInterpreter' is causing messages to appear in the console, coloured as per log level, while 'TradeApiTaskInterpreter'
  // has executed http requests against Stockfighter. Check their implementations to see how they work.
  //
  // Look at this again. We defined the program once, and we have obtained two completely different behaviours from it with just a small change,
  // using a different interpreter.
  // Imagine that in the future Stockfighter provides a Kafka endpoint to interact with it via messages. Or that you want to use a new
  // fancier http library. Now consider what would be the impact on this application, what would you need to replace or refactor.
  // And consider the impact a similar change would have on the codebase at your work. Convinced? ;)
}

object Introduction_3 extends TradingAppHelpers {
  // Ok, let's move to more examples, so we can understand the API better. Remember the LogApi returns Unit on all its methods, as
  // logging is a side effect at best. TradeApi, though, returns 'Result[A]' which is a type alias for 'Xor[String, A]'.
  // We expect a for-comprehension to short-circuit if we get a left result. Is that still true when using our Free Monad?
  // Consider that our helper methods return case classes, of type 'DSL[Result[A]]', so that behaviour is not guaranteed!
  //
  // Let's try to do something that will fail with a Left result, like buying an invalid stock in the test server:

  def example2(venue: String, account: String) = for {
    _ <- info("Example 2: Does it shortcircuit?").freeko[API, O]
    b <- buy(venue, account, Order("BAD", -1, -1)).freeko[API, O]
    _ <- error(s"YOU SHOULDN'T SEE THIS TEXT!").freeko[API, O]
  } yield b

  // Now let's run this failing program
  val result2 = run(example2(testVenue, testAccount))
  println(">> Result 2: " + result2)

  // If you run it, you should not see the error message printed, and 'Result 2' will display a Left with the error received.
  // So, good, our Free Monad behaves as expected according to the result we get.

  // There is something you may have noticed from the output of 'example2', the result is a Right[List[Boolean]], when maybe
  // we would expect a Right[Boolean]. This is due to the use fo Onion to stack different effects.
  // If you don't call a method that returns a List (like 'venueStocks', or 'allMyOrders') then you don't need the onion.
  // You can replace the '.freeko[API, O]' part ny '.freek[API]' and obtain a 'Result[A]' as result, which may be easier to handle.
  // Although be aware if you do that, you won't be able to use the support methods provided (type mismatch with Onion, sorry!)
  // But if you need the list, you'll need to bear with the 'Right[List[A]]' result by now :)
}

object Introduction_4 extends TradingAppHelpers {

  // More experimentation. What about methods that return a List? How do we interact with them?
  // This is one of the places where the magic of Freek makes our life much easier. Thaks to the Onion concept (don't worry too
  // much about it) it exposes the elements of the list directly to us. We don't need pesky 'traverseU' nor similar arcane arts
  // to iterate over it.
  // Let's see an example:

  def example3(venue: String, account: String) = for {
    _ <- info("Example 3: Using list responses").freeko[API, O]
    stock <- venueStocks(venue).freeko[API, O]
    b <- buy(venue, account, Order(stock, 1, 100)).freeko[API, O]
    _ <- warn(s"Buy order for $stock sent").freeko[API, O]
  } yield b

  // Now let's run the program. We expect to see one buy order per stock available in the venue
  val result3 = run(example3(testVenue, testAccount))
  println(">> Result 3: " + result3)

  // As you can see, we sent a buy order per stock. Unfortunately the test server only has one stock, so you will have to
  // trust me and believe this works when you have multiple stocks (Hint: you can hack the interpreter to return a hardcoded
  // list of stocks for 'venueStocks' to verify that is true)
}

object Introduction_5 extends TradingAppHelpers {

  // At this point you should have a grasp on how to use the TradeApi to solve Stockfighter levels.
  // But if you check Stockfighter documentation you will notice they provide a few Websocket endpoints.
  // How can you use them with our Free Monad API?
  // Let's look at one example:

  def example4(venue: String, account: String) = {
    def onMessage(s: String) = println(s)
    def onError(t: Throwable) = throw t

    for {
      _ <- info("Example 4: Using Websockets").freeko[API, O]
      ws <- tickerTape(venue, account, onMessage, onError).freeko[API, O]
      _ <- warn(s"Websocket open").freeko[API, O]
    } yield ws
  }

  // The example calls 'tickerTape' to start the websocket and obtains a reference to the websocket iself, 'ws', which
  // is the result of the program. You can use that reference to send messages via the Websocket, althought that's nothing
  // very useful in StockFighter as they ignore any message you send!
  //
  // Did you notice we defined 2 additional methods within the example, 'onMessage' and 'onError'?
  // Websockets are side-effect-tastic, as you get a stream of data that you want to react to.
  // Our way to provide some control over the behaviour of our websocket connection is via there methods that you can pass
  // to any websocket-related API call. As the names say, 'onError' will be called when the websocket gets an exception,
  // while 'onMessage' will be executed each time you receive a message. Mind you, this can be VERY OFTEN
  //
  // Both 'onMessage' and 'onError' have 2 default implementations that do what you see in the 'example4' implementations.
  // A word of advice: don't rely on the 'println' behaviour for 'onMessage', it's there for use against the test venue,
  // in real scenarios it will crash your terminal :)

  // Now let's run the program to see our websocket in action!
  val result4 = run(example4(testVenue, testAccount))
  println(">> Result 4 (Websocket): " + result4)

  // There is an unfortunate side effect or running the websocket with 'run': we close all the http connections after 'run'
  // terminates. This means the websocket is closed after interpretation! That kinda defeats the purpose of the WebSocket,
  // which we probably want running in the background while we do smart things.
  // To alleaviate this issue we provide a small and dirty hack (psss, don't tell! and we won't!):

  val (client, result4b) = runAndGetHttpClient(example4(testVenue, testAccount))
  println(">> Result 4b (Websocket): " + result4b)
  client.close()

  // The method 'runAndGetHttpClient' executes the Free Monad with the Task interpreter but it doesn't close the http client
  // at the end. Instead, it returns a pair of object: the http client and the result (websocket in our program).
  // This can be used ot keep the websocket open until we manually close the http connection. Note that using this means you
  // responsible of closing the connection, otherwise bad things may happen. Try commenting the 'client.close()' line and see by yourself.
}

object Introduction_6 extends TradingAppHelpers {
  // Thing is, you are most likely going to use websocket only to update your own local Order Book, so you can be aware of the status
  // of the venue and its stocks before you do something. That's an expected task, and wiring everything can be slightly cumbersome.
  // So we provide a helper method that does that for you!
  val (clientRepo, myRepo) = initialiseRepository(testVenue, testAccount)
  println(">> Result 5: " + myRepo)
  clientRepo.close()

  // The function 'initialiseRepository' is available via 'TradingAppHelpers' trait (please check how it is implemented!). It
  // cleans the local repository and launches a pair of websocket tasks to keep it up to date. It also return a reference to
  // the client so you can close it once you are done updating your repository.
  //
  // Wondering what's a repository? It's just a class (see Repository.scala) that stores Quotes and Order Books for venues and
  // their stocks. It's helper methods allow you to query for data so you get a clear image of the current situation within a
  // venue. Or, at least, that's the theory.
  // Note that we provide a single repository across the application, every time you call 'apply' you get the same instance. The
  // reasoning behind this decision is to avoid headaches due to independent repositories living in your code. By providing a
  // Singleton (yup, bej!) we are sure all the data you care about is in one place. A trade-off, for a simpler life.
  //
  // Please note calling 'initialiseRepository' resets the existing data inside the repository before it starts collecting new data.
}

object Introduction_7 extends TradingAppHelpers {
  // As we saw, you have access to a command 'initialiseRepository' which creates a Repository instance 
  // that will try to track the order book for the venue.
  // Handy as it may be, you may also want to add custom behaviour to the Repository. For example, you
  // may want to track Quote changes for a stock, or maybe you want to print Executions for a certain account
  // to devise their strategy.
  // To avoid breaking any Stockfighter quotas regarding Websockets, we provide a way to add your own hooks
  // to the Repository, so any message it receives will be passed to your own functions. For example:
  val quoteTracking = (q: Quote) => println(s"Received quote $q")
  val executionTracking = (e: Execution) => println(s"Received execution $e")
  val (clientRepo, myRepo) = initialiseRepository(testVenue, testAccount)
  myRepo.addHookForQuote(quoteTracking)
  myRepo.addHookForQuote(quoteTracking)
  myRepo.addHookForExecution(executionTracking)
  println(">> Result 6 (with hooks): " + myRepo)
  Thread.sleep(10000) // sleep a bit to se prints on console
  clientRepo.close()

  // The methods 'addHookForQuote' and 'addHookForExecution' allow you to provide your own hooks, reusing the
  // same websocket we created for the Repository. You can add multiple hooks for each type (Quote or Execution),
  // Just please be aware that:
  // - hooks execute after the repository itself is updated
  // - hooks execute sequentially, in the order they were added
  // - a long running hook could, potentially, cause issues with the data in the repository. So keep them simple.

  // Use your imagination with these hooks, they can be VERY handy ;)

  // Ok, that's it. Introduction over. You should now be able to use the API on your own, to start cruising through those pesky
  // Stockfigther levels.
  // As always, feedback is welcome (Twitter handle: pvillega) and Pull requests to make the API better are appreciated and will
  // not be ignored, promise!
}
