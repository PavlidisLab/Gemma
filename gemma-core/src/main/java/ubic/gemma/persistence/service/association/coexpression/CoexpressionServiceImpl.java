/*
 * The Gemma project.
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.persistence.service.association.coexpression;

import cern.colt.list.DoubleArrayList;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.genome.GeneDao;
import ubic.gemma.persistence.util.EntityUtils;

import java.util.*;

/**
 * @see CoexpressionService
 */
@Service

public class CoexpressionServiceImpl implements CoexpressionService {

    private static final Logger log = LoggerFactory.getLogger( CoexpressionServiceImpl.class );

    @Autowired
    private CoexpressionDao coexpressionDao;

    // @Autowired
    // private CoexpressionQueryQueue coexpressionQueryQueue;

    // @Autowired
    // private CoexpressionCache coexpressionCache;

    @Autowired
    private ExpressionExperimentDao experimentDao;

    @Autowired
    private CoexpressionNodeDegreeDao geneCoexpressionNodeDegreeDao;

    @Autowired
    private GeneDao geneDao;

    @Override
    @Transactional(readOnly = true)
    public Integer countLinks( BioAssaySet ee, Gene gene ) {
        return this.coexpressionDao.countLinks( gene, ee );
    }

    @Override
    @Transactional
    public void createOrUpdate( BioAssaySet bioAssaySet, List<NonPersistentNonOrderedCoexpLink> links, LinkCreator c,
            Set<Gene> genesTested ) {
        assert bioAssaySet != null;
        assert genesTested != null;
        this.coexpressionDao.createOrUpdate( bioAssaySet, links, c, genesTested );

        // remove these from the queue, in case they are there.
        // Collection<Long> genes = new HashSet<>();
        // for ( NonPersistentNonOrderedCoexpLink link : links ) {
        //     genes.add( link.getFirstGene() );
        //     genes.add( link.getSecondGene() );
        // }
        // this.coexpressionQueryQueue.removeFromQueue( genes );
    }

    @Override
    public void deleteLinks( BioAssaySet experiment ) {
        this.coexpressionDao.deleteLinks( this.experimentDao.getTaxon( experiment ), experiment );
    }

