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
package ubic.gemma.model.genome.sequenceAnalysis;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.genome.sequenceAnalysis.BlatResultService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService
 */
public abstract class BlatResultServiceBase implements ubic.gemma.model.genome.sequenceAnalysis.BlatResultService {

    @Autowired
    private ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao blatResultDao;

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#create(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult create(
            final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        return this.handleCreate( blatResult );

    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public java.util.Collection findByBioSequence( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleFindByBioSequence( bioSequence );

    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#load(java.util.Collection)
     */
    @Override
    public java.util.Collection load( final java.util.Collection ids ) {
        return this.handleLoad( ids );

    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#remove(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public void remove( final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        this.handleRemove( blatResult );

    }

    /**
     * Sets the reference to <code>blatResult</code>'s DAO.
     */
    public void setBlatResultDao( ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao blatResultDao ) {
        this.blatResultDao = blatResultDao;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResultService#update(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    @Override
    public void update( final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        this.handleUpdate( blatResult );

    }

    /**
     * Gets the reference to <code>blatResult</code>'s DAO.
     */
    protected ubic.gemma.model.genome.sequenceAnalysis.BlatResultDao getBlatResultDao() {
        return this.blatResultDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)}
     */
    protected abstract ubic.gemma.model.genome.sequenceAnalysis.BlatResult handleCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * Performs the core logic for {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract java.util.Collection handleFindByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #load(java.util.Collection)}
     */
    protected abstract java.util.Collection handleLoad( java.util.Collection<Long> ids );

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)}
     */
    protected abstract void handleRemove( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

}