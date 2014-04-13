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
package ubic.gemma.model.association.coexpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cern.colt.list.DoubleArrayList;
import ubic.basecode.math.DescriptiveWithMissing;
import ubic.basecode.math.Rank;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.GeneDao;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.EntityUtils;

/**
 * @see ubic.gemma.model.association.coexpression.CoexpressionService
 */
@Service
public class CoexpressionServiceImpl implements CoexpressionService {

    private static Logger log = LoggerFactory.getLogger( CoexpressionServiceImpl.class );

    @Autowired
    private CoexpressionQueryQueue coexpressionQueryQueue;

    @Autowired
    private ExpressionExperimentDao experimentDao;

    @Autowired
    private CoexpressionDao coexpressionDao;

    @Autowired
    private CoexpressionNodeDegreeDao geneCoexpressionNodeDegreeDao;

    @Autowired
    private GeneDao geneDao;

    @Override
    @Transactional(readOnly = true)
    public Integer countLinks( BioAssaySet ee, Gene gene ) {
        return this.coexpressionDao.countLinks( gene, ee );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.association.coexpression.CoexpressionService#countOldLinks(java.util.Collection)
     */
    @Override
    public Map<Gene, Integer> countOldLinks( Collection<Gene> genes ) {
        return this.coexpressionDao.countOldLinks( genes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#createOrUpdate(ubic.gemma.model.expression
     * .experiment.BioAssaySet, java.util.Collection, boolean)
     */
    @Override
    @Transactional
    public void createOrUpdate( BioAssaySet bioAssaySet, List<NonPersistentNonOrderedCoexpLink> links, LinkCreator c,
            Set<Gene> genesTested ) {
        assert bioAssaySet != null;
        assert genesTested != null;
        this.coexpressionDao.createOrUpdate( bioAssaySet, links, c, genesTested );

        // remove these from the queue, in case they are there.
        Collection<Long> genes = new HashSet<>();
        for ( NonPersistentNonOrderedCoexpLink link : links ) {
            genes.add( link.getFirstGene() );
            genes.add( link.getSecondGene() );
        }
        this.coexpressionQueryQueue.removeFromQueue( genes,
                CoexpressionQueryUtils.getGeneLinkClassName( genesTested.iterator().next() ) );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#deleteLinks(ubic.gemma.model.expression
     * .experiment.BioAssaySet)
     */
    @Override
    public void deleteLinks( BioAssaySet experiment ) {
        this.coexpressionDao.deleteLinks( this.experimentDao.getTaxon( experiment ), experiment );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.CoexpressionService#findCoexpressionRelationships(ubic.gemma.model.
     * genome.Gene, java.util.Collection, int, boolean)
     */
    @Override
    @Transactional(readOnly = true)
    public List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas,
            int maxResults, boolean quick ) {
        List<CoexpressionValueObject> results = this.coexpressionDao.findCoexpressionRelationships( gene, bas,
                maxResults, quick );

        if ( quick || maxResults > 0 ) {
            this.coexpressionQueryQueue.addToFullQueryQueue( gene );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(ubic.gemma
     * .model.genome.Gene, java.util.Collection, int, int)
     */
    @Override
    public List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas,
            int stringency, int maxResults, boolean quick ) {
        assert gene != null;
        Map<Long, List<CoexpressionValueObject>> r = this.findCoexpressionRelationships( gene.getTaxon(),
                EntityUtils.getIds( gene ), bas, stringency, maxResults, quick );
        return r.containsKey( gene.getId() ) ? r.get( gene.getId() ) : new ArrayList<CoexpressionValueObject>();
    }

    /*
     * Find coexpression links for the genes that are common to all the given datasets
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(java.util
     * .Collection, java.util.Collection, int)
     */
    @Override
    @Transactional(readOnly = true)
    public Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int maxResults, boolean quick ) {

        // FIXME if the number of data sets is small, switch to doing a experiment-oriented query

        Map<Long, List<CoexpressionValueObject>> results = this.coexpressionDao.findCoexpressionRelationships( t,
                genes, bas, maxResults, quick );

        // since we require these links occur in all the given data sets, we assume we should cache (if not there
        // already) - don't bother checking 'quick' and 'maxResults'.
        possiblyAddToCacheQueue( t, results );

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findCoexpressionRelationships(ubic.gemma
     * .model.genome.Taxon, java.util.Collection, java.util.Collection, int, int)
     */
    @Override
    public Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, int maxResults, boolean quick ) {
        Map<Long, List<CoexpressionValueObject>> results = this.coexpressionDao.findCoexpressionRelationships( t,
                genes, bas, stringency, maxResults, quick );

        if ( stringency > CoexpressionCache.CACHE_QUERY_STRINGENCY || quick || maxResults > 0 ) {
            possiblyAddToCacheQueue( t, results );
        }

        return results;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.Gene2GeneCoexpressionService#findInterCoexpressionRelationship(java
     * .util.Collection, java.util.Collection, int)
     */
    @Override
    public Map<Long, List<CoexpressionValueObject>> findInterCoexpressionRelationships( Taxon t,
            Collection<Long> genes, Collection<Long> bas, int stringency, boolean quick ) {
        Map<Long, List<CoexpressionValueObject>> results = this.coexpressionDao.findInterCoexpressionRelationships( t,
                genes, bas, stringency, quick );

        // these are always candidates for queuing since the constraint on genes is done at the query level.
        possiblyAddToCacheQueue( t, results );
        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<CoexpressionValueObject> getCoexpression( BioAssaySet experiment, boolean quick ) {
        return this.coexpressionDao.getCoexpression( experimentDao.getTaxon( experiment ), experiment, quick );
    }

    @Override
    @Transactional(readOnly = true)
    public GeneCoexpressionNodeDegreeValueObject getNodeDegree( Gene g ) {
        GeneCoexpressionNodeDegree nd = geneCoexpressionNodeDegreeDao.load( g.getId() );
        if ( nd == null ) return null;
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

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.CoexpressionService#initializeLinksFromOldData(ubic.gemma.model.genome
     * .Taxon)
     */
    @Override
    @Transactional
    public Map<SupportDetails, Gene2GeneCoexpression> initializeLinksFromOldData( Gene g, Map<Long, Gene> idMap,
            Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar, Set<Long> skipGenes ) {
        return this.coexpressionDao.initializeFromOldData( g, idMap, linksSoFar, skipGenes );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.association.coexpression.CoexpressionService#updateNodeDegrees(ubic.gemma.model.genome.Taxon)
     */
    @Override
    public void updateNodeDegrees( Taxon t ) {
        log.info( "Updating node degree for all genes from " + t );

        // map of support to gene to number of links, in order of support.
        TreeMap<Integer, Map<Long, Integer>> forRanks = new TreeMap<>();
        int count = 0;
        for ( Gene g : this.geneDao.loadKnownGenes( t ) ) {
            GeneCoexpressionNodeDegreeValueObject updatedVO = this.updateNodeDegree( g );

            Long id = updatedVO.getGeneId();
            int[] nodeDegreesAtSupports = updatedVO.asIntArray();
            for ( int i = 0; i < nodeDegreesAtSupports.length; i++ ) {
                if ( !forRanks.containsKey( i ) ) {
                    forRanks.put( i, new HashMap<Long, Integer>() );
                }
                // note this is the cumulative value.
                forRanks.get( i ).put( id, updatedVO.getLinksWithMinimumSupport( i ) );
            }

            if ( ++count % 1000 == 0 ) {
                log.info( "Updated node degree for " + count + " genes; last was " + g + " ..." );
            }

        }
        log.info( "Updated node degree for " + count + " genes" );

        /*
         * Update the ranks. Each entry in the resulting map (key = gene id) is a list of the ranks at each support
         * threshold. So it means the rank "at or above" that level of support.
         */
        Map<Long, List<Double>> relRanksPerGene = new HashMap<>();
        for ( Integer support : forRanks.keySet() ) {

            // low ranks = low node degree = good.
            Map<Long, Double> rt = Rank.rankTransform( forRanks.get( support ) );

            double max = DescriptiveWithMissing.max( new DoubleArrayList( ArrayUtils.toPrimitive( new ArrayList<>( rt
                    .values() ).toArray( new Double[] {} ) ) ) );

            for ( Long g : rt.keySet() ) {
                double relRank = rt.get( g ) / max;
                // rt.put( g, relRank );

                if ( !relRanksPerGene.containsKey( g ) ) {
                    relRanksPerGene.put( g, new ArrayList<Double>() );
                }

                relRanksPerGene.get( g ).add( relRank );

            }
        }

        this.updateRelativeNodeDegrees( relRanksPerGene );
    }

    /**
     * Check for results which were not in the cache, and which were not cached; make sure we fully query them.
     * 
     * @param t
     * @param links
     */
    private void possiblyAddToCacheQueue( Taxon t, Map<Long, List<CoexpressionValueObject>> links ) {

        Set<Long> toQueue = new HashSet<>();
        for ( Long id : links.keySet() ) {
            for ( CoexpressionValueObject link : links.get( id ) ) {
                if ( link.isFromCache() ) {
                    continue;
                }
                toQueue.add( link.getQueryGeneId() );
                // toQueue.add( link.getCoexGeneId() );
            }
        }
        if ( !toQueue.isEmpty() ) {
            coexpressionQueryQueue.addToFullQueryQueue( toQueue, CoexpressionQueryUtils.getGeneLinkClassName( t ) );
        }

    }

    private GeneCoexpressionNodeDegreeValueObject updateNodeDegree( Gene gene ) {
        GeneCoexpressionNodeDegree nd = this.geneCoexpressionNodeDegreeDao.findOrCreate( gene );
        return this.coexpressionDao.updateNodeDegree( gene, nd );
    }

    public void updateRelativeNodeDegrees( Map<Long, List<Double>> relRanksPerGene ) {
        this.coexpressionDao.updateRelativeNodeDegrees( relRanksPerGene );

    }

}