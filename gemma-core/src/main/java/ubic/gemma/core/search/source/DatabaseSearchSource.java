package ubic.gemma.core.search.source;

import gemma.gsec.util.SecurityUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.search.*;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.search.lucene.LuceneQueryUtils.*;

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
public class DatabaseSearchSource implements SearchSource {

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

    /**
     * Penalty when results are matched indirectly.
     * <p>
     * For example, if a platform is matched by a gene hit.
     */
    private final double INDIRECT_HIT_PENALTY = 0.8;

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

    /**
     * Searches the DB for array designs which have composite sequences whose names match the given search string.
     * Because of the underlying database search, this is acl aware. That is, returned array designs are filtered based
     * on access control list (ACL) permissions.
     */
    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) throws SearchException {
        if ( !settings.isUseDatabase() )
            return Collections.emptySet();

        StopWatch watch = StopWatch.createStarted();

        Collection<ArrayDesign> adSet = new HashSet<>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( prepareDatabaseQuery( settings ) );
        for ( CompositeSequence sequence : matchedCs ) {
            adSet.add( sequence.getArrayDesign() );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log
                    .info( "Array Design Composite Sequence DB search for " + settings + " took " + watch.getTime()
                            + " ms" + " found " + adSet.size() + " Ads" );

        return toSearchResults( ArrayDesign.class, adSet, MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByName" );
    }

    @Override

    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) throws SearchException {
        return toSearchResults( ExpressionExperimentSet.class, this.experimentSetService.findByName( prepareDatabaseQuery( settings ) ), MATCH_BY_NAME_SCORE, "ExperimentSetService.findByName" );
    }

    /**
     * A database search for biosequences. Biosequence names are already indexed by compass...
     */
    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) throws SearchException {
        if ( !settings.isUseDatabase() )
            return Collections.emptySet();

        StopWatch watch = StopWatch.createStarted();

        String searchString = prepareDatabaseQuery( settings );

        Collection<BioSequence> bs = bioSequenceService.findByName( searchString );
        // bioSequenceService.thawRawAndProcessed( bs );
        Collection<SearchResult<BioSequence>> bioSequenceList = toSearchResults( BioSequence.class, bs, MATCH_BY_NAME_SCORE, "BioSequenceService.findByName" );

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log
                    .info( "BioSequence DB search for " + searchString + " took " + watch.getTime() + " ms and found"
                            + bioSequenceList.size() + " BioSequences" );

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
        Set<SearchResult<Gene>> geneSet = new SearchResultSet<>();
        Collection<SearchResult<CompositeSequence>> matchedCs = this.searchCompositeSequenceAndPopulateGenes( settings, geneSet );
        Collection<SearchResult<?>> combinedResults = new HashSet<>();
        combinedResults.addAll( geneSet );
        combinedResults.addAll( matchedCs );
        return combinedResults;
    }

    private Collection<SearchResult<CompositeSequence>> searchCompositeSequenceAndPopulateGenes( SearchSettings settings, Set<SearchResult<Gene>> geneSet ) throws SearchException {
        if ( !settings.isUseDatabase() )
            return Collections.emptySet();

        StopWatch watch = StopWatch.createStarted();

        String searchString = prepareDatabaseQuery( settings );
        ArrayDesign ad = settings.getPlatformConstraint();

        // search by exact composite sequence name
        Collection<SearchResult<CompositeSequence>> matchedCs = new SearchResultSet<>();
        if ( ad != null ) {
            CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
            if ( cs != null )
                matchedCs.add( SearchResult.from( CompositeSequence.class, cs, MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByName" ) );
        } else {
            matchedCs = toSearchResults( CompositeSequence.class, compositeSequenceService.findByName( searchString ), MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByName" );
        }

        /*
         * Search by biosequence
         */
        if ( matchedCs.isEmpty() ) {
            Collection<CompositeSequence> csViaBioSeq = compositeSequenceService.findByBioSequenceName( searchString );
            if ( ad != null ) {
                csViaBioSeq.removeIf( c -> !c.getArrayDesign().equals( ad ) );
            }
            matchedCs.addAll( toSearchResults( CompositeSequence.class, csViaBioSeq, INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByBioSequenceName" ) );
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
                matchedCs.addAll( toSearchResults( CompositeSequence.class, compositeSequenceService.findByGene( g.getResultObject(), settings.getPlatformConstraint() ), INDIRECT_HIT_PENALTY * g.getScore(), "CompositeSequenceService.findByGene with platform constraint" ) );
            } else {
                matchedCs.addAll( toSearchResults( CompositeSequence.class, compositeSequenceService.findByGene( g.getResultObject() ), INDIRECT_HIT_PENALTY * g.getScore(), "CompositeSequenceService.findByGene" ) );
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
            geneSet.addAll( toSearchResults( Gene.class, genes, INDIRECT_HIT_PENALTY, "CompositeSequenceService.getGenes" ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log
                    .info( "Gene composite sequence DB search " + searchString + " took " + watch.getTime() + " ms, "
                            + geneSet.size() + " items." );

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
        if ( !settings.isUseDatabase() )
            return Collections.emptySet();

        StopWatch watch = StopWatch.createStarted();

        String query = prepareDatabaseQuery( settings );

        Collection<SearchResult<ExpressionExperiment>> results = new SearchResultSet<>();

        Collection<ExpressionExperiment> ees = expressionExperimentService.findByName( query );
        for ( ExpressionExperiment ee : ees ) {
            results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_NAME_SCORE, Collections.singletonMap( "name", ee.getName() ), "ExpressionExperimentService.findByName" ) );
        }

        // in response to https://github.com/PavlidisLab/Gemma/issues/140, always keep going if admin.
        if ( results.isEmpty() || SecurityUtil.isUserAdmin() ) {
            ExpressionExperiment ee = expressionExperimentService.findByShortName( query );
            if ( ee != null ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_SHORT_NAME_SCORE, Collections.singletonMap( "shortName", ee.getShortName() ), "ExpressionExperimentService.findByShortName" ) );
            }
        }

        if ( results.isEmpty() || SecurityUtil.isUserAdmin() ) {
            ees = expressionExperimentService.findByAccession( query ); // this will find split parts
            for ( ExpressionExperiment e : ees ) {
                results.add( SearchResult.from( ExpressionExperiment.class, e, MATCH_BY_ACCESSION_SCORE, Collections.singletonMap( "id", e.getId().toString() ), "ExpressionExperimentService.findByAccession" ) );
            }
        }

        if ( results.isEmpty() ) {
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
            DatabaseSearchSource.log.warn( "DB Expression Experiment search for " + settings + " took " + watch.getTime()
                    + " ms and found " + results.size() + " EEs" );

        return results;
    }

    /**
     * Search the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
     * tables
     */
    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) throws SearchException {
        if ( !settings.isUseDatabase() )
            return Collections.emptySet();

        StopWatch watch = StopWatch.createStarted();

        String searchString;
        if ( settings.isTermQuery() ) {
            // then we can get the NCBI ID, maybe.
            searchString = StringUtils.substringAfterLast( prepareDatabaseQuery( settings ), "/" );
        } else {
            searchString = prepareDatabaseQuery( settings );
        }

        if ( StringUtils.isBlank( searchString ) )
            return Collections.emptySet();

        Set<SearchResult<Gene>> results = new SearchResultSet<>();

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
            results.add( SearchResult.from( Gene.class, result, MATCH_BY_ID_SCORE, "GeneService.findByNCBIId" ) );
        } else {
            result = geneService.findByAccession( searchString, null );
            if ( result != null ) {
                results.add( SearchResult.from( Gene.class, result, MATCH_BY_ACCESSION_SCORE, "GeneService.findByAccession" ) );
            }
        }

        if ( results.isEmpty() ) {
            results.addAll( searchGeneExpanded( settings ) );
        }

        // filter by taxon
        if ( settings.getTaxon() != null ) {
            results.removeIf( result1 -> result1.getResultObject() != null && !result1.getResultObject().getTaxon().equals( settings.getTaxon() ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log
                    .info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                            + results.size() + " genes" );

        return results;
    }

    /**
     * Expanded gene search used when a simple search does not yield results.
     */
    private Collection<SearchResult<Gene>> searchGeneExpanded( SearchSettings settings ) throws SearchException {
        Set<SearchResult<Gene>> results = new SearchResultSet<>();

        String exactString = prepareDatabaseQuery( settings );
        String inexactString = prepareDatabaseQuery( settings, true );

        // if the query is shortish, always do a wild card search. This gives better behavior in 'live
        // search' situations. If we do wildcards on very short queries we get too many results.
        if ( exactString.length() <= 1 ) {
            // case 0: we got no results yet, or user entered a very short string. We search only for exact matches.
            results.addAll( toSearchResults( Gene.class, geneService.findByOfficialSymbol( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE, "GeneService.findByOfficialSymbol" ) );
        } else if ( exactString.length() <= 5 ) {
            if ( isWildcard( settings ) ) {
                // case 2: user did ask for a wildcard, if the string is 2, 3, 4 or 5 characters.
                results.addAll( toSearchResults( Gene.class, geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            } else {
                // case 2: user did not ask for a wildcard, but we add it anyway, if the string is 2, 3, 4 or 5 characters.
                results.addAll( toSearchResults( Gene.class, geneService.findByOfficialSymbolInexact( inexactString + "%" ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            }
        } else {
            if ( isWildcard( settings ) ) {
                // case 3: string is long enough, and user asked for wildcard.
                results.addAll( toSearchResults( Gene.class, geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbol" ) );
            } else {
                // case 3: string is long enough, and user did not ask for wildcard.
                results.addAll( toSearchResults( Gene.class, geneService.findByOfficialSymbol( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE, "GeneService.findByOfficialSymbol" ) );
            }
        }

        /*
         * If we found a match using official symbol or name, don't bother with this
         */
        if ( results.isEmpty() ) {
            results.addAll( toSearchResults( Gene.class, geneService.findByAlias( exactString ), MATCH_BY_ALIAS_SCORE, "GeneService.findByAlias" ) );
            Gene geneByEnsemblId = geneService.findByEnsemblId( exactString );
            if ( geneByEnsemblId != null ) {
                results.add( SearchResult.from( Gene.class, geneByEnsemblId, MATCH_BY_ACCESSION_SCORE, "GeneService.findByAlias" ) );
            }
            results.addAll( toSearchResults( Gene.class, geneProductService.getGenesByName( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, "GeneProductService.getGenesByName" ) );
            results.addAll( toSearchResults( Gene.class, geneProductService.getGenesByNcbiId( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_ACCESSION_SCORE, "GeneProductService.getGenesByNcbiId" ) );
            results.addAll( toSearchResults( Gene.class, bioSequenceService.getGenesByAccession( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_ACCESSION_SCORE, "BioSequenceService.GetGenesByAccession" ) );
            results.addAll( toSearchResults( Gene.class, bioSequenceService.getGenesByName( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, "BioSequenceService.getGenesByName" ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) throws SearchException {
        if ( !settings.isUseDatabase() )
            return Collections.emptySet();
        if ( settings.getTaxon() != null ) {
            return toSearchResults( GeneSet.class, this.geneSetService.findByName( prepareDatabaseQuery( settings ), settings.getTaxon() ), MATCH_BY_NAME_SCORE, "GeneSetService.findByNameWithTaxon" );
        } else {
            return toSearchResults( GeneSet.class, this.geneSetService.findByName( prepareDatabaseQuery( settings ) ), MATCH_BY_NAME_SCORE, "GeneSetService.findByName" );
        }
    }

    private static <T extends Identifiable> Set<SearchResult<T>> toSearchResults( Class<T> resultType, Collection<T> entities, double score, String source ) {
        return entities.stream()
                .filter( Objects::nonNull )
                .map( e -> SearchResult.from( resultType, e, score, source ) )
                .collect( Collectors.toCollection( SearchResultSet::new ) );
    }
}
