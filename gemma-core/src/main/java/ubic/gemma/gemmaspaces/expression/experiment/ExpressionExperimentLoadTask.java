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
package ubic.gemma.gemmaspaces.expression.experiment;

import net.jini.space.JavaSpace;
import ubic.gemma.gemmaspaces.GemmaSpacesResult;
import ubic.gemma.gemmaspaces.GemmaSpacesTask;

/**
 * A task interface to wrap {@link ubic.gemma.model.expression.experiment.ExpressionExperiment} type jobs. Tasks of this
 * type are submitted to a {@link JavaSpace} and taken from the space by a worker, run on a compute server, and the
 * results are returned to the space.
 * 
 * @author keshav
 * @version $Id$
 */
public interface ExpressionExperimentLoadTask extends GemmaSpacesTask {

    /**
     * @param javaSpacesExpressionExperimentLoadCommand
     * @return
     */
    public GemmaSpacesResult execute(
            GemmaSpacesExpressionExperimentLoadCommand javaSpacesExpressionExperimentLoadCommand );

}
