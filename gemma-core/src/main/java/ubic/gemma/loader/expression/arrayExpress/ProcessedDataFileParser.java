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
package ubic.gemma.loader.expression.arrayExpress;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.LineMapParser;

/**
 * Parses the "Processed data" files from ArrayExpress. The file format is part of as MAGE-TAB, I found a description a
 * {@link http://tab2mage.sourceforge.net/docs/magetab_docs.html#datamatrix}, see also {@link http
 * ://www.mged.org/mage-tab/}. The first row names the hybridizations. The second row names the quantitation types.
 * Subsequent rows contain the data.
 * <p>
 * Note that the current format easier to handle than an earlier version, check out version 1.3 of this parser to see
 * how it used to (not) work.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProcessedDataFileParser extends LineMapParser<String, Map<String, List<String>>> {

    // CS -> QTs -> Values
    private Map<String, Map<String, List<String>>> results;

    // Map of quantitation types to which column they show up in.
    private Map<String, List<Integer>> headerMap = new HashMap<String, List<Integer>>();

    // Array of samples in the order they appear.
    private Object[] samples;

    private boolean usingReporters = false;

    /**
     * @return the useReporters
     */
    public boolean isUsingReporters() {
        return usingReporters;
    }

    public ProcessedDataFileParser() {
        super();
        results = new HashMap<String, Map<String, List<String>>>();
    }

    @Override
    public boolean containsKey( String key ) {
        return results.containsKey( key );
    }

    @Override
    public Map<String, List<String>> get( String key ) {
        return results.get( key );
    }

    @Override
    public Collection<Map<String, List<String>>> getResults() {
        return results.values();
    }

    @Override
    public Map<String, List<String>> parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( this.getLinesParsed() == 0 ) {
            parseFirstLine( fields );
            return null;
        } else if ( this.getLinesParsed() == 1 ) {
            parseSecondLine( fields );
            return null;
        } else {
            return parseDataLine( fields );
        }

    }

    /**
     * @param fields
     * @return
     */
    private Map<String, List<String>> parseDataLine( String[] fields ) {
        String rawProbeNameString = fields[0];

        String compositeSequenceName = getUnqualifiedIdentifier( rawProbeNameString );
        compositeSequenceName = compositeSequenceName.replaceFirst( "^A-[A-Z]{4}-[0-9]+\\.", "" );

        if ( results.containsKey( compositeSequenceName ) ) {
            /*
             * This is actually okay. We're sometimes parsing multiple files, so the second one + will have the same
             * names. FIXME we can add a check per file. ALSO some data sets have the same platform more than once.
             */
            // throw new IllegalStateException( "Duplicate compositeSequencename" );
        }

        Map<String, List<String>> csData = new HashMap<String, List<String>>();

        for ( String qt : headerMap.keySet() ) {
            List<Integer> columnIdx = headerMap.get( qt );
            if ( !csData.containsKey( qt ) ) {
                csData.put( qt, new ArrayList<String>() );
            }

            List<String> qtData = csData.get( qt );

            for ( Integer i : columnIdx ) {
                String v = fields[i];
                qtData.add( v );
            }
        }

        results.put( compositeSequenceName, csData );
        return csData;
    }

    /**
     * @param identifier
     * @return
     */
    private String getUnqualifiedIdentifier( String identifier ) {
        return identifier.substring( identifier.lastIndexOf( ':' ) + 1, identifier.length() );
    }

    /**
     * @param header
     */
    private void parseSecondLine( String[] header ) {
        headerMap = new HashMap<String, List<Integer>>();
        for ( int i = 0; i < header.length; i++ ) {
            String field = header[i];

            if ( i == 0 ) {
                if ( field.contains( "Reporter" ) ) {
                    log.info( "Data indexed by reporters" );
                    usingReporters = true;
                } else {
                    log.info( "Data indexed by composite sequences" );
                    usingReporters = false;
                }
                continue;
            }

            String quantitationType = getUnqualifiedIdentifier( field );
            if ( !headerMap.containsKey( quantitationType ) ) {
                headerMap.put( quantitationType, new ArrayList<Integer>() );
            }
            headerMap.get( quantitationType ).add( i );
        }
    }

    /**
     * @param fields
     */
    private void parseFirstLine( String[] fields ) {
        List<String> s = new ArrayList<String>();
        for ( int i = 1; i < fields.length; i++ ) {
            String sampleName = fields[i];
            if ( !s.contains( sampleName ) ) {
                s.add( sampleName );
            }
        }

        this.samples = s.toArray();

    }

    /**
     * Return a list of the sample names in the order they appeared in the file.
     * 
     * @return
     */
    public Object[] getSamples() {
        return samples;
    }

    @Override
    public Collection<String> getKeySet() {
        return results.keySet();
    }

    public Map<String, Map<String, List<String>>> getMap() {
        return results;
    }
}
