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
package ubic.gemma.ontology;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneService;
import ubic.gemma.util.ConfigUtils;

import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="geneOntologyService"
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="geneService" ref="geneService"
 */
public class GeneOntologyService implements InitializingBean {

    private final static String CC_URL = "http://www.berkeleybop.org/ontologies/obo-all/cellular_component/cellular_component.owl";

    private final static String BP_URL = "http://www.berkeleybop.org/ontologies/obo-all/biological_process/biological_process.owl";

    private final static String MF_URL = "http://www.berkeleybop.org/ontologies/obo-all/molecular_function/molecular_function.owl";

    private static Log log = LogFactory.getLog( GeneOntologyService.class.getName() );

    private Gene2GOAssociationService gene2GOAssociationService;

    private GeneService geneService;

    // private DirectedGraph graph = null;

    // map of uris to terms
    private static Map<String, OntologyTerm> terms;
    private Map<String, Collection<OntologyTerm>> childrenCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );
    private Map<String, Collection<OntologyTerm>> parentsCache = Collections
            .synchronizedMap( new HashMap<String, Collection<OntologyTerm>>() );

    private Map<Long, Collection<OntologyTerm>> goTerms = new HashMap<Long, Collection<OntologyTerm>>();

    private static final AtomicBoolean ready = new AtomicBoolean( false );

    private static final AtomicBoolean running = new AtomicBoolean( false );

    private static final String BASE_GO_URI = "http://purl.org/obo/owl/GO#";
    private static final String ALL_ROOT = BASE_GO_URI + "ALL";

    private static final String PART_OF_URI = "http://purl.org/obo/owl/OBO_REL#OBO_REL_part_of";

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
     */
    public void afterPropertiesSet() throws Exception {
        log.debug( "entering AfterpropertiesSet" );
        if ( running.get() ) {
            log.warn( "GO initialization is already running" );
            return;
        }
        init();
    }

    public Collection<OntologyTerm> listTerms() {
        return terms.values();
    }

    /**
     * @param goId e.g. GO:0001312
     * @return null if not found
     */
    public static OntologyTerm getTermForId( String goId ) {
        if ( terms == null ) return null;
        return terms.get( toUri( goId ) );
    }

    /*
     * @param goURI e.g. GO:0001312 @return null if not found
     */
    public static OntologyTerm getTermForURI( String uri ) {
        if ( terms == null ) return null;
        return terms.get( uri );
    }

    /**
     * Return human-readable term ("protein kinase") for a GO Id.
     * 
     * @param goId
     * @return
     */
    public String getTermName( String goId ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null ) return "[Not available]"; // not ready yet?
        return t.getTerm();
    }

    /**
     * Return a definition for a GO Id.
     * 
     * @param goId e.g. GO:0094491
     * @return Definition or null if there is no definition.
     */
    public String getTermDefinition( String goId ) {
        OntologyTerm t = getTermForId( goId );
        assert t != null;
        Collection<AnnotationProperty> annotations = t.getAnnotations();
        for ( AnnotationProperty annot : annotations ) {
            log.info( annot.getProperty() );
            if ( annot.getProperty().equals( "hasDefinition" ) ) {
                return annot.getContents();
            }
        }
        return null;
    }

    /**
     * @param goId
     * @return
     */
    public static String getTermAspect( String goId ) {
        OntologyTerm term = getTermForId( goId );
        return getTermAspect( term );
    }

    /**
     * @param goId
     * @return
     */
    public static String getTermAspect( VocabCharacteristic goId ) {
        String string = asRegularGoId( goId );
        return getTermAspect( string );
    }

    /**
     * @param term
     * @return
     */
    private static String getTermAspect( OntologyTerm term ) {
        String nameSpace = null;
        for ( AnnotationProperty annot : term.getAnnotations() ) {
            if ( annot.getProperty().equals( "hasOBONamespace" ) ) {
                nameSpace = annot.getContents();
                break;
            }
        }
        return nameSpace;
    }

    private synchronized void initilizeGoOntology() {

        Thread loadThread = new Thread( new Runnable() {
            public void run() {

                running.set( true );
                terms = new HashMap<String, OntologyTerm>();
                log.info( "Loading Gene Ontology..." );
                StopWatch loadTime = new StopWatch();
                loadTime.start();
                //
                try {
                    loadTermsInNameSpace( MF_URL );
                    log.info( "Gene Ontology Molecular Function loaded, total of " + terms.size() + " items in "
                            + loadTime.getTime() / 1000 + "s" );

                    loadTermsInNameSpace( BP_URL );
                    log.info( "Gene Ontology Biological Process loaded, total of " + terms.size() + " items in "
                            + loadTime.getTime() / 1000 + "s" );

                    loadTermsInNameSpace( CC_URL );
                    log.info( "Gene Ontology Cellular Component loaded, total of " + terms.size() + " items in "
                            + loadTime.getTime() / 1000 + "s" );

                    ready.set( true );
                    running.set( false );

                    log.info( "Done loading GO" );
                    loadTime.stop();
                } catch ( Exception e ) {
                    log.error( e, e );
                    ready.set( false );
                    running.set( false );
                }
            }

        } );

        synchronized ( running ) {
            if ( running.get() ) return;
            loadThread.start();
        }

    }

    protected synchronized void init() {

        boolean loadOntology = ConfigUtils.getBoolean( "loadOntology", true );

        if ( !loadOntology ) {
            log.info( "Loading GO is disabled" );
            return;
        }

        initilizeGoOntology();

    }

    /**
     * 
     */
    protected synchronized void forceLoadOntology() {

        initilizeGoOntology();

    }

    /**
     * @param url
     * @throws IOException
     */
    protected void loadTermsInNameSpace( String url ) throws IOException {
        Collection<OntologyResource> terms = OntologyLoader.initialize( url, OntologyLoader.loadMemoryModel( url,
                OntModelSpec.OWL_MEM ) );
        addTerms( terms );
    }

    /**
     * Primarily here for testing.
     * 
     * @param is
     * @throws IOException
     */
    protected void loadTermsInNameSpace( InputStream is ) throws IOException {
        Collection<OntologyResource> terms = OntologyLoader.initialize( null, OntologyLoader.loadMemoryModel( is, null,
                OntModelSpec.OWL_MEM ) );
        addTerms( terms );
    }

    private void addTerms( Collection<OntologyResource> newTerms ) {
        if ( terms == null ) terms = new HashMap<String, OntologyTerm>();
        for ( OntologyResource term : newTerms ) {
            if ( term.getUri() == null ) continue;
            if ( term instanceof OntologyTerm ) {
                OntologyTerm ontTerm = ( OntologyTerm ) term;
                terms.put( term.getUri(), ontTerm );
                for ( String alternativeID : ontTerm.getAlternativeIds() ) {
                    log.debug( toUri( alternativeID ) );
                    terms.put( toUri( alternativeID ), ontTerm );
                }
            }
        }
    }

    /**
     * Used for determining if the Gene Ontology has finished loading into memory yet Although calls like getParents,
     * getChildren will still work (its much faster once the gene ontologies have been preloaded into memory.
     * 
     * @returns boolean
     */
    public synchronized boolean isGeneOntologyLoaded() {

        return ready.get();
    }

    /**
     * Return the immediate parent(s) of the given entry. The root node is never returned.
     * 
     * @param entry
     * @return collection, because entries can have multiple parents. (only allroot is excluded)
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyTerm> getParents( OntologyTerm entry ) {
        return getParents( entry, false );
    }

    /**
     * @param entry
     * @param includePartOf
     * @return the immediate parents of the given ontology term. includePartOf determins if part of relationships are
     *         included in the returned information
     */
    public Collection<OntologyTerm> getParents( OntologyTerm entry, boolean includePartOf ) {
        Collection<OntologyTerm> parents = entry.getParents( true );
        Collection<OntologyTerm> results = new HashSet<OntologyTerm>();
        for ( OntologyTerm term : parents ) {
            // The isRoot() returns true for the MolecularFunction, BiologicalProcess, CellularComponent
            // if ( term.isRoot() ) continue;
            if ( term.getUri().equalsIgnoreCase( ALL_ROOT ) ) continue;

            if ( term instanceof OntologyClassRestriction ) {
                // log.info( "Skipping " + term );
                // OntologyProperty restrictionOn = ( ( OntologyClassRestriction ) term ).getRestrictionOn();
                // if ( restrictionOn.getLabel().equals( "part_of" ) ) {
                // OntologyTerm restrictedTo = ( ( OntologyClassRestriction ) term ).getRestrictedTo();
                // results.add( restrictedTo );
                // }
            } else {
                // log.info( "Adding " + term );
                results.add( term );
            }
        }

        if ( includePartOf ) results.addAll( getIsPartOf( entry ) );

        return results;
    }

    /**
     * Return all the parents of GO OntologyEntry, up to the root, as well as terms that this has a restriction
     * relationship with (part_of). NOTE: the term itself is NOT included; nor is the root.
     * 
     * @param entry
     * @return parents (excluding the root)
     */

    public Collection<OntologyTerm> getAllParents( OntologyTerm entry ) {
        return getAllParents( entry, false );
    }

    public Collection<OntologyTerm> getAllParents( OntologyTerm entry, boolean includePartOf ) {
        return getAncestors( entry, includePartOf );
        // Collection<OntologyTerm> parents = new HashSet<OntologyTerm>();
        // getAllParents( entry, parents, includePartOf );
        //
        // return parents;

    }

    /**
     * @param entry
     * @param parents (excluding the root)
     */
    private void getAllParents( OntologyTerm entry, Collection<OntologyTerm> parents, boolean includePartOf ) {
        if ( parents == null ) throw new IllegalArgumentException();

        // if ( parentsCache.containsKey( entry.getUri() ) ) {
        // if ( parentsCache.get( entry.getUri() ) != null ) parents.addAll( parentsCache.get( entry.getUri() ) );
        //
        // return;
        // }

        Collection<OntologyTerm> immediateParents = getParents( entry, includePartOf );
        if ( immediateParents == null ) {
            // parentsCache.put( entry.getUri(), null );
            return;
        }

        for ( OntologyTerm entry2 : immediateParents ) {
            // if ( entry2.isRoot() ) continue;
            parents.add( entry2 );
            Collection<OntologyTerm> entry2Parents = new HashSet<OntologyTerm>();
            getAllParents( entry2, entry2Parents, includePartOf );
            parents.addAll( entry2Parents );
        }

        // parentsCache.put( entry.getUri(), new HashSet<OntologyTerm>( parents ) );

    }

    /**
     * @param entries NOTE terms that are in this collection are NOT explicitly included; however, some of them may be
     *        included incidentally if they are parents of other terms in the collection.
     * @return
     */

    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries ) {
        return getAllParents( entries, false );
    }

    public Collection<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf ) {
        if ( entries == null ) return null;
        Collection<OntologyTerm> result = new HashSet<OntologyTerm>();
        for ( OntologyTerm entry : entries ) {
            result.addAll( getAncestors( entry, includePartOf ) );
            // Collection<OntologyTerm> parents = new HashSet<OntologyTerm>();
            // getAllParents( entry, parents, includePartOf );
            // result.addAll( parents );
        }
        return result;
    }

    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry ) {
        // return getAllChildren( entry, false );
        return getDescendants( entry, false );
    }

    /**
     * @param entry
     * @return
     */
    public Collection<OntologyTerm> getAllChildren( OntologyTerm entry, boolean includePartOf ) {

        Collection<OntologyTerm> children = new HashSet<OntologyTerm>();
        getAllChildren( entry, children, includePartOf );

        return children;
    }

    /**
     * @param entry
     * @param children
     */
    private void getAllChildren( OntologyTerm entry, Collection<OntologyTerm> children, boolean includePartOf ) {

        if ( children == null ) throw new IllegalArgumentException();

        if ( childrenCache.containsKey( entry.getUri() ) ) {
            if ( childrenCache.get( entry.getUri() ) != null ) children.addAll( childrenCache.get( entry.getUri() ) );

            return;
        }

        Collection<OntologyTerm> immediateChildren = getChildren( entry, includePartOf );

        if ( immediateChildren == null ) {
            childrenCache.put( entry.getUri(), null );
            return;
        }

        for ( OntologyTerm child : immediateChildren ) {
            children.add( child );
            Collection<OntologyTerm> grandChildren = new HashSet<OntologyTerm>();
            getAllChildren( child, grandChildren, includePartOf );
            children.addAll( grandChildren );
        }

        childrenCache.put( entry.getUri(), new HashSet<OntologyTerm>( children ) );

    }

    /**
     * Determines if one ontology entry is a parent (direct or otherwise) of a given child term.
     * 
     * @param child
     * @param potentialParent
     * @return True if potentialParent is in the parent graph of the child; false otherwise.
     */
    public Boolean isAParentOf( OntologyTerm child, OntologyTerm potentialParent ) {
        if ( potentialParent.isRoot() ) return true; // well....
        Collection<OntologyTerm> parents = getAllParents( child );
        return parents.contains( potentialParent );
    }

    // /**
    // * @param entity
    // * @return
    // */
    // protected Boolean isRoot( OntologyEntry entity ) {
    // return entity.getAccession().equals( "all" ); // FIXME, use the line below instead.
    // // return entity.equals( this.graph.getRoot().getItem() );
    // }

    /**
     * Determines if one ontology entry is a child (direct or otherwise) of a given parent term.
     * 
     * @param parent
     * @param potentialChild
     * @return
     */
    public Boolean isAChildOf( OntologyTerm parent, OntologyTerm potentialChild ) {
        return isAParentOf( potentialChild, parent );
    }

    /**
     * Returns the immediate children of the given entry
     * 
     * @param entry
     * @return children of entry, or null if there are no children (or if entry is null)
     */

    public Collection<OntologyTerm> getChildren( OntologyTerm entry ) {
        return getChildren( entry, false );

    }

    @SuppressWarnings("unchecked")
    public Collection<OntologyTerm> getChildren( OntologyTerm entry, boolean includePartOf ) {
        if ( entry == null ) return null;
        if ( log.isDebugEnabled() ) log.debug( "Getting children of " + entry );
        Collection<OntologyTerm> terms = entry.getChildren( true );

        if ( includePartOf ) terms.addAll( getPartsOf( entry ) );

        return terms;
    }

    /**
     * @param entry
     * @return Given an ontology term recursivly determines all the children and adds them to a cache (same as
     *         getAllParents but the recusive code is a little cleaner and doesn't use and accumulator)
     */
    private synchronized Collection<OntologyTerm> getDescendants( OntologyTerm entry, boolean includePartOf ) {

        Collection<OntologyTerm> descendants = null; // childrenCache.get( entry.getUri() );
        if ( descendants == null ) {
            descendants = new HashSet<OntologyTerm>();

            Collection<OntologyTerm> children = getChildren( entry, includePartOf );
            if ( children != null ) {
                for ( OntologyTerm child : children ) {
                    descendants.add( child );
                    descendants.addAll( getDescendants( child, includePartOf ) );
                }
            }

            // childrenCache.put( entry.getUri(), descendants );
        }
        return descendants;

    }

    private synchronized Collection<OntologyTerm> getAncestors( OntologyTerm entry, boolean includePartOf ) {

        Collection<OntologyTerm> ancestors = null; // parentsCache.get( entry.getUri() );
        if ( ancestors == null ) {
            ancestors = new HashSet<OntologyTerm>();

            Collection<OntologyTerm> parents = getParents( entry, includePartOf );
            if ( parents != null ) {
                for ( OntologyTerm parent : parents ) {
                    ancestors.add( parent );
                    ancestors.addAll( getAncestors( parent, includePartOf ) );
                }
            }

            // parentsCache.put( entry.getUri(), ancestors );
        }
        return ancestors;
    }

    /**
     * Return terms which have "part_of" relation with the given term (they are "part_of" the given term).
     * 
     * @param entry
     * @return
     */
    private Collection<OntologyTerm> getPartsOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<OntologyTerm>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {" + "?x <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty> <" + PART_OF_URI + ">  . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> <" + u + "> . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                String uri = x.getURI();
                if ( x.isAnon() ) continue; // some reasoners will return these.
                if ( log.isDebugEnabled() ) log.debug( terms.get( uri ) + " is part of " + entry );
                r.add( terms.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    /**
     * Return terms to which the given term has a part_of relation (it is "part_of" them).
     * 
     * @param entry
     * @return
     */
    private Collection<OntologyTerm> getIsPartOf( OntologyTerm entry ) {
        Collection<OntologyTerm> r = new HashSet<OntologyTerm>();
        String u = entry.getUri();
        String queryString = "SELECT ?x WHERE {  <" + u + ">  <http://www.w3.org/2000/01/rdf-schema#subClassOf> ?v . "
                + "?v <http://www.w3.org/2002/07/owl#onProperty>  <" + PART_OF_URI + "> . "
                + "?v <http://www.w3.org/2002/07/owl#someValuesFrom> ?x . }";
        Query q = QueryFactory.create( queryString );
        QueryExecution qexec = QueryExecutionFactory.create( q, ( Model ) entry.getModel() );
        try {
            ResultSet results = qexec.execSelect();
            while ( results.hasNext() ) {
                QuerySolution soln = results.nextSolution();
                Resource x = soln.getResource( "x" );
                if ( x.isAnon() ) continue; // some reasoners will return these.
                String uri = x.getURI();
                if ( log.isDebugEnabled() ) log.debug( entry + " is part of " + terms.get( uri ) );
                r.add( terms.get( uri ) );
            }
        } finally {
            qexec.close();
        }
        return r;
    }

    /**
     * @param queryGene
     * @param geneIds
     * @returns Map<Gene,Collection<OntologyEntries>>
     * @throws Exception
     *         <p>
     *         Given a query Gene, and a collection of gene ids calculates the go term overlap for each pair of
     *         queryGene and gene in the given collection. Returns a Map<Gene,Collection<OntologyEntries>>. The key is
     *         the gene (from the [queryGene,gene] pair) and the values are a collection of the overlapping ontology
     *         entries.
     *         </p>
     */
    @SuppressWarnings("unchecked")
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection geneIds )
            throws Exception {

        if ( queryGene == null ) return null;
        if ( geneIds.size() == 0 ) return null;

        Collection<OntologyTerm> queryGeneTerms = getGOTerms( queryGene );

        // nothing to do.
        if ( ( queryGeneTerms == null ) || ( queryGeneTerms.isEmpty() ) ) return null;

        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<Long, Collection<OntologyTerm>>();
        overlap.put( queryGene.getId(), queryGeneTerms ); // include the query gene in the list. Clearly 100% overlap
        // with itself!

        if ( ( geneIds == null ) || ( geneIds.isEmpty() ) ) return overlap;

        Collection<Gene> genes = this.geneService.loadMultiple( geneIds );

        for ( Object obj : genes ) {
            Gene gene = ( Gene ) obj;
            Collection<OntologyTerm> comparisonOntos = getGOTerms( gene );

            if ( ( comparisonOntos == null ) || ( comparisonOntos.isEmpty() ) ) {
                overlap.put( gene.getId(), new HashSet<OntologyTerm>() );
                continue;
            }

            overlap.put( gene.getId(), computerOverlap( queryGeneTerms, comparisonOntos ) );
        }

        return overlap;
    }

    /**
     * @param queryGene1
     * @param queryGene2
     * @returns Collection<OntologyEntries>
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyTerm> calculateGoTermOverlap( Gene queryGene1, Gene queryGene2 ) throws Exception {

        if ( queryGene1 == null || queryGene2 == null ) return null;

        Collection<OntologyTerm> queryGeneTerms1 = getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = getGOTerms( queryGene2 );

        // nothing to do.
        if ( ( queryGeneTerms1 == null ) || ( queryGeneTerms1.isEmpty() ) ) return null;
        if ( ( queryGeneTerms2 == null ) || ( queryGeneTerms2.isEmpty() ) ) return null;
        queryGeneTerms1.retainAll( queryGeneTerms2 );
        return queryGeneTerms1;
    }

    /**
     * @param goId
     * @param taxon
     * @return Collection of all genes in the given taxon that are annotated with the given id, including its child
     *         terms in the hierarchy.
     */
    @SuppressWarnings("unchecked")
    public Collection<Gene> getGenes( String goId, Taxon taxon ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null ) return null;
        Collection<OntologyTerm> terms = getAllChildren( t );
        Collection<Gene> results = new HashSet<Gene>( this.gene2GOAssociationService.findByGOTerm( goId, taxon ) );

        for ( OntologyTerm term : terms ) {
            results.addAll( this.gene2GOAssociationService.findByGOTerm( asRegularGoId( term ), taxon ) );
        }
        return results;
    }

    /**
     * @param Take a gene and return a set of all GO terms including the parents of each GO term
     * @param geneOntologyTerms
     */
    @SuppressWarnings("unchecked")
    public Collection<OntologyTerm> getGOTerms( Gene gene ) {

        if ( goTerms.containsKey( gene.getId() ) ) {
         //   log.info( "    cached: GO terms for " + gene.getOfficialSymbol() + " (id " + gene.getId() + ")" );
            return goTerms.get( gene.getId() );
        }
      //  log.info( "not cached: GO terms for " + gene.getOfficialSymbol() + " (id " + gene.getId() + ")" );
        Collection<VocabCharacteristic> annotations = gene2GOAssociationService.findByGene( gene );

        Collection<OntologyTerm> allGOTermSet = new HashSet<OntologyTerm>();
        for ( VocabCharacteristic c : annotations ) {
            if ( !terms.containsKey( c.getValueUri() ) ) {
                log.warn( "Term " + c.getValueUri() + " not found in term list cant add to results" );
                continue;
            }
            allGOTermSet.add( terms.get( c.getValueUri() ) );
        }

        allGOTermSet.addAll( getAllParents( allGOTermSet ) );

        goTerms.put( gene.getId(), allGOTermSet );
        return allGOTermSet;
    }

    /**
     * @param masterOntos
     * @param comparisonOntos
     * @return
     */
    public Collection<OntologyTerm> computerOverlap( Collection<OntologyTerm> masterOntos,
            Collection<OntologyTerm> comparisonOntos ) {
        Collection<OntologyTerm> overlapTerms = new HashSet<OntologyTerm>( masterOntos );
        overlapTerms.retainAll( comparisonOntos );

        return overlapTerms;
    }

    /**
     * @param term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( OntologyTerm term ) {
        String uri = term.getUri();
        return uri.replaceAll( ".*?#", "" ).replace( "_", ":" );
    }

    /**
     * @param term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( Characteristic term ) {
        String uri = term.getValue();
        return uri.replaceAll( ".*?#", "" ).replace( "_", ":" );
    }

    /**
     * Turn an id like GO:0038128 into a URI.
     * 
     * @param goId
     * @return
     */
    private static String toUri( String goId ) {
        String uriTerm = goId.replace( ":", "_" );
        return BASE_GO_URI + uriTerm;
    }

    /**
     * @param term
     * @return
     */
    public boolean isBiologicalProcess( OntologyTerm term ) {

        String nameSpace = getTermAspect( term );
        if ( nameSpace == null ) {
            log.warn( "No namespace for " + term + ", assuming not Biological Process" );
            return false;
        }

        return nameSpace.equals( "biological_process" );
    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public boolean isReady() {
        return ready.get();
    }

}