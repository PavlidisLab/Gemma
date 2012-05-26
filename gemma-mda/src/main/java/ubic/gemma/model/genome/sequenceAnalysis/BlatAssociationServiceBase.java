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
 * Spring Service base class for <code>ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService
 */
public abstract class BlatAssociationServiceBase implements
        ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService {

    @Autowired
    private ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao blatAssociationDao;

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#create(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    public ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation create(
            final ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) {
        try {
            return this.handleCreate( blatAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService.create(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#find(ubic.gemma.model.genome.biosequence.BioSequence)
     */
    @Override
    public java.util.Collection<BlatAssociation> find( final ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) {
        try {
            return this.handleFind( bioSequence );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService.find(ubic.gemma.model.genome.biosequence.BioSequence bioSequence)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#find(ubic.gemma.model.genome.Gene)
     */
    @Override
    public java.util.Collection<BlatAssociation> find( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFind( gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService.find(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>blatAssociation</code>'s DAO.
     */
    public void setBlatAssociationDao( ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao blatAssociationDao ) {
        this.blatAssociationDao = blatAssociationDao;
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#thaw(java.util.Collection)
     */
    @Override
    public void thaw( final java.util.Collection<BlatAssociation> blatAssociations ) {
        try {
            this.handleThaw( blatAssociations );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService.thaw(java.util.Collection blatAssociations)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#thaw(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    public void thaw( final ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) {
        try {
            this.handleThaw( blatAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService.thaw(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService#update(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)
     */
    @Override
    public void update( final ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) {
        try {
            this.handleUpdate( blatAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationService.update(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>blatAssociation</code>'s DAO.
     */
    protected ubic.gemma.model.genome.sequenceAnalysis.BlatAssociationDao getBlatAssociationDao() {
        return this.blatAssociationDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)}
     */
    protected abstract ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation handleCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.biosequence.BioSequence)}
     */
    protected abstract java.util.Collection<BlatAssociation> handleFind(
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #find(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection<BlatAssociation> handleFind( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<BlatAssociation> blatAssociations )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)}
     */
    protected abstract void handleThaw( ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation blatAssociation )
            throws java.lang.Exception;

}