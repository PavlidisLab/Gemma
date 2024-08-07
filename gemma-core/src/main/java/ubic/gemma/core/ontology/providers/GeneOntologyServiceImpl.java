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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.ontology.search.OntologySearchResult;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneOntologyTermValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.cache.CacheUtils;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;
import ubic.gemma.persistence.service.genome.gene.GeneService;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.asRegularGoId;

/**
 * <a href="https://geneontology.org/">Gene Ontology</a>
 *
 * @author pavlidis
 */
@Service
@ParametersAreNonnullByDefault
public class GeneOntologyServiceImpl extends AbstractOntologyService implements GeneOntologyService, InitializingBean {

    public enum GOAspect {
        BIOLOGICAL_PROCESS, CELLULAR_COMPONENT, MOLECULAR_FUNCTION
    }

    private static final Log log = LogFactory.getLog( GeneOntologyServiceImpl.class.getName() );

    @Override
    protected String getOntologyName() {
        return "Gene Ontology";
    }

    @Override
    protected String getOntologyUrl() {
        return ontologyUrl;
    }

    @Override
    protected boolean isOntologyEnabled() {
        return loadOntology;
    }

    @Nullable
    @Override
    protected String getCacheName() {
        return "geneOntology";
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

    @Autowired
    @Qualifier("ontologyTaskExecutor")
    private TaskExecutor ontologyTaskExecutor;

    @Value("${load.ontologies}")
    private boolean autoLoadOntologies;

    @Value("${url.geneOntology}")
    private String ontologyUrl;

    @Value("${load.geneOntology}")
    private boolean loadOntology;

    /**
     * Cache of gene -> go terms.
     */
    private Cache goTerms;

    /**
     * Cache term -> aspect.
     */
    private Cache term2Aspect;


    @Override
    public void afterPropertiesSet() {
        goTerms = CacheUtils.getCache( cacheManager, "GeneOntologyService.goTerms" );
        term2Aspect = CacheUtils.getCache( cacheManager, "GeneOntologyService.term2Aspect" );
        if ( autoLoadOntologies ) {
            ontologyTaskExecutor.execute( () -> initialize( false, false ) );
        }
    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Gene queryGene, Collection<Long> geneIds ) {
        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<>();
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
        Collection<OntologyTerm> queryGeneTerms1 = this.getGOTerms( queryGene1 );
        Collection<OntologyTerm> queryGeneTerms2 = this.getGOTerms( queryGene2 );
        return this.computeOverlap( queryGeneTerms1, queryGeneTerms2 );
    }

    @Override
    public Map<Long, Collection<OntologyTerm>> calculateGoTermOverlap( Long queryGene, Collection<Long> geneIds ) {
        Map<Long, Collection<OntologyTerm>> overlap = new HashMap<>();
        if ( geneIds.isEmpty() )
            return overlap;

        Collection<OntologyTerm> queryGeneTerms = this.getGOTerms( geneService.loadOrFail( queryGene ), true, null );

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
    public Collection<OntologySearchResult<OntologyTerm>> findTerm( String queryString, int maxResults ) throws OntologySearchException {
        // make sure we are all-inclusive
        queryString = queryString
                .trim()
                .replaceAll( "\\s+AND\\s+", "" )
                .replaceAll( "\\s+", " AND " );
        StopWatch timer = StopWatch.createStarted();
        Set<OntologySearchResult<OntologyTerm>> matches = super.findTerm( queryString, maxResults )
                .stream()
                .filter( r -> StringUtils.isNotBlank( r.getResult().getUri() ) )
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
    public Collection<Gene> getGenes( OntologyTerm term, @Nullable Taxon taxon ) {
        String goId = asRegularGoId( term );
        if ( goId == null ) {
            return Collections.emptyList();
        }
        Set<String> goIds = new HashSet<>();
        goIds.add( goId );
        for ( OntologyTerm ontologyTerm1 : getChildren( Collections.singleton( term ), false, false ) ) {
            String goId1 = asRegularGoId( ontologyTerm1 );
            if ( goId1 != null ) {
                goIds.add( goId1 );
            }
        }
        return gene2GOAssociationService.findByGOTerms( goIds, taxon );
    }

    @Override
    public Collection<Gene> getGenes( String goId, @Nullable Taxon taxon ) {
        OntologyTerm t = getTermForId( goId );
        if ( t == null ) {
            return Collections.emptyList();
        }
        return getGenes( t, taxon );
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene ) {
        return this.getGOTerms( gene, true, null );
    }

    @Override
    public Collection<OntologyTerm> getGOTerms( Gene gene, boolean includePartOf, @Nullable GOAspect goAspect ) {
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

    @Override
    public GOAspect getTermAspect( Characteristic goId ) {
        String term = asRegularGoId( goId );
        return term != null ? this.getTermAspect( term ) : null;
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
        return new GeneOntologyTermValueObject( asRegularGoId( term ), term );
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
        return this.getValueObjects( this.getGOTerms( gene ) );
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
            buf.append( asRegularGoId( i.next() ) );
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
                overlap.put( gene.getId(), new HashSet<>() );
                continue;
            }

            Collection<OntologyTerm> comparisonOntos = this.getGOTerms( gene );

            if ( comparisonOntos == null || comparisonOntos.isEmpty() ) {
                overlap.put( gene.getId(), new HashSet<>() );
                continue;
            }

            overlap.put( gene.getId(), this.computeOverlap( queryGeneTerms, comparisonOntos ) );
        }
    }

}