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
/**
 * This is only generated once! It will never be overwritten.
 * You can (and have to!) safely modify it by hand.
 */
package ubic.gemma.model.analysis.expression.diff;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @author keshav
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService
 * @version $Id$
 */
public class DifferentialExpressionAnalysisServiceImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase {

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis )
            throws java.lang.Exception {
        return ( DifferentialExpressionAnalysis ) this.getDifferentialExpressionAnalysisDao().create( analysis );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigations( investigations );
    }

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByTaxon( taxon );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected DifferentialExpressionAnalysis handleFindByUniqueInvestigations( Collection investigations )
            throws Exception {

        Map<Investigation, Collection<DifferentialExpressionAnalysis>> anas = this
                .getDifferentialExpressionAnalysisDao().findByInvestigations( investigations );

        /*
         * Find an analysis that uses all the investigations.
         */

        for ( ExpressionExperiment ee : ( Collection<ExpressionExperiment> ) investigations ) {

            if ( !anas.containsKey( ee ) ) {
                return null; // then there can be none meeting the criterion.
            }

            Collection<DifferentialExpressionAnalysis> analyses = anas.get( ee );
            for ( DifferentialExpressionAnalysis a : analyses ) {
                if ( a.getExpressionExperimentSetAnalyzed().getExperiments().size() == investigations.size()
                        && a.getExpressionExperimentSetAnalyzed().getExperiments().containsAll( investigations ) ) {
                    return a;
                }
            }
        }

        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase#handleThaw(java.util.Collection )
     */
    @Override
    protected void handleThaw( Collection expressionAnalyses ) throws Exception {
        this.getDifferentialExpressionAnalysisDao().thaw( expressionAnalyses );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase#handleFindExperimentsWithAnalyses(ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindExperimentsWithAnalyses( Gene gene ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findExperimentsWithAnalyses( gene );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase#handleDelete(java.lang.Long)
     */
    @Override
    protected void handleDelete( Long idToDelete ) throws Exception {
        this.getDifferentialExpressionAnalysisDao().remove( idToDelete );
    }

    @Override
    protected Collection handleFind( Gene gene, ExpressionExperiment expressionExperiment, double threshold )
            throws Exception {
        return this.getDifferentialExpressionAnalysisDao().find( gene, expressionExperiment, threshold );
    }

    @Override
    protected void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) throws Exception {
        this.getDifferentialExpressionAnalysisDao().thaw( differentialExpressionAnalysis );
    }

    @Override
    protected Collection handleFind( Gene gene, ExpressionAnalysisResultSet resultSet, double threshold )
            throws Exception {
        return this.getDifferentialExpressionAnalysisDao().find( gene, resultSet, threshold );
    }

    @Override
    protected Collection handleGetResultSets( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().getResultSets( expressionExperiment );
    }

    @Override
    protected Map handleFindByInvestigationIds( Collection investigationIds ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigationIds( investigationIds );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#find(ubic.gemma.model.genome.Gene,
     *      java.util.Collection)
     */
    public java.util.Map<ubic.gemma.model.expression.experiment.ExpressionExperiment, java.util.Collection<ProbeAnalysisResult>> findResultsForGeneInExperiments(
            Gene gene, Collection<ExpressionExperiment> experimentsAnalyzed ) {
        return this.getDifferentialExpressionAnalysisDao().findResultsForGeneInExperiments( gene, experimentsAnalyzed );
    }
}