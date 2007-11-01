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
package ubic.gemma.analysis.diff;

import ubic.basecode.util.RCommand;
import ubic.gemma.analysis.util.RCommanderWrapper;
import ubic.gemma.model.expression.analysis.ExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * An abstract analyzer to be extended by analyzers which will make use of R.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class AbstractAnalyzer {

    private RCommanderWrapper rCommanderWrapper = null;

    protected RCommand rc = null;

    /**
     * @param expressionExperiment
     * @return
     */
    public abstract ExpressionAnalysis getExpressionAnalysis( ExpressionExperiment expressionExperiment );

    public void connectToR() {
        rCommanderWrapper = new RCommanderWrapper();
        rc = rCommanderWrapper.getRCommandObject();
    }

}
