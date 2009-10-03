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

import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * @version $Id$
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService
 */
public class BlatResultServiceImpl extends ubic.gemma.model.genome.sequenceAnalysis.BlatResultServiceBase {

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#create(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    protected ubic.gemma.model.genome.sequenceAnalysis.BlatResult handleCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) throws java.lang.Exception {
        return ( BlatResult ) this.getBlatResultDao().create( blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#find(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    protected ubic.gemma.model.genome.sequenceAnalysis.BlatResult handleFind(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) throws java.lang.Exception {
        return this.getBlatResultDao().find( blatResult );
    }

    /*
     * (non-Javadoc)
     * @see
     * ubic.gemma.model.genome.sequenceAnalysis.BlatResultServiceBase#handleFindByBioSequence(ubic.gemma.model.genome
     * .biosequence.BioSequence)
     */
    @Override
    protected Collection handleFindByBioSequence( BioSequence bioSequence ) throws Exception {
        return this.getBlatResultDao().findByBioSequence( bioSequence );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#findOrCreate(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    protected ubic.gemma.model.genome.sequenceAnalysis.BlatResult handleFindOrCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) throws java.lang.Exception {
        return this.getBlatResultDao().findOrCreate( blatResult );
    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultServiceBase#handleLoad(java.util.Collection)
     */
    @Override
    protected Collection handleLoad( Collection ids ) throws Exception {
        return this.getBlatResultDao().load( ids );
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#remove(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    protected void handleRemove( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult )
            throws java.lang.Exception {
        this.getBlatResultDao().remove( blatResult );
    }

    @Override
    protected void handleUpdate( BlatResult blatResult ) throws Exception {
        this.getBlatResultDao().update( blatResult );
    }

}