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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.persistence.service.AbstractService;

import java.util.Collection;
import java.util.Map;

/**
 * Spring Service base class for <code>DifferentialExpressionResultService</code>, provides access to all services and
 * entities referenced by this service.
 *
 * @see DifferentialExpressionResultService
 */
public abstract class DifferentialExpressionResultServiceBase
        extends AbstractService<DifferentialExpressionAnalysisResult> implements DifferentialExpressionResultService {

    final DifferentialExpressionResultDao DERDao;

    final ExpressionAnalysisResultSetDao EARDao;

    @Autowired
    public DifferentialExpressionResultServiceBase( DifferentialExpressionResultDao DERDao,
            ExpressionAnalysisResultSetDao EARDao ) {
        super( DERDao );
        this.DERDao = DERDao;
        this.EARDao = EARDao;
    }

    /**
     * @see DifferentialExpressionResultService#getExperimentalFactors(DifferentialExpressionAnalysisResult)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<ExperimentalFactor> getExperimentalFactors(
            final DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        return this.handleGetExperimentalFactors( differentialExpressionAnalysisResult );
    }

    @Override
    @Transactional(readOnly = true)
    public ExpressionAnalysisResultSet thawWithoutContrasts( ExpressionAnalysisResultSet resultSet ) {
        return this.EARDao.thawWithoutContrasts( resultSet );
    }

    protected abstract Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> differentialExpressionAnalysisResults );

    protected abstract Collection<ExperimentalFactor> handleGetExperimentalFactors(
            DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult );

}