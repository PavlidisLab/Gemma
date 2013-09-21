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
package ubic.gemma.model.association.coexpression;

import java.io.Serializable;

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.experiment.BioAssaySet;

/**
 * Represents the correlation of two datavectors.
 */
public abstract class Probe2ProbeCoexpression implements Serializable {

    final private BioAssaySet expressionBioAssaySet = null;

    final private ProcessedExpressionDataVector firstVector = null;

    final private Long id = null;

    final private Double score = null;

    final private ProcessedExpressionDataVector secondVector = null;

    final private Analysis sourceAnalysis = null;

    /**
     * 
     */
    public BioAssaySet getExpressionBioAssaySet() {
        return this.expressionBioAssaySet;
    }

    /**
     * 
     */
    public ProcessedExpressionDataVector getFirstVector() {
        return this.firstVector;
    }

    public Long getId() {
        return id;
    }

    /**
     * 
     */
    public Double getScore() {
        return this.score;
    }

    /**
     * 
     */
    public ProcessedExpressionDataVector getSecondVector() {
        return this.secondVector;
    }

    public Analysis getSourceAnalysis() {
        return sourceAnalysis;
    }

}