package ubic.gemma.core.search.source;

import gemma.gsec.util.SecurityUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
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
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneProductService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;
import ubic.gemma.persistence.util.EntityUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        return this.dbHitsToSearchResult( adSet );
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
        Collection<SearchResult<BioSequence>> bioSequenceList = new HashSet<>( this.dbHitsToSearchResult( bs ) );

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
        Set<Gene> geneSet = new HashSet<>();
        Collection<CompositeSequence> matchedCs = this.searchCompositeSequenceAndPopulateGenes( settings, geneSet );
        return this.dbHitsToSearchResult( matchedCs );
    }

    /**
     * Search the DB for composite sequences and the genes that are matched to them.
     */
    @Override
    public Collection<SearchResult> searchCompositeSequenceAndGene( SearchSettings settings ) {
        Set<Gene> geneSet = new HashSet<>();
        Collection<CompositeSequence> matchedCs = this.searchCompositeSequenceAndPopulateGenes( settings, geneSet );
        Collection<SearchResult> combinedResults = new HashSet<>();
        combinedResults.addAll( this.dbHitsToSearchResult( geneSet ) );
        combinedResults.addAll( this.dbHitsToSearchResult( matchedCs ) );
        return combinedResults;
    }

    private Collection<CompositeSequence> searchCompositeSequenceAndPopulateGenes( SearchSettings settings, Set<Gene> geneSet ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();

        StopWatch watch = StopWatch.createStarted();

        String searchString = settings.getQuery();
        ArrayDesign ad = settings.getPlatformConstraint();

        // search by exact composite sequence name
        Collection<CompositeSequence> matchedCs = new HashSet<>();
        if ( ad != null ) {
            CompositeSequence cs = compositeSequenceService.findByName( ad, searchString );
            if ( cs != null )
                matchedCs.add( cs );
        } else {
            matchedCs = compositeSequenceService.findByName( searchString );
        }

        /*
         * Search by bioseqence
         */
        if ( matchedCs.isEmpty() ) {
            Collection<CompositeSequence> csViaBioSeq = compositeSequenceService.findByBioSequenceName( searchString );
            if ( ad != null ) {
                for ( CompositeSequence c : csViaBioSeq ) {
                    if ( c.getArrayDesign().equals( ad ) ) {
                        matchedCs.add( c );
                    }
                }
            } else {
                matchedCs.addAll( csViaBioSeq );
            }
        }

        /*
         * In case the query _is_ a gene
         */
        Collection<SearchResult<Gene>> rawGeneResults = this.searchGene( settings );
        for ( SearchResult<Gene> searchResult : rawGeneResults ) {
            geneSet.add( searchResult.getResultObject() );
        }

        for ( Gene g : geneSet ) {
            if ( settings.getPlatformConstraint() != null ) {
                matchedCs.addAll( compositeSequenceService.findByGene( g, settings.getPlatformConstraint() ) );
            } else {
                matchedCs.addAll( compositeSequenceService.findByGene( g ) );
            }
        }

        // search by associated genes.
        for ( Collection<Gene> genes : compositeSequenceService.getGenes( matchedCs ).values() ) {
            geneSet.addAll( genes );
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

        Map<ExpressionExperiment, String> results = new HashMap<>();
        String query = StringEscapeUtils.unescapeJava( settings.getQuery() );
        Collection<ExpressionExperiment> ees = expressionExperimentService.findByName( query );

        for ( ExpressionExperiment ee : ees ) {
            results.put( ee, ee.getName() );
        }

        // in response to https://github.com/PavlidisLab/Gemma/issues/140, always keep going if admin.
        if ( results.isEmpty() || SecurityUtil.isUserAdmin() ) {
            ExpressionExperiment ee = expressionExperimentService.findByShortName( query );
            if ( ee != null ) {
                results.put( ee, ee.getShortName() );
            }
        }

        if ( results.isEmpty() || SecurityUtil.isUserAdmin() ) {
            ees = expressionExperimentService.findByAccession( query ); // this will find split parts
            for ( ExpressionExperiment e : ees ) {
                results.put( e, e.getId().toString() );
            }
        }

        if ( results.isEmpty() ) {
            try {
                // maybe user put in a primary key value.
                ExpressionExperiment ee = expressionExperimentService.load( new Long( query ) );
                if ( ee != null )
                    results.put( ee, ee.getId().toString() );
            } catch ( NumberFormatException e ) {
                // no-op - it's not an ID.
            }
        }


        if ( settings.getTaxon() != null ) {
            Map<Long, ExpressionExperiment> idMap = EntityUtils.getIdMap( results.keySet() );
            Collection<Long> retainedIds = expressionExperimentService
                    .filterByTaxon( idMap.keySet(), settings.getTaxon() );

            for ( Long id : idMap.keySet() ) {
                if ( !retainedIds.contains( id ) ) {
                    results.remove( idMap.get( id ) );
                }
            }

        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log.info( "DB Expression Experiment search for " + settings + " took " + watch.getTime()
                    + " ms and found " + results.size() + " EEs" );

        return this.dbHitsToSearchResult( results );
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

        Collection<SearchResult<Gene>> results = new HashSet<>();

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
            results.add( this.dbHitToSearchResult( result, null ) );
        } else {
            result = geneService.findByAccession( searchString, null );
            if ( result != null ) {
                results.add( this.dbHitToSearchResult( result, null ) );
            }
        }
        if ( results.size() > 0 ) {
            this.filterByTaxon( settings, results );
            watch.stop();
            if ( watch.getTime() > 1000 )
                DatabaseSearchSource.log
                        .info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                                + results.size() + " genes" );
            return results;
        }

        // replace * at end with % for inexact symbol search
        String inexactString = searchString;
        Pattern pattern = Pattern.compile( "\\*$" );
        Matcher match = pattern.matcher( inexactString );
        inexactString = match.replaceAll( "%" );
        // note that at this point, the inexactString might not have a wildcard - only if the user asked for it.

        String exactString = inexactString.replaceAll( "%", "" );

        // if the query is shortish, always do a wild card search. This gives better behavior in 'live
        // search' situations. If we do wildcards on very short queries we get too many results.
        Collection<Gene> geneSet = new HashSet<>();
        if ( searchString.length() <= 2 ) {
            // case 0: we got no result syet, or user entered a very short string. We search only for exact matches.
            geneSet.addAll( geneService.findByOfficialSymbolInexact( exactString ) );
        } else if ( inexactString.endsWith( "%" ) ) {
            // case 1: user explicitly asked for wildcard. We allow this on strings of length 3 or more.
            geneSet.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );
        } else if ( searchString.length() > 3 ) {
            // case 2: user did not ask for a wildcard, but we add it anyway, if the string is 4 or 5 characters.
            if ( !inexactString.endsWith( "%" ) ) {
                inexactString = inexactString + "%";
            }
            geneSet.addAll( geneService.findByOfficialSymbolInexact( inexactString ) );

        } else {
            // case 3: string is long enough, and user did not ask for wildcard.
            geneSet.addAll( geneService.findByOfficialSymbol( exactString ) );
        }

        /*
         * If we found a match using official symbol or name, don't bother with this
         */
        if ( geneSet.isEmpty() ) {
            geneSet.addAll( geneService.findByAlias( exactString ) );
            geneSet.addAll( geneProductService.getGenesByName( exactString ) );
            geneSet.addAll( geneProductService.getGenesByNcbiId( exactString ) );
            geneSet.addAll( bioSequenceService.getGenesByAccession( exactString ) );
            geneSet.addAll( bioSequenceService.getGenesByName( exactString ) );
            geneSet.add( geneService.findByEnsemblId( exactString ) );
        }

        watch.stop();
        if ( watch.getTime() > 1000 )
            DatabaseSearchSource.log
                    .info( "Gene DB search for " + searchString + " took " + watch.getTime() + " ms and found "
                            + geneSet.size() + " genes" );

        results = this.dbHitsToSearchResult( geneSet );
        this.filterByTaxon( settings, results );
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
    public Collection<SearchResult<CharacteristicValueObject>> searchPhenotype( SearchSettings settings ) {
        if ( !settings.getUseDatabase() )
            return new HashSet<>();
        return this.dbHitsToSearchResult(
                this.phenotypeAssociationManagerService.searchInDatabaseForPhenotype( settings.getQuery() ) );
    }

    /**
     * Convert hits from database searches into SearchResults.
     */
    private <T extends Identifiable> List<SearchResult<T>> dbHitsToSearchResult( Collection<T> entities ) {
        StopWatch watch = StopWatch.createStarted();
        List<SearchResult<T>> results = new ArrayList<>();
        for ( T e : entities ) {
            if ( e == null ) {
                if ( DatabaseSearchSource.log.isDebugEnabled() )
                    DatabaseSearchSource.log.debug( "Null search result object" );
                continue;
            }
            SearchResult<T> esr = this.dbHitToSearchResult( e, null );
            results.add( esr );
        }
        if ( watch.getTime() > 1000 ) {
            DatabaseSearchSource.log.info( "Unpack " + results.size() + " search resultsS: " + watch.getTime() + "ms" );
        }
        return results;
    }

    /**
     * Convert hits from database searches into SearchResults.
     */
    private <T extends Identifiable> List<SearchResult<T>> dbHitsToSearchResult( Map<T, String> entities ) {
        List<SearchResult<T>> results = new ArrayList<>( entities.size() );
        for ( T e : entities.keySet() ) {
            SearchResult<T> esr = this.dbHitToSearchResult( e, entities.get( e ) );
            results.add( esr );
        }
        return results;
    }

    /**
     * @param text that matched the query (for highlighting)
     */
    private <T extends Identifiable> SearchResult<T> dbHitToSearchResult( T e, String text ) {
        return new SearchResult<>( e, 1.0, text );
    }

    /**
     * We only use this if we are not already filtering during the search (which is faster if the results will be large
     * without the filter)
     *
     */
    private <T extends Identifiable> void filterByTaxon( SearchSettings settings, Collection<SearchResult<T>> results ) {
        if ( settings.getTaxon() == null ) {
            return;
        }
        Collection<SearchResult> toRemove = new HashSet<>();
        Taxon t = settings.getTaxon();

        if ( results == null )
            return;

        for ( SearchResult<T> sr : results ) {

            T o = sr.getResultObject();
            try {

                Taxon currentTaxon;

                if ( o instanceof ExpressionExperiment ) {
                    ExpressionExperiment ee = ( ExpressionExperiment ) o;
                    currentTaxon = expressionExperimentService.getTaxon( ee );

                } else if ( o instanceof ExpressionExperimentSet ) {
                    ExpressionExperimentSet ees = ( ExpressionExperimentSet ) o;
                    currentTaxon = ees.getTaxon();

                } else if ( o instanceof Gene ) {
                    Gene gene = ( Gene ) o;
                    currentTaxon = gene.getTaxon();

                } else if ( o instanceof GeneSet ) {
                    GeneSet geneSet = ( GeneSet ) o;
                    currentTaxon = geneSetService.getTaxon( geneSet );

                } else if ( o instanceof CharacteristicValueObject ) {
                    CharacteristicValueObject charVO = ( CharacteristicValueObject ) o;
                    currentTaxon = taxonService.findByCommonName( charVO.getTaxon() );

                } else {
                    Method m = o.getClass().getMethod( "getTaxon" );
                    currentTaxon = ( Taxon ) m.invoke( o );
                }

                if ( currentTaxon == null || !currentTaxon.getId().equals( t.getId() ) ) {
                    if ( currentTaxon == null ) {
                        // Sanity check for bad data in db (could happen if EE has no samples). Can happen that
                        // searchResults have a vaild getTaxon method
                        // but the method returns null (shouldn't make it this far)
                        DatabaseSearchSource.log.debug( "Object has getTaxon method but it returns null. Obj is: " + o );
                    }
                    toRemove.add( sr );
                }
            } catch ( SecurityException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( NoSuchMethodException e ) {
                /*
                 * In case of a programming error where the results don't have a taxon at all, we assume we should
                 * filter them out but issue a warning.
                 */
                toRemove.add( sr );
                DatabaseSearchSource.log
                        .warn( "No getTaxon method for: " + o.getClass() + ".  Filtering from results. Error was: "
                                + e );

            }
        }
        results.removeAll( toRemove );
    }
}
