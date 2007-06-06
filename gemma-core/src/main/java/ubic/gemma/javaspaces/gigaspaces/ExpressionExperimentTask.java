/*
 * The Gemma project
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
package ubic.gemma.javaspaces.gigaspaces;

import net.jini.space.JavaSpace;
import ubic.gemma.javaspaces.JavaSpacesTask;
import ubic.gemma.loader.expression.geo.model.GeoDataset;

/**
 * A task interface to wrap {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} type jobs. Tasks of this
 * type are submitted to a {@link JavaSpace} and taken from the space by a worker, run on a compute server, and the
 * results are returned to the space.
 * 
 * @author keshav
 * @version $Id$
 */
public interface ExpressionExperimentTask extends JavaSpacesTask {

    /**
     * Methods with the name "execute" are proxied by the client (master) and run by the worker (on the compute server).
     * This method is useful for invoking methods from {@link GeoDataset}.
     * 
     * @param geoAccession
     * @param loadPlatformOnly
     * @param doSampleMatching
     * @param aggressiveQtRemoval
     * @return
     */
    public GigaSpacesResult execute( String geoAccession, boolean loadPlatformOnly, boolean doSampleMatching,
            boolean aggressiveQtRemoval );

    // FIXME Just take in the command object ExpressionExperimentLoadCommand ... seems more robust

    /**
     * Methods with the name "execute" are proxied by the client (master) and run by the worker (on the compute server).
     * This method is useful for invoking methods from {@link GeoDataset}.
     * 
     * @param taskId
     * @param geoAccession
     * @param loadPlatformOnly
     * @param doSampleMatching
     * @param aggressiveQtRemoval
     * @return Result
     */
    public GigaSpacesResult execute( String taskId, String geoAccession, boolean loadPlatformOnly,
            boolean doSampleMatching, boolean aggressiveQtRemoval );
    // TODO remove this - shouldn't be passing in the taskId

}
