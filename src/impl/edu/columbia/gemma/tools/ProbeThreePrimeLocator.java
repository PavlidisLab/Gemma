package edu.columbia.gemma.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.Gene;
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

    protected static final Log log = LogFactory.getLog( ProbeThreePrimeLocator.class );
    private String dbName = "hg17";
    private double exonOverlapThreshold = 0.50;
    private double identityThreshold = 0.90;
    private double scoreThreshold = 0.90;

    public Map run( InputStream input, Writer output ) throws IOException, SQLException, InstantiationException,
            IllegalAccessException, ClassNotFoundException {

        GoldenPath bp = new GoldenPath( 3306, dbName, "localhost", "pavlidis", "toast" );

        BlatResultParser brp = new BlatResultParser();
        brp.parse( input );

        int count = 0;
        int skipped = 0;
        Map allRes = new HashMap();
        for ( Iterator iter = brp.iterator(); iter.hasNext(); ) {
            BlatResultImpl blatRes = ( BlatResultImpl ) iter.next(); // fixme, this should not be an impl

            if ( blatRes.score() < scoreThreshold || blatRes.identity() < identityThreshold ) {
                skipped++;
                continue;
            }

            String[] sa = splitBlatQueryName( blatRes );
            String probeName = sa[0];
            String arrayName = sa[1];

            List tpds = bp.getThreePrimeDistances( blatRes.getTargetName(), blatRes.getTargetStart(), blatRes
                    .getTargetEnd(), blatRes.getTargetStarts(), blatRes.getBlockSizes(), blatRes.getStrand() );

            if ( tpds == null ) continue;

//            Set hitTranscripts = new TreeSet();
//            Set hitGenes = new TreeSet();
            for ( Iterator iterator = tpds.iterator(); iterator.hasNext(); ) {
                ThreePrimeData tpd = ( ThreePrimeData ) iterator.next();

                if ( tpd.getExonOverlap() / blatRes.getQuerySize() < exonOverlapThreshold ) continue;

                Gene gene = tpd.getGene();
                assert gene != null : "Null gene";

//                hitTranscripts.add( gene.getNcbiId() );
//                hitGenes.add( gene.getOfficialSymbol() );
                LocationData ld = new LocationData( tpd, blatRes );
                if ( !allRes.containsKey( probeName ) ) {
                    allRes.put( probeName, new HashSet() );
                }
                ( ( Collection ) allRes.get( probeName ) ).add( ld );

                output.write( probeName + "\t" + arrayName + "\t" + blatRes.getMatches() + "\t"
                        + blatRes.getQuerySize() + "\t" + ( blatRes.getTargetEnd() - blatRes.getTargetStart() ) + "\t"
                        + blatRes.score() + "\t" + gene.getOfficialSymbol() + "\t" + gene.getNcbiId() + "\t"
                        + tpd.getDistance() + "\t" + tpd.getExonOverlap() + "\n" );

                count++;
                if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " probes" );
            }

            // String transcriptSignature = createTranscriptSignature( hitTranscripts, hitGenes );
            // output.write( probeName + "\t" + transcriptSignature + "\n" );

        }
        log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        input.close();
        output.close();
        return allRes;
    }

    /**
     * @param hitGenes
     * @param hitTranscripts
     */
    private String createTranscriptSignature( Set hitTranscripts, Set hitGenes ) {
        List sortedTranscripts = new ArrayList();
        sortedTranscripts.addAll( hitTranscripts );
        Collections.sort( sortedTranscripts );

        List sortedGenes = new ArrayList();
        sortedGenes.addAll( hitGenes );
        Collections.sort( sortedGenes );

        StringBuffer transcriptSignatureBuf = new StringBuffer();
        for ( Iterator iterator = sortedTranscripts.iterator(); iterator.hasNext(); ) {
            String transcript = ( String ) iterator.next();
            if ( transcript.length() == 0 ) continue;
            transcriptSignatureBuf.append( transcript );
            transcriptSignatureBuf.append( "__" );
        }
        for ( Iterator iter = sortedGenes.iterator(); iter.hasNext(); ) {
            String gene = ( String ) iter.next();
            if ( gene.length() == 0 ) continue;
            transcriptSignatureBuf.append( gene );
            transcriptSignatureBuf.append( "__" );
        }
        return transcriptSignatureBuf.toString();
    }

    /**
     * @throws IOException
     * @param results
     * @param writer
     */
    private void getBest( Map results, BufferedWriter writer ) throws IOException {
        for ( Iterator iter = results.keySet().iterator(); iter.hasNext(); ) {
            String probe = ( String ) iter.next();
            Collection probeResults = ( Collection ) results.get( probe );
            double maxBlatScore = -1.0;
            double maxScore = 0.0;
            LocationData best = null;
            double maxOverlap = 0.0;
            for ( Iterator iterator = probeResults.iterator(); iterator.hasNext(); ) {
                LocationData ld = ( LocationData ) iterator.next();
                double blatScore = ld.getBr().score();
                double overlap = ld.getTpd().getExonOverlap()
                        / ( ld.getBr().getTargetEnd() - ld.getBr().getTargetStart() );
                double score = blatScore * overlap;
                if ( score >= maxScore ) {
                    maxScore = score;
                    maxBlatScore = blatScore;
                    best = ld;
                    maxOverlap = overlap;
                }
            }
            best.setBestBlatScore( maxBlatScore );
            best.setBestOverlap( maxOverlap );
            best.setNumHits( probeResults.size() );
            writer.write( best.toString() );

        }

    }

    /**
     * @param blatRes
     * @return
     */
    protected String[] splitBlatQueryName( BlatResultImpl blatRes ) {
        String qName = blatRes.getQueryName();
        String[] sa = qName.split( ":" );
        if ( sa.length < 2 ) throw new IllegalArgumentException( "Expected query name in format 'xxx:xxx'" );
        return sa;
    }

    public static void main( String[] args ) {

        try {
            if ( args.length < 2 ) throw new IllegalArgumentException( "usage: input file name, output filename" );
            String filename = args[0];
            File f = new File( filename );
            if ( !f.canRead() ) throw new IOException();

            String outputFileName = args[1];
            File o = new File( outputFileName );
            // if ( !o.canWrite() ) throw new IOException( "Can't write " + outputFileName );
            String bestOutputFileName = outputFileName.replaceFirst( "\\.", ".best." );
            log.info( "Saving best to " + bestOutputFileName );

            ProbeThreePrimeLocator ptpl = new ProbeThreePrimeLocator();
            Map results = ptpl.run( new FileInputStream( f ), new BufferedWriter( new FileWriter( o ) ) );

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
     * Ju
     * 
     *
     * <hr>
     * <p>Copyright (c) 2004-2005 Columbia University
     * @author pavlidis
     * @version $Id$
     */
    class LocationData {
        private BlatResultImpl br;
        private double maxBlatScore;
        private double maxOverlap;
        private int numHits;
        private ThreePrimeData tpd;

        /**
         * @param tpd2
         * @param blatRes
         */
        public LocationData( ThreePrimeData tpd, BlatResultImpl blatRes ) {
            this.tpd = tpd;
            this.br = blatRes;
        }

        public BlatResultImpl getBr() {
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
            StringBuffer buf = new StringBuffer();
            String[] sa = splitBlatQueryName( this.getBr() );
            String probeName = sa[0];
            String arrayName = sa[1];
            buf.append( probeName + "\t" + arrayName + "\t" + tpd.getGene().getOfficialSymbol() + "\t"
                    + tpd.getGene().getNcbiId() + "\t" );
            buf.append( this.getNumHits() + "\t" + this.getMaxBlatScore() + "\t" + this.getMaxOverlap() );
            buf.append("\n");
            return buf.toString();
        }

    }

}
