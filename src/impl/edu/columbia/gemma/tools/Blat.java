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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.genome.biosequence.BioSequence;
import edu.columbia.gemma.genome.sequenceAnalysis.BlatResult;
import edu.columbia.gemma.loader.genome.BlatResultParser;

/**
 * Class to manage the gfServer and run BLAT searches.
 * <hr>
 * <p>
 * Copyright (c) 2004-2005 Columbia University
 * 
 * @author pavlidis
 * @version $Id$
 */
public class Blat {

    private String gfClientExe = "C:/bin/cygwini686/gfClient.exe";
    private static final Log log = LogFactory.getLog( Blat.class );
    private String host = "localhost";
    private String seqDir = "/";
    private String port = "177777";
    private String gfServerExe = "C:/bin/cygwini686/gfServer.exe";
    private String seqFiles;
    private boolean doShutdown = true;

    private static String os = System.getProperty( "os.name" ).toLowerCase();

    /**
     * @param host
     * @param port
     * @param seqDir
     */
    public Blat( String host, String port, String seqDir ) {

        if ( host == null || port == null || seqDir == null )
            throw new IllegalArgumentException( "All values must be non-null" );
        this.host = host;
        this.port = port;
        this.seqDir = seqDir;
    }

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
     * @throws ConfigurationException
     */
    private void init() throws ConfigurationException {
        URL configFileLocation = ConfigurationUtils.locate( "Gemma.properties" );
        if ( configFileLocation == null ) throw new ConfigurationException( "Doesn't exist" );
        Configuration config = new PropertiesConfiguration( configFileLocation );
        this.port = config.getString( "gfClient.port" );
        this.host = config.getString( "gfClient.host" );
        this.seqDir = config.getString( "gfClient.seqDir" );
        this.seqFiles = config.getString( "gfClient.seqFiles" );
        this.gfClientExe = config.getString( "gfClient.exe" );
        this.gfServerExe = config.getString( "gfServer.exe" );
    }

