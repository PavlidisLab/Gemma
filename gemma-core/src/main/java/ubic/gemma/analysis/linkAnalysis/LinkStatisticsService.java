/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.analysis.linkAnalysis;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionService;
import ubic.gemma.model.association.coexpression.Probe2ProbeCoexpressionDaoImpl.ProbeLink;
import ubic.gemma.model.coexpression.Link;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

import com.ibm.icu.text.NumberFormat;
import com.sdicons.json.validator.impl.predicates.Array;

/**
 * Methods for analyzing links from the database.
 * 
 * @author paul
 * @author xwan
 * @version $Id$
 * @spring.bean id="linkStatisticsService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="p2pService" ref="probe2ProbeCoexpressionService"
 * @spring.property name="eeService" ref="expressionExperimentService"
 */
public class LinkStatisticsService {

    /**
     * FIXME! this shouldn't be hardcoded.
     */
    private static final double EXPRESSION_RANK_THRESHOLD = 0.0;

    private static Log log = LogFactory.getLog( LinkStatisticsService.class.getName() );

    private GeneService geneService = null;

    private Probe2ProbeCoexpressionService p2pService = null;

    private ExpressionExperimentService eeService = null;

    /**
     * @param ees ExpressionExperiments to use
     * @param genes Genes to consider
     * @param shuffle Should the links in each database be shuffled (to get background statistics)
     * @param filterNonSpecific links which involve probes that hit more than one gene will be removed if this is true.
     * @return LinkStatistic object holding the results.
     */
    public LinkStatistics analyze( Collection<ExpressionExperiment> ees, Collection<Gene> genes, String taxonName,
            boolean shuffle, boolean filterNonSpecific ) {
        LinkStatistics stats = new LinkStatistics( ees, genes );
        int numLinks = this.countLinks( stats, ees, shuffle, filterNonSpecific, taxonName );
        log.info( numLinks + " gene links in total for " + ees.size() + " expression experiments " );
        return stats;
    }

