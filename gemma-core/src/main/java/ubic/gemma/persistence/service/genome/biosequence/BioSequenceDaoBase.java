/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.genome.biosequence;

import org.hibernate.SessionFactory;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Collection;
import java.util.Map;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.genome.biosequence.BioSequence</code>.
 *
 * @see ubic.gemma.model.genome.biosequence.BioSequence
 */
public abstract class BioSequenceDaoBase extends VoEnabledDao<BioSequence, BioSequenceValueObject>
        implements BioSequenceDao {

    public BioSequenceDaoBase( SessionFactory sessionFactory ) {
        super( BioSequence.class, sessionFactory );
    }

    /**
     * @see BioSequenceDao#findByGenes(Collection)
     */
    @Override
    public Map<Gene, Collection<BioSequence>> findByGenes( final Collection<Gene> genes ) {
        return this.handleFindByGenes( genes );
    }

    /**
     * @see BioSequenceDao#findByName(java.lang.String)
     */
    @Override
    public Collection<BioSequence> findByName( final java.lang.String name ) {
        return this.handleFindByName( name );
    }

    /**
     * @see BioSequenceDao#getGenesByAccession(java.lang.String)
     */
    @Override
    public Collection<Gene> getGenesByAccession( final java.lang.String search ) {
        return this.handleGetGenesByAccession( search );
    }

    /**
     * @see BioSequenceDao#getGenesByName(java.lang.String)
     */
    @Override
    public Collection<Gene> getGenesByName( final java.lang.String search ) {
        return this.handleGetGenesByName( search );
    }

    /**
     * @see BioSequenceDao#thaw(Collection)
     */
    @Override
    public Collection<BioSequence> thaw( final Collection<BioSequence> bioSequences ) {
        return this.handleThaw( bioSequences );
    }

    @Override
    public BioSequence thaw( final BioSequence bioSequence ) {
        return this.handleThaw( bioSequence );
    }

    /**
     * Performs the core logic for {@link #findByGenes(Collection)}
     */
    protected abstract Map<Gene, Collection<BioSequence>> handleFindByGenes( Collection<Gene> genes );

    /**
     * Performs the core logic for {@link #findByName(java.lang.String)}
     */
    protected abstract Collection<BioSequence> handleFindByName( java.lang.String name );

    /**
     * Performs the core logic for {@link #getGenesByAccession(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByAccession( java.lang.String search );

    /**
     * Performs the core logic for {@link #getGenesByName(java.lang.String)}
     */
    protected abstract Collection<Gene> handleGetGenesByName( java.lang.String search );

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract Collection<BioSequence> handleThaw( Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract BioSequence handleThaw( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

}