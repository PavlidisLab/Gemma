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
package ubic.gemma.core.loader.util.biomart;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;

import ubic.gemma.model.genome.Taxon;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests that biomart fetcher works correctly. That is firstly that a file can be downloaded it calls the biomart
 * service safety net so if the url changes test will fail. Secondly that taxons and biomart queries can be correctly
 * formated.
 *
 * @author ldonnison
 */
public class BioMartEnsemblNcbiFetcherTest {

    private static final Log log = LogFactory.getLog( BioMartEnsemblNcbiFetcherTest.class );
    private BiomartEnsemblNcbiFetcher biomartEnsemblNcbiFetcher = null;

    @Before
    public void setUp() {
        biomartEnsemblNcbiFetcher = new BiomartEnsemblNcbiFetcher();
    }

    /*
     * Tests that given a taxon returns the correct attributes
     */
    @Test
    public void testAttributesToRetrieveFromBioMart() {
        String[] attributes = biomartEnsemblNcbiFetcher.attributesToRetrieveFromBioMartForProteinQuery( "hsapiens" );
        assertNotNull( attributes );
        assertTrue( attributes.length == 5 );
        // should be set for human
        assertNotNull( attributes[4] );
    }

    /*
     * Tests that given a scientific named taxon taxon name is formatted correctly for biomart
     */
    @Test
    public void testGetBioMartTaxonName() {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setScientificName( "Homo sapiens" );

        String biomartFormatedString = biomartEnsemblNcbiFetcher.getBiomartTaxonName( taxon );
        assertNotNull( biomartFormatedString );
        assertTrue( biomartFormatedString.equals( "hsapiens" ) );

    }

    /*
     * Tests that a file can be downloaded from biomart in this case a rat file as it is quite small Test method for
     */
    @Test
    public void testGetEnsemblNcibidata() {

        try {
            File ratBiomartFile = biomartEnsemblNcbiFetcher.fetchFileForProteinQuery( "rnorvegicus" );
            assertNotNull( ratBiomartFile );
            assertTrue( ratBiomartFile.canRead() );

        } catch ( ConnectException e ) {
            BioMartEnsemblNcbiFetcherTest.log.warn( "Connection error, skipping test" );
        } catch ( IOException e ) {
            if ( e.getMessage().startsWith( "Error from BioMart" ) ) {
                BioMartEnsemblNcbiFetcherTest.log.warn( e.getMessage() );
            }
        }

    }

    /*
     * Method that downloads all the files for biomart uncomment if there are problems But will take some time to run
     *
     * @Test public void fetchAllTaxa(){ Collection<Taxon> taxa = new ArrayList<Taxon>(); Taxon taxon =
     * Taxon.Factory.newInstance( "Rattus norvegicus", "", "", "", "", 0, true, true, null, null ); Taxon taxon1 =
     * Taxon.Factory.newInstance( "Homo sapiens", "", "", "", "", 0, true, true, null, null ); Taxon taxon2 =
     * Taxon.Factory.newInstance( "Daneo rerio", "", "", "", "", 0, true, true, null, null ); Taxon taxon3 =
     * Taxon.Factory.newInstance( "Mus musculus", "", "", "", "", 0, true, true, null, null ); taxa.add(taxon);
     * taxa.add(taxon1); taxa.add(taxon2); taxa.add(taxon3); try { BioMartEnsemblNcbiFetcher test = new
     * BioMartEnsemblNcbiFetcher(); test.fetch(taxa); } catch ( Exception e ) { e.printStackTrace(); fail(); } }
     */

}
