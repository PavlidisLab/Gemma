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

import org.springframework.beans.factory.annotation.Autowired;

/**
 * <p>
 * Spring Service base class for
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService</code>, provides access to all
 * services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService
 */
public abstract class DifferentialExpressionAnalysisServiceBase extends
        ubic.gemma.model.analysis.AnalysisServiceImpl<DifferentialExpressionAnalysis> implements
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService {

    @Autowired
    ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;

    @Autowired
    private ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    public ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis create(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis ) {
        try {
            return this.handleCreate( analysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService.create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#find(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    public java.util.Collection find( final ubic.gemma.model.genome.Gene gene,
            final ExpressionAnalysisResultSet resultSet, final double threshold ) {
        try {
            return this.handleFind( gene, resultSet, threshold );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService.find(ubic.gemma.model.genome.Gene gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet resultSet, double threshold)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#findByInvestigationIds(java.util.Collection)
     */
    public java.util.Map findByInvestigationIds( final java.util.Collection investigationIds ) {
        try {
            return this.handleFindByInvestigationIds( investigationIds );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService.findByInvestigationIds(java.util.Collection investigationIds)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)
     */
    public java.util.Collection findExperimentsWithAnalyses( final ubic.gemma.model.genome.Gene gene ) {
        try {
            return this.handleFindExperimentsWithAnalyses( gene );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService.findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene gene)' --> "
                            + th, th );
        }
    }

    /**
     * @return the expressionAnalysisResultSetDao
     */
    public ExpressionAnalysisResultSetDao getExpressionAnalysisResultSetDao() {
        return expressionAnalysisResultSetDao;
    }

    /**
     * Sets the reference to <code>differentialExpressionAnalysis</code>'s DAO.
     */
    public void setDifferentialExpressionAnalysisDao(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao ) {
        this.differentialExpressionAnalysisDao = differentialExpressionAnalysisDao;
    }

    /**
     * @param expressionAnalysisResultSetDao the expressionAnalysisResultSetDao to set
     */
    public void setExpressionAnalysisResultSetDao( ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao ) {
        this.expressionAnalysisResultSetDao = expressionAnalysisResultSetDao;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#thaw(java.util.Collection)
     */
    public void thaw( final java.util.Collection expressionAnalyses ) {
        try {
            this.handleThaw( expressionAnalyses );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService.thaw(java.util.Collection expressionAnalyses)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    public void thaw(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        try {
            this.handleThaw( differentialExpressionAnalysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService.thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>differentialExpressionAnalysis</code>'s DAO.
     */
    protected ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao getDifferentialExpressionAnalysisDao() {
        return this.differentialExpressionAnalysisDao;
    }

    /**
     * Performs the core logic for
     * {@link #create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)}
     */
    protected abstract ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis )
            throws java.lang.Exception;

    @Override
    protected abstract void handleDelete( DifferentialExpressionAnalysis toDelete ) throws Exception;

    /**
     * Performs the core logic for
     * {@link #find(ubic.gemma.model.genome.Gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)}
     */
    protected abstract java.util.Collection handleFind( ubic.gemma.model.genome.Gene gene,
            ExpressionAnalysisResultSet resultSet, double threshold ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByInvestigationIds(java.util.Collection)}
     */
    protected abstract java.util.Map handleFindByInvestigationIds( java.util.Collection investigationIds )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)}
     */
    protected abstract java.util.Collection handleFindExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection expressionAnalyses ) throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)}
     */
    protected abstract void handleThaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis )
            throws java.lang.Exception;

}