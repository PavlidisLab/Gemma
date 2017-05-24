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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.analysis.AnalysisDaoBase;

import java.util.Collection;
import java.util.Map;

/**
 * DAO able to create, update, remove, load, and find objects of type
 * {@link DifferentialExpressionAnalysis}
 */
public abstract class DifferentialExpressionAnalysisDaoBase extends AnalysisDaoBase<DifferentialExpressionAnalysis>
        implements DifferentialExpressionAnalysisDao {

    public DifferentialExpressionAnalysisDaoBase() {
        super( DifferentialExpressionAnalysis.class );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#find(Gene, ExpressionAnalysisResultSet, double)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> find( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        return this.handleFind( gene, resultSet, threshold );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#findByInvestigationIds(Collection)
     */
    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            Collection<Long> investigationIds ) {
        return this.handleFindByInvestigationIds( investigationIds );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#findExperimentsWithAnalyses(Gene)
     */
    @Override
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {
        return this.handleFindExperimentsWithAnalyses( gene );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#thaw(Collection)
     */
    @Override
    public void thaw( final Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        this.handleThaw( expressionAnalyses );
    }

    /**
     * @see DifferentialExpressionAnalysisDao#thaw(DifferentialExpressionAnalysis)
     */
    @Override
    public void thaw( final DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.handleThaw( differentialExpressionAnalysis );
    }

    /**
     * Performs the core logic for {@link #find(Gene, ExpressionAnalysisResultSet, double)}
     */
    protected abstract Collection<DifferentialExpressionAnalysis> handleFind( Gene gene,
            ExpressionAnalysisResultSet resultSet, double threshold );

    /**
     * Performs the core logic for {@link #findByInvestigationIds(Collection)}
     */
    protected abstract Map<Long, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigationIds(
            Collection<Long> investigationIds );

    /**
     * Performs the core logic for {@link #findExperimentsWithAnalyses(Gene)}
     */
    protected abstract Collection<BioAssaySet> handleFindExperimentsWithAnalyses( Gene gene );

    /**
     * Performs the core logic for {@link #thaw(Collection)}
     */
    protected abstract void handleThaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses );

    /**
     * Performs the core logic for {@link #thaw(DifferentialExpressionAnalysis)}
     */
    protected abstract void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis );

}