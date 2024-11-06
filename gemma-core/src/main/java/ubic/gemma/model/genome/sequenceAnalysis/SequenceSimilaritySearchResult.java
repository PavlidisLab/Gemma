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

import ubic.gemma.model.common.AbstractIdentifiable;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.util.Objects;

public abstract class SequenceSimilaritySearchResult extends AbstractIdentifiable {

    private BioSequence querySequence;
    private BioSequence targetSequence;
    private Chromosome targetChromosome;
    private ExternalDatabase searchedDatabase;
    private PhysicalLocation targetAlignedRegion;

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

    @SuppressWarnings("unused")
    public void setTargetSequence( BioSequence targetSequence ) {
        this.targetSequence = targetSequence;
    }

    /**
     * @return a hash code based on this entity's identifiers.
     */
    @Override
    public int hashCode() {
        return Objects.hash( querySequence, targetSequence, targetChromosome, searchedDatabase, targetChromosome );
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
        if ( this.getId() != null && that.getId() != null ) {
            return this.getId().equals( that.getId() );
        } else {
            return Objects.equals( this.querySequence, that.querySequence )
                    && Objects.equals( this.targetSequence, that.targetSequence )
                    && Objects.equals( this.targetChromosome, that.targetChromosome )
                    && Objects.equals( this.searchedDatabase, that.searchedDatabase )
                    && Objects.equals( this.targetAlignedRegion, that.targetAlignedRegion );
        }
    }
}