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

package cc.factorie.app.classify

import cc.factorie._
import cc.factorie.optimize._
import la.{Tensor1, Tensor}

class MaxEntSampleRankTrainer(val optimizer:GradientOptimizer = new MIRA) extends ClassifierTrainer {
  var iterations = 10
  var learningRateDecay = 0.9
  def train[L <: LabeledMutableDiscreteVar[_], F <: DiscreteTensorVar](il: LabelList[L, F]): Classifier[L] = {
    val cmodel = new LogLinearModel(il.labelToFeatures, il.labelDomain, il.instanceDomain)(il.labelManifest, il.featureManifest)
    val sampler = new GibbsSampler(cmodel, HammingObjective) {
      override def pickProposal(proposals: Seq[Proposal]): Proposal = proposals.head // which proposal is picked is irrelevant, so make it quick
    }
    val learner = new SampleRankTrainer(sampler, optimizer)
    learner.processContexts(il, iterations)
    new ModelBasedClassifier[L](cmodel, il.head.domain)
  }
}

class MaxEntLikelihoodTrainer(val variance: Double = 10.0, val warmStart: Tensor = null) extends ClassifierTrainer {
  def train[L <: LabeledMutableDiscreteVar[_], F <: DiscreteTensorVar](il: LabelList[L, F]): Classifier[L] = {
    val cmodel = new LogLinearModel(il.labelToFeatures, il.labelDomain, il.instanceDomain)(il.labelManifest, il.featureManifest)
    val pieces = il.map(l => new GLMExample(
      il.labelToFeatures(l).tensor.asInstanceOf[Tensor1],
      l.intValue,
      ObjectiveFunctions.logMultiClassObjective,
      weight = il.instanceWeight(l)))
    if (warmStart != null) cmodel.evidenceTemplate.weights := warmStart
    // Do the training by BFGS
    val lbfgs = new optimize.LBFGS with L2Regularization { variance = variance }
    val strategy = new BatchTrainer(cmodel, lbfgs)
    while (!strategy.isConverged)
      strategy.processExamples(pieces)
    new ModelBasedClassifier[L](cmodel, il.head.domain)
  }
}

// The default trainer
class MaxEntTrainer extends MaxEntLikelihoodTrainer()

class GeneralClassifierTrainer[L <: LabeledMutableDiscreteVar[_], F <: DiscreteTensorVar](val trainer: Trainer[LogLinearModel[L,F]], val loss: ObjectiveFunctions.MultiClassObjectiveFunction) extends ClassifierTrainer {
  def train[L <: LabeledMutableDiscreteVar[_], F <: DiscreteTensorVar](il: LabelList[L, F]) = {
    val cmodel = new LogLinearModel(il.labelToFeatures, il.labelDomain, il.instanceDomain)(il.labelManifest, il.featureManifest)
    val examples = il.map(l => new GLMExample(
      il.labelToFeatures(l).tensor.asInstanceOf[Tensor1],
      l.intValue,
      ObjectiveFunctions.logMultiClassObjective,
      weight = il.instanceWeight(l)))
    while (!trainer.isConverged)
      trainer.processExamples(examples)
    new ModelBasedClassifier[L](cmodel, il.head.domain)
  }
}