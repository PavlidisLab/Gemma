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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import ubic.gemma.loader.util.parser.LineMapParser;

/**
 * Parses the "Processed data" files from ArrayExpress. The file format is part of as MAGE-TAB, but is not clearly
 * defined in the current spec. {@link http://www.mged.org/Workgroups/MAGE/MAGE-TAB.pdf}
 * <p>
 * The columns look like this
 * 
 * <pre>
 * CompositeSequence Identifier - always present?
 * CompositeSequence name - always present but values missing for first few rows when experimental factors are defined
 * Database DB:XXXXX where XXXXX is the name of a database. There can be multiple of these. We ignore them.
 * SampleName QuantitationType - these contain the data.
 * </pre>
 * 
 * <p>
 * For an exapmle of a column that contains data, the name might be "GSE1729GSM30320 Norm/DETECTION P-VALUE".
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ProcessedDataFileParser extends LineMapParser {

    // CS -> QTs -> Values
    private Map<String, Map<String, List<String>>> results;

    // Map of quantitation types to which column they show up in.
    private Map<String, List<Integer>> headerMap = null;

    // Array of samples in the order they appear.
    private Object[] samples;

    public ProcessedDataFileParser() {
        super();
        results = new HashMap<String, Map<String, List<String>>>();
    }

    @Override
    public boolean containsKey( Object key ) {
        return results.containsKey( key );
    }

    @Override
    public Object get( Object key ) {
        return results.get( key );
    }

    @Override
    public Collection getResults() {
        return results.values();
    }

    @Override
    public Object parseOneLine( String line ) {
        String[] fields = StringUtils.splitPreserveAllTokens( line, '\t' );
        if ( fields.length == 0 ) return null;
        if ( fields[0].equals( "CompositeSequence identifier" ) ) {
            parseHeader( fields );
            return null;
        }

        if ( headerMap == null ) throw new IllegalStateException( "Header was not detected" );

        if ( fields[1].equals( "" ) ) {
            return null; // sample description.
        }

        String compositeSequenceName = fields[1];

        if ( results.containsKey( compositeSequenceName ) ) {
            throw new IllegalStateException( "Duplicate compositeSequencename" );
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
     * @param header
     */
    private void parseHeader( String[] header ) {
        headerMap = new HashMap<String, List<Integer>>();
        Collection<String> samples = new LinkedHashSet<String>();

        for ( int i = 0; i < header.length; i++ ) {
            String field = header[i];
            if ( field.startsWith( "CompositeSequence" ) ) continue;
            if ( field.startsWith( "Database" ) ) continue;

            String[] subFields = StringUtils.splitPreserveAllTokens( field, '/' );
            if ( subFields.length > 2 )
                throw new UnsupportedOperationException(
                        "Can't parse sample header with more than two fields per sample" );

            if ( subFields.length < 2 ) {
                throw new UnsupportedOperationException(
                        "Can't parse sample header with less than two fields per sample" );
            }

            String sampleName = subFields[0];
            String quantitationType = subFields[1];

            if ( !samples.contains( sampleName ) ) samples.add( sampleName );

            if ( !headerMap.containsKey( quantitationType ) ) {
                headerMap.put( quantitationType, new ArrayList<Integer>() );
            }

            headerMap.get( quantitationType ).add( i );

        }

        this.samples = samples.toArray();
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
    public Collection getKeySet() {
        return results.keySet();
    }

    public Map<String, Map<String, List<String>>> getMap() {
        return results;
    }
}
