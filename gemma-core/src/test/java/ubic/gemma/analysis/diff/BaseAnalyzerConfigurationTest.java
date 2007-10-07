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
package ubic.gemma.analysis.diff;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang.math.RandomUtils;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.DesignElement;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;

/**
 * Other tests can extend this class if they want an expression experiment with complete block design and biological
 * replicates.
 * 
 * @author keshav
 * @version $Id$
 */
public class BaseAnalyzerConfigurationTest extends BaseSpringContextTest {

    private static final int NUM_DESIGN_ELEMENTS = 10;

    private static final int NUM_BIOASSAYS = 8;

    // TODO consolidate the test data here and in the BaseAnalyzerTest. This class is
    // easier to work with, but it doesn't have any data in the vectors.

    protected ExpressionExperiment expressionExperiment = null;

    protected ExperimentalDesign experimentalDesign = null;

    protected ExperimentalFactor experimentalFactorA = null;
    protected ExperimentalFactor experimentalFactorB = null;

    protected Collection<BioMaterial> biomaterials = null;

    protected QuantitationType quantitationType = null;

    protected BioAssayDimension bioAssayDimension = null;

    private BioMaterial biomaterial0a = null;
    private BioMaterial biomaterial0b = null;
    private BioMaterial biomaterial1a = null;
    private BioMaterial biomaterial1b = null;
    private BioMaterial biomaterial2a = null;
    private BioMaterial biomaterial2b = null;
    private BioMaterial biomaterial3a = null;
    private BioMaterial biomaterial3b = null;

    private Collection<ExperimentalFactor> experimentalFactors = null;

    private Collection<FactorValue> factorValuesA = null;

    private Collection<FactorValue> factorValuesB = null;

    private Collection<BioAssay> bioAssays = null;

    private BioAssay bioAssay0a = null;
    private BioAssay bioAssay0b = null;
    private BioAssay bioAssay1a = null;
    private BioAssay bioAssay1b = null;
    private BioAssay bioAssay2a = null;
    private BioAssay bioAssay2b = null;
    private BioAssay bioAssay3a = null;
    private BioAssay bioAssay3b = null;

    private Collection<DesignElementDataVector> vectors = null;

    private ByteArrayConverter bac = new ByteArrayConverter();

