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
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.expression.ProbeCoexpressionAnalysisService
 * @version $Id$
 * @author paul
 */
@Service
public class ProbeCoexpressionAnalysisServiceImpl implements ProbeCoexpressionAnalysisService {

    @Autowired
    private ProbeCoexpressionAnalysisDao probeCoexpressionAnalysisDao;

    @Override
    @Transactional
    public void addCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment,
            CoexpCorrelationDistribution coexpd ) {
        Collection<ProbeCoexpressionAnalysis> analyses = findByInvestigation( expressionExperiment );
        if ( analyses.size() > 1 ) {
            throw new IllegalStateException( "Multiple coexpression analyses for one experiment" );
        } else if ( analyses.isEmpty() ) {
            throw new IllegalStateException( "No coexpression analysis" );
        }
        ProbeCoexpressionAnalysis analysis = analyses.iterator().next();
        analysis.setCoexpCorrelationDistribution( coexpd );
        this.update( analysis );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ProbeCoexpressionAnalysisService#createFromValueObject(ubic.gemma.model.analysis.expression.ProbeCoexpressionAnalysis)
     */
    @Override
    @Transactional
    public ProbeCoexpressionAnalysis create(
            ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) {
        return this.getProbeCoexpressionAnalysisDao().create( probeCoexpressionAnalysis );
    }

    @Override
    @Transactional
    public void delete( ProbeCoexpressionAnalysis toDelete ) {
        this.getProbeCoexpressionAnalysisDao().remove( toDelete );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProbeCoexpressionAnalysis> findByInvestigation( Investigation investigation ) {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @SuppressWarnings("unchecked")
    @Override
    @Transactional(readOnly = true)
    public Map<Investigation, Collection<ProbeCoexpressionAnalysis>> findByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigations(
                ( Collection<Investigation> ) investigations );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProbeCoexpressionAnalysis> findByName( String name ) {
        return this.getProbeCoexpressionAnalysisDao().findByName( name );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProbeCoexpressionAnalysis> findByParentTaxon( Taxon taxon ) {
        return this.getProbeCoexpressionAnalysisDao().findByParentTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProbeCoexpressionAnalysis> findByTaxon( Taxon taxon ) {
        return this.getProbeCoexpressionAnalysisDao().findByTaxon( taxon );
    }

    @Override
    @Transactional(readOnly = true)
    public ProbeCoexpressionAnalysis findByUniqueInvestigations( Collection<? extends Investigation> investigations ) {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection<ProbeCoexpressionAnalysis> found = this.findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() ) return null;
        return found.iterator().next();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CompositeSequence> getAssayedProbes( ExpressionExperiment experiment ) {
        return this.getProbeCoexpressionAnalysisDao().getAssayedProbes( experiment );
    }

    @Override
    @Transactional(readOnly = true)
    public CoexpCorrelationDistribution getCoexpCorrelationDistribution( ExpressionExperiment expressionExperiment ) {
        return probeCoexpressionAnalysisDao.getCoexpCorrelationDistribution( expressionExperiment );
    }

    public ProbeCoexpressionAnalysisDao getProbeCoexpressionAnalysisDao() {
        return probeCoexpressionAnalysisDao;
    }

    @Override
    @Transactional(readOnly = true)
    public ProbeCoexpressionAnalysis load( Long id ) {
        return this.getProbeCoexpressionAnalysisDao().load( id );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProbeCoexpressionAnalysis> loadAll() {
        return ( Collection<ProbeCoexpressionAnalysis> ) this.getProbeCoexpressionAnalysisDao().loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProbeCoexpressionAnalysis> loadMyAnalyses() {
        return loadAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<ProbeCoexpressionAnalysis> loadMySharedAnalyses() {
        return loadAll();
    }

    @Override
    @Transactional
    public void update( ProbeCoexpressionAnalysis o ) {
        this.getProbeCoexpressionAnalysisDao().update( o );
    }

}