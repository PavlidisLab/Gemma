package ubic.gemma.analysis.expression.coexpression.links;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.basecode.dataStructure.Link;
import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.model.analysis.expression.coexpression.SupportDetails;
import ubic.gemma.model.association.coexpression.CoexpressionService;
import ubic.gemma.model.association.coexpression.Gene2GeneCoexpression;
import ubic.gemma.model.association.coexpression.LinkCreator;
import ubic.gemma.model.association.coexpression.NonPersistentNonOrderedCoexpLink;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.Persister;
import ubic.gemma.util.EntityUtils;
import cern.colt.list.ObjectArrayList;

/**
 * Handles moving gene coexpression links from memory into the database; updates related meta-data.
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class LinkAnalysisPersisterImpl implements LinkAnalysisPersister {

    private static Logger log = LoggerFactory.getLogger( LinkAnalysisPersisterImpl.class );

    @Autowired
    private CoexpressionAnalysisService coexpressionAnalysisService;

    @Autowired
    private CoexpressionService gene2GeneCoexpressionService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private Persister persisterHelper;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisPersister#initializeLinksFromOldData(ubic.gemma
     * .model.genome.Taxon)
     */
    @Override
    public void initializeLinksFromOldData( Taxon t ) {
        Collection<Gene> genes = geneService.loadAll( t );
        Map<Long, Gene> idMap = EntityUtils.getIdMap( genes );

        /*
         * First count the old links for every gene, and remove genes that have too few. That set of genes has to be
         * passed in to the service so they would be recognized in the second gene. We have to do that counting as a
         * separate step because we need to know ahead of time. This might be more trouble than it is worth...
         */
        log.info( "Counting old links for " + genes.size() + " genes." );
        Map<Gene, Integer> counts = gene2GeneCoexpressionService.countOldLinks( genes );
        int LIMIT = 100;
        Set<Long> skipGenes = new HashSet<>();
        for ( Gene g : counts.keySet() ) {
            if ( counts.get( g ) < LIMIT ) {
                skipGenes.add( g.getId() );
            }
        }

        if ( skipGenes.size() == genes.size() ) {
            throw new IllegalStateException( "There weren't enough links to bother making any stubs." );
        }

        Map<NonPersistentNonOrderedCoexpLink, SupportDetails> linksSoFar = new HashMap<>();
        log.info( "Creating stub links for up to " + genes.size() + " genes; " + skipGenes.size()
                + " genes will be ignored because they have too few links." );

        int numGenes = 0;
        int count = 0;
        for ( Gene gene : genes ) {
            Map<SupportDetails, Gene2GeneCoexpression> links = gene2GeneCoexpressionService.initializeLinksFromOldData(
                    gene, idMap, linksSoFar, skipGenes );
            if ( links == null || links.isEmpty() ) continue;

            count += links.size();

            /*
             * Keep track of links created so far (ignoring "direction") so we can resuse the supportDetails.
             */
            for ( SupportDetails sd : links.keySet() ) {
                assert sd.getId() != null;
                Gene2GeneCoexpression g2g = links.get( sd );
                assert g2g.getId() != null;
                assert g2g.getSupportDetails() != null && g2g.getSupportDetails().getId() != null;
                assert sd.equals( g2g.getSupportDetails() );

                NonPersistentNonOrderedCoexpLink linkVO = new NonPersistentNonOrderedCoexpLink( g2g.getFirstGene(),
                        g2g.getSecondGene(), g2g.isPositiveCorrelation() );
                if ( linksSoFar.containsKey( linkVO ) ) {
                    // if this happens we can actually remove it from linksSoFar as it means we have now persisted both
                    // directions. Removing it will help us free up memory.
                    assert sd.equals( linksSoFar.get( linkVO ) );
                    linksSoFar.remove( linkVO );
                } else {
                    linksSoFar.put( linkVO, sd );
                }
            }

            log.info( links.size() + " links created for " + gene + ", " + count + " links created so far." );

            if ( ++numGenes % 500 == 0 ) {
                log.info( "***** " + numGenes + " processed" );
            }

        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisPersister#saveLinks(ubic.gemma.analysis.expression
     * .coexpression.links.LinkAnalysis)
     */
    @Override
    public void saveLinksToDb( LinkAnalysis la ) {

        if ( !la.getConfig().isUseDb() ) {
            throw new IllegalArgumentException( "Analysis is not configured to use the db to persist" );
        }
        deleteAnalyses( la.getExpressionExperiment() );

        // the analysis object will get updated.
        CoexpressionAnalysis analysisObj = la.getAnalysisObj();
        analysisObj.setCoexpCorrelationDistribution( la.getCorrelationDistribution() );
        analysisObj = ( CoexpressionAnalysis ) persisterHelper.persist( analysisObj );

        /*
         * At this point we have the populated analysis object, but no links.
         */

        la.setAnalysisObj( analysisObj );

        StopWatch watch = new StopWatch();
        watch.start();

        ObjectArrayList links = la.getKeep();

        int numSaved = saveLinks( la, links );
        log.info( "Seconds to process " + numSaved + " links (plus flipped versions):" + watch.getTime() / 1000.0 );

        watch.stop();

    }

    /**
     * @param la
     * @return
     */
    private LinkCreator getLinkCreator( LinkAnalysis la ) {
        Taxon taxon = la.getTaxon();
        LinkCreator c;
        c = new LinkCreator( taxon );
        return c;
    }

    /**
     * @param w
     * @param c helper class
     * @param v1
     * @param v2
     * @return entity ready for saving to the database (or updating equivalent existing link)
     */
    private Gene2GeneCoexpression initCoexp( double w, LinkCreator c, Gene v1, Gene v2 ) {
        Gene2GeneCoexpression ppCoexpression = c.create( w, v1.getId(), v2.getId() );

        return ppCoexpression;
    }

    /**
     * @param la
     * @param links
     * @return how many links were saved
     */
    private int saveLinks( LinkAnalysis la, ObjectArrayList links ) {

        LinkCreator c = getLinkCreator( la );

        int selfLinksSkipped = 0;
        int duplicateLinksSkipped = 0;

        Set<Gene> genesWithLinks = new HashSet<>();
        Set<NonPersistentNonOrderedCoexpLink> linksForDb = new HashSet<>();
        for ( int i = 0, n = links.size(); i < n; i++ ) {

            Object val = links.getQuick( i );
            if ( val == null ) continue;
            Link m = ( Link ) val;
            Double w = m.getWeight();

            assert w != null;

            int x = m.getx();
            int y = m.gety();

            CompositeSequence p1 = la.getProbe( x );
            CompositeSequence p2 = la.getProbe( y );

            /*
             * we have to deal with all the possible genes pairs, if probes map to more than one pair. A single pair of
             * probes could result in more than one link. This assumes that preprocessing of the data allowed retention
             * of probes that map to more than one gene.
             */
            for ( Gene g1 : la.getProbeToGeneMap().get( p1 ) ) {
                boolean g1HasLinks = false;
                for ( Gene g2 : la.getProbeToGeneMap().get( p2 ) ) {
                    if ( g1.equals( g2 ) ) {
                        selfLinksSkipped++;
                        continue;
                    }

                    NonPersistentNonOrderedCoexpLink link = new NonPersistentNonOrderedCoexpLink( initCoexp( w, c, g1,
                            g2 ) );
                    if ( linksForDb.contains( link ) ) {
                        /*
                         * This happens if there is more than one probe retained for a gene (or both genes) and the
                         * coexpression shows up more than once (different pair of probes, same genes).
                         */
                        if ( log.isDebugEnabled() ) log.debug( "Skipping duplicate: " + link );
                        duplicateLinksSkipped++;
                        continue;

                        /*
                         * TODO what do we do when a pair of genes is both positively and negatively correlated in the
                         * same experiment? Currently they are both kept, but if we go to a completely gene-based
                         * analysis we wouldn't do that, so it's an inconsistency;
                         */
                    }

                    if ( log.isDebugEnabled() ) {
                        log.debug( "Adding : " + link );
                    }

                    linksForDb.add( link );

                    g1HasLinks = true;
                    genesWithLinks.add( g2 );
                }
                if ( g1HasLinks ) genesWithLinks.add( g1 );
            }

            if ( i > 0 && i % 50000 == 0 ) {
                log.info( i + " links checked" );
            }

        }

        if ( selfLinksSkipped > 0 ) {
            log.info( selfLinksSkipped + " self-links skipped" );
        }
        if ( duplicateLinksSkipped > 0 ) {
            log.info( duplicateLinksSkipped
                    + " duplicate links skipped (likely cause: more than one probe supporting the same link)" );
        }

        if ( linksForDb.isEmpty() ) {
            throw new RuntimeException( "No links left!" );
        }

        log.info( linksForDb.size() + " links ready for saving to db" );
        assert la.getGenesTested().containsAll( genesWithLinks );

        /*
         * Do the actual database writing. It's a good idea to do this part in one (big) transaction. Note that even if
         * there are no links, we still update the "genes tested" information.
         */
        this.gene2GeneCoexpressionService.createOrUpdate( la.getExpressionExperiment(), new ArrayList<>( linksForDb ),
                c, la.getGenesTested() );

        /*
         * Update the meta-data about the analysis
         */
        CoexpressionAnalysis analysisObj = la.getAnalysisObj();
        assert analysisObj.getId() != null;
        analysisObj.setNumberOfElementsAnalyzed( la.getGenesTested().size() );
        analysisObj.setNumberOfLinks( linksForDb.size() );
        coexpressionAnalysisService.update( analysisObj );

        return linksForDb.size();
        /*
         * Updating node degree cannot be done here, since we need to know the support. We have to do that
         * "periodically" if we want it available in summary form.
         */
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.analysis.expression.coexpression.links.LinkAnalysisPersister#deleteAnalyses(ubic.gemma.model.expression
     * .experiment.ExpressionExperiment)
     */
    @Override
    public void deleteAnalyses( BioAssaySet ee ) {
        Collection<CoexpressionAnalysis> oldAnalyses = coexpressionAnalysisService.findByInvestigation( ee );

        if ( !oldAnalyses.isEmpty() )
            log.info( "Deleting old coexpression analysis, link data and 'genes tested-in' for " + ee );

        for ( CoexpressionAnalysis old : oldAnalyses ) {
            coexpressionAnalysisService.delete( old );
        }

    }

}
