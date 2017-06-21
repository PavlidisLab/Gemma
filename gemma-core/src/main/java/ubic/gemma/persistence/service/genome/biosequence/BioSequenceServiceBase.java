/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2011 University of British Columbia
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.AbstractService;
import ubic.gemma.persistence.service.VoEnabledService;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Service base class for <code>BioSequenceService</code>, provides access to
 * all services and entities referenced by this service.
 *
 * @see BioSequenceService
 */
public abstract class BioSequenceServiceBase extends VoEnabledService<BioSequence, BioSequenceValueObject>
        implements BioSequenceService {

    final BioSequenceDao bioSequenceDao;

    @Autowired
    public BioSequenceServiceBase( BioSequenceDao bioSequenceDao ) {
        super( bioSequenceDao );
        this.bioSequenceDao = bioSequenceDao;
    }

    /**
     * @see BioSequenceService#findByAccession(DatabaseEntry)
     */
    @Override
    @Transactional(readOnly = true)
    public BioSequence findByAccession( final DatabaseEntry accession ) {
        return this.handleFindByAccession( accession );

    }

    /**
     * @see BioSequenceService#findByGenes(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Gene, Collection<BioSequence>> findByGenes( final Collection<Gene> genes ) {
        return this.handleFindByGenes( genes );

    }

    /**
     * @see BioSequenceService#findByName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BioSequence> findByName( final String name ) {
        return this.handleFindByName( name );

    }

    /**
     * @see BioSequenceService#findOrCreate(Collection)
     */
    @Override
    @Transactional
    public Collection<BioSequence> findOrCreate( final Collection<BioSequence> bioSequences ) {
        return this.handleFindOrCreate( bioSequences );

    }

    /**
     * @see BioSequenceService#getGenesByAccession(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByAccession( final String search ) {
        return this.handleGetGenesByAccession( search );

    }

    /**
     * @see BioSequenceService#getGenesByName(String)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Gene> getGenesByName( final String search ) {
        return this.handleGetGenesByName( search );

    }

    /**
     * Performs the core logic for {@link #findByAccession(DatabaseEntry)}
     */
    protected abstract BioSequence handleFindByAccession( DatabaseEntry accession );

    /**
     * Performs the core logic for {@link #findByGenes(Collection)}
     */
    protected abstract Map<Gene, Collection<BioSequence>> handleFindByGenes( Collection<Gene> genes );

    /**
     * Performs the core logic for {@link #findByName(String)}
     */
    protected abstract Collection<BioSequence> handleFindByName( String name );

    /**
     * Performs the core logic for {@link #findOrCreate(Collection)}
     */
    protected abstract Collection<BioSequence> handleFindOrCreate( Collection<BioSequence> bioSequences );

    /**
     * Performs the core logic for {@link #getGenesByAccession(String)}
     */
    protected abstract Collection<Gene> handleGetGenesByAccession( String search );

    /**
     * Performs the core logic for {@link #getGenesByName(String)}
     */
    protected abstract Collection<Gene> handleGetGenesByName( String search );

}