package ubic.gemma.core.search.source;

import gemma.gsec.util.SecurityUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchResultSet;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.search.lucene.LuceneQueryUtils.isWildcard;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.prepareDatabaseQuery;

/**
 * Search source for direct database results.
 *
 * @author klc
 * @author paul
 * @author keshav
 * @author poirigui
 */
@Component
@CommonsLog
public class DatabaseSearchSource implements SearchSource, Ordered {

    public static final String NCBI_GENE_ID_URI_PREFIX = "http://purl.org/commons/record/ncbi_gene/";

    /**
     * Score when a result is matched exactly by numerical ID.
     */
    public static final double MATCH_BY_ID_SCORE = 1.0;
    public static final double MATCH_BY_SHORT_NAME_SCORE = 1.0;

    public static final double MATCH_BY_ACCESSION_SCORE = 1.0;
    public static final double MATCH_BY_NAME_SCORE = 0.95;

    /**
     * Score when a result is matched by an alias.
     */
    private static final double MATCH_BY_ALIAS_SCORE = 0.90;

    private static final double MATCH_BY_OFFICIAL_SYMBOL_SCORE = 1.0;
    private static final double MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE = 0.9;

    private static final double MATCH_BY_OFFICIAL_NAME_SCORE = 0.8;
    private static final double MATCH_BY_OFFICIAL_NAME_INEXACT_SCORE = 0.7;

    /**
     * Penalty when results are matched indirectly.
     * <p>
     * For example, if a platform is matched by a gene hit.
     */
    private final double INDIRECT_HIT_PENALTY = 0.8;

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private BioSequenceService bioSequenceService;
    @Autowired
    private CompositeSequenceService compositeSequenceService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private GeneService geneService;
    @Autowired
    private GeneProductService geneProductService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private ExpressionExperimentSetService experimentSetService;
    @Autowired
    private BlacklistedEntityService blacklistedEntityService;

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    @Override
    public boolean accepts( SearchSettings settings ) {
        return settings.isUseDatabase();
    }

    /**
     * A general search for array designs.
     * <p>
     * This search does both an database search and a compass search. This is also contains an underlying
     * {@link CompositeSequence} search, returning the {@link ArrayDesign} collection for the given composite sequence
     * search string (the returned collection of array designs does not contain duplicates).
     * <p>
     * Searches the DB for array designs which have composite sequences whose names match the given search string.
     * Because of the underlying database search, this is acl aware. That is, returned array designs are filtered based
     * on access control list (ACL) permissions.
     */
    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();
        String query = prepareDatabaseQuery( settings );
        if ( query == null ) {
            return Collections.emptySet();
        }

        SearchResultSet<ArrayDesign> results = new SearchResultSet<>( settings );

        ArrayDesign shortNameResult = arrayDesignService.findByShortName( query );
        if ( shortNameResult != null ) {
            results.add( SearchResult.from( ArrayDesign.class, shortNameResult, DatabaseSearchSource.MATCH_BY_SHORT_NAME_SCORE, null, "ArrayDesignService.findByShortName" ) );
            return results;
        }

        Collection<ArrayDesign> nameResult = arrayDesignService.findByName( query );
        if ( nameResult != null && !nameResult.isEmpty() ) {
            for ( ArrayDesign ad : nameResult ) {
                results.add( SearchResult.from( ArrayDesign.class, ad, DatabaseSearchSource.MATCH_BY_NAME_SCORE, null, "ArrayDesignService.findByShortName" ) );
            }
            return results;
        }

        Collection<ArrayDesign> altNameResults = arrayDesignService.findByAlternateName( query );
        for ( ArrayDesign arrayDesign : altNameResults ) {
            results.add( SearchResult.from( ArrayDesign.class, arrayDesign, 0.9, null, "ArrayDesignService.findByAlternateName" ) );
        }

