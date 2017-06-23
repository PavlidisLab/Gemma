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
package ubic.gemma.persistence.service.analysis.expression.diff;

import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.persistence.service.BaseDao;

/**
 * @see ExpressionAnalysisResultSet
 */
public interface ExpressionAnalysisResultSetDao extends BaseDao<ExpressionAnalysisResultSet> {

    ExpressionAnalysisResultSet thaw( ExpressionAnalysisResultSet resultSet );

    /**
     * @param resultSet Only thaws the factor not the probe information
     */
    void thawLite( ExpressionAnalysisResultSet resultSet );

    boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    DifferentialExpressionAnalysis thawFully( DifferentialExpressionAnalysis differentialExpressionAnalysis );

    ExpressionAnalysisResultSet thawWithoutContrasts( ExpressionAnalysisResultSet resultSet );

}
