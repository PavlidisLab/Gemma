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
import java.io.FileNotFoundException;
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

import ubic.gemma.analysis.sequence.BlatAssociationScorer;
import ubic.gemma.analysis.sequence.ProbeMapper;
import ubic.gemma.externalDb.GoldenPathSequenceAnalysis;
import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.loader.genome.FastaParser;
import ubic.gemma.loader.util.parser.TabDelimParser;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResultService;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Given a blat result set for an array design, annotate and find the 3' locations for all the really good hits. This
 * CLI was written to take a file as input, and write results to a file or standard out, but has been modified to
 * process sequence ids from a file and load the results into the db.
 * <p>
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProbeMapperCli extends AbstractSpringAwareCLI {

    ProbeMapper probeMapper;

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
            log.error( e, e );
        }
    }

    private String databaseName = DEFAULT_DATABASE;

    private String outputFileName = null;

    private String blatFileName = null;

    private String ncbiIdentifierFileName = null;

    private String fastaFileName = null;

    private String sequenceIdentifierFileName = null;

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

        addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription(
                "File containing BioSequence primary keys (results are saved to database)" ).create( "seqIds" ) );

        addOption( OptionBuilder.hasArg().withArgName( "file name" ).withDescription( "Output file basename" )
                .withLongOpt( "outputFile" ).create( 'o' ) );

        addOption( databaseNameOption );

    }

    @Override
    protected Exception doWork( String[] args ) {

        try {
            Exception err = processCommandLine( "probeMapper", args );
            if ( err != null ) return err;

            Writer resultsOut = null;
            Writer bestResultsOut = null;
            if ( outputFileName != null ) {
                String bestOutputFileName = outputFileName + ".best";
                log.info( "Saving best to " + bestOutputFileName );

                resultsOut = getWriterForBestResults( outputFileName );
                bestResultsOut = getWriterForBestResults( bestOutputFileName );
            }

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
            } else if ( this.sequenceIdentifierFileName != null ) {

                runOnBioSequences();

            } else {
                String[] moreArgs = getArgs();
                if ( moreArgs.length == 0 ) {
                    System.out
                            .println( "You must provide either a Blat result file, a FASTA file, a Genbank identifier file, or some Genbank identifiers" );
                    printHelp( "probeMapper" );
                    return new Exception( "Missing genbank identifiers" );
                }

                GoldenPathSequenceAnalysis goldenPathDb;

                goldenPathDb = new GoldenPathSequenceAnalysis( this.databaseName );

                for ( int i = 0; i < moreArgs.length; i++ ) {
                    String gbId = moreArgs[i];

                    log.debug( "Got " + gbId );
                    Map<String, Collection<BlatAssociation>> results = probeMapper.processGbId( goldenPathDb, gbId );

                    printBlatAssociations( resultsOut, results );
                    printBestResults( results, bestResultsOut );
                }
            }

            if ( resultsOut != null ) resultsOut.close();
            if ( bestResultsOut != null ) bestResultsOut.close();

        } catch ( Exception e ) {
            return new RuntimeException( e );
        }
        return null;
    }

    /**
     * @throws SQLException
     * @throws FileNotFoundException
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private void runOnBioSequences() throws SQLException, FileNotFoundException, IOException {
        GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( this.databaseName );

        InputStream stream = new FileInputStream( new File( this.sequenceIdentifierFileName ) );
        TabDelimParser parser = new TabDelimParser();
        parser.parse( stream );

        Collection<String[]> seqIds = parser.getResults();

        log.info( seqIds.size() + " ids found in file" );

        BioSequenceService bss = ( BioSequenceService ) this.getBean( "bioSequenceService" );
        PersisterHelper persisterHelper = ( PersisterHelper ) this.getBean( "persisterHelper" );
        BlatResultService blatResultService = ( BlatResultService ) this.getBean( "blatResultService" );

        int count = 0;
        int hits = 0;
        for ( String[] strings : seqIds ) {
            try {
                if ( strings.length == 0 ) continue;
                String idString = strings[0];
                Long id = Long.parseLong( idString );
                BioSequence bs = bss.load( id );
                bss.thaw( bs );

                if ( bs == null ) continue;

                final Collection<BlatResult> blatResults = blatResultService.findByBioSequence( bs );

                if ( blatResults == null || blatResults.isEmpty() ) continue;

                Map<String, Collection<BlatAssociation>> results = probeMapper.processBlatResults( goldenPathDb,
                        blatResults );

                for ( Collection<BlatAssociation> col : results.values() ) {
                    for ( BlatAssociation association : col ) {
                        if ( log.isDebugEnabled() ) log.debug( association );
                    }

                    persisterHelper.persist( col );
                    ++hits;
                }

                if ( ++count % 100 == 0 ) {
                    log
                            .info( "Processed " + count + "  sequences" + " with blat results; " + hits
                                    + " mappings found." );
                }

            } catch ( NumberFormatException e ) {
                log.warn( strings[0] + " was not a number" );
            }
        }
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
            BlatAssociation best = BlatAssociationScorer.scoreResults( results.get( probe ) );
            if ( best == null ) {
                continue;
            }
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
            GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( this.databaseName );

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
        super.processOptions();

        probeMapper = ( ProbeMapper ) this.getBean( "probeMapper" );

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

        if ( hasOption( "seqIds" ) ) {
            this.sequenceIdentifierFileName = getFileNameOptionValue( "seqIds" );
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

        GoldenPathSequenceAnalysis goldenPathAnalysis = new GoldenPathSequenceAnalysis( this.databaseName );

        BlatResultParser brp = new BlatResultParser();
        brp.parse( blatResultInputStream );

        writeHeader( output );

        Collection<BlatResult> blatResults = brp.getResults();

        Map<String, Collection<BlatAssociation>> allRes = probeMapper.processBlatResults( goldenPathAnalysis,
                blatResults );

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
        if ( output == null ) return;
        for ( Collection<BlatAssociation> associations : allRes.values() ) {
            for ( BlatAssociation blatAssociation : associations ) {
                this.writeDesignElementBlatAssociation( output, blatAssociation );
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
        GoldenPathSequenceAnalysis goldenPathDb = new GoldenPathSequenceAnalysis( this.databaseName );

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
