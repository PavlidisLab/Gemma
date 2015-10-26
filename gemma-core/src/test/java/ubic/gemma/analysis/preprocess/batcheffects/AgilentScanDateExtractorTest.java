/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.analysis.preprocess.batcheffects;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;

/**
 * Tests of parsing various flat file formats used for Agilent slides (and possibly other types)
 * 
 * @author paul
 * @version $Id$
 */
public class AgilentScanDateExtractorTest {

    @Test
    public void testExtractGPR() throws Exception {
        try (InputStream is = getClass().getResourceAsStream( "/data/loader/expression/rawdata/GSM393974.gpr.txt" );) {
            AgilentScanDateExtractor extractor = new AgilentScanDateExtractor();

            Date actual = extractor.extract( is );

            DateFormat formatter = new SimpleDateFormat( "yyyy/MM/dd HH:mm:ss" );
            Date expected = formatter.parse( "2005/11/09 11:36:27" );

            assertEquals( expected, actual );
        }
    }

    @Test
    public void testExtractAgilent() throws Exception {
        try (InputStream is = getClass().getResourceAsStream( "/data/loader/expression/rawdata/GSM361301.agilent.txt" );) {
            AgilentScanDateExtractor extractor = new AgilentScanDateExtractor();

            Date actual = extractor.extract( is );

            DateFormat formatter = new SimpleDateFormat( "MM-dd-yyyy HH:mm:ss" );
            Date expected = formatter.parse( "10-18-2005 13:02:36" );

            assertEquals( expected, actual );
        }
    }
    
    @Test
    public void testExtractAgilent2() throws Exception {
        try (InputStream is = getClass().getResourceAsStream( "/data/loader/expression/rawdata/GSM1662306.agilent2.txt" );) {
            AgilentScanDateExtractor extractor = new AgilentScanDateExtractor();

            Date actual = extractor.extract( is );

            DateFormat formatter = new SimpleDateFormat( "MM-dd-yyyy HH:mm:ss" );
            Date expected = formatter.parse( "02-27-2014 09:32:52" );

            assertEquals( expected, actual );
        }
    }
}
