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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.apps.Blat.BlattableGenome;
import ubic.gemma.externalDb.GoldenPath;
import ubic.gemma.externalDb.GoldenPath.MeasurementMethod;
import ubic.gemma.externalDb.GoldenPath.ThreePrimeData;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.loader.genome.FastaParser;
import ubic.gemma.loader.util.parser.Parser;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultImpl;
import ubic.gemma.util.AbstractCLI;

/**
 * Given a blat result set for an array design, annotate and find the 3' locations for all the really good hits.
 * <p>
 * FIXME this class contains more logic and functionality than just command line processing; it should be refactored.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapper extends AbstractCLI {

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
         * @param tpd2
         * @param blatRes
         */
        public LocationData( ThreePrimeData tpd, BlatResult blatRes ) {
            this.tpd = tpd;
            this.br = blatRes;
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
                    + "\t" + "numberTied" + "\n" );

            return buf.toString();
        }

        /**
         * @return Returns the alignLength.
         */
        public long getAlignLength() {
            return this.alignLength;
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

        /**
         * @return Returns the numTied.
         */
        public int getNumTied() {
            return this.numTied;
        }

        public ThreePrimeData getTpd() {
            return this.tpd;
        }

        /**
         * @param alignLength The alignLength to set.
         */
        public void setAlignLength( long alignLength ) {
            this.alignLength = alignLength;
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

        /**
         * @param numTied
         */
        public void setNumTied( int numTied ) {
            this.numTied = numTied;
        }

        public void setTpd( ThreePrimeData tpd ) {
            this.tpd = tpd;
        }

        public String toString() {
            StringBuilder buf = new StringBuilder();
            String[] sa = splitBlatQueryName( this.getBr() );
            String arrayName = "";
            String queryName = null;
            if ( sa.length == 2 ) {
                arrayName = sa[0];
                queryName = sa[1];
            } else if ( sa.length == 1 ) {
                queryName = sa[0];
            } else {
                throw new RuntimeException( "Query name was not in understood format" );
            }
            buf.append( queryName + "\t" + arrayName + "\t" + tpd.getGene().getOfficialSymbol() + "\t"
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

    }

    private static final String DEFAULT_DATABASE = "hg18";

    private static final double DFEAULT_IDENTITY_THRESHOLD = 0.90;
    private static final double DEFAULT_SCORE_THRESHOLD = 0.90;
    private static Log log = LogFactory.getLog( ProbeMapper.class.getName() );

    /**
     * @param bestOutputFileName
     * @return
     * @throws IOException
     */
    private static Writer getWriterForBestResults( String bestOutputFileName ) throws IOException {
        Writer w = null;
        File o = new File( bestOutputFileName );
        w = new BufferedWriter( new FileWriter( o ) );

        try {
            w.write( "" );
        } catch ( IOException e ) {
            throw new RuntimeException( "Could not write to " + bestOutputFileName );
        }
        return w;
    }

    public static void main( String[] args ) {
        ProbeMapper p = new ProbeMapper();
        try {
            p.doWork( args );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    private String databaseName = DEFAULT_DATABASE;
    private double identityThreshold = DFEAULT_IDENTITY_THRESHOLD;
    private double scoreThreshold = DEFAULT_SCORE_THRESHOLD;
    private String outputFileName = null;

    private MeasurementMethod threeprimeMethod = MeasurementMethod.right;

    private String blatFileName = null;

    private String ncbiIdentifierFileName = null;

    private String fastaFileName = null;

    public ProbeMapper() {
        super();
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

        addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription(
                "File containing sequences in FASTA format" ).withLongOpt( "fastaFile" ).create( 'f' ) );

        addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription( "Output file basename" )
                .isRequired().withLongOpt( "outputFile" ).create( 'o' ) );

        addOption( databaseNameOption );

        addUserNameAndPasswordOptions();
        addHostAndPortOptions( false, false );

    }

    @Override
    protected Exception doWork( String[] args ) throws Exception {

        try {
            Exception err = processCommandLine( "probeMapper", args );
            if ( err != null ) return err;
            String bestOutputFileName = outputFileName + ".best";
            log.info( "Saving best to " + bestOutputFileName );

            Writer resultsOut = getWriterForBestResults( outputFileName );
            Writer bestResultsOut = getWriterForBestResults( bestOutputFileName );

            if ( blatFileName != null ) {
                File f = new File( blatFileName );
                if ( !f.canRead() ) throw new IOException( "Can't read file " + blatFileName );
                Map<String, Collection<LocationData>> results = runOnBlatResults( new FileInputStream( f ), resultsOut );

                getBest( results, bestResultsOut );

            } else if ( ncbiIdentifierFileName != null ) {
                File f = new File( ncbiIdentifierFileName );
                if ( !f.canRead() ) throw new IOException( "Can't read file " + ncbiIdentifierFileName );

                Map<String, Collection<LocationData>> results = runOnGbIds( new FileInputStream( f ), resultsOut );

                getBest( results, bestResultsOut );
            } else if ( fastaFileName != null ) {
                File f = new File( fastaFileName );
                if ( !f.canRead() ) throw new IOException( "Can't read file " + fastaFileName );

                Map<String, Collection<LocationData>> results = runOnSequences( new FileInputStream( f ), resultsOut );

                getBest( results, bestResultsOut );
            } else {
                String[] moreArgs = getArgs();
                if ( moreArgs.length == 0 ) {
                    System.out
                            .println( "You must provide either a Blat result file, a FASTA file, a Genbank identifier file, or some Genbank identifiers" );
                    printHelp( "probeMapper" );
                    return new Exception( "Missing genbank identifiers" );
                }

                for ( int i = 0; i < moreArgs.length; i++ ) {
                    String gbId = moreArgs[i];

                    log.debug( "Got " + gbId );
                    Map<String, Collection<LocationData>> results = processGbId( resultsOut, null, gbId );
                    getBest( results, bestResultsOut );
                }
            }

            resultsOut.close();
            bestResultsOut.close();

        } catch ( Exception e ) {
            return new RuntimeException( e );
        }
        return null;
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
    protected void getBest( Map<String, Collection<LocationData>> results, Writer writer ) throws IOException {
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
     * Given a sequence, BLAT it against the appropriate genome.
     */
    private Map<String, Collection<LocationData>> processSequences( Writer output, GoldenPath goldenpath,
            Collection<?> sequences ) {
        Blat b = new Blat();
        Map<String, Collection<LocationData>> allRes = new HashMap<String, Collection<LocationData>>();

        BlattableGenome bg = BlattableGenome.HUMAN;
        // FIXME - this should not be hard coded like this, what happens when more genomes are added.
        if ( goldenpath.getDatabaseName().startsWith( "mm" ) ) {
            bg = BlattableGenome.MOUSE;
        } else if ( goldenpath.getDatabaseName().startsWith( "hg" ) ) {
            bg = BlattableGenome.HUMAN;
        } else if ( goldenpath.getDatabaseName().startsWith( "rn" ) ) {
            bg = BlattableGenome.RAT;
        } else {
            throw new IllegalArgumentException( "Unsupported database for blatting " + goldenpath.getDatabaseName() );
        }

        for ( Object object : sequences ) {
            BioSequence sequence = ( BioSequence ) object;
            try {
                Collection<Object> results = b.blatQuery( sequence, bg );
                Map<String, Collection<LocationData>> res = processBlatResults( output, null, results );
                allRes.putAll( res );
            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }
        }
        return allRes;
    }

    public Map<String, Collection<LocationData>> runOnSequences( InputStream stream, Writer output ) {
        GoldenPath goldenPathDb;
        try {
            goldenPathDb = new GoldenPath( port, databaseName, host, username, password );

            Parser parser = new FastaParser();
            parser.parse( stream );

            writeHeader( output );

            Collection<Object> sequences = parser.getResults();

            log.debug( "Parsed " + sequences.size() + " sequences from the stream" );

            return processSequences( output, goldenPathDb, sequences );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
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
            Collection<?> blatResults ) throws IOException {
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
            String arrayName = "";
            String queryName = null;
            if ( sa.length == 2 ) {
                arrayName = sa[0];
                queryName = sa[1];
            } else if ( sa.length == 1 ) {
                queryName = sa[0];
            } else {
                throw new RuntimeException( "Query name was not in understood format" );
            }

            List<ThreePrimeData> tpds = goldenPathDb.getThreePrimeDistances( blatRes.getTargetChromosome().getName(),
                    blatRes.getTargetStart(), blatRes.getTargetEnd(), blatRes.getTargetStarts(), blatRes
                            .getBlockSizes(), blatRes.getStrand(), threeprimeMethod );

            if ( tpds == null ) continue;

            for ( ThreePrimeData tpd : tpds ) {

                LocationData res = processThreePrimeData( output, blatRes, arrayName, queryName, tpd );

                if ( !allRes.containsKey( queryName ) ) {
                    log.debug( "Adding " + queryName + " to results" );
                    allRes.put( queryName, new HashSet<LocationData>() );
                }
                allRes.get( queryName ).add( res );

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
     * @param writer
     * @param goldenPathDb
     * @param genbankId
     * @throws IOException
     */
    public Map<String, Collection<LocationData>> processGbId( Writer writer, GoldenPath goldenPathDb, String genbankId )
            throws IOException {

        log.debug( "Entering processGbId with " + genbankId );

        if ( goldenPathDb == null ) {
            try {
                goldenPathDb = new GoldenPath( port, databaseName, host, username, password );
            } catch ( SQLException e ) {
                throw new RuntimeException( e );
            } catch ( InstantiationException e ) {
                throw new RuntimeException( e );
            } catch ( IllegalAccessException e ) {
                throw new RuntimeException( e );
            } catch ( ClassNotFoundException e ) {
                throw new RuntimeException( e );
            }
        }
        Collection<BlatResult> blatResults = goldenPathDb.findSequenceLocations( genbankId );

        if ( blatResults == null || blatResults.size() == 0 ) {
            log.warn( "No results obtained for " + genbankId );
        }

        return this.processBlatResults( writer, goldenPathDb, blatResults );

    }

    /**
     * @param writer
     * @param goldenPathDb
     * @param genbankIds
     * @return
     */
    private Map<String, Collection<LocationData>> processGbIds( Writer writer, GoldenPath goldenPathDb,
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

            processGbId( writer, goldenPathDb, genbankId );

            count++;
            if ( count % 100 == 0 ) log.info( "Annotations computed for " + count + " genbank identifiers" );
        }
        log.info( "Annotations computed for " + count + " genbank identifiers" );
        log.info( "Skipped " + skipped + " results that didn't meet criteria" );
        return allRes;
    }

    @Override
    protected void processOptions() throws Exception {
        if ( hasOption( 's' ) ) {
            this.scoreThreshold = getDoubleOptionValue( 's' );
        }

        if ( hasOption( 'i' ) ) {
            this.identityThreshold = getDoubleOptionValue( 'i' );
        }

        if ( hasOption( 'd' ) ) {
            this.databaseName = getOptionValue( 'd' );
        } else {
            this.databaseName = DEFAULT_DATABASE;
        }

        if ( hasOption( 'f' ) ) {
            this.fastaFileName = getOptionValue( 'f' );
        }

        if ( hasOption( 'b' ) ) {
            this.blatFileName = getFileNameOptionValue( 'b' );
        }

        if ( hasOption( 'g' ) ) {
            this.ncbiIdentifierFileName = getFileNameOptionValue( 'g' );
        }

        this.outputFileName = getOptionValue( 'o' );

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

        Collection<Object> blatResults = brp.getResults();

        Map<String, Collection<LocationData>> allRes = processBlatResults( output, goldenPathDb, blatResults );

        input.close();
        output.close();
        return allRes;
    }

    /**
     * @param stream containing genbank accessions, one per line.
     * @param writer
     * @return
     */
    public Map<String, Collection<LocationData>> runOnGbIds( InputStream stream, Writer writer ) throws IOException,
            SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
        GoldenPath goldenPathDb = new GoldenPath( port, databaseName, host, username, password );

        Parser parser = new TabDelimParser();
        parser.parse( stream );

        writeHeader( writer );

        Collection<Object> genbankIds = parser.getResults();

        log.debug( "Parsed " + genbankIds.size() + " lines from the stream" );

        Map<String, Collection<LocationData>> allRes = processGbIds( writer, goldenPathDb, genbankIds );
        stream.close();
        writer.close();
        return allRes;
    }

    /**
     * @param blatRes
     * @return
     */
    protected String[] splitBlatQueryName( BlatResult blatRes ) {
        String qName = blatRes.getQuerySequence().getName();
        String[] sa = qName.split( ":" );
        // if ( sa.length < 2 ) throw new IllegalArgumentException( "Expected query name in format 'xxx:xxx'" );
        return sa;
    }

    /**
     * Generate a header for the "best result" file.
     */
    private void writeBestHeader( Writer writer ) throws IOException {
        LocationData f = new LocationData( null, null );
        writer.write( f.generateHeader() );
    }

    /**
     * Generate a header for the output file. TODO: should be optional.
     */
    private void writeHeader( Writer output ) throws IOException {
        output.write( "Probe" + "\t" + "Array" + "\t" + "Blat.matches" + "\t" + "Blat.queryLength" + "\t"
                + "Blat.targetAlignmentLength" + "\t" + "Blat.score" + "\t" + "Gene.symbol" + "\t" + "Gene.NCBIid"
                + "\t" + "threePrime.distance" + "\t" + "exonOverlap" + "\t" + "Blat.Chromosome" + "\t"
                + "Blat.targetStart" + "\t" + "Blat.targetEnd" + "\n" );
    }

}
