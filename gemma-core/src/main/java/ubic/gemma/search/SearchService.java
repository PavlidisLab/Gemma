/*
 * The Gemma project
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

package ubic.gemma.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.compass.core.Compass;
import org.compass.core.CompassCallback;
import org.compass.core.CompassDetachedHits;
import org.compass.core.CompassException;
import org.compass.core.CompassHit;
import org.compass.core.CompassHits;
import org.compass.core.CompassQuery;
import org.compass.core.CompassSession;
import org.compass.core.CompassTemplate;
import org.compass.core.CompassTransaction;
import org.compass.spring.web.mvc.CompassSearchResults;

import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * This a service class used for preforming searches.  there are two kinds of searches available, 
 * percise db searchs looking for specific exact mathces in the db and the compass/lucene style searches 
 *
 * <hr>
 * <p>Copyright (c) 2006 UBC Pavlab
 * @author klc
 * @version $Id$
 * 
 * @spring.bean id="searchService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="geneProductService" ref="geneProductService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="geneBean" ref="compassGene"
 * @spring.property name="eeBean" ref="compassExpression"
 */


public class SearchService {

    private static Log log = LogFactory.getLog( SearchService.class.getName() );

    
    
    private GeneService geneService;
    private GeneProductService geneProductService;
    private CompositeSequenceService compositeSequenceService;
    private BioSequenceService bioSequenceService;
    private Compass geneBean;
    private Compass eeBean;
    private Compass arrayBean;

    
    
    /**
     *  searchs the DB for genes that exactly match the given search string
     *  searches geneProducts, gene and bioSequence tables
     *  
     * @param searchString
     * @return
     * @throws Exception
     */
    public List<Gene> geneDbSearch( String searchString ) throws Exception {

        // search by inexact symbol
        Set<Gene> geneSet = new HashSet<Gene>();
        Set<Gene> geneMatch = new HashSet<Gene>();
        Set<Gene> aliasMatch = new HashSet<Gene>();
        Set<Gene> geneProductMatch = new HashSet<Gene>();
        Set<Gene> bioSequenceMatch = new HashSet<Gene>();

        geneMatch.addAll( geneService.findByOfficialSymbolInexact( searchString ) );
        aliasMatch.addAll( geneService.getByGeneAlias( searchString ) );

        geneProductMatch.addAll( geneProductService.getGenesByName( searchString ) );
        geneProductMatch.addAll( geneProductService.getGenesByNcbiId( searchString ) );

        bioSequenceMatch.addAll( bioSequenceService.getGenesByAccession( searchString ) );
        bioSequenceMatch.addAll( bioSequenceService.getGenesByName( searchString ) );

        geneSet.addAll( geneMatch );
        geneSet.addAll( aliasMatch );
        geneSet.addAll( geneProductMatch );
        geneSet.addAll( bioSequenceMatch );

        List<Gene> geneList = new ArrayList<Gene>( geneSet );
        Comparator<Gene> comparator = new GeneComparator();
        Collections.sort( geneList, comparator );

        return geneList;

    }
    
    /**
     * 
     *An inner class used for the ordering of genes
     * <hr>
     * <p>Copyright (c) 2006 UBC Pavlab
     * @author klc
     * @version $Id$
     */
    
    class GeneComparator implements Comparator<Gene> {

        public int compare( Gene arg0, Gene arg1 ) {
            Gene obj0 = arg0;
            Gene obj1 = arg1;

            return obj0.getName().compareTo( obj1.getName() );
        }
    }

    
    /**
     * @param query
     * @return
     */
    public List<Gene> compassGeneSearch(final String query){
        
        CompassSearchResults searchResults;
        
        CompassTemplate template = new CompassTemplate(geneBean);
        
        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );
        
        return convert2GeneList(searchResults.getHits());
    }
    
    //fixme:  there should be a static method in the java package to do this.  Just need to find it. :)
    protected List<Gene> convert2GeneList( CompassHit[] anArray ) {

        ArrayList<Gene> converted = new ArrayList<Gene>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( (Gene) anArray[i].getData() );
            

        return converted;

    }
    
    protected List<ExpressionExperiment> convert2ExpressionList( CompassHit[] anArray ) {

        ArrayList<ExpressionExperiment> converted = new ArrayList<ExpressionExperiment>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( (ExpressionExperiment) anArray[i].getData() );
            

        return converted;

    }
    
  public List<ExpressionExperiment> compassExpressionSearch(final String query){
        
        CompassSearchResults searchResults;
        
        CompassTemplate template = new CompassTemplate(eeBean);
        
        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );
        
        return convert2ExpressionList(searchResults.getHits());
    }

    
    protected CompassSearchResults performSearch( String query, CompassSession session ) {
        long time = System.currentTimeMillis();
        
        assert StringUtils.isBlank( query );       
        CompassQuery compassQuery = session.queryBuilder().queryString( query.trim() ).toQuery();
        
        CompassHits hits = compassQuery.hits();
        CompassDetachedHits detachedHits = hits.detach();    
        time = System.currentTimeMillis() - time;
        CompassSearchResults searchResults = new CompassSearchResults( detachedHits.getHits(), time );

        return searchResults;
    }
    
    
    
    /**
     * @return the bioSequenceService
     */
    public BioSequenceService getBioSequenceService() {
        return bioSequenceService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @return the compositeSequenceService
     */
    public CompositeSequenceService getCompositeSequenceService() {
        return compositeSequenceService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @return the geneProductService
     */
    public GeneProductService getGeneProductService() {
        return geneProductService;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @return the geneBean
     */
    public Compass getGeneBean() {
        return geneBean;
    }

    /**
     * @param geneBean the geneBean to set
     */
    public void setGeneBean( Compass geneBean ) {
        this.geneBean = geneBean;
    }


    /**
     * @return the eeBean
     */
    public Compass getEeBean() {
        return eeBean;
    }


    /**
     * @param eeBean the eeBean to set
     */
    public void setEeBean( Compass eeBean ) {
        this.eeBean = eeBean;
    }

    /**
     * @return the arrayBean
     */
    public Compass getArrayBean() {
        return arrayBean;
    }

    /**
     * @param arrayBean the arrayBean to set
     */
    public void setArrayBean( Compass arrayBean ) {
        this.arrayBean = arrayBean;
    }
    
}
