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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;

/**
 * Spring Service base class for
 * <code>ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService</code>, provides access to all
 * services and entities referenced by this service.
 * 
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService
 * @version $Id$
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
    @Override
    public ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis create(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis ) {
        return this.handleCreate( analysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#find(ubic.gemma.model.genome.Gene,
     *      ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)
     */
    @Override
    public java.util.Collection<DifferentialExpressionAnalysis> find( final ubic.gemma.model.genome.Gene gene,
            final ExpressionAnalysisResultSet resultSet, final double threshold ) {
        return this.handleFind( gene, resultSet, threshold );

    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#findByInvestigationIds(java.util.Collection)
     */
    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            final java.util.Collection<Long> investigationIds ) {
        return this.handleFindByInvestigationIds( investigationIds );

    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<BioAssaySet> findExperimentsWithAnalyses( final ubic.gemma.model.genome.Gene gene ) {
        return this.handleFindExperimentsWithAnalyses( gene );

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
    @Override
    public void thaw( final java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        this.handleThaw( expressionAnalyses );

    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public void thaw(
            final ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.handleThaw( differentialExpressionAnalysis );

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
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis );

    @Override
    protected abstract void handleDelete( DifferentialExpressionAnalysis toDelete );

    /**
     * Performs the core logic for
     * {@link #find(ubic.gemma.model.genome.Gene, ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet, double)}
     */
    protected abstract java.util.Collection<DifferentialExpressionAnalysis> handleFind( Gene gene,
            ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * Performs the core logic for {@link #findByInvestigationIds(java.util.Collection)}
     */
    protected abstract java.util.Map<Long, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigationIds(
            java.util.Collection<Long> investigationIds );

    /**
     * Performs the core logic for {@link #findExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)}
     */
    protected abstract Collection<BioAssaySet> handleFindExperimentsWithAnalyses( ubic.gemma.model.genome.Gene gene );

    /**
     * Performs the core logic for {@link #thaw(java.util.Collection)}
     */
    protected abstract void handleThaw( java.util.Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * Performs the core logic for
     * {@link #thaw(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)}
     */
    protected abstract void handleThaw(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis differentialExpressionAnalysis );

}