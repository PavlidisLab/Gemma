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
package ubic.gemma.analysis.expression.diff;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.math.random.MersenneTwister;
import org.apache.commons.math.random.RandomData;
import org.apache.commons.math.random.RandomDataImpl;
import org.easymock.classextension.EasyMock;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.RClient;
import ubic.basecode.util.RConnectionFactory;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.analysis.expression.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.ProbeAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVectorService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
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
public abstract class BaseAnalyzerConfigurationTest extends BaseSpringContextTest {

    private static final double FRACTION_MISSING_DATA = 0.2;

    protected static final int NUM_DESIGN_ELEMENTS = 100;

    protected static final int NUM_BIOASSAYS = 8;

    protected final int NUM_TWA_RESULT_SETS = 3;

    protected ArrayDesign arrayDesign = null;

    protected ExpressionExperiment expressionExperiment = null;

    protected ExperimentalDesign experimentalDesign = null;

    protected List<BioMaterial> biomaterials = null;

    protected QuantitationType quantitationType = null;

    protected BioAssayDimension bioAssayDimension = null;

    protected Collection<ExperimentalFactor> experimentalFactors = null;

    protected ExperimentalFactor experimentalFactorA = null;
    protected ExperimentalFactor experimentalFactorB = null;

    private BioMaterial biomaterial0a = null;
    private BioMaterial biomaterial0b = null;
    private BioMaterial biomaterial1a = null;
    private BioMaterial biomaterial1b = null;
    private BioMaterial biomaterial2a = null;
    private BioMaterial biomaterial2b = null;
    private BioMaterial biomaterial3a = null;
    private BioMaterial biomaterial3b = null;

    private Collection<FactorValue> factorValuesA = null;

    private Collection<FactorValue> factorValuesB = null;

    private List<BioAssay> bioAssays = null;

    private BioAssay bioAssay0a = null;
    private BioAssay bioAssay0b = null;
    private BioAssay bioAssay1a = null;
    private BioAssay bioAssay1b = null;
    private BioAssay bioAssay2a = null;
    private BioAssay bioAssay2b = null;
    private BioAssay bioAssay3a = null;
    private BioAssay bioAssay3b = null;

    private Collection<ProcessedExpressionDataVector> vectors = null;

    private ByteArrayConverter bac = new ByteArrayConverter();

    private RClient rc = null;
    protected boolean connected = false;

    @Autowired
    protected ExpressionDataMatrixService expressionDataMatrixService = null;

    @Autowired
    protected ProcessedExpressionDataVectorService processedExpressionDataVectorService = null;

    /**
     * Configure the test data for one way anova.
     */
    public void configureTestDataForOneWayAnova() throws Exception {

        /*
         * TODO This really configures it for a t-test, which is just a one way anova if there are only 2 factor values.
         * Rename this to configureTestDataForTTest and create a separate method with test data for one way anova with 3
         * (or more factor value).
         */
        log.info( "Configuring test data for one way anova." );

        List<BioMaterial> updatedBiomaterials = new ArrayList<BioMaterial>();
        for ( BioMaterial m : biomaterials ) {
            Collection<FactorValue> fvs = m.getFactorValues();
            Collection<FactorValue> updatedFactorValues = new HashSet<FactorValue>();
            for ( FactorValue fv : fvs ) {
                if ( fv.getExperimentalFactor().getName() != experimentalFactorB.getName() ) {
                    continue;
                }
                updatedFactorValues.add( fv );

                m.setFactorValues( updatedFactorValues );
                updatedBiomaterials.add( m );
            }
        }

        biomaterials = updatedBiomaterials;

        configureVectors( biomaterials );

        experimentalFactors.remove( experimentalFactorA );
        experimentalDesign.setExperimentalFactors( experimentalFactors );
        expressionExperiment.setExperimentalDesign( experimentalDesign );
    }

    /**
     * The default test data configuration.
     */
    public void configureTestDataForTwoWayAnovaWithInteractions() {

        /* this is the default configuration */
        log.info( "This is the default configuration of the test data." );
    }

