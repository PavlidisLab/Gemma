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

import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.association.Gene2GeneAssociation;

/**
 * 
 */
public abstract class Gene2GeneCoexpression extends Gene2GeneAssociation {

    final private byte[] datasetsSupportingVector = null;

    final private byte[] datasetsTestedVector = null;

    final private Double effect = null;

    final private Integer numDataSets = null;

    private final Analysis sourceAnalysis = null;

    final private byte[] specificityVector = null;

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

    public Analysis getSourceAnalysis() {
        return sourceAnalysis;
    }

    /**
     * A bit vector representing whether the experiments had probes specific to the query gene AND the target genes. A
     * '1' means it had specific probes; a '0' mean it did not.
     */
    public byte[] getSpecificityVector() {
        return this.specificityVector;
    }

}