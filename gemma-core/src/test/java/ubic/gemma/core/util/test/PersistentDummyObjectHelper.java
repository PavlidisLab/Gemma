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
package ubic.gemma.core.util.test;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.distribution.RealDistribution;
import org.apache.commons.math3.distribution.TDistribution;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ContrastResult;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.*;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.persister.ArrayDesignsForExperimentCache;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.ExpressionAnalysisResultSetService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomSingleCellDataUtils;
import ubic.gemma.persistence.service.expression.experiment.*;

import javax.annotation.Nullable;
import java.util.*;

/**
 * Used to generate test data.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Component
@Transactional
public class PersistentDummyObjectHelper {

    private final Log log = LogFactory.getLog( this.getClass() );

    private static final int DEFAULT_TEST_ELEMENT_COLLECTION_SIZE = 6;
    private static final int NUM_BIOMATERIALS = 8;
    private static final int NUM_EXPERIMENTAL_FACTORS = 2;
    private static final int NUM_FACTOR_VALUES = 2;
    private static final int NUM_QUANTITATION_TYPES = 2;
    private static final int NUM_DESIGN_ELEMENTS = 100;
    private static final int NUM_CELL_TYPES = 8;
    private static final double PERCENT_UNASSIGNED = 0.1;
    private static final int RANDOM_STRING_LENGTH = 10;

    private ExternalDatabase genbank;
    private ExternalDatabase geo;
    private ExternalDatabase pubmed;
    private Taxon testTaxon;
    private int testElementCollectionSize = PersistentDummyObjectHelper.DEFAULT_TEST_ELEMENT_COLLECTION_SIZE;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private PersisterHelper persisterHelper;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ArrayDesignService adService;

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExpressionAnalysisResultSetService expressionAnalysisResultSetService;

    // setting seed globally does not guarantee reproducibliity always as methods could access
    // different parts of the sequence if called in different orders, so callers should reset it using resetSeed()
    private final Random randomizer = new Random( 12345 );

    /**
     * Set the seed to use to generate random data.
     */
    public void setSeed( long seed ) {
        randomizer.setSeed( seed );
        RandomSingleCellDataUtils.setSeed( seed );
    }

    /**
     * Reset the seed to the default value.
     */
    public void resetSeed() {
        setSeed( 12345 );
    }

    public BioSequence getTestNonPersistentBioSequence( Taxon taxon ) {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH )
                + "_testbiosequence" );
        bs.setSequence( RandomStringUtils.random( 40, "ATCG" ) );
        if ( taxon == null )
            bs.setTaxon( getTestNonPersistentTaxon() );
        else
            bs.setTaxon( taxon );

        if ( genbank == null ) {
            genbank = ExternalDatabase.Factory.newInstance();
            genbank.setName( "Genbank" );
        }

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();

        de.setExternalDatabase( genbank );
        de.setAccession( RandomStringUtils.randomAlphanumeric( 10 ) );

        bs.setSequenceDatabaseEntry( de );
        return bs;
    }

    public GeneProduct getTestNonPersistentGeneProduct( Gene gene ) {
        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setNcbiGi( RandomStringUtils.randomAlphanumeric( 10 ) );
        gp.setName( RandomStringUtils.randomAlphanumeric( 6 ) );
        gp.setGene( gene );
        return gp;
    }

    /**
     * Note that if you want a 'preferred' qt or other special properties you have to set it yourself.
     *
     * @return QT
     */
    public QuantitationType getTestNonPersistentQuantitationType() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testqt" );
        qt.setDescription(
                RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testqt" );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsBackground( false );
        qt.setIsBackgroundSubtracted( false );
        qt.setIsNormalized( false );
        qt.setIsPreferred( false );
        qt.setIsRatio( false );
        qt.setIsMaskedPreferred( false );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LINEAR );
        return qt;
    }

    /**
     * To allow the persister helper to manage
     *
     * @return taxon
     */
    private Taxon getTestNonPersistentTaxon() {

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "Mus musculus" );
        t.setIsGenesUsable( true );

        return t;
    }

    public Collection<Analysis> addTestAnalyses( ExpressionExperiment ee ) {
        Collection<Analysis> analyses = new ArrayList<>();
        /*
         * Add analyses
         */
        CoexpressionAnalysis pca = CoexpressionAnalysis.Factory.newInstance();
        pca.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );

        pca.setExperimentAnalyzed( ee );

        analyses.add( ( Analysis ) persisterHelper.persist( pca ) );

        /*
         * Diff
         */
        DifferentialExpressionAnalysis expressionAnalysis = DifferentialExpressionAnalysis.Factory.newInstance();
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Differential expression analysis settings" );
        protocol.setDescription( "qvalue: " + true );
        expressionAnalysis.setProtocol( protocol );
        expressionAnalysis.setExperimentAnalyzed( ee );

        analyses.add( ( Analysis ) persisterHelper.persist( expressionAnalysis ) );

        return analyses;
    }

    /**
     * @param  allFactorValues all factor values
     * @return Non-persistent ED
     */
    public ExperimentalDesign getExperimentalDesign( Collection<FactorValue> allFactorValues ) {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( "Experimental Design " + RandomStringUtils.randomNumeric( 10 ) );
        ed.setDescription( RandomStringUtils.randomNumeric( 10 ) + ": A test experimental design." );
        log.debug( "experimental design => experimental factors" );
        ed.setExperimentalFactors(
                this.getExperimentalFactors( ed, allFactorValues ) ); // set test experimental factors
        return ed;
    }

    public Collection<ExperimentalFactor> getExperimentalFactors( ExperimentalDesign ed ) {
        return this.getExperimentalFactors( ed, new HashSet<FactorValue>() );
    }

    public Collection<FactorValue> getFactorValues( ExperimentalFactor ef ) {
        return this.getFactorValues( ef, new HashSet<FactorValue>() );
    }

    public int getTestElementCollectionSize() {
        return testElementCollectionSize;
    }

    /**
     * Override the number of elements made in collections. By default this is quite small, so you can increase it. But
     * please call 'reset' afterwards.
     *
     * @param testElementCollectionSize new size
     */
    public void setTestElementCollectionSize( int testElementCollectionSize ) {
        this.testElementCollectionSize = testElementCollectionSize;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations.
     *
     * @return EE
     */
    public ExpressionExperiment getTestExpressionExperimentWithAllDependencies() {
        return this.getTestExpressionExperimentWithAllDependencies( true );
    }

    public ExpressionExperiment getTestExpressionExperimentWithAllDependencies( ExpressionExperiment prototype ) {

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setName( "Expression Experiment " + RandomStringUtils
                .randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setDescription( "A test expression experiment" );
        ee.setSource( "https://www.ncbi.nlm.nih.gov/geo/" );
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( geo );
        ee.setAccession( de1 );
        Collection<FactorValue> allFactorValues = new HashSet<>();

        ExperimentalDesign ed = this.getExperimentalDesign( allFactorValues );
        Collection<BioMaterial> bioMaterials = this.getBioMaterials( allFactorValues );

        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );
        Collection<ArrayDesign> arrayDesignsUsed = eeService.getArrayDesignsUsed( prototype );
        Set<BioAssay> bioAssays = new HashSet<>();

        Set<QuantitationType> quantitationTypes = this.getRawQuantitationTypes();

        prototype = eeService.thaw( prototype );
        Set<RawExpressionDataVector> vectors = new HashSet<>();
        arrayDesignsUsed = adService.thaw( arrayDesignsUsed );
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            List<BioAssay> bas = this.getBioAssays( bioMaterials, ad );
            bioAssays.addAll( bas );
            vectors.addAll( this.getDesignElementDataVectors( ee, quantitationTypes, bas, ad ) );
        }

        ee.setBioAssays( bioAssays );
        ee.setNumberOfSamples( bioAssays.size() );
        ee.setTaxon( bioAssays.iterator().next().getSampleUsed().getSourceTaxon() );

        assert quantitationTypes.size() > 0;

        ee.setQuantitationTypes( quantitationTypes );

        ee.setRawExpressionDataVectors( vectors );

        ArrayDesignsForExperimentCache c = persisterHelper.prepare( ee );
        ee = persisterHelper.persist( ee, c );

        return ee;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations
     *
     * @param  doSequence Should the array design get all the sequence information filled in? (true = slower)
     * @return EE
     */
    public ExpressionExperiment getTestExpressionExperimentWithAllDependencies( boolean doSequence ) {

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( RandomStringUtils.randomAlphanumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setName( "Expression Experiment " + RandomStringUtils
                .randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setDescription( "A test expression experiment" );
        ee.setSource( "https://www.ncbi.nlm.nih.gov/geo/" );
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( geo );
        ee.setAccession( de1 );

        ArrayDesign adA = this.getTestPersistentArrayDesign( this.getTestElementCollectionSize(), true, doSequence );
        ArrayDesign adB = this.getTestPersistentArrayDesign( this.getTestElementCollectionSize(), true, doSequence );
        Collection<FactorValue> allFactorValues = new HashSet<>();

        ExperimentalDesign ed = this.getExperimentalDesign( allFactorValues );
        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );

        Set<BioAssay> bioAssays = new HashSet<>();
        Collection<BioMaterial> bioMaterials = this.getBioMaterials( allFactorValues );
        List<BioAssay> bioAssaysA = this.getBioAssays( bioMaterials, adA );
        List<BioAssay> bioAssaysB = this.getBioAssays( bioMaterials, adB );
        bioAssays.addAll( bioAssaysA );
        bioAssays.addAll( bioAssaysB );
        ee.setBioAssays( bioAssays );
        ee.setNumberOfSamples( bioAssays.size() );
        ee.setTaxon( bioAssays.iterator().next().getSampleUsed().getSourceTaxon() );

        log.debug( "expression experiment => design element data vectors" );
        Set<RawExpressionDataVector> vectors = new HashSet<>();

        Set<QuantitationType> quantitationTypes = this.getRawQuantitationTypes();

        vectors.addAll( this.getDesignElementDataVectors( ee, quantitationTypes, bioAssaysA, adA ) );
        vectors.addAll( this.getDesignElementDataVectors( ee, quantitationTypes, bioAssaysB, adB ) );

        ee.setQuantitationTypes( quantitationTypes );

        ee.setRawExpressionDataVectors( vectors );

        ArrayDesignsForExperimentCache c = persisterHelper.prepare( ee );
        ee = persisterHelper.persist( ee, c );

        return ee;
    }

    public ExpressionExperiment getTestPersistentSingleCellExpressionExperiment() {
        ArrayDesign ad = getTestPersistentArrayDesign( NUM_DESIGN_ELEMENTS, true, false );
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment( ad, false );
        QuantitationType scQt = QuantitationType.Factory.newInstance();
        scQt.setName( "counts" );
        scQt.setGeneralType( GeneralType.QUANTITATIVE );
        scQt.setType( StandardQuantitationType.COUNT );
        scQt.setScale( ScaleType.COUNT );
        scQt.setRepresentation( PrimitiveType.DOUBLE );
        scQt.setIsSingleCellPreferred( true );
        List<SingleCellExpressionDataVector> vectors = RandomSingleCellDataUtils.randomSingleCellVectors( ee, ad, scQt );
        singleCellExpressionExperimentService.addSingleCellDataVectors( ee, scQt, vectors, null );

        SingleCellDimension dimension = vectors.iterator().next().getSingleCellDimension();

        CellTypeAssignment cta = new CellTypeAssignment();
        for ( int i = 0; i < NUM_CELL_TYPES; i++ ) {
            cta.getCellTypes().add( Characteristic.Factory.newInstance( Categories.CELL_TYPE, "ct" + i, null ) );
        }
        int[] indices = new int[dimension.getNumberOfCells()];
        int assigned = 0;
        for ( int i = 0; i < dimension.getNumberOfCells(); i++ ) {
            if ( randomizer.nextDouble() < PERCENT_UNASSIGNED ) {
                indices[i] = CellTypeAssignment.UNKNOWN_CELL_TYPE;
            } else {
                indices[i] = randomizer.nextInt( NUM_CELL_TYPES );
                assigned++;
            }
        }
        cta.setCellTypeIndices( indices );
        cta.setNumberOfCellTypes( NUM_CELL_TYPES );
        cta.setNumberOfAssignedCells( assigned );
        cta.setPreferred( true );
        singleCellExpressionExperimentService.addCellTypeAssignment( ee, scQt, dimension, cta );

        return ee;
    }

    @Autowired
    private ExperimentalFactorService experimentalFactorService;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    /**
     * Create a test EE with analysis results.
     *
     * The design is simple: two conditions: healthy and sick with 3 replicates
     *
     * Random statistics will be generated for all the probes defined in the provided {@link ArrayDesign}.
     *
     */
    public ExpressionExperiment getTestExpressionExperimentWithAnalysisAndResults() {
        ArrayDesign ad = getTestPersistentArrayDesign( 10, true, false );
        ExpressionExperiment ee = getTestPersistentBasicExpressionExperiment( ad );

        /* no experimental design, but we just create a single factor model */
        ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
        ef.setType( FactorType.CATEGORICAL );
        ef.setExperimentalDesign( ee.getExperimentalDesign() );

        FactorValue baseline = FactorValue.Factory.newInstance( ef );
        baseline.setIsBaseline( true );
        baseline.setValue( "healthy" );
        ef.getFactorValues().add( baseline );
        FactorValue condition = FactorValue.Factory.newInstance( ef );
        condition.setValue( "sick" );
        ef.getFactorValues().add( condition );

        ef = experimentalFactorService.create( ef );

        DifferentialExpressionAnalysis dea = new DifferentialExpressionAnalysis();
        dea.setExperimentAnalyzed( ee );

        ExpressionAnalysisResultSet dears = new ExpressionAnalysisResultSet();
        dears.setAnalysis( dea );
        dears.setExperimentalFactors( Collections.singleton( ef ) );

        // draw log2fc from an over-dispersed normal distribution
        RealDistribution log2fcDistribution = new NormalDistribution( 0, 2 );
        // assuming 3 replicates (thus 2 degree of freedom)
        TDistribution ttestDistribution = new TDistribution( 3 - 1 );
        Set<DifferentialExpressionAnalysisResult> results = new HashSet<>();
        for ( CompositeSequence probe : ad.getCompositeSequences() ) {
            double log2fc = log2fcDistribution.sample();
            double tstat = ( log2fc - 0.0 ) / 2.0; // simple Z-transform
            double pvalue = 2.0 * ( 1.0 - ttestDistribution.cumulativeProbability( tstat ) );

            ContrastResult cr = new ContrastResult();
            cr.setCoefficient( 1.0 );
            cr.setLogFoldChange( log2fc );
            cr.setTstat( tstat );
            cr.setPvalue( pvalue );
            cr.setFactorValue( condition );

            DifferentialExpressionAnalysisResult dear = DifferentialExpressionAnalysisResult.Factory.newInstance();
            dear.setResultSet( dears );
            dear.setPvalue( pvalue );
            dear.setCorrectedPvalue( dear.getPvalue() / ad.getCompositeSequences().size() );
            dear.setProbe( probe );
            dear.setContrasts( Collections.singleton( cr ) );

            results.add( dear );
        }

        dears.setResults( results );

        dea.setResultSets( Collections.singleton( dears ) );

        // create everything at once
        differentialExpressionAnalysisService.create( dea );

        return ee;
    }

    /**
     * @return with default taxon
     */
    public Gene getTestPersistentGene() {
        return this.getTestPersistentGene( null );
    }

    public Gene getTestPersistentGene( Taxon t ) {
        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_test" );
        gene.setOfficialName(
                RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_test" );
        gene.setOfficialSymbol(
                RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ).toUpperCase() );

        if ( t == null ) {
            gene.setTaxon( getTestNonPersistentTaxon() );
        } else {
            gene.setTaxon( t );
        }

        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setGene( gene );
        gp.setName( RandomStringUtils.randomNumeric( 5 ) + "_test" );
        gene.getProducts().add( gp );
        return ( Gene ) persisterHelper.persist( gene );
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     * The ArrayDesign is provided with some CompositeSequence DesignElements if desired. If composite sequences are
     * created, they are each associated with a single generated Reporter.
     *
     * @param  numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param  randomNames           If true, probe names will be random strings; otherwise they will be
     *                               0_probe_at....N_probe_at
     * @param  doSequence            If true, biosequences and biosequence2GeneProduct associations are filled in
     *                               (slower).
     * @return ArrayDesign
     */
    public ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames,
            boolean doSequence ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();

        ad.setName( "arrayDesign_" + RandomStringUtils
                .randomAlphabetic( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ad.setShortName( "AD_" + RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setTechnologyType( TechnologyType.ONECOLOR );

        ad.setPrimaryTaxon( this.getTestPersistentTaxon() );

        for ( int i = 0; i < numCompositeSequences; i++ ) {

            // Reporter reporter = Reporter.Factory.newInstance();
            CompositeSequence compositeSequence = CompositeSequence.Factory.newInstance();

            if ( randomNames ) {
                compositeSequence.setName(
                        RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH )
                                + "_testcs" );
            } else {
                compositeSequence.setName( "probeset_" + i );
            }

            // compositeSequence.getComponentReporters().add( reporter );
            compositeSequence.setArrayDesign( ad );
            ad.getCompositeSequences().add( compositeSequence );

            if ( doSequence ) {
                BioSequence bioSequence = this.getTestPersistentBioSequence();
                compositeSequence.setBiologicalCharacteristic( bioSequence );
                bioSequence.setBioSequence2GeneProduct( this.getTestPersistentBioSequence2GeneProducts( bioSequence ) );
            }
        }

        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            cs.setArrayDesign( ad );
        }
        assert ( ad.getCompositeSequences().size() == numCompositeSequences );

        return ( ArrayDesign ) persisterHelper.persist( ad );
    }

    public ExpressionExperiment getTestPersistentBasicExpressionExperiment() {
        return this.getTestPersistentBasicExpressionExperiment( null );
    }

    public ExpressionExperiment getTestPersistentBasicExpressionExperiment( ArrayDesign arrayDesign ) {
        return getTestPersistentBasicExpressionExperiment( arrayDesign, true );
    }

    /**
     * @param arrayDesign   platform to use, if null two random platforms are generated
     * @param includeRawQts include two raw QTs (one being preferred)
     * @return A lighter-weight EE, with no data, and the ADs have no sequences.
     */
    public ExpressionExperiment getTestPersistentBasicExpressionExperiment( @Nullable ArrayDesign arrayDesign, boolean includeRawQts ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setName( "Expression Experiment " + RandomStringUtils
                .randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setDescription( "A test expression experiment" );
        ee.setSource( "https://www.ncbi.nlm.nih.gov/geo/" );
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( geo );
        ee.setAccession( de1 );

        Collection<FactorValue> allFactorValues = new HashSet<>();
        ExperimentalDesign ed = this.getExperimentalDesign( allFactorValues );
        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );

        Collection<BioAssay> bioAssays = new HashSet<>();
        Collection<BioMaterial> bioMaterials = this.getBioMaterials( allFactorValues );

        if ( arrayDesign != null ) {
            bioAssays = this.getBioAssays( bioMaterials, arrayDesign );
        } else {
            ArrayDesign adA = this.getTestPersistentArrayDesign( 0, true, false );
            ArrayDesign adB = this.getTestPersistentArrayDesign( 0, true, false );

            Collection<BioAssay> bioAssaysA = this.getBioAssays( bioMaterials, adA );
            Collection<BioAssay> bioAssaysB = this.getBioAssays( bioMaterials, adB );
            bioAssays.addAll( bioAssaysA );
            bioAssays.addAll( bioAssaysB );
        }
        ee.getBioAssays().addAll( bioAssays );
        ee.setTaxon( bioAssays.iterator().next().getSampleUsed().getSourceTaxon() );

        if ( includeRawQts ) {
            ee.setQuantitationTypes( this.getRawQuantitationTypes() );
        }

        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );

        return ee;
    }

    public BibliographicReference getTestPersistentBibliographicReference( String accession ) {
        BibliographicReference br = BibliographicReference.Factory.newInstance();
        if ( pubmed == null ) {
            pubmed = externalDatabaseService.findByName( "PubMed" );
        }
        br.setPubAccession( this.getTestPersistentDatabaseEntry( accession, pubmed ) );
        return ( BibliographicReference ) persisterHelper.persist( br );
    }

    public BioAssay getTestPersistentBioAssay( ArrayDesign ad ) {
        BioMaterial bm = this.getTestPersistentBioMaterial();
        return this.getTestPersistentBioAssay( ad, bm );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     *
     * @param  ad AD
     * @param  bm BM
     * @return BA
     */
    public BioAssay getTestPersistentBioAssay( ArrayDesign ad, BioMaterial bm ) {
        if ( ad == null || bm == null ) {
            throw new IllegalArgumentException();
        }
        BioAssay ba = this.getTestNonPersistentBioAssay( ad, bm );
        return ( BioAssay ) persisterHelper.persist( ba );
    }

    public BioMaterial getTestPersistentBioMaterial() {
        BioMaterial bm = this.getTestNonPersistentBioMaterial();
        return ( BioMaterial ) persisterHelper.persist( bm );
    }

    public BioMaterial getTestPersistentBioMaterial( Taxon tax ) {
        BioMaterial bm = this.getTestNonPersistentBioMaterial( tax );
        return ( BioMaterial ) persisterHelper.persist( bm );
    }

    public BioSequence getTestPersistentBioSequence() {
        BioSequence bs = getTestNonPersistentBioSequence( null );

        return ( BioSequence ) persisterHelper.persist( bs );
    }

    public BioSequence getTestPersistentBioSequence( Taxon taxon ) {
        BioSequence bs = getTestNonPersistentBioSequence( taxon );

        return ( BioSequence ) persisterHelper.persist( bs );
    }

    /**
     * @param  bioSequence bio sequence
     * @return bio sequence to gene products
     */
    @SuppressWarnings("unchecked")
    public Set<BioSequence2GeneProduct> getTestPersistentBioSequence2GeneProducts( BioSequence bioSequence ) {

        Collection<BioSequence2GeneProduct> b2gCol = new HashSet<>();

        BlatAssociation b2g = BlatAssociation.Factory.newInstance();
        b2g.setScore( this.randomizer.nextDouble() );
        b2g.setBioSequence( bioSequence );
        b2g.setGeneProduct( this.getTestPersistentGeneProduct( this.getTestPersistentGene() ) );
        b2g.setBlatResult( this.getTestPersistentBlatResult( bioSequence, null ) );
        b2gCol.add( b2g );

        //noinspection unchecked
        return new HashSet<>( ( List<BioSequence2GeneProduct> ) persisterHelper.persist( b2gCol ) );
    }

    public BlatResult getTestPersistentBlatResult( BioSequence querySequence, Taxon taxon ) {
        BlatResult br = BlatResult.Factory.newInstance();

        if ( taxon == null ) {
            taxon = this.getTestPersistentTaxon();
        }
        Chromosome chromosome = new Chromosome( "XXX", null, this.getTestPersistentBioSequence( taxon ), taxon );
        assert chromosome.getSequence() != null;
        chromosome = ( Chromosome ) persisterHelper.persist( chromosome );
        assert chromosome != null;
        assert chromosome.getSequence() != null;
        br.setTargetChromosome( chromosome );
        assert br.getTargetChromosome().getSequence() != null;
        br.setQuerySequence( querySequence );
        br.setTargetStart( 1L );
        br.setTargetEnd( 1000L );
        PhysicalLocation targetAlignedRegion = PhysicalLocation.Factory.newInstance();
        targetAlignedRegion.setChromosome( br.getTargetChromosome() );
        targetAlignedRegion.setNucleotide( 10000010L );
        targetAlignedRegion.setNucleotideLength( 1001 );
        targetAlignedRegion.setStrand( "-" );
        return ( BlatResult ) persisterHelper.persist( br );
    }

    /**
     * Convenience method to provide a Contact that can be used to fill non-nullable associations in test objects.
     *
     * @return contact
     */
    public Contact getTestPersistentContact() {
        Contact c = Contact.Factory.newInstance();
        c.setName(
                RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testcontact" );
        c.setEmail( c.getName() + "@foo.org" );
        c = ( Contact ) persisterHelper.persist( c );
        return c;
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession is set to a random string
     *
     * @param  ed ED
     * @return db entry
     */
    public DatabaseEntry getTestPersistentDatabaseEntry( ExternalDatabase ed ) {
        return this.getTestPersistentDatabaseEntry(
                RandomStringUtils.randomAlphabetic( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ), ed );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     *
     * @param  ed        ED
     * @param  accession accession
     * @return db entry
     */
    public DatabaseEntry getTestPersistentDatabaseEntry( String accession, ExternalDatabase ed ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        if ( accession == null ) {
            result.setAccession( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH )
                    + "_testaccession" );
        } else {
            result.setAccession( accession );
        }

        if ( ed == null ) {
            ed = ExternalDatabase.Factory.newInstance();
            ed.setName(
                    RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testdb" );
            ed = ( ExternalDatabase ) persisterHelper.persist( ed );
        }

        result.setExternalDatabase( ed );
        return result;
    }

    /**
     * @param  databaseName GEO or PubMed (others could be supported)
     * @param  accession    accession
     * @return db entry
     */
    public DatabaseEntry getTestPersistentDatabaseEntry( String accession, String databaseName ) {
        switch ( databaseName ) {
            case "GEO":
                return this.getTestPersistentDatabaseEntry( accession, geo );
            case "PubMed":
                return this.getTestPersistentDatabaseEntry( accession, pubmed );
            default:
                ExternalDatabase edp = ExternalDatabase.Factory.newInstance();
                edp.setName( databaseName );
                edp = ( ExternalDatabase ) persisterHelper.persist( edp );
                return this.getTestPersistentDatabaseEntry( accession, edp );
        }
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object.
     *
     * @return EE
     */
    public ExpressionExperiment getTestPersistentExpressionExperiment() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        String shortName = RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testee";
        ee.setShortName( shortName );
        ee.setName( shortName );
        ee.setTaxon( this.getTestPersistentTaxon() );
        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );
        return ee;
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object except for the creation of
     * persistent BioMaterials and BioAssays so that database taxon lookups for this experiment will work.
     *
     * @param  taxon the experiment will have this taxon
     * @return EE
     */
    public ExpressionExperiment getTestPersistentExpressionExperiment( Taxon taxon ) {
        BioAssay ba;
        BioMaterial bm;
        ArrayDesign ad;

        bm = this.getTestPersistentBioMaterial( taxon );
        ad = this.getTestPersistentArrayDesign( 4, true, true );
        ba = this.getTestPersistentBioAssay( ad, bm );
        Set<BioAssay> bas1 = new HashSet<>();
        bas1.add( ba );

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testee" );
        ee.setShortName(
                RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testee" );
        ee.setBioAssays( bas1 );
        ee.setNumberOfSamples( bas1.size() );

        Collection<FactorValue> allFactorValues = new HashSet<>();

        ExperimentalDesign ed = this.getExperimentalDesign( allFactorValues );
        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );

        log.debug( "expression experiment => design element data vectors" );
        Set<RawExpressionDataVector> vectors = new HashSet<>();

        Set<QuantitationType> quantitationTypes = this.getRawQuantitationTypes();

        assert quantitationTypes.size() > 0;

        ee.setQuantitationTypes( quantitationTypes );
        ee.setTaxon( taxon );
        ee.setRawExpressionDataVectors( vectors );

        ArrayDesignsForExperimentCache c = persisterHelper.prepare( ee );
        return persisterHelper.persist( ee, c );
    }

    public GeneProduct getTestPersistentGeneProduct( Gene gene ) {
        GeneProduct gp = getTestNonPersistentGeneProduct( gene );
        return ( GeneProduct ) persisterHelper.persist( gp );
    }

    /**
     * Convenience method to provide a QuantitationType that can be used to fill non-nullable associations in test
     * objects.
     *
     * @return QT
     */
    public QuantitationType getTestPersistentQuantitationType() {
        QuantitationType qt = getTestNonPersistentQuantitationType();
        return ( QuantitationType ) persisterHelper.persist( qt );
    }

    public Taxon getTestPersistentTaxon() {
        if ( testTaxon == null ) {
            testTaxon = Taxon.Factory.newInstance();
            testTaxon.setCommonName( "elephant" );
            testTaxon.setScientificName( "Loxodonta" );
            testTaxon.setNcbiId( 1245 );
            testTaxon.setIsGenesUsable( true );
            testTaxon = ( Taxon ) persisterHelper
                    .persist( testTaxon );
            assert testTaxon != null
                    && testTaxon.getId() != null;
        }
        return testTaxon;
    }

    public void resetTestElementCollectionSize() {
        this.testElementCollectionSize = PersistentDummyObjectHelper.DEFAULT_TEST_ELEMENT_COLLECTION_SIZE;
    }

    private Set<ExperimentalFactor> getExperimentalFactors( ExperimentalDesign ed,
            Collection<FactorValue> allFactorValues ) {
        Set<ExperimentalFactor> efCol = new HashSet<>();
        for ( int i = 0; i < PersistentDummyObjectHelper.NUM_EXPERIMENTAL_FACTORS; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setExperimentalDesign( ed );
            ef.setName( "Experimental Factor " + RandomStringUtils
                    .randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
            ef.setDescription( i + ": A test experimental factor" );
            ef.setType( FactorType.CATEGORICAL );
            Characteristic c = Characteristic.Factory.newInstance();
            c.setCategory( "OrganismPart" );
            c.setName( "OrganismPart" );
            ef.setCategory( c );
            log.debug( "experimental factor => factor values" );
            ef.setFactorValues( this.getFactorValues( ef, allFactorValues ) );
            efCol.add( ef );
        }
        return efCol;
    }

    protected Set<FactorValue> getFactorValues( ExperimentalFactor ef,
            Collection<FactorValue> allFactorValues ) {

        Set<FactorValue> fvCol = new HashSet<>();
        for ( int i = 0; i < PersistentDummyObjectHelper.NUM_FACTOR_VALUES; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance();
            fv.setValue( "Factor value " + RandomStringUtils
                    .randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
            fv.setExperimentalFactor( ef );
            fv.setCharacteristics( Collections.singleton( getTestStatement( "name" + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ), fv.getValue() ) ) );
            fvCol.add( fv );
        }

        allFactorValues.addAll( fvCol );

        return fvCol;
    }

    protected Characteristic getTestCharacteristic( String name, String value ) {
        return Characteristic.Factory.newInstance( name, null, value, null, null, null, null );
    }

    protected Statement getTestStatement( String name, String value ) {
        Statement s = Statement.Factory.newInstance();
        s.setName( name );
        s.setValue( value );
        return s;
    }

    /**
     * Create raw quantitation types.
     * <p>
     * One of these will be marked as preferred.
     */
    private Set<QuantitationType> getRawQuantitationTypes() {
        Set<QuantitationType> quantitationTypes = new HashSet<>();
        for ( int quantitationTypeNum = 0; quantitationTypeNum < PersistentDummyObjectHelper.NUM_QUANTITATION_TYPES; quantitationTypeNum++ ) {
            QuantitationType q = getTestNonPersistentQuantitationType();
            if ( quantitationTypeNum == 0 ) {
                q.setIsPreferred( true );
            }
            quantitationTypes.add( q );
        }
        return quantitationTypes;
    }

    private List<BioAssay> getBioAssays( Collection<BioMaterial> bioMaterials, ArrayDesign ad ) {
        List<BioAssay> baCol = new ArrayList<>();
        for ( BioMaterial bm : bioMaterials ) {
            BioAssay ba = this.getTestNonPersistentBioAssay( ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            baCol.add( ba );
        }
        return baCol;
    }

    private Collection<BioMaterial> getBioMaterials( Collection<FactorValue> allFactorValues ) {

        if ( allFactorValues.isEmpty() )
            throw new RuntimeException(
                    "Factor values have not been associated with biomaterials.  Try creating the experimental design first." );

        Iterator<FactorValue> iter = allFactorValues.iterator();

        Collection<BioMaterial> baCol = new HashSet<>();
        // one biomaterial for each set of bioassays
        for ( int j = 0; j < PersistentDummyObjectHelper.NUM_BIOMATERIALS; j++ ) {
            BioMaterial bm = this.getTestNonPersistentBioMaterial();
            Set<FactorValue> fvCol = new HashSet<>();
            if ( iter.hasNext() ) {
                fvCol.add( iter.next() );
            } else {
                // start over.
                iter = allFactorValues.iterator();
                fvCol.add( iter.next() );
            }

            Characteristic c = Characteristic.Factory.newInstance();
            c.setValue( "test_char_" + RandomStringUtils.randomAlphanumeric( 10 ) );

            bm.getCharacteristics().add( c );

            bm.setFactorValues( fvCol );
            baCol.add( bm );
        }
        return baCol;
    }

    /**
     * @param  bioAssays         BAs
     * @param  ad                AD
     * @param  ee                EE
     * @param  quantitationTypes QTs
     * @return These are non-persistent
     */
    private Collection<RawExpressionDataVector> getDesignElementDataVectors( ExpressionExperiment ee,
            Collection<QuantitationType> quantitationTypes, List<BioAssay> bioAssays, ArrayDesign ad ) {

        BioAssayDimension baDim = BioAssayDimension.Factory.newInstance(  bioAssays );

        Collection<RawExpressionDataVector> vectors = new HashSet<>();
        for ( QuantitationType quantType : quantitationTypes ) {
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                assert cs.getArrayDesign() != null;
                RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
                vector.setDesignElement( cs );
                vector.setExpressionExperiment( ee );
                vector.setQuantitationType( quantType );
                vector.setBioAssayDimension( baDim );
                vector.setDataAsDoubles( this.getDoubleData() );
                vectors.add( vector );
            }
        }
        return vectors;
    }

    private double[] getDoubleData() {
        double[] data = new double[PersistentDummyObjectHelper.NUM_BIOMATERIALS];
        double bump = 0.0;
        for ( int j = 0; j < data.length; j++ ) {
            data[j] = this.randomizer.nextDouble() + bump;
            if ( j % 3 == 0 ) {
                // add some correlation structure to the data.
                bump += 0.3; // adjusted to possibly avoid outlier samples from being generated by accident.
            }
        }
        return data;
    }

    private BioAssay getTestNonPersistentBioAssay( ArrayDesign ad, BioMaterial bm ) {
        BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
        ba.setName(
                RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testbioassay" );
        ba.setSampleUsed( bm );
        ba.setArrayDesignUsed( ad );
        ba.setIsOutlier( false );
        ba.setSequencePairedReads( false );
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();

        de.setExternalDatabase( geo );
        de.setAccession( ba.getName() );
        ba.setAccession( de );

        return ba;
    }

    /**
     * @return Slightly misleading, associations are persistent.
     */
    private BioMaterial getTestNonPersistentBioMaterial() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH )
                + "_testbiomaterial" );
        if ( geo == null ) {
            geo = externalDatabaseService.findByName( "GEO" );
            assert geo != null;
        }
        bm.setSourceTaxon( getTestNonPersistentTaxon() );
        bm.setExternalAccession( this.getTestPersistentDatabaseEntry( geo ) );
        return bm;
    }

    /**
     * @return Slightly misleading, associations are persistent.
     */
    private BioMaterial getTestNonPersistentBioMaterial( Taxon tax ) {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH )
                + "_testbiomaterial" );
        if ( geo == null ) {
            geo = externalDatabaseService.findByName( "GEO" );
            assert geo != null;
        }
        bm.setSourceTaxon( tax );
        bm.setExternalAccession( this.getTestPersistentDatabaseEntry( geo ) );
        return bm;
    }
}
