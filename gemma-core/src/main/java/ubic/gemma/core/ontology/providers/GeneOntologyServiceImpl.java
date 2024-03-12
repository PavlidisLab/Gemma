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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.jena.AbstractOntologyMemoryBackedService;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.util.CacheUtils;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Holds a complete copy of the GeneOntology. This gets loaded on startup.
 *
 * @author pavlidis
 */
@Service
public class GeneOntologyServiceImpl extends AbstractOntologyMemoryBackedService implements GeneOntologyService, InitializingBean, DisposableBean {

    public enum GOAspect {
        BIOLOGICAL_PROCESS, CELLULAR_COMPONENT, MOLECULAR_FUNCTION
    }

    private static final Log log = LogFactory.getLog( GeneOntologyServiceImpl.class.getName() );

    /**
     * @param  term the term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( Characteristic term ) {
        String uri = term.getValue();
        return GeneOntologyServiceImpl.asRegularGoId( uri );
    }

    /**
     * @param  term ontology term
     * @return Usual formatted GO id, e.g., GO:0039392
     */
    public static String asRegularGoId( OntologyTerm term ) {
        if ( term == null )
            return null;
        String uri = term.getUri();
        if ( uri == null ) {
            return null;
        }
        return GeneOntologyServiceImpl.asRegularGoId( uri );
    }

    public static String asRegularGoId( String uri ) {
        return uri.replaceAll( ".*?/", "" ).replace( "_", ":" );
    }

    @Override
    protected String getOntologyName() {
        return "geneOntology";
    }

    @Override
    protected String getOntologyUrl() {
        return ontologyUrl;
    }

    /**
     * @return Turn an id like GO:0038128 into a URI.
     */
    private static String toUri( String goId ) {
        String uriTerm = goId.replace( ":", "_" );
        return GeneOntologyService.BASE_GO_URI + uriTerm;
    }

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneService geneService;
    @Autowired
    private CacheManager cacheManager;

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    @Value("${url.geneOntology}")
    private String ontologyUrl;

    /**
     * Cache of gene -> go terms.
     */
    private Cache goTerms;

    /**
     * Cache term -> aspect.
     */
    private Cache term2Aspect;


    @Override
    public void afterPropertiesSet() throws InterruptedException {
        goTerms = CacheUtils.getCache( cacheManager, "GeneOntologyService.goTerms" );
        term2Aspect = CacheUtils.getCache( cacheManager, "GeneOntologyService.term2Aspect" );
        if ( autoLoadOntologies ) {
            startInitializationThread( false, false );
        }
    }

