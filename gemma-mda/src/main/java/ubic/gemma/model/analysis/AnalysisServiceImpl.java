/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.model.analysis;

import java.util.Collection;
import java.util.Map;

import ubic.gemma.model.genome.Taxon;

/**
 * @see ubic.gemma.model.analysis.AnalysisService
 */
public class AnalysisServiceImpl extends ubic.gemma.model.analysis.AnalysisServiceBase {

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.analysis.AnalysisServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected Collection handleFindByName( String name ) throws Exception {
        return this.getAnalysisDao().findByName( name );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#load(java.lang.Long)
     */
    @Override
    protected ubic.gemma.model.analysis.Analysis handleLoad( java.lang.Long id ) throws java.lang.Exception {
        return ( Analysis ) this.getAnalysisDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    @Override
    protected java.util.Collection handleLoadAll() throws java.lang.Exception {
        return this.getAnalysisDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(ubic.gemma.model.analysis.Analysis)
     */
    @Override
    protected void handleDelete( ubic.gemma.model.analysis.Analysis toDelete ) throws java.lang.Exception {
        this.getAnalysisDao().remove( toDelete );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(java.lang.Long)
     */
    @Override
    protected void handleDelete( java.lang.Long idToDelete ) throws java.lang.Exception {

        this.getAnalysisDao().remove( idToDelete );
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        throw new UnsupportedOperationException( "Please call this method on a subclass" );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
        throw new UnsupportedOperationException( "Please call this method on a subclass" );
    }

    @Override
    protected Collection handleFindByTaxon( Taxon taxon ) throws Exception {
        throw new UnsupportedOperationException( "Please call this method on a subclass" );
    }

    @Override
    protected Analysis handleFindByUniqueInvestigations( Collection investigations ) throws Exception {
        throw new UnsupportedOperationException( "Please call this method on a subclass" );
    }

}