/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Represents a summary of a batch effect confound.
 * 
 * @author paul
 * @version $Id$
 */
public class BatchConfoundValueObject {

    private final double chiSquare;
    private final int df;
    private final ExpressionExperiment ee;
    private final ExperimentalFactor ef;
    private final double p;
    private int numBatches;

    /**
     * @param ee
     * @param ef
     * @param chiSquare
     * @param df
     * @param p
     * @param numBatches
     */
    public BatchConfoundValueObject( ExpressionExperiment ee, ExperimentalFactor ef, double chiSquare, int df,
            double p, int numBatches ) {
        this.ee = ee;
        this.ef = ef;
        this.chiSquare = chiSquare;
        this.df = df;
        this.p = p;
        this.numBatches = numBatches;
    }

    public double getChiSquare() {
        return chiSquare;
    }

    public int getDf() {
        return df;
    }

    public ExpressionExperiment getEe() {
        return ee;
    }

    public ExperimentalFactor getEf() {
        return ef;
    }

    public int getNumBatches() {
        return numBatches;
    }

    public double getP() {
        return p;
    }

    @Override
    public String toString() {
        return ee.getId() + "\t" + ee.getShortName() + "\t" + ef.getId() + "\t" + ef.getCategory().getCategory() + "\t"
                + String.format( "%.2f", chiSquare ) + "\t" + df + "\t" + String.format( "%.2g", p ) + "\t"
                + numBatches;
    }

}
