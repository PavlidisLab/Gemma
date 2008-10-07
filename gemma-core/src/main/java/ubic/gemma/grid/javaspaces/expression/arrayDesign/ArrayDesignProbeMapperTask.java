/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.grid.javaspaces.expression.arrayDesign;

import net.jini.space.JavaSpace;
import ubic.gemma.grid.javaspaces.SpacesResult;
import ubic.gemma.grid.javaspaces.SpacesTask;

/**
 * A task interface to wrap array design probe mapper type jobs. Tasks of this type are submitted to a {@link JavaSpace}
 * and taken from the space by a worker, run on a compute server, and the results are returned to the space.
 * 
 * @author keshav
 * @version $Id$
 */
public interface ArrayDesignProbeMapperTask extends SpacesTask {

    /**
     * @param jsProbeMapperCommand
     * @return
     */
    public SpacesResult execute( SpacesArrayDesignProbeMapperCommand jsProbeMapperCommand );

}
