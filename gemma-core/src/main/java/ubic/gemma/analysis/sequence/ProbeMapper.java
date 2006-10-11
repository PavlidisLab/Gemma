/*
 * The Gemma project
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
package ubic.gemma.analysis.sequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.apps.Blat;
import ubic.gemma.apps.Blat.BlattableGenome;
import ubic.gemma.externalDb.GoldenPath;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.ThreePrimeDistanceMethod;

/**
 * Provides methods for mapping sequences to genes and gene products.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapper {
    public static final double DEFAULT_IDENTITY_THRESHOLD = 0.90;
    public static final double DEFAULT_SCORE_THRESHOLD = 0.90;
    private Log log = LogFactory.getLog( ProbeMapper.class.getName() );
    private double identityThreshold = DEFAULT_IDENTITY_THRESHOLD;
    private double scoreThreshold = DEFAULT_SCORE_THRESHOLD;
    private double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;
    private ThreePrimeDistanceMethod threeprimeMethod = ThreePrimeDistanceMethod.RIGHT;

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankId
     * @throws IOException
     */
    public Map<String, Collection<BlatAssociation>> processGbId( GoldenPathSequenceAnalysis goldenPathDb,
            String genbankId ) {

        log.debug( "Entering processGbId with " + genbankId );

        Collection<BlatResult> blatResults = goldenPathDb.findSequenceLocations( genbankId );

        if ( blatResults == null || blatResults.size() == 0 ) {
            log.warn( "No results obtained for " + genbankId );
        }

        return processBlatResults( goldenPathDb, blatResults );

    }

    /**
     * From a collection of BlatAssociations, pick the one with the best scoring statistics and fill in the specificity
     * for all of them.
     * 
     * @param results
     * @return
     */
    public BlatAssociation selectBest( Collection<BlatAssociation> results ) {

        int maxScore = 0;
        BlatAssociation best = null;
        List<Double> scores = new ArrayList<Double>();
        for ( BlatAssociation blatAssociation : results ) {

            BlatResult br = blatAssociation.getBlatResult();

            double blatScore = br.score();
            double overlap = ( double ) blatAssociation.getOverlap() / ( double ) ( br.getQuerySequence().getLength() );
            int score = computeScore( blatScore, overlap );
            scores.add( new Double( score ) );
            if ( score >= maxScore ) {
                maxScore = score;
                best = blatAssociation;
            }
        }

        Collections.sort( scores );
        Collections.reverse( scores );

        // measure the specificity for all the scores (this could be made more efficient)
        for ( BlatAssociation ld : results ) {
            double blatScore = ld.getBlatResult().score();
            double overlap = ld.getOverlap() / ( double ) ( ld.getBlatResult().getQuerySequence().getLength() );
            int score = computeScore( blatScore, overlap );
            ld.setSpecificity( computeSpecificity( scores, score ) );
        }

        return best;
    }

    /**
     * Compute a score to quantify the specificity of a hit to a GeneProduct. A value between 0 and 1.
     * <p>
     * The criteria considered are: the number of equal or better hits, and the difference between this hit and the next
     * worst hit.
     * <p>
     * If there are n identical or better hits (including this one), the specificity is 1/n.
     * <p>
     * If this is the best hit, then the specificity is (scoremax - nextscore)/scoremax.
     * 
     * @param scores A list in decreasing order. If it is not sorted you won't get the right results!
     * @param score
     * @return
     */
    protected Double computeSpecificity( List<Double> scores, int score ) {

        if ( scores.size() == 1 ) {
            return 1.0;
        }

        // algorithm: compute the number of scores which are equal or higher than this one.
        int numBetter = 0;
        int i = 0;
        double nextBest = 0.0;
        double total = 0.0;
        for ( Double s : scores ) {

            total += s;

            if ( s >= score ) {
                numBetter++; // this is guaranteed to be at least one
            }
            if ( s < score ) {
                nextBest = s;
                break;
            }
            i++;
        }

        if ( numBetter > 1 ) {
            return 1.0 / numBetter;
        }

        return ( score - nextBest ) / score;

    }

    /**
     * Compute a score we use to quantify the quality of a hit to a GeneProduct.
     * <p>
     * There are two criteria being considered: the quality of the alignment, and the amount of overlap.
     * 
     * @param blatScore A value from 0-1 indicating alignment quality.
     * @param overlap A value from 0-1 indicating how much of the alignment overlaps the GeneProduct being considered.
     * @return
     */
    protected int computeScore( double blatScore, double overlap ) {
        return ( int ) ( 1000 * blatScore * overlap );
    }

    /**
     * Given a collection of sequences, blat them against the selected genome.
     * 
     * @param output
     * @param goldenpath for the genome to be used.
     * @param sequences
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processSequences( GoldenPathSequenceAnalysis goldenpath,
            Collection<BioSequence> sequences ) {
        Blat b = new Blat();
        b.setBlatScoreThreshold( blatScoreThreshold );
        BlattableGenome bg = inferBlatGenome( goldenpath );

        try {
            Map<String, Collection<BlatResult>> results = b.blatQuery( sequences, bg );
            Collection<BlatResult> blatres = new HashSet<BlatResult>();
            for ( Collection<BlatResult> coll : results.values() ) {
                blatres.addAll( coll );
            }
            Map<String, Collection<BlatAssociation>> allRes = processBlatResults( goldenpath, blatres );
            return allRes;
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * FIXME - this should not be hard coded like this, what happens when more genomes are added.
     * 
     * @param goldenpath
     * @return
     */
    private BlattableGenome inferBlatGenome( GoldenPath goldenpath ) {
        BlattableGenome bg = BlattableGenome.HUMAN;

        if ( goldenpath.getDatabaseName().startsWith( "mm" ) ) {
            bg = BlattableGenome.MOUSE;
        } else if ( goldenpath.getDatabaseName().startsWith( "hg" ) ) {
            bg = BlattableGenome.HUMAN;
        } else if ( goldenpath.getDatabaseName().startsWith( "rn" ) ) {
            bg = BlattableGenome.RAT;
        } else {
            throw new IllegalArgumentException( "Unsupported database for blatting " + goldenpath.getDatabaseName() );
        }
        return bg;
    }

    /**
     * Get BlatAssociation results for a single sequence. If you have multiple sequences to run it is always better to
     * use processSequences();
     * 
     * @param goldenPath
     * @param sequence
     * @return
     * @see processSequences
     */
    public Collection<BlatAssociation> processSequence( GoldenPathSequenceAnalysis goldenPath, BioSequence sequence ) {
        BlattableGenome bg = inferBlatGenome( goldenPath );
        Blat b = new Blat();
        b.setBlatScoreThreshold( blatScoreThreshold );
        Collection<BlatResult> results;
        try {
            results = b.blatQuery( sequence, bg );
        } catch ( IOException e ) {
            throw new RuntimeException( "Error running blat", e );
        }
        Map<String, Collection<BlatAssociation>> allRes = processBlatResults( goldenPath, results );
        assert allRes.keySet().size() == 1;
        return allRes.values().iterator().next();
    }

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankIds
     * @return
     */
    public Map<String, Collection<BlatAssociation>> processGbIds( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<String[]> genbankIds ) {
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<String, Collection<BlatAssociation>>();
        int count = 0;
        int skipped = 0;
        for ( String[] genbankIdAr : genbankIds ) {

            if ( genbankIdAr == null || genbankIdAr.length == 0 ) {
                continue;
            }

            if ( genbankIdAr.length > 1 ) {
                throw new IllegalArgumentException( "Input file must have just one genbank identifier per line" );
            }

            String genbankId = genbankIdAr[0];

            Map<String, Collection<BlatAssociation>> res = processGbId( goldenPathDb, genbankId );
            allRes.putAll( res );

            count++;
            if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " genbank identifiers" );
        }
        log.info( "Annotations computed for " + count + " genbank identifiers" );
        if ( log.isInfoEnabled() && skipped > 0 )
            log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        return allRes;
    }

    /**
     * Given some blat results,
     * 
     * @param goldenPathDb
     * @param blatResults
     * @return
     * @throws IOException
     */
    public Map<String, Collection<BlatAssociation>> processBlatResults( GoldenPathSequenceAnalysis goldenPathDb,
            Collection<BlatResult> blatResults ) {

        assert goldenPathDb != null;
        Map<String, Collection<BlatAssociation>> allRes = new HashMap<String, Collection<BlatAssociation>>();
        int count = 0;
        int skipped = 0;
        for ( BlatResult blatResult : blatResults ) {

            if ( blatResult.score() < scoreThreshold || blatResult.identity() < identityThreshold ) {
                skipped++;
                continue;
            }

            Collection<BlatAssociation> blatAssociations = processBlatResult( goldenPathDb, blatResult );

            if ( blatAssociations == null ) continue;

            String queryName = blatResult.getQuerySequence().getName();
            for ( BlatAssociation blatAssociation : blatAssociations ) {
                if ( !allRes.containsKey( queryName ) ) {
                    log.debug( "Adding " + queryName + " to results" );
                    allRes.put( queryName, new HashSet<BlatAssociation>() );
                }
                allRes.get( queryName ).add( blatAssociation );
            }

            count++;
            if ( log.isInfoEnabled() && count % 100 == 0 )
                log.info( "Annotations computed for " + count + " blat results" );
        }

        if ( log.isInfoEnabled() && skipped > 0 )
            log.info( "Skipped " + skipped + " results that didn't meet criteria" );

        return allRes;
    }

    /**
     * Process a single BlatResult.
     * 
     * @param goldenPathDb
     * @param blatResult
     * @return
     */
    private Collection<BlatAssociation> processBlatResult( GoldenPathSequenceAnalysis goldenPathDb,
            BlatResult blatResult ) {
        assert blatResult.getTargetChromosome() != null : "Chromosome not filled in for blat result";
        Collection<BlatAssociation> blatAssociations = goldenPathDb.getThreePrimeDistances( blatResult
                .getTargetChromosome().getName(), blatResult.getTargetStart(), blatResult.getTargetEnd(), blatResult
                .getTargetStarts(), blatResult.getBlockSizes(), blatResult.getStrand(), threeprimeMethod );

        if ( blatAssociations == null ) return null;

        for ( BlatAssociation association : blatAssociations ) {
            association.setBlatResult( blatResult );
        }

        return blatAssociations;
    }

    /**
     * @param scoreThreshold
     */
    public void setScoreThreshold( double scoreThreshold ) {
        this.scoreThreshold = scoreThreshold;

    }

    /**
     * @param identityThreshold
     */
    public void setIdentityThreshold( double identityThreshold ) {
        this.identityThreshold = identityThreshold;

    }

    /**
     * @return the blatScoreThreshold
     */
    public double getBlatScoreThreshold() {
        return this.blatScoreThreshold;
    }

    /**
     * @param blatScoreThreshold the blatScoreThreshold to set
     */
    public void setBlatScoreThreshold( double blatScoreThreshold ) {
        this.blatScoreThreshold = blatScoreThreshold;
    }
}
