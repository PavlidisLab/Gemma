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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.analysis;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult;

/**
 * @author keshav
 * @version $Id$
 * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisResultService
 */
public class DifferentialExpressionAnalysisResultServiceImpl extends
        ubic.gemma.model.analysis.DifferentialExpressionAnalysisResultServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisResultServiceBase#handleGetFactorValues(ubic.gemma.model.expression.analysis.DifferentialExpressionAnalysisResult)
     */
    @Override
    protected Collection handleGetFactorValues(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) throws Exception {
        return this.getDifferentialExpressionAnalysisResultDao().getFactorValues( differentialExpressionAnalysisResult );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.DifferentialExpressionAnalysisResultServiceBase#handleGetFactorValues(java.util.Collection)
     */
    @Override
    protected Map handleGetFactorValues( Collection differentialExpressionAnalysisResults ) throws Exception {
        return this.getDifferentialExpressionAnalysisResultDao()
                .getFactorValues( differentialExpressionAnalysisResults );
    }

}