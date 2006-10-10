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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.genome.BlatResultParser;
import ubic.gemma.loader.util.concurrent.GenericStreamConsumer;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.util.ConfigUtils;

/**
 * Class to manage the gfServer and run BLAT searches.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class Blat {

    private static final Log log = LogFactory.getLog( Blat.class );
    private static final double BLAT_SCORE_THRESHOLD = 0.8; // FIXME - make this user-modifiable.
    private static String os = System.getProperty( "os.name" ).toLowerCase();

    public static enum BlattableGenome {
        HUMAN, MOUSE, RAT
    };

    static {
        if ( !os.toLowerCase().startsWith( "windows" ) ) {
            try {
                log.debug( "Loading gfClient library, looking in " + System.getProperty( "java.library.path" ) );
                System.loadLibrary( "Blat" );
                log.info( "Loaded Blat native library successfully" );
            } catch ( UnsatisfiedLinkError e ) {
                log.error( e, e );
                throw new ExceptionInInitializerError( "Unable to locate or load the Blat native library: "
                        + e.getMessage() );
            }
        }
    }
    private boolean doShutdown = true;

    // typical values.
    private String gfClientExe = "/cygdrive/c/cygwin/usr/local/bin/gfClient.exe";
    private String gfServerExe = "/cygdrive/c/cygwin/usr/local/bin/gfServer.exe";
    private String host = "localhost";
    private String seqDir = "/";

    private String humanSeqFiles;
    private String ratSeqFiles;
    private String mouseSeqFiles;

    private Process serverProcess;
    private int humanServerPort;
    private int mouseServerPort;
    private int ratServerPort;

    private String humanServerHost;
    private String mouseServerHost;
    private String ratServerHost;

    /**
     * Create a blat object with settings read from the config file.
     */
    public Blat() {
        try {
            init();
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( "Could not load configuration", e );
        }
    }

    /**
     * @param host
     * @param port
     * @param seqDir
     */
    public Blat( String host, int humanServerPort, String seqDir ) {

        if ( host == null || humanServerPort <= 0 || seqDir == null )
            throw new IllegalArgumentException( "All values must be non-null" );
        this.host = host;
        this.humanServerPort = humanServerPort;
        this.seqDir = seqDir;
    }

    /**
     * @return Returns the gfClientExe.
     */
    public String getGfClientExe() {
        return this.gfClientExe;
    }

    /**
     * @return Returns the gfServerExe.
     */
    public String getGfServerExe() {
        return this.gfServerExe;
    }

    /**
     * @return Returns the host.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * @return Returns the seqDir.
     */
    public String getSeqDir() {
        return this.seqDir;
    }

    /**
     * @return Returns the seqFiles.
     */
    public String getSeqFiles( BlattableGenome genome ) {
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

    /**
     * Run a BLAT search using the gfClient.
     * 
     * @param b
     * @param genome
     * @return Collection of BlatResult objects.
     * @throws IOException
     */
    public Collection<BlatResult> blatQuery( BioSequence b, BlattableGenome genome ) throws IOException {
        assert seqDir != null;
        // write the sequence to a temporary file.
        File querySequenceFile = File.createTempFile( "pattern", ".fa" );

        BufferedWriter out = new BufferedWriter( new FileWriter( querySequenceFile ) );
        out.write( ">" + b.getName() + "\n" + b.getSequence() );
        out.close();
        log.info( "Wrote sequence to " + querySequenceFile.getPath() );

        String outputPath = getTmpPslFilePath();

        Collection<BlatResult> results = gfClient( querySequenceFile, outputPath, choosePortForQuery( genome ) );

        cleanUpTmpFiles( querySequenceFile, outputPath );
        return results;

    }

    /**
     * Run a BLAT search using the gfClient.
     * 
     * @param b. The genome is inferred from the Taxon held by the sequence.
     * @return Collection of BlatResult objects.
     * @throws IOException
     */
    public Collection<BlatResult> blatQuery( BioSequence b ) throws IOException {
        Taxon t = b.getTaxon();
        if ( t == null ) {
            throw new IllegalArgumentException( "Cannot blat sequence unless taxon is given or inferrable" );
        }

        // FIXME - this should not be hard coded like this, what happens when more genomes are added.
        BlattableGenome g = BlattableGenome.MOUSE;
        if ( t.getNcbiId() == 10090 ) {
            g = BlattableGenome.MOUSE;
        } else if ( t.getNcbiId() == 9606 ) {
            g = BlattableGenome.HUMAN;
        } else if ( t.getNcbiId() == 10116 ) {
            g = BlattableGenome.RAT;
        } else {
            throw new IllegalArgumentException( "Unsupported taxon " + t );
        }

        return blatQuery( b, g );
    }

    /**
     * @param genome
     * @return
     */
    private int choosePortForQuery( BlattableGenome genome ) {
        switch ( genome ) {
            case HUMAN:
                return humanServerPort;
            case MOUSE:
                return mouseServerPort;
            case RAT:
                return ratServerPort;
            default:
                return humanServerPort;

        }
    }

    /**
     * @param querySequenceFile
     * @param outputPath
     */
    private void cleanUpTmpFiles( File querySequenceFile, String outputPath ) {
        if ( !querySequenceFile.delete() || !( new File( outputPath ) ).delete() ) {
            log.warn( "Could not clean up temporary files." );
        }
    }

    /**
     * @param sequences
     * @return map of the input sequence names to a corresponding collection of blat result(s)
     * @throws IOException
     */
    public Map<String, Collection<BlatResult>> blatQuery( Collection<BioSequence> sequences, BlattableGenome genome )
            throws IOException {
        Map<String, Collection<BlatResult>> results = new HashMap<String, Collection<BlatResult>>();

        File querySequenceFile = File.createTempFile( "pattern", ".fa" );
        BufferedWriter out = new BufferedWriter( new FileWriter( querySequenceFile ) );

        /*
         * Note this silliness. Often the sequences have been read in from a file in the first place. The problem is
         * there are no easy hooks to gfClient that don't use a file. This could be changed at a later time. It would
         * require customizing Kent's code (even more than we do).
         */

        log.debug( "Processing " + sequences.size() + " sequences for blat analysis" );
        for ( BioSequence b : sequences ) {
            out.write( ">" + b.getName() + "\n" + b.getSequence() + "\n" );
        }
        out.close();

        String outputPath = getTmpPslFilePath();

        Collection<BlatResult> rawresults = gfClient( querySequenceFile, outputPath, choosePortForQuery( genome ) );

        log.debug( "Got" + rawresults.size() + " raw blat results" );

        for ( BlatResult blatResult : rawresults ) {
            String name = blatResult.getQuerySequence().getName();

            if ( !results.containsKey( name ) ) {
                results.put( name, new HashSet<BlatResult>() );
            }

            results.get( name ).add( blatResult );
        }

        querySequenceFile.delete();
        return results;
    }

    /**
     * Start the server, if the port isn't already being used. If the port is in use, we assume it is a gfServer.
     */
    public void startServer( BlattableGenome genome, int port ) throws IOException {
        try {
            new Socket( host, port );
            log.info( "There is already a server on port " + port );
            this.doShutdown = false;
        } catch ( UnknownHostException e ) {
            throw new RuntimeException( "Unknown host " + host, e );
        } catch ( IOException e ) {
            String cmd = this.getGfServerExe() + " -canStop start " + this.getHost() + " " + port + " "
                    + this.getSeqFiles( genome );
            log.info( "Starting gfServer with command " + cmd );
            this.serverProcess = Runtime.getRuntime().exec( cmd, null, new File( this.getSeqDir() ) );

            try {
                Thread.sleep( 100 );
                int exit = serverProcess.exitValue();
                if ( exit != 0 ) {
                    throw new IOException( "Could not start server" );
                }
            } catch ( IllegalThreadStateException e1 ) {
                log.info( "Server seems to have started" );
            } catch ( InterruptedException e1 ) {
                ;
            }

        }
    }

    /**
     * Stop the gfServer, if it was started by this.
     */
    public void stopServer( int port ) {
        if ( false && !doShutdown ) {
            return;
        }
        log.info( "Shutting down gfServer" );

        if ( serverProcess == null ) return;
        // serverProcess.destroy();
        try {
            // this doesn't work unless the server was invoked with the option "-canStop"
            Process server = Runtime.getRuntime().exec( this.getGfServerExe() + " stop " + this.getHost() + " " + port );
            server.waitFor();
            int exit = server.exitValue();
            log.info( "Server on port " + port + " shut down with exit value " + exit );
        } catch ( InterruptedException e ) {
            log.error( e, e );
        } catch ( IOException e ) {
            log.error( e, e );
        }

    }

    /**
     * Run a gfClient query, using a call to exec(). This runs in a separate thread.
     * 
     * @param querySequenceFile
     * @param outputPath
     * @return
     */
    private Collection<BlatResult> execGfClient( File querySequenceFile, String outputPath, int portToUse )
            throws IOException {
        final String cmd = gfClientExe + " -nohead " + host + " " + portToUse + " " + seqDir + " "
                + querySequenceFile.getAbsolutePath() + " " + outputPath;

        FutureTask<Process> future = new FutureTask<Process>( new Callable<Process>() {
            public Process call() throws IOException {
                try {
                    Process run = Runtime.getRuntime().exec( cmd );

                    GenericStreamConsumer gsc = new GenericStreamConsumer( run.getErrorStream() );
                    gsc.start();

                    try {
                        run.waitFor();
                    } catch ( InterruptedException e ) {
                        ;
                    }
                    return run;
                } catch ( IOException e ) {
                    String message = e.getMessage();
                    if ( message.startsWith( "CreateProcess" ) && message.indexOf( "error=" ) > 0 ) {
                        int errorCode = Integer.parseInt( message.substring( e.getMessage().lastIndexOf( "error=" )
                                + ( new String( "error=" ) ).length() ) );

                        if ( errorCode == 2 )
                            throw new IOException( "Could not locate executable to run command (Error " + errorCode
                                    + ") " + cmd );

                        throw new IOException( "Error (" + errorCode + ") " + cmd );
                    }
                    return null;
                }
            }
        } );

        Executors.newSingleThreadExecutor().execute( future );
        while ( !future.isDone() ) {
            try {
                Thread.sleep( 10 );
            } catch ( InterruptedException ie ) {
                ;
            }
        }

        try {

            if ( future.get() == null ) {
                log.error( "GfClient Failed" );
                throw new RuntimeException( "GfClient Failed" );
            } else if ( future.get().exitValue() != 0 ) {

                StringBuilder buf = getErrOutput( future );

                throw new RuntimeException( "GfClient Error on command : " + cmd + " exit value: "
                        + future.get().exitValue() + " error:" + buf );
            } else {
                log.info( "GfClient Success" );
            }
        } catch ( ExecutionException e ) {
            log.error( e, e );
            throw new RuntimeException( "GfClient Failed", e );
        } catch ( InterruptedException e ) {
            log.error( e, e );
            throw new RuntimeException( "GfClient Failed (Interrupted)", e );
        }
        return processPsl( outputPath, null );
    }

    /**
     * @param future
     * @return
     * @throws InterruptedException
     * @throws ExecutionException
     * @throws IOException
     */
    private StringBuilder getErrOutput( FutureTask<Process> future ) throws InterruptedException, ExecutionException,
            IOException {
        InputStream result = future.get().getErrorStream();
        BufferedReader br = new BufferedReader( new InputStreamReader( result ) );
        String l = null;
        StringBuilder buf = new StringBuilder();
        while ( ( l = br.readLine() ) != null ) {
            buf.append( l + "\n" );
        }
        br.close();
        return buf;
    }

    /**
     * Get a temporary file name.
     * 
     * @throws IOException
     */
    private String getTmpPslFilePath() throws IOException {
        return File.createTempFile( "pattern", ".psl" ).getPath();
    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @return processed results.
     * @throws IOException
     */
    private Collection<BlatResult> gfClient( File querySequenceFile, String outputPath, int portToUse )
            throws IOException {
        if ( !os.startsWith( "windows" ) ) return jniGfClientCall( querySequenceFile, outputPath, portToUse );

        return execGfClient( querySequenceFile, outputPath, portToUse );
    }

    /**
     * @param host
     * @param port
     * @param seqDir
     * @param inputFile
     * @param outputFile
     */
    private native void GfClientCall( String h, String p, String dir, String input, String output );

    /**
     * @throws ConfigurationException
     */
    private void init() throws ConfigurationException {

        log.debug( "Reading global config" );
        this.humanServerPort = ConfigUtils.getInt( "gfClient.humanServerPort" );
        this.mouseServerPort = ConfigUtils.getInt( "gfClient.mouseServerPort" );
        this.ratServerPort = ConfigUtils.getInt( "gfClient.ratServerPort" );
        this.humanServerHost = ConfigUtils.getString( "gfClient.humanServerHost" );
        this.mouseServerHost = ConfigUtils.getString( "gfClient.mouseServerHost" );
        this.ratServerHost = ConfigUtils.getString( "gfClient.ratServerHost" );
        this.host = ConfigUtils.getString( "gfClient.host" );
        this.seqDir = ConfigUtils.getString( "gfClient.seqDir" );
        this.mouseSeqFiles = ConfigUtils.getString( "gfClient.mouse.seqFiles" );
        this.ratSeqFiles = ConfigUtils.getString( "gfClient.rat.seqFiles" );
        this.humanSeqFiles = ConfigUtils.getString( "gfClient.human.seqFiles" );
        this.gfClientExe = ConfigUtils.getString( "gfClient.exe" );
        this.gfServerExe = ConfigUtils.getString( "gfServer.exe" );

        if ( gfServerExe == null ) {
            log.warn( "You will not be able to start the server due to a configuration error." );
        }

        if ( gfClientExe == null && os.startsWith( "windows" ) ) {
            throw new ConfigurationException( "BLAT client calls will not work under windows." );
        }

    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @return processed results.
     */
    private Collection<BlatResult> jniGfClientCall( File querySequenceFile, String outputPath, int portToUse )
            throws IOException {
        try {
            this.GfClientCall( host, Integer.toString( portToUse ), seqDir, querySequenceFile.getPath(), outputPath );
        } catch ( UnsatisfiedLinkError e ) {
            log.error( e, e );
            log.info( "Falling back on exec()" );
            this.execGfClient( querySequenceFile, outputPath, portToUse );
        }
        return this.processPsl( outputPath, null );
    }

    /**
     * @param outputPath to the Blat output file in psl format
     * @return processed results.
     */
    private Collection<BlatResult> processPsl( String outputPath, Taxon taxon ) throws IOException {
        log.debug( "Processing " + outputPath );
        BlatResultParser brp = new BlatResultParser();
        brp.setTaxon( taxon );
        brp.setScoreThreshold( BLAT_SCORE_THRESHOLD );
        brp.parse( outputPath );
        return brp.getResults();
    }

    /**
     * @return Returns the humanServerPort.
     */
    public int getHumanServerPort() {
        return this.humanServerPort;
    }

    /**
     * @return Returns the mouseServerPort.
     */
    public int getMouseServerPort() {
        return this.mouseServerPort;
    }

    /**
     * @return Returns the ratServerPort.
     */
    public int getRatServerPort() {
        return this.ratServerPort;
    }

}
