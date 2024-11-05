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
package ubic.gemma.core.analysis.expression.diff;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.basecode.util.r.RClient;
import ubic.basecode.util.r.RConnectionFactory;
import ubic.basecode.util.r.RServeClient;
import ubic.gemma.core.analysis.service.ExpressionDataMatrixService;
import ubic.gemma.core.config.Settings;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.bioAssayData.ProcessedExpressionDataVectorService;

import java.util.*;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Other tests can extend this class if they want an expression experiment with complete block design and biological
 * replicates.
 *
 * @author keshav
 */
public abstract class BaseAnalyzerConfigurationTest extends BaseSpringContextTest {

    static final int NUM_DESIGN_ELEMENTS = 100;
    static final int NUM_TWA_RESULT_SETS = 3;

    @Autowired
    protected ExpressionDataMatrixService expressionDataMatrixService = null;

    @Autowired
    protected ProcessedExpressionDataVectorService processedExpressionDataVectorService = null;

    List<BioMaterial> biomaterials = null;
    boolean connected = false;
    ExpressionExperiment expressionExperiment = null;

    ExperimentalFactor experimentalFactorA_Area = null;
    ExperimentalFactor experimentalFactorB = null;
    List<ExperimentalFactor> experimentalFactors = null;
    QuantitationType quantitationType = null;
    FactorValue factorValueA1;
    FactorValue factorValueA2;
    FactorValue factorValueB2;

    private ArrayDesign arrayDesign = null;
    private BioAssayDimension bioAssayDimension = null;
    private ExperimentalDesign experimentalDesign = null;
    @SuppressWarnings("FieldCanBeLocal")
    private BioAssay bioAssay0a = null;
    private BioAssay bioAssay0b = null;
    @SuppressWarnings("FieldCanBeLocal")
    private BioAssay bioAssay1a = null;
    private BioAssay bioAssay1b = null;
    @SuppressWarnings("FieldCanBeLocal")
    private BioAssay bioAssay2a = null;
    private BioAssay bioAssay2b = null;
    @SuppressWarnings("FieldCanBeLocal")
    private BioAssay bioAssay3a = null;
    private BioAssay bioAssay3b = null;

    private List<BioAssay> bioAssays = null;

    private RClient rc = null;

    private Collection<ProcessedExpressionDataVector> vectors = null;

