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
            ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis probeCoexpressionAnalysis )
            throws java.lang.Exception {
        return this.getProbeCoexpressionAnalysisDao().create( probeCoexpressionAnalysis );
    }

    @Override
    protected void handleDelete( ProbeCoexpressionAnalysis toDelete ) throws Exception {
        this.getProbeCoexpressionAnalysisDao().remove( toDelete );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByInvestigations( investigations );
    }

    @Override
    protected Collection handleFindByName( String name ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByName( name );
    }

    @Override
    protected Collection handleFindByParentTaxon( Taxon taxon ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByParentTaxon( taxon );
    }

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().findByTaxon( taxon );
    }

    @Override
    protected ProbeCoexpressionAnalysis handleLoad( Long id ) throws Exception {
        return this.getProbeCoexpressionAnalysisDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    protected Collection handleLoadAll() throws java.lang.Exception {
        return this.getProbeCoexpressionAnalysisDao().loadAll();
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
    protected ProbeCoexpressionAnalysis handleFindByUniqueInvestigations( Collection<Investigation> investigations )
            throws Exception {
        if ( investigations == null || investigations.isEmpty() || investigations.size() > 1 ) {
            return null;
        }
        Collection found = this.findByInvestigation( investigations.iterator().next() );
        if ( found.isEmpty() ) return null;
        return ( ProbeCoexpressionAnalysis ) found.iterator().next();
    }

}