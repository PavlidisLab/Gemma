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
package ubic.gemma.model.genome.biosequence;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.springframework.stereotype.Service;

import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.genome.Gene;

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.biosequence.BioSequenceService
 */
@Service
public class BioSequenceServiceImpl extends BioSequenceServiceBase {

    @Override
    protected Integer handleCountAll() {
        return this.getBioSequenceDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleCreate(ubic.gemma.model.genome.biosequence.
     * BioSequence)
     */
    @Override
    protected BioSequence handleCreate( BioSequence bioSequence ) {
        return this.getBioSequenceDao().create( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleCreate(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<BioSequence> handleCreate( Collection<BioSequence> bioSequences ) {
        return ( Collection<BioSequence> ) this.getBioSequenceDao().create( bioSequences );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected ubic.gemma.model.genome.biosequence.BioSequence handleFind(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.getBioSequenceDao().find( bioSequence );
    }

    @Override
    protected BioSequence handleFindByAccession( DatabaseEntry accession ) {
        return this.getBioSequenceDao().findByAccession( accession );
    }

    @Override
    protected Map<Gene, Collection<BioSequence>> handleFindByGenes( Collection<Gene> genes ) {
        return this.getBioSequenceDao().findByGenes( genes );
    }

    @Override
    protected Collection<BioSequence> handleFindByName( String name ) {
        return this.getBioSequenceDao().findByName( name );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected BioSequence handleFindOrCreate( BioSequence bioSequence ) {
        return this.getBioSequenceDao().findOrCreate( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleFindOrCreate(java.util.Collection)
     */
    @Override
    protected Collection<BioSequence> handleFindOrCreate( Collection<BioSequence> bioSequences ) {
        Collection<BioSequence> result = new HashSet<BioSequence>();
        for ( BioSequence bioSequence : bioSequences ) {
            result.add( this.getBioSequenceDao().findOrCreate( bioSequence ) );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleGetGenesByAccession(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByAccession( String search ) {
        return this.getBioSequenceDao().getGenesByAccession( search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleGetGenesByName(java.lang.String)
     */
    @Override
    protected Collection<Gene> handleGetGenesByName( String search ) {
        return this.getBioSequenceDao().getGenesByName( search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleLoad(long)
     */
    @Override
    protected BioSequence handleLoad( long id ) {
        return this.getBioSequenceDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection<BioSequence> handleLoadMultiple( Collection<Long> ids ) {
        return this.getBioSequenceDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#remove(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        this.getBioSequenceDao().remove( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleThaw(ubic.gemma.model.genome.biosequence.
     * BioSequence )
     */
    @Override
    protected BioSequence handleThaw( BioSequence bioSequence ) {
        return this.getBioSequenceDao().thaw( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleThaw(java.util.Collection)
     */
    @Override
    protected Collection<BioSequence> handleThaw( Collection<BioSequence> bioSequences ) {
        return this.getBioSequenceDao().thaw( bioSequences );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#update(java.util.Collection)
     */
    @Override
    protected void handleUpdate( Collection<BioSequence> bioSequences ) {
        this.getBioSequenceDao().update( bioSequences );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#update(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        this.getBioSequenceDao().update( bioSequence );
    }

}