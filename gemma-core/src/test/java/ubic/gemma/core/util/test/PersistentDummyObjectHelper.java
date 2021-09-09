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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.*;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
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
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.ArrayDesignsForExperimentCache;

import java.util.*;

/**
 * Used to generate test data.
 *
 * @author pavlidis
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
@Component
public class PersistentDummyObjectHelper {

    private static final int DEFAULT_TEST_ELEMENT_COLLECTION_SIZE = 6;
    private static final int NUM_BIOMATERIALS = 8;
    private static final int NUM_EXPERIMENTAL_FACTORS = 2;
    private static final int NUM_FACTOR_VALUES = 2;
    private static final int NUM_QUANTITATION_TYPES = 2;
    private static final int RANDOM_STRING_LENGTH = 10;
    private static ExternalDatabase genbank;
    private static ExternalDatabase geo;
    private static ExternalDatabase pubmed;
    private static Taxon testTaxon;
    private final Log log = LogFactory.getLog( this.getClass() );
    private int testElementCollectionSize = PersistentDummyObjectHelper.DEFAULT_TEST_ELEMENT_COLLECTION_SIZE;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private Persister persisterHelper;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private ArrayDesignService adService;

    public static BioSequence getTestNonPersistentBioSequence( Taxon taxon ) {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH )
                + "_testbiosequence" );
        bs.setSequence( RandomStringUtils.random( 40, "ATCG" ) );
        if ( taxon == null )
            bs.setTaxon( PersistentDummyObjectHelper.getTestNonPersistentTaxon() );
        else
            bs.setTaxon( taxon );

        if ( PersistentDummyObjectHelper.genbank == null ) {
            PersistentDummyObjectHelper.genbank = ExternalDatabase.Factory.newInstance();
            PersistentDummyObjectHelper.genbank.setName( "Genbank" );
        }

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();

        de.setExternalDatabase( PersistentDummyObjectHelper.genbank );
        de.setAccession( RandomStringUtils.randomAlphanumeric( 10 ) );

        bs.setSequenceDatabaseEntry( de );
        return bs;
    }

    public static GeneProduct getTestNonPersistentGeneProduct( Gene gene ) {
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
    public static QuantitationType getTestNonPersistentQuantitationType() {
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
    private static Taxon getTestNonPersistentTaxon() {

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
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( PersistentDummyObjectHelper.geo );
        ee.setAccession( de1 );
        Collection<FactorValue> allFactorValues = new HashSet<>();

        ExperimentalDesign ed = this.getExperimentalDesign( allFactorValues );
        Collection<BioMaterial> bioMaterials = this.getBioMaterials( allFactorValues );

        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );
        List<ArrayDesign> arrayDesignsUsed = new ArrayList<>( eeService.getArrayDesignsUsed( prototype ) );
        Collection<BioAssay> bioAssays = new HashSet<>();

        Collection<QuantitationType> quantitationTypes = this.addQuantitationTypes( new HashSet<QuantitationType>() );

        eeService.thaw( prototype );
        Collection<RawExpressionDataVector> vectors = new HashSet<>();
        for ( ArrayDesign ad : arrayDesignsUsed ) {
            List<BioAssay> bas = this.getBioAssays( bioMaterials, ad );
            bioAssays.addAll( bas );
            ad = this.adService.thaw( ad );
            vectors.addAll( this.getDesignElementDataVectors( ee, quantitationTypes, bas, ad ) );
        }

        ee.setBioAssays( bioAssays );
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
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( PersistentDummyObjectHelper.geo );
        ee.setAccession( de1 );

        ArrayDesign adA = this.getTestPersistentArrayDesign( this.getTestElementCollectionSize(), true, doSequence );
        ArrayDesign adB = this.getTestPersistentArrayDesign( this.getTestElementCollectionSize(), true, doSequence );
        Collection<FactorValue> allFactorValues = new HashSet<>();

        ExperimentalDesign ed = this.getExperimentalDesign( allFactorValues );
        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );

        Collection<BioAssay> bioAssays = new HashSet<>();
        Collection<BioMaterial> bioMaterials = this.getBioMaterials( allFactorValues );
        List<BioAssay> bioAssaysA = this.getBioAssays( bioMaterials, adA );
        List<BioAssay> bioAssaysB = this.getBioAssays( bioMaterials, adB );
        bioAssays.addAll( bioAssaysA );
        bioAssays.addAll( bioAssaysB );
        ee.setBioAssays( bioAssays );
        ee.setTaxon( bioAssays.iterator().next().getSampleUsed().getSourceTaxon() );

        log.debug( "expression experiment => design element data vectors" );
        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        Collection<QuantitationType> quantitationTypes = this.addQuantitationTypes( new HashSet<QuantitationType>() );

        assert quantitationTypes.size() > 0;

        vectors.addAll( this.getDesignElementDataVectors( ee, quantitationTypes, bioAssaysA, adA ) );
        vectors.addAll( this.getDesignElementDataVectors( ee, quantitationTypes, bioAssaysB, adB ) );

        ee.setQuantitationTypes( quantitationTypes );

        ee.setRawExpressionDataVectors( vectors );

        ArrayDesignsForExperimentCache c = persisterHelper.prepare( ee );
        ee = persisterHelper.persist( ee, c );

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
            gene.setTaxon( PersistentDummyObjectHelper.getTestNonPersistentTaxon() );
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

    /**
     * @param  arrayDesign AD
     * @return A lighter-weight EE, with no data, and the ADs have no sequences.
     */
    public ExpressionExperiment getTestPersistentBasicExpressionExperiment( ArrayDesign arrayDesign ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setName( "Expression Experiment " + RandomStringUtils
                .randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
        ee.setDescription( "A test expression experiment" );
        ee.setSource( "https://www.ncbi.nlm.nih.gov/geo/" );
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( PersistentDummyObjectHelper.geo );
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

        Collection<QuantitationType> quantitationTypes = this.addQuantitationTypes( new HashSet<QuantitationType>() );

        assert quantitationTypes.size() > 0;
        ee.setQuantitationTypes( quantitationTypes );
        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );

        return ee;
    }

    public BibliographicReference getTestPersistentBibliographicReference( String accession ) {
        BibliographicReference br = BibliographicReference.Factory.newInstance();
        if ( PersistentDummyObjectHelper.pubmed == null ) {
            PersistentDummyObjectHelper.pubmed = externalDatabaseService.findByName( "PubMed" );
        }
        br.setPubAccession( this.getTestPersistentDatabaseEntry( accession, PersistentDummyObjectHelper.pubmed ) );
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
        BioSequence bs = PersistentDummyObjectHelper.getTestNonPersistentBioSequence( null );

        return ( BioSequence ) persisterHelper.persist( bs );
    }

    public BioSequence getTestPersistentBioSequence( Taxon taxon ) {
        BioSequence bs = PersistentDummyObjectHelper.getTestNonPersistentBioSequence( taxon );

        return ( BioSequence ) persisterHelper.persist( bs );
    }

    /**
     * @param  bioSequence bio sequence
     * @return bio sequence to gene products
     */
    @SuppressWarnings("unchecked")
    public Collection<BioSequence2GeneProduct> getTestPersistentBioSequence2GeneProducts( BioSequence bioSequence ) {

        Collection<BioSequence2GeneProduct> b2gCol = new HashSet<>();

        BlatAssociation b2g = BlatAssociation.Factory.newInstance();
        b2g.setScore( new Random().nextDouble() );
        b2g.setBioSequence( bioSequence );
        b2g.setGeneProduct( this.getTestPersistentGeneProduct( this.getTestPersistentGene() ) );
        b2g.setBlatResult( this.getTestPersistentBlatResult( bioSequence, null ) );
        b2gCol.add( b2g );

        //noinspection unchecked
        return ( Collection<BioSequence2GeneProduct> ) persisterHelper.persist( b2gCol );
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
                return this.getTestPersistentDatabaseEntry( accession, PersistentDummyObjectHelper.geo );
            case "PubMed":
                return this.getTestPersistentDatabaseEntry( accession, PersistentDummyObjectHelper.pubmed );
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
        ee.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) + "_testee" );
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

        Collection<FactorValue> allFactorValues = new HashSet<>();

        ExperimentalDesign ed = this.getExperimentalDesign( allFactorValues );
        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );

        log.debug( "expression experiment => design element data vectors" );
        Collection<RawExpressionDataVector> vectors = new HashSet<>();

        Collection<QuantitationType> quantitationTypes = this.addQuantitationTypes( new HashSet<QuantitationType>() );

        assert quantitationTypes.size() > 0;

        ee.setQuantitationTypes( quantitationTypes );
        ee.setTaxon( taxon );
        ee.setRawExpressionDataVectors( vectors );

        ArrayDesignsForExperimentCache c = persisterHelper.prepare( ee );
        return persisterHelper.persist( ee, c );
    }

    public GeneProduct getTestPersistentGeneProduct( Gene gene ) {
        GeneProduct gp = PersistentDummyObjectHelper.getTestNonPersistentGeneProduct( gene );
        return ( GeneProduct ) persisterHelper.persist( gp );
    }

    /**
     * Convenience method to provide a QuantitationType that can be used to fill non-nullable associations in test
     * objects.
     *
     * @return QT
     */
    public QuantitationType getTestPersistentQuantitationType() {
        QuantitationType qt = PersistentDummyObjectHelper.getTestNonPersistentQuantitationType();
        return ( QuantitationType ) persisterHelper.persist( qt );
    }

    public Taxon getTestPersistentTaxon() {
        if ( PersistentDummyObjectHelper.testTaxon == null ) {
            PersistentDummyObjectHelper.testTaxon = Taxon.Factory.newInstance();
            PersistentDummyObjectHelper.testTaxon.setCommonName( "elephant" );
            PersistentDummyObjectHelper.testTaxon.setScientificName( "Loxodonta" );
            PersistentDummyObjectHelper.testTaxon.setNcbiId( 1245 );
            PersistentDummyObjectHelper.testTaxon.setIsGenesUsable( true );
            PersistentDummyObjectHelper.testTaxon = ( Taxon ) persisterHelper
                    .persist( PersistentDummyObjectHelper.testTaxon );
            assert PersistentDummyObjectHelper.testTaxon != null
                    && PersistentDummyObjectHelper.testTaxon.getId() != null;
        }
        return PersistentDummyObjectHelper.testTaxon;
    }

    public void resetTestElementCollectionSize() {
        this.testElementCollectionSize = PersistentDummyObjectHelper.DEFAULT_TEST_ELEMENT_COLLECTION_SIZE;
    }

    protected Collection<ExperimentalFactor> getExperimentalFactors( ExperimentalDesign ed,
            Collection<FactorValue> allFactorValues ) {
        Collection<ExperimentalFactor> efCol = new HashSet<>();
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

    protected Collection<FactorValue> getFactorValues( ExperimentalFactor ef,
            Collection<FactorValue> allFactorValues ) {

        Collection<FactorValue> fvCol = new HashSet<>();
        for ( int i = 0; i < PersistentDummyObjectHelper.NUM_FACTOR_VALUES; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance();
            fv.setValue( "Factor value " + RandomStringUtils
                    .randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH ) );
            fv.setExperimentalFactor( ef );
            fvCol.add( fv );
        }

        allFactorValues.addAll( fvCol );

        return fvCol;
    }

    private Collection<QuantitationType> addQuantitationTypes( Collection<QuantitationType> quantitationTypes ) {
        for ( int quantitationTypeNum = 0; quantitationTypeNum < PersistentDummyObjectHelper.NUM_QUANTITATION_TYPES; quantitationTypeNum++ ) {
            QuantitationType q = PersistentDummyObjectHelper.getTestNonPersistentQuantitationType();
            if ( quantitationTypes.size() == 0 ) {
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
            Collection<FactorValue> fvCol = new HashSet<>();
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

        BioAssayDimension baDim = BioAssayDimension.Factory
                .newInstance( ee.getShortName() + "_" + RandomStringUtils.randomAlphanumeric( 20 ), null, bioAssays );

        Collection<RawExpressionDataVector> vectors = new HashSet<>();
        for ( QuantitationType quantType : quantitationTypes ) {
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
                byte[] bdata = this.getDoubleData();
                vector.setData( bdata );
                vector.setDesignElement( cs );

                assert cs.getArrayDesign() != null;

                vector.setExpressionExperiment( ee );
                vector.setQuantitationType( quantType );
                vector.setBioAssayDimension( baDim );
                vectors.add( vector );
            }
        }
        return vectors;
    }

    private byte[] getDoubleData() {
        double[] data = new double[PersistentDummyObjectHelper.NUM_BIOMATERIALS];
        double bump = 0.0;
        for ( int j = 0; j < data.length; j++ ) {
            data[j] = new Random().nextDouble() + bump;
            if ( j % 3 == 0 ) {
                // add some correlation structure to the data.
                bump += 0.5;
            }
        }
        ByteArrayConverter bconverter = new ByteArrayConverter();
        return bconverter.doubleArrayToBytes( data );
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

        de.setExternalDatabase( PersistentDummyObjectHelper.geo );
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
        if ( PersistentDummyObjectHelper.geo == null ) {
            PersistentDummyObjectHelper.geo = externalDatabaseService.findByName( "GEO" );
            assert PersistentDummyObjectHelper.geo != null;
        }
        bm.setSourceTaxon( PersistentDummyObjectHelper.getTestNonPersistentTaxon() );
        bm.setExternalAccession( this.getTestPersistentDatabaseEntry( PersistentDummyObjectHelper.geo ) );
        return bm;
    }

    /**
     * @return Slightly misleading, associations are persistent.
     */
    private BioMaterial getTestNonPersistentBioMaterial( Taxon tax ) {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( RandomStringUtils.randomNumeric( PersistentDummyObjectHelper.RANDOM_STRING_LENGTH )
                + "_testbiomaterial" );
        if ( PersistentDummyObjectHelper.geo == null ) {
            PersistentDummyObjectHelper.geo = externalDatabaseService.findByName( "GEO" );
            assert PersistentDummyObjectHelper.geo != null;
        }
        bm.setSourceTaxon( tax );
        bm.setExternalAccession( this.getTestPersistentDatabaseEntry( PersistentDummyObjectHelper.geo ) );
        return bm;
    }

}