    @Override
    @Transactional(readOnly = true)
    public List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas, int maxResults,
            boolean quick ) {
        List<CoexpressionValueObject> results = this.coexpressionDao
                .findCoexpressionRelationships( gene, bas, maxResults, quick );

        // if ( quick || maxResults > 0 ) {
        //     this.coexpressionQueryQueue.addToFullQueryQueue( gene );
        // }

        return results;
    }

    @Override
    public List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas, int stringency,
            int maxResults, boolean quick ) {
        assert gene != null;
        Map<Long, List<CoexpressionValueObject>> r = this
                .findCoexpressionRelationships( gene.getTaxon(), Collections.singleton( gene.getId() ), bas, stringency,
                        maxResults, quick );
        return r.containsKey( gene.getId() ) ? r.get( gene.getId() ) : new ArrayList<CoexpressionValueObject>();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int maxResults, boolean quick ) {

        if ( ( genes == null || genes.isEmpty() ) && t == null ) {
            throw new IllegalArgumentException( "Must specify genes or the taxon" );
        }

        Map<Long, List<CoexpressionValueObject>> results = this.coexpressionDao
                .findCoexpressionRelationships( t, genes, bas, maxResults, quick );

        // since we require these links occur in all the given data sets, we assume we should cache (if not there
        // already) - don't bother checking 'quick' and 'maxResults'.
        // this.possiblyAddToCacheQueue( results );

        return results;
    }

    @Override
    public Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, int maxResults, boolean quick ) {
        // if ( stringency > CoexpressionCache.CACHE_QUERY_STRINGENCY || quick || maxResults > 0 ) {
        //     this.possiblyAddToCacheQueue( results );
        // }
        return this.coexpressionDao
                .findCoexpressionRelationships( t, genes, bas, stringency, maxResults, quick );
    }

    @Override
    public Map<Long, List<CoexpressionValueObject>> findInterCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, boolean quick ) {
        // these are always candidates for queuing since the constraint on genes is done at the query level.
        // this.possiblyAddToCacheQueue( results );
        return this.coexpressionDao
                .findInterCoexpressionRelationships( t, genes, bas, stringency, quick );
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionValueObject> getCoexpression( BioAssaySet experiment, boolean quick ) {
        return this.coexpressionDao.getCoexpression( experimentDao.getTaxon( experiment ), experiment, quick );
    }

    @Override
    public void updateNodeDegrees( Taxon t ) {
        CoexpressionServiceImpl.log.info( "Updating node degree for all genes from " + t );

        // map of support to gene to number of links, in order of support.
        TreeMap<Integer, Map<Long, Integer>> forRanksPos = new TreeMap<>();
        TreeMap<Integer, Map<Long, Integer>> forRanksNeg = new TreeMap<>();

        int count = 0;
        for ( Gene g : this.geneDao.loadKnownGenes( t ) ) {
            this.updateNodeDegree( g, forRanksPos, forRanksNeg );

            if ( ++count % 1000 == 0 ) {
                CoexpressionServiceImpl.log
                        .info( "Updated node degree for " + count + " genes; last was " + g + " ..." );
            }

        }
        CoexpressionServiceImpl.log.info( "Updated node degree for " + count + " genes" );

        /*
         * Update the ranks. Each entry in the resulting map (key = gene id) is a list of the ranks at each support
         * threshold. So it means the rank "at or above" that level of support.
         */
        Map<Long, List<Double>> relRanksPerGenePos = this.computeRelativeRanks( forRanksPos );
        Map<Long, List<Double>> relRanksPerGeneNeg = this.computeRelativeRanks( forRanksNeg );

        this.updateRelativeNodeDegrees( relRanksPerGenePos, relRanksPerGeneNeg );

    }

    @Override
    @Transactional(readOnly = true)
    public GeneCoexpressionNodeDegreeValueObject getNodeDegree( Gene g ) {
        GeneCoexpressionNodeDegree nd = geneCoexpressionNodeDegreeDao.load( g.getId() );
        if ( nd == null )
            return null;
        return new GeneCoexpressionNodeDegreeValueObject( nd );
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, GeneCoexpressionNodeDegreeValueObject> getNodeDegrees( Collection<Long> g ) {
        Map<Long, GeneCoexpressionNodeDegreeValueObject> results = new HashMap<>();
        for ( GeneCoexpressionNodeDegree ndo : geneCoexpressionNodeDegreeDao.load( g ) ) {
            results.put( ndo.getGeneId(), new GeneCoexpressionNodeDegreeValueObject( ndo ) );
        }
        return results;

    }

    @Override
    @Transactional
    public Map<SupportDetails, Gene2GeneCoexpression> initializeLinksFromOldData( Gene g, Map<Long, Gene> idMap,
            Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar, Set<Long> skipGenes ) {
        return this.coexpressionDao.initializeFromOldData( g, idMap, linksSoFar, skipGenes );
    }

    @Override
    public Map<Gene, Integer> countOldLinks( Collection<Gene> genes ) {
        return this.coexpressionDao.countOldLinks( genes );
    }

    private void updateRelativeNodeDegrees( Map<Long, List<Double>> relRanksPerGenePos,
            Map<Long, List<Double>> relRanksPerGeneNeg ) {
        this.coexpressionDao.updateRelativeNodeDegrees( relRanksPerGenePos, relRanksPerGeneNeg );

    }

    private Map<Long, List<Double>> computeRelativeRanks( TreeMap<Integer, Map<Long, Integer>> forRanks ) {
        Map<Long, List<Double>> relRanks = new HashMap<>();
        for ( Integer support : forRanks.keySet() ) {

            // low ranks = low node degree = good.
            Map<Long, Double> rt = Rank.rankTransform( forRanks.get( support ) );

            double max = DescriptiveWithMissing.max( new DoubleArrayList(
                    ArrayUtils.toPrimitive( new ArrayList<>( rt.values() ).toArray( new Double[] {} ) ) ) );

            for ( Long g : rt.keySet() ) {
                double relRank = rt.get( g ) / max;

                if ( !relRanks.containsKey( g ) ) {
                    relRanks.put( g, new ArrayList<Double>() );
                }

                // the ranks are in order.
                relRanks.get( g ).add( relRank );
            }
        }
        return relRanks;
    }

    /**
     * Check for results which were not in the cache, and which were not cached; make sure we fully query them.
     */
    // private void possiblyAddToCacheQueue( Map<Long, List<CoexpressionValueObject>> links ) {

    //     if ( !coexpressionCache.isEnabled() )
    //         return;

    //     Set<Long> toQueue = new HashSet<>();
    //     for ( Long id : links.keySet() ) {
    //         for ( CoexpressionValueObject link : links.get( id ) ) {
    //             if ( link.isFromCache() ) {
    //                 continue;
    //             }
    //             toQueue.add( link.getQueryGeneId() );
    //         }
    //     }
    //     if ( !toQueue.isEmpty() ) {
    //         CoexpressionServiceImpl.log.info( "Queuing " + toQueue.size() + " genes for coexpression cache warm" );
    //         coexpressionQueryQueue.addToFullQueryQueue( toQueue );
    //     }

    // }
    private GeneCoexpressionNodeDegreeValueObject updateNodeDegree( Gene gene ) {
        GeneCoexpressionNodeDegree nd = this.geneCoexpressionNodeDegreeDao.findOrCreate( gene );
        return this.coexpressionDao.updateNodeDegree( gene, nd );
    }

    private void updateNodeDegree( Gene g, TreeMap<Integer, Map<Long, Integer>> forRanksPos,
            TreeMap<Integer, Map<Long, Integer>> forRanksNeg ) {
        GeneCoexpressionNodeDegreeValueObject updatedVO = this.updateNodeDegree( g );

        if ( CoexpressionServiceImpl.log.isDebugEnabled() )
            CoexpressionServiceImpl.log.debug( updatedVO.toString() );

        /*
         * Positive
         */
        Long id = updatedVO.getGeneId();
        for ( Integer i = 0; i < updatedVO.asIntArrayPos().length; i++ ) {
            if ( !forRanksPos.containsKey( i ) ) {
                forRanksPos.put( i, new HashMap<Long, Integer>() );
            }
            // note this is the cumulative value.
            assert !forRanksPos.get( i ).containsKey( id );
            forRanksPos.get( i ).put( id, updatedVO.getLinksWithMinimumSupport( i, true ) );
        }

        /*
         * Negative
         */
        for ( Integer i = 0; i < updatedVO.asIntArrayNeg().length; i++ ) {
            if ( !forRanksNeg.containsKey( i ) ) {
                forRanksNeg.put( i, new HashMap<Long, Integer>() );
            }
            // note this is the cumulative value.
            assert !forRanksNeg.get( i ).containsKey( id );
            forRanksNeg.get( i ).put( id, updatedVO.getLinksWithMinimumSupport( i, false ) );
        }
    }

}