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

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Spring Service base class for <code>BlatResultService</code>, provides access to all services and entities referenced
 * by this service.
 * 
 * @see BlatResultService
 */
public abstract class BlatResultServiceBase implements BlatResultService {

    @Autowired
    private BlatResultDao blatResultDao;

    /**
     * @see BlatResultService#create(BlatResult)
     */
    @Override
    @Transactional
    public BlatResult create( final BlatResult blatResult ) {
        return this.handleCreate( blatResult );

    }

    /**
     * @see BlatResultService#findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BlatResult> findByBioSequence( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        return this.handleFindByBioSequence( bioSequence );

    }

    /**
     * @see BlatResultService#load(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BlatResult> load( final Collection<Long> ids ) {
        return this.handleLoad( ids );

    }

    /**
     * @see BlatResultService#remove(BlatResult)
     */
    @Override
    @Transactional
    public void remove( final BlatResult blatResult ) {
        this.handleRemove( blatResult );

    }

    /**
     * Sets the reference to <code>blatResult</code>'s DAO.
     */
    public void setBlatResultDao( BlatResultDao blatResultDao ) {
        this.blatResultDao = blatResultDao;
    }

    /**
     * @see BlatResultService#update(BlatResult)
     */
    @Override
    public void update( final BlatResult blatResult ) {
        this.handleUpdate( blatResult );

    }

    /**
     * Gets the reference to <code>blatResult</code>'s DAO.
     */
    protected BlatResultDao getBlatResultDao() {
        return this.blatResultDao;
    }

    /**
     * Performs the core logic for {@link #create(BlatResult)}
     */
    protected abstract BlatResult handleCreate( BlatResult blatResult );

    /**
     * Performs the core logic for {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract Collection<BlatResult> handleFindByBioSequence(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * Performs the core logic for {@link #load(Collection)}
     */
    protected abstract Collection<BlatResult> handleLoad( Collection<Long> ids );

    /**
     * Performs the core logic for {@link #remove(BlatResult)}
     */
    protected abstract void handleRemove( BlatResult blatResult );

    /**
     * Performs the core logic for {@link #update(BlatResult)}
     */
    protected abstract void handleUpdate( BlatResult blatResult );

}