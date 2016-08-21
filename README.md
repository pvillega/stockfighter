# StockFighter 

This is the project I used to solve [StockFighter](https://www.stockfighter.io/) levels.

** Please note that solutions are not included.**

This only provides an API that helped me to solve the levels.

## Implementation Details
 
This program uses [FreeK](https://github.com/ProjectSeptemberInc/freek) to implement [Free Monad](http://typelevel.org/cats/tut/freemonad.html).
I wrote a [post](http://perevillega.com/understanding-free-monads) on Free Monads you may want to read first.

For Stockfighter the benefit of Free may not be apparent at first, but you can consider Free Monad as a compile-time dependency injection
mechanism. By providing a certain interpreter to a program, we can change how is it executed, deciding that on compile time.

Open file `Introduction.scala` and read top to bottom to see examples on how to use the API. 

Pull requests are welcome, please send feedback via [twitter](https://twitter.com/pvillega). 


## License

Copyright 2016 Pere Villega

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
