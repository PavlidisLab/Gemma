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
package ubic.gemma.core.analysis.preprocess.batcheffects;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;

/**
 * Represents a summary of a batch effect confound.
 *
 * @author paul
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Used in frontend
public class BatchConfoundValueObject {

    private final double chiSquare;
    private final int df;
    private final BioAssaySet ee;
    private final ExperimentalFactor ef;
    private final double p;
    private final int numBatches;

    public BatchConfoundValueObject( BioAssaySet ee, ExperimentalFactor ef, double chiSquare, int df, double p,
            int numBatches ) {
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

    public BioAssaySet getEe() {
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
        String name = null;
        if ( ee instanceof ExpressionExperimentSubSet ) {
            name = ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment().getShortName();
        } else {
            name = " Subset " + ee.getName() + " of " + ( ( ExpressionExperiment ) ee ).getShortName();
        }
        return ee.getId() + "\t" + name + "\t" + ef.getId() + "\t" + ( ef.getCategory() != null ? ef.getCategory().getCategory() : ef.getName() ) + "\t"
                + String.format( "%.2f", chiSquare ) + "\t" + df + "\t" + String.format( "%.2g", p ) + "\t"
                + numBatches;
    }

}
