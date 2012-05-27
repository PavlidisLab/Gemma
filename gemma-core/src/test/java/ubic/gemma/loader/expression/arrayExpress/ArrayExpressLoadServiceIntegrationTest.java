/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.loader.expression.arrayExpress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ChannelUtils;

/**
 * These are full-sized tests (but not too big), we don't actually load it into the db, just test the download and
 * parsing, conversion and merge with processed data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressLoadServiceIntegrationTest extends BaseSpringContextTest {

    //Tests disabled.
    
    @Autowired
    ArrayExpressLoadService svc;

    /**
     * This only works if you have GPL81 fully loaded!!
     * 
     * @throws Exception
     */
    //@Test
    public void testLoad() throws Exception {
        // Affymetrix GeneChip Murine Genome U74Av2 [MG_U74Av2] = GPL81
        ArrayDesignService ads = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        ArrayDesign ad = ads.findByShortName( "GPL81" );

        if ( ad == null || ads.getCompositeSequenceCount( ad ) < 12000 ) {
            log.warn( "Skipping integration test, GPL81 is not fully loaded" );
            return;
        }

        ExpressionExperiment experiment = svc.load( "E-MEXP-955", "GPL81", false, false );
        assertNotNull( experiment );
    }

    /**
     * sample name problem...this fails.
     * 
     * @throws Exception
     */
    //@Test
    public void testLoadWithAEDesign1() throws Exception {

        ExpressionExperiment experiment = svc.load( "E-MEXP-297", null, true, false ); // uses A-MEXP-153
        assertNotNull( experiment );

        Set<String> probeNames = new HashSet<String>();
        for ( BioAssay ba : experiment.getBioAssays() ) {
            ArrayDesign ad = ba.getArrayDesignUsed();
            assertNotNull( ad );
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                probeNames.add( cs.getName() );
            }
        }

        assertEquals( 1000, experiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : experiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
        }
    }

    /**
     * Affy platform, easy
     * 
     * @throws Exception
     */
    //@Test
    final public void testLoadWithAEDesign2() throws Exception {

        ExpressionExperiment experiment = svc.load( "E-MEXP-955", null, true, false );
        assertNotNull( experiment );

        Set<String> probeNames = new HashSet<String>();
        for ( BioAssay ba : experiment.getBioAssays() ) {
            ArrayDesign ad = ba.getArrayDesignUsed();
            assertNotNull( ad );
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                probeNames.add( cs.getName() );
            }
        }

        assertEquals( 12488, experiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : experiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
        }
    }

    /**
     * works...
     * 
     * @throws Exception
     */
    //@Test
    final public void testLoadWithAEDesign3() throws Exception {

        ExpressionExperiment experiment = svc.load( "E-TABM-631", null, true, false ); // uses A-MEXP-691, Illumina
        assertNotNull( experiment );

        Set<String> probeNames = new HashSet<String>();
        for ( BioAssay ba : experiment.getBioAssays() ) {
            ArrayDesign ad = ba.getArrayDesignUsed();
            assertNotNull( ad );
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                probeNames.add( cs.getName() );
            }
        }

        assertEquals( 7, experiment.getQuantitationTypes().size() );
        assertEquals( 331072, experiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : experiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
        }
    }

    /**
     * E-SMDB-1853 uses A-SMDB-681 - SMD Mus musculus Array, spotted. QT names a pain, array design not referenced from
     * the MAGE-ML, rows in the data file missing names....etc. etc. This is also tested in the MageMLConverter
     * 
     * @throws Exception
     */
    //@Test
    final public void testLoadWithAEDesign4() throws Exception {

        ExpressionExperiment expressionExperiment = svc.load( "E-SMDB-1853", null, true, true ); // <----
        assertNotNull( expressionExperiment );

        ExpressionExperimentService ees = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );

        /*
         * Make sure we start with the persistent instance. This is just paranoia.
         */
        expressionExperiment = ees.load( expressionExperiment.getId() );
        expressionExperiment = ees.thawLite( expressionExperiment );

        Set<String> probeNames = new HashSet<String>();
        assertEquals( 20, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment );
        assertEquals( 20, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );

        assertEquals( 1, expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );
        for ( ExperimentalFactor ef : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            assertEquals( 2, ef.getFactorValues().size() );
            for ( FactorValue fv : ef.getFactorValues() ) {
                assertEquals( FactorType.CONTINUOUS, fv.getExperimentalFactor().getType() );
                assertNotNull( fv.getMeasurement() );
                assertEquals( ef, fv.getExperimentalFactor() );
            }
        }

        /*
         * Has temperature as a factor; this _really_ should be a fixed-level factor but it's stored as a measurement,
         * not a characteristic.
         */
        ArrayDesign ad = null;
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            if ( ad == null ) {
                ArrayDesignService ads = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
                ad = ads.thaw( ba.getArrayDesignUsed() );
                for ( CompositeSequence cs : ba.getArrayDesignUsed().getCompositeSequences() ) {
                    probeNames.add( cs.getName() );
                }
                ad = ba.getArrayDesignUsed();
            }
            assertEquals( 2, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertNotNull( bm.getSourceTaxon() );
                assertEquals( 1, bm.getFactorValues().size() );
                for ( FactorValue fv : bm.getFactorValues() ) {
                    assertNotNull( fv.getMeasurement() );
                    assertNotNull( fv.getMeasurement().getUnit() );
                    assertEquals( "degree_C", fv.getMeasurement().getUnit().getUnitNameCV() );
                    assertEquals( PrimitiveType.DOUBLE, fv.getMeasurement().getRepresentation() );
                    assertEquals( MeasurementType.ABSOLUTE, fv.getMeasurement().getType() );
                }
            }
        }

        boolean found = false;
        boolean foundBB = false;
        boolean foundSB = false;
        for ( QuantitationType qt : expressionExperiment.getQuantitationTypes() ) {

            if ( qt.getName().equals( "LOG_RAT2N_MEAN" ) ) {
                assertEquals( ScaleType.LOG2, qt.getScale() );
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                assertTrue( qt.getIsPreferred() );
                found = true;
            }

            if ( ChannelUtils.isBackgroundChannelB( qt.getName() ) ) {
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                foundBB = true;
            }
            if ( ChannelUtils.isSignalChannelB( qt.getName() ) ) {
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                foundSB = true;
            }

        }

        // Note we're missing the data for the other channel, it's not in the processed data file.
        assertTrue( found );
        assertTrue( foundBB );
        assertTrue( foundSB );

        /*
         * Processed data file has 42624 raw data rows, 40595 unique reporters. Indexed by Composite Sequences, of which
         * there are ~18000 unique names (most have no name, like '-'). There are 10 different quantitation types in the
         * raw data file. There are only 11236 named rows in the processed data file, the others have no CS name.
         * Another problem: there is no channel 1 data in the processed data file.
         */
        assertEquals( 17799, probeNames.size() );

        DesignElementDataVectorService dedvs = ( DesignElementDataVectorService ) this
                .getBean( "designElementDataVectorService" );

        dedvs.thaw( expressionExperiment.getRawExpressionDataVectors() );

        assertEquals( 10, expressionExperiment.getQuantitationTypes().size() );
        assertEquals( 112360, expressionExperiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : expressionExperiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
            assertEquals( 20, dedv.getBioAssayDimension().getBioAssays().size() );
        }
    }

    /**
     * E-MEXP-740. Affy design, but good test anyway (this was successfully loaded into Gemma a while ago)
     * 
     * @throws Exception
     */
    //@Test
    final public void testLoadWithAEDesign5() throws Exception {

        ExpressionExperiment experiment = svc.load( "E-MEXP-740", null, true, false );
        assertNotNull( experiment );

        Set<String> probeNames = new HashSet<String>();
        for ( BioAssay ba : experiment.getBioAssays() ) {
            ArrayDesign ad = ba.getArrayDesignUsed();
            assertNotNull( ad );
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                probeNames.add( cs.getName() );
            }
        }

        assertEquals( 6, experiment.getQuantitationTypes().size() );
        assertEquals( 75750, experiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : experiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
        }
    }

    /**
     * Another SMDB one. Missing channel 1 data.
     * 
     * @throws Exception
     */
    //@Test
    final public void testLoadWithAEDesign6() throws Exception {

        ExpressionExperiment experiment = svc.load( "E-SMDB-3827", null, true, false );
        assertNotNull( experiment );

        Set<String> probeNames = new HashSet<String>();
        assertEquals( 16, experiment.getBioAssays().size() );

        for ( BioAssay ba : experiment.getBioAssays() ) {
            ArrayDesign ad = ba.getArrayDesignUsed();
            assertNotNull( ad );
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                probeNames.add( cs.getName() );
            }
            assertEquals( 2, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertNotNull( bm.getSourceTaxon() );
            }
        }

        assertEquals( 36213, probeNames.size() );
        assertEquals( 10, experiment.getQuantitationTypes().size() );
        assertEquals( 134870, experiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : experiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
            assertEquals( 16, dedv.getBioAssayDimension().getBioAssays().size() );
        }
    }

}
