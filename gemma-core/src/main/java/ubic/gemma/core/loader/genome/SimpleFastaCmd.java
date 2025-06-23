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
package ubic.gemma.core.loader.genome;

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.core.config.Settings;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;

/**
 * Simple implementation of methods for fetching sequences from blast-formatted databases, using blastdbcmd (aka
 * fastacmd)
 *
 * @author pavlidis
 */
@CommonsLog
public class SimpleFastaCmd implements FastaCmd {

    // this name should be eventually changed to blastdbCmd.exe, since NCBI BLAST changed the name of the program.
    public static final String FASTA_CMD_CONFIG_NAME = "fastaCmd.exe";

    private static String FASTA_CMD_EXE = Settings.getString( FASTA_CMD_CONFIG_NAME );

    private static final String DB_OPTION;
    private static final String QUERY_OPTION;
    private static final String ENTRY_BATCH_OPTION;

    static {
        if ( FASTA_CMD_EXE.endsWith( "blastdbcmd" ) ) {
            log.debug( "Detected that blastdbcmd is being used, setting options accordingly." );
            DB_OPTION = "db";
            QUERY_OPTION = "entry";
            ENTRY_BATCH_OPTION = "entry_batch";
        } else {
            log.debug( "Detected that fastacmd is being used, setting options accordingly." );
            DB_OPTION = "d";
            QUERY_OPTION = "s";
            ENTRY_BATCH_OPTION = "i";
        }
    }

    private String blastHome = System.getenv( "BLASTDB" );

    public void setBlastHome( String blastDbHome ) {
        this.blastHome = blastDbHome;
    }

    @Override
    public BioSequence getByAccession( String accession, String database ) {
        try {
            return getSingle( accession, database );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public BioSequence getByIdentifier( int identifier, String database ) {
        try {
            return getSingle( String.valueOf( identifier ), database );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Collection<BioSequence> getBatchAccessions( Collection<String> accessions, String database ) {
        try {
            return getMultiple( accessions, database );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    public Collection<BioSequence> getBatchIdentifiers( Collection<Integer> identifiers, String database ) {
        try {
            return getMultiple( identifiers, database );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Keys can be numbers or strings...
     *
     * @param keys keys
     * @param database database
     * @return bio sequences
     * @throws IOException when there are IO problems
     */
    private Collection<BioSequence> getMultiple( Collection<?> keys, String database )
            throws IOException {
        checkBlastConfig();
        Path tmp = Files.createTempFile( "sequenceIds", ".txt" );
        try {
            try ( BufferedWriter tmpOut = Files.newBufferedWriter( tmp ) ) {
                for ( Object object : keys ) {
                    if ( object instanceof String ) {
                        String acc = object.toString().replaceFirst( "\\.[0-9]+", "" );
                        tmpOut.write( acc + "\n" );
                    } else {
                        tmpOut.write( object.toString() + "\n" );
                    }
                }
            }
            String[] command = new String[] { SimpleFastaCmd.FASTA_CMD_EXE, "-long_seqids", "-target_only",
                    "-" + DB_OPTION, database, "-" + ENTRY_BATCH_OPTION, tmp.toString() };
            SimpleFastaCmd.log.info( String.join( " ", command ) );
            ProcessBuilder pb = new ProcessBuilder( command )
                    .redirectOutput( ProcessBuilder.Redirect.PIPE )
                    .redirectError( ProcessBuilder.Redirect.PIPE );
            SimpleFastaCmd.log.info( "BLASTDB=" + blastHome );
            pb.environment().put( "BLASTDB", blastHome );
            Process pr = pb.start();
            return getSequencesFromFastaCmdOutput( pr );
        } finally {
            Files.delete( tmp );
        }
    }

    /**
     * @param key, which is normally either a String (ACC) or an Integer (GID)
     * @param database db
     * @throws IOException io problems
     */
    private BioSequence getSingle( String key, String database ) throws IOException {
        checkBlastConfig();
        String[] command = new String[] { SimpleFastaCmd.FASTA_CMD_EXE, "-long_seqids", "-target_only",
                "-" + DB_OPTION, database, "-" + QUERY_OPTION, key };
        SimpleFastaCmd.log.info( String.join( " ", command ) );
        ProcessBuilder pb = new ProcessBuilder( command )
                .redirectOutput( ProcessBuilder.Redirect.PIPE )
                .redirectError( ProcessBuilder.Redirect.PIPE );
        SimpleFastaCmd.log.info( "BLASTDB=" + blastHome );
        pb.environment().put( "BLASTDB", blastHome );
        Process pr = pb.start();
        Collection<BioSequence> sequences = getSequencesFromFastaCmdOutput( pr );
        if ( sequences.isEmpty() ) {
            return null;
        } else if ( sequences.size() == 1 ) {
            return sequences.iterator().next();
        } else {
            throw new IllegalStateException( "Got more than one sequence!" );
        }
    }

    private void checkBlastConfig() {
        if ( StringUtils.isBlank( SimpleFastaCmd.FASTA_CMD_EXE ) )
            throw new IllegalStateException( "No blastdbcmd executable: You must set " + SimpleFastaCmd.FASTA_CMD_CONFIG_NAME
                    + " in your environment." );
        if ( blastHome == null ) {
            throw new IllegalArgumentException( "No blast database location specified, you must set the BLASTDB environment variable or use setBlastHome()." );
        }
    }

    private Collection<BioSequence> getSequencesFromFastaCmdOutput( Process pr ) {
        try {
            final FastaParser parser = new FastaParser();
            parser.parse( pr.getInputStream() );
            int exitVal = pr.waitFor();
            if ( exitVal != 0 ) {
                // check standard error stream for specific error messages
                String errorMessage = StringUtils.strip( IOUtils.toString( pr.getErrorStream(), StandardCharsets.UTF_8 ) );
                if ( errorMessage.contains( "Entry or entries not found in BLAST database" ) || errorMessage.contains( "Skipped" ) ) {
                    log.warn( "There are warnings in " + FASTA_CMD_EXE + " output:\n" + errorMessage );
                    return parser.getResults();
                }
                throw new RuntimeException( FASTA_CMD_EXE + " exit value=" + exitVal + " " + errorMessage );
            }
            return parser.getResults();
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }
    }
}
