/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2008 University of British Columbia
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

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.dataStructure.CountingMap;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressedGenes;
import ubic.gemma.model.analysis.expression.coexpression.GeneCoexpressionTestedIn;
import ubic.gemma.model.analysis.expression.coexpression.IdArrayValueObject;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.association.coexpression.ExperimentCoexpressionLink;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegree;
import ubic.gemma.model.association.coexpression.GeneCoexpressionNodeDegreeValueObject;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.util.ByteArrayUtils.doubleArrayToBytes;

/**
 * Manages and queries coexpression 'links' between genes.
 *
 * @author klc
 * @author paul
 * @see ubic.gemma.model.association.coexpression.Gene2GeneCoexpression
 */
@SuppressWarnings("unchecked")
@Repository
public class CoexpressionDaoImpl implements CoexpressionDao {

    /*
     * Important implementation note: For efficiency reason, it is important that gene-level links be stored clustered
     * by gene in the database: so all all links with gene=x are clustered. (the actual order of x1 vs x2 doesn't
     * matter, just so long they are clustered). This makes retrievals much faster for the most common types of queries.
     */

    /**
     * When links drop to support zero, should we remove them? Or leave them under the assumption they might get put
     * back again. Keeping them will help reduce fragmentation. Changing this setting without wiping the database could
     * be problematic...
     */
    private static final boolean DELETE_ORPHAN_LINKS = false;
    /**
     * If there are more datasets than this specified, use a gene-specific query unless it's too few genes specified. If
     * there are just a few genes it's going to be faster to just get the data for the genes and filter by dataset. But
     * for a large number of genes in a small number of datasets, going to the datasets first will be faster. Note that
     * this really depends on how many data sets are available.
     */
    private static final int MAX_DATASETS_FOR_DATASET_FIRST_QUERY = 50;
    /**
     * If no genes are specified, find results common to the given data sets, but only if there aren't too many data
     * sets.
     */
    private static final int MAX_DATASETS_FOR_DATASET_ONLY_QUERY = 20;
    /**
     * If there are fewer genes than this specified, use an experiment-specific query if there aren't too many datasets
     * specified. If it's just a few genes, it's always going to be faster to just get the data for the genes and filter
     * by dataset.
     */
    private static final int MIN_GENES_FOR_DATASET_FIRST_QUERY = 10;
    private static final int BATCH_SIZE = 2048;
    private static final int BATCH_SIZE_SMALL = 8;
    private static final Log log = LogFactory.getLog( CoexpressionDaoImpl.class );
    /**
     * If the stringency is less than this, we will usually want to use a dataset-first query unless the number of
     * datasets is quite large. Note that this setting should depend on how many datasets are in the system in the first
     * place, and is thus species-specific. So this is just a temporary measure.
     */
    // private static final int MIN_STRINGENCY_FOR_GENE_FIRST_QUERY = 6;

    @Autowired
    private CoexpressionCache gene2GeneCoexpressionCache;

    @Autowired
    private GeneTestedInCache geneTestedInCache;

    @Autowired
    private SessionFactory sessionFactory;

    @Override
    public boolean hasLinks( Taxon taxon, BioAssaySet ee ) {
        return ( Boolean ) sessionFactory.getCurrentSession()
                .createQuery( "select count(*) > 0 from " + CoexpressionQueryUtils.getExperimentLinkClassName( taxon ) + " e "
                        + "where e.experiment = :ee" )
                .setParameter( "ee", ee )
                .uniqueResult();
    }

    @Override
    public Integer countLinks( Gene gene, BioAssaySet ee ) {
        // Looking at the first gene is enough if we save the flipped versions; we don't get a double-count here because
        // of the constraint on the first gene.
        Session sess = sessionFactory.getCurrentSession();
        Query q = sess.createSQLQuery(
                "select count(*) from " + CoexpressionQueryUtils.getExperimentLinkTableName( gene.getTaxon() )
                        + " e where e.EXPERIMENT_FK=:ee and e.GENE1_FK=:g " );
        q.setParameter( "ee", ee.getId() ).setParameter( "g", gene.getId() );
        return ( ( BigInteger ) q.uniqueResult() ).intValue();
    }

