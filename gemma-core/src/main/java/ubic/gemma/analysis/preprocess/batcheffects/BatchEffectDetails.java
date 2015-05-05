/*
 * The gemma-core project
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
package ubic.gemma.analysis.preprocess.batcheffects;

/**
 * provide some basic information about the strength of a batch effect.
 * 
 * @author Paul
 * @version $Id$
 */
public class BatchEffectDetails {

    private Integer component;

    private double componentVarianceProportion;

    private boolean hasBatchInformation = false;
     
    private boolean dataWasBatchCorrected = false;

    private double pvalue;

    public Integer getComponent() {
        return component;
    }

    public double getComponentVarianceProportion() {
        return componentVarianceProportion;
    }

    public boolean getDataWasBatchCorrected() {
        return this.dataWasBatchCorrected;
    }

    public double getPvalue() {
        return pvalue;
    }

    public void setComponent( Integer component ) {
        this.component = component;
    }

    public void setComponentVarianceProportion( double compomentVarianceProportion ) {
        this.componentVarianceProportion = compomentVarianceProportion;
    }

    /**
     * @param b
     */
    public void setDataWasBatchCorrected( boolean b ) {
        this.dataWasBatchCorrected = b;

    }

    public void setPvalue( double pvalue ) {
        this.pvalue = pvalue;
    }

    @Override
    public String toString() {
        return String.format( "BatchEffectDetails [pvalue=%.2g, component=%d, varFraction=%.2f]", pvalue, component,
                componentVarianceProportion );
    }

    /**
     * @return the hasBatchInformation
     */
    public boolean isHasBatchInformation() {
        return hasBatchInformation;
    }

    /**
     * @param hasBatchInformation the hasBatchInformation to set
     */
    public void setHasBatchInformation( boolean hasBatchInformation ) {
        this.hasBatchInformation = hasBatchInformation;
    }

}
