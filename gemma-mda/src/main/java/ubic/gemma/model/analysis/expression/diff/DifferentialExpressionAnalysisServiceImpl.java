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
public class DifferentialExpressionAnalysisServiceImpl implements
        ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService {

    @Autowired
    private ExpressionAnalysisResultSetDao expressionAnalysisResultSetDao;

    @Autowired
    private ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao differentialExpressionAnalysisDao;

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

    /**
     * @see ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisService#create(ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis)
     */
    @Override
    public ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis create(
            ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis analysis ) {
        return this.getDifferentialExpressionAnalysisDao().create( analysis );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(ubic.gemma.model.analysis.Analysis)
     */
    @Override
    public void delete( DifferentialExpressionAnalysis toDelete ) {
        this.getDifferentialExpressionAnalysisDao().remove( toDelete );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> find( Gene gene, ExpressionAnalysisResultSet resultSet,
            double threshold ) {
        return this.getDifferentialExpressionAnalysisDao().find( gene, resultSet, threshold );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByFactor( ExperimentalFactor ef ) {
        return this.getDifferentialExpressionAnalysisDao().findByFactor( ef );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByInvestigation( Investigation investigation ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    public Map<Long, Collection<DifferentialExpressionAnalysis>> findByInvestigationIds(
            Collection<Long> investigationIds ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigationIds( investigationIds );
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<Investigation, Collection<DifferentialExpressionAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigations(
                ( Collection<Investigation> ) investigations );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisServiceBase#findByName(java.lang.String)
     */
    @Override
    public Collection<DifferentialExpressionAnalysis> findByName( String name ) {
        return this.getDifferentialExpressionAnalysisDao().findByName( name );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByParentTaxon( Taxon taxon ) {
        return this.getDifferentialExpressionAnalysisDao().findByParentTaxon( taxon );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> findByTaxon( Taxon taxon ) {
        return this.getDifferentialExpressionAnalysisDao().findByTaxon( taxon );
    }

    @Override
    public DifferentialExpressionAnalysis findByUniqueInvestigations( Collection<? extends Investigation> investigations ) {
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
     * ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisServiceBase#findExperimentsWithAnalyses
     * (ubic.gemma.model.genome.Gene)
     */
    @Override
    public Collection<BioAssaySet> findExperimentsWithAnalyses( Gene gene ) {
        return this.getDifferentialExpressionAnalysisDao().findExperimentsWithAnalyses( gene );
    }

    @Override
    public Collection<DifferentialExpressionAnalysis> getAnalyses( BioAssaySet expressionExperiment ) {
        return this.getDifferentialExpressionAnalysisDao().findByInvestigation( expressionExperiment );
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
    public Map<Long, Collection<DifferentialExpressionAnalysisValueObject>> getAnalysisValueObjects(
            Collection<Long> experimentIds ) {
        return this.getDifferentialExpressionAnalysisDao().getAnalysisValueObjects( experimentIds );
    }

    @Override
    public Collection<DifferentialExpressionAnalysisValueObject> getAnalysisValueObjects( Long experimentId ) {
        return this.getDifferentialExpressionAnalysisDao().getAnalysisValueObjects( experimentId );
    }

    /**
     * Gets the reference to <code>differentialExpressionAnalysis</code>'s DAO.
     */
    public ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisDao getDifferentialExpressionAnalysisDao() {
        return this.differentialExpressionAnalysisDao;
    }

    /**
     * @return the expressionAnalysisResultSetDao
     */
    public ExpressionAnalysisResultSetDao getExpressionAnalysisResultSetDao() {
        return expressionAnalysisResultSetDao;
    }

    @Override
    public java.util.Collection<ExpressionAnalysisResultSet> getResultSets( java.util.Collection<Long> resultSetIds ) {
        return null;
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#load(java.lang.Long)
     */
    @Override
    public DifferentialExpressionAnalysis load( java.lang.Long id ) {
        return this.getDifferentialExpressionAnalysisDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    public java.util.Collection<DifferentialExpressionAnalysis> loadAll() {
        return ( Collection<DifferentialExpressionAnalysis> ) this.getDifferentialExpressionAnalysisDao().loadAll();
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
    public void thaw( Collection<DifferentialExpressionAnalysis> expressionAnalyses ) {
        this.getDifferentialExpressionAnalysisDao().thaw( expressionAnalyses );
    }

    @Override
    public void thaw( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        this.getDifferentialExpressionAnalysisDao().thaw( differentialExpressionAnalysis );
    }

    @Override
    public void update( DifferentialExpressionAnalysis o ) {
        this.getDifferentialExpressionAnalysisDao().update( o );
    }

    @Override
    public void update( ExpressionAnalysisResultSet a ) {
        this.getExpressionAnalysisResultSetDao().update( a );

    }

    @Override
    public boolean canDelete( DifferentialExpressionAnalysis differentialExpressionAnalysis ) {
        return this.getExpressionAnalysisResultSetDao().canDelete( differentialExpressionAnalysis );
    }

}