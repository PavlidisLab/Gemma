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

/**
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.model.genome.biosequence.BioSequenceService
 */
@Service
public class BioSequenceServiceImpl extends ubic.gemma.model.genome.biosequence.BioSequenceServiceBase {

    @Override
    protected Integer handleCountAll() throws Exception {
        return this.getBioSequenceDao().countAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleCreate(ubic.gemma.model.genome.biosequence.
     * BioSequence)
     */
    @Override
    protected BioSequence handleCreate( BioSequence bioSequence ) throws Exception {
        return this.getBioSequenceDao().create( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleCreate(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleCreate( Collection bioSequences ) throws Exception {
        return this.getBioSequenceDao().create( bioSequences );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected ubic.gemma.model.genome.biosequence.BioSequence handleFind(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception {
        return this.getBioSequenceDao().find( bioSequence );
    }

    @Override
    protected BioSequence handleFindByAccession( DatabaseEntry accession ) throws Exception {
        return this.getBioSequenceDao().findByAccession( accession );
    }

    @Override
    protected Map handleFindByGenes( Collection genes ) throws Exception {
        return this.getBioSequenceDao().findByGenes( genes );
    }

    @Override
    protected Collection handleFindByName( String name ) throws Exception {
        return this.getBioSequenceDao().findByName( name );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#findOrCreate(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected BioSequence handleFindOrCreate( BioSequence bioSequence ) throws Exception {
        return this.getBioSequenceDao().findOrCreate( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleFindOrCreate(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection handleFindOrCreate( Collection bioSequences ) throws Exception {
        Collection<BioSequence> result = new HashSet<BioSequence>();
        for ( BioSequence bioSequence : ( Collection<BioSequence> ) bioSequences ) {
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
    protected Collection handleGetGenesByAccession( String search ) throws Exception {
        return this.getBioSequenceDao().getGenesByAccession( search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleGetGenesByName(java.lang.String)
     */
    @Override
    protected Collection handleGetGenesByName( String search ) throws Exception {
        return this.getBioSequenceDao().getGenesByName( search );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleLoad(long)
     */
    @Override
    protected BioSequence handleLoad( long id ) throws Exception {
        return this.getBioSequenceDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleLoadMultiple(java.util.Collection)
     */
    @Override
    protected Collection handleLoadMultiple( Collection ids ) throws Exception {
        return this.getBioSequenceDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#remove(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception {
        this.getBioSequenceDao().remove( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleThaw(ubic.gemma.model.genome.biosequence.BioSequence
     * )
     */
    @Override
    protected BioSequence handleThaw( BioSequence bioSequence ) throws Exception {
        return this.getBioSequenceDao().thaw( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.biosequence.BioSequenceServiceBase#handleThaw(java.util.Collection)
     */
    @Override
    protected Collection<BioSequence> handleThaw( Collection<BioSequence> bioSequences ) throws Exception {
        return this.getBioSequenceDao().thaw( bioSequences );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#update(java.util.Collection)
     */
    @Override
    protected void handleUpdate( Collection bioSequences ) throws Exception {
        this.getBioSequenceDao().update( bioSequences );
    }

    /**
     * @see ubic.gemma.model.genome.biosequence.BioSequenceService#update(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    protected void handleUpdate( ubic.gemma.model.genome.biosequence.BioSequence bioSequence )
            throws java.lang.Exception {
        this.getBioSequenceDao().update( bioSequence );
    }

}