        Collection<ArrayDesign> manufacturerResults = arrayDesignService.findByManufacturer( query );
        for ( ArrayDesign arrayDesign : manufacturerResults ) {
            results.add( SearchResult.from( ArrayDesign.class, arrayDesign, 0.9, null, "ArrayDesignService.findByManufacturer" ) );
        }

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( query );
        for ( CompositeSequence sequence : matchedCs ) {
            ArrayDesign entity = sequence.getArrayDesign();
            results.add( SearchResult.from( ArrayDesign.class, entity, INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, null, "CompositeSequenceService.findByName" ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            DatabaseSearchSource.log.warn( String.format( "Array Design DB search for %s with '%s' took %d ms found %d Ads",
                    settings, query, watch.getTime(), results.size() ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException {
        String query = prepareDatabaseQuery( settings );
        if ( query == null ) {
            return Collections.emptySet();
        }
        return toSearchResults( settings, ExpressionExperimentSet.class, this.experimentSetService.findByName( query ), MATCH_BY_NAME_SCORE, "ExperimentSetService.findByName" );
    }

    /**
     * A database search for biosequences. Biosequence names are already indexed by compass...
     */
    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        String searchString = prepareDatabaseQuery( settings );
        if ( searchString == null ) {
            return Collections.emptySet();
        }

        Collection<BioSequence> bs = bioSequenceService.findByName( searchString );
        // bioSequenceService.thawRawAndProcessed( bs );
        Collection<SearchResult<BioSequence>> bioSequenceList = toSearchResults( settings, BioSequence.class, bs, MATCH_BY_NAME_SCORE, "BioSequenceService.findByName" );

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            DatabaseSearchSource.log.warn( String.format( "BioSequence DB search for %s with '%s' took %d ms and found %d BioSequences",
                    settings, searchString, watch.getTime(), bioSequenceList.size() ) );
        }

        return bioSequenceList;
    }

    @Override
    public Collection<SearchResult<?>> searchBioSequenceAndGene( SearchSettings settings, @Nullable Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        return new HashSet<>( this.searchBioSequence( settings ) );
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) throws SearchException {
        return this.searchCompositeSequenceAndPopulateGenes( settings, Collections.emptySet() );
    }

    /**
     * Search the DB for composite sequences and the genes that are matched to them.
     */
    @Override
    public Collection<SearchResult<?>> searchCompositeSequenceAndGene( SearchSettings settings ) throws SearchException {
        Set<SearchResult<Gene>> geneSet = new SearchResultSet<>( settings );
        Collection<SearchResult<CompositeSequence>> matchedCs = this.searchCompositeSequenceAndPopulateGenes( settings, geneSet );
        Collection<SearchResult<?>> combinedResults = new HashSet<>();
        combinedResults.addAll( geneSet );
        combinedResults.addAll( matchedCs );
        return combinedResults;
    }

    private Collection<SearchResult<CompositeSequence>> searchCompositeSequenceAndPopulateGenes( SearchSettings settings, Set<SearchResult<Gene>> geneSet ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        String searchString = prepareDatabaseQuery( settings );
        if ( searchString == null ) {
            return Collections.emptySet();
        }
        ArrayDesign ad = settings.getPlatformConstraint();

        // search by exact composite sequence name
        Collection<SearchResult<CompositeSequence>> matchedCs = new SearchResultSet<>( settings );
        if ( ad != null ) {
            CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
            if ( cs != null )
                matchedCs.add( SearchResult.from( CompositeSequence.class, cs, MATCH_BY_NAME_SCORE, null, "CompositeSequenceService.findByName" ) );
        } else {
            matchedCs = toSearchResults( settings, CompositeSequence.class, compositeSequenceService.findByName( searchString ), MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByName" );
        }

        /*
         * Search by biosequence
         */
        if ( matchedCs.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) ) {
            Collection<CompositeSequence> csViaBioSeq = compositeSequenceService.findByBioSequenceName( searchString );
            if ( ad != null ) {
                csViaBioSeq.removeIf( c -> !c.getArrayDesign().equals( ad ) );
            }
            matchedCs.addAll( toSearchResults( settings, CompositeSequence.class, csViaBioSeq, INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByBioSequenceName" ) );
        }

        /*
         * In case the query _is_ a gene
         */
        Collection<SearchResult<Gene>> rawGeneResults = this.searchGene( settings );
        for ( SearchResult<Gene> searchResult : rawGeneResults ) {
            if ( searchResult.getResultObject() != null ) {
                geneSet.add( searchResult );
            }
        }

        for ( SearchResult<Gene> g : geneSet ) {
            // results from the database are always pre-filled
            assert g.getResultObject() != null;
            if ( settings.getPlatformConstraint() != null ) {
                matchedCs.addAll( toSearchResults( settings, CompositeSequence.class, compositeSequenceService.findByGene( g.getResultObject(), settings.getPlatformConstraint() ), INDIRECT_HIT_PENALTY * g.getScore(), "CompositeSequenceService.findByGene with platform constraint" ) );
            } else {
                matchedCs.addAll( toSearchResults( settings, CompositeSequence.class, compositeSequenceService.findByGene( g.getResultObject() ), INDIRECT_HIT_PENALTY * g.getScore(), "CompositeSequenceService.findByGene" ) );
            }
        }

        // search by associated genes.
        Collection<CompositeSequence> compositeSequences = matchedCs.stream()
                .map( SearchResult::getResultObject )
                .filter( Objects::nonNull )
                .collect( Collectors.toSet() );
        for ( Collection<Gene> genes : compositeSequenceService.getGenes( compositeSequences ).values() ) {
            // TODO: each individual CS have a potentially different score that should be reflected in the gene score,
            //       but that would require knowing which CS matched which gene
            geneSet.addAll( toSearchResults( settings, Gene.class, genes, INDIRECT_HIT_PENALTY, "CompositeSequenceService.getGenes" ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log.warn( String.format( "Gene composite sequence DB search for %s with '%s' took %d ms, %d items.",
                    settings, searchString, watch.getTime(), geneSet.size() ) );

        return matchedCs;
    }


    /**
     * Does search on exact string by: id, name and short name. This only returns results if these fields match exactly,
     * but it's fast.
     *
     * @return {@link Collection}
     */
    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        String query = prepareDatabaseQuery( settings );
        if ( query == null ) {
            return Collections.emptySet();
        }

        Collection<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>( settings );

        Collection<ExpressionExperiment> ees = expressionExperimentService.findByName( query );
        for ( ExpressionExperiment ee : ees ) {
            results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_NAME_SCORE, Collections.singletonMap( "name", ee.getName() ), "ExpressionExperimentService.findByName" ) );
        }

        // in response to https://github.com/PavlidisLab/Gemma/issues/140, always keep going if admin.
        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) || SecurityUtil.isUserAdmin() ) {
            ExpressionExperiment ee = expressionExperimentService.findByShortName( query );
            if ( ee != null ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_SHORT_NAME_SCORE, Collections.singletonMap( "shortName", ee.getShortName() ), "ExpressionExperimentService.findByShortName" ) );
            }
        }

        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) || SecurityUtil.isUserAdmin() ) {
            ees = expressionExperimentService.findByAccession( query ); // this will find split parts
            for ( ExpressionExperiment e : ees ) {
                results.add( SearchResult.from( ExpressionExperiment.class, e, MATCH_BY_ACCESSION_SCORE, Collections.singletonMap( "id", e.getId().toString() ), "ExpressionExperimentService.findByAccession" ) );
            }
        }

        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) ) {
            try {
                // maybe user put in a primary key value.
                ExpressionExperiment ee = expressionExperimentService.load( Long.parseLong( query ) );
                if ( ee != null ) {
                    results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_ID_SCORE, Collections.singletonMap( "id", ee.getId().toString() ), "ExpressionExperimentService.load" ) );
                }
            } catch ( NumberFormatException e ) {
                // no-op - it's not an ID.
            }
        }

        // filter matches by taxon
        if ( settings.getTaxon() != null ) {
            Collection<Long> retainedIds = expressionExperimentService
                    .filterByTaxon( results.stream().map( SearchResult::getResultId ).collect( Collectors.toList() ), settings.getTaxon() );
            results.removeIf( sr -> !retainedIds.contains( sr.getResultId() ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log.warn( String.format( "DB Expression Experiment search for %s with '%s' took %d ms and found %d EEs",
                    settings, query, watch.getTime(), results.size() ) );

        return results;
    }

    /**
     * Search the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
     * tables
     */
    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        Set<SearchResult<Gene>> results = new SearchResultSet<>( settings );

        String searchString = prepareDatabaseQuery( settings );
        if ( searchString != null ) {
            // then we can get the NCBI ID, maybe.
            if ( searchString.startsWith( NCBI_GENE_ID_URI_PREFIX ) ) {
                searchString = searchString.substring( NCBI_GENE_ID_URI_PREFIX.length() );
            }

            /*
             * First search by accession. If we find it, stop.
             */
            Gene result = null;
            try {
                result = geneService.findByNCBIId( Integer.parseInt( searchString ) );
            } catch ( NumberFormatException e ) {
                //
            }
            if ( result != null ) {
                results.add( SearchResult.from( Gene.class, result, MATCH_BY_ID_SCORE, null, "GeneService.findByNCBIId" ) );
            } else {
                result = geneService.findByAccession( searchString, null );
                if ( result != null ) {
                    results.add( SearchResult.from( Gene.class, result, MATCH_BY_ACCESSION_SCORE, null, "GeneService.findByAccession" ) );
                }
            }
        }

        // attempt to do an inexact search if no results were yielded
        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) ) {
            searchGeneExpanded( settings, results );
        }

        // filter by taxon
        if ( settings.getTaxon() != null ) {
            results.removeIf( result1 -> result1.getResultObject() != null && !result1.getResultObject().getTaxon().equals( settings.getTaxon() ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log.warn( String.format( "Gene DB search for %s with '%s' took %d ms and found %d genes",
                    settings, searchString, watch.getTime(), results.size() ) );

        return results;
    }

    /**
     * Expanded gene search used when a simple search does not yield results.
     */
    private void searchGeneExpanded( SearchSettings settings, Set<SearchResult<Gene>> results ) throws SearchException {
        String inexactString = prepareDatabaseQuery( settings, true );
        if ( inexactString == null ) {
            return;
        }

        // trim all the unescaped reserved characters from the string to get the "exact" string
        String exactString = inexactString.replaceAll( "([^\\\\])[%_\\\\]", "$1" );

        // if the query is shortish, always do a wild card search. This gives better behavior in 'live
        // search' situations. If we do wildcards on very short queries we get too many results.
        if ( exactString.length() <= 1 ) {
            // case 0: we got no results yet, or user entered a very short string. We search only for exact matches.
            results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbol( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE, "GeneService.findByOfficialSymbol" ) );
        } else if ( exactString.length() <= 5 ) {
            if ( isWildcard( settings ) ) {
                // case 2: user did ask for a wildcard, if the string is 2, 3, 4 or 5 characters.
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            } else {
                // case 2: user did not ask for a wildcard, but we add it anyway, if the string is 2, 3, 4 or 5 characters.
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbolInexact( inexactString + "%" ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            }
        } else {
            if ( isWildcard( settings ) ) {
                // case 3: string is long enough, and user asked for wildcard.
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            } else {
                // case 3: string is long enough, and user did not ask for wildcard.
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbol( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE, "GeneService.findByOfficialSymbol" ) );
            }
        }

        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) ) {
            // sometimes, the full gene name is uttered, unquoted
            Collection<Gene> r = geneService.findByOfficialName( StringUtils.strip( settings.getQuery() ) );
            if ( !r.isEmpty() ) {
                results.addAll( toSearchResults( settings, Gene.class, r, MATCH_BY_OFFICIAL_NAME_SCORE, "GeneService.findByOfficialName" ) );
            } else {
                // use the parsed string
                if ( isWildcard( settings ) ) {
                    results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialNameInexact( inexactString ), MATCH_BY_OFFICIAL_NAME_INEXACT_SCORE, "GeneService.findByOfficialNameInexact" ) );
                } else {
                    results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialName( exactString ), MATCH_BY_OFFICIAL_NAME_SCORE, "GeneService.findByOfficialName" ) );
                }
            }
        }

        /*
         * If we found a match using official symbol or name, don't bother with this
         */
        if ( results.isEmpty() || settings.getMode().equals( SearchSettings.SearchMode.ACCURATE ) ) {
            results.addAll( toSearchResults( settings, Gene.class, geneService.findByAlias( exactString ), MATCH_BY_ALIAS_SCORE, "GeneService.findByAlias" ) );
            Gene geneByEnsemblId = geneService.findByEnsemblId( exactString );
            if ( geneByEnsemblId != null ) {
                results.add( SearchResult.from( Gene.class, geneByEnsemblId, MATCH_BY_ACCESSION_SCORE, null, "GeneService.findByEnsemblId" ) );
            }
            results.addAll( toSearchResults( settings, Gene.class, geneProductService.getGenesByName( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, "GeneProductService.getGenesByName" ) );
            results.addAll( toSearchResults( settings, Gene.class, geneProductService.getGenesByNcbiId( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_ACCESSION_SCORE, "GeneProductService.getGenesByNcbiId" ) );
            results.addAll( toSearchResults( settings, Gene.class, bioSequenceService.getGenesByAccession( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_ACCESSION_SCORE, "BioSequenceService.GetGenesByAccession" ) );
            results.addAll( toSearchResults( settings, Gene.class, bioSequenceService.getGenesByName( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, "BioSequenceService.getGenesByName" ) );
        }
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        String query = prepareDatabaseQuery( settings );
        if ( query == null ) {
            return Collections.emptySet();
        }
        if ( settings.getTaxon() != null ) {
            return toSearchResults( settings, GeneSet.class, this.geneSetService.findByName( query, settings.getTaxon() ), MATCH_BY_NAME_SCORE, "GeneSetService.findByNameWithTaxon" );
        } else {
            return toSearchResults( settings, GeneSet.class, this.geneSetService.findByName( query ), MATCH_BY_NAME_SCORE, "GeneSetService.findByName" );
        }
    }

    @Override
    public Collection<SearchResult<BlacklistedEntity>> searchBlacklistedEntities( SearchSettings settings ) throws SearchException {
        Collection<SearchResult<BlacklistedEntity>> blacklistedResults = new SearchResultSet<>( settings );
        String query = prepareDatabaseQuery( settings );

        if ( query == null ) {
            return Collections.emptySet();
        }

        BlacklistedEntity b = blacklistedEntityService.findByAccession( query );
        if ( b != null ) {
            blacklistedResults.add( SearchResult.from( BlacklistedEntity.class, b, DatabaseSearchSource.MATCH_BY_ACCESSION_SCORE, null, "BlacklistedEntityService.findByAccession" ) );
        }

        return blacklistedResults;
    }

    private static <T extends Identifiable> Set<SearchResult<T>> toSearchResults( SearchSettings settings, Class<T> resultType, Collection<T> entities, double score, String source ) {
        return entities.stream()
                .filter( Objects::nonNull )
                .map( e -> SearchResult.from( resultType, e, score, null, source ) )
                .collect( Collectors.toCollection( () -> new SearchResultSet<>( settings ) ) );
    }
}
