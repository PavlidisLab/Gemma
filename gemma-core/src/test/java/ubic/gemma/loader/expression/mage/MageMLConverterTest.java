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
package ubic.gemma.loader.expression.mage;

import java.io.File;
import java.io.InputStream;
import java.util.Collection;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.util.ChannelUtils;
import ubic.gemma.util.ConfigUtils;

/**
 * @author pavlidis
 * @version $Id$
 */
public class MageMLConverterTest extends AbstractMageTest {

    private MageMLConverter mageMLConverter = null;
    private MageMLParser mageMLParser = null;

    /**
     * @param mageMLConverter The mageMLConverter to set.
     */
    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

    /**
     * @param mageMLParser The mageMLParser to set.
     */
    public void setMageMLParser( MageMLParser mageMLParser ) {
        this.mageMLParser = mageMLParser;
    }

    /**
     * E-MEXP-268
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public final void testConvert1() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "jml_projID_4_E-HGMP-2.part.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        ExpressionExperiment expressionExperiment = null;
        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 32, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertTrue( ba.getName().contains( "DBA" ) );
            assertEquals( 1, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( 1, bm.getBioAssaysUsedIn().size() );
                assertEquals( 1, bm.getFactorValues().size() );
                assertNotNull( bm.getSourceTaxon() );
            }
        }

        assertEquals( 6, expressionExperiment.getQuantitationTypes().size() );
        for ( QuantitationType qt : expressionExperiment.getQuantitationTypes() ) {
            log.info( qt );
        }

        /*
         * This study has one factor, tissue type.
         */
        assertEquals( 1, expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );

