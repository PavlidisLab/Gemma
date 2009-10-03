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

/**
 * <p>
 * Spring Service base class for
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService
 */
public abstract class DifferentialExpressionAnalysisResultServiceBase implements
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService {

    private ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao differentialExpressionAnalysisResultDao;

    private ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService#getExperimentalFactors(java.util.Collection)
     */
    public java.util.Map getExperimentalFactors( final java.util.Collection differentialExpressionAnalysisResults ) {
        try {
            return this.handleGetExperimentalFactors( differentialExpressionAnalysisResults );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService.getExperimentalFactors(java.util.Collection differentialExpressionAnalysisResults)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService#getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)
     */
    public java.util.Collection getExperimentalFactors(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult ) {
        try {
            return this.handleGetExperimentalFactors( differentialExpressionAnalysisResult );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService.getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>differentialExpressionAnalysisResult</code>'s DAO.
     */
    public void setDifferentialExpressionAnalysisResultDao(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao differentialExpressionAnalysisResultDao ) {
        this.differentialExpressionAnalysisResultDao = differentialExpressionAnalysisResultDao;
    }

    /**
     * Sets the reference to <code>expressionAnalysisResultSet</code>'s DAO.
     */
    public void setExpressionAnalysisResultSetDao(
            ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao ) {
        this.expressionAnalysisResultSetDao = expressionAnalysisResultSetDao;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService#thaw(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)
     */
    public void thaw( final ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet ) {
        try {
            this.handleThaw( resultSet );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultService.thaw(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>differentialExpressionAnalysisResult</code>'s DAO.
     */
    protected ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResultDao getDifferentialExpressionAnalysisResultDao() {
        return this.differentialExpressionAnalysisResultDao;
    }

    /**
     * Gets the reference to <code>expressionAnalysisResultSet</code>'s DAO.
     */
    protected ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSetDao getExpressionAnalysisResultSetDao() {
        return this.expressionAnalysisResultSetDao;
    }

    /**
     * Performs the core logic for {@link #getExperimentalFactors(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetExperimentalFactors(
            java.util.Collection differentialExpressionAnalysisResults ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getExperimentalFactors(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult)}
     */
    protected abstract java.util.Collection handleGetExperimentalFactors(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult differentialExpressionAnalysisResult )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet)}
     */
    protected abstract void handleThaw( ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet )
            throws java.lang.Exception;

}