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
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
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

    @Override
    public Integer countDownregulated( ExpressionAnalysisResultSet par, double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().countDownregulated( par, threshold );
    }

    @Override
    public Integer countProbesMeetingThreshold( ExpressionAnalysisResultSet ears, double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().countProbesMeetingThreshold( ears, threshold );

    }

    @Override
    public Integer countUpregulated( ExpressionAnalysisResultSet par, double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().countUpregulated( par, threshold );

    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.getDifferentialExpressionAnalysisDao().findByFactor( ef );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> getAnalyses( BioAssaySet expressionExperiment ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( expressionExperiment );
    }

    @Override
    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets( java.util.Collection<Long> resultSetIds ) {
        return null;
    }

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis ) {
        return this.getDifferentialExpressionAnalysisDao().create( analysis );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(ubic.gemma.model.analysis.Analysis)
     */
    @Override
    protected void handleDelete( DifferentialExpressionAnalysis toDelete ) {
        this.getDifferentialExpressionAnalysisDao().remove( toDelete );
    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFind( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().find( gene, resultSet, threshold );
    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByInvestigation( Investigation investigation ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map<Long, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigationIds(
            Collection<Long> investigationIds ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigationIds( investigationIds );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Investigation, Collection<DifferentialExpressionAnalysis>> handleFindByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigations(
                ( Collection<Investigation> ) investigations );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByName( String name ) {
        return this.getDifferentialExpressionAnalysisDao().findByName( name );
    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByParentTaxon( Taxon taxon ) {
        return this.getDifferentialExpressionAnalysisDao().findByParentTaxon( taxon );
    }

    @Override
    protected Collection<DifferentialExpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        return this.getDifferentialExpressionAnalysisDao().findByTaxon( taxon );
    }

    @Override
    protected DifferentialExpressionAnalysis handleFindByUniqueInvestigations(
            Collection<? extends Investigation> investigations ) {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection<DifferentialExpressionAnalysis> found = this.findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() ) return null;
        return found.iterator().next();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase#handleFindExperimentsWithAnalyses
     * (ubic.gemma.model.genome.Gene)
     */
    @Override
    protected Collection<BioAssaySet> handleFindExperimentsWithAnalyses( Gene gene ) {
        return this.getDifferentialExpressionAnalysisDao().findExperimentsWithAnalyses( gene );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#load(java.lang.Long)
     */
    @Override
    protected DifferentialExpressionAnalysis handleLoad( java.lang.Long id ) {
        return this.getDifferentialExpressionAnalysisDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    protected java.util.Collection<DifferentialExpressionAnalysis> handleLoadAll() {
        return ( Collection<DifferentialExpressionAnalysis> ) this.getDifferentialExpressionAnalysisDao().loadAll();
    }

    @Override
    protected void handleThaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        this.getDifferentialExpressionAnalysisDao().thaw( expressionAnalyses );
    }

    @Override
    protected void handleThaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.getDifferentialExpressionAnalysisDao().thaw( differentialExpressionAnalysis );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> loadMyAnalyses() {
        return this.loadAll();
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> loadMySharedAnalyses() {
        return this.loadAll();
    }

    @Override
    public void update( DifferentialExpressionAnalysis o ) {
        this.getDifferentialExpressionAnalysisDao().update( o );
    }

    @Override
    public void update( ExpressionAnalysisResultSet a ) {
        this.getExpressionAnalysisResultSetDao().update( a );

    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#getAnalyses(java.util.Collection)
     */
    @Override
    public Map<BioAssaySet, Collection<DifferentialExpressionAnalysis>> getAnalyses(
            Collection<? extends BioAssaySet> expressionExperiments ) {
        return this.getDifferentialExpressionAnalysisDao().getAnalyses( expressionExperiments );
    }

    @Override
    public Collection<DifferentialExpressionAnalysisValueObject> getAnalysisValueObjects( Long experimentId ) {
        return this.getDifferentialExpressionAnalysisDao().getAnalysisValueObjects( experimentId );
    }

    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> getAnalysisValueObjects(
            Collection<Long> experimentIds ) {
        return this.getDifferentialExpressionAnalysisDao().getAnalysisValueObjects( experimentIds );
    }

}