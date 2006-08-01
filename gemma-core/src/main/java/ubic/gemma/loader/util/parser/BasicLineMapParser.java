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
package ubic.gemma.loader.util.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A line parser that produces a Map instead of a Collection. Subclasses must provide a method to generate keys.
 * 
 * @author pavlidis
 * @version $Id$
 */
public abstract class BasicLineMapParser implements LineParser {

    protected Log log = LogFactory.getLog( BasicLineMapParser.class.getName() );

    protected Map<Object, Object> results = new HashMap<Object, Object>();

    /**
     * @param key
     * @param value
     */
    protected abstract void put( Object key, Object value );

    /**
     * @param probeSetId
     * @see Map
     */
    public abstract Object get( Object key );

    /**
     * @param key
     * @return
     * @see Map
     */
    public boolean containsKey( Object key ) {
        return results.containsKey( key );
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.InputStream)
     */
    public void parse( InputStream is ) throws IOException {

        if ( is == null ) throw new IllegalArgumentException( "InputStream was null" );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        int count = 0;
        while ( ( line = br.readLine() ) != null ) {
            Object newItem = parseOneLine( line );

            if ( newItem == null ) {
                log.debug( "Null object returned" ); // this could be a header or a tolerated parse error
                continue;
            }

            Object key = getKey( newItem );
            if ( key == null ) {
                throw new IllegalStateException( "Got null key for item " + count );
            }
            results.put( key, newItem );
            count++;
            if ( count % PARSE_ALERT_FREQUENCY == 0 )
                log.debug( "Parsed " + count + " lines..., last had key " + key );

        }
        log.info( "Parsed " + count + " lines." );
        br.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#parse(java.io.File)
     */
    public void parse( File file ) throws IOException {
        if ( file == null ) {
            throw new IllegalArgumentException( "File cannot be null" );
        }
        if ( !file.exists() || !file.canRead() ) {
            throw new IOException( "Could not read from file " + file.getPath() );
        }
        FileInputStream stream = new FileInputStream( file );
        parse( stream );
        stream.close();
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.LineParser#pasre(java.lang.String)
     */
    public void parse( String filename ) throws IOException {
        if ( StringUtils.isBlank( filename ) ) {
            throw new IllegalArgumentException( "No filename provided" );
        }
        log.info( "Parsing " + filename );
        File infile = new File( filename );
        parse( infile );
    }

    /*
     * (non-Javadoc)
     * 
     * @see baseCode.io.reader.BasicLineParser#parseOneLine(java.lang.String)
     */
    public abstract Object parseOneLine( String line );

    /**
     * @param newItem
     * @return
     */
    protected abstract Object getKey( Object newItem );

    /**
     * 
     */
    public abstract Collection getResults();

}
