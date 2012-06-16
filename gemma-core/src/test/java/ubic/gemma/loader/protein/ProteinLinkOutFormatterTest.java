/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.loader.protein;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import ubic.gemma.model.common.description.DatabaseEntry;

/**
 * Test to ensure string links are set correctly in gemma and that evidence codes for interactions are correctly set.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class ProteinLinkOutFormatterTest {

    private ProteinLinkOutFormatter formatter = null;
    DatabaseEntry entry = DatabaseEntry.Factory.newInstance();
    private static String accession = "9606.ENSP00000293813%0D9606.ENSP00000360616";
    private static String url = "http://string-db.org/version_8_2/newstring_cgi/show_network_section.pl?identifier";
    private static String baseFormattedUrl = url.concat( "s=" ).concat( accession );

    @Before
    public void setUp() {
        formatter = new ProteinLinkOutFormatter();
        entry.setUri( url + "=" );
        entry.setAccession( accession );
    }

    /**
     * Test that a base url for string can be appended with a parameter to lower confidence
     */
    @Test
    public void testGetStringProteinProteinInteractionLinkFormatted() {
        String urlForString = formatter.getStringProteinProteinInteractionLinkGemmaDefault( entry );
        String defaultUrl = ( baseFormattedUrl.concat( "&required_score=150" ) );
        assertEquals( defaultUrl, urlForString );
    }

    /**
     * Test that a url for string can be further customised
     */
    @Test
    public void testGetStringProteinProteinInteractionLinkDefault() {
        String urlForString = formatter.getStringProteinProteinInteractionLinkFormatted( entry, "20", "420" );
        String defaultUrl = ( baseFormattedUrl.concat( "&limit=20" ).concat( "&required_score=420" ) );
        assertEquals( defaultUrl, urlForString );
    }

    /**
     * Test given a byte array can be mapped to evidence codes
     */
    @Test
    public void testGetEvidenceDisplayText() {
        byte[] bytes = new byte[] { 0, 0, 0, 0, 1, 0, 1 };
        try {
            String evidenceText = formatter.getEvidenceDisplayText( bytes );
            assertEquals( "Experimental:TextMining", evidenceText );

        } catch ( Exception e ) {
            e.printStackTrace();
            fail();
        }

    }

}
