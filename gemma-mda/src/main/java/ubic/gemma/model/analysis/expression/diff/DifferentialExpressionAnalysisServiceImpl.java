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

import org.springframework.stereotype.Service;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;

/**
 * @author paul
 * @author keshav
 * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService
 * @version $Id$
 */
@Service
public class DifferentialExpressionAnalysisServiceImpl extends
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase {

    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().countProbesMeetingThreshold( ears, threshold );

    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.getDifferentialExpressionAnalysisDao().findByFactor( ef );
    }

    public Collection<DifferentialExpressionAnalysis> getAnalyses( ExpressionExperiment expressionExperiment ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( expressionExperiment );
    }

    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets( java.util.Collection<Long> resultSetIds ) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionAnalysis> loadMyAnalyses() {
        return this.loadAll();
    }

    @SuppressWarnings("unchecked")
    public Collection<DifferentialExpressionAnalysis> loadMySharedAnalyses() {
        return this.loadAll();
    }

    public void update( DifferentialExpressionAnalysis o ) {
        this.getDifferentialExpressionAnalysisDao().update( o );
    }

    public void update( ExpressionAnalysisResultSet a ) {
        this.getExpressionAnalysisResultSetDao().update( a );

    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis )
            throws java.lang.Exception {
        return this.getDifferentialExpressionAnalysisDao().create( analysis );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(ubic.gemma.model.analysis.Analysis)
     */
    @Override
    protected void handleDelete( DifferentialExpressionAnalysis toDelete ) throws java.lang.Exception {
        this.getDifferentialExpressionAnalysisDao().remove( toDelete );
    }

    @Override
    protected Collection handleFind( Gene gene, ExpressionAnalysisResultSet resultSet, double threshold )
            throws Exception {
        return this.getDifferentialExpressionAnalysisDao().find( gene, resultSet, threshold );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigationIds( Collection investigationIds ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigationIds( investigationIds );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigations( investigations );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected Collection handleFindByName( String name ) throws Exception {
        Collection results = this.getDifferentialExpressionAnalysisDao().findByName( name );
        //
        // DifferentialExpressionAnalysis mostRecent = null;
        //
        // // If there is more than one (against the rules at the moment) find the most recent one that matches. Perhaps
        // // the best way is to use the audit trail but would have to thaw
        // // them.
        // // Instead of thawing the audit trail just use the analysis with the largest ID as we don't update analysis
        // // currently so
        // // the same results should be returned.
        //
        // for ( Object obj : results ) {
        // DifferentialExpressionAnalysis ana = ( DifferentialExpressionAnalysis ) obj;
        //
        // if ( ana.getName().equalsIgnoreCase( name ) ) return ana;
        //
        // if ( mostRecent == null ) {
        // mostRecent = ana;
        // continue;
        // }
        //
        // if ( ana.getId() > mostRecent.getId() ) mostRecent = ana;
        // }
        //
        // return mostRecent;
        return results;
    }

    @Override
    protected Collection handleFindByParentTaxon( Taxon taxon ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByParentTaxon( taxon );
    }

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findByTaxon( taxon );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase#handleFindExperimentsWithAnalyses
     * (ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection handleFindExperimentsWithAnalyses( Gene gene ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().findExperimentsWithAnalyses( gene );
    }

    @Override
    protected Collection handleGetResultSets( ExpressionExperiment expressionExperiment ) throws Exception {
        return this.getDifferentialExpressionAnalysisDao().getResultSets( expressionExperiment );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#load(java.lang.Long)
     */
    @Override
    protected DifferentialExpressionAnalysis handleLoad( java.lang.Long id ) throws java.lang.Exception {
        return this.getDifferentialExpressionAnalysisDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getDifferentialExpressionAnalysisDao().loadAll();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase#handleThaw(java.util.Collection
     * )
     */
    @Override
    protected void handleThaw( Collection expressionAnalyses ) throws Exception {
        this.getDifferentialExpressionAnalysisDao().thaw( expressionAnalyses );
    }

    @Override
    protected void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) throws Exception {
        this.getDifferentialExpressionAnalysisDao().thaw( differentialExpressionAnalysis );
    }

    @Override
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().countDownregulated( par, threshold );
    }

    @Override
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().countUpregulated( par, threshold );

    }

    @Override
    protected DifferentialExpressionAnalysis handleFindByUniqueInvestigations( Collection<Investigation> investigations )
            throws Exception {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection found = this.findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() ) return null;
        return ( DifferentialExpressionAnalysis ) found.iterator().next();
    }

}