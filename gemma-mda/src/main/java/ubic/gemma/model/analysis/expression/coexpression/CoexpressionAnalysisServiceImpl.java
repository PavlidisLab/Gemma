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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.association.coexpression.CoexpressionService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.expression.CoexpressionAnalysisService
 * @version $Id$
 * @author paul
 */
@Service
public class CoexpressionAnalysisServiceImpl implements CoexpressionAnalysisService {

    @Autowired
    private CoexpressionAnalysisDao coexpressionAnalysisDao;

    @Autowired
    private CoexpressionService geneCoexpressionService;

    @Override
    @Transactional
    public void addCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment,
            CoexpCorrelationDistribution coexpd ) {
        Collection<CoexpressionAnalysis> analyses = findByInvestigation( expressionExperiment );
        if ( analyses.size() > 1 ) {
            throw new IllegalStateException( "Multiple coexpression analyses for one experiment" );
        } else if ( analyses.isEmpty() ) {
            throw new IllegalStateException( "No coexpression analysis" );
        }
        CoexpressionAnalysis analysis = analyses.iterator().next();
        analysis.setCoexpCorrelationDistribution( coexpd );
        this.update( analysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.CoexpressionAnalysisService#createFromValueObject(ubic.gemma.model.analysis.expression.CoexpressionAnalysis)
     */
    @Override
    @Transactional
    public CoexpressionAnalysis create( CoexpressionAnalysis probeCoexpressionAnalysis ) {
        return this.getCoexpressionAnalysisDao().create( probeCoexpressionAnalysis );
    }

    @Override
    @Transactional
    public void delete( CoexpressionAnalysis toDelete ) {
        this.geneCoexpressionService.deleteLinks( toDelete.getExperimentAnalyzed() );
        this.getCoexpressionAnalysisDao().remove( toDelete );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionAnalysis> findByInvestigation( Investigation investigation ) {
        return this.getCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Map<Investigation, Collection<CoexpressionAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return this.getCoexpressionAnalysisDao().findByInvestigations( ( Collection<Investigation> ) investigations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionAnalysis> findByName( String name ) {
        return this.getCoexpressionAnalysisDao().findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionAnalysis> findByParentTaxon( Taxon taxon ) {
        return this.getCoexpressionAnalysisDao().findByParentTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionAnalysis> findByTaxon( Taxon taxon ) {
        return this.getCoexpressionAnalysisDao().findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public CoexpressionAnalysis findByUniqueInvestigations( Collection<? extends Investigation> investigations ) {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection<CoexpressionAnalysis> found = this.findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() ) return null;
        return found.iterator().next();
    }

    @Override
    @Transactional(readOnly = true)
    public CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment ) {
        return coexpressionAnalysisDao.getCoexpCorrelationDistribution( expressionExperiment );
    }

    public CoexpressionAnalysisDao getCoexpressionAnalysisDao() {
        return coexpressionAnalysisDao;
    }

    @Override
    @Transactional(readOnly = true)
    public CoexpressionAnalysis load( Long id ) {
        return this.getCoexpressionAnalysisDao().load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionAnalysis> loadAll() {
        return ( Collection<CoexpressionAnalysis> ) this.getCoexpressionAnalysisDao().loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionAnalysis> loadMyAnalyses() {
        return loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionAnalysis> loadMySharedAnalyses() {
        return loadAll();
    }

    @Override
    @Transactional
    public void update( CoexpressionAnalysis o ) {
        this.getCoexpressionAnalysisDao().update( o );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisService#getExperimentsWithAnalysis()
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Taxon taxon ) {
        Collection<Long> haveCoexpressionAnalysis = new HashSet<>();
        Collection<CoexpressionAnalysis> analyses = findByTaxon( taxon );
        for ( CoexpressionAnalysis a : analyses ) {
            haveCoexpressionAnalysis.add( a.getExperimentAnalyzed().getId() );
        }
        return haveCoexpressionAnalysis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisService#getExperimentsWithAnalysis(java
     * .util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<Long> getExperimentsWithAnalysis( Collection<Long> idsToFilter ) {
        return this.getCoexpressionAnalysisDao().getExperimentsWithAnalysis( idsToFilter );
    }

}