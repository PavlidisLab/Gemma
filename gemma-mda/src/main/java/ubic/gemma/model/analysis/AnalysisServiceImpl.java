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
import java.util.Date;
import java.util.Map;

import ubic.gemma.model.expression.analysis.ExpressionAnalysis;

/**
 * @see ubic.gemma.model.analysis.AnalysisService
 */
public class AnalysisServiceImpl
    extends ubic.gemma.model.analysis.AnalysisServiceBase
{

    /* (non-Javadoc)
     * @see ubic.gemma.model.analysis.AnalysisServiceBase#handleFindByName(java.lang.String)
     */
    @Override
    protected Analysis handleFindByName( String name ) throws Exception {
       Collection results = this.getAnalysisDao().findByName( name + "?" );
       
       Analysis mostRecent = null;
       
       //find the most recent one that matches
       for (Object obj: results){
           Analysis ana = (Analysis) obj;
           
           if (mostRecent == null){
               mostRecent = ana;
               continue;
           }
           
           Date current = ana.getAuditTrail().getLast().getDate();
           if (current.after( mostRecent.getAuditTrail().getLast().getDate() ))
               mostRecent = ana;           
       }
       
       return mostRecent;
    }

    @Override
    protected Collection handleFindByInvestigation( Investigation investigation ) throws Exception {
        return this.getAnalysisDao().findByInvestigation( investigation );
    }

    @Override
    protected Map handleFindByInvestigations( Collection investigations ) throws Exception {
       return this.getAnalysisDao().findByInvestigations( investigations );
    }

    @Override
    protected Analysis handleFindByUniqueInvestigations( Collection Investigations ) throws Exception {

        Map<Analysis, Collection<Investigation>> anas = this.getAnalysisDao().findByInvestigations( Investigations );
        
        for (Analysis ana: anas.keySet()){
            
            Collection<Investigation> foundInvestigations = anas.get( ana );
                       
                if (Investigations.size() == foundInvestigations.size()){
                    if (Investigations.containsAll( foundInvestigations ))
                            return ana;
                
            }
        }
        
        return null;
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