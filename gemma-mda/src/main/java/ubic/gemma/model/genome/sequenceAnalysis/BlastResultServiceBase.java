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
 * Spring Service base class for <code>ubic.gemma.model.genome.sequenceAnalysis.BlastResultService</code>, provides
 * access to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultService
 */
public abstract class BlastResultServiceBase implements ubic.gemma.model.genome.sequenceAnalysis.BlastResultService {

    @Autowired
    private ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao blastResultDao;

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultService#create(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlastResult create(
            final ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult ) {
        try {
            return this.handleCreate( blastResult );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlastResultServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlastResultService.create(ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.sequenceAnalysis.BlastResultService#remove(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)
     */
    public void remove( final ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult ) {
        try {
            this.handleRemove( blastResult );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.sequenceAnalysis.BlastResultServiceException(
                    "Error performing 'ubic.gemma.model.genome.sequenceAnalysis.BlastResultService.remove(ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>blastResult</code>'s DAO.
     */
    public void setBlastResultDao( ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao blastResultDao ) {
        this.blastResultDao = blastResultDao;
    }

    /**
     * Gets the reference to <code>blastResult</code>'s DAO.
     */
    protected ubic.gemma.model.genome.sequenceAnalysis.BlastResultDao getBlastResultDao() {
        return this.blastResultDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)}
     */
    protected abstract ubic.gemma.model.genome.sequenceAnalysis.BlastResult handleCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #remove(ubic.gemma.model.genome.sequenceAnalysis.BlastResult)}
     */
    protected abstract void handleRemove( ubic.gemma.model.genome.sequenceAnalysis.BlastResult blastResult )
            throws java.lang.Exception;

}