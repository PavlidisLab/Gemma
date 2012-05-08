/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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
package ubic.gemma.genome.gene;

import java.util.Collection;

import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * Used for gene page
 * 
 * @author tvrossum
 * @version $Id$
 */
public class GeneDetailsValueObject extends GeneValueObject {

    private static final long serialVersionUID = -8145779822182567113L;
    private Collection<GeneValueObject> homologues;
    private Collection<GeneSetValueObject> geneSets;
    private Long compositeSequenceCount = 0L; // number of probes

    private Integer numGoTerms = 0;
    private Double multifunctionalityRank = 0.0;

    private Double nodeDegreeRank = 0.0;
    
    /**
     * How many experiments "involve" (manipulate, etc.) this gene
     */
    private Integer associatedExperimentCount = 0;

    public GeneDetailsValueObject() {
        super();
    }

    public GeneDetailsValueObject( GeneValueObject otherBean ) {
        super( otherBean );
    }

    public Integer getAssociatedExperimentCount() {
        return associatedExperimentCount;
    }

    /**
     * @return the compositeSequenceCount
     */
    public Long getCompositeSequenceCount() {
        return compositeSequenceCount;
    }

    /**
     * @return the geneSets
     */
    public Collection<GeneSetValueObject> getGeneSets() {
        return geneSets;
    }

    /**
     * @return the homologues
     */
    public Collection<GeneValueObject> getHomologues() {
        return homologues;
    }

    public Double getMultifunctionalityRank() {
        return multifunctionalityRank;
    }

    public Double getNodeDegreeRank() {
        return nodeDegreeRank;
    }

    public Integer getNumGoTerms() {
        return numGoTerms;
    }

    public void setAssociatedExperimentCount( Integer associatedExperimentCount ) {
        this.associatedExperimentCount = associatedExperimentCount;
    }

    /**
     * @param compositeSequenceCount the compositeSequenceCount to set
     */
    public void setCompositeSequenceCount( Long compositeSequenceCount ) {
        this.compositeSequenceCount = compositeSequenceCount;
    }

    /**
     * @param geneSets the geneSets to set
     */
    public void setGeneSets( Collection<GeneSetValueObject> geneSets ) {
        this.geneSets = geneSets;
    }

    /**
     * @param homologues the homologues to set
     */
    public void setHomologues( Collection<GeneValueObject> homologues ) {
        this.homologues = homologues;
    }

    public void setMultifunctionalityRank( Double multifunctionalityRank ) {
        this.multifunctionalityRank = multifunctionalityRank;
    }

    public void setNodeDegreeRank( Double nodeDegreeRank ) {
        this.nodeDegreeRank = nodeDegreeRank;
    }

    public void setNumGoTerms( Integer numGoTerms ) {
        this.numGoTerms = numGoTerms;
    }

}
