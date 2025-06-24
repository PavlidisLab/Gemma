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
package ubic.gemma.core.analysis.sequence;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.util.Assert;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.genome.BlatResultParser;
import ubic.gemma.core.profiling.StopWatchUtils;
import ubic.gemma.model.common.description.DatabaseType;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ShellUtils;

import javax.annotation.Nullable;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Class to manage the gfServer and run BLAT searches. Delegates to the command-line shell to run blat.
 *
 * @author pavlidis
 */
@Getter
@CommonsLog
public class ShellDelegatingBlat implements Blat {

    /**
     * Interval in milliseconds to report on BLAT progress by peeking at its output file.
     */
    private static final int BLAT_UPDATE_INTERVAL_MS = 1000 * 30;
    /**
     * Minimum alignment length for retention.
     */
    private static final int MIN_SCORE = 16;
    /**
     * Strings of As or Ts at the start or end of a sequence longer than this will be stripped off prior to analysis.
     */
    private static final int POLY_AT_THRESHOLD = 5;

    private static final int STEPSIZE = 7;

    // typical values.
    private final String gfClientExe;
    private final String gfServerExe;
    private final String host;
    private final int humanSensitiveServerPort;
    private final String[] humanSeqFiles;
    private final int humanServerPort;
    private final int mouseSensitiveServerPort;
    private final String[] mouseSeqFiles;
    private final int mouseServerPort;
    private final int ratSensitiveServerPort;
    private final String[] ratSeqFiles;
    private final int ratServerPort;
    private final Path seqDir;
    private final Path tmpDir;

    @Setter
    private double blatScoreThreshold = Blat.DEFAULT_BLAT_SCORE_THRESHOLD;

    @Nullable
    private Process serverProcess;
    private String serverHost;
    private int serverPort;

    /**
     * Create a blat object with settings read from the config file.
     */
    public ShellDelegatingBlat() {
        ShellDelegatingBlat.log.debug( "Reading global config" );
        this.humanServerPort = Settings.getInt( "gfClient.humanServerPort" );
        this.mouseServerPort = Settings.getInt( "gfClient.mouseServerPort" );
        this.ratServerPort = Settings.getInt( "gfClient.ratServerPort" );
        this.humanSensitiveServerPort = Settings.getInt( "gfClient.sensitive.humanServerPort" );
        this.mouseSensitiveServerPort = Settings.getInt( "gfClient.sensitive.mouseServerPort" );
        this.ratSensitiveServerPort = Settings.getInt( "gfClient.sensitive.ratServerPort" );
        this.host = Settings.getString( "gfClient.host" );
        this.seqDir = Paths.get( Settings.getString( "gfClient.seqDir" ) );
        this.tmpDir = Paths.get( Settings.getDownloadPath() );
        this.mouseSeqFiles = Settings.getStringArray( "gfClient.mouse.seqFiles" );
        this.ratSeqFiles = Settings.getStringArray( "gfClient.rat.seqFiles" );
        this.humanSeqFiles = Settings.getStringArray( "gfClient.human.seqFiles" );
        this.gfClientExe = Settings.getString( "gfClient.exe" );
        this.gfServerExe = Settings.getString( "gfServer.exe" );
        if ( gfServerExe == null ) {
            /*
             * This won't ever really work -- it's left over from earlier iterations.
             */
            ShellDelegatingBlat.log
                    .warn( "You will not be able to start the server: gfServer.exe is not set in config" );
        }
    }

    public static ExternalDatabase getSearchedGenome( Taxon taxon ) {
        BlattableGenome genome = ShellDelegatingBlat.inferBlatDatabase( taxon );
        ExternalDatabase searchedDatabase = ExternalDatabase.Factory.newInstance();
        searchedDatabase.setType( DatabaseType.SEQUENCE );
        searchedDatabase.setName( genome.toString().toLowerCase() );
        return searchedDatabase;
    }

    private static BlattableGenome inferBlatDatabase( Taxon taxon ) {
        assert taxon != null;
        BlattableGenome bg;
        if ( Objects.equals( taxon.getNcbiId(), 10090 ) || Objects.equals( taxon.getCommonName(), "mouse" ) ) {
            bg = BlattableGenome.MOUSE;
        } else if ( Objects.equals( taxon.getNcbiId(), 10116 ) || Objects.equals( taxon.getCommonName(), "rat" ) ) {
            bg = BlattableGenome.RAT;
        } else if ( Objects.equals( taxon.getNcbiId(), 9606 ) || Objects.equals( taxon.getCommonName(), "human" ) ) {
            bg = BlattableGenome.HUMAN;
        } else {
            throw new UnsupportedOperationException( "Cannot determine which database to search for " + taxon );
        }
        return bg;
    }

