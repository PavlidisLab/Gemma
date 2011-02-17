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
package ubic.gemma.analysis.sequence;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.genome.FastaParser;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.TimeUtil;
import ubic.gemma.util.concurrent.GenericStreamConsumer;

/**
 * Scan sequences for repeats
 * 
 * @author pavlidis
 * @version $Id$
 */
public class RepeatScan {

    private static final String REPEAT_MASKER_CONFIG_PARAM = "repeatMasker.exe";

    private static Log log = LogFactory.getLog( RepeatScan.class.getName() );

    private static String REPEAT_MASKER = ConfigUtils.getString( REPEAT_MASKER_CONFIG_PARAM );

    private static final int UPDATE_INTERVAL_MS = 1000 * 60 * 2;

    /**
     * @param sequences
     * @param outputSequencePath in FASTA format
     * @return Sequences which were updated.
     */
    public Collection<BioSequence> processRepeatMaskerOutput( Collection<BioSequence> sequences,
            String outputSequencePath ) {
        FastaParser parser = new FastaParser();
        try {
            parser.parse( outputSequencePath );
        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        Collection<BioSequence> finalRes = new HashSet<BioSequence>();
        // build map of identifiers to sequences.
        Collection<BioSequence> results = parser.getResults();

        Map<String, BioSequence> map = new HashMap<String, BioSequence>();
        for ( BioSequence maskedSeq : results ) {
            String identifier = maskedSeq.getName();
            if ( log.isDebugEnabled() ) log.debug( "Masked: " + identifier );
            map.put( identifier, maskedSeq );
        }

        // fill in old sequences with new information

        for ( BioSequence origSeq : sequences ) {
            String identifier = SequenceWriter.getIdentifier( origSeq );
            BioSequence maskedSeq = map.get( identifier );

            if ( log.isDebugEnabled() ) log.debug( "Orig: " + identifier );

            if ( maskedSeq == null ) {
                log.warn( "No masked sequence for " + identifier );
                continue;
            }

            origSeq.setSequence( maskedSeq.getSequence() );
            double fraction = computeFractionMasked( maskedSeq );
            origSeq.setFractionRepeats( fraction );

            if ( fraction > 0 ) {

                finalRes.add( origSeq );
            }
        }

        log.info( finalRes.size() + " sequences had non-zero repeat fractions." );
        return finalRes;

    }

    /**
     * Run repeatmasker on the sequences. The sequence will be updated with the masked (lower-case) sequences and the
     * fraction of masked bases will be filled in.
     * 
     * @param sequences
     * @return sequences that had repeats.
     */
    public Collection<BioSequence> repeatScan( Collection<BioSequence> sequences ) {
        try {
            if ( sequences.size() == 0 ) {
                log.warn( "No sequences to test" );
                return sequences;
            }

            File querySequenceFile = File.createTempFile( "repmask", ".fa" );
            SequenceWriter.writeSequencesToFile( sequences, querySequenceFile );

            Taxon taxon = sequences.iterator().next().getTaxon();

            execRepeatMasker( querySequenceFile, taxon );

            final String outputSequencePath = querySequenceFile.getParent() + File.separatorChar
                    + querySequenceFile.getName() + ".masked";
            // final String outputScorePath = querySequenceFile.getParent() + File.separatorChar
            // + querySequenceFile.getName() + ".masked";

            File output = new File( outputSequencePath );
            if ( !output.exists() ) {
                handleNoOutputCondition( querySequenceFile, outputSequencePath );
                return new HashSet<BioSequence>();
            }

            return processRepeatMaskerOutput( sequences, outputSequencePath );

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    double computeFractionMasked( BioSequence maskedSeq ) {
        // count fraction of masked bases.
        int origLength = maskedSeq.getSequence().length();
        int unmaskedBases = maskedSeq.getSequence().replaceAll( "[a-z]", "" ).length();

        double fraction = ( origLength - unmaskedBases ) / ( double ) origLength;
        return fraction;
    }

    private void checkForExe() {
        if ( REPEAT_MASKER == null ) {
            throw new IllegalStateException( "Repeatmasker executable could not be found. Make sure you correctly set "
                    + REPEAT_MASKER_CONFIG_PARAM );
        }
    }

    /**
     * Run a gfClient query, using a call to exec().
     * 
     * @param querySequenceFile
     * @param outputPath
     * @return
     */
    private void execRepeatMasker( File querySequenceFile, Taxon taxon ) throws IOException {

        checkForExe();

        final String cmd = REPEAT_MASKER + " -parallel 8 -xsmall -species " + taxon.getCommonName() + " "
                + querySequenceFile.getAbsolutePath();
        log.info( "Running repeatmasker like this: " + cmd );

        final Process run = Runtime.getRuntime().exec( cmd );

        // to ensure that we aren't left waiting for these streams
        GenericStreamConsumer gscErr = new GenericStreamConsumer( run.getErrorStream() );
        GenericStreamConsumer gscIn = new GenericStreamConsumer( run.getInputStream() );
        gscErr.start();
        gscIn.start();

        try {

            int exitVal = Integer.MIN_VALUE;

            // wait...
            StopWatch overallWatch = new StopWatch();
            overallWatch.start();

            while ( exitVal == Integer.MIN_VALUE ) {
                try {
                    exitVal = run.exitValue();
                } catch ( IllegalThreadStateException e ) {
                    // okay, still waiting.
                }
                Thread.sleep( UPDATE_INTERVAL_MS );
                String minutes = TimeUtil.getMinutesElapsed( overallWatch );
                log.info( "Repeatmasker: " + minutes + " minutes elapsed" );
            }

            overallWatch.stop();
            String minutes = TimeUtil.getMinutesElapsed( overallWatch );
            log.info( "Repeatmasker took a total of " + minutes + " minutes" );

            // int exitVal = run.waitFor();

            log.debug( "Repeatmasker exit value=" + exitVal );
        } catch ( InterruptedException e ) {
            throw new RuntimeException( e );
        }
        log.debug( "Repeatmasker Success" );

    }

    /**
     * @param querySequenceFile
     * @param outputSequencePath
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void handleNoOutputCondition( File querySequenceFile, final String outputSequencePath )
            throws FileNotFoundException, IOException {
        // this happens if there were no repeats to mask. Check to make sure.
        final String outputSummary = querySequenceFile.getParent() + File.separatorChar + querySequenceFile.getName()
                + ".out";
        if ( !( new File( outputSummary ) ).exists() ) {
            // okay, something is wrong for sure.
            throw new RuntimeException( "Repeatmasker seems to have failed, it left no useful output (looking for "
                    + outputSequencePath + " or " + outputSummary );
        }
        InputStream is = new FileInputStream( outputSummary );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
        String nothingFound = "There were no repetitive sequences detected";
        String line = br.readLine();
        if ( line == null || line.startsWith( nothingFound ) ) {
            log.info( "There were no repeats found" );
        } else {
            log.warn( "Something might have gone wrong with repeatmasking. The output file reads: " + line );
        }

    }

}
