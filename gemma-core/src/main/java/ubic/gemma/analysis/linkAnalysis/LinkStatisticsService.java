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
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

import com.ibm.icu.text.NumberFormat;

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

    private static Log log = LogFactory.getLog( LinkStatisticsService.class.getName() );

    private GeneService geneService = null;

    private Probe2ProbeCoexpressionService p2pService = null;

    private ExpressionExperimentService eeService = null;

    /**
     * @param ees ExpressionExperiments to use
     * @param genes Genes to consider
     * @param shuffle Should the links in each database be shuffled (to get background statistics)
     */
    public LinkStatistics analyze( Collection<ExpressionExperiment> ees, Collection<Gene> genes, String taxonName,
            boolean shuffle ) {
        LinkStatistics stats = new LinkStatistics( ees, genes );
        // this.countGeneLinks( stats, ees, shuffle, taxonName );
        this.countProbeLinks( stats, ees, shuffle, taxonName );
        return stats;
    }

    /**
     * @param out
     * @param realStats
     * @param shuffleStats
     */
    public void writeStats( Writer out, LinkConfirmationStatistics realStats,
            Collection<LinkConfirmationStatistics> shuffleStats ) {
        try {
            NumberFormat nf = NumberFormat.getInstance();
            nf.setMaximumFractionDigits( 3 );
            // Determine the largest number of data sets any link was seen in, in the 'real' data.
            int maxSupport = realStats.getMaxLinkSupport();
            // Header
            out.write( "Support" );
            for ( int j = 1; j <= maxSupport; j++ )
                out.write( "\t" + j + "+" );
            out.write( "\n" );

            // True links
            out.write( "RealLinks" );
            for ( int j = 1; j <= maxSupport; j++ ) {
                out.write( "\t" + realStats.getCumulativeRepCount( j ) );
            }
            out.write( "\n" );

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
        out.write( "ShuffleMean" );
        double[] falsePositiveRates = new double[maxSupport + 1];
        for ( LinkConfirmationStatistics shuffleStat : shuffleStats ) {

            for ( int j = 1; j <= maxSupport; j++ ) {
                if ( realStats.getCumulativeRepCount( j ) != 0 ) {
                    falsePositiveRates[j] = falsePositiveRates[j] + ( double ) shuffleStat.getCumulativeRepCount( j )
                            / ( double ) realStats.getCumulativeRepCount( j );
                }
            }
        }
        for ( int j = 1; j < maxSupport; j++ ) {
            out.write( "\t" + nf.format( falsePositiveRates[j] / shuffleStats.size() ) );
        }
        out.write( "\n" );

        // FP rates for each run.
        int i = 1;
        for ( LinkConfirmationStatistics shuffleStat : shuffleStats ) {
            out.write( "ShuffleRun " + i );
            for ( int j = 1; j <= maxSupport; j++ ) {
                out.write( "\t" );
                if ( realStats.getCumulativeRepCount( j ) != 0 ) {
                    out.write( nf.format( ( double ) shuffleStat.getCumulativeRepCount( j )
                            / ( double ) realStats.getCumulativeRepCount( j ) ) );
                }
            }
            ++i;
            out.write( "\n" );
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
            Long tmpId = links.get( k ).getSecondDesignElementId();
            links.get( i ).setSecondDesignElementId( links.get( k ).getSecondDesignElementId() );
            links.get( k ).setSecondDesignElementId( tmpId );
        }
    }

    /**
     * Do shuffling at gene level. We simply shuffle the identify of the second gene. This guarantees that links per
     * gene is constant under shuffling.
     * 
     * @param links
     */
    private void shuffleGeneLinks( List<GeneLink> links ) {
        Random random = new Random();
        int i = links.size();
        while ( --i > 0 ) {
            int k = random.nextInt( i + 1 );
            Long tmpId = links.get( k ).getSecondGene();
            links.get( i ).setSecondGene( links.get( k ).getSecondGene() );
            links.get( k ).setSecondGene( tmpId );
        }
    }

    /**
     * Given probe links, convert to gene links.
     * 
     * @param links
     * @return collection of GeneLink objects.
     */
    private Collection<GeneLink> getGeneLinks( Collection<ProbeLink> links ) {
        Collection<GeneLink> result = new HashSet<GeneLink>();
        Map<Long, Collection<Long>> cs2genes = getCS2GeneMap( links );
        for ( ProbeLink link : links ) {
            Collection<Long> firstGeneIds = cs2genes.get( link.getFirstDesignElementId() );
            Collection<Long> secondGeneIds = cs2genes.get( link.getSecondDesignElementId() );
            if ( firstGeneIds == null || secondGeneIds == null ) {
                log.info( " Preparation is not correct (get null genes) for:  " + link.getFirstDesignElementId() + ","
                        + link.getSecondDesignElementId() );
                continue;
            }

            for ( Long firstGeneId : firstGeneIds ) {
                for ( Long secondGeneId : secondGeneIds ) {
                    result.add( new GeneLink( firstGeneId, secondGeneId, link.getScore() ) );
                }
            }
        }

        return result;
    }

    /**
     * @param stats object to hold the results
     * @param ees ExpressionExperiments to analyze.
     * @param shuffle if true, the links are shuffled before being tabulated
     * @param taxonName
     */
    @SuppressWarnings("unchecked")
    private void countGeneLinks( LinkStatistics stats, Collection<ExpressionExperiment> ees, boolean shuffle,
            String taxonName ) {
        for ( ExpressionExperiment ee : ees ) {
            assert ee != null;
            log.info( "Loading links for  " + ee.getShortName() );

            // FIXME if not shuffling, don't use the working table, so we can 'get on with it' without worrying about
            // creating that table first.
            Collection<ProbeLink> links = p2pService.getProbeCoExpression( ee, taxonName, true );
            Collection<GeneLink> geneLinks = getGeneLinks( links );

            if ( links == null || links.size() == 0 ) continue;
            if ( shuffle ) {
                log.info( "Shuffling links for  " + ee.getShortName() );
                shuffleGeneLinks( new ArrayList<GeneLink>( geneLinks ) );
            }
            stats.addLinks( geneLinks, ee );
        }
    }

    /**
     * @param stats object to hold the results
     * @param ees ExpressionExperiments to analyze.
     * @param shuffle if true, the links are shuffled before being tabulated
     * @param taxonName
     */
    @SuppressWarnings("unchecked")
    private void countProbeLinks( LinkStatistics stats, Collection<ExpressionExperiment> ees, boolean shuffle,
            String taxonName ) {
        for ( ExpressionExperiment ee : ees ) {
            assert ee != null;
            log.info( "Loading links for  " + ee.getShortName() );

            // FIXME if not shuffling, don't use the working table, so we can 'get on with it' without worrying about
            // creating that table first.
            Collection<ProbeLink> links = p2pService.getProbeCoExpression( ee, taxonName, true );

            // FIXME 0.3 is the threshold for genes that are expressed used in link analysis. The actual filtering might
            // be more complex (remove invariant genes etc.) But this should be close. The paramter 0.3 really should be
            // determined programatically, as it might have been set during the analysis.

            Collection<Gene> genesAssayed = eeService.getAssayedGenes( ee, 0.3 );

            if ( links == null || links.size() == 0 ) continue;
            if ( shuffle ) {
                log.info( "Shuffling links for  " + ee.getShortName() );
                shuffleProbeLinks( new ArrayList<ProbeLink>( links ) );
            }

            stats.addLinks( getGeneLinks( links ), ee );
        }
    }

    /**
     * This must be run just the first time you want to analyze some data sets.
     * 
     * @param candidates
     * @param taxonName to figure out which table to get the links from (FIXME should not be needed)
     */
    public void prepareDatabase( Collection<ExpressionExperiment> ees, String taxonName ) {
        log.info( " Creating intermediate table for link analysis" );
        StopWatch watch = new StopWatch();
        watch.start();
        p2pService.prepareForShuffling( ees, taxonName );
        watch.stop();
        log.info( " Spent " + watch.getTime() / 1000 + "s preparing the database" );
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Collection<Long>> getCS2GeneMap( Collection<ProbeLink> links ) {
        Set<Long> csIds = new HashSet<Long>();
        for ( ProbeLink link : links ) {
            csIds.add( link.getFirstDesignElementId() );
            csIds.add( link.getSecondDesignElementId() );
        }
        Map<Long, Collection<Long>> cs2genes = geneService.getCS2GeneMap( csIds );
        return cs2genes;
    }

    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    public void setP2pService( Probe2ProbeCoexpressionService service ) {
        p2pService = service;
    }

    public void setEeService( ExpressionExperimentService eeService ) {
        this.eeService = eeService;
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

    public Long getFirstGene() {
        return firstGene;
    }

    public Long getSecondGene() {
        return secondGene;
    }

    @Override
    public boolean equals( Object obj ) {
        GeneLink g = ( GeneLink ) obj;
        return g.getFirstGene().equals( this.firstGene ) && g.getSecondGene().equals( this.secondGene )
                && Math.signum( this.score ) == Math.signum( g.getScore() );
    }

    @Override
    public int hashCode() {
        return 29 * ( int ) Math.signum( this.score ) * this.firstGene.hashCode() + this.secondGene.hashCode();
    }

    public void setFirstGene( Long firstGene ) {
        this.firstGene = firstGene;
    }

    public void setSecondGene( Long secondGene ) {
        this.secondGene = secondGene;
    }

    public Double getScore() {
        return score;
    }

    public void setScore( Double score ) {
        this.score = score;
    }

}
