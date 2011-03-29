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
import org.easymock.classextension.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.ByteArrayConverter;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.r.RClient;
import ubic.basecode.util.r.RConnectionFactory;
import ubic.basecode.util.r.RServeClient;
import ubic.gemma.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
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
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.testing.BaseSpringContextTest;
import ubic.gemma.util.ConfigUtils;

/**
 * Other tests can extend this class if they want an expression experiment with complete block design and biological
 * replicates.
 * 
 * @author keshav
 * @version $Id$
 */
public abstract class BaseAnalyzerConfigurationTest extends BaseSpringContextTest {

    protected static final int NUM_BIOASSAYS = 8;

    protected static final int NUM_DESIGN_ELEMENTS = 100;

    protected ArrayDesign arrayDesign = null;

    protected BioAssayDimension bioAssayDimension = null;

    protected List<BioMaterial> biomaterials = null;

    protected boolean connected = false;

    protected ExperimentalDesign experimentalDesign = null;

    protected ExperimentalFactor experimentalFactorA_Area = null;

    protected ExperimentalFactor experimentalFactorB = null;

    protected List<ExperimentalFactor> experimentalFactors = null;

    @Autowired
    protected ExpressionDataMatrixService expressionDataMatrixService = null;
    protected ExpressionExperiment expressionExperiment = null;

    protected final int NUM_TWA_RESULT_SETS = 3;

    @Autowired
    protected ProcessedExpressionDataVectorService processedExpressionDataVectorService = null;

    protected QuantitationType quantitationType = null;

    protected FactorValue factorValueA1;
    protected FactorValue factorValueA2;
    protected FactorValue factorValueB2;

    private ByteArrayConverter bac = new ByteArrayConverter();

    private BioAssay bioAssay0a = null;
    private BioAssay bioAssay0b = null;
    private BioAssay bioAssay1a = null;
    private BioAssay bioAssay1b = null;
    private BioAssay bioAssay2a = null;
    private BioAssay bioAssay2b = null;
    private BioAssay bioAssay3a = null;
    private BioAssay bioAssay3b = null;

    private List<BioAssay> bioAssays = null;

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

    private RClient rc = null;

