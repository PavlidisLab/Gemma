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
package ubic.gemma.loader.protein.string;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import ubic.gemma.loader.protein.string.model.StringProteinProteinInteraction;
import ubic.gemma.model.genome.Taxon;

/**
 * Tests the parsing of a STRING proteinlinksdetailed file. Each line in the file represents a protein protein
 * interaction. Tests cover parsing an individual line, a sample file and checking that if formating errors are
 * encountered errors are thrown.
 * 
 * @author ldonnison
 * @version $Id$
 */
public class StringProteinLinksDetailedParserTest {

    private StringProteinProteinInteractionFileParser parser = null;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        // set up an array of taxon to process
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setNcbiId( 882 );
        Taxon taxon2 = Taxon.Factory.newInstance();
        taxon2.setNcbiId( 10090 );

        ArrayList<Taxon> taxa = new ArrayList<Taxon>();
        taxa.add( taxon );
        taxa.add( taxon2 );
        parser = new StringProteinProteinInteractionFileParser();
        parser.setTaxa( taxa );
    }

    /**
     * Test to make sure that a line can be parsed correctly to its constituent values. Also that the alpabetical
     * sorting of the protein works so that the most alpabetically higer value gets stored in protein 1. Make sure that
     * the proteins get stored in the same order. Test method for
     * {@link ubic.gemma.loader.protein.string.StringProteinProteinInteractionFileParser#parseOneLine(java.lang.String)}
     * . *
     */
    @Test
    public void testParseOneValidLine() {
        String line = "10090.ENSMUSP00000000201 10090.ENSMUSP00000000153 707 0 10 2 3 0 0 222";

        StringProteinProteinInteraction stringProteinProteinInteraction = parser.parseOneLine( line );
        assertTrue( stringProteinProteinInteraction.getNcbiTaxonId().equals( 10090 ) );
        assertEquals( "10090.ENSMUSP00000000153", stringProteinProteinInteraction.getProtein1() );
        assertEquals( "10090.ENSMUSP00000000201", stringProteinProteinInteraction.getProtein2() );

        byte[] arrayStored = stringProteinProteinInteraction.getEvidenceVector();
        byte[] array = new byte[] { 1, 0, 1, 1, 1, 0, 0 };
        assertArrayEquals( "Compare bit vector", array, arrayStored );
        assertEquals( new Double( 222 ), stringProteinProteinInteraction.getCombined_score() );

    }

    /**
     * Test to make sure that is an invalid line is parsed an error is thrown. This line contains too many characters it
     * should be 10 Test method for
     * {@link ubic.gemma.loader.protein.string.StringProteinProteinInteractionFileParser#parseOneLine(java.lang.String)}
     * . *
     */
    @Test
    public void testParseOneLineInvalidWrongNumberOfFields() {
        String line = "882.DVU0002 882.DVU0001 707 0 1 2 3 4 172 742 777";
        try {
            parser.parseOneLine( line );
            fail( "Should have gotten an exception" );
        } catch ( RuntimeException e ) {
            assert ( e.getMessage().startsWith( "Line + " + line + " is not in the right format:" ) );
            e.printStackTrace();
        }

    }

    /**
     * Test to make sure that is an invalid line is parsed an error is thrown. This line contains some characters where
     * they should be numbers Test method for
     * {@link ubic.gemma.loader.protein.string.StringProteinProteinInteractionFileParser#parseOneLine(java.lang.String)}
     * . *
     */
    @Test
    public void testParseOneLineNumberFormatException() {
        String line = "882.DVU0002 882.DVU0001 707 0 1 2 3 4 172 AAA";
        try {
            parser.parseOneLine( line );
            fail( "Should have gotten an exception" );
        } catch ( RuntimeException e ) {
            assertTrue( e.getMessage().contains( "This line does not contain valid number" ) );

        }

    }

    /**
     * Test to make sure that if taxon is not supported then null is returned. Test method for
     * {@link ubic.gemma.loader.protein.string.StringProteinProteinInteractionFileParser#parseOneLine(java.lang.String)}
     * . *
     */
    @Test
    public void testParseOneLineTaxonNotSupported() {
        String line = "778.DVU0002 778.DVU0001 707 0 1 2 3 4 172 AAA";
        try {
            StringProteinProteinInteraction interaction = parser.parseOneLine( line );
            assertNull( interaction );
        } catch ( RuntimeException e ) {
            e.printStackTrace();
            fail();
        }

    }

    /**
     * Test to make sure that if file is corrupt and the two taxon do not match throw error Test method for
     * {@link ubic.gemma.loader.protein.string.StringProteinProteinInteractionFileParser#parseOneLine(java.lang.String)}
     * . *
     */
    @Test
    public void testParseOneLineTaxonDifferent() {
        String line = "882.DVU0002 333.DVU0001 707 0 1 2 3 4 172 AAA";
        try {
            parser.parseOneLine( line );
            fail();
        } catch ( RuntimeException e ) {
            assertTrue( e.getMessage().contains(
                    "Protein 1 882.DVU0002 protein 2  333.DVU0001 do not contain matching taxons" ) );
        }

    }

    /**
     * Test to ensure that a small file containing 100 lines can be parsed correctly. The file contains all one taxon.
     * There are duplicate interactions in the file e.g. 10090.ENSMUSP00000000001 10090.ENSMUSP00000000153 0 0 0 0 0 900
     * 27 902 10090.ENSMUSP00000000153 10090.ENSMUSP00000000001 0 0 0 0 0 900 27 902 are effectively the same and should
     * be treated as one. protein.links.detailed.txt contains references but two are duplicates
     */
    @Test
    public void testParseFileContainingOneTaxon() {
        String fileName = "/data/loader/protein/string/protein.links.detailed.txt";
        URL myurl = this.getClass().getResource( fileName );
        try {
            parser.parse( new File( myurl.getFile() ) );
            Collection<StringProteinProteinInteraction> items = parser.getResults();
            assertEquals( 23, items.size() );
            for ( StringProteinProteinInteraction item : items ) {
                assertTrue( item.getProtein1().startsWith( "10090.ENSMUSP" ) );
                assertTrue( item.getProtein2().startsWith( "10090.ENSMUSP" ) );
            }
        } catch ( RuntimeException e ) {
            fail();
        } catch ( IOException e ) {
            fail();
        }
    }

    /*
     * Code for debugging if needed public void testMultiTaxonStringFile(){ long timestart
     * =System.currentTimeMillis()/1000; Taxon taxon = Taxon.Factory.newInstance(); taxon.setNcbiId( 10090 );
     * 
     * ArrayList<Taxon> taxa = new ArrayList<Taxon>(); taxa.add(taxon);
     * 
     * parser.setTaxa( taxa); String fileName =
     * "~/gemmaData/string.embl.de/newstring_download_protein.links.detailed.v8.2.txt.gz";
     * 
     * try { parser.parse(new File(fileName)); Collection<StringProteinProteinInteraction> items = parser.getResults();
     * long timeend =System.currentTimeMillis()/1000; System.out.println("Total time is : " + (timeend- timestart));
     * System.out.println("There were : " + items.size() + " found"); } catch ( RuntimeException e ) { fail(); } catch (
     * IOException e ) { fail(); } }
     */

}
