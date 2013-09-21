/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2012 University of British Columbia
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

/**
 * Represents the processed data that is used for actual analyses. The vectors in this class would have been masked to
 * remove missing values.
 */
public abstract class ProcessedExpressionDataVector extends
        ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorImpl {

    /**
     * Constructs new instances of {@link ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector}.
     */
    public static final class Factory {
        /**
         * Constructs a new instance of {@link ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector}.
         */
        public static ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector newInstance() {
            return new ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorImpl();
        }

    }

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 7802069398062779337L;
    private Double rankByMean;

    private Double rankByMax;

    private ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public ProcessedExpressionDataVector() {
    }

    /**
     * 
     */
    @Override
    public ubic.gemma.model.expression.experiment.ExpressionExperiment getExpressionExperiment() {
        return this.expressionExperiment;
    }

    /**
     * The relative expression level of this vector in the study. Used as a quick-and-dirty way to provide feedback
     * about the expession level without referring to any absolute baseline other than the minimum in the entire
     * dataset, based on the maximum expression measurement for the probe (so the probe with the lowest expression is
     * the one with the lowest maximum value). For two-color data sets, this is computed using the intensity values for
     * the probe in the two channels, not from the ratios stored in this vector. For one-color data sets, this is
     * computed directly from the intensity levels in this vector.
     */
    public Double getRankByMax() {
        return this.rankByMax;
    }

    /**
     * The relative expression level of this vector in the study. Used as a quick-and-dirty way to provide feedback
     * about the expession level without referring to any absolute baseline other than the minimum in the entire
     * dataset, based on the mean expression measurement for the probe. For two-color data sets, this is computed using
     * the intensity values for the probe in the two channels, not from the ratios stored in this vector. For one-color
     * data sets, this is computed directly from the intensity levels in this vector.
     */
    public Double getRankByMean() {
        return this.rankByMean;
    }

    @Override
    public void setExpressionExperiment(
            ubic.gemma.model.expression.experiment.ExpressionExperiment expressionExperiment ) {
        this.expressionExperiment = expressionExperiment;
    }

    public void setRankByMax( Double rankByMax ) {
        this.rankByMax = rankByMax;
    }

    public void setRankByMean( Double rankByMean ) {
        this.rankByMean = rankByMean;
    }

}