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
package ubic.gemma.apps;

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
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.externalDb.GoldenPath;
import ubic.gemma.externalDb.GoldenPath.MeasurementMethod;
import ubic.gemma.externalDb.GoldenPath.ThreePrimeData;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.loader.util.AbstractCLI;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultImpl;

/**
 * Given a blat result set for an array design, annotate and find the 3' locations for all the really good hits. FIXME -
 * make this use the standard CLI.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapper extends AbstractCLI {

    private static final String DEFAULT_DATABASE = "hg18";
    private String databaseName = DEFAULT_DATABASE;

    private static final double DFEAULT_IDENTITY_THRESHOLD = 0.90;
    private static final double DEFAULT_SCORE_THRESHOLD = 0.90;
    private double identityThreshold = DFEAULT_IDENTITY_THRESHOLD;
    private double scoreThreshold = DEFAULT_SCORE_THRESHOLD;
    private String outputFileName = null;

    private MeasurementMethod threeprimeMethod = MeasurementMethod.right;
    private String blatFileName = null;
    private String ncbiIdentifierFileName = null;
    private static Log log = LogFactory.getLog( ProbeMapper.class.getName() );

    public ProbeMapper() {
        super();
    }

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
    @SuppressWarnings("unchecked")
    public Map<String, Collection<LocationData>> runOnBlatResults( InputStream input, Writer output )
            throws IOException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

        GoldenPath goldenPathDb = new GoldenPath( port, databaseName, host, username, password );

        BlatResultParser brp = new BlatResultParser();
        brp.parse( input );

        writeHeader( output );

        Collection blatResults = brp.getResults();

        Map<String, Collection<LocationData>> allRes = processBlatResults( output, goldenPathDb, blatResults );

        input.close();
        output.close();
        return allRes;
    }

    /**
     * @param stream containing genbank ids, one per line.
     * @param writer
     * @return
     */
    private Map<String, Collection<LocationData>> runOnGbIds( FileInputStream stream, BufferedWriter writer )
            throws IOException, SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        GoldenPath goldenPathDb = new GoldenPath( port, databaseName, host, username, password );

        TabDelimParser brp = new TabDelimParser();
        brp.parse( stream );

        writeHeader( writer );

        Collection<Object> genbankIds = brp.getResults();

        Map<String, Collection<LocationData>> allRes = processGbIds( writer, goldenPathDb, genbankIds );
        stream.close();
        writer.close();
        return allRes;
    }

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankIds
     * @return
     */
    private Map<String, Collection<LocationData>> processGbIds( BufferedWriter writer, GoldenPath goldenPathDb,
            Collection<Object> genbankIds ) throws IOException {
        Map<String, Collection<LocationData>> allRes = new HashMap<String, Collection<LocationData>>();
        int count = 0;
        int skipped = 0;
        for ( Object obj : genbankIds ) {

            assert obj instanceof String[];

            String[] genbankIdAr = ( String[] ) obj;

            if ( genbankIdAr == null || genbankIdAr.length == 0 ) {
                continue;
            }

            if ( genbankIdAr.length > 1 ) {
                throw new IllegalArgumentException( "Input file must have just one genbank identifier per line" );
            }

            String genbankId = genbankIdAr[0];

            Collection<BlatResult> blatResults = goldenPathDb.findSequenceLocations( genbankId );

            this.processBlatResults( writer, goldenPathDb, blatResults );

            count++;
            if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " genbank identifiers" );
        }
        log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        return allRes;
    }

    /**
     * Given some blat results,
     * 
     * @param output
     * @param goldenPathDb
     * @param blatResults
     * @return
     * @throws IOException
     */
    private Map<String, Collection<LocationData>> processBlatResults( Writer output, GoldenPath goldenPathDb,
            Collection<BlatResult> blatResults ) throws IOException {
        Map<String, Collection<LocationData>> allRes = new HashMap<String, Collection<LocationData>>();
        int count = 0;
        int skipped = 0;
        for ( Object blatResult : blatResults ) {

            assert blatResult instanceof BlatResult;
            BlatResult blatRes = ( BlatResult ) blatResult;

            if ( blatRes.score() < scoreThreshold || blatRes.identity() < identityThreshold ) {
                skipped++;
                continue;
            }

            String[] sa = splitBlatQueryName( blatRes );
            String arrayName = sa[0];
            String probeName = sa[1];

            List<ThreePrimeData> tpds = goldenPathDb.getThreePrimeDistances( blatRes.getTargetChromosome().getName(),
                    blatRes.getTargetStart(), blatRes.getTargetEnd(), blatRes.getTargetStarts(), blatRes
                            .getBlockSizes(), blatRes.getStrand(), threeprimeMethod );

            if ( tpds == null ) continue;

            for ( ThreePrimeData tpd : tpds ) {

                LocationData res = processThreePrimeData( output, blatRes, arrayName, probeName, tpd );

                if ( !allRes.containsKey( probeName ) ) {
                    log.debug( "Adding " + probeName + " to results" );
                    allRes.put( probeName, new HashSet<LocationData>() );
                }
                allRes.get( probeName ).add( res );

                try {
                    Thread.sleep( 5 );
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
            }

            count++;
            if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " blat results" );
        }
        log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        return allRes;
    }

    /**
     * @param output
     * @param blatRes
     * @param arrayName
     * @param probeName
     * @param tpd
     * @return LocationDAta
     * @throws IOException
     */
    private LocationData processThreePrimeData( Writer output, BlatResult blatRes, String arrayName, String probeName,
            ThreePrimeData tpd ) throws IOException {
        Gene gene = tpd.getGene();
        assert gene != null : "Null gene";

        LocationData ld = new LocationData( tpd, blatRes );

        output.write( probeName + "\t" + arrayName + "\t" + blatRes.getMatches() + "\t"
                + blatRes.getQuerySequence().getLength() + "\t" + ( blatRes.getTargetEnd() - blatRes.getTargetStart() )
                + "\t" + blatRes.score() + "\t" + gene.getOfficialSymbol() + "\t" + gene.getNcbiId() + "\t"
                + tpd.getDistance() + "\t" + tpd.getExonOverlap() + "\t" + blatRes.getTargetChromosome().getName()
                + "\t" + blatRes.getTargetStart() + "\t" + blatRes.getTargetEnd() + "\n" );

        return ld;
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
     * Generate a header for the output file. TODO: should be optional.
     */
    private void writeHeader( Writer output ) throws IOException {
        output.write( "Probe" + "\t" + "Array" + "\t" + "Blat.matches" + "\t" + "Blat.queryLength" + "\t"
                + "Blat.targetAlignmentLength" + "\t" + "Blat.score" + "\t" + "Gene.symbol" + "\t" + "Gene.NCBIid"
                + "\t" + "threePrime.distance" + "\t" + "exonOverlap" + "\t" + "Blat.Chromosome" + "\t"
                + "Blat.targetStart" + "\t" + "Blat.targetEnd" + "\n" );
    }

    /**
     * Generate a header for the "best result" file.
     */
    private void writeBestHeader( Writer writer ) throws IOException {
        LocationData f = new LocationData( null, null );
        writer.write( f.generateHeader() );
    }

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
        log.info( "Preparing 'best' matches" );

        writeBestHeader( writer );

        for ( String probe : results.keySet() ) {
            log.debug( "Checking " + probe );
            Collection<LocationData> probeResults = results.get( probe );
            double maxBlatScore = -1.0;
            int maxScore = 0;
            LocationData best = null;
            double maxOverlap = 0.0;
            long alignLength = 0L;
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
        writer.close();
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

        ProbeMapper ptpl = new ProbeMapper();

        int err = ptpl.processCommandLine( "probeMapper", args );
        if ( err != 0 ) return;

        try {

            String bestOutputFileName = ptpl.outputFileName + ".best";
            log.info( "Saving best to " + bestOutputFileName );
            File o = new File( bestOutputFileName );

            if ( ptpl.blatFileName != null ) {
                File f = new File( ptpl.blatFileName );

                Map<String, Collection<LocationData>> results = ptpl.runOnBlatResults( new FileInputStream( f ),
                        new BufferedWriter( new FileWriter( o ) ) );

                ptpl.getBest( results, new BufferedWriter( new FileWriter( o ) ) );

            } else if ( ptpl.ncbiIdentifierFileName != null ) {
                File f = new File( ptpl.ncbiIdentifierFileName );
                if ( !f.canRead() ) throw new IOException( "Can't read file" );

                Map<String, Collection<LocationData>> results = ptpl.runOnGbIds( new FileInputStream( f ),
                        new BufferedWriter( new FileWriter( o ) ) );

                ptpl.getBest( results, new BufferedWriter( new FileWriter( o ) ) );
            } else {
                String[] moreArgs = ptpl.getArgs();
                if ( moreArgs.length == 0 ) {
                    System.out
                            .println( "You must provide either a Blat result file, a Genbank identifier file, or some Genbank identifiers" );
                    ptpl.printHelp( "probeMapper" );
                    return;
                }

                // TODO - process loose Genbank identifiers.

            }

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
     * @author pavlidis
     * @version $Id$
     */
    class LocationData {
        private BlatResult br;
        private double maxBlatScore;
        private double maxOverlap;
        private int numHits;
        private ThreePrimeData tpd;
        private long alignLength;
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

        /**
         * Generate a header to be used in output files.
         * 
         * @return
         */
        public String generateHeader() {
            StringBuilder buf = new StringBuilder();

            buf.append( "probe" + "\t" + "array" + "\t" + "gene.symbol" + "\t" + "gene.NCBIid" + "\t" + "numHits"
                    + "\t" + "blat.score" + "\t" + "blat.exonOverlap" + "\t" + "threeprime.distance" + "\t"
                    + "blat.alignLenth" + "\t" + "targetSequenceName" + "\t" + "startInTarget" + "\t" + "endInTarget"
                    + "\t" + "numberTied" );

            return buf.toString();
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
        public long getAlignLength() {
            return this.alignLength;
        }

        /**
         * @param alignLength The alignLength to set.
         */
        public void setAlignLength( long alignLength ) {
            this.alignLength = alignLength;
        }

    }

    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option blatResultOption = OptionBuilder.hasArg().withArgName( "PSL file" ).withDescription(
                "Blat result file in PSL format" ).withLongOpt( "blatfile" ).create( 'b' );

        addOption( blatResultOption );

        Option databaseNameOption = OptionBuilder.hasArg().withArgName( "database" ).withDescription(
                "GoldenPath database id (default=" + DEFAULT_DATABASE + ")" ).withLongOpt( "database" ).create( 'd' );

        addOption( OptionBuilder.hasArg().withArgName( "value" ).withDescription(
                "Sequence identity threshold, default = " + DFEAULT_IDENTITY_THRESHOLD ).withLongOpt(
                "identityThreshold" ).create( 'i' ) );

        addOption( OptionBuilder.hasArg().withArgName( "value" ).withDescription(
                "Blat score threshold, default = " + DEFAULT_SCORE_THRESHOLD ).withLongOpt( "scoreThreshold" ).create(
                's' ) );

        addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription(
                "File containing Genbank identifiers" ).withLongOpt( "gbfile" ).create( 'g' ) );

        addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription( "Output file basename" )
                .isRequired().withLongOpt( "outputFile" ).create( 'o' ) );

        addOption( databaseNameOption );

        addUserNameAndPasswordOptions();
        addHostAndPortOptions( false, false );

    }

    @Override
    protected void processOptions() {
        if ( hasOption( 's' ) ) {
            this.scoreThreshold = getDoubleOptionValue( 's' );
        }

        if ( hasOption( 'i' ) ) {
            this.identityThreshold = getDoubleOptionValue( 'i' );
        }

        if ( hasOption( 'd' ) ) {
            this.databaseName = getOptionValue( 'd' );
        }

        if ( hasOption( 'b' ) ) {
            this.blatFileName = getFileNameOptionValue( 'b' );
        }

        if ( hasOption( 'g' ) ) {
            this.ncbiIdentifierFileName = getFileNameOptionValue( 'g' );
        }

        this.outputFileName = getOptionValue( 'o' );

    }

}
