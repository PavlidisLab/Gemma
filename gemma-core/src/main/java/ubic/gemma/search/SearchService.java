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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * This a service class used for preforming searches. there are two kinds of searches available, percise db searchs
 * looking for specific exact mathces in the db and the compass/lucene style searches
 * 
 * @author klc
 * @author paul
 * @version $Id$
 * @spring.bean id="searchService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="geneProductService" ref="geneProductService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"
 * @spring.property name="geneBean" ref="compassGene"
 * @spring.property name="eeBean" ref="compassExpression"
 * @spring.property name="arrayBean" ref="compassArray"
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
     * combines the compass style search and the db style search and returns 1 combined list with no duplicates.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    public List<Gene> geneSearch( String searchString ) throws Exception {

        List<Gene> geneDbList = geneDbSearch( searchString );
        List<Gene> geneCompassList = compassGeneSearch( searchString );

        // If either search has no results return the other list
        if ( geneDbList.isEmpty() ) return geneCompassList;

        if ( geneCompassList.isEmpty() ) return geneDbList;

        // Both searchs aren't empty at this point. so just check for duplicates
        List<Gene> combinedGeneList = new ArrayList<Gene>();
        combinedGeneList.addAll( geneDbList );

        for ( Gene gene : geneCompassList ) {
            if ( !geneDbList.contains( gene ) ) combinedGeneList.add( gene );
        }

        // returned combined list
        return combinedGeneList;
    }

    /**
     * searchs the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
     * tables
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public List<Gene> geneDbSearch( String searchString ) throws Exception {

        // search by inexact symbol
        Set<Gene> geneSet = new HashSet<Gene>();
        Set<Gene> geneMatch = new HashSet<Gene>();
        Set<Gene> aliasMatch = new HashSet<Gene>();
        Set<Gene> geneProductMatch = new HashSet<Gene>();
        Set<Gene> bioSequenceMatch = new HashSet<Gene>();

        // replace * with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );

        geneMatch.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );
        aliasMatch.addAll( geneService.getByGeneAlias( inexactString ) );

        geneProductMatch.addAll( geneProductService.getGenesByName( inexactString ) );
        geneProductMatch.addAll( geneProductService.getGenesByNcbiId( inexactString ) );

        bioSequenceMatch.addAll( bioSequenceService.getGenesByAccession( inexactString ) );
        bioSequenceMatch.addAll( bioSequenceService.getGenesByName( inexactString ) );

        geneSet.addAll( geneMatch );
        geneSet.addAll( aliasMatch );
        geneSet.addAll( geneProductMatch );
        geneSet.addAll( bioSequenceMatch );

        List<Gene> geneList = new ArrayList<Gene>( geneSet );
        Comparator<Describable> comparator = new DescribableComparator();
        Collections.sort( geneList, comparator );

        return geneList;

    }

    /**
     * Partial implementation
     * 
     * @param searchString
     * @param arrayDesign
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public List<CompositeSequence> compositeSequenceSearch( String searchString, ArrayDesign arrayDesign ) {
        List<CompositeSequence> allResults = new ArrayList<CompositeSequence>();
        if ( StringUtils.isBlank( searchString ) ) return allResults;
        Collection<CompositeSequence> nameMatch;

        String cleanedSearchString = StringUtils.strip( searchString );

        if ( arrayDesign == null ) {
            nameMatch = compositeSequenceService.findByName( cleanedSearchString );
            allResults.addAll( nameMatch );
        } else {
            assert arrayDesign.getId() != null;
            CompositeSequence res = compositeSequenceService.findByName( arrayDesign, searchString );
            if ( res != null ) allResults.add( res );
        }

        Collections.sort( allResults, new DescribableComparator() );
        return allResults;
    }

    /**
     * An inner class used for the ordering of genes
     */
    class DescribableComparator implements Comparator<Describable> {
        public int compare( Describable arg0, Describable arg1 ) {
            return arg0.getName().compareTo( arg1.getName() );
        }
    }

    /**
     * @param query
     * @return
     */
    public List<Gene> compassGeneSearch( final String query ) {

        CompassSearchResults searchResults;

        CompassTemplate template = new CompassTemplate( geneBean );

        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );

        return convert2GeneList( searchResults.getHits() );
    }

    /**
     * @param anArray
     * @return
     */
    protected List<Gene> convert2GeneList( CompassHit[] anArray ) {

        ArrayList<Gene> converted = new ArrayList<Gene>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( ( Gene ) anArray[i].getData() );

        return converted;

    }

    /**
     * @param anArray
     * @return
     */
    protected List<ExpressionExperiment> convert2ExpressionList( CompassHit[] anArray ) {

        ArrayList<ExpressionExperiment> converted = new ArrayList<ExpressionExperiment>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( ( ExpressionExperiment ) anArray[i].getData() );

        return converted;

    }

    /**
     * does a compass style search on expressionExperiments
     * 
     * @param query
     * @return
     */
    public List<ExpressionExperiment> compassExpressionSearch( final String query ) {

        CompassSearchResults searchResults;

        CompassTemplate template = new CompassTemplate( eeBean );

        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );

        return convert2ExpressionList( searchResults.getHits() );
    }

    /**
     * Does a compass style search on ArrayDesigns
     * 
     * @param query
     * @return
     */
    public List<ArrayDesign> compassArrayDesignSearch( final String query ) {

        CompassSearchResults searchResults;

        CompassTemplate template = new CompassTemplate( arrayBean );

        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );

        return convert2ArrayDesignList( searchResults.getHits() );
    }

    /**
     * @param anArray
     * @return
     */
    protected List<ArrayDesign> convert2ArrayDesignList( CompassHit[] anArray ) {

        ArrayList<ArrayDesign> converted = new ArrayList<ArrayDesign>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( ( ArrayDesign ) anArray[i].getData() );

        return converted;

    }

    /**
     * @param query
     * @param session
     * @return
     */
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
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param geneBean the geneBean to set
     */
    public void setGeneBean( Compass geneBean ) {
        this.geneBean = geneBean;
    }

    /**
     * @param eeBean the eeBean to set
     */
    public void setEeBean( Compass eeBean ) {
        this.eeBean = eeBean;
    }

    /**
     * @param arrayBean the arrayBean to set
     */
    public void setArrayBean( Compass arrayBean ) {
        this.arrayBean = arrayBean;
    }

}
