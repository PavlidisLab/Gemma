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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.zip.GZIPInputStream;

import static org.junit.Assert.assertEquals;

/**
 * @author paul
 */
public class AffyChipTypeExtractorTest {

    @Test
    public void testExtractInputStreamGCOS() throws Exception {
        Resource resource = new ClassPathResource( "/data/loader/expression/geo/GSM506974.part.CEL.gz" );
        InputStream is = new GZIPInputStream( resource.getInputStream() );

        String actual = AffyChipTypeExtractor.extract( is );

        String expected = "Rat230_2";

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractInputStreamV3() throws Exception {
        Resource resource = new ClassPathResource( "/data/loader/expression/geo/GSM3448.part.CEL.txt.gz" );
        InputStream is = new GZIPInputStream( resource.getInputStream() );

        String actual = AffyChipTypeExtractor.extract( is );

        String expected = "RG_U34A";

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractInputStreamV4() throws Exception {
        Resource resource = new ClassPathResource( "/data/loader/expression/geo/GSM306831.part.CEL.gz" );
        InputStream is = new GZIPInputStream( resource.getInputStream() );

        String actual = AffyChipTypeExtractor.extract( is );

        String expected = "Zebrafish";

        assertEquals( expected, actual );
    }

    @Test
    public void testExtractInputStreamVx() throws Exception {
        Resource resource = new ClassPathResource( "/data/loader/expression/geo/GSM239803.CEL.gz" );
        InputStream is = new GZIPInputStream( resource.getInputStream() );

        String actual = AffyChipTypeExtractor.extract( is );

        String expected = "RN_U34";

        assertEquals( expected, actual );
    }

    // @Test
    // public void testExtractInputStreamCommandConsole() throws Exception {
    // InputStream is = new GZIPInputStream( getClass().getResourceAsStream(
    // "/data/loader/expression/geo/GSM1389719_A742_138.CEL.gz" ) );
    // AffyScanDateExtractor extractor = new AffyScanDateExtractor();
    //
    // Date actual = extractor.extract( is );
    //
    // DateFormat formatter = new SimpleDateFormat( "MM/dd/yy hh:mm:ss" );
    // Date expected = formatter.parse( "03/28/12 01:38:47" );
    //
    // assertEquals( expected, actual );
    // }

}
