/*
 * The gemma project
 *
 * Copyright (c) 2013 University of British Columbia
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

import ubic.gemma.model.common.auditAndSecurity.eventType.BatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.FailedBatchInformationFetchingEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.SingletonBatchInvalidEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.UninformativeFASTQHeadersForBatchingEvent;

import javax.annotation.Nullable;

/**
 * provide some basic information about the properties and strength of a batch effect, if any.
 *
 * @author Paul
 */
public class BatchEffectDetails {

    public class BatchEffectStatistics {

        private BatchEffectStatistics() {

        }

        public int getComponent() {
            return component;
        }

        public double getComponentVarianceProportion() {
            return componentVarianceProportion;
        }

        public double getPvalue() {
            return pvalue;
        }
    }

    /**
     * Indicate if the batch information is present.
     */
    private final boolean hasBatchInformation;
    /**
     * Indicate if the batch information is problematic.
     */
    private final boolean hasProblematicBatchInformation;
    /**
     * Indicate if the dataset has singleton batches (i.e. a batch only one sample).
     */
    private final boolean hadSingletonBatches;
    /**
     * Indicate if the batch information is uninformative.
     */
    private final boolean hadUninformativeHeaders;
    /**
     * Indicate if batch correction was performed on the expression data.
     */
    private final boolean dataWasBatchCorrected;

    private final boolean singleBatch;

    /* if present and suitable, those are filled */
    private boolean hasBatchEffectStatistics = false;
    private double pvalue;
    private int component;
    private double componentVarianceProportion;

    public BatchEffectDetails( @Nullable BatchInformationFetchingEvent infoEvent, boolean dataWasBatchCorrected, boolean singleBatch ) {
        this.hasBatchInformation = infoEvent != null;
        if ( infoEvent != null ) {
            this.hasProblematicBatchInformation = FailedBatchInformationFetchingEvent.class.isAssignableFrom( ( infoEvent.getClass() ) );
            this.hadSingletonBatches = SingletonBatchInvalidEvent.class.isAssignableFrom( infoEvent.getClass() );
            this.hadUninformativeHeaders = UninformativeFASTQHeadersForBatchingEvent.class.isAssignableFrom( infoEvent.getClass() );
        } else {
            this.hasProblematicBatchInformation = false;
            this.hadSingletonBatches = false;
            this.hadUninformativeHeaders = false;
        }
        this.dataWasBatchCorrected = dataWasBatchCorrected;
        this.singleBatch = singleBatch;
        this.pvalue = 1.0;
    }

    public boolean getDataWasBatchCorrected() {
        return this.dataWasBatchCorrected;
    }

    public boolean getHadSingletonBatches() {
        return hadSingletonBatches;
    }

    public boolean getHadUninformativeHeaders() {
        return hadUninformativeHeaders;
    }

    public boolean hasBatchInformation() {
        return hasBatchInformation;
    }

    public boolean hasProblematicBatchInformation() {
        return hasProblematicBatchInformation;
    }

    /**
     *
     * @return true if the experiment was determined to have just one batch, or false for any other state (including we
     *         don't know)
     */
    public boolean isSingleBatch() {
        return singleBatch;
    }

    @Nullable
    public BatchEffectStatistics getBatchEffectStatistics() {
        if ( hasBatchEffectStatistics ) {
            return new BatchEffectStatistics();
        } else {
            return null;
        }
    }

    public void setBatchEffectStatistics( double pVal, int i, double variance ) {
        this.hasBatchEffectStatistics = true;
        this.pvalue = pVal;
        this.component = i;
        this.componentVarianceProportion = variance;
    }

    @Override
    public String toString() {
        if ( hasBatchEffectStatistics ) {
            return String.format( "BatchEffectDetails [pvalue=%.2g, component=%d, varFraction=%.2f]", pvalue, component,
                    componentVarianceProportion );
        } else {
            return "BatchEffectDetails";
        }
    }
}
