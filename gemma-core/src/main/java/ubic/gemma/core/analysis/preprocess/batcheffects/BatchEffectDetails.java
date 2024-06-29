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

import org.springframework.util.Assert;
import ubic.gemma.core.lang.Nullable;
import ubic.gemma.model.common.auditAndSecurity.eventType.*;

/**
 * provide some basic information about the properties and strength of a batch effect, if any.
 *
 * @author Paul
 */
public class BatchEffectDetails {

    public class BatchEffectStatistics {

        private BatchEffectStatistics() {

        }

        /**
         * A PCA component that is explained by the batch factor. It is 1-based.
         */
        public int getComponent() {
            return component;
        }

        /**
         * The variance explained by the component.
         */
        public double getComponentVarianceProportion() {
            return componentVarianceProportion;
        }

        /**
         * A P-value statistic for that component.
         */
        public double getPvalue() {
            return pvalue;
        }
    }

    private final boolean hasBatchInformation;
    private final boolean hasUninformativeBatchInformation;
    private final boolean hasProblematicBatchInformation;
    private final boolean hasSingletonBatches;
    private final boolean singleBatch;
    private final boolean dataWasBatchCorrected;

    /* if present and suitable, those are filled */
    private boolean hasBatchEffectStatistics = false;
    private double pvalue;
    private int component;
    private double componentVarianceProportion;

    public BatchEffectDetails( @Nullable BatchInformationEvent infoEvent, boolean dataWasBatchCorrected, boolean singleBatch ) {
        if ( infoEvent instanceof BatchInformationFetchingEvent ) {
            this.hasBatchInformation = true;
            // FIXME hasProblematicBatchInformation should not be assigned when there is no batch information available.
            this.hasProblematicBatchInformation = infoEvent instanceof FailedBatchInformationFetchingEvent;
            this.hasSingletonBatches = infoEvent instanceof SingletonBatchInvalidEvent;
            this.hasUninformativeBatchInformation = infoEvent instanceof UninformativeFASTQHeadersForBatchingEvent;
            this.dataWasBatchCorrected = dataWasBatchCorrected;
            this.singleBatch = singleBatch;
        } else {
            // infoEvent is either null or a BatchInformationMissingEvent
            Assert.isTrue( infoEvent == null || infoEvent instanceof BatchInformationMissingEvent );
            this.hasBatchInformation = false;
            this.hasProblematicBatchInformation = false;
            this.hasSingletonBatches = false;
            this.hasUninformativeBatchInformation = false;
            this.dataWasBatchCorrected = false;
            this.singleBatch = false;
        }
    }

    /**
     * Indicate if the batch information is present.
     */
    public boolean hasBatchInformation() {
        return hasBatchInformation;
    }

    /**
     * Indicate if the batch information is present, but problematic.
     */
    public boolean hasProblematicBatchInformation() {
        return hasProblematicBatchInformation;
    }

    /**
     * Indicate if the batch information is present, but uninformative.
     */
    public boolean hasUninformativeBatchInformation() {
        return hasUninformativeBatchInformation;
    }

    /**
     * Indicate if the dataset has one or more singleton batches (i.e. a batch with only one sample).
     */
    public boolean hasSingletonBatches() {
        return hasSingletonBatches;
    }

    /**
     * Indicate if the experiment was determined to have just one batch, or false for any other state (including we don't know).
     */
    public boolean isSingleBatch() {
        return singleBatch;
    }

    /**
     * Indicate if batch correction was performed on the expression data.
     */
    public boolean dataWasBatchCorrected() {
        return this.dataWasBatchCorrected;
    }

    /**
     * Obtain an object describing the batch effect if available.
     */
    @Nullable
    public BatchEffectStatistics getBatchEffectStatistics() {
        if ( hasBatchEffectStatistics ) {
            return new BatchEffectStatistics();
        } else {
            return null;
        }
    }

    /**
     * Set the batch effect statistics.
     * @param pVal     P-value
     * @param i        component connfounded by the bat
     * @param variance variance explained by the component
     */
    public void setBatchEffectStatistics( double pVal, int i, double variance ) {
        Assert.isTrue( pVal >= 0 );
        Assert.isTrue( pVal <= 1 );
        Assert.isTrue( i >= 1 );
        Assert.isTrue( variance >= 0 );
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
