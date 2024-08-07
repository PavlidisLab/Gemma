package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.QueryUtils.*;

@Service
@CommonsLog
public class CachedProcessedExpressionDataVectorServiceImpl implements CachedProcessedExpressionDataVectorService {

    private final ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;
    private final SessionFactory sessionFactory;

    @Autowired
    private ProcessedDataVectorByGeneCache processedDataVectorByGeneCache;

    @Autowired
    public CachedProcessedExpressionDataVectorServiceImpl( ProcessedExpressionDataVectorDao processedExpressionDataVectorDao, SessionFactory sessionFactory ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment ) {
        Collection<ProcessedExpressionDataVector> pedvs = processedExpressionDataVectorDao.getProcessedVectors( this.getExperiment( expressionExperiment ) );

        if ( pedvs.isEmpty() ) {
            log.warn( "No processed vectors for experiment " + expressionExperiment );
            return new HashSet<>();
        }

        Map<ProcessedExpressionDataVector, Collection<Long>> vector2gene = processedExpressionDataVectorDao.getGenes( pedvs );

        Collection<BioAssayDimension> bioAssayDimensions = getBioAssayDimensions( expressionExperiment );

        if ( bioAssayDimensions.size() == 1 ) {
            return this.unpack( pedvs, vector2gene ).values();
        }

        /*
         * deal with 'misalignment problem'
         */

        BioAssayDimension longestBad = this.checkRagged( bioAssayDimensions );

        if ( longestBad != null ) {
            return this.unpack( pedvs, vector2gene, longestBad );
        }
        return this.unpack( pedvs, vector2gene ).values();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Long> genes ) {
        Collection<BioAssaySet> expressionExperiments = new HashSet<>();
        expressionExperiments.add( expressionExperiment );
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getRandomProcessedDataArrays( BioAssaySet ee, int limit ) {

        Collection<ProcessedExpressionDataVector> pedvs = this.getRandomProcessedVectors( this.getExperiment( ee ), limit );

        if ( pedvs.isEmpty() ) {
            log.warn( "No processed vectors for experiment " + ee );
            return new HashSet<>();
        }

        Map<ProcessedExpressionDataVector, Collection<Long>> cs2gene = processedExpressionDataVectorDao.getGenes( pedvs );

        Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

        if ( bioAssayDimensions.size() == 1 ) {
            return this.unpack( pedvs, cs2gene ).values();
        }

        /*
         * deal with 'misalignment problem'
         */

        BioAssayDimension longestBad = this.checkRagged( bioAssayDimensions );

        if ( longestBad != null ) {
            return this.unpack( pedvs, cs2gene, longestBad );
        }
        return this.unpack( pedvs, cs2gene ).values();
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<Long> genes ) {
        return this.handleGetProcessedExpressionDataArrays( expressionExperiments, genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<? extends BioAssaySet> ees,
            Collection<CompositeSequence> probes ) {

        if ( probes.isEmpty() )
            return new HashSet<>();

        Collection<Long> probeIds = EntityUtils.getIds( probes );

        return this.getProcessedDataArraysByProbeIds( ees, probeIds );

    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee,
            Collection<Long> probes ) {
        return this.getProcessedDataArraysByProbeIds( Collections.singleton( ee ), probes );
    }

    private Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( Collection<? extends BioAssaySet> ees,
            Collection<Long> probeIds ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();

        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneMapForProbes( probeIds, this.sessionFactory.getCurrentSession() );

        Map<Long, Collection<Long>> noGeneProbes = new HashMap<>();
        for ( Long pid : probeIds ) {
            if ( !cs2gene.containsKey( pid ) || cs2gene.get( pid ).isEmpty() ) {
                noGeneProbes.put( pid, new HashSet<>() );
                cs2gene.remove( pid );
            }
        }

        log.debug( cs2gene.size() + " probes associated with a gene; " + noGeneProbes.size() + " not" );

        /*
         * To Check the cache we need the list of genes 1st. Get from CS2Gene list then check the cache.
         */
        Collection<Long> genes = new HashSet<>();
        for ( Long cs : cs2gene.keySet() ) {
            genes.addAll( cs2gene.get( cs ) );
        }

        // this will be populated with experiments for which we don't have all the needed results cached
        Collection<ExpressionExperiment> needToSearch = new HashSet<>();
        // will contain IDs of genes that weren't covered by the cache
        Collection<Long> genesToSearch = new HashSet<>();
        this.checkCache( ees, genes, results, needToSearch, genesToSearch );

        if ( !results.isEmpty() )
            log.debug( results.size() + " vectors fetched from cache" );

        /*
         * Get data that wasn't in the cache.
         *
         * Small problem: noGeneProbes are never really cached since we use the gene as part of that. So always need to get them.
         */
        Map<ProcessedExpressionDataVector, Collection<Long>> noncached = new HashMap<>();
        if ( !noGeneProbes.isEmpty() ) {
            Collection<ExpressionExperiment> eesForNoGeneProbes = new HashSet<>();
            for ( BioAssaySet ee : ees ) {
                if ( ee instanceof ExpressionExperiment ) {
                    eesForNoGeneProbes.add( ( ExpressionExperiment ) ee );
                } else {
                    eesForNoGeneProbes.add( ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment() );
                }
            }
            needToSearch.addAll( eesForNoGeneProbes );
            noncached.putAll( this.getProcessedVectorsAndGenes( eesForNoGeneProbes, noGeneProbes ) );
        }

        if ( !noncached.isEmpty() )
            log.debug( noncached.size() + " vectors retrieved so far, for noGeneProbes" );

        /*
         * Non-cached items.
         */
        Map<ProcessedExpressionDataVector, Collection<Long>> moreNonCached = new HashMap<>();
        if ( !needToSearch.isEmpty() && !genesToSearch.isEmpty() ) {
            /*
             * cut cs2gene down, otherwise we're probably fetching everything again.
             */
            Map<Long, Collection<Long>> filteredcs2gene = cs2gene.entrySet().stream()
                    .filter( entry -> entry.getValue().stream().anyMatch( genesToSearch::contains ) )
                    .collect( Collectors.toMap( Map.Entry::getKey, Map.Entry::getValue ) );

            moreNonCached = this.getProcessedVectorsAndGenes( needToSearch, filteredcs2gene );
        }

        if ( !moreNonCached.isEmpty() )
            log.debug( noncached.size() + " more fetched from db" );

        noncached.putAll( moreNonCached );

        /*
         * Deal with possibility of 'gaps' and unpack the vectors.
         */
        Collection<DoubleVectorValueObject> newResults = new HashSet<>();
        for ( ExpressionExperiment ee : needToSearch ) {

            Collection<BioAssayDimension> bioAssayDimensions = this.getBioAssayDimensions( ee );

            if ( bioAssayDimensions.size() == 1 ) {
                newResults.addAll( this.unpack( noncached ) );
            } else {
                /*
                 * See handleGetProcessedExpressionDataArrays(Collection<? extends BioAssaySet>, Collection<Gene>,
                 * boolean) and bug 1704.
                 */
                BioAssayDimension longestBad = this.checkRagged( bioAssayDimensions );
                assert longestBad != null;
                newResults.addAll( this.unpack( noncached, longestBad ) );
            }

            if ( !newResults.isEmpty() ) {
                this.cacheResults( newResults );

                newResults = this.sliceSubsets( ees, newResults );

                results.addAll( newResults );
            }
        }

        return results;
    }

    /**
     * This is an important method for fetching vectors.
     *
     * @param  genes genes
     * @param  ees   ees
     * @return vectors, possibly subsetted.
     */
    private Collection<DoubleVectorValueObject> handleGetProcessedExpressionDataArrays(
            Collection<? extends BioAssaySet> ees, Collection<Long> genes ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<>();

        /*
         * Check the cache.
         */
        // using a TreeSet to prevent hashCode() from initializing proxies
        Collection<ExpressionExperiment> needToSearch = new TreeSet<>( Comparator.comparing( ExpressionExperiment::getId ) );
        Collection<Long> genesToSearch = new HashSet<>();
        this.checkCache( ees, genes, results, needToSearch, genesToSearch );
        log.info( "Using " + results.size() + " DoubleVectorValueObject(s) from cache" );

        if ( needToSearch.isEmpty() ) {
            return results;
        }

        /*
         * Get items not in the cache.
         */
        log.info( "Searching for vectors for " + genes.size() + " genes from " + needToSearch.size()
                + " experiments not in cache" );

        Collection<ArrayDesign> arrays = CommonQueries
                .getArrayDesignsUsed( EntityUtils.getIds( this.getExperiments( ees ) ),
                        this.sessionFactory.getCurrentSession() )
                .keySet();
        assert !arrays.isEmpty();
        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneIdMap( genesToSearch, EntityUtils.getIds( arrays ),
                        this.sessionFactory.getCurrentSession() );

        if ( cs2gene.isEmpty() ) {
            if ( results.isEmpty() ) {
                log.warn( "No composite sequences found for genes" );
                return new HashSet<>();
            }
            return results;
        }

        /*
         * Fill in the map, because we want to track information on the specificity of the probes used in the data
         * vectors.
         */
        cs2gene = CommonQueries
                .getCs2GeneMapForProbes( cs2gene.keySet(), this.sessionFactory.getCurrentSession() );

        Map<ProcessedExpressionDataVector, Collection<Long>> processedDataVectors = this
                .getProcessedVectorsAndGenes( needToSearch, cs2gene );

        Map<BioAssaySet, Collection<BioAssayDimension>> bioAssayDimensions = this.getBioAssayDimensions( needToSearch );

        Collection<DoubleVectorValueObject> newResults = new HashSet<>();

        /*
         * This loop is to ensure that we don't get misaligned vectors for experiments that use more than one array
         * design. See bug 1704. This isn't that common, so we try to break out as soon as possible.
         */
        for ( BioAssaySet bas : needToSearch ) {

            Collection<BioAssayDimension> dims = bioAssayDimensions.get( bas );

            if ( dims == null || dims.isEmpty() ) {
                log.warn( "BioAssayDimensions were null/empty unexpectedly." );
                continue;
            }

            /*
             * Get the vectors for just this experiment. This is made more efficient by removing things from the map
             * each time through.
             */
            Map<ProcessedExpressionDataVector, Collection<Long>> vecsForBas = new HashMap<>();
            if ( needToSearch.size() == 1 ) {
                vecsForBas = processedDataVectors;
            } else {
                // isolate the vectors for the current experiment.
                for ( Iterator<ProcessedExpressionDataVector> it = processedDataVectors.keySet().iterator(); it
                        .hasNext(); ) {
                    ProcessedExpressionDataVector v = it.next();
                    if ( v.getExpressionExperiment().equals( bas ) ) {
                        vecsForBas.put( v, processedDataVectors.get( v ) );
                        it.remove(); // since we're done with it.
                    }
                }
            }

            /*
             * Now see if anything is 'ragged' (fewer bioassays per biomaterial than in some other vector)
             */
            if ( dims.size() == 1 ) {
                newResults.addAll( this.unpack( vecsForBas ) );
            } else {
                BioAssayDimension longestBad = this.checkRagged( dims );
                if ( longestBad == null ) {
                    newResults.addAll( this.unpack( vecsForBas ) );
                } else {
                    newResults.addAll( this.unpack( vecsForBas, longestBad ) );
                }
            }
        }

        /*
         * Finally....
         */

        if ( !newResults.isEmpty() ) {
            this.cacheResults( newResults );
            newResults = this.sliceSubsets( ees, newResults );
            results.addAll( newResults );
        }

        return results;
    }

    /**
     * @param newResults Always provide full vectors, not subsets.
     */
    private void cacheResults( Collection<DoubleVectorValueObject> newResults ) {
        /*
         * Break up by gene and EE to cache collections of vectors for EE-gene combos.
         */
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = this.makeCacheMap( newResults );
        int i = 0;
        for ( Long eeid : mapForCache.keySet() ) {
            for ( Long g : mapForCache.get( eeid ).keySet() ) {
                i++;
                this.processedDataVectorByGeneCache.putById( eeid, g, mapForCache.get( eeid ).get( g ) );
            }
        }
        // WARNING cache size() can be slow, esp. terracotta.
        log.debug( "Cached " + i + ", input " + newResults.size() + "; total cached: "
                /* + this.processedDataVectorCache.size() */ );
    }

    /**
     * We cache vectors at the experiment level. If we need subsets, we have to slice them out.
     *
     * @param bioAssaySets  that we exactly need the data for.
     * @param genes         that might have cached results
     * @param results       from the cache will be put here
     * @param needToSearch  experiments that need to be searched (not fully cached); this will be populated
     * @param genesToSearch that still need to be searched (not in cache)
     */
    private void checkCache( Collection<? extends BioAssaySet> bioAssaySets, Collection<Long> genes,
            Collection<DoubleVectorValueObject> results, Collection<ExpressionExperiment> needToSearch,
            Collection<Long> genesToSearch ) {

        for ( BioAssaySet ee : bioAssaySets ) {

            ExpressionExperiment experiment = null;
            boolean needSubSet = false;
            if ( ee instanceof ExpressionExperiment ) {
                experiment = ( ExpressionExperiment ) ee;
            } else if ( ee instanceof ExpressionExperimentSubSet ) {
                experiment = ( ( ExpressionExperimentSubSet ) ee ).getSourceExperiment();
                needSubSet = true;
            }

            assert experiment != null;

            for ( Long g : genes ) {
                Collection<DoubleVectorValueObject> obs = processedDataVectorByGeneCache.getById( ee.getId(), g );
                if ( obs != null ) {
                    if ( needSubSet ) {
                        obs = this.sliceSubSet( ( ExpressionExperimentSubSet ) ee, obs );
                    }
                    results.addAll( obs );
                } else {
                    genesToSearch.add( g );
                }
            }
            /*
             * This experiment is not fully cached for the genes in question.
             */
            if ( !genesToSearch.isEmpty() ) {
                needToSearch.add( experiment );
            }
        }
    }

    /**
     * @param  bioAssayDimensions See if anything is 'ragged' (fewer bioassays per biomaterial than in some other
     *                            sample)
     * @return bio assay dimension
     */
    private BioAssayDimension checkRagged( Collection<BioAssayDimension> bioAssayDimensions ) {
        int s = -1;
        int longest = -1;
        BioAssayDimension longestBad = null;
        for ( BioAssayDimension bad : bioAssayDimensions ) {
            Collection<BioAssay> assays = bad.getBioAssays();
            if ( s < 0 ) {
                s = assays.size();
            }

            if ( assays.size() > longest ) {
                longest = assays.size();
                longestBad = bad;
            }
        }
        return longestBad;
    }

    /**
     * Obtain a random sample of processed vectors for the given experiment.
     * @param  ee    ee
     * @param  limit if >0, you will get a "random" set of vectors for the experiment
     * @return processed data vectors
     */
    private Collection<ProcessedExpressionDataVector> getRandomProcessedVectors( ExpressionExperiment ee, int limit ) {
        if ( limit <= 0 ) {
            return processedExpressionDataVectorDao.getProcessedVectors( ee );
        }

        StopWatch timer = StopWatch.createStarted();

        Integer availableVectorCount = ee.getNumberOfDataVectors();
        if ( availableVectorCount == null || availableVectorCount == 0 ) {
            log.info( "Experiment does not have vector count populated." );
            // cannot fix this here, because we're read-only.
        }

        //noinspection unchecked
        List<ProcessedExpressionDataVector> result = this.sessionFactory.getCurrentSession()
                .createQuery( " from ProcessedExpressionDataVector dedv "
                        + "where dedv.expressionExperiment = :ee and dedv.rankByMean > 0.5 order by RAND()" ) // order by rand() works?
                .setParameter( "ee", ee )
                .setMaxResults( limit )
                .list();

        // maybe ranks are not set for some reason; can happen e.g. GeneSpring mangled data.
        if ( result.isEmpty() ) {
            //noinspection unchecked
            result = this.sessionFactory.getCurrentSession()
                    .createQuery( " from ProcessedExpressionDataVector dedv "
                            + "where dedv.expressionExperiment = :ee order by RAND()" )
                    .setParameter( "ee", ee )
                    .setMaxResults( limit )
                    .list();
        }

        processedExpressionDataVectorDao.thaw( result ); // needed?

        if ( result.isEmpty() ) {
            log.warn( "Experiment does not have any processed data vectors to display? " + ee );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( String.format( "Fetch %d random vectors from %s took %d ms.", result.size(), ee.getShortName(), timer.getTime() ) );
        }

        return result;
    }

    private Map<CompositeSequence, DoubleVectorValueObject> unpack( Collection<ProcessedExpressionDataVector> data,
            Map<ProcessedExpressionDataVector, Collection<Long>> vector2GeneMap ) {
        Map<CompositeSequence, DoubleVectorValueObject> result = new HashMap<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );
        for ( ProcessedExpressionDataVector v : data ) {
            result.put( v.getDesignElement(),
                    new DoubleVectorValueObject( v, vector2GeneMap.get( v ),
                            badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack( Collection<ProcessedExpressionDataVector> data,
            Map<ProcessedExpressionDataVector, Collection<Long>> vector2GeneMap, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this.getBioAssayDimensionValueObjects( data );
        for ( ProcessedExpressionDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ),
                    vector2GeneMap.get( v ), longestBad ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack( Map<ProcessedExpressionDataVector, Collection<Long>> data ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this
                .getBioAssayDimensionValueObjects( data.keySet() );
        for ( ProcessedExpressionDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, data.get( v ), badVos.get( v.getBioAssayDimension() ) ) );
        }
        return result;
    }

    private Collection<? extends DoubleVectorValueObject> unpack(
            Map<ProcessedExpressionDataVector, Collection<Long>> data, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = this
                .getBioAssayDimensionValueObjects( data.keySet() );
        for ( ProcessedExpressionDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, badVos.get( v.getBioAssayDimension() ), data.get( v ),
                    longestBad ) );
        }
        return result;
    }

    /**
     * @param  ees  Experiments and/or subsets required
     * @param  vecs vectors to select from and if necessary slice, obviously from the given ees.
     * @return vectors that are for the requested subset. If an ee is not a subset, vectors will be unchanged.
     *              Otherwise
     *              the data in a vector will be for the subset of samples in the ee subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubsets( Collection<? extends BioAssaySet> ees,
            Collection<DoubleVectorValueObject> vecs ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();
        if ( vecs == null || vecs.isEmpty() )
            return results;

        for ( BioAssaySet bas : ees ) {
            if ( bas instanceof ExpressionExperimentSubSet ) {

                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().getId()
                            .equals( ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment().getId() ) ) {

                        Collection<DoubleVectorValueObject> ddvos = new HashSet<>();
                        ddvos.add( d );
                        results.addAll( this.sliceSubSet( ( ExpressionExperimentSubSet ) bas, ddvos ) );// coll

                    }
                }

            } else {
                for ( DoubleVectorValueObject d : vecs ) {
                    if ( d.getExpressionExperiment().getId().equals( bas.getId() ) ) {
                        results.add( d );
                    }
                }
            }

        }

        return results;
    }

    /**
     * @param  ee  ee
     * @param  obs obs
     * @return Given an ExpressionExperimentSubset and vectors from the source experiment, give vectors that include
     *             just the
     *             data for the subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubSet( ExpressionExperimentSubSet ee,
            Collection<DoubleVectorValueObject> obs ) {

        Collection<DoubleVectorValueObject> sliced = new HashSet<>();
        if ( obs == null || obs.isEmpty() )
            return sliced;

        Hibernate.initialize( ee.getBioAssays() );
        List<BioAssayValueObject> sliceBioAssays = new ArrayList<>();

        DoubleVectorValueObject exemplar = obs.iterator().next();

        BioAssayDimensionValueObject bad = new BioAssayDimensionValueObject( -1L );
        bad.setName( "Subset of :" + exemplar.getBioAssayDimension().getName() );
        bad.setDescription( "Subset slice" );
        bad.setSourceBioAssayDimension( exemplar.getBioAssayDimension() );
        bad.setIsSubset( true );
        Collection<Long> subsetBioAssayIds = EntityUtils.getIds( ee.getBioAssays() );

        for ( BioAssayValueObject ba : exemplar.getBioAssays() ) {
            if ( !subsetBioAssayIds.contains( ba.getId() ) ) {
                continue;
            }

            sliceBioAssays.add( ba );
        }

        bad.addBioAssays( sliceBioAssays );
        for ( DoubleVectorValueObject vec : obs ) {
            DoubleVectorValueObject s = vec.slice( ee, bad );
            sliced.add( s );
        }

        return sliced;
    }

    /**
     * Obtain processed expression vectors with their associated genes.
     *
     * @param  cs2gene Map of probe to genes.
     * @param  ees     ees
     * @return map of vectors to genes.
     */
    private Map<ProcessedExpressionDataVector, Collection<Long>> getProcessedVectorsAndGenes( @Nullable Collection<ExpressionExperiment> ees, Map<Long, Collection<Long>> cs2gene ) {
        if ( ( ees != null && ees.isEmpty() ) || cs2gene.isEmpty() ) {
            return Collections.emptyMap();
        }

        StopWatch timer = StopWatch.createStarted();

        // Do not do in clause for experiments, as it can't use the indices
        Query queryObject = this.sessionFactory.getCurrentSession().createQuery(
                        "select dedv from ProcessedExpressionDataVector dedv "
                                + "where dedv.designElement.id in ( :cs )"
                                + ( ees != null ? " and dedv.expressionExperiment in :ees" : "" ) )
                .setParameterList( "cs", optimizeParameterList( cs2gene.keySet() ) );
        List<ProcessedExpressionDataVector> results;
        if ( ees != null ) {
            results = listByIdentifiableBatch( queryObject, "ees", ees, 2048 );
        } else {
            //noinspection unchecked
            results = queryObject.list();
        }
        Map<ProcessedExpressionDataVector, Collection<Long>> dedv2genes = new HashMap<>();
        for ( ProcessedExpressionDataVector dedv : results ) {
            Collection<Long> associatedGenes = cs2gene.get( dedv.getDesignElement().getId() );
            if ( !dedv2genes.containsKey( dedv ) ) {
                dedv2genes.put( dedv, associatedGenes );
            } else {
                Collection<Long> mappedGenes = dedv2genes.get( dedv );
                mappedGenes.addAll( associatedGenes );
            }
        }

        if ( timer.getTime() > Math.max( 200, 20 * dedv2genes.size() ) ) {
            log.warn( String.format( "Fetched %d vectors for %d probes in %dms",
                    dedv2genes.size(), cs2gene.size(), timer.getTime() ) );

        }

        return dedv2genes;
    }

    private Map<BioAssaySet, Collection<BioAssayDimension>> getBioAssayDimensions(
            Collection<ExpressionExperiment> ees ) {
        Map<BioAssaySet, Collection<BioAssayDimension>> result = new HashMap<>();

        if ( ees.size() == 1 ) {
            ExpressionExperiment ee = ees.iterator().next();
            result.put( ee, this.getBioAssayDimensions( ee ) );
            return result;
        }

        StopWatch timer = new StopWatch();
        timer.start();
        //noinspection unchecked
        List<Object[]> r = this.sessionFactory.getCurrentSession().createQuery(
                        "select e, bad from ExpressionExperiment e, BioAssayDimension bad "
                                + "inner join e.bioAssays b "
                                + "inner join bad.bioAssays badba "
                                + "where e in (:ees) and b in (badba) "
                                + "group by e, bad" )
                .setParameterList( "ees", optimizeIdentifiableParameterList( ees ) )
                .list();

        for ( Object[] o : r ) {
            BioAssaySet bas = ( BioAssaySet ) o[0];
            if ( !result.containsKey( bas ) )
                result.put( bas, new HashSet<>() );

            result.get( bas ).add( ( BioAssayDimension ) o[1] );
        }
        if ( timer.getTime() > 100 ) {
            log.info( "Fetch " + r.size() + " bioAssayDimensions for " + ees.size() + " experiment(s): " + timer.getTime() + " ms" );
        }

        return result;

    }

    private Collection<BioAssayDimension> getBioAssayDimensions( BioAssaySet ee ) {
        if ( ee instanceof ExpressionExperiment ) {
            StopWatch timer = new StopWatch();
            timer.start();
            //noinspection unchecked
            List<BioAssayDimension> r = sessionFactory.getCurrentSession().createQuery(
                            // this does not look efficient.
                            "select bad from ExpressionExperiment e, BioAssayDimension bad "
                                    + "inner join e.bioAssays b "
                                    + "inner join bad.bioAssays badba "
                                    + "where e = :ee and b in (badba) "
                                    + "group by bad" )
                    .setParameter( "ee", ee )
                    .list();
            timer.stop();
            if ( timer.getTime() > 100 ) {
                log.info( "Fetch " + r.size() + " bioassayDimensions for experiment id=" + ee.getId() + ": "
                        + timer.getTime() + "ms" );
            }
            return r;
        }

        // subset.
        return this.getBioAssayDimensions( this.getExperiment( ee ) );

    }

    private ExpressionExperiment getExperiment( BioAssaySet bas ) {
        ExpressionExperiment e;
        if ( bas instanceof ExpressionExperiment ) {
            e = ( ExpressionExperiment ) bas;
        } else if ( bas instanceof ExpressionExperimentSubSet ) {
            e = ( ( ExpressionExperimentSubSet ) bas ).getSourceExperiment();
        } else {
            throw new UnsupportedOperationException( "Couldn't handle a " + bas.getClass() );
        }
        assert e != null;
        return e;
    }

    /**
     * Determine the experiments that bioAssaySets refer to.
     *
     * @param  bioAssaySets - either ExpressionExperiment or ExpressionExperimentSubSet (which has an associated
     *                      ExpressionExperiment, which is what we're after)
     * @return Note that this collection can be smaller than the input, if two bioAssaySets come from (or
     *                      are) the same
     *                      Experiment
     */
    private Collection<ExpressionExperiment> getExperiments( Collection<? extends BioAssaySet> bioAssaySets ) {
        Collection<ExpressionExperiment> result = new TreeSet<>( Comparator.comparing( BioAssaySet::getId ) );
        for ( BioAssaySet bas : bioAssaySets ) {
            ExpressionExperiment e = this.getExperiment( bas );
            result.add( e );
        }
        return result;
    }

    private Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> makeCacheMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = new HashMap<>();
        for ( DoubleVectorValueObject v : newResults ) {
            ExpressionExperimentValueObject e = v.getExpressionExperiment();
            if ( !mapForCache.containsKey( e.getId() ) ) {
                mapForCache.put( e.getId(), new HashMap<>() );
            }
            Map<Long, Collection<DoubleVectorValueObject>> innerMap = mapForCache.get( e.getId() );
            for ( Long g : v.getGenes() ) {
                if ( !innerMap.containsKey( g ) ) {
                    innerMap.put( g, new HashSet<>() );
                }
                innerMap.get( g ).add( v );
            }
        }
        return mapForCache;
    }

    /**
     * @param  data data
     * @return Pre-fetch and construct the BioAssayDimensionValueObjects. Used on the basis that the data probably
     *              just
     *              have one
     *              (or a few) BioAssayDimensionValueObjects needed, not a different one for each vector. See bug 3629
     *              for
     *              details.
     */
    private Map<BioAssayDimension, BioAssayDimensionValueObject> getBioAssayDimensionValueObjects(
            Collection<ProcessedExpressionDataVector> data ) {
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = new HashMap<>();
        for ( ProcessedExpressionDataVector v : data ) {
            BioAssayDimension bioAssayDimension = v.getBioAssayDimension();
            if ( !badVos.containsKey( bioAssayDimension ) ) {
                badVos.put( bioAssayDimension, new BioAssayDimensionValueObject( bioAssayDimension ) );
            }
        }
        return badVos;
    }
}
