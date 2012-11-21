/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.expression.diff;

import java.util.Collection;

import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;

/**
 * Used to perform meta-analyses of complete data sets (actually result sets), select the top genes, and potentially
 * store the results.
 * 
 * @author Paul
 * @version $Id$
 */
public interface DiffExMetaAnalyzerService {

    /**
     * @param analysisResultSetIds
     * @param name
     * @param description
     * @return
     */
    public GeneDifferentialExpressionMetaAnalysis analyze( Collection<Long> analysisResultSetIds, String name,
            String description );

}
