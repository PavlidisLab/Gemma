package ubic.gemma.core.search.source;

import gemma.gsec.util.SecurityUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.core.search.BaseCodeOntologySearchException;
import ubic.gemma.core.search.SearchException;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.core.search.SearchSource;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Search source for direct database results.
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
    private static final double MATCH_BY_SHORT_NAME_SCORE = 1.0;

    private static final double MATCH_BY_ACCESSION_SCORE = 1.0;
    private static final double MATCH_BY_NAME_SCORE = 0.95;

    private static final double MATCH_BY_NAME_INEXACT_SCORE = 0.90;

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
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;
    @Autowired
    private TaxonService taxonService;

    /**
     * Searches the DB for array designs which have composite sequences whose names match the given search string.
     * Because of the underlying database search, this is acl aware. That is, returned array designs are filtered based
     * on access control list (ACL) permissions.
     */
    @Override
    public Collection<SearchResult<ArrayDesign>> searchArrayDesign( SearchSettings settings ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = StopWatch.createStarted();

        Collection<ArrayDesign> adSet = new HashSet<>();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = compositeSequenceService.findByName( settings.getQuery() );
        for ( CompositeSequence sequence : matchedCs ) {
            adSet.add( sequence.getArrayDesign() );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log
                    .info( "Array Design Composite Sequence DB search for " + settings + " took " + watch.getTime()
                            + " ms" + " found " + adSet.size() + " Ads" );

        return this.toSearchResults( adSet, MATCH_BY_NAME_SCORE );
    }

    @Override
    public Collection<SearchResult<BibliographicReference>> searchBibliographicReference( SearchSettings settings ) {
        throw new NotImplementedException( "Database search for bibliographic reference is not implemented." );
    }

    @Override
    public Collection<SearchResult<ExpressionExperimentSet>> searchExperimentSet( SearchSettings settings ) {
        throw new NotImplementedException( "Database search for expression experiment set is not implemented." );
    }

    /**
     * A database search for biosequences. Biosequence names are already indexed by compass...
     */
    @Override
    public Collection<SearchResult<BioSequence>> searchBioSequence( SearchSettings settings ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = StopWatch.createStarted();

        String searchString = settings.getQuery();

        // replace * with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );

        Collection<BioSequence> bs = bioSequenceService.findByName( inexactString );
        // bioSequenceService.thawRawAndProcessed( bs );
        Collection<SearchResult<BioSequence>> bioSequenceList = new HashSet<>( this.toSearchResults( bs, MATCH_BY_NAME_INEXACT_SCORE ) );

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log
                    .info( "BioSequence DB search for " + searchString + " took " + watch.getTime() + " ms and found"
                            + bioSequenceList.size() + " BioSequences" );

        return bioSequenceList;
    }

    @Override
    public Collection<SearchResult> searchBioSequenceAndGene( SearchSettings settings, Collection<SearchResult<Gene>> previousGeneSearchResults ) {
        return new HashSet<>( this.searchBioSequence( settings ) );
    }

    @Override
    public Collection<SearchResult<CompositeSequence>> searchCompositeSequence( SearchSettings settings ) {
        return this.searchCompositeSequenceAndPopulateGenes( settings, new HashSet<>() );
    }

    /**
     * Search the DB for composite sequences and the genes that are matched to them.
     */
    @Override
    public Collection<SearchResult> searchCompositeSequenceAndGene( SearchSettings settings ) {
        Set<SearchResult<Gene>> geneSet = new HashSet<>();
        Collection<SearchResult<CompositeSequence>> matchedCs = this.searchCompositeSequenceAndPopulateGenes( settings, geneSet );
        Collection<SearchResult> combinedResults = new HashSet<>();
        combinedResults.addAll( geneSet );
        combinedResults.addAll( matchedCs );
        return combinedResults;
    }

    private Collection<SearchResult<CompositeSequence>> searchCompositeSequenceAndPopulateGenes( SearchSettings settings, Set<SearchResult<Gene>> geneSet ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = StopWatch.createStarted();

        String searchString = settings.getQuery();
        ArrayDesign ad = settings.getPlatformConstraint();

        // search by exact composite sequence name
        Collection<SearchResult<CompositeSequence>> matchedCs = new LinkedHashSet<>();
        if ( ad != null ) {
            CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
            if ( cs != null )
                matchedCs.add( SearchResult.from( cs, MATCH_BY_NAME_SCORE ) );
        } else {
            matchedCs = toSearchResults( compositeSequenceService.findByName( searchString ), MATCH_BY_NAME_SCORE );
        }

        /*
         * Search by biosequence
         */
        if ( matchedCs.isEmpty() ) {
            Collection<CompositeSequence> csViaBioSeq = compositeSequenceService.findByBioSequenceName( searchString );
            if ( ad != null ) {
                csViaBioSeq.removeIf( c -> !c.getArrayDesign().equals( ad ) );
            }
            matchedCs.addAll( toSearchResults( csViaBioSeq, INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE ) );
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
            if ( settings.getPlatformConstraint() != null ) {
                matchedCs.addAll( toSearchResults( compositeSequenceService.findByGene( g.getResultObject(), settings.getPlatformConstraint() ), INDIRECT_HIT_PENALTY * g.getScore() ) );
            } else {
                matchedCs.addAll( toSearchResults( compositeSequenceService.findByGene( g.getResultObject() ), INDIRECT_HIT_PENALTY * g.getScore() ) );
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
            geneSet.addAll( toSearchResults( genes, INDIRECT_HIT_PENALTY ) );
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
    public Collection<SearchResult<ExpressionExperiment>> searchExpressionExperiment( SearchSettings settings ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = StopWatch.createStarted();

        String query = StringEscapeUtils.unescapeJava( settings.getQuery() );

        LinkedHashSet<SearchResult<ExpressionExperiment>> results = new LinkedHashSet<>();

        Collection<ExpressionExperiment> ees = expressionExperimentService.findByName( query );
        for ( ExpressionExperiment ee : ees ) {
            results.add( SearchResult.from( ee, MATCH_BY_NAME_SCORE, ee.getName() ) );
        }

        // in response to https://github.com/PavlidisLab/Gemma/issues/140, always keep going if admin.
        if ( results.isEmpty() || SecurityUtil.isUserAdmin() ) {
            ExpressionExperiment ee = expressionExperimentService.findByShortName( query );
            if ( ee != null ) {
                results.add( SearchResult.from( ee, MATCH_BY_SHORT_NAME_SCORE, ee.getShortName() ) );
            }
        }

        if ( results.isEmpty() || SecurityUtil.isUserAdmin() ) {
            ees = expressionExperimentService.findByAccession( query ); // this will find split parts
            for ( ExpressionExperiment e : ees ) {
                results.add( SearchResult.from( e, MATCH_BY_ACCESSION_SCORE, e.getId().toString() ) );
            }
        }

        if ( results.isEmpty() ) {
            try {
                // maybe user put in a primary key value.
                ExpressionExperiment ee = expressionExperimentService.load( Long.parseLong( query ) );
                if ( ee != null ) {
                    results.add( SearchResult.from( ee, MATCH_BY_ID_SCORE, ee.getId().toString() ) );
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
            DatabaseSearchSource.log.info( "DB Expression Experiment search for " + settings + " took " + watch.getTime()
                    + " ms and found " + results.size() + " EEs" );

        return results;
    }

    /**
     * Search the DB for genes that exactly match the given search string searches geneProducts, gene and bioSequence
     * tables
     */
    @Override
    public Collection<SearchResult<Gene>> searchGene( SearchSettings settings ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = StopWatch.createStarted();

        String searchString;
        if ( settings.isTermQuery() ) {
            // then we can get the NCBI ID, maybe.
            searchString = StringUtils.substringAfterLast( settings.getQuery(), "/" );
        } else {
            searchString = StringEscapeUtils.unescapeJava( settings.getQuery() );
        }

        if ( StringUtils.isBlank( searchString ) )
            return new HashSet<>();

        Collection<SearchResult<Gene>> results = new LinkedHashSet<>();

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
            results.add( SearchResult.from( result, MATCH_BY_ID_SCORE ) );
        } else {
            result = geneService.findByAccession( searchString, null );
            if ( result != null ) {
                results.add( SearchResult.from( result, MATCH_BY_ACCESSION_SCORE ) );
            }
        }

        if ( results.isEmpty() ) {
            results.addAll( searchGeneExpanded( searchString ) );
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
    private LinkedHashSet<SearchResult<Gene>> searchGeneExpanded( String searchString ) {
        LinkedHashSet<SearchResult<Gene>> results = new LinkedHashSet<>();

        // replace * at end with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*$" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );
        // note that at this point, the inexactString might not have a wildcard - only if the user asked for it.

        String exactString = inexactString.replaceAll( "%", "" );

        // if the query is shortish, always do a wild card search. This gives better behavior in 'live
        // search' situations. If we do wildcards on very short queries we get too many results.
        if ( searchString.length() <= 2 ) {
            // case 0: we got no results yet, or user entered a very short string. We search only for exact matches.
            results.addAll( toSearchResults( geneService.findByOfficialSymbolInexact( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE ) );
        } else if ( inexactString.endsWith( "%" ) ) {
            // case 1: user explicitly asked for wildcard. We allow this on strings of length 3 or more.
            results.addAll( toSearchResults( geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE ) );
        } else if ( searchString.length() > 3 ) {
            // case 2: user did not ask for a wildcard, but we add it anyway, if the string is 4 or 5 characters.
            if ( !inexactString.endsWith( "%" ) ) {
                inexactString = inexactString + "%";
            }
            results.addAll( toSearchResults( geneService.findByOfficialSymbolInexact( inexactString ), MATCH_BY_OFFICIAL_SYMBOL_INEXACT_SCORE ) );

        } else {
            // case 3: string is long enough, and user did not ask for wildcard.
            results.addAll( toSearchResults( geneService.findByOfficialSymbol( exactString ), MATCH_BY_OFFICIAL_SYMBOL_SCORE ) );
        }

        /*
         * If we found a match using official symbol or name, don't bother with this
         */
        if ( results.isEmpty() ) {
            results.addAll( toSearchResults( geneService.findByAlias( exactString ), MATCH_BY_ALIAS_SCORE ) );
            Gene geneByEnsemblId = geneService.findByEnsemblId( exactString );
            if ( geneByEnsemblId != null ) {
                results.add( SearchResult.from( geneByEnsemblId, MATCH_BY_ACCESSION_SCORE ) );
            }
            results.addAll( toSearchResults( geneProductService.getGenesByName( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE ) );
            results.addAll( toSearchResults( geneProductService.getGenesByNcbiId( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_ACCESSION_SCORE ) );
            results.addAll( toSearchResults( bioSequenceService.getGenesByAccession( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_ACCESSION_SCORE ) );
            results.addAll( toSearchResults( bioSequenceService.getGenesByName( exactString ), INDIRECT_HIT_PENALTY * MATCH_BY_NAME_SCORE ) );
        }

        return results;
    }

    @Override
    public Collection<SearchResult<GeneSet>> searchGeneSet( SearchSettings settings ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();
        throw new NotImplementedException( "Searching by gene set from the database is not supported." );
    }

    /**
     * Find phenotypes.
     */
    @Override
    public Collection<SearchResult<CharacteristicValueObject>> searchPhenotype( SearchSettings settings ) throws SearchException {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();
        try {
            return this.toSearchResults( this.phenotypeAssociationManagerService.searchInDatabaseForPhenotype( settings.getQuery() ), 1.0 );
        } catch ( OntologySearchException e ) {
            throw new BaseCodeOntologySearchException( "Failed to search for phenotype associations.", e );
        }
    }

    private <T extends Identifiable> List<SearchResult<T>> toSearchResults( Collection<T> entities, double score ) {
        return entities.stream()
                .filter( Objects::nonNull )
                .map( e -> {
                    SearchResult<T> sr = new SearchResult<>( e );
                    sr.setScore( score );
                    return sr;
                } )
                .collect( Collectors.toList() );
    }
}
