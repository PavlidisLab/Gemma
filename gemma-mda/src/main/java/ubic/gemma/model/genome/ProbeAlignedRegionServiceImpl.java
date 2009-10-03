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
package ubic.gemma.model.genome;

import java.util.Collection;

import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;

/**
 * @see ubic.gemma.model.genome.ProbeAlignedRegionService
 */
public class ProbeAlignedRegionServiceImpl extends ubic.gemma.model.genome.ProbeAlignedRegionServiceBase {

    @SuppressWarnings("unchecked")
    @Override
    protected Collection<ProbeAlignedRegion> handleFindAssociations( BlatResult blatResult ) throws Exception {
        return this.getProbeAlignedRegionDao().find( blatResult );
    }

    /**
     * @see ubic.gemma.model.genome.ProbeAlignedRegionService#findAssociations(ubic.gemma.model.genome.PhysicalLocation)
     */
    @Override
    protected java.util.Collection<ProbeAlignedRegion> handleFindAssociations(
            ubic.gemma.model.genome.PhysicalLocation physicalLocation ) throws java.lang.Exception {
        return this.getProbeAlignedRegionDao().findByPhysicalLocation( physicalLocation );
    }

    public void thaw( ProbeAlignedRegion par ) {
        this.getProbeAlignedRegionDao().thaw( par );
    }

}