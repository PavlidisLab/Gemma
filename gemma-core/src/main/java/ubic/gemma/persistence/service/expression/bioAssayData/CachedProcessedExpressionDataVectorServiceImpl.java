package ubic.gemma.persistence.service.expression.bioAssayData;

import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import ubic.gemma.core.util.ListUtils;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimensionValueObject;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.CommonQueries;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@CommonsLog
class CachedProcessedExpressionDataVectorServiceImpl implements CachedProcessedExpressionDataVectorService {

    private final ProcessedExpressionDataVectorDao processedExpressionDataVectorDao;
    private final SessionFactory sessionFactory;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ProcessedDataVectorByGeneCache processedDataVectorByGeneCache;

    @Autowired
    private ProcessedDataVectorCache processedDataVectorCache;

    @Autowired
    public CachedProcessedExpressionDataVectorServiceImpl( ProcessedExpressionDataVectorDao processedExpressionDataVectorDao, SessionFactory sessionFactory ) {
        this.processedExpressionDataVectorDao = processedExpressionDataVectorDao;
        this.sessionFactory = sessionFactory;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet bas ) {
        ExpressionExperiment ee = getExperiment( bas );
        Collection<DoubleVectorValueObject> cachedResults = processedDataVectorCache.get( ee );
        if ( cachedResults != null ) {
            return sliceSubSet( bas, cachedResults );
        }

        Collection<ProcessedExpressionDataVector> pedvs = processedExpressionDataVectorDao.getProcessedVectors( ee );

        Collection<DoubleVectorValueObject> results;
        if ( pedvs.isEmpty() ) {
            log.warn( "No processed vectors for experiment " + bas );
            results = new HashSet<>();
        } else {
            Map<ProcessedExpressionDataVector, Collection<Long>> vector2gene = processedExpressionDataVectorDao.getGenes( pedvs );

            // this works for both experiment, their subsets and also sub-bioassays
            Collection<BioAssayDimension> bioAssayDimensions = getBioAssayDimensions( bas );

            if ( bioAssayDimensions.size() == 1 ) {
                results = unpack( pedvs, vector2gene );
            } else {
                /*
                 * deal with 'misalignment problem'
                 */
                BioAssayDimension longestBad = this.checkRagged( bioAssayDimensions );
                if ( longestBad != null ) {
                    results = unpack( pedvs, vector2gene, longestBad );
                } else {
                    results = unpack( pedvs, vector2gene );
                }
            }
        }

        processedDataVectorCache.put( getExperiment( bas ), results );

        return sliceSubSet( bas, results );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays( BioAssaySet expressionExperiment,
            Collection<Long> genes ) {
        return getProcessedDataArrays( Collections.singleton( expressionExperiment ), genes );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getRandomProcessedDataArrays( BioAssaySet bas, int limit ) {
        Assert.isTrue( limit > 0, "Limit must be greater than 0" );
        ExpressionExperiment ee = getExperiment( bas );

        // if cached results exist, sample from there instead
        Collection<DoubleVectorValueObject> cachedVectors = processedDataVectorCache.get( ee );
        if ( cachedVectors != null ) {
            log.debug( "Encountered a cached result for " + bas + ", sampling from there instead." );
            List<DoubleVectorValueObject> vectorsL = new ArrayList<>( cachedVectors );
            Collections.shuffle( vectorsL );
            return sliceSubSet( bas, vectorsL.subList( 0, Math.min( limit, vectorsL.size() ) ) );
        }

        // since we're not getting all the vectors, we don't bother caching the results

        Collection<ProcessedExpressionDataVector> pedvs = this.processedExpressionDataVectorDao.getRandomProcessedVectors( ee, limit );

        if ( pedvs.isEmpty() ) {
            log.warn( "No processed vectors for experiment " + bas );
            return new HashSet<>();
        }

        Map<ProcessedExpressionDataVector, Collection<Long>> cs2gene = processedExpressionDataVectorDao.getGenes( pedvs );

        // this works for both experiment, their subsets and also sub-bioassays
        Collection<BioAssayDimension> bioAssayDimensions = getBioAssayDimensions( bas );

        if ( bioAssayDimensions.size() == 1 ) {
            return sliceSubSet( bas, unpack( pedvs, cs2gene ) );
        }

        /*
         * deal with 'misalignment problem'
         */

        BioAssayDimension longestBad = this.checkRagged( bioAssayDimensions );

        if ( longestBad != null ) {
            return sliceSubSet( bas, unpack( pedvs, cs2gene, longestBad ) );
        }
        return sliceSubSet( bas, unpack( pedvs, cs2gene ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArrays(
            Collection<? extends BioAssaySet> expressionExperiments, Collection<Long> genes ) {

        // ees must be thawed first as currently implemented (?)

        Collection<DoubleVectorValueObject> results = new HashSet<>();

        /*
         * Check the cache.
         */
        // using a TreeSet to prevent hashCode() from initializing proxies
        Collection<BioAssaySet> needToSearch = new TreeSet<>( Comparator.comparing( BioAssaySet::getId ) );
        Collection<Long> genesToSearch = new HashSet<>();
        this.checkCache( expressionExperiments, genes, results, needToSearch, genesToSearch );
        log.info( "Using " + results.size() + " DoubleVectorValueObject(s) from cache" );

        if ( needToSearch.isEmpty() ) {
            return results;
        }

        /*
         * Get items not in the cache.
         */
        log.info( "Searching for vectors for " + genes.size() + " genes from " + needToSearch.size()
                + " experiments not in cache" );

        Collection<ArrayDesign> arrays = CommonQueries.getArrayDesignsUsed( expressionExperiments, this.sessionFactory.getCurrentSession() );
        assert !arrays.isEmpty();
        Map<Long, Collection<Long>> cs2gene = CommonQueries
                .getCs2GeneIdMapForGenes( genesToSearch, IdentifiableUtils.getIds( arrays ),
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

        List<ExpressionExperiment> eesToSearch = needToSearch.stream()
                .map( this::getExperiment )
                .collect( Collectors.toList() );

        Map<ProcessedExpressionDataVector, Collection<Long>> processedDataVectors = this.processedExpressionDataVectorDao
                .getProcessedVectorsAndGenes( eesToSearch, cs2gene );

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
            newResults = this.sliceSubsets( expressionExperiments, newResults );
            results.addAll( newResults );
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( BioAssaySet ee, Collection<CompositeSequence> compositeSequences ) {
        return getProcessedDataArraysByProbeIds( Collections.singleton( ee ), IdentifiableUtils.getIds( compositeSequences ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbe( Collection<? extends BioAssaySet> ees,
            Collection<CompositeSequence> probes ) {
        return getProcessedDataArraysByProbeIds( ees, IdentifiableUtils.getIds( probes ) );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( BioAssaySet ee,
            Collection<Long> probes ) {
        return getProcessedDataArraysByProbeIds( Collections.singleton( ee ), probes );
    }

    private Collection<DoubleVectorValueObject> getProcessedDataArraysByProbeIds( Collection<? extends BioAssaySet> ees,
            Collection<Long> probeIds ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();

        if ( probeIds.isEmpty() ) {
            return results;
        }

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
        Collection<BioAssaySet> needToSearch = new HashSet<>();
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
            noncached.putAll( this.processedExpressionDataVectorDao.getProcessedVectorsAndGenes( eesForNoGeneProbes, noGeneProbes ) );
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

            List<ExpressionExperiment> eesToSearch = needToSearch.stream()
                    .map( this::getExperiment )
                    .collect( Collectors.toList() );

            moreNonCached = this.processedExpressionDataVectorDao.getProcessedVectorsAndGenes( eesToSearch, filteredcs2gene );
        }

        if ( !moreNonCached.isEmpty() )
            log.debug( noncached.size() + " more fetched from db" );

        noncached.putAll( moreNonCached );

        /*
         * Deal with possibility of 'gaps' and unpack the vectors.
         */
        Collection<DoubleVectorValueObject> newResults = new HashSet<>();
        for ( BioAssaySet ee : needToSearch ) {

            // this works for both experiment, their subsets and also sub-bioassays
            Collection<BioAssayDimension> bioAssayDimensions = getBioAssayDimensions( ee );

            if ( bioAssayDimensions.isEmpty() ) {
                continue;
            } else if ( bioAssayDimensions.size() == 1 ) {
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

    @Override
    // does not need to be transactional
    public void evict( BioAssaySet bas ) {
        ExpressionExperiment ee = getExperiment( bas );
        processedDataVectorCache.evict( ee );
        processedDataVectorByGeneCache.evict( ee );
    }

    /**
     * Store vectors in the cache.
     *
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

    private Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> makeCacheMap(
            Collection<DoubleVectorValueObject> newResults ) {
        Map<Long, Map<Long, Collection<DoubleVectorValueObject>>> mapForCache = new HashMap<>();
        for ( DoubleVectorValueObject v : newResults ) {
            Map<Long, Collection<DoubleVectorValueObject>> innerMap = mapForCache
                    .computeIfAbsent( v.getExpressionExperiment().getId(), k -> new HashMap<>() );
            if ( v.getGenes() == null ) {
                throw new IllegalStateException( "Cannot cache a vector without genes." );
            }
            for ( Long g : v.getGenes() ) {
                innerMap.computeIfAbsent( g, k -> new HashSet<>() ).add( v );
            }
        }
        return mapForCache;
    }


    /**
     * Retrieve vectors from the cache.
     *
     * @param bioAssaySets  that we exactly need the data for.
     * @param genes         that might have cached results
     * @param results       from the cache will be put here
     * @param needToSearch  experiments that need to be searched (not fully cached); this will be populated
     * @param genesToSearch that still need to be searched (not in cache)
     */
    private void checkCache( Collection<? extends BioAssaySet> bioAssaySets, Collection<Long> genes,
            Collection<DoubleVectorValueObject> results, Collection<BioAssaySet> needToSearch,
            Collection<Long> genesToSearch ) {

        for ( BioAssaySet ee : bioAssaySets ) {

            for ( Long g : genes ) {
                Collection<DoubleVectorValueObject> obs = processedDataVectorByGeneCache.get( getExperiment( ee ), g );
                if ( obs != null ) {
                    if ( ee instanceof ExpressionExperimentSubSet ) {
                        // we cache vectors at the experiment level. If we need subsets, we have to slice them out.
                        results.addAll( this.sliceSubSet( ( ExpressionExperimentSubSet ) ee, obs ) );
                    } else {
                        results.addAll( obs );
                    }
                } else {
                    genesToSearch.add( g );
                }
            }
            /*
             * This experiment is not fully cached for the genes in question.
             */
            if ( !genesToSearch.isEmpty() ) {
                needToSearch.add( ee );
            }
        }
    }

    /**
     * @param bioAssayDimensions See if anything is 'ragged' (fewer bioassays per biomaterial than in some other
     *                           sample)
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


    private Collection<DoubleVectorValueObject> unpack( Collection<ProcessedExpressionDataVector> data,
            Map<ProcessedExpressionDataVector, Collection<Long>> vector2GeneMap ) {
        Collection<DoubleVectorValueObject> result = new ArrayList<>( data.size() );
        Map<ExpressionExperiment, ExpressionExperimentValueObject> eeVos = createValueObjectCache( data,
                ProcessedExpressionDataVector::getExpressionExperiment, ExpressionExperimentValueObject::new );
        Map<QuantitationType, QuantitationTypeValueObject> qtVos = createValueObjectCache( data,
                ProcessedExpressionDataVector::getQuantitationType, QuantitationTypeValueObject::new );
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = createValueObjectCache( data,
                ProcessedExpressionDataVector::getBioAssayDimension, BioAssayDimensionValueObject::new );
        Map<ArrayDesign, ArrayDesignValueObject> adVos = createValueObjectCache( data,
                vec -> vec.getDesignElement().getArrayDesign(), ArrayDesignValueObject::new );
        for ( ProcessedExpressionDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v, eeVos.get( v.getExpressionExperiment() ),
                    qtVos.get( v.getQuantitationType() ), badVos.get( v.getBioAssayDimension() ),
                    adVos.get( v.getDesignElement().getArrayDesign() ), vector2GeneMap.get( v ) ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack( Collection<ProcessedExpressionDataVector> data,
            Map<ProcessedExpressionDataVector, Collection<Long>> vector2GeneMap, BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new HashSet<>();
        Map<ExpressionExperiment, ExpressionExperimentValueObject> eeVos = createValueObjectCache( data,
                ProcessedExpressionDataVector::getExpressionExperiment, ExpressionExperimentValueObject::new );
        Map<QuantitationType, QuantitationTypeValueObject> qtVos = createValueObjectCache( data,
                ProcessedExpressionDataVector::getQuantitationType, QuantitationTypeValueObject::new );
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = createValueObjectCache( data,
                ProcessedExpressionDataVector::getBioAssayDimension, BioAssayDimensionValueObject::new );
        Map<ArrayDesign, ArrayDesignValueObject> adVos = createValueObjectCache( data,
                vec -> vec.getDesignElement().getArrayDesign(), ArrayDesignValueObject::new );
        BioAssayDimensionValueObject dimToMatch = new BioAssayDimensionValueObject( longestBad );
        for ( ProcessedExpressionDataVector v : data ) {
            result.add( new DoubleVectorValueObject( v, eeVos.get( v.getExpressionExperiment() ),
                    qtVos.get( v.getQuantitationType() ), badVos.get( v.getBioAssayDimension() ),
                    adVos.get( v.getDesignElement().getArrayDesign() ), vector2GeneMap.get( v ), dimToMatch ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack( Map<ProcessedExpressionDataVector, Collection<Long>> data ) {
        Collection<DoubleVectorValueObject> result = new ArrayList<>( data.size() );
        Map<ExpressionExperiment, ExpressionExperimentValueObject> eeVos = createValueObjectCache( data.keySet(),
                ProcessedExpressionDataVector::getExpressionExperiment, ExpressionExperimentValueObject::new );
        Map<QuantitationType, QuantitationTypeValueObject> qtVos = createValueObjectCache( data.keySet(),
                ProcessedExpressionDataVector::getQuantitationType, QuantitationTypeValueObject::new );
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = createValueObjectCache( data.keySet(),
                ProcessedExpressionDataVector::getBioAssayDimension, BioAssayDimensionValueObject::new );
        Map<ArrayDesign, ArrayDesignValueObject> adVos = createValueObjectCache( data.keySet(),
                vec -> vec.getDesignElement().getArrayDesign(), ArrayDesignValueObject::new );
        for ( ProcessedExpressionDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, eeVos.get( v.getExpressionExperiment() ),
                    qtVos.get( v.getQuantitationType() ), badVos.get( v.getBioAssayDimension() ),
                    adVos.get( v.getDesignElement().getArrayDesign() ), data.get( v ) ) );
        }
        return result;
    }

    private Collection<DoubleVectorValueObject> unpack( Map<ProcessedExpressionDataVector, Collection<Long>> data,
            BioAssayDimension longestBad ) {
        Collection<DoubleVectorValueObject> result = new ArrayList<>( data.size() );
        Map<ExpressionExperiment, ExpressionExperimentValueObject> eeVos = createValueObjectCache( data.keySet(),
                ProcessedExpressionDataVector::getExpressionExperiment, ExpressionExperimentValueObject::new );
        Map<QuantitationType, QuantitationTypeValueObject> qtVos = createValueObjectCache( data.keySet(),
                ProcessedExpressionDataVector::getQuantitationType, QuantitationTypeValueObject::new );
        Map<BioAssayDimension, BioAssayDimensionValueObject> badVos = createValueObjectCache( data.keySet(),
                ProcessedExpressionDataVector::getBioAssayDimension, BioAssayDimensionValueObject::new );
        Map<ArrayDesign, ArrayDesignValueObject> adVos = createValueObjectCache( data.keySet(),
                vec -> vec.getDesignElement().getArrayDesign(), ArrayDesignValueObject::new );
        BioAssayDimensionValueObject dimToMatch = new BioAssayDimensionValueObject( longestBad );
        for ( ProcessedExpressionDataVector v : data.keySet() ) {
            result.add( new DoubleVectorValueObject( v, eeVos.get( v.getExpressionExperiment() ),
                    qtVos.get( v.getQuantitationType() ), badVos.get( v.getBioAssayDimension() ),
                    adVos.get( v.getDesignElement().getArrayDesign() ), data.get( v ), dimToMatch ) );
        }
        return result;
    }

    /**
     * @param ees  Experiments and/or subsets required
     * @param vecs vectors to select from and if necessary slice, obviously from the given ees.
     * @return vectors that are for the requested subset. If an ee is not a subset, vectors will be unchanged.
     * Otherwise
     * the data in a vector will be for the subset of samples in the ee subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubsets( Collection<? extends BioAssaySet> ees,
            @Nullable Collection<DoubleVectorValueObject> vecs ) {
        Collection<DoubleVectorValueObject> results = new HashSet<>();
        if ( vecs == null || vecs.isEmpty() )
            return results;

        Map<Long, List<DoubleVectorValueObject>> vectorByExperimentId = vecs.stream()
                .collect( Collectors.groupingBy( v -> v.getExpressionExperiment().getId(), Collectors.toList() ) );

        for ( BioAssaySet bas : ees ) {
            ExpressionExperiment ee = getExperiment( bas );
            if ( vectorByExperimentId.containsKey( ee.getId() ) ) {
                results.addAll( sliceSubSet( bas, vectorByExperimentId.get( ee.getId() ) ) );
            }
        }

        return results;
    }

    private Collection<DoubleVectorValueObject> sliceSubSet( BioAssaySet bas, @Nullable Collection<DoubleVectorValueObject> obs ) {
        if ( bas instanceof ExpressionExperimentSubSet ) {
            return sliceSubSet( ( ExpressionExperimentSubSet ) bas, obs );
        } else {
            return obs;
        }
    }

    /**
     * @param ee  ee
     * @param obs obs
     * @return Given an ExpressionExperimentSubset and vectors from the source experiment, give vectors that include
     * just the
     * data for the subset.
     */
    private Collection<DoubleVectorValueObject> sliceSubSet( ExpressionExperimentSubSet ee,
            @Nullable Collection<DoubleVectorValueObject> obs ) {
        if ( obs == null || obs.isEmpty() )
            return obs;

        Collection<DoubleVectorValueObject> sliced = new HashSet<>();

        Hibernate.initialize( ee.getBioAssays() );
        List<BioAssayValueObject> sliceBioAssays = new ArrayList<>();

        DoubleVectorValueObject exemplar = obs.iterator().next();

        BioAssayDimensionValueObject slicedBad = new BioAssayDimensionValueObject();
        slicedBad.setName( "Subset of :" + exemplar.getBioAssayDimension().getName() );
        slicedBad.setDescription( "Subset slice" );
        slicedBad.setSourceBioAssayDimension( exemplar.getBioAssayDimension() );
        slicedBad.setIsSubset( true );
        Collection<Long> subsetBioAssayIds = IdentifiableUtils.getIds( ee.getBioAssays() );

        for ( BioAssayValueObject ba : exemplar.getBioAssays() ) {
            if ( !subsetBioAssayIds.contains( ba.getId() ) ) {
                continue;
            }

            sliceBioAssays.add( ba );
        }

        slicedBad.addBioAssays( sliceBioAssays );
        ExpressionExperimentSubsetValueObject eeVo = new ExpressionExperimentSubsetValueObject( ee );

        List<BioAssayValueObject> assays = obs.iterator().next().getBioAssays();
        Map<BioAssayValueObject, Integer> ba2i = ListUtils.indexOfElements( assays );
        int[] bioAssayIndex = new int[sliceBioAssays.size()];
        for ( int i = 0; i < sliceBioAssays.size(); i++ ) {
            bioAssayIndex[i] = ba2i.get( sliceBioAssays.get( i ) );
        }

        for ( DoubleVectorValueObject vec : obs ) {
            sliced.add( vec.slice( eeVo, slicedBad, bioAssayIndex ) );
        }

        return sliced;
    }


    private Map<BioAssaySet, Collection<BioAssayDimension>> getBioAssayDimensions( Collection<? extends BioAssaySet> ees ) {
        Map<BioAssaySet, Collection<BioAssayDimension>> result = new HashMap<>();
        for ( BioAssaySet ee : ees ) {
            result.put( ee, getBioAssayDimensions( ee ) );
        }
        return result;
    }

    private Collection<BioAssayDimension> getBioAssayDimensions( BioAssaySet bas ) {
        return expressionExperimentService.getProcessedBioAssayDimensionsWithAssays( getExperiment( bas ) );
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
     * @return Pre-fetch and construct the BioAssayDimensionValueObjects. Used on the basis that the data probably
     * just
     * have one
     * (or a few) BioAssayDimensionValueObjects needed, not a different one for each vector. See bug 3629
     * for
     * details.
     */
    private <S, T> Map<S, T> createValueObjectCache( Collection<ProcessedExpressionDataVector> vectors, Function<ProcessedExpressionDataVector, S> keyExtractor, Function<S, T> valueExtractor ) {
        Map<S, T> result = new HashMap<>();
        for ( ProcessedExpressionDataVector v : vectors ) {
            S key = keyExtractor.apply( v );
            if ( !result.containsKey( key ) ) {
                result.put( key, valueExtractor.apply( key ) );
            }
        }
        return result;
    }
}