    @Override
    public void onSetUpInTransaction() {

        expressionExperiment = ExpressionExperiment.Factory.newInstance();
        expressionExperiment.setName( "a test expression experiment" );

        /* experimental factor "area" */
        experimentalFactorA = ExperimentalFactor.Factory.newInstance();
        experimentalFactorA.setName( "area" );

        factorValuesA = new HashSet<FactorValue>();

        FactorValue factorValueA1 = FactorValue.Factory.newInstance();
        factorValueA1.setValue( "cerebellum" );
        factorValueA1.setExperimentalFactor( experimentalFactorA );

        FactorValue factorValueA2 = FactorValue.Factory.newInstance();
        factorValueA2.setValue( "amygdala" );
        factorValueA2.setExperimentalFactor( experimentalFactorA );

        factorValuesA.add( factorValueA1 );
        factorValuesA.add( factorValueA2 );

        experimentalFactorA.setFactorValues( factorValuesA );

        /* experimental factor "treat" */
        experimentalFactorB = ExperimentalFactor.Factory.newInstance();
        experimentalFactorB.setName( "treat" );

        factorValuesB = new HashSet<FactorValue>();

        FactorValue factorValueB1 = FactorValue.Factory.newInstance();
        factorValueB1.setValue( "no pcp" );
        factorValueB1.setExperimentalFactor( experimentalFactorB );

        FactorValue factorValueB2 = FactorValue.Factory.newInstance();
        factorValueB2.setValue( "pcp" );
        factorValueB2.setExperimentalFactor( experimentalFactorB );

        factorValuesB.add( factorValueB1 );
        factorValuesB.add( factorValueB2 );

        experimentalFactorB.setFactorValues( factorValuesB );

        /* set up the biomaterials */
        biomaterials = new HashSet<BioMaterial>();

        // 2 replicates
        biomaterial0a = BioMaterial.Factory.newInstance();
        biomaterial0a.setName( "0a" );
        Collection<FactorValue> factorValuesForBioMaterial0 = new HashSet<FactorValue>();
        factorValuesForBioMaterial0.add( factorValueA1 );
        factorValuesForBioMaterial0.add( factorValueB1 );
        biomaterial0a.setFactorValues( factorValuesForBioMaterial0 );

        biomaterial0b = BioMaterial.Factory.newInstance();
        biomaterial0b.setName( "0b" );
        biomaterial0b.setFactorValues( factorValuesForBioMaterial0 );

        // 2 replicates
        biomaterial1a = BioMaterial.Factory.newInstance();
        biomaterial1a.setName( "1a" );
        Collection<FactorValue> factorValuesForBioMaterial1 = new HashSet<FactorValue>();
        factorValuesForBioMaterial1.add( factorValueA1 );
        factorValuesForBioMaterial1.add( factorValueB2 );
        biomaterial1a.setFactorValues( factorValuesForBioMaterial1 );

        biomaterial1b = BioMaterial.Factory.newInstance();
        biomaterial1b.setName( "1b" );
        biomaterial1b.setFactorValues( factorValuesForBioMaterial1 );

        // 2 replicates
        biomaterial2a = BioMaterial.Factory.newInstance();
        biomaterial2a.setName( "2a" );
        Collection<FactorValue> factorValuesForBioMaterial2 = new HashSet<FactorValue>();
        factorValuesForBioMaterial2.add( factorValueA2 );
        factorValuesForBioMaterial2.add( factorValueB1 );
        biomaterial2a.setFactorValues( factorValuesForBioMaterial2 );

        biomaterial2b = BioMaterial.Factory.newInstance();
        biomaterial2b.setName( "2b" );
        biomaterial2b.setFactorValues( factorValuesForBioMaterial2 );

        // 2 replicates
        biomaterial3a = BioMaterial.Factory.newInstance();
        biomaterial3a.setName( "3a" );
        Collection<FactorValue> factorValuesForBioMaterial3 = new HashSet<FactorValue>();
        factorValuesForBioMaterial3.add( factorValueA2 );
        factorValuesForBioMaterial3.add( factorValueB2 );
        biomaterial3a.setFactorValues( factorValuesForBioMaterial3 );

        biomaterial3b = BioMaterial.Factory.newInstance();
        biomaterial3b.setName( "3b" );
        biomaterial3b.setFactorValues( factorValuesForBioMaterial3 );

        biomaterials.add( biomaterial0a );
        biomaterials.add( biomaterial0b );
        biomaterials.add( biomaterial1a );
        biomaterials.add( biomaterial1b );
        biomaterials.add( biomaterial2a );
        biomaterials.add( biomaterial2b );
        biomaterials.add( biomaterial3a );
        biomaterials.add( biomaterial3b );

        /* set up the bioassays */
        bioAssay0a = BioAssay.Factory.newInstance();
        bioAssay0a.setName( "a test bioassay 0a" );
        Collection<BioMaterial> samplesUsed0a = new HashSet<BioMaterial>();
        samplesUsed0a.add( biomaterial0a );
        bioAssay0a.setSamplesUsed( samplesUsed0a );

        bioAssay0b = BioAssay.Factory.newInstance();
        bioAssay0b.setName( "a test bioassay 0b" );
        Collection<BioMaterial> samplesUsed0b = new HashSet<BioMaterial>();
        samplesUsed0b.add( biomaterial0b );
        bioAssay0b.setSamplesUsed( samplesUsed0b );

        bioAssay1a = BioAssay.Factory.newInstance();
        bioAssay1a.setName( "a test bioassay 1a" );
        Collection<BioMaterial> samplesUsed1a = new HashSet<BioMaterial>();
        samplesUsed1a.add( biomaterial1a );
        bioAssay1a.setSamplesUsed( samplesUsed1a );

        bioAssay1b = BioAssay.Factory.newInstance();
        bioAssay1b.setName( "a test bioassay 1b" );
        Collection<BioMaterial> samplesUsed1b = new HashSet<BioMaterial>();
        samplesUsed1b.add( biomaterial1b );
        bioAssay1b.setSamplesUsed( samplesUsed1b );

        bioAssay2a = BioAssay.Factory.newInstance();
        bioAssay2a.setName( "a test bioassay 2a" );
        Collection<BioMaterial> samplesUsed2a = new HashSet<BioMaterial>();
        samplesUsed2a.add( biomaterial2a );
        bioAssay2a.setSamplesUsed( samplesUsed2a );

        bioAssay2b = BioAssay.Factory.newInstance();
        bioAssay2b.setName( "a test bioassay 2b" );
        Collection<BioMaterial> samplesUsed2b = new HashSet<BioMaterial>();
        samplesUsed2b.add( biomaterial2b );
        bioAssay2b.setSamplesUsed( samplesUsed2b );

        bioAssay3a = BioAssay.Factory.newInstance();
        bioAssay3a.setName( "a test bioassay 3a" );
        Collection<BioMaterial> samplesUsed3a = new HashSet<BioMaterial>();
        samplesUsed3a.add( biomaterial3a );
        bioAssay3a.setSamplesUsed( samplesUsed3a );

        BioAssay bioAssay3b = BioAssay.Factory.newInstance();
        bioAssay3b.setName( "a test bioassay 3b" );
        Collection<BioMaterial> samplesUsed3b = new HashSet<BioMaterial>();
        samplesUsed3b.add( biomaterial3b );
        bioAssay3b.setSamplesUsed( samplesUsed3b );

        bioAssays = new HashSet<BioAssay>();
        bioAssays.add( bioAssay0a );
        bioAssays.add( bioAssay0b );
        bioAssays.add( bioAssay1a );
        bioAssays.add( bioAssay1b );
        bioAssays.add( bioAssay2a );
        bioAssays.add( bioAssay2b );
        bioAssays.add( bioAssay3a );
        bioAssays.add( bioAssay3b );

        expressionExperiment.setBioAssays( bioAssays );

        experimentalFactors = new HashSet<ExperimentalFactor>();
        experimentalFactors.add( experimentalFactorA );
        experimentalFactors.add( experimentalFactorB );

        experimentalDesign = ExperimentalDesign.Factory.newInstance();
        experimentalDesign.setName( "a test experimental design" );
        experimentalDesign.setExperimentalFactors( experimentalFactors );

        expressionExperiment.setExperimentalDesign( experimentalDesign );

        quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setName( "a test quantitation type" );
        quantitationType.setType( StandardQuantitationType.AMOUNT );
        quantitationType.setIsPreferred( true );
        quantitationType.setIsBackground( false );
        quantitationType.setIsNormalized( false );
        quantitationType.setIsBackgroundSubtracted( false );
        quantitationType.setIsRatio( false );

        bioAssayDimension = BioAssayDimension.Factory.newInstance();
        bioAssayDimension.setName( "test bioassay dimension" );
        bioAssayDimension.setBioAssays( bioAssays );

        configureVectors( NUM_BIOASSAYS );

        expressionExperiment.setDesignElementDataVectors( vectors );
    }

