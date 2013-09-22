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

import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;

/**
 * Spring Service base class for <code>BlatAssociationService</code>, provides access to all services and entities
 * referenced by this service.
 * 
 * @see BlatAssociationService
 */
public abstract class BlatAssociationServiceBase implements BlatAssociationService {

    @Autowired
    private BlatAssociationDao blatAssociationDao;

    /**
     * @see BlatAssociationService#create(BlatAssociation)
     */
    @Override
    @Transactional
    public BlatAssociation create( final BlatAssociation blatAssociation ) {
        try {
            return this.handleCreate( blatAssociation );
        } catch ( Throwable th ) {
            throw new BlatAssociationServiceException(
                    "Error performing 'BlatAssociationService.create(BlatAssociation blatAssociation)' --> " + th, th );
        }
    }

    /**
     * @see BlatAssociationService#find(BioSequence)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BlatAssociation> find( final BioSequence bioSequence ) {
        try {
            return this.handleFind( bioSequence );
        } catch ( Throwable th ) {
            throw new BlatAssociationServiceException(
                    "Error performing 'BlatAssociationService.find(BioSequence bioSequence)' --> " + th, th );
        }
    }

    /**
     * @see BlatAssociationService#find(Gene)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<BlatAssociation> find( final Gene gene ) {
        try {
            return this.handleFind( gene );
        } catch ( Throwable th ) {
            throw new BlatAssociationServiceException( "Error performing 'BlatAssociationService.find(Gene gene)' --> "
                    + th, th );
        }
    }

    /**
     * @see BlatAssociationService#thaw(BlatAssociation)
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( final BlatAssociation blatAssociation ) {
        try {
            this.handleThaw( blatAssociation );
        } catch ( Throwable th ) {
            throw new BlatAssociationServiceException(
                    "Error performing 'BlatAssociationService.thaw(BlatAssociation blatAssociation)' --> " + th, th );
        }
    }

    /**
     * @see BlatAssociationService#thaw(Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public void thaw( final Collection<BlatAssociation> blatAssociations ) {
        try {
            this.handleThaw( blatAssociations );
        } catch ( Throwable th ) {
            throw new BlatAssociationServiceException(
                    "Error performing 'BlatAssociationService.thaw(Collection blatAssociations)' --> " + th, th );
        }
    }

    /**
     * @see BlatAssociationService#update(BlatAssociation)
     */
    @Override
    @Transactional
    public void update( final BlatAssociation blatAssociation ) {
        try {
            this.handleUpdate( blatAssociation );
        } catch ( Throwable th ) {
            throw new BlatAssociationServiceException(
                    "Error performing 'BlatAssociationService.update(BlatAssociation blatAssociation)' --> " + th, th );
        }
    }

    /**
     * Gets the reference to <code>blatAssociation</code>'s DAO.
     */
    BlatAssociationDao getBlatAssociationDao() {
        return this.blatAssociationDao;
    }

    /**
     * Performs the core logic for {@link #create(BlatAssociation)}
     */
    protected abstract BlatAssociation handleCreate( BlatAssociation blatAssociation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(BioSequence)}
     */
    protected abstract Collection<BlatAssociation> handleFind( BioSequence bioSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(Gene)}
     */
    protected abstract Collection<BlatAssociation> handleFind( Gene gene ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(BlatAssociation)}
     */
    protected abstract void handleThaw( BlatAssociation blatAssociation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract void handleThaw( Collection<BlatAssociation> blatAssociations ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(BlatAssociation)}
     */
    protected abstract void handleUpdate( BlatAssociation blatAssociation ) throws java.lang.Exception;

}