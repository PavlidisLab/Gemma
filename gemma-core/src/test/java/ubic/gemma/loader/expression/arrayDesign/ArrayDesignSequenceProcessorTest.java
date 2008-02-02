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
package ubic.gemma.loader.expression.arrayDesign;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.RandomStringUtils;

import ubic.gemma.loader.expression.geo.GeoDomainObjectGenerator;
import ubic.gemma.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.loader.expression.geo.service.AbstractGeoService;
import ubic.gemma.loader.genome.SimpleFastaCmd;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.model.genome.biosequence.SequenceType;
import ubic.gemma.testing.AbstractGeoServiceTest;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class ArrayDesignSequenceProcessorTest extends BaseSpringContextTest {

    Collection<CompositeSequence> designElements = new HashSet<CompositeSequence>();
    InputStream seqFile;
    InputStream probeFile;
    InputStream designElementStream;
    ArrayDesign result;
    ArrayDesignSequenceProcessingService app;
    ArrayDesignService arrayDesignService;
    Taxon taxon;
    BioSequenceService bss;

    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();
        endTransaction();

        TaxonService taxonService = ( TaxonService ) getBean( "taxonService" );
        taxon = taxonService.findByCommonName( "mouse" );

        // note that the name MG-U74A is not used by the result. this defines genbank ids etc.
        designElementStream = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A.txt" );
        
        //
        app = ( ArrayDesignSequenceProcessingService ) getBean( "arrayDesignSequenceProcessingService" );
        
        // Target sequences
        seqFile = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A_target" );

        probeFile = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A_probe" );

        arrayDesignService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        bss = ( BioSequenceService ) this.getBean( "bioSequenceService" );

    }

    @Override
    protected void onTearDownInTransaction() throws Exception {
        if ( result != null ) {
            arrayDesignService.thawLite( result );
            BioSequenceService bss = ( BioSequenceService ) this.getBean( "bioSequenceService" );
            for ( CompositeSequence cs : result.getCompositeSequences() ) {
                bss.remove( cs.getBiologicalCharacteristic() );
            }
            arrayDesignService.remove( result );
        }
    }

    public void testAssignSequencesToDesignElements() throws Exception {
        app.assignSequencesToDesignElements( designElements, seqFile );
        CompositeSequenceParser parser = new CompositeSequenceParser();
        parser.parse( designElementStream );
        designElements = parser.getResults();
        for ( DesignElement de : designElements ) {
            assertTrue( ( ( CompositeSequence ) de ).getBiologicalCharacteristic() != null );
        }
    }

    public void testAssignSequencesToDesignElementsMissingSequence() throws Exception {

        CompositeSequenceParser parser = new CompositeSequenceParser();
        parser.parse( designElementStream );
        designElements = parser.getResults();

        CompositeSequence doesntExist = CompositeSequence.Factory.newInstance();
        String fakeName = "I'm not real";
        doesntExist.setName( fakeName );
        designElements.add( doesntExist );

        app.assignSequencesToDesignElements( designElements, seqFile );

        boolean found = false;
        assertEquals( 34, designElements.size() ); // 33 from file plus one fake.

        for ( DesignElement de : designElements ) {

            if ( de.getName().equals( fakeName ) ) {
                found = true;
                if ( ( ( CompositeSequence ) de ).getBiologicalCharacteristic() != null ) {
                    fail( "Shouldn't have found a biological characteristic for this sequence" );

                    continue;
                }
            } else {
                assertTrue( de.getName() + " biological sequence not found", ( ( CompositeSequence ) de )
                        .getBiologicalCharacteristic() != null );
            }

        }

        assertTrue( found ); // sanity check.

    }

    public void testProcessAffymetrixDesign() throws Exception {
        result = app.processAffymetrixDesign( RandomStringUtils.randomAlphabetic( 10 ) + "_arraydesign", taxon,
                designElementStream, probeFile );

        assertEquals( "composite sequence count", 33, result.getCompositeSequences().size() );
        assertTrue( result.getCompositeSequences().iterator().next().getArrayDesign() == result );

    }

    @SuppressWarnings("unchecked")
    public void testFetchAndLoadWithSequences() throws Exception {

        String path = ConfigUtils.getString( "gemma.home" );
        AbstractGeoService geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT ) );
        final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( "GPL226", true, true,
                false );
        result = ads.iterator().next();
        arrayDesignService.thawLite( result );
        try {
            Collection<BioSequence> res = app.processArrayDesign( result, new String[] { "testblastdb",
                    "testblastdbPartTwo" }, ConfigUtils.getString( "gemma.home" )
                    + "/gemma-core/src/test/resources/data/loader/genome/blast", false );
            assertNotNull( res );
            for ( BioSequence sequence : res ) {
                assertNotNull( sequence.getSequence() );
            }
        } catch ( Exception e ) {
            if ( e.getMessage().contains( "not found" ) ) {
                log.error( "fastacmd is not installed or is misconfigured.  Test skipped" );
                return;
            }
        }

    }

    @SuppressWarnings("unchecked")
    public void testFetchAndLoadWithIdentifiers() throws Exception {
        endTransaction();
        String fastacmdExe = ConfigUtils.getString( SimpleFastaCmd.FASTA_CMD_ENV_VAR );
        if ( fastacmdExe == null ) {
            log.warn( "No fastacmd executable is configured, skipping test" );
            return;
        }

        File fi = new File( fastacmdExe );
        if ( !fi.canRead() ) {
            log.warn( fastacmdExe + " not found, skipping test" );
            return;
        }

        String path = ConfigUtils.getString( "gemma.home" );
        AbstractGeoService geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( path
                + AbstractGeoServiceTest.GEO_TEST_DATA_ROOT ) );

        final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( "GPL226", true, true,
                false );
        result = ads.iterator().next();
        arrayDesignService.thawLite( result );

        InputStream f = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/identifierTest.txt" );
        Collection<BioSequence> res = app.processArrayDesign( result, f, new String[] { "testblastdb",
                "testblastdbPartTwo" }, ConfigUtils.getString( "gemma.home" )
                + "/gemma-core/src/test/resources/data/loader/genome/blast", true );
        assertNotNull( res );
        for ( BioSequence sequence : res ) {
            assertNotNull( sequence.getSequence() );
        }
        for ( CompositeSequence cs : result.getCompositeSequences() ) {
            assert cs.getBiologicalCharacteristic() != null;
        }
    }

    @SuppressWarnings("unchecked")
    public void testBig() throws Exception {
        // first load the GPL88 - small
        AbstractGeoService geoService = ( AbstractGeoService ) this.getBean( "geoDatasetService" );
        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGenerator() );
        final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService.fetchAndLoad( "GPL88", true, true,
                false );
        result = ads.iterator().next();
        arrayDesignService.thawLite( result );

        // now do the sequences.
        ZipInputStream z = new ZipInputStream( this.getClass().getResourceAsStream(
                "/data/loader/expression/arrayDesign/RN-U34_probe_tab.zip" ) );

        z.getNextEntry();

        Collection<BioSequence> res = app.processArrayDesign( result, z, SequenceType.AFFY_PROBE );
        assertEquals( 1322, res.size() );
    }

    public void testProcessNonAffyDesign() throws Exception {
        result = app.processAffymetrixDesign( RandomStringUtils.randomAlphabetic( 10 ) + "_arraydesign", taxon,
                designElementStream, probeFile );

        assertNotNull( result.getId() );

        app.processArrayDesign( result, seqFile, SequenceType.EST );

        assertEquals( "composite sequence count", 33, result.getCompositeSequences().size() );

        // assertEquals( "reporter per composite sequence", 17, result.getCompositeSequences().iterator().next()
        // .getComponentReporters().size() );
        assertTrue( result.getCompositeSequences().iterator().next().getArrayDesign() == result );
    }

}
