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
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.experiment.ExperimentalFactor;

/**
 * <p>
 * Spring Service base class for
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService
 */
public abstract class DifferentialExpressionResultServiceBase implements
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService {

    @Autowired
    private ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao differentialExpressionAnalysisResultDao;

    @Autowired
    private ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService#getExperimentalFactors(java.util.Collection)
     */
    public java.util.Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> getExperimentalFactors(
            final java.util.Collection<ProbeAnalysisResult> differentialExpressionAnalysisResults ) {
        try {
            return this.handleGetExperimentalFactors( differentialExpressionAnalysisResults );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService.getExperimentalFactors(java.util.Collection differentialExpressionAnalysisResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultService#getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    public java.util.Collection<ExperimentalFactor> getExperimentalFactors(
            final ProbeAnalysisResult differentialExpressionAnalysisResult ) {
        try {
            return this.handleGetExperimentalFactors( differentialExpressionAnalysisResult );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService.getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>differentialExpressionAnalysisResult</code>'s DAO.
     */
    public void setDifferentialExpressionResultDao(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionResultDao differentialExpressionAnalysisResultDao ) {
        this.differentialExpressionAnalysisResultDao = differentialExpressionAnalysisResultDao;
    }

    /**
     * Sets the reference to <code>expressionAnalysisResultSet</code>'s DAO.
     */
    public void setExpressionAnalysisResultSetDao(
            ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao ) {
        this.expressionAnalysisResultSetDao = expressionAnalysisResultSetDao;
    }

    /**
     * @see diff.DifferentialExpressionResultService#thaw(ExpressionAnalysisResultSet)
     */
    public void thaw( final ExpressionAnalysisResultSet resultSet ) {
        try {
            this.handleThaw( resultSet );
        } catch ( Throwable th ) {
            throw new DifferentialExpressionResultServiceException(
                    "Error performing 'diff.DifferentialExpressionAnalysisResultService.thaw(ExpressionAnalysisResultSet resultSet)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>differentialExpressionAnalysisResult</code>'s DAO.
     */
    protected DifferentialExpressionResultDao getDifferentialExpressionResultDao() {
        return this.differentialExpressionAnalysisResultDao;
    }

    /**
     * Gets the reference to <code>expressionAnalysisResultSet</code>'s DAO.
     */
    protected ExpressionAnalysisResultSetDao getExpressionAnalysisResultSetDao() {
        return this.expressionAnalysisResultSetDao;
    }

    /**
     * Performs the core logic for {@link #getExperimentalFactors(java.util.Collection)}
     */
    protected abstract java.util.Map<ProbeAnalysisResult, Collection<ExperimentalFactor>> handleGetExperimentalFactors(
            java.util.Collection<ProbeAnalysisResult> differentialExpressionAnalysisResults )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getExperimentalFactors(diff.DifferentialExpressionAnalysisResult)}
     */
    protected abstract java.util.Collection<ExperimentalFactor> handleGetExperimentalFactors(
            ProbeAnalysisResult differentialExpressionAnalysisResult ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ExpressionAnalysisResultSet)}
     */
    protected abstract void handleThaw( ExpressionAnalysisResultSet resultSet ) throws java.lang.Exception;

}