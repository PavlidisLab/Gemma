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
package ubic.gemma.model.genome;

/**
 * <p>
 * Spring Service base class for <code>ubic.gemma.model.genome.ProbeAlignedRegionService</code>, provides access to all
 * services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.genome.ProbeAlignedRegionService
 */
public abstract class ProbeAlignedRegionServiceBase implements ubic.gemma.model.genome.ProbeAlignedRegionService {

    private ubic.gemma.model.genome.ProbeAlignedRegionDao probeAlignedRegionDao;

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionService#findAssociations(ubic.gemma.model.genome.PhysicalLocation)
     */
    public java.util.Collection findAssociations( final ubic.gemma.model.genome.PhysicalLocation physicalLocation ) {
        try {
            return this.handleFindAssociations( physicalLocation );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.ProbeAlignedRegionServiceException(
                    "Error performing 'ubic.gemma.model.genome.ProbeAlignedRegionService.findAssociations(ubic.gemma.model.genome.PhysicalLocation physicalLocation)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionService#findAssociations(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)
     */
    public java.util.Collection findAssociations( final ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) {
        try {
            return this.handleFindAssociations( blatResult );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.genome.ProbeAlignedRegionServiceException(
                    "Error performing 'ubic.gemma.model.genome.ProbeAlignedRegionService.findAssociations(ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>probeAlignedRegion</code>'s DAO.
     */
    public void setProbeAlignedRegionDao( ubic.gemma.model.genome.ProbeAlignedRegionDao probeAlignedRegionDao ) {
        this.probeAlignedRegionDao = probeAlignedRegionDao;
    }

    /**
     * Gets the reference to <code>probeAlignedRegion</code>'s DAO.
     */
    protected ubic.gemma.model.genome.ProbeAlignedRegionDao getProbeAlignedRegionDao() {
        return this.probeAlignedRegionDao;
    }

    /**
     * Performs the core logic for {@link #findAssociations(ubic.gemma.model.genome.PhysicalLocation)}
     */
    protected abstract java.util.Collection handleFindAssociations(
            ubic.gemma.model.genome.PhysicalLocation physicalLocation ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findAssociations(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)}
     */
    protected abstract java.util.Collection handleFindAssociations(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult ) throws java.lang.Exception;

}