    /**
     * Configure the test data for two way anova without interactions.
     * <p>
     * Removes the replicates.
     */
    public void configureTestDataForTwoWayAnovaWithoutInteractions() throws Exception {

        log.info( "Configuring test data for two way anova without interactions." );

        /* remove the "replicates" */
        bioAssays.remove( bioAssay0b );
        bioAssays.remove( bioAssay1b );
        bioAssays.remove( bioAssay2b );
        bioAssays.remove( bioAssay3b );

        expressionExperiment.setBioAssays( bioAssays );

        bioAssayDimension.setBioAssays( bioAssays );

        List<BioMaterial> biomaterials = new ArrayList<BioMaterial>();
        for ( BioAssay b : bioAssayDimension.getBioAssays() ) {
            biomaterials.add( b.getSamplesUsed().iterator().next() );
        }

        configureVectors( biomaterials );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {

        rc = RConnectionFactory.getRConnection();
        if ( rc != null && rc.isConnected() ) {
            connected = true;
        }

        /* array designs */
        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setTechnologyType( TechnologyType.ONECOLOR );
        arrayDesign.setId( 1L );
        arrayDesign.setName( "MG-U74Test_" + RandomStringUtils.randomAlphanumeric( 12 ) );
        arrayDesign.setPrimaryTaxon( taxonService.findByCommonName( "mouse" ) );

        expressionExperiment = ExpressionExperiment.Factory.newInstance();
        expressionExperiment.setName( "analysistest_" + RandomStringUtils.randomAlphanumeric( 12 ) );
        expressionExperiment.setShortName( RandomStringUtils.randomAlphanumeric( 12 ) );

        /* experimental factor "area" */
        experimentalFactorA = ExperimentalFactor.Factory.newInstance();
        experimentalFactorA.setName( "area" );
        factorValuesA = new HashSet<FactorValue>();

        FactorValue factorValueA1 = FactorValue.Factory.newInstance();
        factorValueA1.setId( 1001L );
        factorValueA1.setValue( "cerebellum" );
        Characteristic characteristicA1 = Characteristic.Factory.newInstance();
        characteristicA1.setValue( factorValueA1.getValue() );
        Collection<Characteristic> characteristicsA1 = new HashSet<Characteristic>();
        characteristicsA1.add( characteristicA1 );
        factorValueA1.setCharacteristics( characteristicsA1 );
        factorValueA1.setExperimentalFactor( experimentalFactorA );

        FactorValue factorValueA2 = FactorValue.Factory.newInstance();
        factorValueA2.setValue( "amygdala" );
        factorValueA2.setId( 1002L );
        Characteristic characteristicA2 = Characteristic.Factory.newInstance();
        characteristicA2.setValue( factorValueA2.getValue() );
        Collection<Characteristic> characteristicsA2 = new HashSet<Characteristic>();
        characteristicsA2.add( characteristicA2 );
        factorValueA2.setCharacteristics( characteristicsA2 );
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
        factorValueB1.setId( 1003L );
        Characteristic characteristicB1 = Characteristic.Factory.newInstance();
        characteristicB1.setValue( factorValueB1.getValue() );
        Collection<Characteristic> characteristicsB1 = new HashSet<Characteristic>();
        characteristicsB1.add( characteristicB1 );
        factorValueB1.setCharacteristics( characteristicsB1 );
        factorValueB1.setExperimentalFactor( experimentalFactorB );

        FactorValue factorValueB2 = FactorValue.Factory.newInstance();
        factorValueB2.setValue( "pcp" );
        factorValueB2.setId( 1004L );
        Characteristic characteristicB2 = Characteristic.Factory.newInstance();
        characteristicB2.setValue( factorValueB2.getValue() );
        Collection<Characteristic> characteristicsB2 = new HashSet<Characteristic>();
        characteristicsB2.add( characteristicB2 );
        factorValueB2.setCharacteristics( characteristicsB2 );
        factorValueB2.setExperimentalFactor( experimentalFactorB );

        factorValuesB.add( factorValueB1 );
        factorValuesB.add( factorValueB2 );

        experimentalFactorB.setFactorValues( factorValuesB );

        /* set up the biomaterials */
        biomaterials = new ArrayList<BioMaterial>();

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
        bioAssay0a.setName( "bioassay 0a" );
        Collection<BioMaterial> samplesUsed0a = new HashSet<BioMaterial>();
        samplesUsed0a.add( biomaterial0a );
        bioAssay0a.setSamplesUsed( samplesUsed0a );
        bioAssay0a.setArrayDesignUsed( arrayDesign );

        bioAssay0b = BioAssay.Factory.newInstance();
        bioAssay0b.setName( "bioassay 0b" );
        Collection<BioMaterial> samplesUsed0b = new HashSet<BioMaterial>();
        samplesUsed0b.add( biomaterial0b );
        bioAssay0b.setSamplesUsed( samplesUsed0b );
        bioAssay0b.setArrayDesignUsed( arrayDesign );

        bioAssay1a = BioAssay.Factory.newInstance();
        bioAssay1a.setName( "bioassay 1a" );
        Collection<BioMaterial> samplesUsed1a = new HashSet<BioMaterial>();
        samplesUsed1a.add( biomaterial1a );
        bioAssay1a.setSamplesUsed( samplesUsed1a );
        bioAssay1a.setArrayDesignUsed( arrayDesign );

        bioAssay1b = BioAssay.Factory.newInstance();
        bioAssay1b.setName( "bioassay 1b" );
        Collection<BioMaterial> samplesUsed1b = new HashSet<BioMaterial>();
        samplesUsed1b.add( biomaterial1b );
        bioAssay1b.setSamplesUsed( samplesUsed1b );
        bioAssay1b.setArrayDesignUsed( arrayDesign );

        bioAssay2a = BioAssay.Factory.newInstance();
        bioAssay2a.setName( "bioassay 2a" );
        Collection<BioMaterial> samplesUsed2a = new HashSet<BioMaterial>();
        samplesUsed2a.add( biomaterial2a );
        bioAssay2a.setSamplesUsed( samplesUsed2a );
        bioAssay2a.setArrayDesignUsed( arrayDesign );

        bioAssay2b = BioAssay.Factory.newInstance();
        bioAssay2b.setName( "bioassay 2b" );
        Collection<BioMaterial> samplesUsed2b = new HashSet<BioMaterial>();
        samplesUsed2b.add( biomaterial2b );
        bioAssay2b.setSamplesUsed( samplesUsed2b );
        bioAssay2b.setArrayDesignUsed( arrayDesign );

        bioAssay3a = BioAssay.Factory.newInstance();
        bioAssay3a.setName( "bioassay 3a" );
        Collection<BioMaterial> samplesUsed3a = new HashSet<BioMaterial>();
        samplesUsed3a.add( biomaterial3a );
        bioAssay3a.setSamplesUsed( samplesUsed3a );
        bioAssay3a.setArrayDesignUsed( arrayDesign );

        bioAssay3b = BioAssay.Factory.newInstance();
        bioAssay3b.setName( "bioassay 3b" );
        Collection<BioMaterial> samplesUsed3b = new HashSet<BioMaterial>();
        samplesUsed3b.add( biomaterial3b );
        bioAssay3b.setSamplesUsed( samplesUsed3b );
        bioAssay3b.setArrayDesignUsed( arrayDesign );

        bioAssays = new ArrayList<BioAssay>();
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
        experimentalDesign.setName( "experimental design" );
        experimentalDesign.setExperimentalFactors( experimentalFactors );

        expressionExperiment.setExperimentalDesign( experimentalDesign );

        quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setName( "quantitation type" );
        quantitationType.setRepresentation( PrimitiveType.DOUBLE );
        quantitationType.setType( StandardQuantitationType.AMOUNT );
        quantitationType.setIsPreferred( true );
        quantitationType.setIsMaskedPreferred( false );
        quantitationType.setIsBackground( false );
        quantitationType.setIsNormalized( false );
        quantitationType.setIsBackgroundSubtracted( false );
        quantitationType.setIsRatio( false );
        expressionExperiment.getQuantitationTypes().add( quantitationType );

        bioAssayDimension = BioAssayDimension.Factory.newInstance();
        bioAssayDimension.setName( "test bioassay dimension" );
        bioAssayDimension.setBioAssays( bioAssays );

        configureVectors( biomaterials );
    }

    /**
     * Mocks the method getVectors in the {@link ExpressionDataMatrixService}.
     * 
     * @param numMethodCalls The number of times the mocked method will be called.
     * @throws Exception
     */
    protected void configureMockAnalysisServiceHelper( int numMethodCalls ) throws Exception {
        this.expressionDataMatrixService = EasyMock.createMock( ExpressionDataMatrixService.class );

        org.easymock.EasyMock.expect(
                expressionDataMatrixService.getProcessedExpressionDataMatrix( expressionExperiment ) ).andReturn(
                new ExpressionDataDoubleMatrix( this.vectors ) ).times( numMethodCalls );
        org.easymock.EasyMock.expect(
                expressionDataMatrixService.getProcessedExpressionDataVectors( expressionExperiment ) ).andReturn(
                this.vectors ).times( numMethodCalls );
        EasyMock.replay( expressionDataMatrixService );
    }

    /**
     * @throws Exception
     */
    protected abstract void configureMocks() throws Exception;

    /**
     * @param resultSet
     */
    protected void checkResults( ExpressionAnalysisResultSet resultSet ) {

        for ( DifferentialExpressionAnalysisResult r : resultSet.getResults() ) {
            ProbeAnalysisResult probeAnalysisResult = ( ProbeAnalysisResult ) r;
            log.debug( "probe: " + probeAnalysisResult.getProbe() + "; p-value: " + probeAnalysisResult.getPvalue() );
        }
    }

    /**
     * @param numAssays
     */
    private void configureVectors( List<BioMaterial> bioMaterials ) throws Exception {
        this.vectors = new HashSet<ProcessedExpressionDataVector>();

        DoubleMatrixReader r = new DoubleMatrixReader();
        DoubleMatrix<String, String> dataMatrix = r.read( this.getClass().getResourceAsStream(
                "/data/loader/aov.results-2-monocyte-data-bytime.bypat.data.sort" ) );
        RandomData randomData = new RandomDataImpl( new MersenneTwister( 0 ) ); // fixed seed

        Collection<CompositeSequence> compositeSequences = new HashSet<CompositeSequence>();
        for ( int i = 0; i < NUM_DESIGN_ELEMENTS; i++ ) {
            ProcessedExpressionDataVector vector = ProcessedExpressionDataVector.Factory.newInstance();
            vector.setBioAssayDimension( bioAssayDimension );
            vector.setQuantitationType( quantitationType );

            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( String.valueOf( i ) );
            cs.setArrayDesign( arrayDesign );
            vector.setDesignElement( cs );

            double[] dvals = new double[bioMaterials.size()];
            for ( int j = 0; j < dvals.length; j++ ) {

                if ( randomData.nextUniform( 0, 1 ) < FRACTION_MISSING_DATA ) {
                    dvals[j] = Double.NaN;
                } else if ( i < 20 ) {
                    // make it a little more interesting
                    if ( bioMaterials.get( j ).getFactorValues().contains( this.factorValuesA.iterator().next() ) ) {
                        dvals[j] = dataMatrix.get( i, j ) + Math.abs( randomData.nextGaussian( 200, 1 ) );
                    } else if ( bioMaterials.get( j ).getFactorValues().contains( this.factorValuesB.iterator().next() ) ) {
                        dvals[j] = dataMatrix.get( i, j ) - Math.abs( randomData.nextGaussian( 4, 1 ) );
                    } else {
                        dvals[j] = dataMatrix.get( i, j );
                    }
                } else {
                    dvals[j] = dataMatrix.get( i, j );
                }
            }

            byte[] bvals = bac.doubleArrayToBytes( dvals );
            vector.setData( bvals );

            vectors.add( vector );

            compositeSequences.add( cs );
        }

        expressionExperiment.getProcessedExpressionDataVectors().addAll( vectors );

        arrayDesign.setCompositeSequences( compositeSequences );
    }
}
