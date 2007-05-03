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

import ubic.gemma.analysis.service.CompositeSequenceGeneMapperService;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
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
 */
public class SearchService {

    /*
     * SearchService has not been configured with xdoclet tags because we it needs to reside in
     * applicationContext-compass.xml so we can choose whether or not to load this part of the application context at
     * spring startup.
     */
    private static Log log = LogFactory.getLog( SearchService.class.getName() );

    private GeneService geneService;
    private GeneProductService geneProductService;
    private CompositeSequenceService compositeSequenceService;
    private ExpressionExperimentService expressionExperimentService;
    private BioSequenceService bioSequenceService;
    private CompositeSequenceGeneMapperService compositeSequenceGeneMapperService;
    private Compass geneBean;
    private Compass eeBean;
    private Compass arrayBean;
    private Compass ontologyBean;

    /**
     * combines the compass style search, the db style search, and the compositeSequence search and returns 1 combined
     * list with no duplicates.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    public Collection<Gene> geneSearch( String searchString ) throws Exception {

        searchString = searchString.trim();
        Collection<Gene> geneDbList = geneDbSearch( searchString );
        Collection<Gene> geneCompassList = compassGeneSearch( searchString );
        Collection<Gene> geneCsList = geneCompositeSequenceSearch( searchString );

        // If either search has no results return the other list
        // if ( geneDbList.isEmpty() ) return geneCompassList;

        // if ( geneCompassList.isEmpty() ) return geneDbList;

        // Both searchs aren't empty at this point. so just check for duplicates
        List<Gene> combinedGeneList = new ArrayList<Gene>();
        combinedGeneList.addAll( geneDbList );

        for ( Gene gene : geneCompassList ) {
            if ( !geneDbList.contains( gene ) ) combinedGeneList.add( gene );
        }

        for ( Gene gene : geneCsList ) {
            if ( !combinedGeneList.contains( gene ) ) combinedGeneList.add( gene );
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
    public Collection<Gene> geneDbSearch( String searchString ) throws Exception {

        searchString = searchString.trim();

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
     * searchs the DB for genes that are matched by a given compositeSequence tables
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Collection<Gene> geneCompositeSequenceSearch( String searchString ) throws Exception {

        searchString = searchString.trim();
        Set<Gene> geneSet = new HashSet<Gene>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( searchString );
        for ( CompositeSequence sequence : matchedCs ) {
            geneSet.addAll( compositeSequenceService.getGenes( sequence ) );
        }

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

        searchString = searchString.trim();
        List<CompositeSequence> allResults = new ArrayList<CompositeSequence>();
        if ( StringUtils.isBlank( searchString ) ) return allResults;
        Collection<CompositeSequence> nameMatch;

        String cleanedSearchString = StringUtils.strip( searchString );

        if ( arrayDesign == null ) {
            nameMatch = compositeSequenceService.findByName( cleanedSearchString );
            allResults.addAll( nameMatch );
        } else {
            assert arrayDesign.getId() != null;
            CompositeSequence res = compositeSequenceService.findByName( arrayDesign, cleanedSearchString );
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
    public Collection<Gene> compassGeneSearch( String searchString ) {

        final String query = searchString.trim();
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
    protected Collection<Gene> convert2GeneList( CompassHit[] anArray ) {

        Collection<Gene> converted = new HashSet<Gene>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( ( Gene ) anArray[i].getData() );

        return converted;

    }

    /**
     * @param anArray
     * @return
     */
    protected Collection<ExpressionExperiment> convert2ExpressionList( CompassHit[] anArray ) {

        Collection<ExpressionExperiment> converted = new HashSet<ExpressionExperiment>( anArray.length );

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
    public Collection<ExpressionExperiment> compassExpressionSearch( String query ) {

        CompassSearchResults searchResults;

        CompassTemplate template = new CompassTemplate( eeBean );
        final String tq = StringUtils.strip( query );
        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( tq, session );
                    }
                } );

        return convert2ExpressionList( searchResults.getHits() );
    }

    /**
     * @param query
     * @return
     */
    public Collection<ExpressionExperiment> expressionExperimentSearch( final String query ) {

        String tq = StringUtils.strip( query );

        Collection<ExpressionExperiment> results = expressionExperimentDbSearch( tq );

        results.addAll( compassExpressionSearch( tq ) );

        return results;
    }

    /**
     * Does search on exact string by: id, name and short name.
     * 
     * @param query
     * @return
     */
    public Collection<ExpressionExperiment> expressionExperimentDbSearch( final String query ) {
        String tq = StringUtils.strip( query );
        Collection<ExpressionExperiment> results = new HashSet<ExpressionExperiment>();
        ExpressionExperiment ee = expressionExperimentService.findByName( tq );
        if ( ee != null ) results.add( ee );
        ee = expressionExperimentService.findByShortName( tq );
        if ( ee != null ) results.add( ee );
        try {
            ee = expressionExperimentService.findById( new Long( tq ) );
            if ( ee != null ) results.add( ee );
        } catch ( NumberFormatException e ) {
            // noop
        }
        return results;
    }

    /**
     * combines the compass style search, and the compositeSequence search and returns a collection with no duplicates.
     * 
     * @param searchString
     * @return a hashset of arraydesigns
     * @throws Exception
     */
    public Collection<ArrayDesign> arrayDesignSearch( String searchString ) throws Exception {

        searchString = searchString.trim();
        Collection<ArrayDesign> adCompassList = compassArrayDesignSearch( searchString );
        Collection<ArrayDesign> adCsList = arrayDesignCompositeSequenceSearch( searchString );

        Collection<ArrayDesign> combinedList = new HashSet<ArrayDesign>();
        combinedList.addAll( adCompassList );
        combinedList.addAll( adCsList );

        return combinedList;
    }

    /**
     * searchs the DB for array designs which have composite sequences whose names match the given search string
     * 
     * @param searchString
     * @return a collection of array designs (no duplicates)
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Collection<ArrayDesign> arrayDesignCompositeSequenceSearch( String searchString ) throws Exception {

        searchString = searchString.trim();
        Set<ArrayDesign> adSet = new HashSet<ArrayDesign>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( searchString );
        for ( CompositeSequence sequence : matchedCs ) {
            adSet.add( sequence.getArrayDesign() );
        }

        return adSet;

    }

    /**
     * Does a compass style search on ArrayDesigns
     * 
     * @param query
     * @return
     */
    public Collection<ArrayDesign> compassArrayDesignSearch( String searchString ) {

        final String query = searchString.trim();
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
     * Does a compass style search on
     * 
     * @param query
     * @return
     */
    public Collection<Characteristic> compassOntologySearch( String searchString ) {

        final String query = searchString.trim();
        CompassSearchResults searchResults;

        CompassTemplate template = new CompassTemplate( ontologyBean );

        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );

        return convert2OntologyList( searchResults.getHits() );
    }

    /**
     * @param anArray
     * @return
     */
    protected Collection<ArrayDesign> convert2ArrayDesignList( CompassHit[] anArray ) {

        Collection<ArrayDesign> converted = new HashSet<ArrayDesign>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( ( ArrayDesign ) anArray[i].getData() );

        return converted;

    }

    /**
     * @param anOntology
     * @return
     */
    protected Collection<Characteristic> convert2OntologyList( CompassHit[] anOntology ) {

        Collection<Characteristic> converted = new HashSet<Characteristic>( anOntology.length );

        for ( int i = 0; i < anOntology.length; i++ )
            converted.add( ( Characteristic ) anOntology[i].getData() );

        return converted;

    }

    /**
     * @param query
     * @param session
     * @return
     */
    protected CompassSearchResults performSearch( String query, CompassSession session ) {
        long time = System.currentTimeMillis();

        assert !StringUtils.isBlank( query );
        query = query.trim();
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

    /**
     * @param ontologyBean the ontologyBean to set
     */
    public void setOntologyBean( Compass ontologyBean ) {
        this.ontologyBean = ontologyBean;
    }

    /**
     * @param compositeSequenceGeneMapperService the compositeSequenceGeneMapperService to set
     */
    public void setCompositeSequenceGeneMapperService(
            CompositeSequenceGeneMapperService compositeSequenceGeneMapperService ) {
        this.compositeSequenceGeneMapperService = compositeSequenceGeneMapperService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

}
