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
package ubic.gemma.model.genome.sequenceAnalysis;

import java.util.Collection;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @version $Id$
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService
 */
@Service
public class BlatResultServiceImpl extends BlatResultServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public BlatResult load( Long id ) {
        return this.getBlatResultDao().load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#thaw(ubic.gemma.model.genome.sequenceAnalysis.
     * BlatResult )
     */
    @Override
    @Transactional(readOnly = true)
    public BlatResult thaw( BlatResult blatResult ) {
        return this.getBlatResultDao().thaw( blatResult );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#thaw(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BlatResult> thaw( Collection<BlatResult> blatResults ) {
        return this.getBlatResultDao().thaw( blatResults );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#create(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    protected BlatResult handleCreate( BlatResult blatResult ) {
        return this.getBlatResultDao().create( blatResult );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.BlatResultServiceBase#handleFindByBioSequence(ubic.gemma.model.genome
     * .biosequence.BioSequence)
     */
    @Override
    protected Collection<BlatResult> handleFindByBioSequence( BioSequence bioSequence ) {
        return this.getBlatResultDao().findByBioSequence( bioSequence );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultServiceBase#handleLoad(java.util.Collection)
     */
    @SuppressWarnings("unchecked")
    @Override
    protected Collection<BlatResult> handleLoad( Collection<Long> ids ) {
        return ( Collection<BlatResult> ) this.getBlatResultDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#remove(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        this.getBlatResultDao().remove( blatResult );
    }

    @Override
    protected void handleUpdate( BlatResult blatResult ) {
        this.getBlatResultDao().update( blatResult );
    }

}