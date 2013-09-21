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

/**
 * Represents the correlation of two datavectors.
 */
public abstract class Probe2ProbeCoexpression extends ubic.gemma.model.association.Relationship {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = 5767420332087241608L;

    private Double score;
    private Double pvalue;

    private ubic.gemma.model.expression.experiment.BioAssaySet expressionBioAssaySet;

    private ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector secondVector;

    private ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector firstVector;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Probe2ProbeCoexpression() {
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.experiment.BioAssaySet getExpressionBioAssaySet() {
        return this.expressionBioAssaySet;
    }

    /**
     * 
     */
    public ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector getFirstVector() {
        return this.firstVector;
    }

    /**
     * 
     */
    public Double getPvalue() {
        return this.pvalue;
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
    public ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector getSecondVector() {
        return this.secondVector;
    }

    public void setExpressionBioAssaySet( ubic.gemma.model.expression.experiment.BioAssaySet expressionBioAssaySet ) {
        this.expressionBioAssaySet = expressionBioAssaySet;
    }

    public void setFirstVector( ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector firstVector ) {
        this.firstVector = firstVector;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    public void setSecondVector( ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector secondVector ) {
        this.secondVector = secondVector;
    }

}