    private Collection<ProcessedExpressionDataVector> vectors = null;

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.testing.BaseSpringContextTest#onSetUpInTransaction()
     */
    @Before
    public void setup() throws Exception {

        try {
            if ( ConfigUtils.getBoolean( "gemma.linearmodels.useR" ) ) {
                rc = RConnectionFactory.getRConnection( ConfigUtils.getString( "gemma.rserve.hostname", "localhost" ) );

                if ( rc != null && rc.isConnected() ) {
                    connected = true;
                    /*
                     * We have to disconnect right away for test to work under Windows, where only one connection is
                     * allowed at a time. The classes under test will get their own connections.
                     */
                    if ( rc != null && rc.isConnected() && rc instanceof RServeClient )
                        ( ( RServeClient ) rc ).disconnect();
                }
            } else {
                connected = true; // not using R
            }
        } catch ( Exception e ) {
            log.warn( e.getMessage() );
        }

        /* array designs */
        arrayDesign = ArrayDesign.Factory.newInstance();
        arrayDesign.setTechnologyType( TechnologyType.ONECOLOR );
        arrayDesign.setId( 1L );
        arrayDesign.setName( "MG-U74Test_" + RandomStringUtils.randomAlphanumeric( 12 ) );
        arrayDesign.setPrimaryTaxon( taxonService.findByCommonName( "mouse" ) );

        expressionExperiment = ExpressionExperiment.Factory.newInstance();
        expressionExperiment.setName( "analysistest_" + RandomStringUtils.randomAlphanumeric( 12 ) );
        expressionExperiment.setId( 100009L );
        expressionExperiment.setShortName( RandomStringUtils.randomAlphanumeric( 12 ) );

        /* experimental factor "area" */
        experimentalFactorA_Area = ExperimentalFactor.Factory.newInstance();
        experimentalFactorA_Area.setName( "area" );
        experimentalFactorA_Area.setType( FactorType.CATEGORICAL );
        experimentalFactorA_Area.setId( 5001L );
        factorValuesA = new HashSet<FactorValue>();

        factorValueA1 = FactorValue.Factory.newInstance();
        factorValueA1.setId( 1001L );
        factorValueA1.setValue( "cerebellum" );
        Characteristic characteristicA1 = Characteristic.Factory.newInstance();
        characteristicA1.setValue( factorValueA1.getValue() );
        Collection<Characteristic> characteristicsA1 = new HashSet<Characteristic>();
        characteristicsA1.add( characteristicA1 );
        factorValueA1.setCharacteristics( characteristicsA1 );
        factorValueA1.setExperimentalFactor( experimentalFactorA_Area );

        factorValueA2 = FactorValue.Factory.newInstance();
        factorValueA2.setValue( "amygdala" ); // this will automatically be set as the baseline
        factorValueA2.setId( 1002L );
        Characteristic characteristicA2 = Characteristic.Factory.newInstance();
        characteristicA2.setValue( factorValueA2.getValue() );
        Collection<Characteristic> characteristicsA2 = new HashSet<Characteristic>();
        characteristicsA2.add( characteristicA2 );
        factorValueA2.setCharacteristics( characteristicsA2 );
        factorValueA2.setExperimentalFactor( experimentalFactorA_Area );

        factorValuesA.add( factorValueA1 );
        factorValuesA.add( factorValueA2 );
        experimentalFactorA_Area.getFactorValues().addAll( factorValuesA );

        /* experimental factor "treat" */
        experimentalFactorB = ExperimentalFactor.Factory.newInstance();
        experimentalFactorB.setName( "treat" );
        experimentalFactorB.setId( 5002L );
        experimentalFactorB.setType( FactorType.CATEGORICAL );

        factorValuesB = new HashSet<FactorValue>();

        FactorValue factorValueB1 = FactorValue.Factory.newInstance();
        factorValueB1.setValue( "pcp" );
        factorValueB1.setId( 1003L );
        Characteristic characteristicB1 = Characteristic.Factory.newInstance();
        characteristicB1.setValue( factorValueB1.getValue() );
        Collection<Characteristic> characteristicsB1 = new HashSet<Characteristic>();
        characteristicsB1.add( characteristicB1 );
        factorValueB1.setCharacteristics( characteristicsB1 );
        factorValueB1.setExperimentalFactor( experimentalFactorB );

        factorValueB2 = FactorValue.Factory.newInstance();
        factorValueB2.setValue( "control_group" );
        factorValueB2.setId( 1004L );
        Characteristic characteristicB2 = Characteristic.Factory.newInstance();
        characteristicB2.setValue( factorValueB2.getValue() );
        Collection<Characteristic> characteristicsB2 = new HashSet<Characteristic>();
        characteristicsB2.add( characteristicB2 );
        factorValueB2.setCharacteristics( characteristicsB2 );
        factorValueB2.setExperimentalFactor( experimentalFactorB );

        factorValuesB.add( factorValueB1 );
        factorValuesB.add( factorValueB2 );

        experimentalFactorB.getFactorValues().addAll( factorValuesB );

        /* set up the biomaterials */
        biomaterials = new ArrayList<BioMaterial>();

        // 2 replicates
        biomaterial0a = BioMaterial.Factory.newInstance();
        biomaterial0a.setName( "0a" );
        Collection<FactorValue> factorValuesForBioMaterial0 = new HashSet<FactorValue>();
        factorValuesForBioMaterial0.add( factorValueA1 );
        factorValuesForBioMaterial0.add( factorValueB1 );
        biomaterial0a.getFactorValues().addAll( factorValuesForBioMaterial0 );

        biomaterial0b = BioMaterial.Factory.newInstance();
        biomaterial0b.setName( "0b" );
        biomaterial0b.getFactorValues().addAll( factorValuesForBioMaterial0 );

        // 2 replicates
        biomaterial1a = BioMaterial.Factory.newInstance();
        biomaterial1a.setName( "1a" );
        Collection<FactorValue> factorValuesForBioMaterial1 = new HashSet<FactorValue>();
        factorValuesForBioMaterial1.add( factorValueA1 );
        factorValuesForBioMaterial1.add( factorValueB2 );
        biomaterial1a.getFactorValues().addAll( factorValuesForBioMaterial1 );

        biomaterial1b = BioMaterial.Factory.newInstance();
        biomaterial1b.setName( "1b" );
        biomaterial1b.getFactorValues().addAll( factorValuesForBioMaterial1 );

        // 2 replicates
        biomaterial2a = BioMaterial.Factory.newInstance();
        biomaterial2a.setName( "2a" );
        Collection<FactorValue> factorValuesForBioMaterial2 = new HashSet<FactorValue>();
        factorValuesForBioMaterial2.add( factorValueA2 );
        factorValuesForBioMaterial2.add( factorValueB1 );
        biomaterial2a.getFactorValues().addAll( factorValuesForBioMaterial2 );

        biomaterial2b = BioMaterial.Factory.newInstance();
        biomaterial2b.setName( "2b" );
        biomaterial2b.getFactorValues().addAll( factorValuesForBioMaterial2 );

        // 2 replicates
        biomaterial3a = BioMaterial.Factory.newInstance();
        biomaterial3a.setName( "3a" );
        Collection<FactorValue> factorValuesForBioMaterial3 = new HashSet<FactorValue>();
        factorValuesForBioMaterial3.add( factorValueA2 );
        factorValuesForBioMaterial3.add( factorValueB2 );
        biomaterial3a.getFactorValues().addAll( factorValuesForBioMaterial3 );

        biomaterial3b = BioMaterial.Factory.newInstance();
        biomaterial3b.setName( "3b" );
        biomaterial3b.getFactorValues().addAll( factorValuesForBioMaterial3 );

        biomaterial0a.setId( 100000L );
        biomaterial0b.setId( 100001L );
        biomaterial1a.setId( 100002L );
        biomaterial1b.setId( 100003L );
        biomaterial2a.setId( 100004L );
        biomaterial2b.setId( 100005L );
        biomaterial3a.setId( 100006L );
        biomaterial3b.setId( 100007L );

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
        bioAssay0a.getSamplesUsed().addAll( samplesUsed0a );
        bioAssay0a.setArrayDesignUsed( arrayDesign );

        bioAssay0b = BioAssay.Factory.newInstance();
        bioAssay0b.setName( "bioassay 0b" );
        Collection<BioMaterial> samplesUsed0b = new HashSet<BioMaterial>();
        samplesUsed0b.add( biomaterial0b );
        bioAssay0b.getSamplesUsed().addAll( samplesUsed0b );

        bioAssay0b.setArrayDesignUsed( arrayDesign );

        bioAssay1a = BioAssay.Factory.newInstance();
        bioAssay1a.setName( "bioassay 1a" );
        Collection<BioMaterial> samplesUsed1a = new HashSet<BioMaterial>();
        samplesUsed1a.add( biomaterial1a );
        bioAssay1a.getSamplesUsed().addAll( samplesUsed1a );
        bioAssay1a.setArrayDesignUsed( arrayDesign );

        bioAssay1b = BioAssay.Factory.newInstance();
        bioAssay1b.setName( "bioassay 1b" );
        Collection<BioMaterial> samplesUsed1b = new HashSet<BioMaterial>();
        samplesUsed1b.add( biomaterial1b );
        bioAssay1b.getSamplesUsed().addAll( samplesUsed1b );
        bioAssay1b.setArrayDesignUsed( arrayDesign );

        bioAssay2a = BioAssay.Factory.newInstance();
        bioAssay2a.setName( "bioassay 2a" );
        Collection<BioMaterial> samplesUsed2a = new HashSet<BioMaterial>();
        samplesUsed2a.add( biomaterial2a );
        bioAssay2a.getSamplesUsed().addAll( samplesUsed2a );
        bioAssay2a.setArrayDesignUsed( arrayDesign );

        bioAssay2b = BioAssay.Factory.newInstance();
        bioAssay2b.setName( "bioassay 2b" );
        Collection<BioMaterial> samplesUsed2b = new HashSet<BioMaterial>();
        samplesUsed2b.add( biomaterial2b );
        bioAssay2b.getSamplesUsed().addAll( samplesUsed2b );
        bioAssay2b.setArrayDesignUsed( arrayDesign );

        bioAssay3a = BioAssay.Factory.newInstance();
        bioAssay3a.setName( "bioassay 3a" );
        Collection<BioMaterial> samplesUsed3a = new HashSet<BioMaterial>();
        samplesUsed3a.add( biomaterial3a );
        bioAssay3a.getSamplesUsed().addAll( samplesUsed3a );
        bioAssay3a.setArrayDesignUsed( arrayDesign );

        bioAssay3b = BioAssay.Factory.newInstance();
        bioAssay3b.setName( "bioassay 3b" );
        Collection<BioMaterial> samplesUsed3b = new HashSet<BioMaterial>();
        samplesUsed3b.add( biomaterial3b );
        bioAssay3b.getSamplesUsed().addAll( samplesUsed3b );
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

        biomaterial0a.getBioAssaysUsedIn().add( bioAssay0a );
        biomaterial0b.getBioAssaysUsedIn().add( bioAssay0b );
        biomaterial1a.getBioAssaysUsedIn().add( bioAssay1a );
        biomaterial1b.getBioAssaysUsedIn().add( bioAssay1b );
        biomaterial2a.getBioAssaysUsedIn().add( bioAssay2a );
        biomaterial2b.getBioAssaysUsedIn().add( bioAssay2b );
        biomaterial3a.getBioAssaysUsedIn().add( bioAssay3a );
        biomaterial3b.getBioAssaysUsedIn().add( bioAssay3b );

        expressionExperiment.setBioAssays( bioAssays );

        experimentalFactors = new ArrayList<ExperimentalFactor>();
        experimentalFactors.add( experimentalFactorA_Area );
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
        quantitationType.setScale( ScaleType.LOG2 );
        quantitationType.setIsNormalized( false );
        quantitationType.setIsBackgroundSubtracted( false );
        quantitationType.setIsRatio( false );
        expressionExperiment.getQuantitationTypes().add( quantitationType );

        bioAssayDimension = BioAssayDimension.Factory.newInstance();
        bioAssayDimension.setName( "test bioassay dimension" );
        bioAssayDimension.setBioAssays( bioAssays );

        configureVectors( biomaterials, null );
    }

