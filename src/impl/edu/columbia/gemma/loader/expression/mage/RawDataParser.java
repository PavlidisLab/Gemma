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
package edu.columbia.gemma.loader.expression.mage;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StopWatch;

import edu.columbia.gemma.loader.loaderutils.Parser;

/**
 * Parse the raw files from array express.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
 * 
 * @author keshav
 * @version $Id$
 */
public class RawDataParser implements Parser {
    private static Log log = LogFactory.getLog( RawDataParser.class.getName() );
    protected static final int ALERT_FREQUENCY = 5000; // TODO put in interface since this is a constant
    
    //TODO move indicies to a the properties (mage.properties).
    protected static final int X = 0;
    protected static final int Y = 1;
    protected static final int INTENSITY = 2;
    protected static final int STDEV = 3;
    protected static final int PIXELS = 4;
    protected static final int OUTLIER = 5;
    protected static final int MASKED = 6;

    /**
     * @param is
     * @throws IOException FIXME DON'T RUN THIS. IT TAKES TOO LONG. This is still a work in progress ... but getting
     *         there.
     */
    @SuppressWarnings("unchecked")
    public void parse( InputStream is ) throws IOException {

        if ( is == null ) throw new IllegalArgumentException( "InputStream was null" );
        BufferedReader br = new BufferedReader( new InputStreamReader( is ) );

        String line = null;
        int count = 0;
        // Quantitation types as data structures.
        // Using arrayList because it is ordered and more efficient than LinkedList implementation.
        List arrayListX = new ArrayList();
        List arrayListY = new ArrayList();
        List arrayListIntensity = new ArrayList();
        List arrayListStdev = new ArrayList();
        List arrayListPixels = new ArrayList();
        List arrayListOutlier = new ArrayList();
        List arrayListMasked = new ArrayList();

        StopWatch sw = new StopWatch();
        sw.start( "Start watch" );
        while ( ( line = br.readLine() ) != null ) {
            String[] values = StringUtils.split( line, " " );

            arrayListX.add( Integer.parseInt( values[X] ) );
            arrayListY.add( Integer.parseInt( values[Y] ) );
            arrayListIntensity.add( Double.parseDouble( values[INTENSITY] ) );
            arrayListStdev.add( Double.parseDouble( values[STDEV] ) );
            arrayListPixels.add( Integer.parseInt( values[PIXELS] ) );
            arrayListOutlier.add( Boolean.parseBoolean( values[OUTLIER] ) );
            arrayListMasked.add( Boolean.parseBoolean( values[MASKED] ) );

            count++;
            if ( count % ALERT_FREQUENCY == 0 ) {
                log.debug( "Read in " + count + " items..." );
                log.info( arrayListX );
                log.info( arrayListY );
                log.info( arrayListIntensity );
                log.info( arrayListStdev );
                log.info( arrayListPixels );
                log.info( arrayListOutlier );
                log.info( arrayListMasked );
            }
        }
        sw.stop();
        sw.shortSummary();

        br.close();

        log.info( "X: " + arrayListX.size() );
        log.info( "Y: " + arrayListY.size() );
        log.info( "Intensity: " + arrayListIntensity.size() );
        log.info( "Stdev: " + arrayListStdev.size() );
        log.info( "Pixels: " + arrayListPixels.size() );
        log.info( "Outlier: " + arrayListOutlier.size() );
        log.info( "Masked: " + arrayListMasked.size() );

    }

    /**
     * @param file
     * @throws IOException
     */
    public void parse( File f ) throws IOException {
        InputStream is = new FileInputStream( f );
        parse( is );
    }

    @SuppressWarnings("unused")
    public void parse( String filename ) {
        // TODO implement and remove the SuppressedWarnings("unused") from this method. I have added it for now
        // so you will not see any warnings.

    }

    public Collection<Object> getResults() {
        // TODO Auto-generated method stub
        return null;
    }

}