    /*
     * Errors here will be big trouble, leading to corrupt data. It has to be all one transaction.
     *
     */
    @Override
    public void createOrUpdate( BioAssaySet bioAssaySet, List<NonPersistentNonOrderedCoexpLink> links, LinkCreator c,
            Set<Gene> genesTested ) {

        // assumption is that these are _all_ the links for this experiment
        assert !links.isEmpty();
        assert bioAssaySet != null;
        assert c != null;

        Collections.sort( links );

        Session sess = sessionFactory.getCurrentSession();
        sess.setCacheMode( CacheMode.IGNORE );

        // to determine the species
        Gene gene = ( Gene ) sess.get( Gene.class, links.iterator().next().getFirstGene() );
        String geneLinkClassName = CoexpressionQueryUtils.getGeneLinkClassName( gene );

        /*
         * Check that there are no links for this experiment.
         */
        if ( this.countLinks( gene.getTaxon(), bioAssaySet ) > 0 ) {
            throw new IllegalStateException(
                    "There are already links for given bioAssaySet; they must be deleted before proceeding" );
        }

        /*
         * Attempt to save database trips
         */
        Map<NonPersistentNonOrderedCoexpLink, Boolean> existingResults = this.preFetch( links );

        String s = "from " + geneLinkClassName + " where firstGene =:f and secondGene=:s and positiveCorrelation=:pc";
        Query q = sess.createQuery( s );

        SQLQuery updateFlippedLinkQuery = sess.createSQLQuery(
                "UPDATE " + CoexpressionQueryUtils.getGeneLinkTableName( gene.getTaxon() )
                        + " SET SUPPORT=:s WHERE FIRST_GENE_FK=:g2 AND SECOND_GENE_FK=:g1 AND POSITIVE=:po" );

        // map of linkid to links, for establishing the EE-level links.
        TreeMap<Long, NonPersistentNonOrderedCoexpLink> linkIds = new TreeMap<>(); // keep order so for this experiment
        // they are in order.

        Set<Long> seenExistingLinks = new HashSet<>(); // for sanity checks.
        Set<NonPersistentNonOrderedCoexpLink> seenNewLinks = new HashSet<>(); // for sanity checks.
        Set<SupportDetails> seenNewSupportDetails = new HashSet<>(); // for sanity checks.
        int numNew = 0;
        int numUpdated = 0;
        int progress = 0;
        int BATCH_SIZE = 1024; // make a multiple of jdbc batch size...

        Map<SupportDetails, Gene2GeneCoexpression> batchToCreate = new LinkedHashMap<>();
        List<Gene2GeneCoexpression> newFlippedLinks = new ArrayList<>();
        Set<Long> genesWithUpdatedData = new HashSet<>();

        sess.flush();
        sess.clear();

        // for each link see if there is already an entry; make a new one if necessary or update the old one.
        CoexpressionDaoImpl.log.info( "Starting link processing" );
        for ( NonPersistentNonOrderedCoexpLink proposedG2G : links ) {

            Long firstGene = proposedG2G.getFirstGene();
            Long secondGene = proposedG2G.getSecondGene();

            // There is an index for f+s, but querying one-at-a-time is going to be slow. I attempted to speed it up by
            // fetching all links for a gene when we see it, but this causes problems with data being stale. Prefetching
            // with just the ability to tell if a link is new or not takes a lot of memory and doesn't speed things up
            // much. Trying keeping an index of which links a gene has, so we know whether we need to check the database
            // or not.
            //
            // Currently it takes about 1 minute to process 10k links on a relatively small database, much of this is
            // the findLink call.
            Gene2GeneCoexpression existingLink = this.findLink( q, proposedG2G, existingResults );

            /*
             * To speed this up?
             *
             * - Fetch all links for a gene in one batch, instead of looping over them one at a time. The problem is the
             * flipped links involve other genes that we fetch later in the same transaction, and this all has to be
             * done in one transaction. I experimented with this already
             */

            if ( existingLink == null ) {
                // initialize the supportdetails
                SupportDetails sd = c
                        .createSupportDetails( firstGene, secondGene, proposedG2G.isPositiveCorrelation() );
                sd.addEntity( bioAssaySet.getId() );

                assert sd.getNumIds() > 0;
                assert sd.isIncluded( bioAssaySet.getId() );

                // Must be unique
                assert !seenNewSupportDetails.contains( sd ) : "Already saw " + sd + " while processing " + proposedG2G;

                assert proposedG2G.getLink() != null;
                batchToCreate.put( sd, proposedG2G.getLink() );

                if ( seenNewLinks.contains( proposedG2G ) ) {
                    CoexpressionDaoImpl.log
                            .warn( "The data passed had the same new link represented more than once: " + proposedG2G );
                    continue;
                }

                seenNewSupportDetails.add( sd );
                seenNewLinks.add( proposedG2G );

                if ( CoexpressionDaoImpl.log.isDebugEnabled() )
                    CoexpressionDaoImpl.log.debug( "New: " + proposedG2G );
                numNew++;
            } else {
                // This code assumes that the flipped version is in the database, but we don't retrieve it
                // yet. also note that the support of the existing link could be zero, if DELETE_ORPHAN_LINKS = false
                // (or if initializeLinksFromExistingData was used)

                // Sanity check. If this happens, there must be two versions of the same link already in the input.
                if ( seenExistingLinks.contains( existingLink.getId() ) ) {
                    throw new IllegalStateException(
                            "The data passed had the same existing link represented more than once: " + existingLink );
                }

                /* sanity check that we aren't adding dataset twice; we might be able make this an assertion instead. */
                if ( existingLink.isSupportedBy( bioAssaySet ) ) {
                    throw new IllegalStateException( "Support for this experiment already exists for " + existingLink
                            + ", must be deleted first" );
                }

                // cache old support for sanity check
                int oldSupport = existingLink.getSupportDetails().getNumIds();

                // update the support
                existingLink.getSupportDetails().addEntity( bioAssaySet.getId() );
                existingLink.updateNumDatasetsSupporting();

                // there is no cascade... on purpose.
                sess.update( existingLink.getSupportDetails() );

                assert oldSupport + 1 == existingLink.getNumDatasetsSupporting();
                assert existingLink.getSupportDetails().getNumIds() == oldSupport + 1;

                // track so we add corresponding Experiment-level links later.
                linkIds.put( existingLink.getId(), new NonPersistentNonOrderedCoexpLink( existingLink ) );
                seenExistingLinks.add( existingLink.getId() );

                /*
                 * The flipped link is asserted to be in the database. The support details is already dealt with; we
                 * just have to update the support value.
                 */

                int numFlippedUpdated = updateFlippedLinkQuery
                        .setParameter( "s", existingLink.getNumDatasetsSupporting() )
                        .setParameter( "g2", proposedG2G.getSecondGene() )
                        .setParameter( "g1", proposedG2G.getFirstGene() )
                        .setParameter( "po", proposedG2G.isPositiveCorrelation() ? 1 : 0 ).executeUpdate();
                assert numFlippedUpdated == 1 :
                        "Flipped link missing for " + proposedG2G + " [" + numFlippedUpdated + "]";

                numUpdated++;
                if ( CoexpressionDaoImpl.log.isDebugEnabled() )
                    CoexpressionDaoImpl.log.debug( "Updated: " + proposedG2G );

            }

            genesWithUpdatedData.add( firstGene );
            genesWithUpdatedData.add( secondGene );

            if ( ++progress % 5000 == 0 ) {
                CoexpressionDaoImpl.log
                        .info( "Processed " + progress + "/" + links.size() + " gene-level links..." + numUpdated
                                + " updated, " + numNew + " new" );
            }

            if ( batchToCreate.size() >= BATCH_SIZE ) {
                newFlippedLinks.addAll( this.saveBatchAndMakeFlipped( sess, linkIds, batchToCreate, c ) );
            } else if ( numUpdated > 0 && numUpdated % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }

        } // loop over links

        // tail end batch
        if ( !batchToCreate.isEmpty() ) {
            // we make the flipped links later to optimize their ordering.
            newFlippedLinks.addAll( this.saveBatchAndMakeFlipped( sess, linkIds, batchToCreate, c ) );
        }

        // flush the updated ones one last time...
        if ( numUpdated > 0 ) {
            sess.flush();
            sess.clear();
        }

        assert links.size() == linkIds.size();

        CoexpressionDaoImpl.log.info( numUpdated + " updated, " + numNew + " new links" );

        /*
         * sort and save the accumulated new flipped versions of the new links, which reuse the supportDetails. In the
         * flipped links, the first gene is the second gene and vice versa. Continue to accumulate the flipped links.
         */
        CoexpressionDaoImpl.log.info( "Saving " + newFlippedLinks.size() + " flipped versions of new links ..." );
        Collections.sort( newFlippedLinks, new Comparator<Gene2GeneCoexpression>() {
            @Override
            public int compare( Gene2GeneCoexpression o1, Gene2GeneCoexpression o2 ) {
                return o1.getFirstGene().compareTo( o2.getFirstGene() );
            }
        } );

        progress = 0;
        for ( Gene2GeneCoexpression gl : newFlippedLinks ) {
            sess.save( gl );
            if ( ++progress % 5000 == 0 ) {
                CoexpressionDaoImpl.log.info( "Processed " + progress + "/" + newFlippedLinks.size()
                        + " new flipped gene-level links..." );
            }
            if ( progress % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

        /*
         * Save experiment-level links
         */
        CoexpressionDaoImpl.log
                .info( "Saving " + linkIds.size() + " experiment-level links (plus flipped versions) ..." );
        this.saveExperimentLevelLinks( sess, c, linkIds, bioAssaySet );

        if ( genesTested != null )
            this.updatedTestedIn( bioAssaySet, genesTested );

        this.updateGeneCoexpressedWith( links );

        // kick anything we updated out of the cache.
        int numRemovedFromCache = this.gene2GeneCoexpressionCache.remove( genesWithUpdatedData );
        if ( numRemovedFromCache > 0 )
            CoexpressionDaoImpl.log.info( numRemovedFromCache + " results evicted from cache" );

        // flush happens on commit...
        CoexpressionDaoImpl.log.info( "Done,  flushing changes ..." );
    }

    /*
     * Errors here will be big trouble, leading to corrupt data. It has to be all one transaction.
     *
     */
    @Override
    public void deleteLinks( Taxon t, BioAssaySet experiment ) {
        Session sess = sessionFactory.getCurrentSession();
        sess.setCacheMode( CacheMode.IGNORE );

        CoexpressionDaoImpl.log.info( "Fetching any old coexpression ..." );
        Collection<Gene2GeneCoexpression> links = this.getCoexpression( t, experiment );

        Set<NonPersistentNonOrderedCoexpLink> toRemove = new HashSet<>();

        // even if there are no links, we shouldn't assume we can bail; the 'tested-in' information might be there.
        if ( !links.isEmpty() ) {

            CoexpressionDaoImpl.log
                    .info( "Removing coexpression information for " + experiment + "; updating " + links.size()
                            + " links (count includes flipped versions)." );

            // adjust gene-level links
            int count = 0;
            int numWithZeroSupportLeft = 0;
            int BATCH_SIZE = 1024;
            Collection<SupportDetails> supportDetailsToDelete = new HashSet<>();
            Collection<SupportDetails> supportDetailsToUpdate = new HashSet<>();

            Collection<Long> genesAffected = new HashSet<>();

            for ( Gene2GeneCoexpression g2g : links ) {

                genesAffected.add( g2g.getFirstGene() );
                genesAffected.add( g2g.getSecondGene() );

                // decrement support; details are shared by both links, just update it once!
                SupportDetails sd = g2g.getSupportDetails();

                if ( !supportDetailsToUpdate.contains( sd ) && !supportDetailsToDelete.contains( sd ) ) {

                    /*
                     * If we already saw the supportDetails it might already be zero. But if we didn't, it can't.
                     */
                    assert g2g.getNumDatasetsSupporting() > 0 :
                            "Support was " + g2g.getNumDatasetsSupporting() + " for " + g2g;

                    sd.removeEntity( experiment.getId() );
                    assert !sd.getIds().contains( experiment.getId() );
                    supportDetailsToUpdate.add( sd );
                }

                g2g.updateNumDatasetsSupporting();
                assert g2g.getNumDatasetsSupporting() >= 0;

                if ( g2g.getNumDatasetsSupporting() == 0 ) {

                    /*
                     * we might still want to keep it, on the presumption that it will get filled back in.
                     */
                    if ( CoexpressionDaoImpl.DELETE_ORPHAN_LINKS ) {
                        sess.delete( g2g );
                        // it might be in here already (flipped), but that's okay.
                        supportDetailsToDelete.add( sd );

                        // from the quickindex. But leave it there otherwise.
                        toRemove.add( new NonPersistentNonOrderedCoexpLink( g2g ) );
                    } else {
                        sess.update( g2g );
                    }

                    numWithZeroSupportLeft++;
                } else {
                    sess.update( g2g );
                }

                if ( ++count % 10000 == 0 ) {
                    CoexpressionDaoImpl.log.info( "Removed support for " + count + " links for " + experiment + "..." );
                }

                if ( count % BATCH_SIZE == 0 ) {
                    sess.flush();
                    sess.clear();
                }
            }

            sess.flush();
            sess.clear();

            this.updateModifiedSupportDetails( experiment, supportDetailsToDelete, supportDetailsToUpdate );

            if ( CoexpressionDaoImpl.DELETE_ORPHAN_LINKS ) {
                CoexpressionDaoImpl.log
                        .info( "Adjusted " + links.size() + " gene-level links supported by the experiment; "
                                + numWithZeroSupportLeft
                                + " links removed from the system as support dropped to zero." );
            } else {
                CoexpressionDaoImpl.log
                        .info( "Adjusted " + links.size() + " gene-level links supported by the experiment; "
                                + numWithZeroSupportLeft
                                + " gene-level links now have support dropped to zero but they were left in place" );
            }

            // remove the ExperimentCoexpressionLinks
            int numDeleted = sess.createQuery(
                            "delete from " + CoexpressionQueryUtils.getExperimentLinkClassName( t ) + " where experiment=:ee" )
                    .setParameter( "ee", experiment ).executeUpdate();
            CoexpressionDaoImpl.log.info( "Deleted " + numDeleted + " experiment-level links" );

            // invalidate the cache.
            int numRemovedFromCache = gene2GeneCoexpressionCache.remove( genesAffected );
            if ( numRemovedFromCache > 0 )
                CoexpressionDaoImpl.log.info( numRemovedFromCache + " results evicted from cache" );

        }

        // we do NOT redo the node degree information, which will be refreshed "periodically"

        // we always have to do this, even if there are no links.
        this.removeTestedIn( t, experiment );

        // update our quick index
        if ( !toRemove.isEmpty() )
            this.removeCoexpressedWith( toRemove );
    }

    @Override
    public List<CoexpressionValueObject> findCoexpressionRelationships( Gene gene, Collection<Long> bas, int maxResults,
            boolean quick ) {
        assert !bas.isEmpty();
        assert gene != null;
        assert maxResults >= 0;

        Collection<Long> g = new HashSet<>();
        g.add( gene.getId() );
        assert gene.getTaxon() != null;
        Map<Long, List<CoexpressionValueObject>> r = this
                .getCoexpressionFromCacheOrDb( gene.getTaxon(), g, bas, bas.size(), maxResults, quick );

        if ( r == null || !r.containsKey( gene.getId() ) )
            return new ArrayList<>();
        return r.get( gene.getId() );

    }

    @Override
    public Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon taxon, Collection<Long> genes,
            Collection<Long> bas, int maxResults, boolean quick ) {
        assert !bas.isEmpty();
        assert !genes.isEmpty();
        assert maxResults >= 0;

        Map<Long, List<CoexpressionValueObject>> rrr = this
                .getCoexpressionFromCacheOrDb( taxon, genes, bas, bas.size(), maxResults, quick );

        int total = 0;
        for ( Long g : rrr.keySet() ) {
            total += rrr.get( g ).size();
        }

        CoexpressionDaoImpl.log
                .info( "Found " + total + " coexpression links for " + genes.size() + " genes in " + bas.size()
                        + " datasets." );
        return rrr;

    }

    @Override
    public Map<Long, List<CoexpressionValueObject>> findCoexpressionRelationships( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, int maxResults, boolean quick ) {
        assert !bas.isEmpty();
        assert genes != null;
        assert maxResults >= 0; // maxResults is ignored if it is a "my genes only" query.
        assert stringency >= 1;

        Map<Long, List<CoexpressionValueObject>> rrr = this
                .getCoexpressionFromCacheOrDb( t, genes, bas, stringency, maxResults, quick );

        // DEBUG code; count up for logging only.
        int totalLinks = 0;
        for ( List<CoexpressionValueObject> list : rrr.values() ) {
            totalLinks += list.size();
        }
        if ( genes.size() > 1 && totalLinks > 0 )
            CoexpressionDaoImpl.log
                    .info( "Found " + totalLinks + " coexpression links in total for " + genes.size() + " genes in "
                            + bas.size() + " datasets at stringency=" + stringency + " quick=" + quick
                            + " maxresults per gene=" + maxResults );
        // end debug code

        return rrr;
    }

    @Override
    public Map<Long, List<CoexpressionValueObject>> findInterCoexpressionRelationships( Taxon taxon,
            Collection<Long> genes, Collection<Long> bas, int stringency, boolean quick ) {

        assert !bas.isEmpty();
        assert !genes.isEmpty();

        if ( bas.size() < stringency ) {
            throw new IllegalArgumentException( "Stringency is larger than the number of data sets" );
        }

        Map<Long, List<CoexpressionValueObject>> results = new HashMap<>();
        Collection<Long> genesNeeded = this.checkCacheForInterGeneLinks( genes, results, stringency );

        if ( !genesNeeded.isEmpty() ) { // something wasn't in the cache.
            Map<Long, List<CoexpressionValueObject>> dbResults;
            if ( bas.size() > CoexpressionDaoImpl.MAX_DATASETS_FOR_DATASET_FIRST_QUERY
                    || genes.size() < CoexpressionDaoImpl.MIN_GENES_FOR_DATASET_FIRST_QUERY ) {
                dbResults = this.getInterCoexpressionFromDbViaGenes( taxon, genes, stringency, quick );
            } else {
                dbResults = this.getInterCoexpressionFromDbViaExperiments( taxon, genes, bas, quick );
            }

            /*
             * We can't cache this because it was done with a constraint on the 'found' gene. But it might get added to
             * the queue for cache warm.
             */

            results.putAll( dbResults );

        }
        this.trimAndFinishResults( results, bas, stringency, 0 );

        return results;

    }

    @Override
    public GeneCoexpressionNodeDegreeValueObject updateNodeDegree( Gene g, GeneCoexpressionNodeDegree nd ) {
        Session sess = sessionFactory.getCurrentSession();

        List<CoexpressionValueObject> hits = this.getCoexpression( g );

        /*
         * We have to reset the support.
         */
        GeneCoexpressionNodeDegreeValueObject gcndvo = new GeneCoexpressionNodeDegreeValueObject( nd );
        gcndvo.clear();

        assert gcndvo.getMaxSupportNeg() == 0;

        for ( CoexpressionValueObject hit : hits ) {
            if ( hit.isPositiveCorrelation() ) {
                gcndvo.increment( hit.getNumDatasetsSupporting(), true );
            } else {
                gcndvo.increment( hit.getNumDatasetsSupporting(), false );
            }
        }

        assert gcndvo.total() == hits.size();

        GeneCoexpressionNodeDegree entity = gcndvo.toEntity();
        nd.setLinkCountsPositive( entity.getLinkCountsPositive() );
        nd.setLinkCountsNegative( entity.getLinkCountsNegative() );

        if ( CoexpressionDaoImpl.log.isDebugEnabled() )
            CoexpressionDaoImpl.log.debug( "gene=" + g.getId() + " pos=" + StringUtils
                    .join( ArrayUtils.toObject( nd.getLinkCountsPositive() ), " " ) + " neg=" + StringUtils
                    .join( ArrayUtils.toObject( nd.getLinkCountsNegative() ), " " ) );

        sess.update( nd );

        // might not be necessary, but presumption is data is stale now...
        this.gene2GeneCoexpressionCache.remove( g.getId() );
        this.geneTestedInCache.remove( g.getId() );

        return gcndvo;
    }

    @Override
    public Collection<CoexpressionValueObject> getCoexpression( Taxon taxon, BioAssaySet experiment, boolean quick ) {

        Session sess = sessionFactory.getCurrentSession();

        // could just fetch linkId.
        Query q = sess.createQuery(
                " from " + CoexpressionQueryUtils.getExperimentLinkClassName( taxon ) + " where experiment=:ee" );
        q.setParameter( "ee", experiment );
        List<ExperimentCoexpressionLink> links = q.list();

        Collection<CoexpressionValueObject> results = new HashSet<>();
        if ( links.isEmpty() ) {
            return results;
        }
        List<Long> linksToFetch = new ArrayList<>();
        for ( ExperimentCoexpressionLink link : links ) {
            linksToFetch.add( link.getLinkId() );
        }

        String q2 = "from " + CoexpressionQueryUtils.getGeneLinkClassName( taxon ) + " where id in (:ids)";
        BatchIterator<Long> it = BatchIterator.batches( linksToFetch, 1000 );
        for ( ; it.hasNext(); ) {
            List<Gene2GeneCoexpression> rawResults = sess.createQuery( q2 ).setParameterList( "ids", it.next() ).list();
            for ( Gene2GeneCoexpression g2g : rawResults ) {
                CoexpressionValueObject g2gvo = new CoexpressionValueObject( g2g );
                results.add( g2gvo );
            }
        }

        if ( !quick ) {
            this.populateTestedInDetails( results );
        }

        return results;
    }

    @Override
    public int queryAndCache( Gene gene ) {

        if ( gene2GeneCoexpressionCache.get( gene.getId() ) != null ) {
            // already in the cache.
            return -1;
        }

        CoexpressionDaoImpl.log.debug( "Fetching data for gene=" + gene.getId() + " for cache" );
        Collection<Long> gg = new HashSet<>();
        gg.add( gene.getId() );
        // Map<Long, List<CoexpressionValueObject>> rr = getCoexpressionFromDbViaGenes( gg,
        // CoexpressionQueryUtils.getGeneLinkClassName( gene ), CoexpressionCache.CACHE_QUERY_STRINGENCY, true,
        // true );
        Map<Long, List<CoexpressionValueObject>> rr = this
                .getCoexpressionFromDbViaGenes2( gg, gene.getTaxon(), CoexpressionCache.CACHE_QUERY_STRINGENCY, true );

        List<CoexpressionValueObject> results = rr.get( gene.getId() );

        if ( results == null || results.isEmpty() ) {
            // it is necessary to avoid searching again when there are no results.
            gene2GeneCoexpressionCache.cacheCoexpression( gene.getId(), new ArrayList<CoexpressionValueObject>() );
            return 0;
        }

        gene2GeneCoexpressionCache.cacheCoexpression( gene.getId(), results );
        return results.size();
    }

    /*
     * This assumes that we're going to do this for all genes, so we get links in both directions eventually. We don't
     * have to explicitly make the flipped linSks here.
     */
    @Override
    public Map<SupportDetails, Gene2GeneCoexpression> initializeFromOldData( Gene gene, Map<Long, Gene> geneIdMap,
            Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar, Set<Long> skipGenes ) {

        Session sess = sessionFactory.getCurrentSession();
        LinkCreator c = new LinkCreator( gene.getTaxon() );
        String geneLinkTableName = CoexpressionQueryUtils.getGeneLinkTableName( gene.getTaxon() );
        String oldGeneLinkTableName = geneLinkTableName.replace( "COEX", "CO_EX" );
        assert oldGeneLinkTableName.contains( "CO_EX" );

        int BATCH_SIZE = 1024;

        /*
         * Query the old table
         */
        SQLQuery oldLinkQuery = sess.createSQLQuery(
                "select FIRST_GENE_FK, SECOND_GENE_FK, EFFECT from " + oldGeneLinkTableName
                        + " where FIRST_GENE_FK=?" );
        List<Object[]> oldLinks = oldLinkQuery.setLong( 0, gene.getId() ).list();

        if ( oldLinks.isEmpty() ) {
            return null;
        }

        Map<SupportDetails, Gene2GeneCoexpression> linksToSave = new LinkedHashMap<>();

        /*
         * Make new links.
         */
        Collection<NonPersistentNonOrderedCoexpLink> links = new HashSet<>();
        int i = 0;
        for ( Object[] o : oldLinks ) {
            Long fgid = ( ( BigInteger ) o[0] ).longValue();
            Long sgid = ( ( BigInteger ) o[1] ).longValue();

            if ( skipGenes != null && ( skipGenes.contains( fgid ) || skipGenes.contains( sgid ) ) ) {
                continue;
            }

            Double eff = ( Double ) o[2];

            if ( fgid.equals( sgid ) ) {
                continue;
            }

            assert geneIdMap.containsKey( fgid );
            assert geneIdMap.containsKey( sgid );

            Gene2GeneCoexpression g2g = c.create( eff, fgid, sgid );

            /*
             * Check if we already have a link like this for the reverse - if so, reuse the supportdetails; the keys of
             * linksSoFar are id-less, so equals() is by genes and direction.
             */
            SupportDetails sdOfFlipped = linksSoFar
                    .get( new NonPersistentNonOrderedCoexpLink( geneIdMap.get( fgid ), geneIdMap.get( sgid ),
                            eff > 0 ) );

            SupportDetails sd;
            if ( sdOfFlipped != null ) {
                sd = sdOfFlipped;
            } else {
                // we haven't saved the flipped link already so make a new support details.
                sd = c.createSupportDetails( geneIdMap.get( fgid ), geneIdMap.get( sgid ), eff > 0 );
                sess.save( sd );
            }

            g2g.setNumDatasetsSupporting( 0 );
            g2g.setSupportDetails( sd );

            assert sd.getId() != null;
            linksToSave.put( sd, g2g );

            links.add( new NonPersistentNonOrderedCoexpLink( g2g ) );

            if ( i++ % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

        for ( SupportDetails sd : linksToSave.keySet() ) {
            assert sd.getId() != null;
            sess.save( linksToSave.get( sd ) );
            if ( i++ % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

        this.updateGeneCoexpressedWith( links );

        return linksToSave;
    }

    @Override
    public Map<Gene, Integer> countOldLinks( Collection<Gene> genes ) {
        Map<Gene, Integer> results = new HashMap<>();
        Gene g = genes.iterator().next();
        String oldTable = CoexpressionQueryUtils.getGeneLinkTableName( g.getTaxon() ).replace( "COEXP", "CO_EXP" );
        SQLQuery q = sessionFactory.getCurrentSession()
                .createSQLQuery( "select count(*) from " + oldTable + " WHERE FIRST_GENE_FK = :firstGeneId" );
        int i = 0;
        for ( Gene gene : genes ) {
            Number c = ( Number ) q.setParameter( "firstGeneId", gene.getId() ).uniqueResult();
            results.put( gene, c.intValue() );
            if ( ++i % 500 == 0 ) {
                CoexpressionDaoImpl.log
                        .info( "Got counts for " + i + " genes, last was " + gene + " and had " + c.intValue()
                                + " links ..." );
            }
        }
        return results;
    }

    @Override
    public void updateRelativeNodeDegrees( Map<Long, List<Double>> relRanksPerGenePositive,
            Map<Long, List<Double>> relRanksPerGeneNegative ) {
        Session session = sessionFactory.getCurrentSession();

        int i = 0;
        for ( Long g : relRanksPerGenePositive.keySet() ) {
            i = this.process( relRanksPerGenePositive.get( g ), session, i, g, true );
        }
        for ( Long g : relRanksPerGeneNegative.keySet() ) {
            i = this.process( relRanksPerGeneNegative.get( g ), session, i, g, false );
        }
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public void updateModifiedSupportDetails( BioAssaySet experiment, Collection<SupportDetails> supportDetailsToDelete,
            Collection<SupportDetails> supportDetailsToUpdate ) {
        int count;
        int BATCH_SIZE = 1024;
        Session sess = sessionFactory.getCurrentSession();

        /*
         * no cascade, so we have to make sure these get updated.
         */
        count = 0;
        for ( SupportDetails sd : supportDetailsToUpdate ) {
            sess.update( sd );
            if ( ++count % 10000 == 0 ) {
                CoexpressionDaoImpl.log
                        .info( "Updated  " + count + " support details relevant to " + experiment + "..." );
            }
            if ( count % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

        CoexpressionDaoImpl.log.info( "Updated " + count + " support details relevant to " + experiment + "..." );

        sess.flush();
        sess.clear();

        count = 0;
        for ( SupportDetails sd : supportDetailsToDelete ) {
            sess.delete( sd );
            if ( ++count % 10000 == 0 ) {
                CoexpressionDaoImpl.log
                        .info( "Removed support details for " + count + " links for " + experiment + "..." );
            }
            if ( count % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

        // finish deletes of the sd (this is not really necessary here, but trying to be consistent)
        sess.flush();
        sess.clear();
    }

    @SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
    public List<Object[]> getRawCoexpressionFromDbViaGenes( Collection<Long> geneIds, Taxon t, int stringency ) {
        String sqlQuery1 = "select ID, POSITIVE, SUPPORT, FIRST_GENE_FK, SECOND_GENE_FK, SUPPORT_DETAILS_FK from "
                + CoexpressionQueryUtils.getGeneLinkTableName( t ) + " where FIRST_GENE_FK in (:genes) and SUPPORT>=:s";

        Session sess = sessionFactory.getCurrentSession();
        SQLQuery query1 = sess.createSQLQuery( sqlQuery1 );

        query1.setParameterList( "genes", geneIds.toArray() );
        query1.setParameter( "s", Math.max( 1, stringency ) );

        // This is actually pretty fast.
        return ( List<Object[]> ) query1.list();
    }

    private int process( List<Double> relRanks, Session sess, int i,
            Long g, boolean positive ) {
        GeneCoexpressionNodeDegree nd = ( GeneCoexpressionNodeDegree ) sess.load( GeneCoexpressionNodeDegree.class, g );

        byte[] r = doubleArrayToBytes( ArrayUtils.toPrimitive( relRanks.toArray( new Double[0] ) ) );

        if ( positive ) {
            nd.setRelativeLinkRanksPositive( r );
        } else {
            nd.setRelativeLinkRanksNegative( r );
        }

        sess.update( nd );
        if ( ++i % 1024 == 0 ) {
            sess.flush();
            sess.clear();
        }
        return i;
    }

    /**
     * @param geneIds   gene ids
     * @param className class name
     * @return query
     */
    private Query buildQuery( Collection<Long> geneIds, String className ) {
        String query = "select g2g from " + className + " as g2g";

        // we usually need to get the support details, so we can security-filter the data. Exception is some admin
        // tasks.

        query = query + " where g2g.firstGene in (:geneIds) ";

        if ( !CoexpressionDaoImpl.DELETE_ORPHAN_LINKS ) {
            // means links could have support of zero, and we don't want those.
            query = query + " and g2g.numDataSetsSupporting > 0 ";
        }

        Query q = sessionFactory.getCurrentSession().createQuery( query );

        q.setParameterList( "geneIds", geneIds );
        return q;
    }

    /**
     * Importantly, this method does not filter by stringency or anything.
     *
     * @param genes   genes
     * @param results will go here, each list is sorted
     * @return genes which were not found in the cache
     */
    private Collection<Long> checkCache( Collection<Long> genes, Map<Long, List<CoexpressionValueObject>> results ) {
        assert results != null;
        assert !genes.isEmpty();
        /*
         * Check cache and initialize the result data structure.
         */
        Collection<Long> geneIdsNeeded = new HashSet<>();
        int resultsFound = 0;
        for ( Long g : genes ) {

            List<CoexpressionValueObject> cachedResults = this.gene2GeneCoexpressionCache.get( g );
            if ( cachedResults != null ) {
                resultsFound += cachedResults.size();
                results.put( g, cachedResults );
            } else {
                geneIdsNeeded.add( g );
            }
        }

        if ( genes.size() > 1 && geneIdsNeeded.size() < genes.size() ) {
            CoexpressionDaoImpl.log
                    .info( "Found results for " + ( genes.size() - geneIdsNeeded.size() ) + " genes in the cache" );
            CoexpressionDaoImpl.log.debug( "There were " + resultsFound + " results, before any stringency filtering" );
        }
        return geneIdsNeeded;
    }

    /**
     * find results for the cached results that include the second gene. No filter for maximum. Importantly, this method
     * *does* filter by stringency. Still need to trimAndFinish.
     *
     * @param genes      genes
     * @param results    results will be placed here
     * @param stringency used to filter the results from the cache to ones we want
     * @return genes which were not found in the cache.
     */
    private Collection<Long> checkCacheForInterGeneLinks( Collection<Long> genes,
            Map<Long, List<CoexpressionValueObject>> results, int stringency ) {

        // is the cache going to help?
        if ( stringency < CoexpressionCache.CACHE_QUERY_STRINGENCY )
            return genes;

        Collection<Long> genesNeeded = new HashSet<>();
        int resultsFound = 0;
        for ( Long gid : genes ) {
            List<CoexpressionValueObject> e = this.gene2GeneCoexpressionCache.get( gid );
            if ( e != null ) {
                for ( CoexpressionValueObject g2g : e ) {
                    // check stringency AND *both* genes are in the link.
                    if ( g2g.getNumDatasetsSupporting() >= stringency && genes.contains( g2g.getQueryGeneId() ) && genes
                            .contains( g2g.getCoexGeneId() ) ) {

                        if ( !results.containsKey( gid ) ) {
                            results.put( gid, new ArrayList<CoexpressionValueObject>() );
                        }

                        resultsFound++;
                        assert g2g.isFromCache();
                        results.get( gid ).add( g2g );
                    }
                }
            } else {
                genesNeeded.add( gid );
            }
        }

        if ( genesNeeded.size() < genes.size() ) {
            CoexpressionDaoImpl.log
                    .info( "Found " + resultsFound + " results for " + ( genes.size() - genesNeeded.size() )
                            + " genes in the cache at stringency " + stringency );
        }

        return genesNeeded;
    }

    private Map<Long, List<CoexpressionValueObject>> convertToValueObjects( List<Object[]> rawResults,
            List<Object[]> supportDetails, Collection<Long> geneIds ) {

        int removed = 0;
        Set<NonPersistentNonOrderedCoexpLink> allSeen = new HashSet<>( rawResults.size() );

        // unwrap the supportDetails into a map.
        Map<Long, Set<Long>> supportDetailsLists = null;
        if ( supportDetails != null ) {
            supportDetailsLists = new HashMap<>();
            for ( Object[] oa : supportDetails ) {
                Long id = ( ( BigInteger ) oa[0] ).longValue();
                byte[] data = ( byte[] ) oa[1];
                IdArrayValueObject vo = new IdArrayValueObject( data );
                supportDetailsLists.put( id, vo.getIdsSet() );
            }
        }

        StopWatch timer = new StopWatch();
        timer.start();

        Map<Long, List<CoexpressionValueObject>> results = new HashMap<>();
        int numUnsupported = 0;
        int n = 0;
        for ( Object[] oa : rawResults ) {
            Long id = ( ( BigInteger ) oa[0] ).longValue();
            Boolean pos = ( byte ) oa[1] > 0;
            Integer support = ( Integer ) oa[2];
            Long queryGeneId = ( ( BigInteger ) oa[3] ).longValue();
            Long secondGene = ( ( BigInteger ) oa[4] ).longValue();
            Long supportDetailsId = ( ( BigInteger ) oa[5] ).longValue();

            if ( support == 0 ) {
                throw new IllegalArgumentException( "Links should not be unsupported: " + id );
            }

            NonPersistentNonOrderedCoexpLink seen = new NonPersistentNonOrderedCoexpLink( queryGeneId, secondGene,
                    pos );

            /*
             * remove duplicates, since each link can be here twice (x->y and y->x). (can happen.)
             */
            if ( allSeen.contains( seen ) ) {
                ++removed;
                continue;
            }

            allSeen.add( seen );

            if ( !results.containsKey( queryGeneId ) ) {
                results.put( queryGeneId, new ArrayList<CoexpressionValueObject>() );
            }

            CoexpressionValueObject g2gvo = new CoexpressionValueObject( queryGeneId, secondGene, pos, support,
                    supportDetailsId,
                    supportDetailsLists == null ? null : supportDetailsLists.get( supportDetailsId ) );

            assert g2gvo.getNumDatasetsSupporting() > 0;

            results.get( queryGeneId ).add( g2gvo );

            if ( geneIds != null && geneIds.contains( g2gvo.getCoexGeneId() ) ) {
                g2gvo.setInterQueryLink( true );
            }

            if ( ++n % 1000 == 0 && timer.getTime() > 1000 ) {
                CoexpressionDaoImpl.log.debug( "Process " + n + " coexpressions: " + timer.getTime() + "ms" );
                n = 0;
                timer.reset();
                timer.start();
            }

        }

        if ( removed > 0 )
            CoexpressionDaoImpl.log
                    .debug( "Removed " + removed + " duplicate links while converting to value objects" );
        //noinspection ConstantConditions // Can change
        if ( numUnsupported > 0 )
            CoexpressionDaoImpl.log.info( "Removed " + numUnsupported + " links that had support of zero." );

        if ( results.isEmpty() )
            throw new IllegalStateException( "Removed everything! (of " + rawResults.size() + " results)" );

        return results;

    }

    /**
     * Remove duplicates and convert to value objects. Links are marked as "interQuery" if the geneIds is non-null and
     * the link is between two of them.
     *
     * @param rawResults from the database. The support details might not have been fetched.
     * @param geneIds    gene IDs used in the query, can be null
     * @return value objects, organized by the "first" gene of each entity. Note: For some query genes, we might not
     * have gotten any results.
     */
    private Map<Long, List<CoexpressionValueObject>> convertToValueObjects( List<Gene2GeneCoexpression> rawResults,
            @Nullable Collection<Long> geneIds ) {

        int removed = 0;
        Set<NonPersistentNonOrderedCoexpLink> allSeen = new HashSet<>( rawResults.size() );

        // raw results from db.
        Map<Long, List<CoexpressionValueObject>> results = new HashMap<>();
        int numUnsupported = 0;
        for ( Gene2GeneCoexpression g2g : rawResults ) {

            if ( g2g.getNumDatasetsSupporting() == 0 ) {
                throw new IllegalArgumentException( "Links should not be unsupported: " + g2g );
            }

            Long queryGeneId = g2g.getFirstGene();

            if ( geneIds != null && !geneIds.contains( queryGeneId ) ) {
                continue;
            }

            NonPersistentNonOrderedCoexpLink seen = new NonPersistentNonOrderedCoexpLink( g2g );

            /*
             * remove duplicates, since each link can be here twice (x->y and y->x). (can happen; + and - links are
             * counted separately.)
             */
            if ( allSeen.contains( seen ) ) {
                ++removed;
                continue;
            }

            allSeen.add( seen );

            if ( !results.containsKey( queryGeneId ) ) {
                results.put( queryGeneId, new ArrayList<CoexpressionValueObject>() );
            }

            CoexpressionValueObject g2gvo = new CoexpressionValueObject( g2g );
            assert g2gvo.getNumDatasetsSupporting() > 0;

            results.get( queryGeneId ).add( g2gvo );

            if ( geneIds != null && geneIds.contains( g2gvo.getCoexGeneId() ) ) {
                g2gvo.setInterQueryLink( true );
            }

        }

        if ( removed > 0 )
            CoexpressionDaoImpl.log.debug( "Removed " + removed + " duplicate links" );
        //noinspection ConstantConditions // Can change
        if ( numUnsupported > 0 )
            CoexpressionDaoImpl.log.info( "Removed " + numUnsupported + " links that had support of zero." );

        if ( results.isEmpty() )
            throw new IllegalStateException( "Removed everything! (of" + rawResults.size() + " results)" );

        return results;
    }

    private Integer countLinks( Taxon t, BioAssaySet ee ) {
        int rawCount = ( ( BigInteger ) sessionFactory.getCurrentSession().createSQLQuery(
                "select count(*) from " + CoexpressionQueryUtils.getExperimentLinkTableName( t )
                        + " e where e.EXPERIMENT_FK=:ee" ).setParameter( "ee", ee.getId() ).uniqueResult() ).intValue();
        // this includes the flipped versions.
        assert rawCount % 2 == 0;
        return rawCount / 2;
    }

    /**
     * Find link (or null) based on the genes and direction of correlation in the given nonpersistent link.
     *
     * @param q               q
     * @param g2g             g2g
     * @param existingResults index of which links already have an entry in the database (possibly with a support of
     *                        zero)
     * @return gene 2 gene coexp
     */
    private Gene2GeneCoexpression findLink( Query q, NonPersistentNonOrderedCoexpLink g2g,
            Map<NonPersistentNonOrderedCoexpLink, Boolean> existingResults ) {

        Long firstGene = g2g.getFirstGene();
        Long secondGene = g2g.getSecondGene();

        assert firstGene < secondGene;

        if ( existingResults.containsKey( g2g ) && existingResults.get( g2g ) ) {
            try {
                q.setParameter( "f", firstGene );
                q.setParameter( "s", secondGene );
                q.setParameter( "pc", g2g.isPositiveCorrelation() );
                Gene2GeneCoexpression existingLink = ( Gene2GeneCoexpression ) q.uniqueResult();
                if ( CoexpressionDaoImpl.log.isDebugEnabled() && existingLink != null && existingResults
                        .containsKey( g2g ) && existingResults.get( g2g ) )
                    CoexpressionDaoImpl.log
                            .debug( "fetched existing link: " + existingLink + " (" + g2g + ") " + existingResults
                                    .get( g2g ) );
                return existingLink; // which can be null
            } catch ( HibernateException e ) {
                CoexpressionDaoImpl.log.error( "Error while searching for: " + g2g + ": " + e.getMessage() );
                throw e;
            }
        }
        // // it isn't in the existing results we fetched already, so we don't bother checking
        if ( CoexpressionDaoImpl.log.isDebugEnabled() )
            CoexpressionDaoImpl.log.debug( "No existing link for " + g2g );
        return null;

    }

    /**
     * This method is for internal use only since it does not constrain on datasets. E.g. for node degree computations.
     *
     * @param gene gene
     * @return results for the gene.
     */
    private List<CoexpressionValueObject> getCoexpression( Gene gene ) {

        // DO NOT change this to use the alternative method getCoexpressionFromDbViaGenes2
        Map<Long, List<CoexpressionValueObject>> r = this.getCoexpressionFromDbViaGenes( Collections.singleton( gene.getId() ),
                CoexpressionQueryUtils.getGeneLinkClassName( gene ) );

        List<CoexpressionValueObject> rr = r.get( gene.getId() );

        if ( rr == null ) {
            return new ArrayList<>();
        }

        return rr;
    }

    /**
     * @param t          t
     * @param experiment ee
     * @return all the links which involve this experiment, including the "flipped" versions.
     */
    private Collection<Gene2GeneCoexpression> getCoexpression( Taxon t, BioAssaySet experiment ) {
        Session sess = sessionFactory.getCurrentSession();

        // distinct because ee links are stored twice. However, the flipped versions of the ee links are linked to only
        // the forward version, so we only get half of the g2g links here.
        CoexpressionDaoImpl.log.info( "Fetching support details ..." );
        List<Long> supportDetails = sess.createQuery(
                        "select distinct sd.id from " + CoexpressionQueryUtils.getExperimentLinkClassName( t ) + " e, "
                                + CoexpressionQueryUtils.getGeneLinkClassName( t )
                                + " g2g join g2g.supportDetails sd where e.experiment=:ee and e.linkId = g2g.id " )
                .setParameter( "ee", experiment ).list();

        Collections.sort( supportDetails );

        List<Gene2GeneCoexpression> results = new ArrayList<>();

        CoexpressionDaoImpl.log.info( "Fetching links ..." );
        // refetch, this time in a manner that gets the flipped versions too.
        int i = 0;
        BatchIterator<Long> bi = BatchIterator.batches( supportDetails, 1024 );
        for ( ; bi.hasNext(); ) {
            results.addAll( sess.createQuery( "from " + CoexpressionQueryUtils.getGeneLinkClassName( t )
                            + " g2g join fetch g2g.supportDetails sd where sd.id in (:ids)" )
                    .setParameterList( "ids", bi.next() ).list() );
            if ( ++i % 200 == 0 ) {
                CoexpressionDaoImpl.log.info( i + " batches fetched (" + results.size() + " links fetched so far)" );
            }
        }

        assert results.size() % 2 == 0; // not a great check, but we should have flipped versions of every link.

        CoexpressionDaoImpl.log.info( "Fetched " + results.size() + " links" );
        return results;

    }

    /**
     * Key method. Depending on the input, the query is done experiment-first or gene-first. This is a low-level method
     * called by several others.
     * The support and tested-in details (if populated) will reflect only the datasets given. This means that data might
     * be removed if it no longer meets stringency requirements.
     *
     * @param t          t
     * @param genes      must be non-null, but can be empty to remove constraint on genes.
     * @param bas        must be non-empty.
     * @param stringency stringency
     * @param maxResults max results
     * @param quick      quick
     * @return map
     */
    private Map<Long, List<CoexpressionValueObject>> getCoexpressionFromCacheOrDb( Taxon t, Collection<Long> genes,
            Collection<Long> bas, int stringency, int maxResults, boolean quick ) {

        /*
         * If the stringency is too low (relative to the total number of datasets analyzed in the system), we end up
         * getting tons of data from the LINK table that then gets thrown out at the SUPPORT_DETAILS phase. Because the
         * stringency is largely set based on the number of data sets (genes too, but a lesser extent), this is partly
         * accounted for. But we should check both since the stringency can bee too low.
         */

        assert !bas.isEmpty();

        Map<Long, List<CoexpressionValueObject>> results;
        if ( genes.isEmpty() && bas.size() < CoexpressionDaoImpl.MAX_DATASETS_FOR_DATASET_ONLY_QUERY ) {
            /*
             * Experiment-major mode, no gene constraint: Find links common to the experiments in question at the
             * requested stringency. This could be quite slow since the cache cannot be used very well, so the caller
             * has to decide whether to allow this.
             *
             * NOTE we could have an experiment-level cache, but it would get big very fast for limited utility.
             */
            if ( bas.size() > 1 )
                CoexpressionDaoImpl.log.info( "Query in experiment-only mode, no gene constraint, " + bas.size()
                        + " datasets specified, stringency=" + stringency );
            results = this.getCoexpressionFromDbViaExperiments( t, bas, quick );

        } else if ( bas.size() < CoexpressionDaoImpl.MAX_DATASETS_FOR_DATASET_FIRST_QUERY
                && genes.size() > CoexpressionDaoImpl.MIN_GENES_FOR_DATASET_FIRST_QUERY ) {
            /*
             * Experiment-major mode, with gene constraint: get results for the given genes in just the given data sets;
             * fetch the details after that.
             */
            if ( bas.size() > 1 )
                CoexpressionDaoImpl.log.info( "Query in experiment-first mode, with gene constraint, " + bas.size()
                        + " datasets specified, stringency=" + stringency );
            results = this.getCoexpressionFromCacheOrDbViaExperiments( t, genes, bas, stringency, quick );

        } else if ( !genes.isEmpty() ) {
            /*
             * Gene-major mode: get all the results for the genes; filter for data sets selection separately.
             */
            if ( genes.size() > 1 ) {
                CoexpressionDaoImpl.log.info( "Query in gene-first mode for " + genes.size() + " genes, " + bas.size()
                        + " datasets specified, stringency=" + stringency );
            }
            results = this.getCoexpressionFromCacheOrDbViaGenes( t, genes, stringency, quick );

        } else {
            throw new IllegalArgumentException(
                    "Query cannot be safely constructed, please provide more constraints to datasets and/or genes" );
        }

        this.trimAndFinishResults( results, bas, stringency, maxResults );

        return results;
    }

    /*
     * Get links from the cache or the database, querying in experiment-first mode, but constrained to involve the given
     * genes. Does not do the trimming step, nor are the results guaranteed to meet the stringency set.
     */
    private Map<Long, List<CoexpressionValueObject>> getCoexpressionFromCacheOrDbViaExperiments( Taxon t,
            Collection<Long> genes, Collection<Long> bas, int stringency, boolean quick ) {

        assert stringency <= bas.size();
        assert !genes.isEmpty();

        Map<Long, List<CoexpressionValueObject>> results = new HashMap<>();

        /*
         * First, check the cache -- if the stringency is >= limit
         */
        Collection<Long> genesNeeded = new HashSet<>( genes );
        if ( stringency >= CoexpressionCache.CACHE_QUERY_STRINGENCY ) {
            genesNeeded = this.checkCache( genes, results );
            if ( genesNeeded.isEmpty() ) {
                return results;
            }
        }

        /*
         * Get all the data for all the experiments queried, constrained to involve the genes in question.
         *
         * This uses the ECL1EFK index, which is of (experiment, gene1, gene2). Note that if there are a lot of genes
         * this can get slow ...
         */
        Query q = sessionFactory.getCurrentSession().createQuery(
                " from " + CoexpressionQueryUtils.getExperimentLinkClassName( t )
                        + " where experiment.id in (:ees) and firstGene in (:genes)" );

        // May need to batch over genes...
        BatchIterator<Long> it = BatchIterator.batches( bas, CoexpressionDaoImpl.BATCH_SIZE_SMALL );

        StopWatch timer = new StopWatch();
        timer.start();
        List<ExperimentCoexpressionLink> links = new ArrayList<>();
        for ( ; it.hasNext(); ) {
            q.setParameterList( "ees", it.next() ).setParameterList( "genes", genesNeeded );
            links.addAll( q.list() );
        }

        if ( timer.getTime() > 2000 ) {
            CoexpressionDaoImpl.log
                    .info( "Query for coexp for : " + genes.size() + " genes " + " in " + bas.size() + " experiments: "
                            + timer.getTime() + "ms" );
        }

        /*
         * Track the support for the links among the queried data sets as we go over this in experiment-major mode.
         */
        //noinspection MismatchedQueryAndUpdateOfCollection // We still need to compare it to stringency
        CountingMap<Long> supportCounts = new CountingMap<>();
        List<Long> keepers = new ArrayList<>();
        for ( ExperimentCoexpressionLink link : links ) {

            assert genes.contains( link.getFirstGene() );

            if ( supportCounts.increment( link.getLinkId() ) >= stringency ) {
                keepers.add( link.getLinkId() );
            }
        }

        if ( keepers.isEmpty() ) {
            return new HashMap<>();
        }

        return this.loadAndConvertLinks( t, keepers, genes, quick );
    }

    /**
     * Fetch coexpression data for one or more genes, without a constraint on data sets, but with other parameters
     * possible. It checks the cache, then the database. Results not retrieved from the cache will be immediately cached
     * (if appropriate)
     *
     * @param t          taxon
     * @param genes      IDs, assumed to be all from the same taxon
     * @param stringency minimum level of support required
     * @param quick      whether to fill in the information on which data sets were supporting and how many datasets were
     *                   tested.
     * @return map of gene ids to ranked list of coexpression value objects, which will still need to be trimmed.
     */
    private Map<Long, List<CoexpressionValueObject>> getCoexpressionFromCacheOrDbViaGenes( Taxon t,
            Collection<Long> genes, int stringency, boolean quick ) {

        Map<Long, List<CoexpressionValueObject>> finalResult = new HashMap<>();

        /*
         * First, check the cache -- if the stringency is > =limit
         */
        Collection<Long> genesNeeded = new HashSet<>( genes );
        if ( stringency >= CoexpressionCache.CACHE_QUERY_STRINGENCY ) {
            genesNeeded = this.checkCache( genes, finalResult );
            if ( genesNeeded.isEmpty() ) {
                return finalResult;
            }
        }

        // we assume the genes are from the same taxon.
        assert t != null;

        // fetch rest of genes needed from the database.
        StopWatch timer = new StopWatch();
        timer.start();
        int CHUNK_SIZE = 64; // how many genes to get at once.
        int genesQueried = 0;
        BatchIterator<Long> geneIdsIt = new BatchIterator<>( genesNeeded, CHUNK_SIZE );
        int total = 0;

        for ( ; geneIdsIt.hasNext(); ) {
            StopWatch innertimer = new StopWatch();
            innertimer.start();
            Collection<Long> batch = geneIdsIt.next();

            Map<Long, List<CoexpressionValueObject>> rr = this
                    .getCoexpressionFromDbViaGenes2( batch, t, stringency, !quick );

            // we should not cache unless everything is populated
            if ( !rr.isEmpty() && stringency <= CoexpressionCache.CACHE_QUERY_STRINGENCY && !quick ) {
                gene2GeneCoexpressionCache.cacheCoexpression( rr );
            }

            for ( Long g : rr.keySet() ) {
                // could replace with a single putAll but want this assertion for now.
                assert !finalResult.containsKey( g );
                finalResult.put( g, rr.get( g ) );
                total += rr.get( g ).size();
            }

            if ( innertimer.getTime() > 1000 && genesQueried > 0 ) {
                CoexpressionDaoImpl.log
                        .debug( "Fetched " + total + "  coexpression results from db for " + genesQueried + "/"
                                + genesNeeded.size() + " genes needed in " + innertimer.getTime() + "ms" );
            }

            genesQueried += batch.size();

        }
        if ( timer.getTime() > 10000 ) {
            // this raw count is not really relevant - it has to be filtered later.
            CoexpressionDaoImpl.log
                    .debug( "Fetched " + total + "  coexpression results from db or cache for " + genes.size()
                            + " genes in " + timer.getTime() + "ms" );
        }

        return finalResult;
    }

    /**
     * Find links common to the given experiments at stringency given, without any constraint on the genes.
     *
     * @param t     t
     * @param bas   not too many or else this could be slow, especially if stringency << bas.size().
     * @param quick quick
     */
    private Map<Long, List<CoexpressionValueObject>> getCoexpressionFromDbViaExperiments( Taxon t, Collection<Long> bas,
            boolean quick ) {
        /*
         * Get all the data for all the experiments queried. We avoid a join on the gene2gene table (defeats purpose).
         * Distinct okay here because we're not counting stringency based on the raw results here - see comment below.
         */
        Query q = sessionFactory.getCurrentSession().createQuery(
                "select distinct linkId from " + CoexpressionQueryUtils.getExperimentLinkClassName( t )
                        + " where experiment.id in (:ees)" ).setParameterList( "ees", bas );
        List<Long> links = q.list();

        if ( links.isEmpty() ) {
            return new HashMap<>();
        }

        return this.loadAndConvertLinks( t, links, null, quick );
    }

    /**
     * Gene-focused query. Use this if you don't care about which data sets are involved (or if there are many data
     * sets), for a relatively small number of genes. This DOES NOT cache the results, the caller has to do that. It
     * also does not check the cache.
     *
     * @param geneIds   the gene IDs
     * @param className the class name
     * @return results without any limit on the size, each list is already sorted.
     */
    private Map<Long, List<CoexpressionValueObject>> getCoexpressionFromDbViaGenes( Collection<Long> geneIds,
            String className ) {

        Query q = this.buildQuery( geneIds, className );
        StopWatch timer = new StopWatch();
        timer.start();
        List<Gene2GeneCoexpression> rawResults = q.list();

        if ( timer.getTime() > 1000 ) {
            CoexpressionDaoImpl.log
                    .debug( "Initial coexp query for " + geneIds.size() + "genes took " + timer.getTime() + "ms: "
                            + rawResults.size() + " results" );
            CoexpressionDaoImpl.log.debug( "Query was: " + q.getQueryString() );
        }

        if ( rawResults.isEmpty() )
            return new HashMap<>();

        timer.reset();
        timer.start();
        Map<Long, List<CoexpressionValueObject>> results = this.convertToValueObjects( rawResults, geneIds );

        for ( Long g : results.keySet() ) {
            List<CoexpressionValueObject> gc = results.get( g );
            Collections.sort( gc );
        }

        if ( timer.getTime() > 100 ) {
            CoexpressionDaoImpl.log
                    .debug( "Convert to value objects, filter, sort and finish " + rawResults.size() + " results: "
                            + timer.getTime() + "ms" );
        }

        return results;
    }

    /**
     * Alternative method: query twice, once to get the coexpression basics and then again to get the support details,
     * instead of using a join.
     *
     * @param populateTestedInDetails populate tested in details
     * @param stringency              stringency
     * @param geneIds                 gene IDs
     * @param t                       taxon
     */
    private Map<Long, List<CoexpressionValueObject>> getCoexpressionFromDbViaGenes2( Collection<Long> geneIds, Taxon t,
            int stringency, boolean populateTestedInDetails ) {

        StopWatch timer = new StopWatch();
        timer.start();
        List<Object[]> q1results = this.getRawCoexpressionFromDbViaGenes( geneIds, t, stringency );

        CoexpressionDaoImpl.log
                .debug( q1results.size() + " raw coexpression results for " + geneIds.size() + " genes at support>="
                        + stringency + " " + timer.getTime() + "ms" );

        if ( q1results.isEmpty() ) {
            return new HashMap<>();
        }

        List<Object[]> supportDetails = new ArrayList<>();

        /*
         * Because we are not trimming the results at all here, this can be a lot of data to iterate over, even at
         * high stringencies. For example, for 20 genes at a stringency of 5, because the query above does not
         * constrain to data sets, there can be >500 per gene, or >100k links in total. Fetching the support details
         * here is rather wasteful if we are not retaining the results, but we don't know that until we know which
         * data sets are supporting.
         */

        BatchIterator<Object[]> batches = BatchIterator.batches( q1results, CoexpressionDaoImpl.BATCH_SIZE );

        int n = 1;
        for ( Collection<Object[]> batch : batches ) {
            StopWatch timer2 = new StopWatch();
            timer2.start();

            List<Long> supportDetailsIds = new ArrayList<>();
            for ( Object[] oa : batch ) {
                Long supportDetailsId = ( ( BigInteger ) oa[5] ).longValue();
                supportDetailsIds.add( supportDetailsId );
            }

            // Note: should never be empty
            String sqlQuery2 = "select ID,BYTES from " + CoexpressionQueryUtils.getSupportDetailsTableName( t )
                    + " where ID in (:ids)";
            SQLQuery query2 = sessionFactory.getCurrentSession().createSQLQuery( sqlQuery2 );

            query2.setParameterList( "ids", supportDetailsIds.toArray() );

            supportDetails.addAll( query2.list() );

            if ( timer2.getTime() > 1000 ) {
                CoexpressionDaoImpl.log.debug( "Fetch batch " + n + " of support details: " + timer2.getTime() + "ms" );
            }
            n++;
        }

        CoexpressionDaoImpl.log
                .debug( "Fetched details for " + supportDetails.size() + " coexpressions, " + n + " batches" );

        if ( timer.getTime() > 5000 ) {
            CoexpressionDaoImpl.log
                    .info( "Coexpression query: " + geneIds.size() + " genes took " + timer.getTime() + "ms: "
                            + q1results.size() + " results" );
        }

        timer.reset();
        timer.start();
        // it might be better to do this in the loop above, incrementally per batch.
        Map<Long, List<CoexpressionValueObject>> results = this
                .convertToValueObjects( q1results, supportDetails, geneIds );
        if ( timer.getTime() > 100 ) {
            CoexpressionDaoImpl.log
                    .info( "Convert to value objects " + q1results.size() + " results: " + timer.getTime() + "ms" );
        }

        timer.reset();
        timer.start();

        for ( Long g : results.keySet() ) {
            List<CoexpressionValueObject> gc = results.get( g );
            Collections.sort( gc );
            if ( populateTestedInDetails ) {
                this.populateTestedInDetails( gc );
            }
        }

        if ( timer.getTime() > 100 ) {
            CoexpressionDaoImpl.log
                    .info( "Filter, sort and finish " + q1results.size() + " results: " + timer.getTime() + "ms" );
        }

        return results;
    }

    /**
     * Find links among the given genes in the given experiments, querying the experiment-level table. Does not check
     * the cache. There are easily hundreds of genes, number of experiments would be relatively small (otherwise we
     * would query gene-major).
     *
     * @param bas   not too many
     * @param t     taxon
     * @param genes gene IDs
     * @param quick quick run
     */
    private Map<Long, List<CoexpressionValueObject>> getInterCoexpressionFromDbViaExperiments( Taxon t,
            Collection<Long> genes, Collection<Long> bas, boolean quick ) {

        // distinct okay here because we're not counting stringency based on the raw results here. See comment below.
        Query q = sessionFactory.getCurrentSession().createQuery(
                        "select distinct linkId from " + CoexpressionQueryUtils.getExperimentLinkClassName( t )
                                + " where experiment.id in (:ees) and firstGene in (:genes) and secondGene in (:genes2)" )
                .setParameterList( "ees", bas ).setParameterList( "genes", genes ).setParameterList( "genes2", genes );

        StopWatch timer = new StopWatch();
        timer.start();
        List<Long> links = q.list();

        // We cannot batch this because we miss some combinations of links.
        CoexpressionDaoImpl.log
                .info( links.size() + " distinct gene2gene link ids obtained for experiment-level query for " + genes
                        .size() + "genes in " + bas.size() + " experiments: " + timer.getTime() + "ms" );

        /*
         * Track the support for the links seen as we go over this in experiment-major mode.
         *
         * WARNING: the following idea is messed up because the links can be in a-b or b-a order, which are separate db
         * entities, and therefore counted separately here. [Only retain links (keepers) that meet the requested
         * stringency. Note that the only way we know the support is counting the number of experiments the link is in,
         * which happens in trimAndFinishResults(). See bug 4411
         */

        if ( links.isEmpty() ) {
            return new HashMap<>();
        }

        return this.loadAndConvertLinks( t, new ArrayList<>( links ), genes, quick );
    }

    /*
     * Does not check the cache - this must be done by the caller
     *
     */
    private Map<Long, List<CoexpressionValueObject>> getInterCoexpressionFromDbViaGenes( Taxon taxon,
            Collection<Long> genes, int stringency, boolean quick ) {

        if ( genes.size() == 0 )
            return new HashMap<>();

        Map<Long, List<CoexpressionValueObject>> results = new HashMap<>();

        // we assume the genes are from the same taxon. Confirmed: this uses the index (see bug 4055)
        String g2gClassName = CoexpressionQueryUtils.getGeneLinkClassName( taxon );
        final String firstQueryString = "select g2g from " + g2gClassName
                + " as g2g where g2g.firstGene in (:qgene) and g2g.secondGene in (:genes) "
                + "and g2g.numDataSetsSupporting  >= :stringency ";

        /*
         * Note: if the number of genes is too large, it may be faster to simply query without the second 'in' clause
         * and filter the results.
         */

        StopWatch oTimer = new StopWatch();
        oTimer.start();
        int batchSize = 32;
        BatchIterator<Long> it = BatchIterator.batches( genes, batchSize );

        List<CoexpressionValueObject> g2gs = new ArrayList<>( genes.size() );
        Set<CoexpressionValueObject> seen = new HashSet<>();

        while ( it.hasNext() ) {

            Collection<Long> queryGeneBatch = it.next();
            StopWatch timer = new StopWatch();
            timer.start();

            Collection<Gene2GeneCoexpression> r = sessionFactory.getCurrentSession()
                    .createQuery( firstQueryString )
                    .setParameterList( "qgene", queryGeneBatch )
                    .setParameterList( "genes", genes )
                    .setParameter( "stringency", stringency )
                    .list();

            if ( timer.getTime() > 5000 ) {
                CoexpressionDaoImpl.log
                        .debug( "Slow query: " + firstQueryString + " took " + timer.getTime() + "ms (" + queryGeneBatch
                                .size() + " query gene batch, " + genes.size() + " target genes), Stringency="
                                + stringency );

                // raw db results, for a batch of genes, add to the whole.
                for ( Gene2GeneCoexpression g2g : r ) {
                    CoexpressionValueObject g2gvo = new CoexpressionValueObject( g2g );

                    // we get the links in 'both directions' so we want to omit them. This means some of the query genes
                    // might not be returned as query genes, since they show up in the 'coexpressed' gene instead.
                    if ( seen.contains( g2gvo ) )
                        continue;
                    seen.add( g2gvo );
                    g2gvo.setInterQueryLink( true );
                    g2gs.add( g2gvo );
                }
            }
        }

        if ( !quick && !g2gs.isEmpty() ) {
            StopWatch timer = new StopWatch();
            timer.start();

            this.populateTestedInDetails( g2gs );

            if ( timer.getTime() > 2000 ) {
                CoexpressionDaoImpl.log
                        .debug( "Query genes only,fetch tested-in details " + g2gs.size() + " results took " + timer
                                .getTime() + "ms" );
            }

            timer.reset();
            timer.start();
        }

        /*
         * all the genes are guaranteed to be in the query list.
         */
        for ( CoexpressionValueObject g2g : g2gs ) {
            if ( !results.containsKey( g2g.getQueryGeneId() ) ) {
                results.put( g2g.getQueryGeneId(), new ArrayList<CoexpressionValueObject>() );
            }
            results.get( g2g.getQueryGeneId() ).add( g2g );
        }

        if ( oTimer.getTime() > 2000 ) {
            CoexpressionDaoImpl.log
                    .info( "Query genes only, fetch for " + genes.size() + " genes took " + oTimer.getTime() + "ms" );
        }
        for ( Long id : results.keySet() ) {
            Collections.sort( results.get( id ) );
        }

        return results;

    }

    private Map<Long, Collection<Long>> getQuickCoex( Collection<Long> ba ) {
        Session sess = sessionFactory.getCurrentSession();
        Collection<GeneCoexpressedGenes> r = sess.createQuery( "from GeneCoexpressedGenes where geneId in (:ids)" )
                .setParameterList( "ids", ba ).list();

        Map<Long, Collection<Long>> result = new HashMap<>();
        for ( GeneCoexpressedGenes gcog : r ) {
            result.put( gcog.getGeneId(), gcog.getIds() );
        }

        return result;
    }

    /**
     * Load links given their ids (e.g. retrieved from the EE link tables). This is predicted to be slow when fetching
     * many links, because of random seeks in the g2g table.
     *
     * @param t          t
     * @param linkIds    to fetch; should be unique. Can already be stringency-filtered to some extent, but this will be
     *                   checked again.
     * @param queryGenes can be null if was unconstrained
     * @param quick      if true, the 'testedin' details will be populated.
     * @return map
     */
    private Map<Long, List<CoexpressionValueObject>> loadAndConvertLinks( Taxon t, List<Long> linkIds,
            @Nullable Collection<Long> queryGenes, boolean quick ) {

        assert !linkIds.isEmpty();

        /*
         * Note that we are not checking the cache, but we could by getting the firstGene from the EE-level links?
         */
        Query q = sessionFactory.getCurrentSession().createQuery(
                "from " + CoexpressionQueryUtils.getGeneLinkClassName( t )
                        + " g2g join fetch g2g.supportDetails where g2g.id in (:ids)" );

        /*
         * It is possible that we are retrieving the same underlying link twice - in the a-b and b-a orientations. Those
         * have to be merged. This is taken care of in the convertToValueObjects
         */
        int BATCH_SIZE = 1024;
        Collections.sort( linkIds ); // more efficient querying.

        BatchIterator<Long> idBatches = BatchIterator.batches( linkIds, BATCH_SIZE );

        StopWatch timer = new StopWatch();
        timer.start();
        List<Gene2GeneCoexpression> rawResults = new ArrayList<>();
        for ( ; idBatches.hasNext(); ) {
            rawResults.addAll( q.setParameterList( "ids", idBatches.next() ).list() );
        }

        if ( rawResults.isEmpty() ) {
            CoexpressionDaoImpl.log.warn( "Ids were invalid: no results for linkIds including " + linkIds.get( 0 ) );
            return new HashMap<>();
        } else if ( rawResults.size() < linkIds.size() && rawResults.size() < new HashSet<>( linkIds ).size() ) {
            // maybe linkIds has repeats?
            CoexpressionDaoImpl.log
                    .warn( "Some ids were invalid, only got " + rawResults.size() + ", expected " + linkIds.size()
                            + " results" );
        }

        if ( timer.getTime() > 2000 ) {
            CoexpressionDaoImpl.log
                    .info( "Load and convert " + rawResults.size() + " links: " + timer.getTime() + "ms" );
        }

        Map<Long, List<CoexpressionValueObject>> results = this.convertToValueObjects( rawResults, queryGenes );
        for ( Long g : results.keySet() ) {
            if ( !quick ) {
                assert queryGenes == null || queryGenes.contains( g );
                this.populateTestedInDetails( results.get( g ) );
            }
        }
        return results;
    }

    private void populateSettings( List<CoexpressionValueObject> list, int size, int maxResults ) {
        for ( CoexpressionValueObject g2g : list ) {
            g2g.setQueryStringency( size );
            g2g.setMaxResults( maxResults );
        }
    }

    /**
     * When fetching data. Requires database hits, but values for testedin are cached.
     *
     * @param g2gLinks links
     */
    private void populateTestedInDetails( Collection<CoexpressionValueObject> g2gLinks ) {
        assert !g2gLinks.isEmpty();

        StopWatch timer = new StopWatch();
        timer.start();
        // GeneCoexpressionTestedIn are one-per-gene so we first gather up all the unique genes we have to look at.

        Map<Long, GeneCoexpressionTestedIn> gcTestedIn = new HashMap<>();
        Set<Long> genes = new HashSet<>();
        for ( CoexpressionValueObject gene2GeneCoexpression : g2gLinks ) {
            Long queryGeneId = gene2GeneCoexpression.getQueryGeneId();
            GeneCoexpressionTestedIn queryGeneTestedIn = geneTestedInCache.get( queryGeneId );
            if ( queryGeneTestedIn == null ) {
                genes.add( queryGeneId );
            } else {
                gcTestedIn.put( queryGeneId, queryGeneTestedIn );
            }

            Long coexGeneId = gene2GeneCoexpression.getCoexGeneId();
            GeneCoexpressionTestedIn coexGeneTestedIn = geneTestedInCache.get( coexGeneId );
            if ( coexGeneTestedIn == null ) {
                genes.add( coexGeneId );
            } else {
                gcTestedIn.put( coexGeneId, coexGeneTestedIn );
            }
        }

        if ( !genes.isEmpty() ) {
            // fetch the GeneCoexpressionTestedIn information for those genes which were not cached.
            Query q = sessionFactory.getCurrentSession()
                    .createQuery( "from GeneCoexpressionTestedIn g where geneId in (:genes)" );

            int BATCH_SIZE = 512;
            int n = 0;
            for ( BatchIterator<Long> it = BatchIterator.batches( genes, BATCH_SIZE ); it.hasNext(); ) {
                Collection<Long> g = it.next();
                q.setParameterList( "genes", g );
                List<GeneCoexpressionTestedIn> list = q.list();
                Map<Long, GeneCoexpressionTestedIn> idMap = list.stream()
                        .collect( Collectors.toMap( GeneCoexpressionTestedIn::getGeneId, g2 -> g2, ( a, b ) -> b ) );
                geneTestedInCache.cache( idMap );
                gcTestedIn.putAll( idMap );
                ++n;
            }

            if ( timer.getTime() > 1000 )
                CoexpressionDaoImpl.log
                        .debug( "Query for tested-in details for " + genes.size() + " genes: " + timer.getTime()
                                + " ms (" + n + " batches), values fetched or from cache size=" + gcTestedIn.size() );
        }

        timer.reset();
        timer.start();

        // copy it into the g2g value objects.
        for ( CoexpressionValueObject g2g : g2gLinks ) {
            assert g2g.getNumDatasetsSupporting() > 0 : g2g + " has support less than 1";

            Long id1 = g2g.getQueryGeneId();
            Long id2 = g2g.getCoexGeneId();
            GeneCoexpressionTestedIn geneCoexpressionTestedIn1 = gcTestedIn.get( id1 );
            GeneCoexpressionTestedIn geneCoexpressionTestedIn2 = gcTestedIn.get( id2 );

            if ( geneCoexpressionTestedIn1 == null || geneCoexpressionTestedIn2 == null ) {
                throw new IllegalStateException( "Was missing GeneCoexpressionTestedIn data for genes in " + g2g );
            }

            if ( geneCoexpressionTestedIn1.getNumDatasetsTestedIn() == 0
                    || geneCoexpressionTestedIn2.getNumDatasetsTestedIn() == 0 ) {
                throw new IllegalStateException( g2g + ": had no data sets tested in: " + StringUtils
                        .join( geneCoexpressionTestedIn1.getIds(), "," ) + " :: " + StringUtils
                        .join( geneCoexpressionTestedIn2.getIds(), "," ) );
            }

            Set<Long> testedIn = geneCoexpressionTestedIn1.andSet( geneCoexpressionTestedIn2 );

            if ( testedIn.isEmpty() ) {
                throw new IllegalStateException( g2g + ": had no data sets tested in: " + StringUtils
                        .join( geneCoexpressionTestedIn1.getIds(), "," ) + " :: " + StringUtils
                        .join( geneCoexpressionTestedIn2.getIds(), "," ) );
            }
            g2g.setTestedInDatasets( testedIn );
        }

        if ( timer.getTime() > 100 )
            CoexpressionDaoImpl.log
                    .debug( "Populate into value obects: " + timer.getTime() + "ms (" + g2gLinks.size() + " links)" );

    }

    /**
     * Prefetch information on links, so when we go looking for a particular link we can decide faster. EXPERIMENTAL.
     *
     * @param links links
     * @return Map
     */
    private Map<NonPersistentNonOrderedCoexpLink, Boolean> preFetch( List<NonPersistentNonOrderedCoexpLink> links ) {

        StopWatch timer = new StopWatch();
        timer.start();

        Map<NonPersistentNonOrderedCoexpLink, Boolean> result = new HashMap<>();
        Map<Long, Set<Long>> linksToMap = CoexpressionQueryUtils.linksToMap( links );

        int BATCH_SIZE = 512;
        BatchIterator<Long> b = BatchIterator.batches( linksToMap.keySet(), BATCH_SIZE );

        Map<Long, Collection<Long>> coexg = new HashMap<>();
        for ( ; b.hasNext(); ) {
            Collection<Long> ba = b.next();
            coexg.putAll( this.getQuickCoex( ba ) );
        }

        // compare the links in hand with
        for ( NonPersistentNonOrderedCoexpLink li : links ) {

            Collection<Long> g1h = coexg.get( li.getFirstGene() );

            if ( g1h != null && g1h.contains( li.getSecondGene() ) ) {
                result.put( li, true );
                continue;
            }

            // this seems redundant.
            Collection<Long> g2h = coexg.get( li.getSecondGene() );
            if ( g2h != null && g2h.contains( li.getFirstGene() ) ) {
                result.put( li, true );
            }

            /* never are adding false */
            // result.put( li, false );
        }

        if ( !result.isEmpty() )
            CoexpressionDaoImpl.log
                    .info( "Prefetched link data for " + result.size() + "/" + links.size() + " links in " + timer
                            .getTime() + "ms" );

        return result;
    }

    private void removeCoexpressedWith( Set<NonPersistentNonOrderedCoexpLink> toRemove ) {
        Map<Long, Set<Long>> tr = CoexpressionQueryUtils.linksToMap( toRemove );

        Session sess = sessionFactory.getCurrentSession();
        int i = 0;
        for ( Long g : tr.keySet() ) {
            this.removeGeneCoexpressedWith( g, tr.get( g ) );
            if ( i++ % 1000 == 0 ) {
                sess.flush();
                sess.clear();
            }
        }
    }

    private void removeGeneCoexpressedWith( Long geneId, Collection<Long> removedGenes ) {
        Session sess = sessionFactory.getCurrentSession();

        GeneCoexpressedGenes gcti = ( GeneCoexpressedGenes ) sess
                .createQuery( "from GeneCoexpressedGenes where geneId = :id" ).setParameter( "id", geneId )
                .uniqueResult();
        // note this might be a no-op.
        if ( gcti != null ) {
            for ( Long g : removedGenes ) {
                gcti.removeEntity( g );
                sess.update( gcti );
            }
        }
    }

    /**
     * Reverting the "genes-tested-in" information is annoying: we don't know which genes to fix ahead of time. So we
     * have to check all genes for the taxon.
     *
     * @param experiment ee
     * @param t          t
     */
    private void removeTestedIn( Taxon t, BioAssaySet experiment ) {

        Session sess = sessionFactory.getCurrentSession();

        List<Long> geneids = sess.createQuery( "select id from Gene where taxon = :t" ).setParameter( "t", t ).list();

        CoexpressionDaoImpl.log
                .info( "Removing 'tested-in' information for up to " + geneids.size() + " genes for " + experiment );

        BatchIterator<Long> it = BatchIterator.batches( geneids, 1000 );
        for ( ; it.hasNext(); ) {
            Collection<Long> next = it.next();
            for ( GeneCoexpressionTestedIn gcti : ( Collection<GeneCoexpressionTestedIn> ) sess
                    .createQuery( "from GeneCoexpressionTestedIn where geneId in (:ids)" )
                    .setParameterList( "ids", next ).list() ) {
                // note this might be a no-op.
                gcti.removeEntity( experiment.getId() );
                sess.update( gcti );
            }
            sess.flush();
            sess.clear();
        }
    }

    /**
     * Save a batch of <strong>new</strong> links, and construct the to-be-persisted flipped versions.
     *
     * @param session session
     * @param linkIds will be updated with the ids of the links which were saved.
     * @param batch;  will be cleared by this call.
     * @param c       to create flipped versions of appropriate class
     * @return flipped versions which we will accumulate, sort and save later.
     */
    private List<Gene2GeneCoexpression> saveBatchAndMakeFlipped( Session session,
            Map<Long, NonPersistentNonOrderedCoexpLink> linkIds, Map<SupportDetails, Gene2GeneCoexpression> batch,
            LinkCreator c ) {

        StopWatch timer = new StopWatch();
        timer.start();
        List<Gene2GeneCoexpression> flipped = new ArrayList<>();
        for ( SupportDetails sd : batch.keySet() ) {

            // have to do this first otherwise adding the ID changes hashcode...
            Gene2GeneCoexpression g2g = batch.get( sd );

            assert g2g != null;

            session.save( sd );
            assert sd.getNumIds() > 0;

            g2g.setSupportDetails( sd );

            assert sd.getNumIds() > 0;
            assert g2g.getNumDatasetsSupporting() > 0;
            assert g2g.getSupportDetails().getNumIds() > 0;

            // make a copy that has the genes flipped; reuse the supportDetails.
            Gene2GeneCoexpression flippedG2g = c
                    .create( g2g.isPositiveCorrelation() ? 1 : -1, g2g.getSecondGene(), g2g.getFirstGene() );
            flippedG2g.setSupportDetails( g2g.getSupportDetails() );
            flipped.add( flippedG2g );

            assert flippedG2g.getFirstGene().equals( g2g.getSecondGene() );
            assert flippedG2g.getSecondGene().equals( g2g.getFirstGene() );
        }

        for ( Gene2GeneCoexpression g2g : batch.values() ) {
            Long id = ( Long ) session.save( g2g );
            linkIds.put( id, new NonPersistentNonOrderedCoexpLink( g2g ) );
        }

        session.flush();
        session.clear();
        batch.clear();

        if ( timer.getTime() > 1000 ) {
            CoexpressionDaoImpl.log.info( "Saved batch: " + timer.getTime() + "ms" );
        }

        return flipped;
    }

    private void saveExperimentLevelLinks( Session sess, LinkCreator c,
            TreeMap<Long, NonPersistentNonOrderedCoexpLink> links, BioAssaySet bioAssaySet ) {
        int progress = 0;
        int BATCH_SIZE = 1024;
        List<ExperimentCoexpressionLink> flippedLinks = new ArrayList<>();
        for ( Long linkid : links.keySet() ) {
            NonPersistentNonOrderedCoexpLink link = links.get( linkid );
            ExperimentCoexpressionLink ecl = c
                    .createEELink( bioAssaySet, linkid, link.getFirstGene(), link.getSecondGene() );

            /*
             * At same time, create flipped versions, but save them later for ordering. Notice that we use the SAME link
             * ID - not the one for the flipped version in the gene2gene table.
             *
             * Ideally we would ensure that the gene2gene link ID used is the same for all links that are between
             * the same pair of genes. That would let us be able to easily count the support directly from an
             * experiment-level query, without going to the supportDetails. I do not believe the current code guarantees
             * this.
             */
            flippedLinks.add( c.createEELink( bioAssaySet, linkid, link.getSecondGene(), link.getFirstGene() ) );

            sess.save( ecl );

            if ( ++progress % 50000 == 0 ) {
                CoexpressionDaoImpl.log
                        .info( "Created " + progress + "/" + links.size() + " experiment-level links..." );
            }

            if ( progress % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

        sess.flush();
        sess.clear();

        /*
         * Sort the flipped links by the first gene
         */
        Collections.sort( flippedLinks, new Comparator<ExperimentCoexpressionLink>() {
            @Override
            public int compare( ExperimentCoexpressionLink o1, ExperimentCoexpressionLink o2 ) {
                return o1.getFirstGene().compareTo( o2.getFirstGene() );
            }
        } );

        /*
         * Save the flipped ones.
         */
        progress = 0;
        for ( ExperimentCoexpressionLink fl : flippedLinks ) {
            sess.save( fl );

            if ( ++progress % 50000 == 0 ) {
                CoexpressionDaoImpl.log
                        .info( "Created " + progress + "/" + links.size() + " flipped experiment-level links..." );
            }

            if ( progress % BATCH_SIZE == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

        // one for the road.
        sess.flush();
        sess.clear();
    }

    /**
     * Trim results to reflect those in the given data sets, at the selected stringency. This is required for security
     * (partly) but also to remove "irrelevant" results not queried for. Genes which have no results left after
     * filtering will be removed. We also remove (per-gene) links that go over the set maxResults limit, unless it is an
     * inter-query link. It is thus possible that some links at a given stringency will not be returned.
     *
     * @param results    - map of gene to list of coexpressions
     * @param bas        can be null if there is no constraint. Stringency filter will still be applied.
     * @param stringency used to filter, and to populate the settings in the VOs.
     * @param maxResults used to filter per-gene, and to populate the settings in the VOs. 0 means no limit.
     */
    private void trimAndFinishResults( Map<Long, List<CoexpressionValueObject>> results, Collection<Long> bas,
            int stringency, int maxResults ) {

        assert stringency > 0;
        assert !bas.isEmpty();

        Set<Long> toRemove = new HashSet<>();
        for ( Long g : results.keySet() ) {
            /*
             * The results are already sorted at this point, in decreasing stringency. (??)
             */

            int kept = 0;

            for ( Iterator<CoexpressionValueObject> it = results.get( g ).iterator(); it.hasNext(); ) {
                CoexpressionValueObject g2g = it.next();

                if ( g2g.getNumDatasetsSupporting() < stringency || !g2g.trimDatasets( bas, stringency ) ) {
                    it.remove();
                } else if ( maxResults > 0 && kept >= maxResults && !g2g.isInterQueryLink() ) {
                    // only keep up to maxResults, but always keep inter-query links.
                    it.remove();
                } else {
                    // System.err.println( g2g );
                    kept++;
                }

                /*
                 * We're removing individual results for a given query gene; if we have now run out of them, we will
                 * remove the gene entirely from the results.
                 */
                if ( results.get( g ).isEmpty() ) {
                    toRemove.add( g );
                }

                assert g2g.getNumDatasetsSupporting() <= bas.size();// test for bug 4036

            }

            if ( !results.get( g ).isEmpty() ) {
                this.populateSettings( results.get( g ), stringency, maxResults );
            }
        }

        if ( !toRemove.isEmpty() ) {
            for ( Long id : toRemove ) {
                results.remove( id );
            }

            if ( CoexpressionDaoImpl.log.isDebugEnabled() ) {
                if ( results.isEmpty() ) {
                    CoexpressionDaoImpl.log
                            .debug( "After trimming, no genes had results at stringency=" + stringency + "(" + toRemove
                                    .size() + " genes)" );
                } else {
                    CoexpressionDaoImpl.log
                            .debug( "After trimming, " + toRemove.size() + " genes had no results at stringency="
                                    + stringency );
                }
            }
        }

    }

    /**
     * Mark the genes as being tested for coexpression in the data set and persist the information in the database. This
     * is run at the tail end of coexpression analysis for the data set.
     *
     * @param ee          the data set
     * @param genesTested the genes
     */
    private void updatedTestedIn( BioAssaySet ee, Collection<Gene> genesTested ) {
        Session sess = sessionFactory.getCurrentSession();
        Query q = sess.createQuery( "from GeneCoexpressionTestedIn where geneId in (:ids)" );

        Set<Long> seenGenes = new HashSet<>();
        Collection<Long> geneids = IdentifiableUtils.getIds( genesTested );
        BatchIterator<Long> bi = new BatchIterator<>( geneids, 512 );

        for ( ; bi.hasNext(); ) {

            q.setParameterList( "ids", bi.next() );

            List<GeneCoexpressionTestedIn> list = q.list();

            int count = 0;
            for ( GeneCoexpressionTestedIn gcti : list ) {

                // int old = gcti.getNumIds(); // debug code

                gcti.addEntity( ee.getId() );

                sess.update( gcti );
                // gcti.setBytes( gcti.getBytes() );

                assert gcti.isIncluded( ee.getId() );
                seenGenes.add( gcti.getGeneId() );

                if ( ++count % 256 == 0 ) {
                    sess.flush();
                    sess.clear();
                }
            }

        }

        if ( !seenGenes.isEmpty() ) {
            CoexpressionDaoImpl.log.info( "Updated tested-in information for " + seenGenes.size() + " genes" );
            this.geneTestedInCache.clearCache(); // TODO do it just for the genes changed.
        }

        sess.flush();
        sess.clear();

        // discover genes which don't have an entry at all.
        geneids.removeAll( seenGenes );
        if ( geneids.isEmpty() ) {
            return;
        }

        CoexpressionDaoImpl.log.info( "Adding tested-in information for " + geneids.size() + " genes" );
        int count = 0;
        for ( Long id : geneids ) {
            GeneCoexpressionTestedIn gcti = new GeneCoexpressionTestedIn( id );
            gcti.addEntity( ee.getId() );

            assert gcti.isIncluded( ee.getId() );
            assert gcti.getNumIds() == 1;
            sess.save( gcti );

            if ( ++count % 256 == 0 ) {
                sess.flush();
                sess.clear();
            }
        }

    }

    /**
     * Update the index about which genes have links.
     *
     * @param links links
     */
    private void updateGeneCoexpressedWith( Collection<NonPersistentNonOrderedCoexpLink> links ) {
        Map<Long, Set<Long>> coexpressions = CoexpressionQueryUtils.linksToMap( links );
        Session sess = sessionFactory.getCurrentSession();
        int i = 0;

        for ( Long g : coexpressions.keySet() ) {
            GeneCoexpressedGenes gcti = ( GeneCoexpressedGenes ) sess
                    .createQuery( "from GeneCoexpressedGenes where geneId = :id" ).setParameter( "id", g )
                    .uniqueResult();

            if ( gcti == null ) {
                gcti = new GeneCoexpressedGenes( g );
                sess.save( gcti );
            }

            gcti.addEntities( coexpressions.get( g ) );

            assert gcti.getIds().size() > 0;
            assert gcti.getIds().contains( coexpressions.get( g ).iterator().next() );

            if ( ++i % 1000 == 0 ) {
                CoexpressionDaoImpl.log
                        .info( "Updated gene-coexpressed-with information for " + i + " genes, last was geneid=" + g );
                sess.flush();
                sess.clear();
            }
        }
        CoexpressionDaoImpl.log
                .info( "Updated gene-coexpressed-with information for " + coexpressions.size() + " genes." );
    }
}