    @After
    public void tearDown() throws Exception {
        if ( rc != null && rc.isConnected() && rc instanceof RServeClient ) ( ( RServeClient ) rc ).disconnect();
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
     * Configure the test data for one way anova.
     */
    protected void configureTestDataForOneWayAnova() throws Exception {

        /*
         * This really configures it for a t-test, which is just a one way anova if there are only 2 factor values
         */
        log.debug( "Configuring test data for one way anova." );

        List<BioMaterial> updatedBiomaterials = new ArrayList<BioMaterial>();
        for ( BioMaterial m : biomaterials ) {
            Collection<FactorValue> fvs = m.getFactorValues();
            Collection<FactorValue> updatedFactorValues = new HashSet<FactorValue>();
            for ( FactorValue fv : fvs ) {
                if ( !fv.getExperimentalFactor().getName().equals( experimentalFactorB.getName() ) ) {
                    continue;
                }
                updatedFactorValues.add( fv );

                m.getFactorValues().addAll( updatedFactorValues );
                updatedBiomaterials.add( m );
            }
        }

        biomaterials = updatedBiomaterials;

        configureVectors( biomaterials, null );

        experimentalFactors.remove( experimentalFactorA_Area );
        experimentalDesign.setExperimentalFactors( experimentalFactors );
        expressionExperiment.setExperimentalDesign( experimentalDesign );
    }

    /**
     * Configure the test data for two way anova without interactions that isn't valid for a two-way. F
     * <p>
     * Removes the replicates.
     */
    protected void configureTestDataForTwoWayAnovaWithoutInteractions() throws Exception {

        log.info( "Configuring test data for two way anova without interactions." );

        /* remove the "replicates" */
        bioAssays.remove( bioAssay0b );
        bioAssays.remove( bioAssay1b );
        bioAssays.remove( bioAssay2b );
        bioAssays.remove( bioAssay3b );

        expressionExperiment.setBioAssays( bioAssays );

        bioAssayDimension.setBioAssays( bioAssays );

        biomaterials = new ArrayList<BioMaterial>();
        for ( BioAssay b : bioAssayDimension.getBioAssays() ) {
            biomaterials.add( b.getSamplesUsed().iterator().next() );
        }

        configureVectors( biomaterials, null );

    }

    /**
     * @param numAssays
     */
    protected void configureVectors( List<BioMaterial> bioMaterials, String resourcePath ) throws Exception {
        this.vectors = new HashSet<ProcessedExpressionDataVector>();

        DoubleMatrixReader r = new DoubleMatrixReader();

        String path;
        if ( resourcePath == null ) {
            path = "/data/stat-tests/anova-test-data.txt";
        } else {
            path = resourcePath;
        }

        DoubleMatrix<String, String> dataMatrix = r.read( this.getClass().getResourceAsStream( path ) );
        // RandomData randomData = new RandomDataImpl( new MersenneTwister( 0 ) ); // fixed seed - important!

        Collection<CompositeSequence> compositeSequences = new HashSet<CompositeSequence>();
        for ( int i = 0; i < NUM_DESIGN_ELEMENTS; i++ ) {
            ProcessedExpressionDataVector vector = ProcessedExpressionDataVector.Factory.newInstance();
            vector.setBioAssayDimension( bioAssayDimension );
            vector.setQuantitationType( quantitationType );

            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( dataMatrix.getRowName( i ) );
            cs.setId( i + 1000L );
            cs.setArrayDesign( arrayDesign );
            vector.setDesignElement( cs );
            vector.setId( i + 10000L );

            double[] dvals = new double[bioMaterials.size()];
            for ( int j = 0; j < dvals.length; j++ ) {

                // if ( randomData.nextUniform( 0, 1 ) < FRACTION_MISSING_DATA ) {
                // dvals[j] = Double.NaN;
                // } else if ( i < 20 ) {
                // // make it a little more interesting
                // if ( bioMaterials.get( j ).getFactorValues().contains( this.factorValuesA.iterator().next() ) ) {
                // dvals[j] = dataMatrix.get( i, j ) + Math.abs( randomData.nextGaussian( 200, 1 ) );
                // } else if ( bioMaterials.get( j ).getFactorValues().contains( this.factorValuesB.iterator().next() )
                // ) {
                // dvals[j] = dataMatrix.get( i, j ) - Math.abs( randomData.nextGaussian( 4, 1 ) );
                // } else {
                // dvals[j] = dataMatrix.get( i, j );
                // }
                // } else {
                dvals[j] = dataMatrix.get( i, j );
                // }
            }

            byte[] bvals = bac.doubleArrayToBytes( dvals );
            vector.setData( bvals );

            vectors.add( vector );

            compositeSequences.add( cs );
        }
        expressionExperiment.setProcessedExpressionDataVectors( vectors );

        arrayDesign.setCompositeSequences( compositeSequences );
    }
}
