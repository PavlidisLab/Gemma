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
package ubic.gemma.javaspaces;

import net.jini.space.JavaSpace;
import ubic.gemma.javaspaces.gigaspaces.ExpressionExperimentTask;
import ubic.gemma.javaspaces.gigaspaces.GigaSpacesResult;

/**
 * A task interface for all jobs that are to be executed on a compute server. Tasks of this type are submitted to a
 * {@link JavaSpace} and taken from the space by a worker, run on a compute server, and the results are returned to the
 * space.
 * <p>
 * One could extend this interface and create a specific task interface, capturing all inputs that are needed to run the
 * job on the compute server. See {@link ExpressionExperimentTask} for an example.
 * 
 * @author keshav
 * @version $Id$
 */
public interface JavaSpacesTask {

    /**
     * Methods with the name "execute" are proxied by the client (master) and run by the worker (on the compute server).
     * This method performs some action on the given {@link Object}.
     * 
     * @param expressionExperiment
     * @return Result
     */
    public GigaSpacesResult execute( Object obj );
}