    @Before
    public void setUp() throws Exception {

        try {
            if ( Settings.getBoolean( "gemma.linearmodels.useR" ) ) {
                rc = RConnectionFactory.getRConnection( Settings.getString( "gemma.rserve.hostname", "localhost" ) );

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
        arrayDesign.setPrimaryTaxon( this.getTaxon( "mouse" ) );

        expressionExperiment = ExpressionExperiment.Factory.newInstance();
        expressionExperiment.setName( "analysistest_" + RandomStringUtils.randomAlphanumeric( 12 ) );
        expressionExperiment.setId( 100009L );
        expressionExperiment.setShortName( RandomStringUtils.randomAlphanumeric( 12 ) );

        /* experimental factor "area" */
        experimentalFactorA_Area = ExperimentalFactor.Factory.newInstance();
        experimentalFactorA_Area.setName( "area" );
        experimentalFactorA_Area.setType( FactorType.CATEGORICAL );
        experimentalFactorA_Area.setId( 5001L );
        Collection<FactorValue> factorValuesA = new HashSet<>();

        factorValueA1 = FactorValue.Factory.newInstance();
        factorValueA1.setId( 1001L );
        factorValueA1.setValue( "cerebellum" );
        Statement characteristicA1 = Statement.Factory.newInstance();
        characteristicA1.setValue( factorValueA1.getValue() );
        Set<Statement> characteristicsA1 = new HashSet<>();
        characteristicsA1.add( characteristicA1 );
        factorValueA1.setCharacteristics( characteristicsA1 );
        factorValueA1.setExperimentalFactor( experimentalFactorA_Area );

        factorValueA2 = FactorValue.Factory.newInstance();
        factorValueA2.setIsBaseline( true );
        factorValueA2.setValue( "amygdala" );
        factorValueA2.setId( 1002L );
        Statement characteristicA2 = Statement.Factory.newInstance();
        characteristicA2.setValue( factorValueA2.getValue() );
        Set<Statement> characteristicsA2 = new HashSet<>();
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

        Collection<FactorValue> factorValuesB = new HashSet<>();

        FactorValue factorValueB1 = FactorValue.Factory.newInstance();
        factorValueB1.setValue( "pcp" );
        factorValueB1.setId( 1003L );
        Statement characteristicB1 = Statement.Factory.newInstance();
        characteristicB1.setValue( factorValueB1.getValue() );
        Set<Statement> characteristicsB1 = new HashSet<>();
        characteristicsB1.add( characteristicB1 );
        factorValueB1.setCharacteristics( characteristicsB1 );
        factorValueB1.setExperimentalFactor( experimentalFactorB );

        factorValueB2 = FactorValue.Factory.newInstance();
        factorValueB2.setValue( "control_group" );
        factorValueB2.setId( 1004L );
        Statement characteristicB2 = Statement.Factory.newInstance();
        characteristicB2.setValue( factorValueB2.getValue() );
        Set<Statement> characteristicsB2 = new HashSet<>();
        characteristicsB2.add( characteristicB2 );
        factorValueB2.setCharacteristics( characteristicsB2 );
        factorValueB2.setExperimentalFactor( experimentalFactorB );

        factorValuesB.add( factorValueB1 );
        factorValuesB.add( factorValueB2 );

        experimentalFactorB.getFactorValues().addAll( factorValuesB );

        /* set up the biomaterials */
        biomaterials = new ArrayList<>();

        // 2 replicates
        BioMaterial biomaterial0a = BioMaterial.Factory.newInstance();
        biomaterial0a.setName( "0a" );
        Collection<FactorValue> factorValuesForBioMaterial0 = new HashSet<>();
        factorValuesForBioMaterial0.add( factorValueA1 );
        factorValuesForBioMaterial0.add( factorValueB1 );
        biomaterial0a.getFactorValues().addAll( factorValuesForBioMaterial0 );

        BioMaterial biomaterial0b = BioMaterial.Factory.newInstance();
        biomaterial0b.setName( "0b" );
        biomaterial0b.getFactorValues().addAll( factorValuesForBioMaterial0 );

        // 2 replicates
        BioMaterial biomaterial1a = BioMaterial.Factory.newInstance();
        biomaterial1a.setName( "1a" );
        Collection<FactorValue> factorValuesForBioMaterial1 = new HashSet<>();
        factorValuesForBioMaterial1.add( factorValueA1 );
        factorValuesForBioMaterial1.add( factorValueB2 );
        biomaterial1a.getFactorValues().addAll( factorValuesForBioMaterial1 );

        BioMaterial biomaterial1b = BioMaterial.Factory.newInstance();
        biomaterial1b.setName( "1b" );
        biomaterial1b.getFactorValues().addAll( factorValuesForBioMaterial1 );

        // 2 replicates
        BioMaterial biomaterial2a = BioMaterial.Factory.newInstance();
        biomaterial2a.setName( "2a" );
        Collection<FactorValue> factorValuesForBioMaterial2 = new HashSet<>();
        factorValuesForBioMaterial2.add( factorValueA2 );
        factorValuesForBioMaterial2.add( factorValueB1 );
        biomaterial2a.getFactorValues().addAll( factorValuesForBioMaterial2 );

        BioMaterial biomaterial2b = BioMaterial.Factory.newInstance();
        biomaterial2b.setName( "2b" );
        biomaterial2b.getFactorValues().addAll( factorValuesForBioMaterial2 );

        // 2 replicates
        BioMaterial biomaterial3a = BioMaterial.Factory.newInstance();
        biomaterial3a.setName( "3a" );
        Collection<FactorValue> factorValuesForBioMaterial3 = new HashSet<>();
        factorValuesForBioMaterial3.add( factorValueA2 );
        factorValuesForBioMaterial3.add( factorValueB2 );
        biomaterial3a.getFactorValues().addAll( factorValuesForBioMaterial3 );

        BioMaterial biomaterial3b = BioMaterial.Factory.newInstance();
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
        bioAssay0a.setSampleUsed( biomaterial0a );
        bioAssay0a.setArrayDesignUsed( arrayDesign );

        bioAssay0b = BioAssay.Factory.newInstance();
        bioAssay0b.setName( "bioassay 0b" );
        bioAssay0b.setSampleUsed( biomaterial0b );

        bioAssay0b.setArrayDesignUsed( arrayDesign );

        bioAssay1a = BioAssay.Factory.newInstance();
        bioAssay1a.setName( "bioassay 1a" );
        bioAssay1a.setSampleUsed( biomaterial1a );
        bioAssay1a.setArrayDesignUsed( arrayDesign );

        bioAssay1b = BioAssay.Factory.newInstance();
        bioAssay1b.setName( "bioassay 1b" );
        bioAssay1b.setSampleUsed( biomaterial1b );
        bioAssay1b.setArrayDesignUsed( arrayDesign );

        bioAssay2a = BioAssay.Factory.newInstance();
        bioAssay2a.setName( "bioassay 2a" );
        bioAssay2a.setSampleUsed( biomaterial2a );
        bioAssay2a.setArrayDesignUsed( arrayDesign );

        bioAssay2b = BioAssay.Factory.newInstance();
        bioAssay2b.setName( "bioassay 2b" );
        bioAssay2b.setSampleUsed( biomaterial2b );
        bioAssay2b.setArrayDesignUsed( arrayDesign );

        bioAssay3a = BioAssay.Factory.newInstance();
        bioAssay3a.setName( "bioassay 3a" );
        bioAssay3a.setSampleUsed( biomaterial3a );
        bioAssay3a.setArrayDesignUsed( arrayDesign );

        bioAssay3b = BioAssay.Factory.newInstance();
        bioAssay3b.setName( "bioassay 3b" );
        bioAssay3b.setSampleUsed( biomaterial3b );
        bioAssay3b.setArrayDesignUsed( arrayDesign );

        bioAssays = new ArrayList<>();
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

        expressionExperiment.setBioAssays( new HashSet<>( bioAssays ) );
        expressionExperiment.setNumberOfSamples( new HashSet<>( bioAssays ).size() );

        experimentalFactors = new ArrayList<>();
        experimentalFactors.add( experimentalFactorA_Area );
        experimentalFactors.add( experimentalFactorB );

        experimentalDesign = ExperimentalDesign.Factory.newInstance();
        experimentalDesign.setName( "experimental design" );
        experimentalDesign.setExperimentalFactors( new HashSet<>( experimentalFactors ) );

        expressionExperiment.setExperimentalDesign( experimentalDesign );

        experimentalFactorA_Area.setExperimentalDesign( experimentalDesign );
        experimentalFactorB.setExperimentalDesign( experimentalDesign );

        quantitationType = QuantitationType.Factory.newInstance();
        quantitationType.setName( "quantitation type" );
        quantitationType.setGeneralType( GeneralType.QUANTITATIVE );
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

        this.configureVectors( biomaterials, null );
    }

    @After
    public void tearDown() {
        if ( rc != null && rc.isConnected() && rc instanceof RServeClient )
            ( ( RServeClient ) rc ).disconnect();
    }

    /**
     * Mocks the method getVectors in the {@link ExpressionDataMatrixService}.
     *
     */
    void configureMockAnalysisServiceHelper() {
        this.expressionDataMatrixService = mock( ExpressionDataMatrixService.class );
        when( expressionDataMatrixService.getProcessedExpressionDataMatrix( expressionExperiment ) )
                .thenReturn( new ExpressionDataDoubleMatrix( this.vectors ) );
    }

    /**
     * Configure the test data for one way anova.
     */
    void configureTestDataForOneWayAnova() throws Exception {

        /*
         * This really configures it for a t-test, which is just a one way anova if there are only 2 factor values
         */
        log.debug( "Configuring test data for one way anova." );

        List<BioMaterial> updatedBiomaterials = new ArrayList<>();
        for ( BioMaterial m : biomaterials ) {
            Collection<FactorValue> fvs = m.getFactorValues();
            Collection<FactorValue> updatedFactorValues = new HashSet<>();
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

        this.configureVectors( biomaterials, null );

        experimentalFactors.remove( experimentalFactorA_Area );
        experimentalDesign.setExperimentalFactors( new HashSet<>( experimentalFactors ) );
        expressionExperiment.setExperimentalDesign( experimentalDesign );
    }

    /**
     * Configure the test data for two way anova without interactions that isn't valid for a two-way. F
     * Removes the replicates.
     */
    void configureTestDataForTwoWayAnovaWithoutInteractions() throws Exception {

        log.info( "Configuring test data for two way anova without interactions." );

        /* remove the "replicates" */
        bioAssays.remove( bioAssay0b );
        bioAssays.remove( bioAssay1b );
        bioAssays.remove( bioAssay2b );
        bioAssays.remove( bioAssay3b );

        expressionExperiment.setBioAssays( new HashSet<>( bioAssays ) );
        expressionExperiment.setNumberOfSamples( new HashSet<>( bioAssays ).size() );

        bioAssayDimension.setBioAssays( bioAssays );

        biomaterials = new ArrayList<>();
        for ( BioAssay b : bioAssayDimension.getBioAssays() ) {

            biomaterials.add( b.getSampleUsed() );
        }

        this.configureVectors( biomaterials, null );

    }

    void configureVectors( List<BioMaterial> bioMaterials, String resourcePath ) throws Exception {
        this.vectors = new HashSet<>();

        DoubleMatrixReader r = new DoubleMatrixReader();

        String path;
        if ( resourcePath == null ) {
            path = "/data/stat-tests/anova-test-data.txt";
        } else {
            path = resourcePath;
        }

        DoubleMatrix<String, String> dataMatrix = r.read( this.getClass().getResourceAsStream( path ) );
        // RandomData randomData = new RandomDataImpl( new MersenneTwister( 0 ) ); // fixed seed - important!

        Set<CompositeSequence> compositeSequences = new HashSet<>();
        for ( int i = 0; i < BaseAnalyzerConfigurationTest.NUM_DESIGN_ELEMENTS; i++ ) {
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
                dvals[j] = dataMatrix.get( i, j );
            }
            vector.setDataAsDoubles( dvals );

            vectors.add( vector );

            compositeSequences.add( cs );
        }
        Set<ProcessedExpressionDataVector> vectorsSet = new HashSet<>( vectors );
        expressionExperiment.setProcessedExpressionDataVectors( vectorsSet );
        expressionExperiment.setNumberOfDataVectors( vectorsSet.size() );

        arrayDesign.setCompositeSequences( compositeSequences );
    }
}
