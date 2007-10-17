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
import org.compass.core.support.search.CompassSearchResults;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.Securable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * This service is used for performing searches. There are generally two kinds of searches available, percise database
 * searches looking for exact matches in the database and compass/lucene searches which look for matches in the stored
 * index.
 * <p>
 * To enforce access restrictions to the results ("hits") of compass searches, most of the compass searches are backed
 * by a database search. Refer to the javadocs of the individual methods to determine if the result set has factored in
 * access control list permissions.
 * <p>
 * This class is void of any security related code and can be used without a security framework. The security-related
 * javadocs on methods are only pertinent when the programmer wishes to use this class in a declarative security
 * environment, such as ACEGI {@link http://acegisecurity.org/}.
 * <p>
 * 
 * @author klc
 * @author paul
 * @author keshav
 * @version $Id$
 */
public class SearchService {

    /**
     * Global maximum number of search results that will be returned.
     */
    public static final int MAX_SEARCH_RESULTS = 100;

    private GeneService geneService;
    private GeneProductService geneProductService;
    private CompositeSequenceService compositeSequenceService;
    private ArrayDesignService arrayDesignService;
    private BibliographicReferenceService bibliographicReferenceService;
    private ExpressionExperimentService expressionExperimentService;
    private BioSequenceService bioSequenceService;
    private Compass geneBean;
    private Compass eeBean;
    private Compass arrayBean;
    private Compass ontologyBean;
    private Compass bibliographicReferenceBean;
    private Compass probeBean;

    /* EXPRESSION EXPERIMENT SEARCHES */

    /**
     * A general search for expression experiments. This search does both an database search and a compass search.
     * 
     * @param query
     * @return {@link Collection}
     */
    public Collection<ExpressionExperiment> expressionExperimentSearch( final String query ) {

        String tq = StringUtils.strip( query );

        Collection<ExpressionExperiment> results = expressionExperimentDbSearch( tq );

        results.addAll( compassExpressionSearch( tq ) );

        return results;
    }

    /**
     * A compass search on expressionExperiments.
     * <p>
     * The compass search is backed by a database search so the returned collection is filtered based on access
     * permissions to the objects in the collection.
     * 
     * @param query
     * @return {@link Collection}
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
        if ( searchResults == null ) return new HashSet<ExpressionExperiment>();

        Collection<Long> eeIds = convert2IdList( searchResults.getHits() );

        return this.expressionExperimentDbSearch( eeIds );
    }

    /**
     * Does search on exact string by: id, name and short name.
     * <p>
     * The compass search is backed by a database search so the returned collection is filtered based on access
     * permissions to the objects in the collection.
     * 
     * @param query
     * @return {@link Collection}
     */
    public Collection<ExpressionExperiment> expressionExperimentDbSearch( final String query ) {
        String tq = StringUtils.strip( query );
        Collection<ExpressionExperiment> results = new HashSet<ExpressionExperiment>();
        ExpressionExperiment ee = expressionExperimentService.findByName( tq );
        if ( ee != null ) results.add( ee );
        ee = expressionExperimentService.findByShortName( tq );
        if ( ee != null ) results.add( ee );
        try {
            ee = expressionExperimentService.load( new Long( tq ) );
            if ( ee != null ) results.add( ee );
        } catch ( NumberFormatException e ) {
            // noop
        }
        return results;
    }

    /**
     * Database search by ids.
     * <p>
     * The compass search is backed by a database search so the returned collection is filtered based on access
     * permissions to the objects in the collection.
     * 
     * @param ids
     * @return {@link Collection}
     */
    @SuppressWarnings("unchecked")
    private Collection<ExpressionExperiment> expressionExperimentDbSearch( Collection<Long> ids ) {

        Collection<ExpressionExperiment> results = null;

        if ( ids != null && !ids.isEmpty() ) {
            results = expressionExperimentService.loadMultiple( ids );
        } else {
            results = new HashSet<ExpressionExperiment>();
        }

        return results;
    }

    /* ARRAY DESIGN SEARCHES */

    /**
     * A general search for array designs.
     * <p>
     * This search does both an database search and a compass search. This is also contains an underlying
     * {@link CompositeSequence} search, returning the {@link ArrayDesign} collection for the given composite sequence
     * search string (the returned collection of array designs does not contain duplicates).
     * 
     * @param searchString
     * @return a hashset of arraydesigns
     * @throws Exception
     */
    public Collection<ArrayDesign> arrayDesignSearch( String searchString ) throws Exception {

        searchString = searchString.trim();
        ArrayDesign adQueryResult = arrayDesignService.findArrayDesignByName( searchString );
        Collection<ArrayDesign> adCompassList = compassArrayDesignSearch( searchString );
        Collection<ArrayDesign> adCsList = arrayDesignCompositeSequenceDbSearch( searchString );

        Collection<Long> probeAdIdList = new HashSet<Long>();
        for(CompositeSequence cs :  compassProbeSearch( searchString )){
            probeAdIdList.add( cs.getArrayDesign().getId());
        }
        Collection<ArrayDesign> adProbeList = arrayDesignDbLoad( probeAdIdList );
        
        Collection<ArrayDesign> combinedList = new HashSet<ArrayDesign>();
        combinedList.addAll( adCompassList );
        combinedList.addAll( adCsList );
        combinedList.addAll( adProbeList );
        
        if ( adQueryResult != null ) combinedList.add( adQueryResult );

        return combinedList;
    }

    /**
     * A Compass search on array designs.
     * <p>
     * The compass search is backed by a database search so the returned collection is filtered based on access
     * permissions to the objects in the collection.
     * 
     * @param query
     * @return {@link Collection}
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

        if ( searchResults == null ) return new HashSet<ArrayDesign>();

        Collection<Long> adIds = convert2IdList( searchResults.getHits() );
     
        return this.arrayDesignDbLoad( adIds );
    }

    /**
     * @param anArray
     * @return
     */
    protected Collection<ArrayDesign> convert2ArrayDesignList( CompassHit[] anArray ) {
        // TODO Can probably delete this method. Keeping in for now.
        Collection<ArrayDesign> converted = new HashSet<ArrayDesign>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( ( ArrayDesign ) anArray[i].getData() );

        return converted;

    }

    /**
     * Database search by ids.
     * <p>
     * The compass search is backed by a database search so the returned collection is filtered based on access
     * permissions to the objects in the collection.
     * 
     * @param ids
     * @return {@link Collection}
     */
    @SuppressWarnings("unchecked")
    private Collection<ArrayDesign> arrayDesignDbLoad( Collection<Long> ids ) {

        Collection<ArrayDesign> results = arrayDesignService.loadMultiple(ids);
        
        return results;
    }

    /**
     * Searches the DB for array designs which have composite sequences whose names match the given search string.
     * Because of the underlying database search, this is acl aware. That is, returned array designs are filtered based
     * on access control list (ACL) permissions.
     * 
     * @param searchString
     * @return a collection of array designs (no duplicates)
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Collection<ArrayDesign> arrayDesignCompositeSequenceDbSearch( String searchString ) throws Exception {

        searchString = searchString.trim();
        Set<ArrayDesign> adSet = new HashSet<ArrayDesign>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( searchString );
        for ( CompositeSequence sequence : matchedCs ) {
            adSet.add( sequence.getArrayDesign() );
        }

        return adSet;

    }

    /* GENE SEARCHES */

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
        if ( searchResults == null ) return new HashSet<Gene>();
        return convert2GeneList( searchResults.getHits() );
    }

    /**
     * Search the DB for genes that are matched by a given compositeSequence tables
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
        if ( geneList.size() == 0 ) return geneList;
        return geneList.subList( 0, Math.min( geneList.size(), MAX_SEARCH_RESULTS ) );

    }

    /**
     * Search the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
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
        if ( geneList.size() == 0 ) return geneList;
        return geneList.subList( 0, Math.min( geneList.size(), MAX_SEARCH_RESULTS ) );

    }

    /**
     * Combines compass style search, the db style search, and the compositeSequence search and returns 1 combined list
     * with no duplicates.
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

        List<Gene> combinedGeneList = new ArrayList<Gene>();
        combinedGeneList.addAll( geneDbList );

        for ( Gene gene : geneCompassList ) {
            if ( !geneDbList.contains( gene ) ) combinedGeneList.add( gene );
        }

        for ( Gene gene : geneCsList ) {
            if ( !combinedGeneList.contains( gene ) ) combinedGeneList.add( gene );
        }

        if ( combinedGeneList.size() == 0 ) return combinedGeneList;
        return combinedGeneList.subList( 0, Math.min( combinedGeneList.size(), MAX_SEARCH_RESULTS ) );
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

    /* ONTOLOGY SEARCHES */
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

        if ( searchResults == null ) return new HashSet<Characteristic>();
        return convert2OntologyList( searchResults.getHits() );
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
     * @param anOntology
     * @return
     */
    protected Collection<CompositeSequence> convert2ProbeList( CompassHit[] anOntology ) {

        Collection<CompositeSequence> converted = new HashSet<CompositeSequence>( anOntology.length );

        for ( int i = 0; i < anOntology.length; i++ )
            converted.add( ( CompositeSequence ) anOntology[i].getData() );

        return converted;

    }

    /* BIBLIOGRAPHIC REFERENCE SEARCHES */
    /**
     * Does a compass style search on
     * 
     * @param query
     * @return
     */
    public Collection<BibliographicReference> compassBibliographicReferenceSearch( String searchString ) {

        final String query = searchString.trim();
        CompassSearchResults searchResults;

        CompassTemplate template = new CompassTemplate( bibliographicReferenceBean );

        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );

        if ( searchResults == null ) return new HashSet<BibliographicReference>();

        Collection<BibliographicReference> bibRefsFromCompass = convert2BibliographicReferenceList( searchResults
                .getHits() );

        /* if compass is not indexed properly, this db search gives us a second chance. */
        Collection<BibliographicReference> bibliographicReferences = new HashSet<BibliographicReference>();

        for ( BibliographicReference bibRef : bibRefsFromCompass ) {
            if ( bibRef.getPubAccession() == null ) {
                BibliographicReference bibRefFromDb = bibliographicReferenceService.findByTitle( bibRef.getTitle() );
                bibliographicReferences.add( bibRefFromDb );
            } else {
                bibliographicReferences.add( bibRef );
            }
        }

        return bibliographicReferences;

    }

    /**
     * @param bibliographicReferenceHits
     * @return
     */
    protected Collection<BibliographicReference> convert2BibliographicReferenceList(
            CompassHit[] bibliographicReferenceHits ) {

        Collection<BibliographicReference> converted = new HashSet<BibliographicReference>(
                bibliographicReferenceHits.length );

        for ( int i = 0; i < bibliographicReferenceHits.length; i++ )
            converted.add( ( BibliographicReference ) bibliographicReferenceHits[i].getData() );

        return converted;

    }

    /* COMPOSITE SEQUENCE SEARCHES */

    /**
     * A simple database search for composite sequences by name.
     * 
     * @param searchString
     * @return
     */
    @SuppressWarnings("unchecked")
    public Collection<CompositeSequence> compositeSequenceDbSearch( String searchString ) {

        Collection<CompositeSequence> nameMatch = null;

        if ( StringUtils.isBlank( searchString ) ) return nameMatch;

        String cleanedSearchString = StringUtils.strip( searchString );

        nameMatch = compositeSequenceService.findByName( cleanedSearchString );

        return nameMatch;
    }

    /**
     * Search by name of the composite sequence as well as gene.
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
            CompositeSequence res = compositeSequenceService.findByName( arrayDesign, cleanedSearchString );
            if ( res != null ) allResults.add( res );
        }

        Collection<Gene> geneResults = null;
        try {
            geneResults = geneSearch( searchString );
        } catch ( Exception e ) {
            // fail quietly
        }
        // if there have been any genes returned, find the compositeSequences associated with the genes
        if ( geneResults != null && geneResults.size() > 0 ) {
            for ( Gene gene : geneResults ) {

                if ( arrayDesign == null ) {
                    Collection<CompositeSequence> geneCs = geneService.getCompositeSequencesById( gene.getId() );
                    allResults.addAll( geneCs );
                } else {
                    Collection<CompositeSequence> geneCs = geneService.getCompositeSequences( gene, arrayDesign );
                    allResults.addAll( geneCs );
                }

            }
        }

        Collections.sort( allResults, new DescribableComparator() );
        if ( allResults.size() == 0 ) return allResults;
        return allResults.subList( 0, Math.min( allResults.size(), MAX_SEARCH_RESULTS ) );
    }

    /* BIOSEQUENCE SEARCHES */

    /**
     * A database serach for biosequences with the given searchString.
     * 
     * @param searchString
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Collection<BioSequence> bioSequenceDbSearch( String searchString ) throws Exception {
        // TODO add in search for actual sequence, not just the sequence name. Use the wildcard plumbing.
        searchString = searchString.trim();

        // search by inexact symbol
        Set<BioSequence> bioSequenceMatch = new HashSet<BioSequence>();

        // replace * with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );

        bioSequenceMatch.addAll( bioSequenceService.findByName( inexactString ) );

        List<BioSequence> bioSequenceList = new ArrayList<BioSequence>( bioSequenceMatch );
        Comparator<Describable> comparator = new DescribableComparator();
        Collections.sort( bioSequenceList, comparator );
        if ( bioSequenceList.size() == 0 ) return bioSequenceList;
        return bioSequenceList.subList( 0, Math.min( bioSequenceList.size(), MAX_SEARCH_RESULTS ) );

    }
    
    public Collection<CompositeSequence> compassProbeSearch(String searchString) throws Exception{
        
        
        final String query = searchString.trim();
        CompassSearchResults searchResults;

        CompassTemplate template = new CompassTemplate( probeBean );

        searchResults = ( CompassSearchResults ) template.execute(
                CompassTransaction.TransactionIsolation.READ_ONLY_READ_COMMITTED, new CompassCallback() {
                    public Object doInCompass( CompassSession session ) throws CompassException {
                        return performSearch( query, session );
                    }
                } );

        if ( searchResults == null ) return new HashSet<CompositeSequence>();
        
        return convert2ProbeList( searchResults.getHits() );
        
    }

    /**
     * @return the bioSequenceService
     */
    public BioSequenceService getBioSequenceService() {
        return bioSequenceService;
    }

    /**
     * @param query
     * @param session
     * @return
     */
    protected CompassSearchResults performSearch( String query, CompassSession session ) {
        long time = System.currentTimeMillis();

        query = query.trim();
        if ( StringUtils.isBlank( query ) ) return null;
        if ( query.equals( "*" ) ) return null;

        CompassQuery compassQuery = session.queryBuilder().queryString( query.trim() ).toQuery();        
        CompassHits hits = compassQuery.hits();
        CompassDetachedHits detachedHits = hits.detach();
        time = System.currentTimeMillis() - time;
        CompassSearchResults searchResults = new CompassSearchResults( detachedHits.getHits(), time, detachedHits
                .getHits().length );
        
        return searchResults;
    }

    /**
     * Accepts compass hits and returns a collection of ids.
     * 
     * @param anArray
     * @return {@link Collection}
     */
    protected Collection<Long> convert2IdList( CompassHit[] anArray ) {

        Collection<Long> converted = new HashSet<Long>( anArray.length );

        for ( int i = 0; i < anArray.length; i++ )
            converted.add( ( ( Securable ) anArray[i].getData() ).getId() );

        return converted;

    }

    /**
     * @param arrayBean the arrayBean to set
     */
    public void setArrayBean( Compass arrayBean ) {
        this.arrayBean = arrayBean;
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
     * @param eeBean the eeBean to set
     */
    public void setEeBean( Compass eeBean ) {
        this.eeBean = eeBean;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param geneBean the geneBean to set
     */
    public void setGeneBean( Compass geneBean ) {
        this.geneBean = geneBean;
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
     * @param ontologyBean the ontologyBean to set
     */
    public void setOntologyBean( Compass ontologyBean ) {
        this.ontologyBean = ontologyBean;
    }

    /**
     * @param bibliographicReferenceBean the bibliographicReferenceBean to set
     */
    public void setBibliographicReferenceBean( Compass bibliographicReferenceBean ) {
        this.bibliographicReferenceBean = bibliographicReferenceBean;
    }

    /**
     * An inner class used for the ordering of genes
     */
    private class DescribableComparator implements Comparator<Describable> {
        public int compare( Describable arg0, Describable arg1 ) {
            return arg0.getName().compareTo( arg1.getName() );
        }
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param bibliographicReferenceService the bibliographicReferenceService to set
     */
    public void setBibliographicReferenceService( BibliographicReferenceService bibliographicReferenceService ) {
        this.bibliographicReferenceService = bibliographicReferenceService;
    }

    /**
     * @param probeBean the probeBean to set
     */
    public void setProbeBean( Compass probeBean ) {
        this.probeBean = probeBean;
    }

}
