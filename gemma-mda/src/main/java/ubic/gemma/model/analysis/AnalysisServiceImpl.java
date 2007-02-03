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

import ubic.gemma.model.expression.analysis.ExpressionAnalysis;

/**
 * @see ubic.gemma.model.analysis.AnalysisService
 */
public class AnalysisServiceImpl
    extends ubic.gemma.model.analysis.AnalysisServiceBase
{

    @Override
    protected Map handleFindByAnalyses( Collection investigations ) throws Exception {
        return this.getAnalysisDao().findByAnalyses( investigations );
    }

    @Override
    protected Collection handleFindByAnalysis( Investigation investigation ) throws Exception {
       return this.getAnalysisDao().findByAnalysis( investigation );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#load(java.lang.Long)
     */
    protected ubic.gemma.model.analysis.Analysis handleLoad(java.lang.Long id)
        throws java.lang.Exception
    {
       return (Analysis) this.getAnalysisDao().load( id );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#loadAll()
     */
    protected java.util.Collection handleLoadAll()
        throws java.lang.Exception
    {
      return this.getAnalysisDao().loadAll();
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#create(ubic.gemma.model.analysis.Analysis)
     */
    protected ubic.gemma.model.analysis.Analysis handleCreate(ubic.gemma.model.analysis.Analysis analysis)
        throws java.lang.Exception
    {
        if ( analysis instanceof ExpressionAnalysis ) 
            return (Analysis) this.getExpressionAnalysisDao().create( (ExpressionAnalysis ) analysis );    
            
        throw new java.lang.Exception( "analysis isn't of a known type. don't know how to create."
                + analysis );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(ubic.gemma.model.analysis.Analysis)
     */
    protected void handleDelete(ubic.gemma.model.analysis.Analysis toDelete)
        throws java.lang.Exception
    {
        this.getAnalysisDao().remove( toDelete );
    }

    /**
     * @see ubic.gemma.model.analysis.AnalysisService#delete(java.lang.Long)
     */
    protected void handleDelete(java.lang.Long idToDelete)
        throws java.lang.Exception
    {
 
        this.getAnalysisDao().remove( idToDelete );
    }



}