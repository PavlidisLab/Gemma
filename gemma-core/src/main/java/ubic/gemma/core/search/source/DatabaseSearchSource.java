package ubic.gemma.core.search.source;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import ubic.gemma.core.search.SearchContext;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.core.security.util.SecurityUtil;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchResult;
import ubic.gemma.model.common.search.SearchResultSet;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.isWildcard;
import static ubic.gemma.core.search.lucene.LuceneQueryUtils.prepareDatabaseQuery;

/**
 * Search source for direct database results.
 * <p>
 * Restored for HS-7 search restoration Step 3. The DAO-level surface here is unchanged
 * from the pre-strip implementation; the only rename was {@code gemma.gsec.util.SecurityUtil}
 * &rarr; {@link SecurityUtil}.
 *
 * @author klc
 * @author paul
 * @author keshav
 * @author poirigui
 */
@Component
@CommonsLog
public class DatabaseSearchSource implements SearchSource, Ordered {

    /**
     * Score when a result is matched exactly by numerical ID.
     */
    public static final double MATCH_BY_ID_SCORE = 1.0;
    public static final double MATCH_BY_SHORT_NAME_SCORE = 1.0;

    public static final double MATCH_BY_ACCESSION_SCORE = 1.0;
    public static final double MATCH_BY_NCBI_ID_SCORE = 1.0;
    public static final double MATCH_BY_NAME_SCORE = 0.95;

    private static final double MATCH_BY_ALIAS_SCORE = 0.90;

    private static final double MATCH_BY_OFFICIAL_SYMBOL_SCORE = 1.0;
    private static final double MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE = 0.9;

    private static final double MATCH_BY_OFFICIAL_NAME_SCORE = 0.8;
    private static final double MATCH_BY_OFFICIAL_NAME_INEXACT_SCORE = 0.7;

