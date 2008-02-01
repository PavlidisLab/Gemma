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

import ubic.gemma.model.analysis.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;

/**
 * @author keshav
 * @version $Id$
 */
public class DifferentialExpressionAnalysisConfig {

    /**
     * @return representation of this analysis (not completely filled in - only the basic parameters)
     */
    public DifferentialExpressionAnalysis toAnalysis() {
        DifferentialExpressionAnalysis analysis = DifferentialExpressionAnalysis.Factory.newInstance();
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Differential expression analysis settings" );
        protocol.setDescription( "qvalue: " + true );// TODO override toString and use this.toString
        analysis.setProtocol( protocol );
        return analysis;
    }

}
