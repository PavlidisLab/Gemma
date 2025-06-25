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
package ubic.gemma.core.loader.expression.arrayDesign;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.loader.expression.geo.AbstractGeoServiceTest;
import ubic.gemma.core.loader.expression.geo.GeoDomainObjectGeneratorLocal;
import ubic.gemma.core.loader.expression.geo.service.GeoService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import static org.junit.Assert.*;
import static ubic.gemma.core.util.test.Assumptions.assumeThatExecutableExists;

/**
 * @author pavlidis
 */
public class ArrayDesignSequenceProcessorTest extends AbstractGeoServiceTest {

    public static final String FASTA_CMD_CONFIG_NAME = "fastaCmd.exe";
    public static final String FASTA_CMD_EXE = Settings.getString( FASTA_CMD_CONFIG_NAME );

    private Collection<CompositeSequence> designElements = new HashSet<>();
    private InputStream seqFile;
    private InputStream designElementStream;
    private ArrayDesign result;

    @Autowired
    private ArrayDesignSequenceProcessingService app;

    @Autowired
    private ArrayDesignService arrayDesignService;
    private Taxon taxon;

    @Autowired
    private GeoService geoService;

    @Before
    public void setUp() throws Exception {

        taxon = taxonService.findByCommonName( "mouse" );

        // note that the name MG-U74A is not used by the result. this defines genbank ids etc.
        designElementStream = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A.txt" );

        // Target sequences
        seqFile = this.getClass().getResourceAsStream( "/data/loader/expression/arrayDesign/MG-U74A_target" );

    }

    @Test
    public void testAssignSequencesToDesignElements() throws Exception {
        app.assignSequencesToDesignElements( designElements, seqFile );
        CompositeSequenceParser parser = new CompositeSequenceParser();
        parser.parse( designElementStream );
        designElements = parser.getResults();
        for ( CompositeSequence de : designElements ) {
            assertTrue( de.getBiologicalCharacteristic() != null );
        }
    }

    @Test
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

        for ( CompositeSequence de : designElements ) {

            if ( de.getName().equals( fakeName ) ) {
                found = true;
                if ( de.getBiologicalCharacteristic() != null ) {
                    fail( "Shouldn't have found a biological characteristic for this sequence" );
                }
            } else {
                assertTrue( de.getName() + " biological sequence not found", de.getBiologicalCharacteristic() != null );
            }

        }

        assertTrue( found ); // sanity check.

    }

    @Test
    @Ignore("See https://github.com/PavlidisLab/Gemma/issues/1082 for details")
    public void testFetchAndLoadWithIdentifiers() throws Exception {
        assumeThatExecutableExists( FASTA_CMD_EXE );

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );

        @SuppressWarnings("unchecked") final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService
                .fetchAndLoad( "GPL226", true, true, false, true, true );

        result = ads.iterator().next();
        result = arrayDesignService.thaw( result );
        // have to specify taxon as this has two taxons in it
        try ( InputStream f = this.getClass()
                .getResourceAsStream( "/data/loader/expression/arrayDesign/identifierTest.txt" ) ) {
            Collection<BioSequence> res = app
                    .processArrayDesign( result, f, new String[] { "testblastdb", "testblastdbPartTwo" },
                            taxon, true );
            assertNotNull( res );
            for ( BioSequence sequence : res ) {
                assertNotNull( sequence.getSequence() );
            }
            for ( CompositeSequence cs : result.getCompositeSequences() ) {
                assert cs.getBiologicalCharacteristic() != null;
            }
        }
    }

    @Test
    public void testFetchAndLoadWithSequences() throws Exception {
        assumeThatExecutableExists( FASTA_CMD_EXE );

        geoService.setGeoDomainObjectGenerator( new GeoDomainObjectGeneratorLocal( this.getTestFileBasePath() ) );

        @SuppressWarnings("unchecked") final Collection<ArrayDesign> ads = ( Collection<ArrayDesign> ) geoService
                .fetchAndLoad( "GPL226", true, true, false );
        result = ads.iterator().next();

        result = arrayDesignService.thaw( result );
        try {
            Collection<BioSequence> res = app
                    .processArrayDesign( result, new String[] { "testblastdb", "testblastdbPartTwo" },
                            false );
            assertNotNull( res );
            for ( BioSequence sequence : res ) {
                assertNotNull( sequence.getSequence() );
            }
        } catch ( Exception e ) {
            if ( StringUtils.isNotBlank( e.getMessage() ) && e.getMessage().contains( "not found" ) ) {
                log.error( "fastacmd is not installed or is misconfigured.  Test skipped" );
                return;
            }
            throw e;
        }

    }

}
