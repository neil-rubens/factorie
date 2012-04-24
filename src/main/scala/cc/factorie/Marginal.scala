/* Copyright (C) 2008-2010 University of Massachusetts Amherst,
   Department of Computer Science.
   This file is part of "FACTORIE" (Factor graphs, Imperative, Extensible)
   http://factorie.cs.umass.edu, http://code.google.com/p/factorie/
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
    http://www.apache.org/licenses/LICENSE-2.0
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. */

package cc.factorie
import cc.factorie.la._

trait Marginal {
  def variables: Seq[Variable]
  def setToMaximize(implicit d:DiffList): Unit
}

trait DiscreteMarginal extends Marginal {
  def _1: DiscreteVectorVar
  def variables: Seq[DiscreteVectorVar]
  def proportions: Proportions
}
//trait DiscreteMar1[V1<:DiscreteVectorVar] extends DiscreteMar { def _1: V1; def proportions: Proportions1 }
class DiscreteMarginal1[V1<:DiscreteVectorVar](val _1:V1, proportions1:Proportions1 = null) extends DiscreteMarginal {
  def this(f:Factor1[V1]) = this (f._1, null)
  def variables = Seq(_1)
  val proportions = if (proportions1 eq null) new DenseProportions1(_1.domain.dimensionDomain.size) else proportions1 // must do this here because no access to _1 in default argument values
  def incrementCurrentValue(w:Double): Unit = _1 match { case d:DiscreteVar => proportions.+=(d.intValue, w); case d:DiscreteVectorVar => throw new Error("Not yet implemented") }
  def setToMaximize(implicit d:DiffList): Unit = _1 match { case v:DiscreteVariable => v.set(proportions1.maxIndex); case _ => throw new Error }
}
class DiscreteMarginal2[V1<:DiscreteVectorVar,V2<:DiscreteVectorVar](val _1:V1, val _2:V2, proportions2:Proportions2 = null) extends DiscreteMarginal {
  def this(f:Factor2[V1,V2]) = this (f._1, f._2, null)
  def variables = Seq(_1, _2)
  val proportions = if (proportions2 eq null) new DenseProportions2(_1.domain.dimensionDomain.size, _2.domain.dimensionDomain.size) else proportions2 // must do this here because no access to _1 in default argument values
  def incrementCurrentValue(w:Double): Unit = (_1,_2) match { case (d1:DiscreteVar,d2:DiscreteVar) => proportions.+=(d1.intValue, d2.intValue, w); case d:DiscreteVectorVar => throw new Error("Not yet implemented") }
  def setToMaximize(implicit d:DiffList): Unit = throw new Error("Not yet implemented")
}
class DiscreteMarginal3[V1<:DiscreteVectorVar,V2<:DiscreteVectorVar,V3<:DiscreteVectorVar](val _1:V1, val _2:V2, val _3:V3, proportions3:Proportions3 = null) extends DiscreteMarginal {
  def this(f:Factor3[V1,V2,V3]) = this (f._1, f._2, f._3, null)
  def variables = Seq(_1, _2, _3)
  val proportions: Proportions3 = if (proportions3 eq null) new DenseProportions3(_1.domain.dimensionDomain.size, _2.domain.dimensionDomain.size, _3.domain.dimensionDomain.size) else proportions3 // must do this here because no access to _1 in default argument values
  def incrementCurrentValue(w:Double): Unit = (_1,_2,_3) match { case (d1:DiscreteVar,d2:DiscreteVar,d3:DiscreteVar) => proportions.+=(d1.intValue, d2.intValue, d3.intValue, w); case d:DiscreteVectorVar => throw new Error("Not yet implemented") }
  def setToMaximize(implicit d:DiffList): Unit = throw new Error("Not yet implemented")
}
class DiscreteMarginal4[V1<:DiscreteVectorVar,V2<:DiscreteVectorVar,V3<:DiscreteVectorVar,V4<:DiscreteVectorVar](val _1:V1, val _2:V2, val _3:V3, val _4:V4, proportions4:Proportions4 = null) extends DiscreteMarginal {
  def this(f:Factor4[V1,V2,V3,V4]) = this (f._1, f._2, f._3, f._4, null)
  def variables = Seq(_1, _2, _3, _4)
  val proportions: Proportions4 = if (proportions4 eq null) new DenseProportions4(_1.domain.dimensionDomain.size, _2.domain.dimensionDomain.size, _3.domain.dimensionDomain.size, _4.domain.dimensionDomain.size) else proportions4 // must do this here because no access to _1 in default argument values
  def incrementCurrentValue(w:Double): Unit = (_1,_2,_3,_4) match { case (d1:DiscreteVar,d2:DiscreteVar,d3:DiscreteVar,d4:DiscreteVar) => proportions.+=(d1.intValue, d2.intValue, d3.intValue, d4.intValue, w); case d:DiscreteVectorVar => throw new Error("Not yet implemented") }
  def setToMaximize(implicit d:DiffList): Unit = throw new Error("Not yet implemented")
}


