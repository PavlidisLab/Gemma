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
import java.util.zip.GZIPInputStream;

import org.junit.Test;

/**
 * @author paul
 * @version $Id$
 */
public class GenericScanFileDateExtractorTest {

    /**
     * I don't know a good way to detect this format, but the date is in ISO8601 format.
     * 
     * @throws Exception
     */
    @Test
    public void testExtractGeneSpring() throws Exception {
        InputStream is = getClass().getResourceAsStream( "/data/loader/expression/geo/GSM522322.part.genespring.txt" );
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();

        Date actual = extractor.extract( is );

        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        Date expected = formatter.parse( "2005-08-30T15:17:28" );

        assertEquals( expected, actual );
    }

    /**
     * @throws Exception
     */
    @Test
    public void testExtractGpr() throws Exception {
        InputStream is = new GZIPInputStream( getClass().getResourceAsStream(
                "/data/loader/expression/rawdata/GSM489680.short.gpr.gz" ) );
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();

        Date actual = extractor.extract( is );

        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        Date expected = formatter.parse( "2008-11-27T10:27:42" );

        assertEquals( expected, actual );
    }

    /**
     * Test of tolerance of missing 0 in date.
     * 
     * @throws Exception
     */
    @Test
    public void testParseStandardDateWithGap() throws Exception {
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();
        Date actual = extractor
                .parseStandardFormat( "DatHeader=[7..40002]  caf130_E_1:CLS=2367 RWS=2367 XIN=6  YIN=6  VE=17        2.0 08/26/ 3 12:30:45    ^T GridVerify=None ^T YG_S98.1sq ^T  ^T  ^T  ^T  ^T  ^T  ^T  ^T  ^T 6" );

        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        Date expected = formatter.parse( "2003-08-26T12:30:45" );

        assertEquals( expected, actual );
    }

    @Test
    public void testParseStandardDate() throws Exception {
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();
        Date actual = extractor
                .parseStandardFormat( "DatHeader=[7..40002]  caf130_E_1:CLS=2367 RWS=2367 XIN=6  YIN=6  VE=17        2.0 08/26/03 12:30:45    ^T GridVerify=None ^T YG_S98.1sq ^T  ^T  ^T  ^T  ^T  ^T  ^T  ^T  ^T 6" );

        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        Date expected = formatter.parse( "2003-08-26T12:30:45" );

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractImagene() throws Exception {
        InputStream is = getClass().getResourceAsStream( "/data/loader/expression/geo/GSM542196.imagene.part.txt" );
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();

        Date actual = extractor.extract( is );

        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss zzz" );
        Date expected = formatter.parse( "2002-06-17T20:26:36 PDT" );
        assertEquals( expected, actual );
    }

    @Test
    public void testExtractLongDate() throws Exception {
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();
        Date actual = extractor.parseLongFormat( "        Date    Wed Jun 19 14:53:29 PST 2002" );
        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss zzz" );
        Date expected = formatter.parse( "2002-06-19T15:53:29 PDT" );

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractGenePix() throws Exception {
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();
        Date actual = extractor.parseGenePixDateTime( "\"DateTime=2006/04/07 14:18:18\"\t" );
        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        Date expected = formatter.parse( "2006-04-07T14:18:18" );

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractGenePixB() throws Exception {
        GenericScanFileDateExtractor extractor = new GenericScanFileDateExtractor();
        Date actual = extractor.parseGenePixDateTime( "DateTime=2006/04/07 14:18:18\t" );
        DateFormat formatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss" );
        Date expected = formatter.parse( "2006-04-07T14:18:18" );

        assertEquals( expected, actual );
    }
}
