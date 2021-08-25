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

import ubic.gemma.model.common.auditAndSecurity.eventType.*;

/**
 * provide some basic information about the properties and strength of a batch effect, if any.
 *
 * @author Paul
 */
public class BatchEffectDetails {

    private Integer component = null;

    private double componentVarianceProportion;
    private final boolean dataWasBatchCorrected;
    private boolean failedToGetBatchInformation = false;
    private Boolean hadSingletonBatches = false;
    private Boolean hadUninformativeHeaders = false;
    private final boolean hasBatchInformation;
    private double pvalue;

    private final boolean singleBatch;

    public BatchEffectDetails( BatchInformationFetchingEvent infoEvent, boolean dataWasBatchCorrected, boolean singleBatch ) {

        if ( infoEvent == null ) {
            this.hasBatchInformation = false;
        } else {
            if ( SingletonBatchInvalidEvent.class.isAssignableFrom( infoEvent.getClass() ) ) {
                this.hasBatchInformation = false;
                this.hadSingletonBatches = true;
            } else if ( UninformativeFASTQHeadersForBatchingEvent.class.isAssignableFrom( infoEvent.getClass() ) ) {
                this.hasBatchInformation = false;
                this.hadUninformativeHeaders = true;
            } else if ( FailedBatchInformationMissingEvent.class.isAssignableFrom( infoEvent.getClass() ) ) {
                this.hasBatchInformation = false;
                this.failedToGetBatchInformation = true;
            } else if ( FailedBatchInformationFetchingEvent.class.isAssignableFrom( (infoEvent.getClass()) ) ) {
                this.hasBatchInformation = false;
                this.failedToGetBatchInformation = true;
            } else {
                this.hasBatchInformation = true;
            }
        }

        this.dataWasBatchCorrected = dataWasBatchCorrected;
        this.singleBatch = singleBatch;
        this.pvalue = 1.0;
    }

    public Integer getComponent() {
        return component;
    }

    public double getComponentVarianceProportion() {
        return componentVarianceProportion;
    }

    public boolean getDataWasBatchCorrected() {
        return this.dataWasBatchCorrected;
    }

    public Boolean getHadSingletonBatches() {
        return hadSingletonBatches;
    }

    public Boolean getHadUninformativeHeaders() {
        return hadUninformativeHeaders;
    }

    public double getPvalue() {
        return pvalue;
    }

    public boolean hasNoBatchInfo() {
        return !hasBatchInformation;
    }

    public boolean isFailedToGetBatchInformation() {
        return failedToGetBatchInformation;
    }

    /**
     *
     * @return true if the experiment was determined to have just one batch, or false for any other state (including we
     *         don't know)
     */
    public boolean isSingleBatch() {
        return singleBatch;
    }

    public void setComponent( Integer component ) {
        this.component = component;
    }

    public void setComponentVarianceProportion( double componentVarianceProportion ) {
        this.componentVarianceProportion = componentVarianceProportion;
    }

    public void setPvalue( double pvalue ) {
        this.pvalue = pvalue;
    }

    @Override
    public String toString() {
        return String.format( "BatchEffectDetails [pvalue=%.2g, component=%d, varFraction=%.2f]", pvalue, component,
                componentVarianceProportion );
    }

}