    @Override
    public List<BlatResult> blatQuery( BioSequence b ) throws IOException {
        Taxon t = b.getTaxon();
        if ( t == null ) {
            throw new IllegalArgumentException( "Cannot blat sequence unless taxon is given or inferrable" );
        }

        return this.blatQuery( b, t, false );
    }

    @Override
    public List<BlatResult> blatQuery( BioSequence b, Taxon taxon, boolean sensitive ) throws IOException {
        assert seqDir != null;
        // write the sequence to a temporary file.
        String seqName = b.getName().replaceAll( " ", "_" );
        Path querySequenceFile = Files.createTempFile( seqName, ".fa" );

        try ( BufferedWriter out = Files.newBufferedWriter( querySequenceFile ) ) {
            String trimmed = SequenceManipulation
                    .stripPolyAorT( b.getSequence(), ShellDelegatingBlat.POLY_AT_THRESHOLD );
            out.write( ">" + seqName + "\n" + trimmed );
            ShellDelegatingBlat.log.info( "Wrote sequence to " + querySequenceFile );
        }
        Path outputPath = this.getTmpPslFilePath( seqName );

        int portToUse = this.choosePortForQuery( taxon, sensitive );
        List<BlatResult> results = execGfClient( querySequenceFile, outputPath, portToUse, taxon );

        ExternalDatabase searchedDatabase = ShellDelegatingBlat.getSearchedGenome( taxon );
        for ( BlatResult result : results ) {
            result.setSearchedDatabase( searchedDatabase );
        }

        this.cleanUpTmpFiles( querySequenceFile, outputPath );
        return results;

    }

    @Override
    public Map<BioSequence, List<BlatResult>> blatQuery( Collection<BioSequence> sequences, boolean sensitive,
            Taxon taxon ) throws IOException {
        Map<BioSequence, List<BlatResult>> results = new HashMap<>();

        Path querySequenceFile = Files.createTempFile( "sequences-for-blat", ".fa" );
        int count = SequenceWriter.writeSequencesToFile( sequences, querySequenceFile.toFile() );
        if ( count == 0 ) {
            Files.delete( querySequenceFile );
            throw new IllegalArgumentException( "No sequences!" );
        }

        Path outputPath = this.getTmpPslFilePath( "blat-output" );

        int port = this.choosePortForQuery( taxon, sensitive );

        Collection<BlatResult> rawResults = execGfClient( querySequenceFile, outputPath, port, taxon );

        ShellDelegatingBlat.log.info( "Got " + rawResults.size() + " raw blat results" );

        ExternalDatabase searchedDatabase = ShellDelegatingBlat.getSearchedGenome( taxon );

        for ( BlatResult blatResult : rawResults ) {
            blatResult.setSearchedDatabase( searchedDatabase );

            BioSequence query = blatResult.getQuerySequence();

            if ( !results.containsKey( query ) ) {
                results.put( query, new ArrayList<>() );
            }

            results.get( query ).add( blatResult );
        }
        Files.delete( querySequenceFile );
        return results;
    }

    @Override
    public Map<BioSequence, List<BlatResult>> blatQuery( Collection<BioSequence> sequences, Taxon taxon )
            throws IOException {
        return this.blatQuery( sequences, false, taxon );
    }

    /**
     * Process the output of a BLAT search in psl format.
     * @param inputStream to the Blat output file in psl format
     * @param taxon       taxon
     * @return processed results.
     * @throws IOException when there are IO problems.
     */
    public List<BlatResult> processPsl( InputStream inputStream, Taxon taxon ) throws IOException {

        if ( inputStream.available() == 0 ) {
            throw new IOException( "No data from the blat output file. Make sure the gfServer is running" );
        }

        ShellDelegatingBlat.log.debug( "Processing " + inputStream );
        BlatResultParser brp = new BlatResultParser();
        brp.setTaxon( taxon );
        brp.setScoreThreshold( this.blatScoreThreshold );
        brp.parse( inputStream );
        ShellDelegatingBlat.log
                .info( brp.getNumSkipped() + " results were skipped as being below score= " + this.blatScoreThreshold
                        + "; " + brp.getResults().size() + " results retained" );
        return brp.getResults();
    }