object DiscreteMarginal {
  def apply[V1<:DiscreteVectorVar](f:Factor1[V1]): DiscreteMarginal1[V1] = new DiscreteMarginal1(f)
  def apply[V1<:DiscreteVectorVar,V2<:DiscreteVectorVar](f:Factor2[V1,V2]): DiscreteMarginal2[V1,V2] = new DiscreteMarginal2(f)
  def apply[V1<:DiscreteVectorVar,V2<:DiscreteVectorVar,V3<:DiscreteVectorVar](f:Factor3[V1,V2,V3]): DiscreteMarginal3[V1,V2,V3] = new DiscreteMarginal3(f)
  def apply[V1<:DiscreteVectorVar,V2<:DiscreteVectorVar,V3<:DiscreteVectorVar,V4<:DiscreteVectorVar](f:Factor4[V1,V2,V3,V4]): DiscreteMarginal4[V1,V2,V3,V4] = new DiscreteMarginal4(f)
  def apply(f:Factor): DiscreteMarginal = f match {
    case f:Factor1[DiscreteVectorVar] => apply(f)
    case f:Factor2[DiscreteVectorVar,DiscreteVectorVar] => apply(f)
    case f:Factor3[DiscreteVectorVar,DiscreteVectorVar,DiscreteVectorVar] => apply(f)
    case f:Factor4[DiscreteVectorVar,DiscreteVectorVar,DiscreteVectorVar,DiscreteVectorVar] => apply(f)
  }
  def apply(f:Factor, p:Proportions): DiscreteMarginal = f match {
    case f:Factor1[DiscreteVectorVar] => new DiscreteMarginal1(f._1, p.asInstanceOf[Proportions1])
    case f:Factor2[DiscreteVectorVar,DiscreteVectorVar] => new DiscreteMarginal2(f._1, f._2, p.asInstanceOf[Proportions2])
    case f:Factor3[DiscreteVectorVar,DiscreteVectorVar,DiscreteVectorVar] => new DiscreteMarginal3(f._1, f._2, f._3, p.asInstanceOf[Proportions3])
    case f:Factor4[DiscreteVectorVar,DiscreteVectorVar,DiscreteVectorVar,DiscreteVectorVar] => new DiscreteMarginal4(f._1, f._2, f._3, f._4, p.asInstanceOf[Proportions4])
  }
}


// Gaussian Marginal

class GaussianRealMarginal1[V1<:RealVar](val _1:V1) extends Marginal {
  def variables = Seq(_1)
  // TODO Set this up better for incremental estimation
  var mean = 0.0
  var variance = 1.0
  def pr(x:Double): Double = cc.factorie.generative.Gaussian.pr(x, mean, variance)
  def setToMaximize(implicit d:DiffList): Unit = _1 match { case v:RealVariable => v.set(mean) }
}


