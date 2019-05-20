/*

 *
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.core.ontology.providers;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jena.larq.IndexLARQ;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.OntologyLoader;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyClassRestriction;
import ubic.basecode.ontology.model.OntologyResource;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.basecode.ontology.search.OntologyIndexer;
import ubic.basecode.ontology.search.OntologySearch;
import ubic.basecode.ontology.search.SearchIndex;
import ubic.basecode.util.Configuration;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.util.Settings;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "WeakerAccess", "unused" }) // Possible external use
@Component
public class GeneOntologyServiceImpl implements GeneOntologyService {

    public enum GOAspect {
        BIOLOGICAL_PROCESS, CELLULAR_COMPONENT, MOLECULAR_FUNCTION
    }

    private static final String ALL_ROOT = GeneOntologyService.BASE_GO_URI + "ALL";
    private static boolean enabled = true;
    private final static String GO_URL = "http://purl.obolibrary.org/obo/go.owl";
    private final static boolean LOAD_BY_DEFAULT = true;
    private static final String LOAD_GENE_ONTOLOGY_OPTION = "load.geneOntology";
    private static final Log log = LogFactory.getLog( GeneOntologyServiceImpl.class.getName() );
    private static final String PART_OF_URI = "http://purl.obolibrary.org/obo/BFO_0000050";
    private static final AtomicBoolean ready = new AtomicBoolean( false );
    private static final AtomicBoolean running = new AtomicBoolean( false );
    // cache
    private static final Map<String, GOAspect> term2Aspect = new HashMap<>();
    // cache
    private static Map<String, OntologyTerm> uri2Term = new HashMap<>();

    /**
     * @param  term the term
     * @return      Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( Characteristic term ) {
        String uri = term.getValue();
        return GeneOntologyServiceImpl.asRegularGoId( uri );
    }

    /**
     * @param  term ontology term
     * @return      Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( OntologyTerm term ) {
        if ( term == null )
            return null;
        String uri = term.getUri();
        return GeneOntologyServiceImpl.asRegularGoId( uri );
    }

    public static String asRegularGoId( String uri ) {
        return uri.replaceAll( ".*?/", "" ).replace( "_", ":" );
    }

    public static boolean isEnabled() {
        return GeneOntologyServiceImpl.enabled;
    }

    /**
     * @return Turn an id like GO:0038128 into a URI.
     */
    private static String toUri( String goId ) {
        String uriTerm = goId.replace( ":", "_" );
        return GeneOntologyService.BASE_GO_URI + uriTerm;
    }

    /**
     * Cache of go term -> child terms
     */
    private final Map<String, Collection<OntologyTerm>> childrenCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    private Gene2GOAssociationService gene2GOAssociationService;

    private GeneService geneService;

    /**
     * Cache of gene -> go terms.
     */
    private final Map<Gene, Collection<OntologyTerm>> goTerms = new HashMap<>();

    private final Collection<SearchIndex> indices = new HashSet<>();

    private OntModel model;

    /**
     * Cache of go term -> parent terms
     */
    private final Map<String, Collection<OntologyTerm>> parentsCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    @Override
    public void afterPropertiesSet() {

        /*
         * If this load.ontologies is NOT configured, we go ahead (per-ontology config will be checked).
         */
        String doLoad = Configuration.getString( "load.ontologies" );
        if ( StringUtils.isBlank( doLoad ) || Configuration.getBoolean( "load.ontologies" ) ) {
            this.init( false );
        }

    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds ) {

        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<>();
        if ( queryGene == null )
            return null;
        if ( geneIds.size() == 0 )
            return overlap;

        Collection<OntologyTerm> queryGeneTerms = this.getGOTerms( queryGene );

        overlap.put( queryGene.getId(), queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        Collection<Gene> genes = this.geneService.load( geneIds );

        this.putOverlapGenes( overlap, queryGeneTerms, genes );

        return overlap;
    }

    @Override
    public Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 ) {

        if ( queryGene1 == null || queryGene2 == null )
            return null;

        Collection<OntologyTerm> queryGeneTerms1 = this.getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = this.getGOTerms( queryGene2 );

        return this.computeOverlap( queryGeneTerms1, queryGeneTerms2 );
    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Long queryGene, Collection<Long> geneIds ) {
        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<>();
        if ( queryGene == null )
            return null;
        if ( geneIds.size() == 0 )
            return overlap;

        Collection<OntologyTerm> queryGeneTerms = this.getGOTerms( queryGene );

        overlap.put( queryGene, queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        Collection<Gene> genes = this.geneService.load( geneIds );

        this.putOverlapGenes( overlap, queryGeneTerms, genes );

        return overlap;
    }

    @Override
    public Collection<OntologyTerm> computeOverlap( Collection<OntologyTerm> masterTerms,
            Collection<OntologyTerm> comparisonTerms ) {
        Collection<OntologyTerm> overlapTerms = new HashSet<>( masterTerms );
        overlapTerms.retainAll( comparisonTerms );

        return overlapTerms;
    }

    @Override
    public Collection<OntologyTerm> findTerm( String queryString ) {

        if ( !this.isReady() )
            return new HashSet<>();

        if ( GeneOntologyServiceImpl.log.isDebugEnabled() )
            GeneOntologyServiceImpl.log.debug( "Searching Gene Ontology for '" + queryString + "'" );

        // make sure we are all-inclusive
        queryString = queryString.trim();
        queryString = queryString.replaceAll( "\\s+", " AND " );

        StopWatch timer = new StopWatch();
        timer.start();
        Collection<OntologyResource> rawMatches = new HashSet<>();
        for ( SearchIndex index : this.indices ) {
            rawMatches.addAll( OntologySearch.matchIndividuals( model, index, queryString ) );
        }
        if ( timer.getTime() > 100 ) {
            GeneOntologyServiceImpl.log
                    .info( "Find " + rawMatches.size() + " raw go terms from " + queryString + ": " + timer.getTime()
                            + " ms" );
        }
        timer.reset();
        timer.start();

        /*
         * Required to make sure the descriptions are filled in.
         */
        Collection<OntologyTerm> matches = new HashSet<>();
        for ( OntologyResource r : rawMatches ) {
            if ( StringUtils.isBlank( r.getUri() ) )
                continue;
            OntologyTerm termForURI = getTerm( r.getUri() );
            if ( termForURI == null ) {
                GeneOntologyServiceImpl.log.warn( "No term for : " + r );
                continue;
            }
            matches.add( termForURI );
        }

        if ( timer.getTime() > 100 ) {
            GeneOntologyServiceImpl.log
                    .info( "Convert " + rawMatches.size() + " raw go terms to terms: " + timer.getTime() + " ms" );
        }

        return matches;
    }

    @Override
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry ) {
        return this.getAllChildren( entry, false );
    }

    @Override
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry, boolean includePartOf ) {
        return this.getDescendants( entry, includePartOf );
    }

    @Override
    public Collection<String> getAllGOTermIds() {
        return GeneOntologyServiceImpl.uri2Term.keySet();
    }

    @Override
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries ) {
        return this.getAllParents( entries, false );
    }

    @Override
    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf ) {
        if ( entries == null )
            return null;
        Collection<OntologyTerm> result = new HashSet<>();
        for ( OntologyTerm entry : entries ) {
            result.addAll( this.getAncestors( entry, includePartOf ) );
        }
        return result;
    }

    @Override
    public Collection<OntologyTerm> getAllParents( OntologyTerm entry ) {
        return this.getAllParents( entry, true );
    }

    @Override
    public Collection<OntologyTerm> getAllParents( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null )
            return new HashSet<>();
        return this.getAncestors( entry, includePartOf );
    }

    @Override
    public Collection<OntologyTerm> getChildren( OntologyTerm entry ) {
        return this.getChildren( entry, false );

    }

    @Override
    public Collection<OntologyTerm> getChildren( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null )
            return null;
        if ( GeneOntologyServiceImpl.log.isDebugEnabled() )
            GeneOntologyServiceImpl.log.debug( "Getting children of " + entry );
        Collection<OntologyTerm> terms = entry.getChildren( true );

        if ( includePartOf )
            terms.addAll( this.getPartsOf( entry ) );

        return terms;
    }

    @Override
    public Collection<Gene> getGenes( String goId, Taxon taxon ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null )
            return null;
        Collection<OntologyTerm> terms = this.getAllChildren( t );
        Collection<Gene> results = new HashSet<>( this.gene2GOAssociationService.findByGOTerm( goId, taxon ) );

        for ( OntologyTerm term : terms ) {
            results.addAll( this.gene2GOAssociationService
                    .findByGOTerm( GeneOntologyServiceImpl.asRegularGoId( term ), taxon ) );
        }
        return results;
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene ) {
        return this.getGOTerms( gene, true, null );
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf ) {
        return this.getGOTerms( gene, includePartOf, null );
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, GOAspect goAspect ) {
        Collection<OntologyTerm> cachedTerms = goTerms.get( gene );
        if ( GeneOntologyServiceImpl.log.isTraceEnabled() && cachedTerms != null ) {
            this.logIds( "found cached GO terms for " + gene.getOfficialSymbol(), goTerms.get( gene ) );
        }

        if ( cachedTerms == null ) {
            Collection<OntologyTerm> allGOTermSet = new HashSet<>();

            Collection<Characteristic> annotations = gene2GOAssociationService.findByGene( gene );
            for ( Characteristic c : annotations ) {
                if ( !GeneOntologyServiceImpl.uri2Term.containsKey( c.getValueUri() ) ) {
                    GeneOntologyServiceImpl.log
                            .warn( "Term " + c.getValueUri() + " not found in term list can't add to results (ontology not loaded?)" );
                    continue;
                }
                allGOTermSet.add( GeneOntologyServiceImpl.uri2Term.get( c.getValueUri() ) );
            }

            allGOTermSet.addAll( this.getAllParents( allGOTermSet, includePartOf ) );

            cachedTerms = Collections.unmodifiableCollection( allGOTermSet );
            if ( GeneOntologyServiceImpl.log.isTraceEnabled() )
                this.logIds( "caching GO terms for " + gene.getOfficialSymbol(), allGOTermSet );
            goTerms.put( gene, cachedTerms );
        }

        if ( goAspect != null ) {

            Collection<OntologyTerm> finalTerms = new HashSet<>();

            for ( OntologyTerm ontologyTerm : cachedTerms ) {
                GOAspect term = this.getTermAspect( ontologyTerm );
                if ( term != null && term.equals( goAspect ) ) {
                    finalTerms.add( ontologyTerm );
                }
            }

            return finalTerms;
        }

        return cachedTerms;
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Long geneId ) {
        return this.getGOTerms( geneId, true, null );
    }

    /**
     * @param  gene          gene
     * @param  goAspect      go aspect
     * @param  includePartOf include part of
     * @return               collection of ontology terms
     */
    public Collection<OntologyTerm> getGOTerms( Long gene, boolean includePartOf, GOAspect goAspect ) {
        return this.getGOTerms( geneService.load( gene ), includePartOf, goAspect );
    }

    @Override
    public Collection<OntologyTerm> getParents( OntologyTerm entry ) {
        return this.getParents( entry, false );
    }

    @Override
    public Collection<OntologyTerm> getParents( OntologyTerm entry, boolean includePartOf ) {
        Collection<OntologyTerm> parents = entry.getParents( true );
        Collection<OntologyTerm> results = new HashSet<>();
        for ( OntologyTerm term : parents ) {
            // The isRoot() returns true for the MolecularFunction, BiologicalProcess, CellularComponent
            if ( term.isRoot() )
                continue;
            if ( term.getUri().equalsIgnoreCase( GeneOntologyServiceImpl.ALL_ROOT ) )
                continue;
            //noinspection StatementWithEmptyBody // Better readability
            if ( term instanceof OntologyClassRestriction ) {
                // don't keep it. - for example, "ends_during" RO_0002093
            } else {
                results.add( term );
            }
        }

        if ( includePartOf )
            results.addAll( this.getIsPartOf( entry ) );

        return results;
    }

    /**
     * @param  uri uri
     * @return     null if not found
     */
    @Override
    public OntologyTerm getTerm( String uri ) {
        if ( GeneOntologyServiceImpl.uri2Term == null || !GeneOntologyServiceImpl.uri2Term.containsKey( uri ) )
            return null;
        return GeneOntologyServiceImpl.uri2Term.get( uri );
    }

    @Override
    public GOAspect getTermAspect( Characteristic goId ) {
        String string = GeneOntologyServiceImpl.asRegularGoId( goId );
        return this.getTermAspect( string );
    }

    @Override
    public GOAspect getTermAspect( String goId ) {
        OntologyTerm term = getTermForId( goId );
        if ( term == null )
            return null;
        return this.getTermAspect( term );
    }

    @Override
    public String getTermDefinition( String goId ) {
        OntologyTerm t = getTermForId( goId );
        assert t != null;
        Collection<AnnotationProperty> annotations = t.getAnnotations();
        for ( AnnotationProperty annot : annotations ) {
            GeneOntologyServiceImpl.log.info( annot.getProperty() );
            if ( annot.getProperty().equals( "hasDefinition" ) ) {
                return annot.getContents();
            }
        }
        return null;
    }

    /**
     * @param  goId e.g. GO:0001312
     * @return      null if not found
     */
    @Override
    public OntologyTerm getTermForId( String goId ) {
        if ( GeneOntologyServiceImpl.uri2Term == null )
            return null;

        if ( !GeneOntologyServiceImpl.uri2Term.containsKey( GeneOntologyServiceImpl.toUri( goId ) ) ) {
            GeneOntologyServiceImpl.log.warn( "GOID " + goId + " not recognized?" );
        }

        return GeneOntologyServiceImpl.uri2Term.get( GeneOntologyServiceImpl.toUri( goId ) );
    }

    @Override
    public String getTermName( String goId ) {

        OntologyTerm t = getTermForId( goId );
        if ( t == null )
            return "[Not available]"; // not ready yet?
        return t.getTerm();
    }

    @Override
    public GeneOntologyTermValueObject getValueObject( OntologyTerm term ) {
        return new GeneOntologyTermValueObject( GeneOntologyServiceImpl.asRegularGoId( term ), term );
    }

    @Override
    public Collection<GeneOntologyTermValueObject> getValueObjects( Collection<OntologyTerm> terms ) {
        Collection<GeneOntologyTermValueObject> vos = new ArrayList<>( terms.size() );
        for ( OntologyTerm term : terms ) {
            vos.add( this.getValueObject( term ) );
        }
        return vos;
    }

    @Override
    public Collection<GeneOntologyTermValueObject> getValueObjects( Gene gene ) {
        return gene == null ? null : this.getValueObjects( this.getGOTerms( gene ) );
    }

    @Override
    public synchronized void init( boolean force ) {

        if ( GeneOntologyServiceImpl.running.get() ) {
            GeneOntologyServiceImpl.log.warn( "Gene Ontology initialization is already running" );
            return;
        }

        boolean loadOntology = Settings.getBoolean( GeneOntologyServiceImpl.LOAD_GENE_ONTOLOGY_OPTION,
                GeneOntologyServiceImpl.LOAD_BY_DEFAULT );

        if ( !force && !loadOntology ) {
            GeneOntologyServiceImpl.log.info( "Loading Gene Ontology is disabled (both force and "
                    + GeneOntologyServiceImpl.LOAD_GENE_ONTOLOGY_OPTION + " options were false.)" );
            GeneOntologyServiceImpl.enabled = false;
            return;
        }

        this.initializeGeneOntology();
    }

    @Override
    public Boolean isAChildOf( OntologyTerm parent, OntologyTerm potentialChild ) {
        return this.isAParentOf( potentialChild, parent );
    }

    @Override
    public Boolean isAParentOf( OntologyTerm child, OntologyTerm potentialParent ) {
        if ( potentialParent.isRoot() )
            return true; // well....
        Collection<OntologyTerm> parents = this.getAllParents( child );
        return parents.contains( potentialParent );
    }

    @Override
    public Boolean isAValidGOId( String goId ) {
        if ( !this.isReady() ) {
            throw new UnsupportedOperationException( "Gene ontology isn't ready so cannot check validity of IDs" );
        }
        return GeneOntologyServiceImpl.uri2Term.containsKey( GeneOntologyServiceImpl.toUri( goId ) )
                || GeneOntologyServiceImpl.uri2Term
                        .containsKey( GeneOntologyServiceImpl.toUri( goId.replaceFirst( "_", ":" ) ) );
    }

    @Override
    public boolean isBiologicalProcess( OntologyTerm term ) {

        GOAspect nameSpace = this.getTermAspect( term );
        if ( nameSpace == null ) {
            GeneOntologyServiceImpl.log.debug( "No namespace for " + term + ", assuming not Biological Process" );
            return false;
        }

        return nameSpace.equals( GOAspect.BIOLOGICAL_PROCESS );
    }

    @Override
    public synchronized boolean isGeneOntologyLoaded() {

        return GeneOntologyServiceImpl.ready.get();
    }

    @Override
    public synchronized boolean isReady() {
        return GeneOntologyServiceImpl.ready.get();
    }

    @Override
    public synchronized boolean isRunning() {
        return GeneOntologyServiceImpl.running.get();
    }

    @Override
    public Collection<OntologyTerm> listTerms() {
        return GeneOntologyServiceImpl.uri2Term.values();
    }

    @Override
    public void loadTermsInNameSpace( InputStream is ) {
        this.model = OntologyLoader.loadMemoryModel( is, null, OntModelSpec.OWL_MEM );
        Collection<OntologyResource> terms = OntologyLoader.initialize( null, model );
        this.indices.add( OntologyIndexer.indexOntology( "GeneOntology", model ) );
        GeneOntologyServiceImpl.uri2Term.clear();
        this.addTerms( terms );
        GeneOntologyServiceImpl.ready.set( true );
    }

    @Autowired
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    @Autowired
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    @Override
    public void shutDown() {
        if ( this.isReady() ) {
            try {
                this.goTerms.clear();
                this.childrenCache.clear();
                this.parentsCache.clear();
                GeneOntologyServiceImpl.term2Aspect.clear();
                for ( IndexLARQ l : indices ) {
                    l.close();
                }

                this.model.close();
                this.model = null;
            } catch ( Exception e ) {
                throw new RuntimeException( e );
            } finally {
                GeneOntologyServiceImpl.ready.set( false );
                GeneOntologyServiceImpl.running.set( false );
            }

        }

    }

    protected void loadTermsInNameSpace() {
        this.model = OntologyLoader.loadMemoryModel( GeneOntologyServiceImpl.GO_URL, OntModelSpec.OWL_MEM );
        Collection<OntologyResource> terms = OntologyLoader.initialize( GeneOntologyServiceImpl.GO_URL, model );
        this.indices.add( OntologyIndexer
                .indexOntology( GeneOntologyServiceImpl.GO_URL.replaceFirst( ".*/", "" ).replace( ".owl", "" ),
                        model ) );
        this.addTerms( terms );
    }

    private void addTerms( Collection<OntologyResource> newTerms ) {

        for ( OntologyResource term : newTerms ) {
            if ( term.getUri() == null )
                continue;
            if ( term instanceof OntologyTerm ) {
                OntologyTerm ontTerm = ( OntologyTerm ) term;
                GeneOntologyServiceImpl.uri2Term.put( term.getUri(), ontTerm );
                for ( String alternativeID : ontTerm.getAlternativeIds() ) {
                    GeneOntologyServiceImpl.log.debug( GeneOntologyServiceImpl.toUri( alternativeID ) );
                    GeneOntologyServiceImpl.uri2Term.put( GeneOntologyServiceImpl.toUri( alternativeID ), ontTerm );
                }
            }
        }
    }

    private synchronized Collection<OntologyTerm> getAncestors( OntologyTerm entry, boolean includePartOf ) {

        if ( entry == null ) {
            return new HashSet<>();
        }

        Collection<OntologyTerm> ancestors = parentsCache.get( entry.getUri() );
        if ( ancestors == null ) {
            ancestors = new HashSet<>();

            Collection<OntologyTerm> parents = this.getParents( entry, includePartOf );
            if ( parents != null ) {
                for ( OntologyTerm parent : parents ) {
                    ancestors.add( parent );
                    ancestors.addAll( this.getAncestors( parent, includePartOf ) );
                }
            }

            ancestors = Collections.unmodifiableCollection( ancestors );
            parentsCache.put( entry.getUri(), ancestors );
        }
        return new HashSet<>( ancestors );
    }

    /**
     * @return Given an ontology term recursively determines all the children and adds them to a cache (same as
     *         getAllParents but the recursive code is a little cleaner and doesn't use and accumulator)
     */
    private synchronized Collection<OntologyTerm> getDescendants( OntologyTerm entry, boolean includePartOf ) {

        Collection<OntologyTerm> descendants = childrenCache.get( entry.getUri() );
        if ( descendants == null ) {
            descendants = new HashSet<>();

            Collection<OntologyTerm> children = this.getChildren( entry, includePartOf );
            if ( children != null ) {
                for ( OntologyTerm child : children ) {
                    descendants.add( child );
                    descendants.addAll( this.getDescendants( child, includePartOf ) );
                }
            }

            descendants = Collections.unmodifiableCollection( descendants );
            childrenCache.put( entry.getUri(), descendants );
        }
        return new HashSet<>( descendants );

    }

    /**
     * Return terms to which the given term has a part_of relation (it is "part_of" them).
     */
    private Collection<OntologyTerm> getIsPartOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {  <" + u + ">  <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty>  <" + GeneOntologyServiceImpl.PART_OF_URI + "> . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> ?x . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                if ( x.isAnon() )
                    continue; // some reasoners will return these.
                String uri = x.getURI();
                if ( GeneOntologyServiceImpl.log.isDebugEnabled() )
                    GeneOntologyServiceImpl.log
                            .debug( entry + " is part of " + GeneOntologyServiceImpl.uri2Term.get( uri ) );
                r.add( GeneOntologyServiceImpl.uri2Term.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    /**
     * Return terms which have "part_of" relation with the given term (they are "part_of" the given term).
     */
    private Collection<OntologyTerm> getPartsOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {" + "?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty> <" + GeneOntologyServiceImpl.PART_OF_URI + ">  . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> <" + u + "> . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                String uri = x.getURI();
                if ( x.isAnon() )
                    continue; // some reasoners will return these.
                if ( GeneOntologyServiceImpl.log.isDebugEnabled() )
                    GeneOntologyServiceImpl.log
                            .debug( GeneOntologyServiceImpl.uri2Term.get( uri ) + " is part of " + entry );
                r.add( GeneOntologyServiceImpl.uri2Term.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    private GOAspect getTermAspect( OntologyTerm term ) {
        assert term != null;
        String goId = term.getTerm();

        if ( GeneOntologyServiceImpl.term2Aspect.containsKey( goId ) ) {
            return GeneOntologyServiceImpl.term2Aspect.get( goId );
        }

        String nameSpace = null;
        for ( AnnotationProperty annot : term.getAnnotations() ) {
            /*
             * Why they changed this, I can't say. It used to be hasOBONamespace but now comes through as
             * has_obo_namespace.
             */
            if ( annot.getProperty().equals( "hasOBONamespace" ) || annot.getProperty()
                    .equals( "has_obo_namespace" ) ) {
                nameSpace = annot.getContents();
                break;
            }
        }

        if ( nameSpace == null ) {
            GeneOntologyServiceImpl.log.warn( "aspect could not be determined for: " + term );
            return null;
        }

        GOAspect aspect = GOAspect.valueOf( nameSpace.toUpperCase() );
        GeneOntologyServiceImpl.term2Aspect.put( goId, aspect );

        return aspect;
    }

    private synchronized void initializeGeneOntology() {
        if ( GeneOntologyServiceImpl.running.get() )
            return;

        Thread loadThread = new Thread( new Runnable() {
            @Override
            public void run() {
                GeneOntologyServiceImpl.running.set( true );
                GeneOntologyServiceImpl.uri2Term = new HashMap<>();
                GeneOntologyServiceImpl.log.info( "Loading Gene Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();

                try {
                    GeneOntologyServiceImpl.this.loadTermsInNameSpace();

                    GeneOntologyServiceImpl.log
                            .info( "Gene Ontology loaded, total of " + GeneOntologyServiceImpl.uri2Term.size()
                                    + " items in " + loadTime.getTime() / 1000 + "s" );
                    GeneOntologyServiceImpl.ready.set( true );
                    GeneOntologyServiceImpl.running.set( false );

                    GeneOntologyServiceImpl.log.info( "Done loading GO" );
                    loadTime.stop();
                } catch ( Throwable e ) {
                    GeneOntologyServiceImpl.log.error( e, e );
                    GeneOntologyServiceImpl.ready.set( false );
                    GeneOntologyServiceImpl.running.set( false );
                }
            }

        } );

        loadThread.start();

    }

    private void logIds( String prefix, Collection<OntologyTerm> terms ) {
        StringBuilder buf = new StringBuilder( prefix );
        buf.append( ": [ " );
        Iterator<OntologyTerm> i = terms.iterator();
        while ( i.hasNext() ) {
            buf.append( GeneOntologyServiceImpl.asRegularGoId( i.next() ) );
            if ( i.hasNext() )
                buf.append( ", " );
        }
        buf.append( " ]" );
        GeneOntologyServiceImpl.log.trace( buf.toString() );
    }

    private void putOverlapGenes( Map<Long, Collection<OntologyTerm>> overlap, Collection<OntologyTerm> queryGeneTerms,
            Collection<Gene> genes ) {
        for ( Object obj : genes ) {
            Gene gene = ( Gene ) obj;
            if ( queryGeneTerms.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            Collection<OntologyTerm> comparisonOntos = this.getGOTerms( gene );

            if ( comparisonOntos == null || comparisonOntos.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            overlap.put( gene.getId(), this.computeOverlap( queryGeneTerms, comparisonOntos ) );
        }
    }

}