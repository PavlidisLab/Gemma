/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package ubic.gemma.model.expression.bioAssayData;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;

/**
 * @author Paul
 * @version $Id$
 */
public interface ProcessedExpressionDataVectorDao {

    public enum RankMethod {
        mean, max
    }

    public Map<ExpressionExperiment, Collection<DoubleVectorValueObject>> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment );

    public Collection<DoubleVectorValueObject> getProcessedDataArrays( ExpressionExperiment expressionExperiment,
            Collection<Gene> genes );

    public Map<Gene, Collection<Double>> getRanks( ExpressionExperiment expressionExperiment, Collection<Gene> genes,
            RankMethod method );

    public Map<DesignElement, Double> getRanks( ExpressionExperiment expressionExperiment, RankMethod method );

    public Map<ExpressionExperiment, Map<Gene, Collection<Double>>> getRanks(
            Collection<ExpressionExperiment> expressionExperiments, Collection<Gene> genes, RankMethod method );

    /**
     * Populate the processed data for the given experiment. For two-channel studies, the missing value information
     * should already have been computed. If the values already exist, they will be re-written.
     * 
     * @param expressionExperiment
     */
    public Collection<ProcessedExpressionDataVector> createProcessedDataVectors(
            ExpressionExperiment expressionExperiment );

    /**
     * 
     */
    public void thaw( java.util.Collection designElementDataVectors );

    /**
     * 
     */
    public void update( java.util.Collection designElementDataVectors );

    /**
     * @param expressionExperiment
     * @return Processed data for the given experiment. NOTE the vectors are thawed before returning.
     */
    public Collection<ProcessedExpressionDataVector> getProcessedVectors( ExpressionExperiment expressionExperiment );

}
