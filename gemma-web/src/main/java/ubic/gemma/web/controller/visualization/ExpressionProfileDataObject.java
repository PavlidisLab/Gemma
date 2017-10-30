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
package ubic.gemma.web.controller.visualization;

import java.util.Collection;

import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;

/**
 * A lightweight object to hold expression data for a single probe.
 * 
 * @see DoubleVectorValueObject for a similar object.
 * @author Paul
 *
 */
public class ExpressionProfileDataObject {

    private double[] data;

    private Long dedvId;

    private Long probeId;

    private String probeName;

    private Long eeId;

    private Collection<Long> geneIds;

    private boolean isNormalized;

    public ExpressionProfileDataObject( DoubleVectorValueObject data ) {
        this.data = data.getData();
        this.eeId = data.getExpressionExperiment().getId();
        this.probeId = data.getDesignElement().getId();
        this.probeName = data.getDesignElement().getName();
        this.dedvId = data.getId();
    }

    public double[] getData() {
        return data;
    }

    public Long getDedvId() {
        return dedvId;
    }

    public Long getEeId() {
        return eeId;
    }

    public Collection<Long> getGeneIds() {
        return geneIds;
    }

    public Long getProbeId() {
        return probeId;
    }

    public String getProbeName() {
        return probeName;
    }

    public boolean isNormalized() {
        return isNormalized;
    }

    public void setData( double[] data ) {
        this.data = data;
    }

    public void setDedvId( Long dedvId ) {
        this.dedvId = dedvId;
    }

    public void setEeId( Long eeId ) {
        this.eeId = eeId;
    }

    public void setGeneIds( Collection<Long> geneIds ) {
        this.geneIds = geneIds;
    }

    public void setNormalized( boolean isNormalized ) {
        this.isNormalized = isNormalized;
    }

    public void setProbeId( Long probeId ) {
        this.probeId = probeId;
    }

    public void setProbeName( String probeName ) {
        this.probeName = probeName;
    }

}