    /**
     * Penalty when results are matched indirectly (e.g. a platform matched via a gene hit).
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

    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings, SearchContext context ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();
        String query = prepareDatabaseQuery( settings, context.getIssueReporter() );
        if ( query == null ) {
            return Collections.emptySet();
        }

        SearchResultSet<ArrayDesign> results = new SearchResultSet<>( settings );

        if ( canSearchById( settings, ArrayDesign.class ) ) {
            try {
                ArrayDesign ad = arrayDesignService.load( Long.parseLong( query ) );
                if ( ad != null ) {
                    results.add( SearchResult.from( ArrayDesign.class, ad, MATCH_BY_ID_SCORE, Collections.singletonMap( "id", ad.getId().toString() ), "ArrayDesignService.load" ) );
                }
            } catch ( NumberFormatException e ) {
                // ignore - not an ID
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            ArrayDesign shortNameResult = arrayDesignService.findByShortName( query );
            if ( shortNameResult != null ) {
                results.add( SearchResult.from( ArrayDesign.class, shortNameResult, MATCH_BY_SHORT_NAME_SCORE, null, "ArrayDesignService.findByShortName" ) );
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            Collection<ArrayDesign> nameResult = arrayDesignService.findByName( query );
            for ( ArrayDesign ad : nameResult ) {
                results.add( SearchResult.from( ArrayDesign.class, ad, MATCH_BY_NAME_SCORE, null, "ArrayDesignService.findByShortName" ) );
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            Collection<ArrayDesign> altNameResults = arrayDesignService.findByAlternateName( query );
            for ( ArrayDesign arrayDesign : altNameResults ) {
                results.add( SearchResult.from( ArrayDesign.class, arrayDesign, 0.9, null, "ArrayDesignService.findByAlternateName" ) );
            }
        }

        if ( settings.getMode().isAtLeast( SearchSettings.SearchMode.BALANCED ) ) {
            Collection<ArrayDesign> manufacturerResults = arrayDesignService.findByManufacturer( query );
            for ( ArrayDesign arrayDesign : manufacturerResults ) {
                results.add( SearchResult.from( ArrayDesign.class, arrayDesign, 0.9, null, "ArrayDesignService.findByManufacturer" ) );
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.BALANCED ) ) {
            Collection<ArrayDesign> r = arrayDesignService.findByCompositeSequenceName( query );
            for ( ArrayDesign ad : r ) {
                results.add( SearchResult.from( ArrayDesign.class, ad, INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, null, "ArrayDesignService.findByCompositeSequenceName" ) );
            }
        }

        if ( settings.getTaxonConstraint() != null ) {
            results.removeIf( ad -> {
                assert ad.getResultObject() != null;
                return !ad.getResultObject().getPrimaryTaxon().equals( settings.getTaxonConstraint() );
            } );
        }

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            log.warn( String.format( "Array Design DB search for %s with '%s' took %d ms found %d Ads",
                    settings, query, watch.getTime(), results.size() ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings, SearchContext context ) throws SearchException {
        String query = prepareDatabaseQuery( settings, context.getIssueReporter() );
        if ( query == null ) {
            return Collections.emptySet();
        }

        SearchResultSet<ExpressionExperimentSet> results = new SearchResultSet<>( settings );

        if ( canSearchById( settings, ExpressionExperimentSet.class ) ) {
            try {
                ExpressionExperimentSet eeSet = this.experimentSetService.load( Long.parseLong( query ) );
                if ( eeSet != null ) {
                    results.addAll( toSearchResults( settings, ExpressionExperimentSet.class, Collections.singleton( eeSet ), MATCH_BY_ID_SCORE, "ExpressionExperimentSetService.load" ) );
                }
            } catch ( NumberFormatException e ) {
                // ignore
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            results.addAll( toSearchResults( settings, ExpressionExperimentSet.class, experimentSetService.findByName( query ), MATCH_BY_NAME_SCORE, "ExpressionExperimentSetService.findByName" ) );
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            results.addAll( toSearchResults( settings, ExpressionExperimentSet.class, experimentSetService.findByAccession( query ), MATCH_BY_ACCESSION_SCORE, "ExpressionExperimentSetService.findByAccession" ) );
        }

        if ( settings.getTaxonConstraint() != null ) {
            results.removeIf( eeSet -> !Objects.equals( requireNonNull( eeSet.getResultObject() ).getTaxon(), settings.getTaxonConstraint() ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings, SearchContext context ) throws SearchException {
        String searchString = prepareDatabaseQuery( settings, context.getIssueReporter() );
        if ( searchString == null ) {
            return Collections.emptySet();
        }

        StopWatch watch = StopWatch.createStarted();

        SearchResultSet<BioSequence> results = new SearchResultSet<>( settings );

        if ( canSearchById( settings, BioSequence.class ) ) {
            try {
                BioSequence bs = bioSequenceService.load( Long.parseLong( searchString ) );
                if ( bs != null ) {
                    results.addAll( toSearchResults( settings, BioSequence.class, Collections.singleton( bs ), MATCH_BY_ID_SCORE, "BioSequenceService.load" ) );
                }
            } catch ( NumberFormatException e ) {
                // ignore
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            results.addAll( toSearchResults( settings, BioSequence.class, bioSequenceService.findByName( searchString ),
                    MATCH_BY_NAME_SCORE, "BioSequenceService.findByName" ) );
        }

        if ( settings.getTaxonConstraint() != null ) {
            results.removeIf( b -> !Objects.equals( requireNonNull( b.getResultObject() ).getTaxon(), settings.getTaxonConstraint() ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            log.warn( String.format( "BioSequence DB search for %s with '%s' took %d ms and found %d BioSequences",
                    settings, searchString, watch.getTime(), results.size() ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<?>> searchBioSequenceAndGene( SearchSettings settings, SearchContext context, @Nullable Collection<SearchResult<Gene>> previousGeneSearchResults ) throws SearchException {
        return new HashSet<>( this.searchBioSequence( settings, context ) );
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings, SearchContext context ) throws SearchException {
        return this.searchCompositeSequenceAndPopulateGenes( settings, context, null );
    }

    @Override
    public Collection<SearchResult<?>> searchCompositeSequenceAndGene( SearchSettings settings, SearchContext context ) throws SearchException {
        Set<SearchResult<Gene>> geneSet = new SearchResultSet<>( settings );
        Collection<SearchResult<CompositeSequence>> matchedCs = this.searchCompositeSequenceAndPopulateGenes( settings, context, geneSet );
        Collection<SearchResult<?>> combinedResults = new HashSet<>();
        combinedResults.addAll( geneSet );
        combinedResults.addAll( matchedCs );
        return combinedResults;
    }

    private Collection<SearchResult<CompositeSequence>> searchCompositeSequenceAndPopulateGenes( SearchSettings settings, SearchContext context, @Nullable Set<SearchResult<Gene>> geneResults ) throws SearchException {
        String searchString = prepareDatabaseQuery( settings, context.getIssueReporter() );
        if ( searchString == null ) {
            return Collections.emptySet();
        }
        StopWatch watch = StopWatch.createStarted();

        ArrayDesign ad = settings.getPlatformConstraint();

        SearchResultSet<CompositeSequence> results = new SearchResultSet<>( settings );

        if ( canSearchById( settings, CompositeSequence.class ) ) {
            try {
                CompositeSequence cs = compositeSequenceService.load( Long.parseLong( searchString ) );
                if ( cs != null ) {
                    results.addAll( toSearchResults( settings, CompositeSequence.class, Collections.singleton( cs ), MATCH_BY_ID_SCORE, "CompositeSequenceService.load" ) );
                }
            } catch ( NumberFormatException e ) {
                // ignore
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            if ( ad != null ) {
                CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
                if ( cs != null ) {
                    results.add( SearchResult.from( CompositeSequence.class, cs, MATCH_BY_NAME_SCORE, null, "CompositeSequenceService.findByName" ) );
                }
            } else {
                results.addAll( toSearchResults( settings, CompositeSequence.class, compositeSequenceService.findByName( searchString ), MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByName" ) );
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.FAST ) ) {
            Collection<CompositeSequence> csViaBioSeq = compositeSequenceService.findByBioSequenceName( searchString );
            if ( ad != null ) {
                csViaBioSeq.removeIf( c -> !c.getArrayDesign().equals( ad ) );
            }
            results.addAll( toSearchResults( settings, CompositeSequence.class, csViaBioSeq, INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE, "CompositeSequenceService.findByBioSequenceName" ) );
        }

        if ( settings.getTaxonConstraint() != null ) {
            results.removeIf( sr -> {
                assert sr.getResultObject() != null;
                return !sr.getResultObject().getArrayDesign().getPrimaryTaxon().equals( settings.getTaxonConstraint() );
            } );
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.FAST ) ) {
            Collection<SearchResult<Gene>> rawGeneResults = this.searchGene( settings, context );
            Collection<Gene> genes = rawGeneResults.stream()
                    .map( SearchResult::getResultObject )
                    .map( Objects::requireNonNull )
                    .collect( Collectors.toSet() );
            Map<Gene, Double> geneToScore = rawGeneResults.stream()
                    .collect( Collectors.toMap( SearchResult::getResultObject, SearchResult::getScore ) );
            Map<Gene, Collection<CompositeSequence>> gr;
            if ( settings.getPlatformConstraint() != null ) {
                gr = compositeSequenceService.findByGenes( genes, settings.getPlatformConstraint(),
                        settings.getMode().isAtMost( SearchSettings.SearchMode.BALANCED ) );
            } else {
                gr = compositeSequenceService.findByGenes( genes,
                        settings.getMode().isAtMost( SearchSettings.SearchMode.BALANCED ) );
            }

            for ( Map.Entry<Gene, Collection<CompositeSequence>> grEntry : gr.entrySet() ) {
                Gene gene = grEntry.getKey();
                results.addAll( toSearchResults( settings, CompositeSequence.class, grEntry.getValue(), INDIRECT_HIT_PENALTY * geneToScore.get( gene ), "CompositeSequenceService.findByGenes" ) );
            }

            if ( geneResults != null ) {
                for ( SearchResult<Gene> searchResult : rawGeneResults ) {
                    if ( searchResult.getResultObject() != null ) {
                        geneResults.add( searchResult );
                    }
                }
            }
        }

        if ( geneResults != null && settings.getMode().isAtLeast( SearchSettings.SearchMode.FAST ) ) {
            Collection<CompositeSequence> compositeSequences = results.stream()
                    .map( SearchResult::getResultObject )
                    .filter( Objects::nonNull )
                    .collect( Collectors.toSet() );
            for ( Collection<Gene> genes : compositeSequenceService.getGenes( compositeSequences,
                    settings.getMode().isAtMost( SearchSettings.SearchMode.BALANCED ) ).values() ) {
                geneResults.addAll( toSearchResults( settings, Gene.class, genes, INDIRECT_HIT_PENALTY, "CompositeSequenceService.getGenes" ) );
            }
        }

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            log.warn( String.format( "CompositeSequence DB search for %s with '%s' took %d ms, %d items.",
                    settings, searchString, watch.getTime(), results.size() ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings, SearchContext context ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        String query = prepareDatabaseQuery( settings, context.getIssueReporter() );
        if ( query == null ) {
            return Collections.emptySet();
        }

        SearchResultSet<ExpressionExperiment> results = new SearchResultSet<>( settings );

        if ( canSearchById( settings, ExpressionExperiment.class ) ) {
            try {
                ExpressionExperiment ee = expressionExperimentService.load( Long.parseLong( query ) );
                if ( ee != null ) {
                    results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_ID_SCORE, Collections.singletonMap( "id", ee.getId().toString() ), "ExpressionExperimentService.load" ) );
                }
            } catch ( NumberFormatException e ) {
                // ignore - not an ID
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT )
                || SecurityUtil.isUserAdmin() ) {
            ExpressionExperiment ee = expressionExperimentService.findByShortName( query );
            if ( ee != null ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_SHORT_NAME_SCORE, Collections.singletonMap( "shortName", ee.getShortName() ), "ExpressionExperimentService.findByShortName" ) );
            }
        }

        Collection<ExpressionExperiment> ees;
        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT )
                || SecurityUtil.isUserAdmin() ) {
            ees = expressionExperimentService.findByAccession( query );
            for ( ExpressionExperiment e : ees ) {
                assert e.getAccession() != null;
                results.add( SearchResult.from( ExpressionExperiment.class, e, MATCH_BY_ACCESSION_SCORE, Collections.singletonMap( "accession.accession", e.getAccession().getAccession() ), "ExpressionExperimentService.findByAccession" ) );
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            ees = expressionExperimentService.findByName( query );
            for ( ExpressionExperiment ee : ees ) {
                results.add( SearchResult.from( ExpressionExperiment.class, ee, MATCH_BY_NAME_SCORE, Collections.singletonMap( "name", ee.getName() ), "ExpressionExperimentService.findByName" ) );
            }
        }

        if ( settings.getTaxonConstraint() != null ) {
            Collection<Long> retainedIds = expressionExperimentService
                    .filterByTaxon( results.stream().map( SearchResult::getResultId ).collect( Collectors.toList() ), settings.getTaxonConstraint() );
            results.removeIf( sr -> !retainedIds.contains( sr.getResultId() ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            log.warn( String.format( "DB Expression Experiment search for %s with '%s' took %d ms and found %d EEs",
                    settings, query, watch.getTime(), results.size() ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings, SearchContext context ) throws SearchException {
        StopWatch watch = StopWatch.createStarted();

        SearchResultSet<Gene> results = new SearchResultSet<>( settings );

        String searchString = prepareDatabaseQuery( settings, context.getIssueReporter() );
        if ( searchString != null ) {
            if ( searchString.startsWith( Gene.NCBI_URI_PREFIX ) ) {
                searchString = searchString.substring( Gene.NCBI_URI_PREFIX.length() );
            }

            Gene result = null;
            try {
                result = geneService.findByNCBIId( Integer.parseInt( searchString ) );
            } catch ( NumberFormatException e ) {
                // not numeric
            }
            if ( result != null ) {
                results.add( SearchResult.from( Gene.class, result, MATCH_BY_NCBI_ID_SCORE, null, "GeneService.findByNCBIId" ) );
            } else {
                result = geneService.findByAccession( searchString, null );
                if ( result != null ) {
                    results.add( SearchResult.from( Gene.class, result, MATCH_BY_ACCESSION_SCORE, null, "GeneService.findByAccession" ) );
                }
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.FAST ) ) {
            searchGeneExpanded( settings, context.getIssueReporter(), results );
        }

        if ( settings.getTaxonConstraint() != null ) {
            results.removeIf( result1 -> !requireNonNull( result1.getResultObject() ).getTaxon().equals( settings.getTaxonConstraint() ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 ) {
            log.warn( String.format( "Gene DB search for %s with '%s' took %d ms and found %d genes",
                    settings, searchString, watch.getTime(), results.size() ) );
        }

        return results;
    }

    private void searchGeneExpanded( SearchSettings settings, @Nullable Consumer<Throwable> issueReporter, SearchResultSet<Gene> results ) throws SearchException {
        String inexactString = prepareDatabaseQuery( settings, true, issueReporter );
        if ( inexactString == null ) {
            return;
        }
        // trim unescaped reserved characters to derive the "exact" string
        String exactString = inexactString.replaceAll( "([^\\\\])[%_\\\\]", "$1" );

        if ( exactString.length() <= 1 ) {
            results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbol( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE, "GeneService.findByOfficialSymbol" ) );
        } else if ( exactString.length() <= 5 ) {
            if ( isWildcard( settings ) ) {
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            } else {
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbolInexact( inexactString + "%" ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            }
        } else {
            if ( isWildcard( settings ) ) {
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE, "GeneService.findByOfficialSymbolInexact" ) );
            } else {
                results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialSymbol( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE, "GeneService.findByOfficialSymbol" ) );
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
            Collection<Gene> r = geneService.findByOfficialName( StringUtils.strip( settings.getQuery() ) );
            if ( !r.isEmpty() ) {
                results.addAll( toSearchResults( settings, Gene.class, r, MATCH_BY_OFFICIAL_NAME_SCORE, "GeneService.findByOfficialName" ) );
            } else {
                if ( isWildcard( settings ) ) {
                    results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialNameInexact( inexactString ), MATCH_BY_OFFICIAL_NAME_INEXACT_SCORE, "GeneService.findByOfficialNameInexact" ) );
                } else {
                    results.addAll( toSearchResults( settings, Gene.class, geneService.findByOfficialName( exactString ), MATCH_BY_OFFICIAL_NAME_SCORE, "GeneService.findByOfficialName" ) );
                }
            }
        }

        if ( canContinue( results, settings, SearchSettings.SearchMode.EXACT ) ) {
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
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings, SearchContext context ) throws SearchException {
        String query = prepareDatabaseQuery( settings, context.getIssueReporter() );
        if ( query == null ) {
            return Collections.emptySet();
        }
        if ( settings.getTaxonConstraint() != null ) {
            return toSearchResults( settings, GeneSet.class, this.geneSetService.findByName( query, settings.getTaxonConstraint() ), MATCH_BY_NAME_SCORE, "GeneSetService.findByNameWithTaxon" );
        } else {
            return toSearchResults( settings, GeneSet.class, this.geneSetService.findByName( query ), MATCH_BY_NAME_SCORE, "GeneSetService.findByName" );
        }
    }

    @Override
    public Collection<SearchResult<BlacklistedEntity>> searchBlacklistedEntities( SearchSettings settings, SearchContext context ) throws SearchException {
        Collection<SearchResult<BlacklistedEntity>> blacklistedResults = new SearchResultSet<>( settings );
        String query = prepareDatabaseQuery( settings, context.getIssueReporter() );

        if ( query == null ) {
            return Collections.emptySet();
        }

        BlacklistedEntity b = blacklistedEntityService.findByShortName( query );
        if ( b != null ) {
            blacklistedResults.add( SearchResult.from( BlacklistedEntity.class, b, MATCH_BY_SHORT_NAME_SCORE, null, "BlacklistedEntityService.findByShortName" ) );
        }

        b = blacklistedEntityService.findByAccession( query );
        if ( b != null ) {
            blacklistedResults.add( SearchResult.from( BlacklistedEntity.class, b, MATCH_BY_ACCESSION_SCORE, null, "BlacklistedEntityService.findByAccession" ) );
        }

        return blacklistedResults;
    }

    /**
     * We can search by ID only if a single result type is requested. The main reason is that IDs can conflict between
     * entity types.
     */
    private boolean canSearchById( SearchSettings settings, Class<?> resultType ) {
        return settings.getResultTypes().equals( Collections.singleton( resultType ) );
    }

    /**
     * Keep searching only if we have no results yet or if the search mode is at least {@code ACCURATE}.
     */
    private boolean canContinue( SearchResultSet<?> results, SearchSettings settings, SearchSettings.SearchMode minimumMode ) {
        return settings.getMode().isAtLeast( minimumMode ) && ( results.isEmpty() || settings.getMode().isAtLeast( SearchSettings.SearchMode.ACCURATE ) );
    }

    private static <T extends Identifiable> Set<SearchResult<T>> toSearchResults( SearchSettings settings, Class<T> resultType, Collection<T> entities,
            double score, String source ) {
        return entities.stream()
                .filter( Objects::nonNull )
                .map( e -> SearchResult.from( resultType, e, score, null, source ) )
                .collect( Collectors.toCollection( () -> new SearchResultSet<>( settings ) ) );
    }
}