    @Override
    public void destroy() throws Exception {
        if ( isInitializationThreadAlive() ) {
            cancelInitializationThread();
        }
    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds ) {

        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<>();
        if ( queryGene == null )
            return null;
        if ( geneIds.isEmpty() )
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
        if ( geneIds.isEmpty() )
            return overlap;

        Collection<OntologyTerm> queryGeneTerms = this.getGOTerms( queryGene, true, null );

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
    public Collection<OntologyTerm> findTerm( String queryString ) throws OntologySearchException {
        // make sure we are all-inclusive
        queryString = queryString
                .trim()
                .replaceAll( "\\s+AND\\s+", "" )
                .replaceAll( "\\s+", " AND " );
        StopWatch timer = StopWatch.createStarted();
        Set<OntologyTerm> matches = super.findTerm( queryString )
                .stream()
                .filter( r -> StringUtils.isNotBlank( r.getUri() ) )
                .collect( Collectors.toSet() );
        if ( timer.getTime() > 100 ) {
            GeneOntologyServiceImpl.log.warn( String.format( "Finding %d GO terms for '%s' took %d ms",
                    matches.size(), queryString, timer.getTime() ) );
        }
        return matches;
    }

    @Override
    public Set<OntologyTerm> getAllParents( Collection<OntologyTerm> entries ) {
        return this.getAllParents( entries, false );
    }

    @Override
    public Set<OntologyTerm> getAllParents( Collection<OntologyTerm> entries, boolean includePartOf ) {
        return super.getParents( entries, false, includePartOf );
    }

    @Override
    public Collection<Gene> getGenes( String goId, Taxon taxon ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null )
            return null;
        Collection<OntologyTerm> terms = t.getChildren( false, false );
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
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, GOAspect goAspect ) {
        Cache.ValueWrapper value = goTerms.get( gene.getId() );
        //noinspection unchecked
        Collection<OntologyTerm> cachedTerms = value != null ? ( Collection<OntologyTerm> ) value.get() : null;
        if ( GeneOntologyServiceImpl.log.isTraceEnabled() && cachedTerms != null ) {
            this.logIds( "found cached GO terms for " + gene.getOfficialSymbol(), cachedTerms );
        }

        if ( cachedTerms == null ) {
            Collection<OntologyTerm> allGOTermSet = new HashSet<>();

            Collection<Characteristic> annotations = gene2GOAssociationService.findByGene( gene );
            for ( Characteristic c : annotations ) {
                OntologyTerm term;
                if ( c.getValueUri() == null ) {
                    GeneOntologyServiceImpl.log.warn( String.format( "Term %s is free-text, cannot search GO ontology with it.", c ) );
                    continue;
                } else {
                    term = getTerm( c.getValueUri() );
                }
                if ( term == null ) {
                    GeneOntologyServiceImpl.log.warn( String.format( "Term %s not found in term list can't add to results (ontology not loaded?)", c ) );
                    continue;
                }
                allGOTermSet.add( term );
            }

            allGOTermSet.addAll( this.getAllParents( allGOTermSet, includePartOf ) );

            cachedTerms = Collections.unmodifiableCollection( allGOTermSet );
            if ( GeneOntologyServiceImpl.log.isTraceEnabled() )
                this.logIds( "caching GO terms for " + gene.getOfficialSymbol(), allGOTermSet );
            goTerms.put( gene.getId(), cachedTerms );
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

    /**
     * @param  gene          gene
     * @param  goAspect      go aspect
     * @param  includePartOf include part of
     * @return collection of ontology terms
     */
    public Collection<OntologyTerm> getGOTerms( Long gene, boolean includePartOf, GOAspect goAspect ) {
        return this.getGOTerms( geneService.loadOrFail( gene ), includePartOf, goAspect );
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
        if ( t == null ) {
            return null;
        }
        Collection<AnnotationProperty> annotations = t.getAnnotations();
        for ( AnnotationProperty annot : annotations ) {
            if ( annot.getProperty().equals( "hasDefinition" ) ) {
                return annot.getContents();
            }
        }
        return null;
    }

    /**
     * @param  goId e.g. GO_0001312
     * @return null if not found
     */
    @Override
    public OntologyTerm getTermForId( String goId ) {
        return getTerm( GeneOntologyServiceImpl.toUri( goId ) );
    }

    @Override
    public String getTermName( String goId ) {
        OntologyTerm t = getTermForId( goId );
        String label = t != null ? t.getLabel() : null;
        if ( label == null )
            return "[Not available]"; // not ready yet?
        return label;
    }

    @Override
    public GeneOntologyTermValueObject getValueObject( OntologyTerm term ) {
        return new GeneOntologyTermValueObject( GeneOntologyServiceImpl.asRegularGoId( term ), term );
    }

    @Override
    public List<GeneOntologyTermValueObject> getValueObjects( Collection<OntologyTerm> terms ) {
        List<GeneOntologyTermValueObject> vos = new ArrayList<>( terms.size() );
        for ( OntologyTerm term : terms ) {
            vos.add( this.getValueObject( term ) );
        }
        return vos;
    }

    @Override
    public List<GeneOntologyTermValueObject> getValueObjects( Gene gene ) {
        return gene == null ? null : this.getValueObjects( this.getGOTerms( gene ) );
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
    public void clearCaches() {
        goTerms.clear();
        term2Aspect.clear();
    }

    @Override
    public void initialize( InputStream stream, boolean forceIndexing ) {
        super.initialize( stream, forceIndexing );
        clearCaches();
    }

    private GOAspect getTermAspect( OntologyTerm term ) {
        assert term != null;
        String goId = term.getLabel();
        Cache.ValueWrapper value = term2Aspect.get( goId );
        GOAspect aspect;
        if ( value != null ) {
            aspect = ( GOAspect ) value.get();
        } else {
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

            if ( nameSpace != null ) {
                aspect = GOAspect.valueOf( nameSpace.toUpperCase() );
            } else {
                GeneOntologyServiceImpl.log.warn( "aspect could not be determined for: " + term );
                aspect = null;
            }

            term2Aspect.put( goId, aspect );
        }

        return aspect;
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
        for ( Gene gene : genes ) {
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