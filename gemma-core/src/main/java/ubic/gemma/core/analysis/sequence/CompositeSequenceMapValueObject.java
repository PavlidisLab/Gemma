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
package ubic.gemma.core.analysis.sequence;

import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.gene.GeneProductValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;

import java.util.HashMap;
import java.util.Map;

/**
 * @author jsantos
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Frontend use
public class CompositeSequenceMapValueObject implements Comparable<CompositeSequenceMapValueObject> {

    private Long arrayDesignId = null;
    private String arrayDesignName = null;
    private String arrayDesignShortName = null;
    private String bioSequenceId = null;
    private String bioSequenceName = null;
    private String bioSequenceNcbiId = null;
    private String compositeSequenceDescription = null;
    private String compositeSequenceId = null;
    private String compositeSequenceName = null;
    private Map<Long, GeneProductValueObject> geneProducts = new HashMap<>();
    private Map<Long, GeneValueObject> genes = new HashMap<>();
    private Integer numBlatHits = null;

    public CompositeSequenceMapValueObject() {
    }

    public static CompositeSequenceMapValueObject fromEntity( CompositeSequence cs ) {

        CompositeSequenceMapValueObject vo = new CompositeSequenceMapValueObject();
        vo.setArrayDesignId( cs.getArrayDesign().getId() );
        vo.setArrayDesignName( cs.getArrayDesign().getName() );
        vo.setBioSequenceId( cs.getBiologicalCharacteristic().getId().toString() );
        vo.setBioSequenceName( cs.getBiologicalCharacteristic().getName() );
        vo.setCompositeSequenceDescription( cs.getDescription() );
        vo.setCompositeSequenceId( cs.getId().toString() );
        vo.setArrayDesignShortName( cs.getArrayDesign().getShortName() );
        vo.setCompositeSequenceName( cs.getName() );
        return vo;
    }

    @Override
    public int compareTo( CompositeSequenceMapValueObject o ) {
        return this.compositeSequenceName.compareTo( o.getCompositeSequenceName() );
    }

    public Long getArrayDesignId() {
        return arrayDesignId;
    }

    public void setArrayDesignId( Long arrayDesignId ) {
        this.arrayDesignId = arrayDesignId;
    }

    public String getArrayDesignName() {
        return arrayDesignName;
    }

    public void setArrayDesignName( String arrayDesignName ) {
        this.arrayDesignName = arrayDesignName;
    }

    public String getArrayDesignShortName() {
        return arrayDesignShortName;
    }

    public void setArrayDesignShortName( String arrayDesignShortName ) {
        this.arrayDesignShortName = arrayDesignShortName;
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

    public String getCompositeSequenceDescription() {
        return compositeSequenceDescription;
    }

    public void setCompositeSequenceDescription( String compositeSequenceDescription ) {
        this.compositeSequenceDescription = compositeSequenceDescription;
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
     * @return the geneProducts
     */
    public Map<Long, GeneProductValueObject> getGeneProducts() {
        return geneProducts;
    }

    /**
     * @param geneProducts the geneProducts to set
     */
    public void setGeneProducts( Map<Long, GeneProductValueObject> geneProducts ) {
        this.geneProducts = geneProducts;
    }

    /**
     * @return the genes
     */
    public Map<Long, GeneValueObject> getGenes() {
        return genes;
    }

    /**
     * @param genes the genes to set
     */
    public void setGenes( Map<Long, GeneValueObject> genes ) {
        this.genes = genes;
    }

    /**
     * @return the numBlatHits
     */
    public Integer getNumBlatHits() {
        return numBlatHits;
    }

    /**
     * @param numBlatHits the numBlatHits to set
     */
    public void setNumBlatHits( Integer numBlatHits ) {
        this.numBlatHits = numBlatHits;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( compositeSequenceName == null ) ? 0 : compositeSequenceName.hashCode() );
        return result;
    }

    @Override
    public boolean equals( Object obj ) {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( this.getClass() != obj.getClass() )
            return false;
        CompositeSequenceMapValueObject other = ( CompositeSequenceMapValueObject ) obj;
        if ( compositeSequenceName == null ) {
            return other.compositeSequenceName == null;
        }
        return compositeSequenceName.equals( other.compositeSequenceName );
    }

}
