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
 * Spring Service base class for <code>ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationService
 */
public abstract class BlastAssociationServiceBase implements
        ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationService {

    @Autowired
    private ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao blastAssociationDao;

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationService#create(ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation)
     */
    @Override
    public ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation create(
            final ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation blastAssociation ) {
        try {
            return this.handleCreate( blastAssociation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationService.create(ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation blastAssociation)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>blastAssociation</code>'s DAO.
     */
    public void setBlastAssociationDao( ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao blastAssociationDao ) {
        this.blastAssociationDao = blastAssociationDao;
    }

    /**
     * Gets the reference to <code>blastAssociation</code>'s DAO.
     */
    protected ubic.gemma.model.genome.sequenceAnalysis.BlastAssociationDao getBlastAssociationDao() {
        return this.blastAssociationDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation)}
     */
    protected abstract ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation handleCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlastAssociation blastAssociation ) throws java.lang.Exception;

}