    private void configureVectors( int numAssays ) {
        vectors = new HashSet<DesignElementDataVector>();
        for ( int i = 0; i < NUM_DESIGN_ELEMENTS; i++ ) {
            DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
            vector.setBioAssayDimension( bioAssayDimension );
            vector.setQuantitationType( quantitationType );

            DesignElement de = CompositeSequence.Factory.newInstance();
            de.setName( String.valueOf( i ) );
            vector.setDesignElement( de );
            vectors.add( vector );

            double[] dvals = new double[numAssays];
            for ( int j = 0; j < dvals.length; j++ ) {
                dvals[j] = RandomUtils.nextDouble();
            }
            byte[] bvals = bac.doubleArrayToBytes( dvals );
            vector.setData( bvals );
        }
    }

    public void configureTestDataForTwoWayAnovaWithInteractions() {

        /* this is the default configuration */
    }

    /**
     * Removes the replicates.
     */
    public void configureTestDataForTwoWayAnovaWithoutInteractions() {

        /* remove the "replicates" */
        bioAssays.remove( bioAssay0b );
        bioAssays.remove( bioAssay1b );
        bioAssays.remove( bioAssay2b );
        bioAssays.remove( bioAssay3b );

        expressionExperiment.setBioAssays( bioAssays );

        bioAssayDimension.setBioAssays( bioAssays );

        configureVectors( NUM_BIOASSAYS / 2 );

    }

    public void configureTestDataForOneWayAnova() {

        Collection<BioMaterial> updatedBiomaterials = new HashSet<BioMaterial>();
        for ( BioMaterial m : biomaterials ) {
            Collection<FactorValue> fvs = m.getFactorValues();
            for ( FactorValue fv : fvs ) {
                if ( fv.getExperimentalFactor().getName() != experimentalFactorB.getName() ) {
                    updatedBiomaterials.add( m );
                }
            }
        }

        biomaterials = updatedBiomaterials;

        experimentalFactors.remove( experimentalFactorB );
        experimentalDesign.setExperimentalFactors( experimentalFactors );
        expressionExperiment.setExperimentalDesign( experimentalDesign );
    }
}
