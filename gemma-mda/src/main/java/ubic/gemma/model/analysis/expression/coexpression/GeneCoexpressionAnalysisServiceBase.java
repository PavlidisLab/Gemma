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
package ubic.gemma.model.analysis.expression.coexpression;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * <p>
 * Spring Service base class for
 * <code>ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService</code>, provides access to
 * all services and entities referenced by this service.
 * </p>
 * 
 * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService
 */
public abstract class GeneCoexpressionAnalysisServiceBase extends
        ubic.gemma.model.analysis.AnalysisServiceImpl<GeneCoexpressionAnalysis> implements
        ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService {

    @Autowired
    private ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao;

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService#create(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    @Override
    public ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis create(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis ) {
        try {
            return this.handleCreate( analysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService.create(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService#getDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    @Override
    public java.util.Collection<ExpressionExperiment> getDatasetsAnalyzed(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis ) {
        try {
            return this.handleGetDatasetsAnalyzed( analysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService.getDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService#getNumDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    @Override
    public int getNumDatasetsAnalyzed(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis ) {
        try {
            return this.handleGetNumDatasetsAnalyzed( analysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService.getNumDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis)' --> "
                            + th, th );
        }
    }

    /**
     * Sets the reference to <code>geneCoexpressionAnalysis</code>'s DAO.
     */
    public void setGeneCoexpressionAnalysisDao(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao geneCoexpressionAnalysisDao ) {
        this.geneCoexpressionAnalysisDao = geneCoexpressionAnalysisDao;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService#thaw(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    @Override
    public void thaw(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis ) {
        try {
            this.handleThaw( geneCoexpressionAnalysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService.thaw(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService#update(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)
     */
    @Override
    public void update(
            final ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoExpressionAnalysis ) {
        try {
            this.handleUpdate( geneCoExpressionAnalysis );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisServiceException(
                    "Error performing 'ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisService.update(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoExpressionAnalysis)' --> "
                            + th, th );
        }
    }

    /**
     * Gets the reference to <code>geneCoexpressionAnalysis</code>'s DAO.
     */
    protected ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysisDao getGeneCoexpressionAnalysisDao() {
        return this.geneCoexpressionAnalysisDao;
    }

    /**
     * Performs the core logic for
     * {@link #create(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract java.util.Collection<ExpressionExperiment> handleGetDatasetsAnalyzed(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #getNumDatasetsAnalyzed(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract int handleGetNumDatasetsAnalyzed(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis analysis )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #thaw(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract void handleThaw(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoexpressionAnalysis )
            throws java.lang.Exception;

    /**
     * Performs the core logic for
     * {@link #update(ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis)}
     */
    protected abstract void handleUpdate(
            ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionAnalysis geneCoExpressionAnalysis )
            throws java.lang.Exception;

}