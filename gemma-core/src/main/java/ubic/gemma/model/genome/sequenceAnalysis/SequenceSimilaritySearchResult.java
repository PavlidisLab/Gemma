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

import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.Serializable;

public abstract class SequenceSimilaritySearchResult implements Identifiable, Serializable {

    /**
     * The serial version UID of this class. Needed for serialization.
     */
    private static final long serialVersionUID = -7196820023599562042L;

    private Long id;
    private BioSequence querySequence;
    private BioSequence targetSequence;
    private Chromosome targetChromosome;
    private ExternalDatabase searchedDatabase;
    private PhysicalLocation targetAlignedRegion;

    /**
     * No-arg constructor added to satisfy javabean contract
     *
     * @author Paul
     */
    public SequenceSimilaritySearchResult() {
    }

    /**
     * @return <code>true</code> if the argument is an SequenceSimilaritySearchResult instance and all identifiers for
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

    public Long getId() {
        return this.id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public BioSequence getQuerySequence() {
        return this.querySequence;
    }

    public void setQuerySequence( BioSequence querySequence ) {
        this.querySequence = querySequence;
    }

    /**
     * @return The database used for the search. This will be null if the comparison was just between two sequences.
     */
    public ExternalDatabase getSearchedDatabase() {
        return this.searchedDatabase;
    }

    public void setSearchedDatabase( ExternalDatabase searchedDatabase ) {
        this.searchedDatabase = searchedDatabase;
    }

    /**
     * @return The region of the target spanned by the alignment.
     */
    public PhysicalLocation getTargetAlignedRegion() {
        return this.targetAlignedRegion;
    }

    public void setTargetAlignedRegion( PhysicalLocation targetAlignedRegion ) {
        this.targetAlignedRegion = targetAlignedRegion;
    }

    public Chromosome getTargetChromosome() {
        return this.targetChromosome;
    }

    public void setTargetChromosome( Chromosome targetChromosome ) {
        this.targetChromosome = targetChromosome;
    }

    public BioSequence getTargetSequence() {
        return this.targetSequence;
    }

    public void setTargetSequence( BioSequence targetSequence ) {
        this.targetSequence = targetSequence;
    }

    /**
     * @return a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        int hashCode = 0;
        hashCode = 29 * hashCode + ( id == null ? 0 : id.hashCode() );

        return hashCode;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult#toString()
     */
    @Override
    public String toString() {
        return this.getId().toString();
    }

}