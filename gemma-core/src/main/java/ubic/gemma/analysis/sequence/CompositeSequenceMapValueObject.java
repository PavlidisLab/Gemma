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

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

/**
 * @author jsantos
 * @version $Id$
 */
public class CompositeSequenceMapValueObject implements Comparable<CompositeSequenceMapValueObject> {
    public static CompositeSequenceMapValueObject fromEntity( CompositeSequence cs ) {
        CompositeSequenceMapValueObject vo = new CompositeSequenceMapValueObject();
        vo.setArrayDesignId( cs.getArrayDesign().getId() );
        vo.setArrayDesignName( cs.getArrayDesign().getName() );
        vo.setBioSequenceId( cs.getBiologicalCharacteristic().getId().toString() );
        vo.setBioSequenceName( cs.getBiologicalCharacteristic().getName() );
        vo.setCompositeSequenceDescription( cs.getDescription() );
        vo.setCompositeSequenceId( cs.getId().toString() );
        vo.setCompositeSequenceName( cs.getName() );
        // Map<String, GeneProductValueObject> gpvos = new HashMap<String, GeneProductValueObject>();
        // Map<String, GeneValueObject> gvos = new HashMap<String, GeneValueObject>();
        /*
         * for (BioSequence2GeneProduct bs_2_gp : cs.getBiologicalCharacteristic().getBioSequence2GeneProduct() ) {
         * GeneProduct gp = bs_2_gp.getGeneProduct(); gpvos.put(gp.getName(), GeneProductValueObject.fromEntity(gp));
         * gvos.put(gp.getGene().getName(), GeneValueObject.fromEntity(gp.getGene())); }
         * 
         * vo.setGeneProducts(gpvos); vo.setGenes(gvos);
         */
        return vo;
    }

    private String compositeSequenceId = null;
    private String compositeSequenceName = null;
    private String compositeSequenceDescription = null;
    private String bioSequenceId = null;
    private String bioSequenceName = null;
    private String bioSequenceNcbiId = null;
    private String arrayDesignName = null;
    private Long arrayDesignId = null;

    private Long numBlatHits = null;
    private Map<String, GeneProductValueObject> geneProducts = new HashMap<String, GeneProductValueObject>();

    private Map<String, GeneValueObject> genes = new HashMap<String, GeneValueObject>();

    public CompositeSequenceMapValueObject() {
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( CompositeSequenceMapValueObject o ) {
        return this.compositeSequenceName.compareTo( o.getCompositeSequenceName() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if ( this == obj ) return true;
        if ( obj == null ) return false;
        if ( getClass() != obj.getClass() ) return false;
        CompositeSequenceMapValueObject other = ( CompositeSequenceMapValueObject ) obj;
        if ( compositeSequenceName == null ) {
            if ( other.compositeSequenceName != null ) return false;
        } else if ( !compositeSequenceName.equals( other.compositeSequenceName ) ) return false;
        return true;
    }

    public Long getArrayDesignId() {
        return arrayDesignId;
    }

    public String getArrayDesignName() {
        return arrayDesignName;
    }

    /**
     * @return the bioSequenceId
     */
    public String getBioSequenceId() {
        return bioSequenceId;
    }

    /**
     * @return the bioSequenceName
     */
    public String getBioSequenceName() {
        return bioSequenceName;
    }

    /**
     * @return the bioSequenceNcbiId
     */
    public String getBioSequenceNcbiId() {
        return bioSequenceNcbiId;
    }

    public String getCompositeSequenceDescription() {
        return compositeSequenceDescription;
    }

    /**
     * @return the compositeSequenceId
     */
    public String getCompositeSequenceId() {
        return compositeSequenceId;
    }

    /**
     * @return the compositeSequenceName
     */
    public String getCompositeSequenceName() {
        return compositeSequenceName;
    }

    /**
     * @return the geneProducts
     */
    public Map<String, GeneProductValueObject> getGeneProducts() {
        return geneProducts;
    }

    /**
     * @return the genes
     */
    public Map<String, GeneValueObject> getGenes() {
        return genes;
    }

    /**
     * @return the numBlatHits
     */
    public Long getNumBlatHits() {
        return numBlatHits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( compositeSequenceName == null ) ? 0 : compositeSequenceName.hashCode() );
        return result;
    }

    public void setArrayDesignId( Long arrayDesignId ) {
        this.arrayDesignId = arrayDesignId;
    }

    public void setArrayDesignName( String arrayDesignName ) {
        this.arrayDesignName = arrayDesignName;
    }

    /**
     * @param bioSequenceId the bioSequenceId to set
     */
    public void setBioSequenceId( String bioSequenceId ) {
        this.bioSequenceId = bioSequenceId;
    }

    /**
     * @param bioSequenceName the bioSequenceName to set
     */
    public void setBioSequenceName( String bioSequenceName ) {
        this.bioSequenceName = bioSequenceName;
    }

    /**
     * @param bioSequenceNcbiId the bioSequenceNcbiId to set
     */
    public void setBioSequenceNcbiId( String bioSequenceNcbiId ) {
        this.bioSequenceNcbiId = bioSequenceNcbiId;
    }

    public void setCompositeSequenceDescription( String compositeSequenceDescription ) {
        this.compositeSequenceDescription = compositeSequenceDescription;
    }

    /**
     * @param compositeSequenceId the compositeSequenceId to set
     */
    public void setCompositeSequenceId( String compositeSequenceId ) {
        this.compositeSequenceId = compositeSequenceId;
    }

    /**
     * @param compositeSequenceName the compositeSequenceName to set
     */
    public void setCompositeSequenceName( String compositeSequenceName ) {
        this.compositeSequenceName = compositeSequenceName;
    }

    /**
     * @param geneProducts the geneProducts to set
     */
    public void setGeneProducts( Map<String, GeneProductValueObject> geneProducts ) {
        this.geneProducts = geneProducts;
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes( Map<String, GeneValueObject> genes ) {
        this.genes = genes;
    }

    /**
     * @param numBlatHits the numBlatHits to set
     */
    public void setNumBlatHits( long numBlatHits ) {
        this.numBlatHits = numBlatHits;
    }

    public void setNumBlatHits( Long numBlatHits ) {
        this.numBlatHits = numBlatHits;
    }

}
