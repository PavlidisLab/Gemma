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

import org.springframework.stereotype.Service;

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
public class ProbeCoexpressionAnalysisServiceImpl extends
        ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysisServiceBase {

    @Override
    public Collection<CompositeSequence> getAssayedProbes( ExpressionExperiment experiment ) {
        return this.getProbeCoexpressionAnalysisDao().getAssayedProbes( experiment );
    }

    @Override
    public void update( ProbeCoexpressionAnalysis o ) {
        this.getProbeCoexpressionAnalysisDao().update( o );
    }

    /**
     * @see ubic.gemma.model.analysis.expression.ProbeCoexpressionAnalysisService#createDatabaseEntity(ubic.gemma.model.analysis.expression.ProbeCoexpressionAnalysis)
     */
    @Override
    protected ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis handleCreate(
            ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis ) {
        return this.getProbeCoexpressionAnalysisDao().create( probeCoexpressionAnalysis );
    }

    @Override
    protected void handleDelete( ProbeCoexpressionAnalysis toDelete ) {
        this.getProbeCoexpressionAnalysisDao().remove( toDelete );
    }

    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleFindByInvestigation( Investigation investigation ) {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Map<Investigation, Collection<ProbeCoexpressionAnalysis>> handleFindByInvestigations(
            Collection<? extends Investigation> investigations ) {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigations(
                ( Collection<Investigation> ) investigations );
    }

    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleFindByName( String name ) {
        return this.getProbeCoexpressionAnalysisDao().findByName( name );
    }

    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleFindByParentTaxon( Taxon taxon ) {
        return this.getProbeCoexpressionAnalysisDao().findByParentTaxon( taxon );
    }

    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleFindByTaxon( Taxon taxon ) {
        return this.getProbeCoexpressionAnalysisDao().findByTaxon( taxon );
    }

    @Override
    protected ProbeCoexpressionAnalysis handleLoad( Long id ) {
        return this.getProbeCoexpressionAnalysisDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    protected Collection<ProbeCoexpressionAnalysis> handleLoadAll() {
        return ( Collection<ProbeCoexpressionAnalysis> ) this.getProbeCoexpressionAnalysisDao().loadAll();
    }

    @Override
    public Collection<ProbeCoexpressionAnalysis> loadMyAnalyses() {
        return loadAll();
    }

    @Override
    public Collection<ProbeCoexpressionAnalysis> loadMySharedAnalyses() {
        return loadAll();
    }

    @Override
    protected ProbeCoexpressionAnalysis handleFindByUniqueInvestigations(
            Collection<? extends Investigation> investigations ) {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection<ProbeCoexpressionAnalysis> found = this.findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() ) return null;
        return found.iterator().next();
    }

}