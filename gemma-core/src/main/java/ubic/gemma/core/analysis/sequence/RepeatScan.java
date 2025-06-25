/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import ubic.gemma.core.loader.genome.FastaParser;
import ubic.gemma.core.profiling.StopWatchUtils;
import ubic.gemma.core.util.ShellUtils;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Scan sequences for repeats using <a href="https://www.repeatmasker.org/RepeatMasker/">RepeatMasker</a>.
 *
 * @author pavlidis
 */
@CommonsLog
public class RepeatScan {

    private static final int UPDATE_INTERVAL_MS = 1000 * 60 * 2;

    private final String repeatMaskerExe;

    public RepeatScan( String repeatMaskerExe ) {
        this.repeatMaskerExe = repeatMaskerExe;
    }

    /**
     * @param sequences sequences
     * @param outputSequencePath in FASTA format
     * @return Sequences which were updated.
     */
    public Collection<BioSequence> processRepeatMaskerOutput( Collection<BioSequence> sequences,
            Path outputSequencePath ) {
        FastaParser parser = new FastaParser();
        try {
            parser.parse( outputSequencePath.toFile() );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        Collection<BioSequence> finalRes = new HashSet<>();
        // build map of identifiers to sequences.
        Collection<BioSequence> results = parser.getResults();

        Map<String, BioSequence> map = new HashMap<>();
        for ( BioSequence maskedSeq : results ) {
            String identifier = maskedSeq.getName();
            if ( RepeatScan.log.isDebugEnabled() )
                RepeatScan.log.debug( "Masked: " + identifier );
            map.put( identifier, maskedSeq );
        }

        // fill in old sequences with new information

        for ( BioSequence origSeq : sequences ) {
            String identifier = SequenceWriter.getIdentifier( origSeq );
            BioSequence maskedSeq = map.get( identifier );

            if ( RepeatScan.log.isDebugEnabled() )
                RepeatScan.log.debug( "Orig: " + identifier );

            if ( maskedSeq == null ) {
                RepeatScan.log.warn( "No masked sequence for " + identifier );
                continue;
            }

            origSeq.setSequence( maskedSeq.getSequence() );
            double fraction = this.computeFractionMasked( maskedSeq );
            origSeq.setFractionRepeats( fraction );

            if ( fraction > 0 ) {

                finalRes.add( origSeq );
            }
        }

        RepeatScan.log.info( finalRes.size() + " sequences had non-zero repeat fractions." );
        return finalRes;

    }

    /**
     * Run RepeatMasker on the sequences. The sequence will be updated with the masked (lower-case) sequences and the
     * fraction of masked bases will be filled in.
     *
     * @param sequences sequences
     * @return sequences that had repeats.
     */
    public Collection<BioSequence> repeatScan( Collection<BioSequence> sequences ) {
        try {
            if ( sequences.isEmpty() ) {
                RepeatScan.log.warn( "No sequences to test" );
                return sequences;
            }

            Path querySequenceFile = Files.createTempFile( "repmask", ".fa" );
            SequenceWriter.writeSequencesToFile( sequences, querySequenceFile.toFile() );

            Taxon taxon = sequences.iterator().next().getTaxon();

            Path outputSequencePath = this.execRepeatMasker( querySequenceFile, taxon );
            // final String outputScorePath = querySequenceFile.getParent() + File.separatorChar
            // + querySequenceFile.getName() + ".masked";

            if ( !Files.exists( outputSequencePath ) ) {
                this.handleNoOutputCondition( querySequenceFile, outputSequencePath );
                return new HashSet<>();
            }

            return this.processRepeatMaskerOutput( sequences, outputSequencePath );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    double computeFractionMasked( BioSequence maskedSeq ) {
        // count fraction of masked bases.
        int origLength = maskedSeq.getSequence().length();
        int masked = 0;
        for ( char c : maskedSeq.getSequence().toCharArray() ) {
            if ( Character.isLowerCase( c ) ) {
                masked++;
            }
        }
        return ( double ) masked / ( double ) origLength;
    }

    /**
     * Run RepeatMasker using a call to exec().
     *
     * @param querySequenceFile file
     * @param taxon             taxon
     * @return
     */
    private Path execRepeatMasker( Path querySequenceFile, Taxon taxon ) throws IOException {
        String[] cmd = new String[] { repeatMaskerExe, "-parallel", "8", "-xsmall",
                "-species", taxon.getCommonName(),
                // FIXME use -dir option to put output where we want; see https://github.com/PavlidisLab/Gemma/issues/53;
                querySequenceFile.toString() };
        RepeatScan.log.info( "Running RepeatMasker like this: " + ShellUtils.join( cmd ) );

        final Process run = new ProcessBuilder( cmd )
                // to ensure that we aren't left waiting for these streams
                // TODO: switch to Redirect.DISCARD for Java 9+
                .redirectOutput( ProcessBuilder.Redirect.appendTo( new File( "/dev/null" ) ) )
                .redirectError( ProcessBuilder.Redirect.PIPE )
                .start();

        // wait...
        StopWatch overallWatch = StopWatch.createStarted();
        try {
            while ( !run.waitFor( RepeatScan.UPDATE_INTERVAL_MS, TimeUnit.MILLISECONDS ) ) {
                String minutes = StopWatchUtils.getMinutesElapsed( overallWatch );
                RepeatScan.log.info( "RepeatMasker: " + minutes + " minutes elapsed" );
            }
        } catch ( InterruptedException e ) {
            Thread.currentThread().interrupt();
            throw new RuntimeException( e );
        }

        int exitVal = run.exitValue();
        if ( exitVal != 0 ) {
            String errorMessage = StringUtils.strip( IOUtils.toString( run.getErrorStream(), StandardCharsets.UTF_8 ) );
            throw new RuntimeException( "RepeatMasker failed with exit value " + exitVal + ":\n" + errorMessage );
        }

        overallWatch.stop();
        String minutes = StopWatchUtils.getMinutesElapsed( overallWatch );
        RepeatScan.log.info( "RepeatMasker took a total of " + minutes + " minutes" );
        RepeatScan.log.debug( "RepeatMasker Success" );
        return querySequenceFile.resolveSibling( querySequenceFile.getFileName().toString() + ".masked" );
    }

    private void handleNoOutputCondition( Path querySequenceFile, Path outputSequencePath ) throws IOException {
        // this happens if there were no repeats to mask. Check to make sure.
        final Path outputSummary = querySequenceFile.resolveSibling( querySequenceFile.getFileName() + ".out" );
        if ( !Files.exists( outputSummary ) ) {
            // okay, something is wrong for sure.
            throw new RuntimeException( String.format( "RepeatMasker seems to have failed, it left no useful output (looking for %s or %s)",
                    outputSequencePath, outputSummary ) );
        }
        try ( BufferedReader br = Files.newBufferedReader( outputSummary ) ) {
            String nothingFound = "There were no repetitive sequences detected";
            String line = br.readLine();
            if ( line == null || line.startsWith( nothingFound ) ) {
                RepeatScan.log.info( "There were no repeats found" );
            } else {
                RepeatScan.log.warn( "Something might have gone wrong with RepeatMasker. The output file reads: " + line );
            }
        }
    }
}