    /**
     * @param sequences
     * @return map of the input sequence names to a corresponding collection of blat result(s)
     * @throws IOException
     */
    public Map<String, Collection<BlatResult>> GfClient( Collection<BioSequence> sequences ) throws IOException {
        Map<String, Collection<BlatResult>> results = new HashMap<String, Collection<BlatResult>>();

        File querySequenceFile = File.createTempFile( "pattern", ".fa" );
        querySequenceFile.deleteOnExit();
        BufferedWriter out = new BufferedWriter( new FileWriter( querySequenceFile ) );

        for ( BioSequence b : sequences ) {
            out.write( ">" + b.getName() + "\n" + b.getSequence() );
        }
        out.close();

        String outputPath = getTmpPslFilePath();

        gfClient( querySequenceFile, outputPath );

        Collection<Object> rawResults = this.processPsl( outputPath );
        for ( Object object : rawResults ) {
            assert object instanceof BlatResult;
            String name = ( ( BlatResult ) object ).getQuerySequence().getName();

            if ( !results.containsKey( name ) ) {
                results.put( name, new HashSet<BlatResult>() );
            }

            results.get( name ).add( ( BlatResult ) object );
        }

        querySequenceFile.delete();
        return results;
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
     * Run a BLAT search using the gfClient.
     * 
     * @param b
     * @return Collection of BlatResult objects.
     * @throws IOException
     */
    public Collection<Object> GfClient( BioSequence b ) throws IOException {
        assert seqDir != null;
        assert port != null;
        // write the sequence to a temporary file.
        File querySequenceFile = File.createTempFile( "pattern", ".fa" );

        BufferedWriter out = new BufferedWriter( new FileWriter( querySequenceFile ) );
        out.write( ">" + b.getName() + "\n" + b.getSequence() );
        out.close();
        log.info( "Wrote sequence to " + querySequenceFile.getPath() );

        String outputPath = getTmpPslFilePath();

        gfClient( querySequenceFile, outputPath );

        querySequenceFile.delete();
        return processPsl( outputPath );

    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @throws IOException
     */
    private void gfClient( File querySequenceFile, String outputPath ) throws IOException {
        if ( !os.startsWith( "windows" ) ) {
            jniGfClientCall( querySequenceFile, outputPath );
        } else {
            execGfClient( querySequenceFile, outputPath );
        }
    }

    /**
     * @param outputPath
     * @return
     */
    private Collection<Object> processPsl( String outputPath ) throws IOException {
        BlatResultParser brp = new BlatResultParser();
        brp.parse( outputPath );
        return brp.getResults();
    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @return
     */
    private String jniGfClientCall( File querySequenceFile, String outputPath ) throws IOException {
        try {
            this.GfClientCall( host, port, seqDir, querySequenceFile.getPath(), outputPath );
        } catch ( UnsatisfiedLinkError e ) {
            log.error( e, e );
            // throw new RuntimeException( "Failed call to native gfClient: " + e.getMessage() );
            log.info( "Falling back on exec()" );
            this.execGfClient( querySequenceFile, outputPath );
        }
        return outputPath;
    }

    /**
     * Stop the server, if it was started by this.
     */
    public void stopServer() throws IOException {
        if ( !doShutdown ) {
            return;
        }
        log.info( "Shutting down gfServer" );
        Process server = Runtime.getRuntime().exec(
                this.getGfServerExe() + " stop " + this.getHost() + " " + this.getPort() + this.getSeqDir()
                        + this.getSeqFiles() );
        try {
            server.waitFor();
            int exit = server.exitValue();
            log.info( "Server shut down with exit value " + exit );
        } catch ( InterruptedException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Start the server, if the port isn't already being used. If the port is in use, we assume it is a gfServer.
     */
    public void startServer() throws IOException {

        try {
            new Socket( host, Integer.parseInt( port ) );
            log.info( "There is already a server on port " + port );
            this.doShutdown = false;
        } catch ( UnknownHostException e ) {
            throw new RuntimeException( "Unknown host " + host, e );
        } catch ( IOException e ) {
            String cmd = this.getGfServerExe() + " start " + this.getHost() + " " + this.getPort() + " "
                    + this.getSeqDir() + this.getSeqFiles();
            log.info( "Starting gfServer with command " + cmd );
            Runtime.getRuntime().exec( cmd );
        }

    }

    /**
     * @param querySequenceFile
     * @param outputPath
     * @return
     */
    private String execGfClient( File querySequenceFile, String outputPath ) throws IOException {
        final String cmd = gfClientExe + " " + host + " " + port + " " + seqDir + " "
                + querySequenceFile.getAbsolutePath() + " " + outputPath;

        FutureTask<Process> future = new FutureTask<Process>( new Callable<Process>() {
            public Process call() throws IOException {
                try {
                    Process run = Runtime.getRuntime().exec( cmd );
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

                        throw new IOException( "Error " + errorCode + ") " + cmd );
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

                InputStream result = future.get().getErrorStream();
                BufferedReader br = new BufferedReader( new InputStreamReader( result ) );
                String l = null;
                StringBuffer buf = new StringBuffer();
                while ( ( l = br.readLine() ) != null ) {
                    buf.append( l + "\n" );
                }

                log.error( "GfClient Error : " + future.get().exitValue() );
                log.error( "Command was '" + cmd + "'" );
                log.error( "Error message was: " + buf );
                throw new RuntimeException( "GfClient Error : " + future.get().exitValue() );
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
        return outputPath;
        // FIXME, process the results.
    }

    /**
     * @param host
     * @param port
     * @param seqDir
     * @param inputFile
     * @param outputFile
     */
    private native void GfClientCall( String h, String p, String dir, String input, String output );

    static {
        if ( !os.toLowerCase().startsWith( "windows" ) ) {
            try {
                log.info( "Loading gfClient library, looking in " + System.getProperty( "java.library.path" ) );
                System.loadLibrary( "Blat" );
                log.info( "Loaded Blat library successfully" );
            } catch ( UnsatisfiedLinkError e ) {
                log.error( e, e );
                throw new ExceptionInInitializerError( "Unable to locate or load the Blat native library: "
                        + e.getMessage() );
            }
        }
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
     * @return Returns the port.
     */
    public String getPort() {
        return this.port;
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
    public String getSeqFiles() {
        return this.seqFiles;
    }

}