    /**
     * This must be run just the first time you want to analyze some data sets.
     * 
     * @param candidates
     * @param taxonName to figure out which table to get the links from (FIXME should not be needed)
     */
    public void prepareDatabase( Collection<ExpressionExperiment> ees, String taxonName, boolean filterNonSpecific ) {
        log.info( "Creating working table for link analysis" );
        StopWatch watch = new StopWatch();
        watch.start();
        p2pService.prepareForShuffling( ees, taxonName, filterNonSpecific );
        watch.stop();
        log.info( "Done, spent " + watch.getTime() / 1000 + "s preparing the database" );
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setP2pService( Probe2ProbeCoexpressionService service ) {
        p2pService = service;
    }

    /**
     * @param out
     * @param realStats
     * @param shuffleStats
     */
    public void writeStats( Writer out, LinkConfirmationStatistics realStats,
            Collection<LinkConfirmationStatistics> shuffleStats ) {

        // Determine the largest number of data sets any link was seen in.
        int maxSupport = -1;
        if ( realStats != null ) {
            maxSupport = realStats.getMaxLinkSupport();
        } else {
            for ( LinkConfirmationStatistics statistics : shuffleStats ) {
                int s = statistics.getMaxLinkSupport();
                if ( s > maxSupport ) maxSupport = s;
            }
        }

        try {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits( 3 );

            // Header
            out.write( "Support" );
            for ( int j = 1; j <= maxSupport; j++ )
                out.write( "\t" + j /* + "+" */);
            out.write( "\n" );

            // True links
            if ( realStats != null ) {
                out.write( "RealLinks" );
                for ( int j = 1; j <= maxSupport; j++ ) {
                    out.write( "\t" + realStats.getRepCount( j ) );
                }
                out.write( "\n" );
            }

            if ( shuffleStats != null && shuffleStats.size() > 0 ) {
                // number of shuffled links / # of real links
                writeShuffleStats( out, realStats, shuffleStats, maxSupport );
            }
            out.close();
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param stats object to hold the results
     * @param ees ExpressionExperiments to analyze.
     * @param shuffle if true, the links are shuffled before being tabulated
     * @param filterNonSpecific
     * @param taxonName common name e.g. mouse
     */
    private int countLinks( LinkStatistics stats, Collection<ExpressionExperiment> ees, boolean shuffle,
            boolean filterNonSpecific, String taxonName ) {
        int totalLinks = 0;
        for ( ExpressionExperiment ee : ees ) {
            totalLinks += countLinks( stats, shuffle, filterNonSpecific, taxonName, ee );
        }
        return totalLinks;
    }

    /**
     * @param stats
     * @param shuffle
     * @param taxonName
     * @param ee
     */
    @SuppressWarnings("unchecked")
    private int countLinks( LinkStatistics stats, boolean shuffle, boolean filterNonSpecific, String taxonName,
            ExpressionExperiment ee ) {
        assert ee != null;
        log.info( "Loading links for  " + ee.getShortName() );

        // FIXME if not shuffling, don't use the working table, so we can 'get on with it' without worrying about
        // creating that table first.
        Collection<ProbeLink> links = p2pService.getProbeCoExpression( ee, taxonName, true ); // FIXME pass in
        // filterNonSpecific.

        Collection<ProbeLink> filteredLinks = filterLinks( stats, links, filterNonSpecific );

        if ( filteredLinks == null || filteredLinks.size() == 0 ) return 0;

        Collection<GeneLink> geneLinks = null;
        if ( shuffle ) {
            /*
             * We start with a list of all the probes for genes that are in our matrix and which are expressed.
             */
            Map<Long, Collection<Long>> assayedProbes = getRelevantProbeIds( ee, stats, filterNonSpecific );

            /* for shuffling at the probe level */
            // shuffleProbeLinks( stats, ee, filteredLinks, assayedProbes );
            // geneLinks = getGeneLinks( filteredLinks, stats );
            /* shuffle at gene level */
            geneLinks = shuffleGeneLinks( stats, filteredLinks, assayedProbes );

        } else {
            geneLinks = getGeneLinks( filteredLinks, stats );
        }
        return stats.addLinks( geneLinks, ee );
    }

    /**
     * @param stats
     * @param filteredLinks
     * @param probeGeneMap
     * @return
     */
    private Collection<GeneLink> shuffleGeneLinks( LinkStatistics stats, Collection<ProbeLink> filteredLinks,
            Map<Long, Collection<Long>> probeGeneMap ) {
        Collection<GeneLink> geneLinks;
        Collection<Long> geneIds = new HashSet<Long>();
        for ( Long probeId : probeGeneMap.keySet() ) {
            geneIds.addAll( probeGeneMap.get( probeId ) );
        }

        List<Long> geneIdList = new ArrayList<Long>( geneIds );
        List<Long> geneIdListForShuffle = new ArrayList<Long>();
        for ( Long id : geneIdList ) {
            geneIdListForShuffle.add( id );
        }

        // shuffle the genes.
        shuffleList( geneIdListForShuffle );
        shuffleList( geneIdListForShuffle );
        shuffleList( geneIdListForShuffle );
        log.debug( geneIdList.size() + " genes to shuffle" );
        Map<Long, Long> shuffleMap = new HashMap<Long, Long>();
        for ( int i = 0, j = geneIdList.size(); i < j; i++ ) {
            shuffleMap.put( geneIdList.get( i ), geneIdListForShuffle.get( i ) );
        }

        geneLinks = getGeneLinks( filteredLinks, stats );
        log.info( geneLinks.size() + " gene links to shuffle" );

        Set<Long> usedGeneList = new HashSet<Long>();
        for ( GeneLink gl : geneLinks ) {
            if ( !shuffleMap.containsKey( gl.getFirstGene() ) ) {
                throw new IllegalStateException();
            }
            if ( !shuffleMap.containsKey( gl.getSecondGene() ) ) {
                throw new IllegalStateException();
            }

            usedGeneList.add( gl.getFirstGene() );
            usedGeneList.add( gl.getSecondGene() );

            gl.setFirstGene( shuffleMap.get( gl.getFirstGene() ) );
            gl.setSecondGene( shuffleMap.get( gl.getSecondGene() ) );
        }

        log.info( "Gene links used a total of " + usedGeneList.size() + " genes, we shuffled using "
                + geneIdList.size() + " available genes" );

        // usedGeneList.clear();
        // for ( GeneLink gl : geneLinks ) {
        //
        // usedGeneList.add( gl.getFirstGene() );
        // usedGeneList.add( gl.getSecondGene() );
        // }
        // log.info( "After shuffling, Gene links used a total of " + usedGeneList.size() );
        // log.info( geneLinks.size() + " links after first phase of shuffling" );

        // shuffleGeneLinks( new ArrayList<GeneLink>( geneLinks ) );
        log.debug( geneLinks.size() + " links after shuffling" );
        return geneLinks;
    }

    /**
     * Given links, shuffle the probes they are between. The way this is done is designed to reflect the null
     * distribution of link selection in the data set, maintaining the same number of links per probe.
     * 
     * @param stats
     * @param ee
     * @param probeUniverse map of probe ids to gene ids, including only probes and genes to consider.
     * @param linksToShuffle
     * @return
     */
    void shuffleProbeLinks( LinkStatistics stats, ExpressionExperiment ee, Collection<ProbeLink> linksToShuffle,
            Map<Long, Collection<Long>> probeUniverse ) {
        log.info( "Shuffling links for " + ee.getShortName() );

        /*
         * Make a copy so we can make a shuffled mapping
         */
        List<Long> assayedProbeIdsforShuffle = new ArrayList<Long>();
        List<Long> assayedProbeIds = new ArrayList<Long>( probeUniverse.keySet() );
        for ( Long id : assayedProbeIds ) {
            assayedProbeIdsforShuffle.add( id );
        }

        /*
         * Shuffle the probes and create map. We exchange old probes for random new ones.
         */
        shuffleList( assayedProbeIdsforShuffle );
        Map<Long, Long> shuffleMap = new HashMap<Long, Long>();
        for ( int i = 0; i < assayedProbeIdsforShuffle.size(); i++ ) {
            Long old = assayedProbeIds.get( i );
            Long shuffled = assayedProbeIdsforShuffle.get( i );
            // log.info( old + " --> " + shuffled );
            shuffleMap.put( old, shuffled );
        }

        // Now replace the in the probes in the links with the shuffled ones, from the (potentially) expanded list.. Now
        // the links are among random probes, but number per probe is the same.
        for ( ProbeLink p : linksToShuffle ) {

            // Sanity checks.
            if ( !shuffleMap.containsKey( p.getFirstDesignElementId() ) ) {
                throw new IllegalStateException( "Probe " + p.getFirstDesignElementId()
                        + " was used in the links but doesn't show up in the 'assayedProbes'" );
            }
            if ( !shuffleMap.containsKey( p.getSecondDesignElementId() ) ) {
                throw new IllegalStateException( "Probe " + p.getSecondDesignElementId()
                        + " was used in the links but doesn't show up in the 'assayedProbes'" );
            }

            // Replace with the shuffled replacement.
            p.setFirstDesignElementId( shuffleMap.get( p.getFirstDesignElementId() ) );
            p.setSecondDesignElementId( shuffleMap.get( p.getSecondDesignElementId() ) );
        }

        // this step might be redundant.
        shuffleProbeLinks( new ArrayList<ProbeLink>( linksToShuffle ) );

    }

    /**
     * Limit the links to those for genes that were included in the initial setup. Typically this means that links
     * between PARs or PARs and Genes can be removed. This step is important when shuffling, so that the probes used for
     * shuffling match those used in the real links.
     * 
     * @param stats
     * @param links
     * @param filterNonSpecific
     * @return filtered collection of links.
     */
    private Collection<ProbeLink> filterLinks( LinkStatistics stats, Collection<ProbeLink> links,
            boolean filterNonSpecific ) {
        Map<Long, Collection<Long>> cs2genes = getCS2GeneMap( links );
        Collection<Long> probeIdsToKeepLinksFor = filterProbes( stats, cs2genes, filterNonSpecific );

        Collection<ProbeLink> filteredLinks = new HashSet<ProbeLink>();
        for ( ProbeLink pl : links ) {
            if ( probeIdsToKeepLinksFor.contains( pl.getFirstDesignElementId() )
                    && probeIdsToKeepLinksFor.contains( pl.getSecondDesignElementId() ) ) {
                filteredLinks.add( pl );
            }
        }

        log.info( filteredLinks.size() + "/" + links.size()
                + " links retained after removing links for excluded genes." );
        return filteredLinks;
    }

    /**
     * Filter probes to include just those used in the matrix.
     * 
     * @param stats
     * @param cs2genes map of probes -> genes for probes you want to filter.
     * @param filterNonSpecific.
     * @return Collection of probes
     */
    private Collection<Long> filterProbes( LinkStatistics stats, Map<Long, Collection<Long>> cs2genes,
            boolean filterNonSpecific ) {
        Collection<Long> matrixGenes = stats.getGeneIds();

        log.info( "Considering up to " + matrixGenes.size() + " genes, up to " + cs2genes.keySet().size() + " probes" );

        // when probe is for multiple genes, we keep even if only one of the genes is on our query list.
        Collection<Long> probeIdsToKeepLinksFor = new HashSet<Long>();
        for ( Long probeId : cs2genes.keySet() ) {
            Collection<Long> genesForProbesInLinks = cs2genes.get( probeId );

            /*
             * Important if we dont' filter out non-specific probes here, we should not filter them out when we first
             * fetch links.
             */
            if ( filterNonSpecific && genesForProbesInLinks.size() > 1 ) continue;

            for ( Long geneForLink : genesForProbesInLinks ) {
                if ( matrixGenes.contains( geneForLink ) ) {
                    probeIdsToKeepLinksFor.add( probeId );
                    break; // we know we have to keep this probe.
                }
            }
        }
        log.info( "Kept " + probeIdsToKeepLinksFor.size() + "/" + cs2genes.keySet().size() + " probes" );
        return probeIdsToKeepLinksFor;
    }

    /**
     * @param links
     * @return map of cs to genes, by primary key.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Collection<Long>> getCS2GeneMap( Collection<ProbeLink> links ) {
        log.info( "Getting CS -> Gene map" );
        Set<Long> csIds = new HashSet<Long>();
        for ( ProbeLink link : links ) {
            csIds.add( link.getFirstDesignElementId() );
            csIds.add( link.getSecondDesignElementId() );
        }
        return geneService.getCS2GeneMap( csIds );
    }

    /**
     * Given probe links, convert to gene links.
     * 
     * @param links
     * @return collection of GeneLink objects.
     */
    private Collection<GeneLink> getGeneLinks( Collection<ProbeLink> links, LinkStatistics stats ) {
        log.info( "Converting " + links.size() + " probe links to gene links ..." );
        Collection<GeneLink> result = new HashSet<GeneLink>();
        Map<Long, Collection<Long>> cs2genes = getCS2GeneMap( links );

        /*
         * Once again, ignore links that don't show up in the original query
         */
        Collection<Long> geneIds = stats.getGeneIds();

        int missing = 0;
        int count = 0;
        for ( ProbeLink link : links ) {
            Collection<Long> firstGeneIds = cs2genes.get( link.getFirstDesignElementId() );
            Collection<Long> secondGeneIds = cs2genes.get( link.getSecondDesignElementId() );
            if ( firstGeneIds == null || secondGeneIds == null ) {
                ++missing;
                if ( log.isDebugEnabled() )
                    log.debug( "No gene found for one/both of CS:  " + link.getFirstDesignElementId() + ","
                            + link.getSecondDesignElementId() );
                continue;
            }

            for ( Long firstGeneId : firstGeneIds ) {
                if ( !geneIds.contains( firstGeneId ) ) continue;
                for ( Long secondGeneId : secondGeneIds ) {
                    if ( !geneIds.contains( secondGeneId ) ) continue;
                    result.add( new GeneLink( firstGeneId, secondGeneId, link.getScore() ) );
                }
            }
            if ( ++count % 5e5 == 0 ) {
                log.info( count + " links converted" );
            }
        }

        if ( missing > 0 ) {
            log.warn( missing + " links had probes with no genes" );
        }

        log.info( links.size() + " probe links --> " + result.size() + " gene links" );

        return result;
    }

    /**
     * Used for shuffling, to choose the universe of probes to consider 'random links' among. This must be chosen
     * carefully to get an accurate null distribution.
     * 
     * @param ee
     * @param stats
     * @param filterNonSpecific
     * @return map of probes to genes which are 1) assayed and 2) have gene mappings (that is, alignments to the genome;
     *         this includes non-refseq genes etc.) and 3) used as inputs for the 'real' analysis we are comparing to.
     */
    @SuppressWarnings("unchecked")
    private Map<Long, Collection<Long>> getRelevantProbeIds( ExpressionExperiment ee, LinkStatistics stats,
            boolean filterNonSpecific ) {
        /*
         * FIXME EXPRESSION_RANK_THRESHOLD is the threshold for genes that are expressed used in link analysis. The
         * actual filtering might be more complex (remove invariant genes etc.) But this should be close. The parameter
         * 0.3 really should be determined programatically, as it might have been set during the analysis. The probes
         * that are returned are ones that map to all, not just known genes.
         */
        Collection<CompositeSequence> probesAssayed = eeService.getAssayedProbes( ee, EXPRESSION_RANK_THRESHOLD );

        List<Long> assayedProbeIds = new ArrayList<Long>();
        for ( CompositeSequence cs : probesAssayed ) {
            assayedProbeIds.add( cs.getId() );
        }
        log.info( assayedProbeIds.size() + " probes assayed " );

        /*
         * Further filter the list to only include probes that have alignments. This is a map of CS to genes (this is
         * probably not strictly needed as the next step also looks at genes.)
         */
        Map<Long, Collection<Long>> geneMap = geneService.getCS2GeneMap( assayedProbeIds );
        log.info( geneMap.size() + " probes with alignments" );

        /*
         * Further filter to include only probes that are also for genes we used as potential inputs.
         */
        Collection<Long> probesToKeep = filterProbes( stats, geneMap, filterNonSpecific );

        log.info( probesToKeep.size() + " probes after filtering." );

        // return map so we can still have access to the genes. Remove all 'extraneous' genes.
        Collection<Long> geneIdsToKeep = stats.getGeneIds();
        Map<Long, Collection<Long>> finalGeneMap = new HashMap<Long, Collection<Long>>();
        for ( Long p : probesToKeep ) {
            Collection<Long> geneIdsToClean = geneMap.get( p );
            geneIdsToClean.retainAll( geneIdsToKeep );
            finalGeneMap.put( p, geneIdsToClean );
        }

        // return new ArrayList<Long>( probesToKeep );
        return finalGeneMap;
    }

    @SuppressWarnings("unchecked")
    private void shuffleList( List list ) {
        Random random = new Random();
        int i = list.size();
        while ( --i > 0 ) {
            int k = random.nextInt( i + 1 );
            Object tmp = list.get( i );
            list.set( i, list.get( k ) );
            list.set( k, tmp );
        }
    }

    /**
     * Do shuffling at probe level
     * 
     * @param links
     */
    private void shuffleProbeLinks( List<ProbeLink> links ) {
        Random random = new Random();
        int i = links.size();
        while ( --i > 0 ) {
            int k = random.nextInt( i + 1 );
            Long tmpId = links.get( i ).getSecondDesignElementId();
            links.get( i ).setSecondDesignElementId( links.get( k ).getSecondDesignElementId() );
            links.get( k ).setSecondDesignElementId( tmpId );
        }
    }

    /**
     * Do shuffling at gene level
     * 
     * @param links
     */
    private void shuffleGeneLinks( List<GeneLink> links ) {
        Random random = new Random();
        int i = links.size();
        while ( --i > 0 ) {
            int k = random.nextInt( i + 1 );
            Long tmpId = links.get( i ).getSecondGene();
            links.get( i ).setSecondGene( links.get( k ).getSecondGene() );
            links.get( k ).setSecondGene( tmpId );
        }
    }

    /**
     * @param out
     * @param realStats
     * @param shuffleStats
     * @param maxSupport
     * @throws IOException
     */
    private void writeShuffleStats( Writer out, LinkConfirmationStatistics realStats,
            Collection<LinkConfirmationStatistics> shuffleStats, int maxSupport ) throws IOException {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits( 3 );

        if ( realStats != null ) {
            out.write( "ShuffleMean" );
            double[] falsePositiveRates = new double[maxSupport + 1];
            for ( LinkConfirmationStatistics shuffleStat : shuffleStats ) {

                for ( int j = 1; j <= maxSupport; j++ ) {
                    if ( realStats.getRepCount( j ) != 0 ) {
                        falsePositiveRates[j] = falsePositiveRates[j] + ( double ) shuffleStat.getRepCount( j )
                                / ( double ) realStats.getRepCount( j );
                    }
                }
            }
            for ( int j = 1; j <= maxSupport; j++ ) {
                out.write( "\t" + nf.format( falsePositiveRates[j] / shuffleStats.size() ) );
            }
            out.write( "\n" );
        }

        // FP rates for each run.
        int i = 1;
        for ( LinkConfirmationStatistics shuffleStat : shuffleStats ) {
            out.write( "ShuffleRun_" + i );
            for ( int j = 1; j <= maxSupport; j++ ) {
                out.write( "\t" + shuffleStat.getRepCount( j ) );
                // if ( realStats.getCumulativeRepCount( j ) != 0 ) {
                // out.write( nf.format( ( double ) shuffleStat.getCumulativeRepCount( j )
                // / ( double ) realStats.getCumulativeRepCount( j ) ) );
                // }
            }
            ++i;
            out.write( "\n" );
        }
    }
}

class GeneLink implements Link {
    Long firstGene;
    Long secondGene;
    Double score;

    public GeneLink( Long firstGeneId, Long secondGeneId, double score ) {
        this.firstGene = firstGeneId;
        this.secondGene = secondGeneId;
        this.score = score;
    }

    @Override
    public boolean equals( Object obj ) {
        GeneLink that = ( GeneLink ) obj;
        return that.getFirstGene().equals( this.firstGene ) && that.getSecondGene().equals( this.secondGene )
                && Math.signum( this.score ) == Math.signum( that.getScore() );
    }

    public Long getFirstGene() {
        return firstGene;
    }

    public Double getScore() {
        return score;
    }

    public Long getSecondGene() {
        return secondGene;
    }

    @Override
    public int hashCode() {
        return 29 * ( int ) Math.signum( this.score ) * this.firstGene.hashCode() + this.secondGene.hashCode();
    }

    public void setFirstGene( Long firstGene ) {
        this.firstGene = firstGene;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

    public void setSecondGene( Long secondGene ) {
        this.secondGene = secondGene;
    }

}
