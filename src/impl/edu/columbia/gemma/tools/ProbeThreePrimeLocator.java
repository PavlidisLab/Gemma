/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResult;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResultImpl;
import edu.columbia.gemma.loader.genome.BlatResultParser;
import edu.columbia.gemma.tools.GoldenPath.ThreePrimeData;

/**
 * Given a blat result set for an array design, annotate and find the 3' locations for all the really good hits.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeThreePrimeLocator {

    /**
     * 
     */
    private static final String PASSWORD = "toast";
    /**
     * 
     */
    private static final String USERNAME = "pavlidis";
    /**
     * 
     */
    private static final String HOST = "localhost";
    /**
     * 
     */
    private static final int PORT = 3306;

    private String databaseName = "hg17";
    private double identityThreshold = 0.90;
    private double scoreThreshold = 0.90;
    private String threeprimeMethod = GoldenPath.RIGHTEND;
    private static Log log = LogFactory.getLog( ProbeThreePrimeLocator.class.getName() );

    /**
     * @param input
     * @param output
     * @return
     * @throws IOException
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    public Map<String, Collection<LocationData>> run( InputStream input, Writer output ) throws IOException,
            SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        GoldenPath bp = new GoldenPath( PORT, databaseName, HOST, USERNAME, PASSWORD );

        BlatResultParser brp = new BlatResultParser();
        brp.parse( input );

        int count = 0;
        int skipped = 0;
        Map<String, Collection<LocationData>> allRes = new HashMap<String, Collection<LocationData>>();
        for ( Iterator<Object> iter = brp.getResults().iterator(); iter.hasNext(); ) {
            BlatResult blatRes = ( BlatResult ) iter.next();

            if ( blatRes.score() < scoreThreshold || blatRes.identity() < identityThreshold ) {
                skipped++;
                continue;
            }

            String[] sa = splitBlatQueryName( blatRes );
            String probeName = sa[0];
            String arrayName = sa[1];

            List<ThreePrimeData> tpds = bp.getThreePrimeDistances( blatRes.getTargetChromosome().getName(), blatRes
                    .getTargetStart(), blatRes.getTargetEnd(), blatRes.getTargetStarts(), blatRes.getBlockSizes(),
                    blatRes.getStrand(), threeprimeMethod );

            if ( tpds == null ) continue;

            for ( ThreePrimeData tpd : tpds ) {

                Gene gene = tpd.getGene();
                assert gene != null : "Null gene";

                LocationData ld = new LocationData( tpd, blatRes );
                if ( !allRes.containsKey( probeName ) ) {
                    allRes.put( probeName, new HashSet<LocationData>() );
                }
                allRes.get( probeName ).add( ld );

                output.write( probeName + "\t" + arrayName + "\t" + blatRes.getMatches() + "\t"
                        + blatRes.getQuerySequence().getLength() + "\t"
                        + ( blatRes.getTargetEnd() - blatRes.getTargetStart() ) + "\t" + blatRes.score() + "\t"
                        + gene.getOfficialSymbol() + "\t" + gene.getNcbiId() + "\t" + tpd.getDistance() + "\t"
                        + tpd.getExonOverlap() + "\t" + blatRes.getTargetChromosome().getName() + "\t"
                        + blatRes.getTargetStart() + "\t" + blatRes.getTargetEnd() + "\n" );

                count++;
                try {
                    Thread.sleep( 5 );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
                if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " probes" );
            }
        }
        log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        input.close();
        output.close();
        return allRes;
    }

    // /**
    // * @param hitGenes
    // * @param hitTranscripts
    // */
    // private String createTranscriptSignature( Set<String> hitTranscripts, Set<String> hitGenes ) {
    // List<String> sortedTranscripts = new ArrayList<String>();
    // sortedTranscripts.addAll( hitTranscripts );
    // Collections.sort( sortedTranscripts );
    //
    // List<String> sortedGenes = new ArrayList<String>();
    // sortedGenes.addAll( hitGenes );
    // Collections.sort( sortedGenes );
    //
    // StringBuilder transcriptSignatureBuf = new StringBuilder();
    // for ( String transcript : sortedTranscripts ) {
    // if ( transcript.length() == 0 ) continue;
    // transcriptSignatureBuf.append( transcript );
    // transcriptSignatureBuf.append( "__" );
    // }
    //
    // for ( String gene : sortedGenes ) {
    // if ( gene.length() == 0 ) continue;
    // transcriptSignatureBuf.append( gene );
    // transcriptSignatureBuf.append( "__" );
    // }
    // return transcriptSignatureBuf.toString();
    // }

    /**
     * Trim the results down to a set of "best" results. The results are sent to a provided writer
     * <p>
     * FIXME handle ties.
     * 
     * @throws IOException
     * @param results
     * @param writer
     */
    protected void getBest( Map<String, Collection<LocationData>> results, BufferedWriter writer ) throws IOException {
        for ( String probe : results.keySet() ) {
            Collection<LocationData> probeResults = results.get( probe );
            double maxBlatScore = -1.0;
            int maxScore = 0;
            LocationData best = null;
            double maxOverlap = 0.0;
            int alignLength = 0;
            for ( LocationData ld : probeResults ) {
                double blatScore = ld.getBr().score();
                double overlap = ( double ) ld.getTpd().getExonOverlap()
                        / ( double ) ( ld.getBr().getQuerySequence().getLength() );
                int score = ( int ) ( 1000 * blatScore * overlap );
                if ( score >= maxScore ) {
                    maxScore = score;
                    maxBlatScore = blatScore;
                    best = ld;
                    maxOverlap = overlap;
                    alignLength = ld.getBr().getTargetEnd() - ld.getBr().getTargetStart();
                }
            }

            // examine ties.
            int numTied = 0;
            for ( LocationData ld : probeResults ) {
                double blatScore = ld.getBr().score();
                double overlap = ld.getTpd().getExonOverlap() / ( double ) ( ld.getBr().getQuerySequence().getLength() );
                int score = ( int ) ( 1000 * blatScore * overlap );
                if ( score == maxScore ) {
                    numTied++;
                }
            }

            best.setBestBlatScore( maxBlatScore );
            best.setBestOverlap( maxOverlap );
            best.setNumHits( probeResults.size() );
            best.setAlignLength( alignLength );
            best.setNumTied( numTied );
            writer.write( best.toString() );
        }
    }

    /**
     * @param blatRes
     * @return
     */
    protected String[] splitBlatQueryName( BlatResult blatRes ) {
        String qName = blatRes.getQuerySequence().getName();
        String[] sa = qName.split( ":" );
        if ( sa.length < 2 ) throw new IllegalArgumentException( "Expected query name in format 'xxx:xxx'" );
        return sa;
    }

    public static void main( String[] args ) {

        try {
            if ( args.length < 3 ) {
                System.err.println( "usage: input blat result file name, output filename, dbName (hg17)" );
                System.exit( 0 );
            }
            String filename = args[0];
            File f = new File( filename );
            if ( !f.canRead() ) throw new IOException( "Can't read file" );
            ProbeThreePrimeLocator ptpl = new ProbeThreePrimeLocator();
            String outputFileName = args[1];

            File o = new File( outputFileName );
            // if ( !o.canWrite() ) throw new IOException( "Can't write " + outputFileName );
            String bestOutputFileName = outputFileName + ".best";
            log.info( "Saving best to " + bestOutputFileName );

            ptpl.databaseName = args[2];
            Map<String, Collection<LocationData>> results = ptpl.run( new FileInputStream( f ), new BufferedWriter(
                    new FileWriter( o ) ) );

            o = new File( bestOutputFileName );
            ptpl.getBest( results, new BufferedWriter( new FileWriter( o ) ) );

        } catch ( IOException e ) {
            log.error( e, e );
        } catch ( SQLException e ) {
            log.error( e, e );
        } catch ( InstantiationException e ) {
            log.error( e, e );
        } catch ( IllegalAccessException e ) {
            log.error( e, e );
        } catch ( ClassNotFoundException e ) {
            log.error( e, e );
        }
    }

    /**
     * <hr>
     * <p>
     * Copyright (c) 2004-2005 Columbia University
     * 
     * @author pavlidis
     * @version $Id$
     */
    class LocationData {
        private BlatResult br;
        private double maxBlatScore;
        private double maxOverlap;
        private int numHits;
        private ThreePrimeData tpd;
        private int alignLength;
        private int numTied;

        /**
         * @return Returns the numTied.
         */
        public int getNumTied() {
            return this.numTied;
        }

        /**
         * @param tpd2
         * @param blatRes
         */
        public LocationData( ThreePrimeData tpd, BlatResult blatRes ) {
            this.tpd = tpd;
            this.br = blatRes;
        }

        /**
         * @param numTied
         */
        public void setNumTied( int numTied ) {
            this.numTied = numTied;
        }

        /**
         * @return
         */
        public BlatResult getBr() {
            return this.br;
        }

        /**
         * @return Returns the maxBlatScore.
         */
        public double getMaxBlatScore() {
            return this.maxBlatScore;
        }

        /**
         * @return Returns the maxOverlap.
         */
        public double getMaxOverlap() {
            return this.maxOverlap;
        }

        /**
         * @return Returns the numHits.
         */
        public int getNumHits() {
            return this.numHits;
        }

        public ThreePrimeData getTpd() {
            return this.tpd;
        }

        /**
         * @param maxBlatScore
         */
        public void setBestBlatScore( double maxBlatScore ) {
            this.maxBlatScore = maxBlatScore;
        }

        /**
         * @param maxOverlap
         */
        public void setBestOverlap( double maxOverlap ) {
            this.maxOverlap = maxOverlap;

        }

        public void setBr( BlatResultImpl br ) {
            this.br = br;
        }

        /**
         * @param maxBlatScore The maxBlatScore to set.
         */
        public void setMaxBlatScore( double maxBlatScore ) {
            this.maxBlatScore = maxBlatScore;
        }

        /**
         * @param maxOverlap The maxOverlap to set.
         */
        public void setMaxOverlap( double maxOverlap ) {
            this.maxOverlap = maxOverlap;
        }

        /**
         * @param numHits The numHits to set.
         */
        public void setNumHits( int numHits ) {
            this.numHits = numHits;
        }

        public void setTpd( ThreePrimeData tpd ) {
            this.tpd = tpd;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            String[] sa = splitBlatQueryName( this.getBr() );
            String probeName = sa[0];
            String arrayName = sa[1];
            buf.append( probeName + "\t" + arrayName + "\t" + tpd.getGene().getOfficialSymbol() + "\t"
                    + tpd.getGene().getNcbiId() + "\t" );
            buf.append( this.getNumHits() + "\t" + this.getMaxBlatScore() + "\t" + this.getMaxOverlap() );
            buf.append( "\t" + tpd.getDistance() );
            buf.append( "\t" + this.alignLength );
            if ( this.br != null ) {
                if ( this.br.getTargetSequence() != null ) {
                    buf.append( "\t" + this.br.getTargetSequence().getName() );
                } else {
                    buf.append( "\t" );
                }
                buf.append( "\t" + this.br.getTargetStart() );
                buf.append( "\t" + this.br.getTargetEnd() );
            } else {
                buf.append( "\t\t\t" );
            }
            buf.append( "\t" + this.numTied );
            buf.append( "\n" );
            return buf.toString();
        }

        /**
         * @return Returns the alignLength.
         */
        public int getAlignLength() {
            return this.alignLength;
        }

        /**
         * @param alignLength The alignLength to set.
         */
        public void setAlignLength( int alignLength ) {
            this.alignLength = alignLength;
        }

    }

}
