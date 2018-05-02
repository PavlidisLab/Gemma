/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
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
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * Spring Service base class for <code>BioSequenceService</code>, provides access to
 * all services and entities referenced by this service.
 *
 * @author keshav
 * @author pavlidis
 * @see BioSequenceService
 */
@Service
public class BioSequenceServiceImpl extends AbstractVoEnabledService<BioSequence, BioSequenceValueObject>
        implements BioSequenceService {

    private final BioSequenceDao bioSequenceDao;

    @Autowired
    public BioSequenceServiceImpl( BioSequenceDao bioSequenceDao ) {
        super( bioSequenceDao );
        this.bioSequenceDao = bioSequenceDao;
    }

    @Override
    public BioSequence findByAccession( DatabaseEntry accession ) {
        return this.bioSequenceDao.findByAccession( accession );
    }

    @Override
    public Map<Gene, Collection<BioSequence>> findByGenes( Collection<Gene> genes ) {
        return this.bioSequenceDao.findByGenes( genes );
    }

    @Override
    public Collection<BioSequence> findByName( String name ) {
        return this.bioSequenceDao.findByName( name );
    }

    @Override
    public Collection<BioSequence> findOrCreate( Collection<BioSequence> bioSequences ) {
        Collection<BioSequence> result = new HashSet<>();
        for ( BioSequence bioSequence : bioSequences ) {
            result.add( this.bioSequenceDao.findOrCreate( bioSequence ) );
        }
        return result;
    }

    @Override
    public Collection<Gene> getGenesByAccession( String search ) {
        return this.bioSequenceDao.getGenesByAccession( search );
    }

    @Override
    public Collection<Gene> getGenesByName( String search ) {
        return this.bioSequenceDao.getGenesByName( search );
    }

    @Override
    public Collection<BioSequence> thaw( Collection<BioSequence> bioSequences ) {
        return this.bioSequenceDao.thaw( bioSequences );
    }

    @Override
    public BioSequence thaw( BioSequence bioSequence ) {
        return this.bioSequenceDao.thaw( bioSequence );
    }

    @Override
    public BioSequence findByCompositeSequence( Long id ) {
        return this.bioSequenceDao.findByCompositeSequence( id );
    }
}