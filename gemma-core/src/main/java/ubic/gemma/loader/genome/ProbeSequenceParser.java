/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.loader.genome;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.loader.util.parser.BasicLineMapParser;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.util.progress.ProgressData;
import ubic.gemma.util.progress.ProgressManager;

/**
 * Parse probes from a tabular file. First columnn = probe id; Second column = sequence name; Third column = seqeucne.
 * <p>
 * This is designed primarily to deal with oligonucleotide arrays that have sequence names different from the probe
 * names.
 * 
 * @author paul
 * @version $Id$
 */
public class ProbeSequenceParser extends BasicLineMapParser<String, BioSequence> {

    protected static final Log log = LogFactory.getLog( ProbeSequenceParser.class );

    private Map<String, BioSequence> results = new HashMap<String, BioSequence>();

    /*
     * (non-Javadoc)
     * @see baseCode.io.reader.BasicLineMapParser#parseOneLine(java.lang.String)
     */
    @Override
    public BioSequence parseOneLine( String line ) {

        if ( line.startsWith( ">" ) ) {
            throw new RuntimeException(
                    "FASTA format not supported - please use the tabular format for oligonucleotides" );
        }

        String[] sArray = StringUtils.splitPreserveAllTokens( line );
        if ( sArray.length == 0 ) {
            return null;
        }

        String probeId = sArray[0].trim();

        if ( StringUtils.isBlank( probeId ) ) {
            return null;
        }

        String sequenceName = sArray[1].trim();

        String sequence = sArray[2].trim();
        if ( StringUtils.isBlank( sequence ) ) {
            /*
             * No sequence.
             */
            return null;
        }

        BioSequence seq = BioSequence.Factory.newInstance();
        seq.setSequence( sequence );
        seq.setLength( ( long ) sequence.length() );
        seq.setIsCircular( false );
        seq.setIsApproximateLength( false );
        seq.setName( sequenceName );

        put( probeId, seq );

        return seq;
    }

    @Override
    public void parse( InputStream is ) throws IOException {

        if ( is == null ) throw new IllegalArgumentException( "InputStream was null" );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        int nullLines = 0;
        String line = null;
        int linesParsed = 0;
        while ( ( line = br.readLine() ) != null ) {

            BioSequence newItem = parseOneLine( line );

            if ( newItem == null ) {
                nullLines++;
                continue;
            }

            if ( ++linesParsed % PARSE_ALERT_FREQUENCY == 0 ) {
                String message = "Parsed " + linesParsed + " lines ";
                ProgressManager.updateCurrentThreadsProgressJob( new ProgressData( 0, message ) );
                log.info( message );
            }

        }
        log.info( "Parsed " + linesParsed + " lines. "
                + ( nullLines > 0 ? nullLines + " yielded no parse result (they may have been filtered)." : "" ) );

        br.close();
    }

    /*
     * (non-Javadoc)
     * @see baseCode.io.reader.BasicLineMapParser#getKey(java.lang.Object)
     */
    @Override
    protected String getKey( BioSequence newItem ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public BioSequence get( String key ) {
        return results.get( key );
    }

    @Override
    public Collection<BioSequence> getResults() {
        return results.values();
    }

    @Override
    protected void put( String key, BioSequence value ) {
        results.put( key, value );
    }

    @Override
    public boolean containsKey( String key ) {
        return results.containsKey( key );
    }

    @Override
    public Collection<String> getKeySet() {
        return results.keySet();
    }
}
