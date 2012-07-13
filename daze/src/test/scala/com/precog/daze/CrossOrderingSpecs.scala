/*
 *  ____    ____    _____    ____    ___     ____ 
 * |  _ \  |  _ \  | ____|  / ___|  / _/    / ___|        Precog (R)
 * | |_) | | |_) | |  _|   | |     | |  /| | |  _         Advanced Analytics Engine for NoSQL Data
 * |  __/  |  _ <  | |___  | |___  |/ _| | | |_| |        Copyright (C) 2010 - 2013 SlamData, Inc.
 * |_|     |_| \_\ |_____|  \____|   /__/   \____|        All Rights Reserved.
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the 
 * GNU Affero General Public License as published by the Free Software Foundation, either version 
 * 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; 
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See 
 * the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this 
 * program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.precog
package daze

import bytecode.RandomLibrary
import org.specs2.mutable._

object CrossOrderingSpecs extends Specification with CrossOrdering with RandomLibrary {
  import instructions._
  import dag._
  
  "cross ordering" should {
    "order in the appropriate direction when one side is singleton" >> {
      "left" >> {
        val line = Line(0, "")
        
        val left = dag.LoadLocal(line, Root(line, PushString("/foo")))
        val right = Root(line, PushNum("42"))
        
        val input = Join(line, Map2Cross(Eq), left, right)
        val expected = Join(line, Map2CrossLeft(Eq), left, right)
        
        orderCrosses(input) mustEqual expected
      }
      
      "right" >> {
        val line = Line(0, "")
        
        val left = Root(line, PushNum("42"))
        val right = dag.LoadLocal(line, Root(line, PushString("/foo")))
        
        val input = Join(line, Map2Cross(Eq), left, right)
        val expected = Join(line, Map2CrossRight(Eq), left, right)
        
        orderCrosses(input) mustEqual expected
      }
    }
    
    "refrain from sorting when sets are already aligned in match" in {
      val line = Line(0, "")
      
      val left = dag.LoadLocal(line, Root(line, PushString("/foo")))
      val right = Root(line, PushNum("42"))
      
      val input = Join(line, Map2Match(Or), Join(line, Map2Cross(Eq), left, right), left)
      val expected = Join(line, Map2Match(Or), Join(line, Map2CrossLeft(Eq), left, right), left)
      
      orderCrosses(input) mustEqual expected
    }
    
    "refrain from sorting when sets are already aligned in filter" in {
      val line = Line(0, "")
      
      val left = dag.LoadLocal(line, Root(line, PushString("/foo")))
      val right = Root(line, PushNum("42"))
      
      val input = Filter(line, None, Join(line, Map2Cross(Eq), left, right), left)
      val expected = Filter(line, None, Join(line, Map2CrossLeft(Eq), left, right), left)
      
      orderCrosses(input) mustEqual expected
    }
    
    // this will eventually be a re-order cross test case
    "insert sorts for match on out-of-order operand set" >> {
      "left" >> {
        val line = Line(0, "")
        
        val left = dag.LoadLocal(line, Root(line, PushString("/foo")))
        val right = Join(line, Map2CrossRight(Add),
          left,
          dag.LoadLocal(line, Root(line, PushString("/bar"))))
        
        val input = Join(line, Map2Match(Or), left, right)
        val expected = Join(line, Map2Match(Or), left, Sort(right, Vector(1)))
        
        orderCrosses(input) mustEqual expected
      }
      
      "right" >> {
        val line = Line(0, "")
        
        val right = dag.LoadLocal(line, Root(line, PushString("/foo")))
        val left = Join(line, Map2CrossRight(Add),
          right,
          dag.LoadLocal(line, Root(line, PushString("/bar"))))
        
        val input = Join(line, Map2Match(Or), left, right)
        val expected = Join(line, Map2Match(Or), Sort(left, Vector(1)), right)
        
        orderCrosses(input) mustEqual expected
      }
      
      "both" >> {
        val line = Line(0, "")
        
        val foo = dag.LoadLocal(line, Root(line, PushString("/foo")))
        val bar = dag.LoadLocal(line, Root(line, PushString("/bar")))
        val baz = dag.LoadLocal(line, Root(line, PushString("/baz")))
        
        val left = Join(line, Map2CrossRight(Add), foo, bar)
        val right = Join(line, Map2CrossRight(Add), foo, baz)
        
        val input = Join(line, Map2Match(Or), left, right)
        val expected = Join(line, Map2Match(Or), Sort(left, Vector(1)), Sort(right, Vector(1)))
        
        orderCrosses(input) mustEqual expected
      }
      
      "random-case-without-a-label" >> {
        val line = Line(0, "")
        
        val numbers = dag.LoadLocal(line, Root(line, PushString("/hom/numbers")))
        val numbers3 = dag.LoadLocal(line, Root(line, PushString("/hom/numbers3")))
        
        val input = Join(line, Map2Match(And),
          Join(line, Map2Cross(And),
            Join(line, Map2Match(Eq), numbers, numbers),
            Join(line, Map2Match(Eq), numbers3, numbers3)),
          Join(line, Map2Match(Eq), numbers3, numbers3))
        
        val expected = Join(line, Map2Match(And),
          Sort(
            Join(line, Map2CrossLeft(And),
              Join(line, Map2Match(Eq), numbers, numbers),
              Memoize(Join(line, Map2Match(Eq), numbers3, numbers3), 100)),
            Vector(1)),
          Join(line, Map2Match(Eq), numbers3, numbers3))
            
        orderCrosses(input) mustEqual expected
      }
    }
    
    // this will eventually be a re-order cross test case
    "insert sorts for filter on out-of-order operand set" >> {
      "left" >> {
        val line = Line(0, "")
        
        val left = dag.LoadLocal(line, Root(line, PushString("/foo")))
        val right = Join(line, Map2CrossRight(Add),
          left,
          dag.LoadLocal(line, Root(line, PushString("/bar"))))
        
        val input = Filter(line, None, left, right)
        val expected = Filter(line, None, left, Sort(right, Vector(1)))
        
        orderCrosses(input) mustEqual expected
      }
      
      "right" >> {
        val line = Line(0, "")
        
        val right = dag.LoadLocal(line, Root(line, PushString("/foo")))
        val left = Join(line, Map2CrossRight(Add),
          right,
          dag.LoadLocal(line, Root(line, PushString("/bar"))))
        
        val input = Filter(line, None, left, right)
        val expected = Filter(line, None, Sort(left, Vector(1)), right)
        
        orderCrosses(input) mustEqual expected
      }
      
      "both" >> {
        val line = Line(0, "")
        
        val foo = dag.LoadLocal(line, Root(line, PushString("/foo")))
        val bar = dag.LoadLocal(line, Root(line, PushString("/bar")))
        val baz = dag.LoadLocal(line, Root(line, PushString("/baz")))
        
        val left = Join(line, Map2CrossRight(Add), foo, bar)
        val right = Join(line, Map2CrossRight(Add), foo, baz)
        
        val input = Filter(line, None, left, right)
        val expected = Filter(line, None, Sort(left, Vector(1)), Sort(right, Vector(1)))
        
        orderCrosses(input) mustEqual expected
      }
    }
  }
}
