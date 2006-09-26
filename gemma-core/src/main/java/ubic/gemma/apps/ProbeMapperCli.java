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
import java.util.Map;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.analysis.sequence.ProbeMapper;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.loader.genome.FastaParser;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.AbstractCLI;

/**
 * Given a blat result set for an array design, annotate and find the 3' locations for all the really good hits.
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperCli extends AbstractCLI {

    ProbeMapper probeMapper = new ProbeMapper();

    private static final String DEFAULT_DATABASE = "hg18";

    private static Log log = LogFactory.getLog( ProbeMapperCli.class.getName() );

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
        ProbeMapperCli p = new ProbeMapperCli();
        Exception e = p.doWork( args );
        if ( e != null ) {
            System.err.println( e.getLocalizedMessage() );
            if ( log.isDebugEnabled() ) {
                log.debug( e, e );
            }
        }
    }

    private String databaseName = DEFAULT_DATABASE;

    private String outputFileName = null;

    private String blatFileName = null;

    private String ncbiIdentifierFileName = null;

    private String fastaFileName = null;

    public ProbeMapperCli() {
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
                "Sequence identity threshold, default = " + probeMapper.DEFAULT_IDENTITY_THRESHOLD ).withLongOpt(
                "identityThreshold" ).create( 'i' ) );

        addOption( OptionBuilder.hasArg().withArgName( "value" ).withDescription(
                "Blat score threshold, default = " + probeMapper.DEFAULT_SCORE_THRESHOLD ).withLongOpt(
                "scoreThreshold" ).create( 's' ) );

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
    protected Exception doWork( String[] args ) {

        try {
            Exception err = processCommandLine( "probeMapper", args );
            if ( err != null ) return err;
            String bestOutputFileName = outputFileName + ".best";
            log.info( "Saving best to " + bestOutputFileName );

            Writer resultsOut = getWriterForBestResults( outputFileName );
            Writer bestResultsOut = getWriterForBestResults( bestOutputFileName );

            log.info( "DatabaseHost = " + this.host + " User=" + this.username );

            if ( blatFileName != null ) {
                File f = new File( blatFileName );
                if ( !f.canRead() ) throw new IOException( "Can't read file " + blatFileName );
                Map<String, Collection<BlatAssociation>> results = runOnBlatResults( new FileInputStream( f ),
                        resultsOut );

                printBestResults( results, bestResultsOut );

            } else if ( ncbiIdentifierFileName != null ) {
                File f = new File( ncbiIdentifierFileName );
                if ( !f.canRead() ) throw new IOException( "Can't read file " + ncbiIdentifierFileName );

                Map<String, Collection<BlatAssociation>> results = runOnGbIds( new FileInputStream( f ), resultsOut );

                printBestResults( results, bestResultsOut );
            } else if ( fastaFileName != null ) {
                File f = new File( fastaFileName );
                if ( !f.canRead() ) throw new IOException( "Can't read file " + fastaFileName );

                Map<String, Collection<BlatAssociation>> results = runOnSequences( new FileInputStream( f ), resultsOut );

                printBestResults( results, bestResultsOut );
            } else {
                String[] moreArgs = getArgs();
                if ( moreArgs.length == 0 ) {
                    System.out
                            .println( "You must provide either a Blat result file, a FASTA file, a Genbank identifier file, or some Genbank identifiers" );
                    printHelp( "probeMapper" );
                    return new Exception( "Missing genbank identifiers" );
                }

                GoldenPathSequenceAnalysis goldenPathDb;

                goldenPathDb = new GoldenPathSequenceAnalysis( port, databaseName, host, username, password );

                for ( int i = 0; i < moreArgs.length; i++ ) {
                    String gbId = moreArgs[i];

                    log.debug( "Got " + gbId );
                    Map<String, Collection<BlatAssociation>> results = probeMapper.processGbId( goldenPathDb, gbId );

                    printBlatAssociations( resultsOut, results );
                    printBestResults( results, bestResultsOut );
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
     * 
     * @throws IOException
     * @param results
     * @param writer
     */
    protected void printBestResults( Map<String, Collection<BlatAssociation>> results, Writer writer )
            throws IOException {
        log.info( "Preparing 'best' matches" );

        writeHeader( writer );

        for ( String probe : results.keySet() ) {
            BlatAssociation best = probeMapper.selectBest( results.get( probe ) );
            writeDesignElementBlatAssociation( writer, best );
        }

    }

    /**
     * @param stream
     * @param output
     * @return
     */
    public Map<String, Collection<BlatAssociation>> runOnSequences( InputStream stream, Writer output ) {

        try {
            GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( port, databaseName, host,
                    username, password );

            FastaParser parser = new FastaParser();
            parser.parse( stream );

            writeHeader( output );
            Collection<BioSequence> sequences = parser.getResults();

            log.debug( "Parsed " + sequences.size() + " sequences from the stream" );

            assert goldenPathDb != null;
            Map<String, Collection<BlatAssociation>> allRes = probeMapper.processSequences( goldenPathDb, sequences );

            printBlatAssociations( output, allRes );
            return allRes;

        } catch ( SQLException e ) {
            throw new RuntimeException( e );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void processOptions() {
        if ( hasOption( 's' ) ) {
            probeMapper.setScoreThreshold( getDoubleOptionValue( 's' ) );
        }

        if ( hasOption( 'i' ) ) {
            probeMapper.setIdentityThreshold( getDoubleOptionValue( 'i' ) );
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
     * @param probeName
     * @param arrayName
     * @param ld
     * @throws IOException
     */
    public void writeDesignElementBlatAssociation( Writer output, BlatAssociation association ) throws IOException {

        BlatResult blatRes = association.getBlatResult();

        String[] sa = splitBlatQueryName( blatRes );
        String arrayName = "";
        String probeName = null;
        if ( sa.length == 2 ) {
            arrayName = sa[0];
            probeName = sa[1];
        } else if ( sa.length == 1 ) {
            probeName = sa[0];
        } else {
            throw new RuntimeException( "Query name was not in understood format" );
        }
        GeneProduct product = association.getGeneProduct();

        Gene g = product.getGene();

        output.write( probeName + "\t" + arrayName + "\t" + blatRes.getMatches() + "\t"
                + blatRes.getQuerySequence().getLength() + "\t" + ( blatRes.getTargetEnd() - blatRes.getTargetStart() )
                + "\t" + blatRes.score() + "\t" + g.getOfficialSymbol() + "\t" + product.getNcbiId() + "\t"
                + association.getThreePrimeDistance() + "\t" + association.getOverlap() + "\t"
                + blatRes.getTargetChromosome().getName() + "\t" + blatRes.getTargetStart() + "\t"
                + blatRes.getTargetEnd() + "\n" );

    }

    /**
     * @param blatResultInputStream
     * @param output
     * @return
     * @throws IOException
     * @throws SQLException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public Map<String, Collection<BlatAssociation>> runOnBlatResults( InputStream blatResultInputStream, Writer output )
            throws IOException, SQLException {

        GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( port, databaseName, host, username,
                password );

        BlatResultParser brp = new BlatResultParser();
        brp.parse( blatResultInputStream );

        writeHeader( output );

        Collection<BlatResult> blatResults = brp.getResults();

        Map<String, Collection<BlatAssociation>> allRes = probeMapper.processBlatResults( goldenPathDb, blatResults );

        printBlatAssociations( output, allRes );

        blatResultInputStream.close();
        output.close();
        return allRes;
    }

    /**
     * @param output
     * @param allRes
     * @throws IOException
     */
    private void printBlatAssociations( Writer output, Map<String, Collection<BlatAssociation>> allRes )
            throws IOException {
        for ( Collection<BlatAssociation> associtions : allRes.values() ) {
            for ( BlatAssociation blatAssociation : associtions ) {

                if ( output != null ) {
                    this.writeDesignElementBlatAssociation( output, blatAssociation );
                }
            }
        }
    }

    /**
     * @param stream containing genbank accessions, one per line.
     * @param writer
     * @return
     */
    public Map<String, Collection<BlatAssociation>> runOnGbIds( InputStream stream, Writer writer ) throws IOException,
            SQLException {
        GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( port, databaseName, host, username,
                password );

        TabDelimParser parser = new TabDelimParser();
        parser.parse( stream );

        writeHeader( writer );

        Collection<String[]> genbankIds = parser.getResults();

        log.debug( "Parsed " + genbankIds.size() + " lines from the stream" );

        Map<String, Collection<BlatAssociation>> allRes = probeMapper.processGbIds( goldenPathDb, genbankIds );

        printBlatAssociations( writer, allRes );

        stream.close();
        writer.close();
        return allRes;
    }

    /**
     * @param blatRes
     * @return
     */
    private String[] splitBlatQueryName( BlatResult blatRes ) {
        assert blatRes != null;
        assert blatRes.getQuerySequence() != null;
        String qName = blatRes.getQuerySequence().getName();
        String[] sa = qName.split( ":" );
        // if ( sa.length < 2 ) throw new IllegalArgumentException( "Expected query name in format 'xxx:xxx'" );
        return sa;
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
