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
package ubic.gemma.model.genome.sequenceAnalysis;

/**
 * 
 */
public abstract class SequenceSimilaritySearchResult implements java.io.Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7196820023599562042L;

    private Long id;

    private ubic.gemma.model.genome.biosequence.BioSequence querySequence;
    private ubic.gemma.model.genome.biosequence.BioSequence targetSequence;

    private ubic.gemma.model.genome.Chromosome targetChromosome;

    private ubic.gemma.model.common.description.ExternalDatabase searchedDatabase;

    private ubic.gemma.model.genome.PhysicalLocation targetAlignedRegion;

    /**
     * No-arg constructor added to satisfy javabean contract
     * 
     * @author Paul
     */
    public SequenceSimilaritySearchResult() {
    }

    /**
     * Returns <code>true</code> if the argument is an SequenceSimilaritySearchResult instance and all identifiers for
     * this entity equal the identifiers of the argument entity. Returns <code>false</code> otherwise.
     */
    @Override
    public boolean equals( Object object ) {
        if ( this == object ) {
            return true;
        }
        if ( !( object instanceof SequenceSimilaritySearchResult ) ) {
            return false;
        }
        final SequenceSimilaritySearchResult that = ( SequenceSimilaritySearchResult ) object;
        if ( this.id == null || that.getId() == null || !this.id.equals( that.getId() ) ) {
            return false;
        }
        return true;
    }

    /**
     * 
     */
    public Long getId() {
        return this.id;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.biosequence.BioSequence getQuerySequence() {
        return this.querySequence;
    }

    /**
     * <p>
     * <p>
     * The database used for the search. This will be null if the comparison was just between two sequences.
     * </p>
     * </p>
     */
    public ubic.gemma.model.common.description.ExternalDatabase getSearchedDatabase() {
        return this.searchedDatabase;
    }

    /**
     * <p>
     * The region of the target spanned by the alignment.
     * </p>
     */
    public ubic.gemma.model.genome.PhysicalLocation getTargetAlignedRegion() {
        return this.targetAlignedRegion;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.Chromosome getTargetChromosome() {
        return this.targetChromosome;
    }

    /**
     * 
     */
    public ubic.gemma.model.genome.biosequence.BioSequence getTargetSequence() {
        return this.targetSequence;
    }

    /**
     * Returns a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setQuerySequence( ubic.gemma.model.genome.biosequence.BioSequence querySequence ) {
        this.querySequence = querySequence;
    }

    public void setSearchedDatabase( ubic.gemma.model.common.description.ExternalDatabase searchedDatabase ) {
        this.searchedDatabase = searchedDatabase;
    }

    public void setTargetAlignedRegion( ubic.gemma.model.genome.PhysicalLocation targetAlignedRegion ) {
        this.targetAlignedRegion = targetAlignedRegion;
    }

    public void setTargetChromosome( ubic.gemma.model.genome.Chromosome targetChromosome ) {
        this.targetChromosome = targetChromosome;
    }

    public void setTargetSequence( ubic.gemma.model.genome.biosequence.BioSequence targetSequence ) {
        this.targetSequence = targetSequence;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult#toString()
     */
    @Override
    public String toString() {
        return this.getId().toString();
    }

}