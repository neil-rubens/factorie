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
import cc.factorie.generative._

// BPInfer is a factory for BPInferencers
// BPInferencer is specific to a model and some variables

// Infer.apply() does the work and returns true on success; 
//  if the inference is complicated and may be incrementally, it may create a Inferencer 
// An Inferencer is specific to a model and some variables and may be run incrementally;
//  it may also contain a Lattice (which is usually a type of GenerativeModel?)

trait Lattice2 extends Model

trait Infer {
  /** Returns true on success, false if this recipe was unable to handle the relevant factors. */
  def apply(variables:Seq[Variable], varying:Seq[Variable], factors:Seq[Factor], qModel:Model): Lattice2 // Abstract implemented in subclasses
  def apply(variables:Seq[Variable], factors:Seq[Factor], qModel:Model): Lattice2 = apply(variables, Nil, factors, qModel)
  def apply(variables:Seq[Variable], factors:Seq[Factor]): Lattice2 = apply(variables, factors, null)
  def apply(variables:Seq[Variable], model:Model): Lattice2 = apply(variables, model.factors(variables))
  def apply(variables:Seq[Variable], model:Model, qModel:Model): Lattice2 = apply(variables, model.factors(variables), qModel)
}




class IndependentDiscreteLattice extends GenerativeFactorModel with Lattice2 {
  def marginal(d:DiscreteVariable): Proportions = this.parentFactor(d) match {
    case f:Discrete.Factor => f._2
  }
}

object InferIndependentDiscrete extends Infer {
  def array(d:DiscreteVariable, factors:Seq[Factor]): Array[Double] = {
    val distribution = new Array[Double](d.domain.size)
    val origValue = d.intValue
    for (i <- 0 until distribution.size) {
      d := i
      factors.foreach(f => distribution(i) += f.score)
    }
    maths.expNormalize(distribution)
    d := origValue
    distribution
  }
  def array(d:DiscreteVariable, model:Model): Array[Double] = array(d, model.factors1(d))
  def proportions(d:DiscreteVariable, model:Model): Proportions = new DenseProportions(array(d, model))
  def proportions(d:DiscreteVariable, factors:Seq[Factor]): Proportions = new DenseProportions(array(d, factors))
  def apply(d:DiscreteVariable, factors:Seq[Factor]): IndependentDiscreteLattice = {
    implicit val lattice = new IndependentDiscreteLattice
    d ~ Discrete(proportions(d, factors))
    lattice
  }
  def apply(variables:Seq[Variable], varying:Seq[Variable], factors:Seq[Factor], qModel:Model): IndependentDiscreteLattice = {
    if (varying.size > 0) return null
    if (qModel ne null) return null
    implicit val lattice = new IndependentDiscreteLattice
    for (v <- variables) v match {
      case d:DiscreteVariable => d ~ Discrete(proportions(d, factors))
      case _ => return null
    }
    lattice
  }
}


class DiscreteSamplingLattice(variables:Iterable[DiscreteVariable]) extends IndependentDiscreteLattice {
  for (d <- variables) d.~(Discrete(new DenseCountsProportions(d.domain.size)))(this)
  def proportions(d:DiscreteVariable): DenseCountsProportions = this.parentFactor(d) match {
    case df: Discrete.Factor => df._2.asInstanceOf[DenseCountsProportions]
  }
  def increment(d:DiscreteVariable): Unit = proportions(d).increment(d.intValue, 1.0)(null) 
}

class SamplingInferencer2[C](val sampler:Sampler[C]) {
  var burnIn = 100 // I really want these to be  the default-values for parameters to infer, in Scala 2.8.
  var thinning = 20
  var iterations = 500
  def infer(targets:Iterable[DiscreteVariable], contexts:Iterable[C], iterations:Int = 500, burnIn:Int = 100, thinning:Int = 20): DiscreteSamplingLattice = {
    val lattice = new DiscreteSamplingLattice(targets)
    sampler.processAll(contexts, burnIn)
    for (i <- 0 until iterations/thinning) {
      sampler.processAll(contexts, thinning)
      for (d <- targets) lattice.increment(d)
      //postSample(targets)
    }
    lattice
  }
}

object InferByGibbsSampling extends Infer {
  def apply(variables:Seq[DiscreteVariable], varying:Seq[Variable with IterableSettings], model:Model, qModel:Model): DiscreteSamplingLattice = {
    val inferencer = new SamplingInferencer2(new VariableSettingsSampler[Variable with IterableSettings](model, null))
    inferencer.infer(variables, varying)
  }
  def apply(variables:Seq[Variable], varying:Seq[Variable], factors:Seq[Factor], qModel:Model): Lattice2 = null
}
