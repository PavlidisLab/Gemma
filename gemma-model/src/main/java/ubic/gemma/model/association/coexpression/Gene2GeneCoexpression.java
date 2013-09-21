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
 * 
 */
public abstract class Gene2GeneCoexpression extends ubic.gemma.model.association.Gene2GeneAssociation {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -6751728444824889104L;

    private Double pvalue;
    private Double effect;

    private Integer numDataSets;

    private byte[] datasetsTestedVector;

    private byte[] datasetsSupportingVector;

    private byte[] specificityVector;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public Gene2GeneCoexpression() {
    }

    /**
     * 
     */
    public byte[] getDatasetsSupportingVector() {
        return this.datasetsSupportingVector;
    }

    /**
     * 
     */
    public byte[] getDatasetsTestedVector() {
        return this.datasetsTestedVector;
    }

    /**
     * 
     */
    public Double getEffect() {
        return this.effect;
    }

    /**
     * 
     */
    public Integer getNumDataSets() {
        return this.numDataSets;
    }

    /**
     * 
     */
    public Double getPvalue() {
        return this.pvalue;
    }

    /**
     * A bit vector representing whether the experiments had probes specific to the query gene AND the target genes. A
     * '1' means it had specific probes; a '0' mean it did not.
     */
    public byte[] getSpecificityVector() {
        return this.specificityVector;
    }

    public void setDatasetsSupportingVector( byte[] datasetsSupportingVector ) {
        this.datasetsSupportingVector = datasetsSupportingVector;
    }

    public void setDatasetsTestedVector( byte[] datasetsTestedVector ) {
        this.datasetsTestedVector = datasetsTestedVector;
    }

    public void setEffect( Double effect ) {
        this.effect = effect;
    }

    public void setNumDataSets( Integer numDataSets ) {
        this.numDataSets = numDataSets;
    }

    public void setPvalue( Double pvalue ) {
        this.pvalue = pvalue;
    }

    public void setSpecificityVector( byte[] specificityVector ) {
        this.specificityVector = specificityVector;
    }

}