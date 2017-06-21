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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

/**
 * @author keshav
 * @author pavlidis
 * @see BioSequenceService
 */
@Service
public class BioSequenceServiceImpl extends BioSequenceServiceBase {

    @Autowired
    public BioSequenceServiceImpl( BioSequenceDao bioSequenceDao ) {
        super( bioSequenceDao );
    }

    /* ********************************
     * Public methods
     * ********************************/

    @Override
    protected BioSequence handleFindByAccession( DatabaseEntry accession ) {
        return this.bioSequenceDao.findByAccession( accession );
    }

    @Override
    protected Map<Gene, Collection<BioSequence>> handleFindByGenes( Collection<Gene> genes ) {
        return this.bioSequenceDao.findByGenes( genes );
    }

    @Override
    protected Collection<BioSequence> handleFindByName( String name ) {
        return this.bioSequenceDao.findByName( name );
    }

    @Override
    protected Collection<BioSequence> handleFindOrCreate( Collection<BioSequence> bioSequences ) {
        Collection<BioSequence> result = new HashSet<BioSequence>();
        for ( BioSequence bioSequence : bioSequences ) {
            result.add( this.bioSequenceDao.findOrCreate( bioSequence ) );
        }
        return result;
    }

    @Override
    protected Collection<Gene> handleGetGenesByAccession( String search ) {
        return this.bioSequenceDao.getGenesByAccession( search );
    }

    @Override
    protected Collection<Gene> handleGetGenesByName( String search ) {
        return this.bioSequenceDao.getGenesByName( search );
    }

}