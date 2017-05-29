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

import org.hibernate.SessionFactory;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult</code>.
 * </p>
 *
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult
 */
public abstract class DifferentialExpressionResultDaoBase extends AbstractDao<DifferentialExpressionAnalysisResult>
        implements DifferentialExpressionResultDao {

    public DifferentialExpressionResultDaoBase( SessionFactory sessionFactory ) {
        super( DifferentialExpressionAnalysisResult.class, sessionFactory );
    }

    /**
     * @see DifferentialExpressionResultDao#getExperimentalFactors(Collection)
     */
    @Override
    public Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            final Collection<DifferentialExpressionAnalysisResult> DifferentialExpressionAnalysisResults ) {

        return this.handleGetExperimentalFactors( DifferentialExpressionAnalysisResults );

    }

    /**
     * @see DifferentialExpressionResultDao#getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    @Override
    public Collection<ExperimentalFactor> getExperimentalFactors(
            final DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult ) {

        return this.handleGetExperimentalFactors( DifferentialExpressionAnalysisResult );

    }

    /**
     * Performs the core logic for {@link #getExperimentalFactors(Collection)}
     */
    protected abstract Map<DifferentialExpressionAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            Collection<DifferentialExpressionAnalysisResult> DifferentialExpressionAnalysisResults );

    /**
     * Performs the core logic for {@link #getExperimentalFactors(DifferentialExpressionAnalysisResult)}
     */
    protected abstract Collection<ExperimentalFactor> handleGetExperimentalFactors(
            DifferentialExpressionAnalysisResult DifferentialExpressionAnalysisResult );

}