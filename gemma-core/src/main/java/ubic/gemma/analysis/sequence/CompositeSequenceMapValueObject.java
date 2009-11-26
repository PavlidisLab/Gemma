/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.analysis.sequence;

import java.util.HashMap;
import java.util.Map;

import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * @author jsantos
 * @version $Id$
 */
public class CompositeSequenceMapValueObject implements Comparable<CompositeSequenceMapValueObject> {
    private String compositeSequenceId = null;
    private String compositeSequenceName = null;
    private String compositeSequenceDescription = null;
    private String bioSequenceId = null;
    private String bioSequenceName = null;
    private String bioSequenceNcbiId = null;
    private String arrayDesignName = null;
    private Long arrayDesignId = null;
    private Long numBlatHits = null;

    private Map<String, GeneProductValueObject> geneProducts = new HashMap<String, GeneProductValueObject>();;
    private Map<String, GeneValueObject> genes = new HashMap<String, GeneValueObject>();

    public CompositeSequenceMapValueObject() {
    }

    /**
     * @return the bioSequenceId
     */
    public String getBioSequenceId() {
        return bioSequenceId;
    }

    /**
     * @param bioSequenceId the bioSequenceId to set
     */
    public void setBioSequenceId( String bioSequenceId ) {
        this.bioSequenceId = bioSequenceId;
    }

    /**
     * @return the bioSequenceNcbiId
     */
    public String getBioSequenceNcbiId() {
        return bioSequenceNcbiId;
    }

    /**
     * @param bioSequenceNcbiId the bioSequenceNcbiId to set
     */
    public void setBioSequenceNcbiId( String bioSequenceNcbiId ) {
        this.bioSequenceNcbiId = bioSequenceNcbiId;
    }

    /**
     * @return the compositeSequenceId
     */
    public String getCompositeSequenceId() {
        return compositeSequenceId;
    }

    /**
     * @param compositeSequenceId the compositeSequenceId to set
     */
    public void setCompositeSequenceId( String compositeSequenceId ) {
        this.compositeSequenceId = compositeSequenceId;
    }

    /**
     * @return the compositeSequenceName
     */
    public String getCompositeSequenceName() {
        return compositeSequenceName;
    }

    /**
     * @param compositeSequenceName the compositeSequenceName to set
     */
    public void setCompositeSequenceName( String compositeSequenceName ) {
        this.compositeSequenceName = compositeSequenceName;
    }

    /**
     * @return the numBlatHits
     */
    public Long getNumBlatHits() {
        return numBlatHits;
    }

    /**
     * @param numBlatHits the numBlatHits to set
     */
    public void setNumBlatHits( long numBlatHits ) {
        this.numBlatHits = numBlatHits;
    }

    /**
     * @return the bioSequenceName
     */
    public String getBioSequenceName() {
        return bioSequenceName;
    }

    /**
     * @param bioSequenceName the bioSequenceName to set
     */
    public void setBioSequenceName( String bioSequenceName ) {
        this.bioSequenceName = bioSequenceName;
    }

    /**
     * @return the geneProducts
     */
    public Map<String, GeneProductValueObject> getGeneProducts() {
        return geneProducts;
    }

    /**
     * @param geneProducts the geneProducts to set
     */
    public void setGeneProducts( Map<String, GeneProductValueObject> geneProducts ) {
        this.geneProducts = geneProducts;
    }

    /**
     * @return the genes
     */
    public Map<String, GeneValueObject> getGenes() {
        return genes;
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes( Map<String, GeneValueObject> genes ) {
        this.genes = genes;
    }

    public String getArrayDesignName() {
        return arrayDesignName;
    }

    public void setArrayDesignName( String arrayDesignName ) {
        this.arrayDesignName = arrayDesignName;
    }

    public void setNumBlatHits( Long numBlatHits ) {
        this.numBlatHits = numBlatHits;
    }

    public Long getArrayDesignId() {
        return arrayDesignId;
    }

    public void setArrayDesignId( Long arrayDesignId ) {
        this.arrayDesignId = arrayDesignId;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo( CompositeSequenceMapValueObject o ) {
        return this.compositeSequenceName.compareTo( o.getCompositeSequenceName() );
    }

    public String getCompositeSequenceDescription() {
        return compositeSequenceDescription;
    }

    public void setCompositeSequenceDescription( String compositeSequenceDescription ) {
        this.compositeSequenceDescription = compositeSequenceDescription;
    }

}
