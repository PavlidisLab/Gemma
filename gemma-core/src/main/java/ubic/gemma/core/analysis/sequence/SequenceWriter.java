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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import ubic.gemma.model.genome.biosequence.BioSequence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;

/**
 * Tools for writing biosequences to files so they can be analyzed by external tools, and then read back into Gemma.
 *
 * @author paul
 */
public class SequenceWriter {
    /**
     * Spaces in the sequence name will cause problems when converting back from some formats (e.g. PSL), so they are
     * replaced.
     */
    public static final String SPACE_REPLACEMENT = "_____";
    /**
     * Required for some applications (read: Repeatmasker) that can't handle long identifiers.
     */
    private static final int MAX_SEQ_IDENTIFIER_LENGTH = 50;
    private static final Log log = LogFactory.getLog( SequenceWriter.class.getName() );

    /**
     * Write a collection of sequences in FASTA format
     *
     * @param sequences  sequences
     * @param outputFile file
     * @return number of sequences written, excluding blanks and duplicates.
     * @throws IOException io problems
     */
    public static int writeSequencesToFile( Collection<BioSequence> sequences, File outputFile ) throws IOException {
        try (BufferedWriter out = new BufferedWriter( new FileWriter( outputFile ) )) {

            SequenceWriter.log.debug( "Processing " + sequences.size() + " sequences for blat analysis" );
            int count = 0;
            Collection<Object> identifiers = new HashSet<>();
            int repeats = 0;
            for ( BioSequence b : sequences ) {
                if ( StringUtils.isBlank( b.getSequence() ) ) {
                    SequenceWriter.log.warn( "Blank sequence for " + b );
                    continue;
                }
                String identifier = SequenceWriter.getIdentifier( b );
                if ( identifiers.contains( identifier ) ) {
                    SequenceWriter.log.debug( b + " is a repeat with identifier " + identifier );
                    repeats++;
                    continue; // don't repeat sequences.
                }

                // use toUpper to ensure that sequence does not start out 'masked'.
                out.write( ">" + identifier + "\n" + b.getSequence().toUpperCase() + "\n" );
                identifiers.add( identifier );

                if ( ++count % 2000 == 0 ) {
                    SequenceWriter.log.debug( "Wrote " + count + " sequences" );
                }
            }

            SequenceWriter.log.info( "Wrote " + count + " sequences to " + outputFile + ( repeats > 0 ?
                    " ( " + repeats + " repeated items were skipped)." :
                    "" ) );
            return count;
        }
    }

    /**
     * Modify the identifier for the purposes of using in temporary Fasta files. WARNING There is a faint possibility
     * that this could cause problems in identifying the sequences later.
     *
     * @param b b
     * @return string
     */
    public static String getIdentifier( BioSequence b ) {
        String identifier = b.getName();
        identifier = identifier.replaceAll( " ", SequenceWriter.SPACE_REPLACEMENT );
        identifier = identifier
                .substring( 0, Math.min( identifier.length(), SequenceWriter.MAX_SEQ_IDENTIFIER_LENGTH ) );
        return identifier;
    }

}
