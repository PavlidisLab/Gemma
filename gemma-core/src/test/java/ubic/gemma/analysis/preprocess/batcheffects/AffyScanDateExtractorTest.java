/*
 * The Gemma project
 * 
 * Copyright (c) 2011 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.analysis.preprocess.batcheffects;

import static org.junit.Assert.assertEquals;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import org.junit.Test;

/**
 * @author paul
 * @version $Id$
 */
public class AffyScanDateExtractorTest {

    @Test
    public void testExtractInputStreamGCOS() throws Exception {
        InputStream is = new GZIPInputStream( getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSM506974.part.CEL.gz" ) );
        AffyScanDateExtractor extractor = new AffyScanDateExtractor();

        Date actual = extractor.extract( is );

        DateFormat formatter = new SimpleDateFormat( "MM/dd/yy hh:mm:ss" );
        Date expected = formatter.parse( "08/15/08 14:15:36" );

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractInputStreamV3() throws Exception {
        InputStream is = new GZIPInputStream( getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSM3448.part.CEL.txt.gz" ) );
        AffyScanDateExtractor extractor = new AffyScanDateExtractor();

        Date actual = extractor.extract( is );

        DateFormat formatter = new SimpleDateFormat( "MM/dd/yy HH:mm:ss" );
        Date expected = formatter.parse( "03/15/01 12:16:30" );

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractInputStreamV4() throws Exception {
        InputStream is = new GZIPInputStream( getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSM306831.part.CEL.gz" ) );
        AffyScanDateExtractor extractor = new AffyScanDateExtractor();

        Date actual = extractor.extract( is );

        DateFormat formatter = new SimpleDateFormat( "MM/dd/yy HH:mm:ss" );
        Date expected = formatter.parse( "09/09/05 12:14:40" );

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractInputStreamVx() throws Exception {
        InputStream is = new GZIPInputStream( getClass().getResourceAsStream(
                "/data/loader/expression/geo/GSM239803.CEL.gz" ) );
        AffyScanDateExtractor extractor = new AffyScanDateExtractor();

        Date actual = extractor.extract( is );

        DateFormat formatter = new SimpleDateFormat( "MM/dd/yy hh:mm:ss" );
        Date expected = formatter.parse( "01/20/05 11:04:38" );

        assertEquals( expected, actual );
    }

}
