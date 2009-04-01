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

import java.util.HashSet;
import java.util.Set;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * These are full-sized tests (but not too big), we don't actually load it into the db, just test the download and
 * parsing, conversion and merge with processed data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressLoadServiceIntegrationTest extends BaseSpringContextTest {

    /**
     * This only works if you have GPL81 fully loaded!!
     * 
     * @throws Exception
     */
    final public void testLoad() throws Exception {
        endTransaction();
        // Affymetrix GeneChip® Murine Genome U74Av2 [MG_U74Av2] = GPL81
        ArrayDesignService ads = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
        ArrayDesign ad = ads.findByShortName( "GPL81" );

        if ( ads.getCompositeSequenceCount( ad ) < 12000 ) {
            log.warn( "Skipping integration test, GPL81 is not fully loaded" );
            return;
        }

        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
        ExpressionExperiment experiment = svc.load( "E-MEXP-955", "GPL81", false, false );
        assertNotNull( experiment );
    }

    /**
     * sample name problem...this fails.
     * 
     * @throws Exception
     */
    final public void testLoadWithAEDesign1() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
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
    final public void testLoadWithAEDesign2() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
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
    final public void testLoadWithAEDesign3() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
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
     * the MAGE-ML, rows in the data file missing names....etc. etc. This is also tested in the MageMLConverter.
     * 
     * @throws Exception
     */
    final public void testLoadWithAEDesign4() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
        ExpressionExperiment experiment = svc.load( "E-SMDB-1853", null, true, false );
        assertNotNull( experiment );

        Set<String> probeNames = new HashSet<String>();
        for ( BioAssay ba : experiment.getBioAssays() ) {
            ArrayDesign ad = ba.getArrayDesignUsed();
            assertNotNull( ad );
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                probeNames.add( cs.getName() );
            }
            for ( BioMaterial bm : ba.getSamplesUsed() ) {
                assertNotNull( bm.getSourceTaxon() );
            }
        }

        /*
         * Processed data file has 42624 raw data rows, 40595 unique reporters. Indexed by Composite Sequences, of which
         * there are ~18000 unique names (most have no name, like '-'). There are 10 different quantitation types in the
         * raw data file. There are only 11236 named rows in the processed data file, the others have no CS name.
         * Another problem: there is no channel 1 data in the processed data file.
         */
        assertEquals( 17799, probeNames.size() );
        assertEquals( 10, experiment.getQuantitationTypes().size() );
        assertEquals( 112360, experiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : experiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
        }
    }

    /**
     * E-MEXP-740. Affy design, but good test anyway
     * 
     * @throws Exception
     */
    final public void testLoadWithAEDesign5() throws Exception {
        endTransaction();
        ArrayExpressLoadService svc = ( ArrayExpressLoadService ) this.getBean( "arrayExpressLoadService" );
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

        assertEquals( 1, experiment.getQuantitationTypes().size() );
        assertEquals( 12625, experiment.getRawExpressionDataVectors().size() );
        for ( DesignElementDataVector dedv : experiment.getRawExpressionDataVectors() ) {
            assertTrue( probeNames.contains( dedv.getDesignElement().getName() ) );
        }
    }

}