    /**
     * Start the server, if the port isn't already being used. If the port is in use, we assume it is a gfServer.
     *
     * @param genome genome
     * @param waitForFullInitialization if true, wait for the server to be fully initialized before returning, otherwise
     *                                  return immediately after starting the server.
     * @throws IOException when there are IO problems.
     */
    public synchronized void startServer( BlattableGenome genome, boolean sensitive, boolean waitForFullInitialization ) throws IOException {
        Assert.state( serverProcess == null || !serverProcess.isAlive() );
        if ( sensitive ) {
            // TODO: implement sensitive searches
            throw new UnsupportedOperationException( "Sensitive BLAT searches are not supported by this implementation." );
        }
        int port = getPort( genome, sensitive );
        // check if a server is already running
        try ( Socket ignored = new Socket( host, port ) ) {
            throw new RuntimeException( "There is already a gfServer listening on " + host + ":" + port + "." );
        } catch ( IOException e ) {
            // ignore all other errors, the blat server is probably not running
        }
        String[] cmd = ArrayUtils.addAll( new String[] {
                gfServerExe, "-stepSize=" + STEPSIZE, "start", this.host, String.valueOf( port ) }, this.getSeqFiles( genome ) );
        ShellDelegatingBlat.log.info( "Starting gfServer with command: " + ShellUtils.join( cmd ) );
        this.serverProcess = new ProcessBuilder( cmd )
                .directory( seqDir.toFile() )
                .redirectOutput( ProcessBuilder.Redirect.INHERIT )
                .redirectError( ProcessBuilder.Redirect.PIPE )
                .start();
        this.serverHost = host;
        this.serverPort = port;

        // wait a little bit to see if the server fails early (i.e. incorrect parameters)
        try {
            if ( serverProcess.waitFor( 100, TimeUnit.MILLISECONDS ) ) {
                String errorMessage = StringUtils.strip( IOUtils.toString( serverProcess.getErrorStream(), StandardCharsets.UTF_8 ) );
                throw new RuntimeException( "Could not start gfServer (exit value=" + serverProcess.exitValue() + "):\n" + errorMessage );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }

        if ( waitForFullInitialization ) {
            log.info( "Waiting for gfServer to be fully initialized on " + serverHost + ":" + serverPort + "..." );
            while ( true ) {
                if ( isServerReachable( serverHost, serverPort ) ) {
                    log.info( "gfServer is listening on " + serverHost + ":" + serverPort + "." );
                    break;
                }
            }
        }
    }

    /**
     * Check if the gfServer is running.
     */
    public boolean isServerRunning() {
        return serverProcess != null && serverProcess.isAlive() && isServerReachable( serverHost, serverPort );
    }

    /**
     * Check if the gfServer for a given genome is reachable.
     */
    public boolean isServerReachable( BlattableGenome genome, boolean sensitive ) {
        return isServerReachable( host, getPort( genome, sensitive ) );
    }

    /**
     * Check if a gfServer is reachable.
     */
    private boolean isServerReachable( String host, int port ) {
        // try to connect to the server to ensure it is running
        try ( Socket ignored = new Socket( host, port ) ) {
            return true;
        } catch ( IOException e ) {
            return false;
            // ignore all other errors, the blat server is probably not running
        }
    }

    /**
     * Stop the gfServer, if it was started by this.
     */
    public synchronized void stopServer() {
        if ( serverProcess == null ) {
            log.warn( "gfServer was not started, nothing to stop." );
            return;
        } else if ( !serverProcess.isAlive() ) {
            log.info( "gfServer is not running, nothing to stop." );
            return;
        }

        ShellDelegatingBlat.log.info( "Shutting down gfServer at " + serverHost + ":" + serverPort + "..." );

        try {
            // gracefully stop the server
            serverProcess.destroy();
            // give the server 30 seconds to shut down
            if ( serverProcess.waitFor( 30, TimeUnit.SECONDS ) ) {
                int serverExitCode = serverProcess.exitValue();
                // 143 is the exit code for SIGTERM, which is what destroy() sends
                if ( serverExitCode == 0 || serverExitCode == 143 ) {
                    ShellDelegatingBlat.log.info( "gfServer on port " + serverPort + " shut down with exit value " + serverExitCode );
                } else {
                    String errorMessage;
                    try {
                        errorMessage = IOUtils.toString( serverProcess.getErrorStream(), StandardCharsets.UTF_8 );
                    } catch ( IOException e ) {
                        errorMessage = "Could not read error stream from gfServer process.";
                    }
                    ShellDelegatingBlat.log.info( "gfServer on port " + serverPort + " shut down with exit value " + serverExitCode + "\n" + errorMessage );
                }
            } else {
                log.warn( "gfServer did not shut down in time, killing it..." );
                serverProcess.destroyForcibly();
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }

    private int choosePortForQuery( Taxon taxon, boolean sensitive ) {
        BlattableGenome genome = ShellDelegatingBlat.inferBlatDatabase( taxon );
        return getPort( genome, sensitive );
    }

    private int getPort( BlattableGenome genome, boolean sensitive ) {
        switch ( genome ) {
            case MOUSE:
                return sensitive ? mouseSensitiveServerPort : mouseServerPort;
            case RAT:
                return sensitive ? ratSensitiveServerPort : ratServerPort;
            case HUMAN:
            default:
                return sensitive ? humanSensitiveServerPort : humanServerPort;
        }
    }

    private String[] getSeqFiles( BlattableGenome genome ) {
        switch ( genome ) {
            case HUMAN:
                return this.humanSeqFiles;
            case MOUSE:
                return this.mouseSeqFiles;
            case RAT:
                return this.ratSeqFiles;
            default:
                return this.humanSeqFiles;

        }
    }

    private void cleanUpTmpFiles( Path querySequenceFile, Path outputPath ) {
        try {
            Files.deleteIfExists( querySequenceFile );
            Files.deleteIfExists( outputPath );
        } catch ( IOException e ) {
            ShellDelegatingBlat.log.warn( "Could not clean up temporary files.", e );
        }
    }

    /**
     * Run a gfClient query, using a call to exec().
     *
     * @param querySequenceFile query sequence file
     * @param outputPath        output path
     * @return collection of blat results
     */
    private List<BlatResult> execGfClient( Path querySequenceFile, Path outputPath, int portToUse, Taxon taxon )
            throws IOException {
        StopWatch overallWatch = StopWatch.createStarted();

        final String[] cmd = new String[] {
                gfClientExe, "-nohead", "-minScore=" + ShellDelegatingBlat.MIN_SCORE, host, String.valueOf( portToUse ),
                seqDir.toString(), querySequenceFile.toString(), outputPath.toString() };
        ShellDelegatingBlat.log.info( ShellUtils.join( cmd ) );
        final Process run = new ProcessBuilder( cmd )
                // to ensure that we aren't left waiting for these streams
                // TODO: switch to Redirect.DISCARD for Java 9+
                .redirectOutput( ProcessBuilder.Redirect.appendTo( new File( "/dev/null" ) ) )
                .redirectError( ProcessBuilder.Redirect.PIPE )
                .start();
        // wait...
        try {
            while ( !run.waitFor( ShellDelegatingBlat.BLAT_UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS ) ) {
                // I hope this is okay...
                this.checkOutputFile( outputPath, overallWatch );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }

        int exitVal = run.exitValue();
        if ( exitVal != 0 ) {
            String errorMessage = StringUtils.strip( IOUtils.toString( run.getErrorStream(), StandardCharsets.UTF_8 ) );
            throw new RuntimeException( "gfClient exited with " + exitVal + ":\n" + errorMessage );
        }

        overallWatch.stop();
        ShellDelegatingBlat.log.info( "Blat query took a total of " + overallWatch );
        ShellDelegatingBlat.log.debug( "GfClient Success" );
        return this.processPsl( outputPath, taxon );
    }


    private synchronized void checkOutputFile( final Path outputPath, StopWatch overallWatch ) {
        try {
            long size = Files.size( outputPath );
            NumberFormat nf = new DecimalFormat();
            nf.setMaximumFractionDigits( 2 );
            String minutes = StopWatchUtils.getMinutesElapsed( overallWatch );
            ShellDelegatingBlat.log
                    .info( "BLAT output so far: " + nf.format( size / 1024.0 ) + " kb (" + minutes + " minutes elapsed)" );
        } catch ( IOException e ) {
            ShellDelegatingBlat.log.warn( "Failed to check BLAT output file: " + outputPath, e );
        }
    }

    /**
     * Get a temporary file name.
     *
     * @throws IOException if there is an IO problem while accessing the file
     */
    private Path getTmpPslFilePath( String base ) throws IOException {
        return Files.createTempFile( tmpDir, StringUtils.isBlank( base ) ? "blat-output" : base, ".psl" );
    }

    /**
     * @param filePath to the Blat output file in psl format
     * @param taxon    taxon (optional, can be null)
     * @return processed results.
     */
    private List<BlatResult> processPsl( Path filePath, Taxon taxon ) throws IOException {
        ShellDelegatingBlat.log.debug( "Processing " + filePath );
        BlatResultParser brp = new BlatResultParser();
        brp.setTaxon( taxon );
        brp.setScoreThreshold( this.blatScoreThreshold );
        brp.parse( filePath.toFile() );
        return brp.getResults();
    }

    public enum BlattableGenome {
        HUMAN, MOUSE, RAT
    }
}