        for ( ExperimentalFactor factor : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            assertEquals( 26, factor.getFactorValues().size() );
            for ( FactorValue fv : factor.getFactorValues() ) {
                assertTrue( fv.getCharacteristics().size() == 1 );
                for ( Characteristic c : fv.getCharacteristics() ) {
                    assertNotNull( c.getValue() );
                }

            }
        }

    }

    /**
     * This test is NOT redundant with the others one, because it has a different MAGE-ML format -- it exercises a
     * variant path to populating the objects. E-MEXP-268, 8 samples
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public final void testConvert2() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class
                .getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                        + "HeCESbilaterals__intra-individual_differences_between_asymptomatic_and_symptomatic_carotid_plaques.part.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        ExpressionExperiment expressionExperiment = null;
        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 8, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );

        /*
         * One factor, two factor values.
         */
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertEquals( 1, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( 1, bm.getBioAssaysUsedIn().size() );
                assertEquals( 1, bm.getFactorValues().size() );
                assertNotNull( bm.getSourceTaxon() );
            }
        }

        /*
         * This study has one factor, diagnosis, with two factor values, each of which has two characteristics
         */
        assertEquals( 1, expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );

        for ( ExperimentalFactor factor : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            assertEquals( 2, factor.getFactorValues().size() );
            for ( FactorValue fv : factor.getFactorValues() ) {
                assertEquals( 2, fv.getCharacteristics().size() );
                for ( Characteristic c : fv.getCharacteristics() ) {
                    assertNotNull( c.getValue() );
                    assertTrue( c.getCategory().startsWith( "Disease" ) ); // state or staging.
                }

            }
        }
    }

    /**
     * Yet another case that broke our parser. There are four DerivedBioAssays associated with each DerivedBioAssay,
     * etc. E-MEXP-297, 28 samples (we end up with >30 found)
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public final void testConvert3() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-MEXP-297_fixed.part.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        ExpressionExperiment expressionExperiment = null;

        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 28, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );

        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertTrue( "Got: " + ba.getName(), ba.getName().contains( "DBA" ) );
            assertEquals( 2, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertNotNull( bm.getSourceTaxon() );
                assertTrue( bm.getFactorValues().size() >= 2 );
                for ( FactorValue fv : bm.getFactorValues() ) {
                    if ( fv.getCharacteristics().size() > 1 ) {
                        assertNotNull( fv.getCharacteristics().iterator().next().getValue() );
                    } else {
                        // log.info( fv );
                        // assertNotNull( fv.getMeasurement() );
                    }
                }
            }
        }

        assertEquals( 3, expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );

        for ( ExperimentalFactor factor : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            assertTrue( factor.getFactorValues().size() >= 2 );
        }

    }

    /**
     * This dataset has factors but not associated with any of the samples? E-NCMF-4
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public final void testConvert4() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "export3_part.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        ExpressionExperiment expressionExperiment = null;

        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertEquals( 2, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( 1, bm.getBioAssaysUsedIn().size() );
                assertNotNull( bm.getSourceTaxon() );
            }

        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 18, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );

    }

    /**
     * Yet another variant in the bioassay->biomaterial association, E-CBIL-22, 21 samples
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public final void testConvert5() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-CBIL-22-PGC1alphaKO_2380_part.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        ExpressionExperiment expressionExperiment = null;
        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 21, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );
        /*
         * 3 factors
         */
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertEquals( 1, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( 1, bm.getBioAssaysUsedIn().size() );
                assertEquals( 3, bm.getFactorValues().size() );
                assertNotNull( bm.getSourceTaxon() );
            }
        }

        /*
         *  
         */
        assertEquals( 3, expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );

        for ( ExperimentalFactor factor : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            assertEquals( 2, factor.getFactorValues().size() );
            for ( FactorValue fv : factor.getFactorValues() ) {
                assertEquals( 1, fv.getCharacteristics().size() );
                for ( Characteristic c : fv.getCharacteristics() ) {
                    assertNotNull( c.getValue() );
                }

            }
        }
    }

    /**
     * E-MEXP-955, has 13 samples. In MAGE terms it has 13 MeasuredBioAssays, 1 DerivedBioAssay (!).
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public void testConvert6() throws Exception {

        /* PARSING */
        log.info( "***** PARSING *****  " );

        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-MEXP-955.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        /* CONVERTING */
        log.info( "***** CONVERTING ***** " );

        mageMLConverter.addLocalExternalDataPath( ConfigUtils.getString( "gemma.home" ) + File.separatorChar
                + "gemma-core/src/test/resources" + MAGE_DATA_RESOURCE_PATH + "E-MEXP-955" );

        ExpressionExperiment expressionExperiment = null;
        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 13, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );
        /*
         * 3 factors
         */
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertEquals( 1, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( 1, bm.getBioAssaysUsedIn().size() );
                assertEquals( 3, bm.getFactorValues().size() );
                assertNotNull( bm.getSourceTaxon() );
            }

        }

        /*
         *  
         */
        assertEquals( 3, expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );

        for ( ExperimentalFactor factor : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            assertTrue( factor.getFactorValues().size() >= 3 );
            for ( FactorValue fv : factor.getFactorValues() ) {
                log.info( fv );
                assertEquals( 1, fv.getCharacteristics().size() );
                for ( Characteristic c : fv.getCharacteristics() ) {
                    assertNotNull( c.getValue() );
                }

            }
        }
    }

    /**
     * E-MEXP-740 - 32 samples. Does not have top-level bioassays designated, there are two DerivedBioAssays per sample.
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public final void testConvert7() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-MEXP-740.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        ExpressionExperiment expressionExperiment = null;

        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 32, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );

        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertEquals( 1, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( 1, bm.getBioAssaysUsedIn().size() );
                assertEquals( 2, bm.getFactorValues().size() );
                assertNotNull( bm.getSourceTaxon() );
            }

        }

        boolean found = false;
        for ( QuantitationType qt : expressionExperiment.getQuantitationTypes() ) {
            log.info( qt );
            if ( qt.getName().equals( "CHPSignal" ) ) {
                assertEquals( ScaleType.LINEAR, qt.getScale() );
                assertEquals( PrimitiveType.DOUBLE, qt.getRepresentation() );
                assertTrue( qt.getIsPreferred() );
                found = true;
            }
        }
        assertTrue( found );

    }

    /**
     * Has no array package.
     * 
     * @throws Exception
     */
    @SuppressWarnings("null")
    public final void testConvert8() throws Exception {
        /* invoke mageMLParser */
        InputStream istMageExamples = MageMLConverterTest.class.getResourceAsStream( MAGE_DATA_RESOURCE_PATH
                + "E-SMDB-1853-Exptset_1853.part.xml" );

        assert mageMLParser != null;

        mageMLParser.parse( istMageExamples );

        /* get results from parsing step */
        log.info( "Tally:\n" + mageMLParser );
        Collection<Object> mageObjects = mageMLParser.getResults();
        log.debug( "number of SDOs: " + mageObjects.size() );

        istMageExamples.close();

        ExpressionExperiment expressionExperiment = null;

        Collection<Object> gemmaObjects = mageMLConverter.convert( mageObjects );
        log.debug( "number of GDOs: " + gemmaObjects.size() );

        int numExpExp = 0;
        for ( Object obj : gemmaObjects ) {
            if ( obj instanceof ExpressionExperiment ) {
                expressionExperiment = ( ExpressionExperiment ) obj;
                numExpExp++;
            }
            if ( log.isDebugEnabled() ) {
                log.debug( obj.getClass() + ": " + obj );
            }
        }

        assertNotNull( expressionExperiment );
        assertEquals( 1, numExpExp );
        assertEquals( 20, expressionExperiment.getBioAssays().size() );
        assertNotNull( expressionExperiment.getSource() );
        assertNotNull( expressionExperiment.getAccession() );

        assertEquals( 1, expressionExperiment.getExperimentalDesign().getExperimentalFactors().size() );
        for ( ExperimentalFactor ef : expressionExperiment.getExperimentalDesign().getExperimentalFactors() ) {
            assertEquals( 20, ef.getFactorValues().size() );
        }

        /*
         * Has temperature as a factor; this _really_ should be a fixed-level factor but it's stored as a measurement,
         * not a characteristic.
         */
        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            assertEquals( 2, ba.getSamplesUsed().size() );
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertEquals( 1, bm.getBioAssaysUsedIn().size() );
                assertNotNull( bm.getSourceTaxon() );
                assertEquals( 1, bm.getFactorValues().size() );
                for ( FactorValue fv : bm.getFactorValues() ) {
                    assertNotNull( fv.getMeasurement() );
                }
            }

        }

        boolean found = false;
        boolean foundBA = false;
        boolean foundBB = false;
        boolean foundSA = false;
        boolean foundSB = false;
        for ( QuantitationType qt : expressionExperiment.getQuantitationTypes() ) {
            if ( qt.getName().equals( "LOG_RAT2N_MEAN" ) ) {
                assertEquals( ScaleType.LOG2, qt.getScale() );
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                assertTrue( qt.getIsPreferred() );
                found = true;
            }

            if ( ChannelUtils.isBackgroundChannelA( qt.getName() ) ) {
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                foundBA = true;
            }
            if ( ChannelUtils.isBackgroundChannelB( qt.getName() ) ) {
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                foundBB = true;
            }
            if ( ChannelUtils.isSignalChannelA( qt.getName() ) ) {
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                foundSA = true;
            }
            if ( ChannelUtils.isSignalChannelB( qt.getName() ) ) {
                assertEquals( "For " + qt, PrimitiveType.DOUBLE, qt.getRepresentation() );
                foundSB = true;
            }

        }
        assertTrue( found && foundBA && foundBB && foundSA && foundSB );

    }

}
