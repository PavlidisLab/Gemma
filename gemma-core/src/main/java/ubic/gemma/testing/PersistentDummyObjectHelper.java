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
package ubic.gemma.testing;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Chromosome;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.PhysicalLocation;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.Persister;

/**
 * Used to generate test data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class PersistentDummyObjectHelper {

    public static final int NUM_FACTOR_VALUES = 2;
    public static final int NUM_EXPERIMENTAL_FACTORS = 2;
    public static final int NUM_QUANTITATION_TYPES = 2;
    public static final int NUM_BIOMATERIALS = 8;
    private static final int RANDOM_STRING_LENGTH = 10;
    public static final int DEFAULT_TEST_ELEMENT_COLLECTION_SIZE = 6;

    private Persister persisterHelper;

    private ExternalDatabaseService externalDatabaseService;

    private ExternalDatabase geo;

    private ExternalDatabase genbank;

    private int testElementCollectionSize = DEFAULT_TEST_ELEMENT_COLLECTION_SIZE;

    // private Taxon testTaxon;

    private ExternalDatabase pubmed;

    private Collection<FactorValue> allFactorValues = new HashSet<FactorValue>();

    protected Log log = LogFactory.getLog( getClass() );

    /**
     * @return
     */
    Taxon testTaxon;
    Collection<Taxon> testTaxa = null;

    private Collection<BioMaterial> getBioMaterials() {

        if ( allFactorValues.isEmpty() )
            throw new RuntimeException(
                    "Factor values have not been associated with biomaterials.  Try creating the experimental design first." );

        Iterator<FactorValue> iter = allFactorValues.iterator();

        Collection<BioMaterial> baCol = new HashSet<BioMaterial>();
        // one biomaterial for each set of bioassays
        for ( int j = 0; j < NUM_BIOMATERIALS; j++ ) {
            BioMaterial bm = this.getTestNonPersistentBioMaterial();
            Collection<FactorValue> fvCol = new HashSet<FactorValue>();
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
     * @return Collection
     */
    private Collection<BioAssay> getBioAssays( Collection<BioMaterial> bioMaterials, ArrayDesign ad ) {
        Collection<BioAssay> baCol = new HashSet<BioAssay>();
        for ( BioMaterial bm : bioMaterials ) {
            BioAssay ba = this.getTestNonPersistentBioAssay( ad, bm );
            bm.getBioAssaysUsedIn().add( ba );
            baCol.add( ba );
        }
        return baCol;
    }

    /**
     * These are nonpersistent
     * 
     * @param ee
     * @param bioAssays
     * @param ad
     * @return
     */
    private Collection<RawExpressionDataVector> getDesignElementDataVectors( ExpressionExperiment ee,
            Collection<QuantitationType> quantitationTypes, Collection<BioAssay> bioAssays, ArrayDesign ad ) {

        BioAssayDimension baDim = BioAssayDimension.Factory.newInstance(
                ee.getShortName() + "_" + RandomStringUtils.randomAlphanumeric( 20 ), null, bioAssays );

        Collection<RawExpressionDataVector> vectors = new HashSet<RawExpressionDataVector>();
        for ( QuantitationType quantType : quantitationTypes ) {
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                RawExpressionDataVector vector = RawExpressionDataVector.Factory.newInstance();
                byte[] bdata = getDoubleData();
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
        double[] data = new double[NUM_BIOMATERIALS];
        double bump = 0.0;
        for ( int j = 0; j < data.length; j++ ) {
            data[j] = RandomUtils.nextDouble() + bump;
            if ( j % 3 == 0 ) {
                // add some correlation structure to the data.
                bump += 0.5;
            }
        }
        ByteArrayConverter bconverter = new ByteArrayConverter();
        byte[] bdata = bconverter.doubleArrayToBytes( data );
        return bdata;
    }

    /**
     * Non-persistent
     * 
     * @return
     */
    public ExperimentalDesign getExperimentalDesign() {
        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( "Experimental Design " + RandomStringUtils.randomNumeric( 10 ) );
        ed.setDescription( RandomStringUtils.randomNumeric( 10 ) + ": A test experimental design." );
        log.debug( "experimental design => experimental factors" );
        ed.setExperimentalFactors( getExperimentalFactors( ed ) ); // set test experimental factors
        return ed;
    }

    /**
     * @return
     */
    public Collection<ExperimentalFactor> getExperimentalFactors( ExperimentalDesign ed ) {
        Collection<ExperimentalFactor> efCol = new HashSet<ExperimentalFactor>();
        for ( int i = 0; i < NUM_EXPERIMENTAL_FACTORS; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setExperimentalDesign( ed );
            ef.setName( "Experimental Factor " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            ef.setDescription( i + ": A test experimental factor" );
            ef.setType( FactorType.CATEGORICAL );
            VocabCharacteristic c = VocabCharacteristic.Factory.newInstance();
            c.setCategory( "OrganismPart" );
            c.setName( "OrganismPart" );
            ef.setCategory( c );
            log.debug( "experimental factor => factor values" );
            ef.setFactorValues( getFactorValues( ef ) );
            efCol.add( ef );
        }
        return efCol;
    }

    /**
     * @return Collection
     */
    public Collection<FactorValue> getFactorValues( ExperimentalFactor ef ) {

        Collection<FactorValue> fvCol = new HashSet<FactorValue>();
        for ( int i = 0; i < NUM_FACTOR_VALUES; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance();
            fv.setValue( "Factor value " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            fv.setExperimentalFactor( ef );
            fvCol.add( fv );
        }

        allFactorValues.addAll( fvCol );

        return fvCol;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations.
     */
    public ExpressionExperiment getTestExpressionExperimentWithAllDependencies() {
        return this.getTestExpressionExperimentWithAllDependencies( true );
    }

    /**
     * @return A lighter-weight EE, with no data, and the ADs have no sequences.
     */
    public ExpressionExperiment getTestPersistentBasicExpressionExperiment() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
        ee.setName( "Expression Experiment " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
        ee.setDescription( "A test expression experiment" );
        ee.setSource( "http://www.ncbi.nlm.nih.gov/geo/" );
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( geo );
        ee.setAccession( de1 );

        ArrayDesign adA = this.getTestPersistentArrayDesign( 0, false, false );
        ArrayDesign adB = this.getTestPersistentArrayDesign( 0, false, false );

        ExperimentalDesign ed = getExperimentalDesign();
        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );

        Collection<BioAssay> bioAssays = new HashSet<BioAssay>();
        Collection<BioMaterial> bioMaterials = getBioMaterials();
        Collection<BioAssay> bioAssaysA = getBioAssays( bioMaterials, adA );
        Collection<BioAssay> bioAssaysB = getBioAssays( bioMaterials, adB );
        bioAssays.addAll( bioAssaysA );
        bioAssays.addAll( bioAssaysB );
        ee.setBioAssays( bioAssays );

        Collection<QuantitationType> quantitationTypes = new HashSet<QuantitationType>();
        for ( int quantitationTypeNum = 0; quantitationTypeNum < NUM_QUANTITATION_TYPES; quantitationTypeNum++ ) {
            QuantitationType q = getTestNonPersistentQuantitationType();
            if ( quantitationTypes.size() == 0 ) {
                q.setIsPreferred( true );
            }
            quantitationTypes.add( q );
        }

        assert quantitationTypes.size() > 0;
        ee.setQuantitationTypes( quantitationTypes );

        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );

        return ee;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations
     * 
     * @param dosequence Should the array design get all the sequence information filled in? (true = slower)
     */
    public ExpressionExperiment getTestExpressionExperimentWithAllDependencies( boolean dosequence ) {

        this.allFactorValues.clear();

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setShortName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
        ee.setName( "Expression Experiment " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
        ee.setDescription( "A test expression experiment" );
        ee.setSource( "http://www.ncbi.nlm.nih.gov/geo/" );
        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( geo );
        ee.setAccession( de1 );

        LocalFile file = LocalFile.Factory.newInstance();
        try {
            file.setLocalURL( new URL( "file:///just/a/placeholder/" + ee.getShortName() ) );
        } catch ( MalformedURLException e ) {
        }

        ee.setRawDataFile( file );

        ArrayDesign adA = this.getTestPersistentArrayDesign( this.getTestElementCollectionSize(), false, dosequence );
        ArrayDesign adB = this.getTestPersistentArrayDesign( this.getTestElementCollectionSize(), false, dosequence );

        ExperimentalDesign ed = getExperimentalDesign();
        ee.setExperimentalDesign( ed );
        ee.setOwner( this.getTestPersistentContact() );

        Collection<BioAssay> bioAssays = new HashSet<BioAssay>();
        Collection<BioMaterial> bioMaterials = getBioMaterials();
        Collection<BioAssay> bioAssaysA = getBioAssays( bioMaterials, adA );
        Collection<BioAssay> bioAssaysB = getBioAssays( bioMaterials, adB );
        bioAssays.addAll( bioAssaysA );
        bioAssays.addAll( bioAssaysB );
        ee.setBioAssays( bioAssays );

        log.debug( "expression experiment => design element data vectors" );
        Collection<RawExpressionDataVector> vectors = new HashSet<RawExpressionDataVector>();

        Collection<QuantitationType> quantitationTypes = new HashSet<QuantitationType>();
        for ( int quantitationTypeNum = 0; quantitationTypeNum < NUM_QUANTITATION_TYPES; quantitationTypeNum++ ) {
            QuantitationType q = getTestNonPersistentQuantitationType();
            if ( quantitationTypes.size() == 0 ) {
                q.setIsPreferred( true );
            }
            quantitationTypes.add( q );
        }

        assert quantitationTypes.size() > 0;

        vectors.addAll( getDesignElementDataVectors( ee, quantitationTypes, bioAssaysA, adA ) );
        vectors.addAll( getDesignElementDataVectors( ee, quantitationTypes, bioAssaysB, adB ) );

        ee.setQuantitationTypes( quantitationTypes );

        ee.setRawExpressionDataVectors( vectors );

        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );

        return ee;
    }

    /**
     * @param ee
     */
    public void addTestAnalyses( ExpressionExperiment ee ) {
        /*
         * Add analyses
         */
        ProbeCoexpressionAnalysis pca = ProbeCoexpressionAnalysis.Factory.newInstance();
        pca.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );

        pca.setExperimentAnalyzed( ee );

        persisterHelper.persist( pca );

        /*
         * Diff
         */
        DifferentialExpressionAnalysis expressionAnalysis = DifferentialExpressionAnalysis.Factory.newInstance();
        Protocol protocol = Protocol.Factory.newInstance();
        protocol.setName( "Differential expression analysis settings" );
        protocol.setDescription( "qvalue: " + true );
        expressionAnalysis.setProtocol( protocol );
        expressionAnalysis.setExperimentAnalyzed( ee );

        persisterHelper.persist( expressionAnalysis );
    }

    /**
     * @return with default taxon
     */
    public Gene getTestPeristentGene() {
        return this.getTestPeristentGene( null );
    }

    /**
     * @return
     */
    public Gene getTestPeristentGene( Taxon t ) {
        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        gene.setOfficialName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        gene.setOfficialSymbol( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ).toUpperCase() );

        if ( t == null ) {
            gene.setTaxon( this.getTestNonPersistentTaxon() );
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
     * The ArrayDesign is provided with some CompositeSequenece DesignElements if desired. If composite seequences are
     * created, they are each associated with a single generated Reporter.
     * 
     * @param numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param randomNames If true, probe names will be random strings; otherwise they will be 0_probe_at....N_probe_at
     * @param dosequence If true, biosequences and biosequence2GeneProduct associations are filled in (slower).
     * @return ArrayDesign
     */
    public ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames, boolean dosequence ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();

        ad.setName( "arrayDesign_" + RandomStringUtils.randomAlphabetic( RANDOM_STRING_LENGTH ) );
        ad.setShortName( "AD_" + RandomStringUtils.randomAlphabetic( 5 ) );
        ad.setTechnologyType( TechnologyType.ONECOLOR );

        ad.setPrimaryTaxon( this.getTestPersistentTaxon() );

        for ( int i = 0; i < numCompositeSequences; i++ ) {

            // Reporter reporter = Reporter.Factory.newInstance();
            CompositeSequence compositeSequence = CompositeSequence.Factory.newInstance();

            // if ( randomNames ) {
            // reporter.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testreporter" );
            // } else {
            // reporter.setName( i + "_probe_at" );
            // }
            //
            // reporter.setCompositeSequence( compositeSequence );

            if ( randomNames ) {
                compositeSequence.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testcs" );
            } else {
                compositeSequence.setName( "probeset_" + i );
            }

            // compositeSequence.getComponentReporters().add( reporter );
            compositeSequence.setArrayDesign( ad );
            ad.getCompositeSequences().add( compositeSequence );

            if ( dosequence ) {
                BioSequence bioSequence = getTestPersistentBioSequence();
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

    public BibliographicReference getTestPersistentBibliographicReference( String accession ) {
        BibliographicReference br = BibliographicReference.Factory.newInstance();
        if ( pubmed == null ) {
            pubmed = externalDatabaseService.find( "PubMed" );
        }
        br.setPubAccession( this.getTestPersistentDatabaseEntry( accession, pubmed ) );
        return ( BibliographicReference ) persisterHelper.persist( br );
    }

    public BioAssay getTestPersistentBioAssay( ArrayDesign ad ) {
        BioMaterial bm = this.getTestPersistentBioMaterial();
        return getTestPersistentBioAssay( ad, bm );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * 
     * @return
     */
    public BioAssay getTestPersistentBioAssay( ArrayDesign ad, BioMaterial bm ) {
        if ( ad == null || bm == null ) {
            throw new IllegalArgumentException();
        }
        BioAssay ba = getTestNonPersistentBioAssay( ad, bm );
        return ( BioAssay ) persisterHelper.persist( ba );
    }

    /**
     * @param ad
     * @param bm
     * @return
     */
    private BioAssay getTestNonPersistentBioAssay( ArrayDesign ad, BioMaterial bm ) {
        BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
        ba.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testbioassay" );
        ba.getSamplesUsed().add( bm );
        ba.setArrayDesignUsed( ad );
        DatabaseEntry de = DatabaseEntry.Factory.newInstance();

        de.setExternalDatabase( this.geo );
        de.setAccession( ba.getName() );
        ba.setAccession( de );

        LocalFile file = LocalFile.Factory.newInstance();
        try {
            file.setLocalURL( new URL( "file:///tmp/" + ba.getName() ) );
        } catch ( MalformedURLException e ) {
        }
        ba.setRawDataFile( file );

        LocalFile fileb = LocalFile.Factory.newInstance();
        try {
            fileb.setLocalURL( new URL( "file:///tmp/raw" + ba.getName() ) );
        } catch ( MalformedURLException e ) {
        }
        ba.setRawDataFile( file );

        ba.getDerivedDataFiles().add( fileb );

        return ba;
    }

    /**
     * @return
     */
    public BioMaterial getTestPersistentBioMaterial() {
        BioMaterial bm = getTestNonPersistentBioMaterial();
        return ( BioMaterial ) persisterHelper.persist( bm );
    }

    /**
     * @return
     */
    public BioMaterial getTestPersistentBioMaterial( Taxon tax ) {
        BioMaterial bm = getTestNonPersistentBioMaterial( tax );
        return ( BioMaterial ) persisterHelper.persist( bm );
    }

    /**
     * Slightly misleading, associations are persistent.
     * 
     * @return
     */
    private BioMaterial getTestNonPersistentBioMaterial() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testbiomaterial" );
        if ( geo == null ) {
            geo = externalDatabaseService.find( "GEO" );
            assert geo != null;
        }
        bm.setSourceTaxon( getTestNonPersistentTaxon() );
        bm.setExternalAccession( this.getTestPersistentDatabaseEntry( geo ) );
        return bm;
    }

    /**
     * Slightly misleading, associations are persistent.
     * 
     * @return
     */
    private BioMaterial getTestNonPersistentBioMaterial( Taxon tax ) {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testbiomaterial" );
        if ( geo == null ) {
            geo = externalDatabaseService.find( "GEO" );
            assert geo != null;
        }
        bm.setSourceTaxon( tax );
        bm.setExternalAccession( this.getTestPersistentDatabaseEntry( geo ) );
        return bm;
    }

    /**
     * @return
     */
    public BioSequence getTestPersistentBioSequence() {
        BioSequence bs = getTestNonPersistentBioSequence();

        return ( BioSequence ) persisterHelper.persist( bs );
    }

    /**
     * @return
     */
    public BioSequence getTestNonPersistentBioSequence() {
        BioSequence bs = BioSequence.Factory.newInstance();
        bs.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testbiosequence" );
        bs.setSequence( RandomStringUtils.random( 40, "ATCG" ) );
        bs.setTaxon( getTestNonPersistentTaxon() );

        if ( this.genbank == null ) {
            genbank = ExternalDatabase.Factory.newInstance();
            genbank.setName( "Genbank" );
        }

        DatabaseEntry de = DatabaseEntry.Factory.newInstance();

        de.setExternalDatabase( this.genbank );
        de.setAccession( RandomStringUtils.randomAlphanumeric( 10 ) );

        bs.setSequenceDatabaseEntry( de );
        return bs;
    }

    /**
     * @return Collection<BioSequence2GeneProduct>
     */
    private Collection<BioSequence2GeneProduct> getTestPersistentBioSequence2GeneProducts( BioSequence bioSequence ) {

        Collection<BioSequence2GeneProduct> b2gCol = new HashSet<BioSequence2GeneProduct>();
        for ( int i = 0; i < DEFAULT_TEST_ELEMENT_COLLECTION_SIZE; i++ ) {
            BlatAssociation b2g = BlatAssociation.Factory.newInstance();
            b2g.setScore( RandomUtils.nextDouble() );
            b2g.setBioSequence( bioSequence );
            b2g.setGeneProduct( this.getTestPersistentGeneProduct( this.getTestPeristentGene() ) );
            b2g.setBlatResult( this.getTestPersistentBlatResult( bioSequence ) );
            b2gCol.add( b2g );
        }

        return ( Collection<BioSequence2GeneProduct> ) persisterHelper.persist( b2gCol );
    }

    /**
     * @param querySequence
     * @return
     */
    public BlatResult getTestPersistentBlatResult( BioSequence querySequence ) {
        BlatResult br = BlatResult.Factory.newInstance();

        Chromosome chromosome = Chromosome.Factory.newInstance( "X", this.getTestPersistentTaxon() );
        chromosome.setSequence( getTestPersistentBioSequence() );
        chromosome = ( Chromosome ) persisterHelper.persist( chromosome );
        br.setTargetChromosome( chromosome );
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
     * @return
     */
    public Contact getTestPersistentContact() {
        Contact c = Contact.Factory.newInstance();
        c.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testcontact" );
        c = ( Contact ) persisterHelper.persist( c );
        return c;
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession is set to a random string
     * 
     * @return
     */
    public DatabaseEntry getTestPersistentDatabaseEntry( ExternalDatabase ed ) {
        return this.getTestPersistentDatabaseEntry( RandomStringUtils.randomAlphabetic( RANDOM_STRING_LENGTH ), ed );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     * 
     * @return
     */
    public DatabaseEntry getTestPersistentDatabaseEntry( String accession, ExternalDatabase ed ) {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        if ( accession == null ) {
            result.setAccession( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testaccession" );
        } else {
            result.setAccession( accession );
        }

        if ( ed == null ) {
            ed = ExternalDatabase.Factory.newInstance();
            ed.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testdb" );
            ed = ( ExternalDatabase ) persisterHelper.persist( ed );
        }

        result.setExternalDatabase( ed );
        return result;
    }

    /**
     * @param accession
     * @param databaseName GEO or PubMed (others could be supported)
     * @return
     */
    public DatabaseEntry getTestPersistentDatabaseEntry( String accession, String databaseName ) {
        if ( databaseName.equals( "GEO" ) ) {
            return this.getTestPersistentDatabaseEntry( accession, geo );
        } else if ( databaseName.equals( "PubMed" ) ) {
            return this.getTestPersistentDatabaseEntry( accession, pubmed );
        } else {
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
     * @return
     */
    public ExpressionExperiment getTestPersistentExpressionExperiment() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testee" );
        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );
        return ee;
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object.
     * 
     * @return
     */
    public ExpressionExperiment getTestPersistentExpressionExperiment( Collection<BioAssay> bioAssays ) {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testee" );
        ee.setBioAssays( bioAssays );
        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );
        return ee;
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object except for the creation of
     * persistent BioMaterials and BioAssays so that database taxon lookups for this experiment will work.
     * 
     * @param taxon the experiment will have this taxon
     * @return
     */
    public ExpressionExperiment getTestPersistentExpressionExperiment( Taxon taxon ) {
        BioAssay ba = null;
        BioMaterial bm = null;
        ArrayDesign ad = null;

        bm = this.getTestPersistentBioMaterial( taxon );
        ad = this.getTestPersistentArrayDesign( 4, true, true );
        ba = this.getTestPersistentBioAssay( ad, bm );
        Set<BioAssay> bas1 = new HashSet<BioAssay>();
        bas1.add( ba );

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testee" );
        ee.setBioAssays( bas1 );
        ee = ( ExpressionExperiment ) persisterHelper.persist( ee );
        return ee;
    }

    /**
     * @param gene
     * @return
     */
    public GeneProduct getTestPersistentGeneProduct( Gene gene ) {
        GeneProduct gp = getTestNonPersistentGeneProduct( gene );
        return ( GeneProduct ) persisterHelper.persist( gp );
    }

    /**
     * @param gene
     * @return
     */
    public GeneProduct getTestNonPersistentGeneProduct( Gene gene ) {
        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setNcbiGi( RandomStringUtils.randomAlphanumeric( 10 ) );
        gp.setName( RandomStringUtils.randomAlphanumeric( 6 ) );
        gp.setGene( gene );
        return gp;
    }

    /**
     * Convenience method to provide a QuantitationType that can be used to fill non-nullable associations in test
     * objects.
     * 
     * @return
     */
    public QuantitationType getTestPersistentQuantitationType() {
        QuantitationType qt = getTestNonPersistentQuantitationType();
        return ( QuantitationType ) persisterHelper.persist( qt );
    }

    /**
     * Note that if you want a 'preferred' qt or other special properites you have to set it yourself.
     * 
     * @return
     */
    public static QuantitationType getTestNonPersistentQuantitationType() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testqt" );
        qt.setDescription( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testqt" );
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

    public Taxon getTestPersistentTaxon() {
        if ( testTaxon == null ) {
            testTaxon = Taxon.Factory.newInstance();
            testTaxon.setCommonName( "elephant" );
            testTaxon.setScientificName( "Loxodonta" );
            testTaxon.setNcbiId( 1245 );
            testTaxon.setIsSpecies( true );
            testTaxon.setIsGenesUsable( true );
            testTaxon = ( Taxon ) persisterHelper.persist( testTaxon );
            assert testTaxon != null && testTaxon.getId() != null;
        }
        return testTaxon;
    }

    /**
     * To allow the persister helper to manaage
     * 
     * @return
     */
    private Taxon getTestNonPersistentTaxon() {

        Taxon t = Taxon.Factory.newInstance();
        t.setCommonName( "mouse" );
        t.setScientificName( "Mus musculus" );
        t.setIsSpecies( true );
        t.setIsGenesUsable( true );

        return t;
    }

    /**
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * Override the number of elements made in collections. By default this is quite small, so you can increase it. But
     * please call 'reset' afterwards.
     * 
     * @param testElementCollectionSize
     */
    public void setTestElementCollectionSize( int testElementCollectionSize ) {
        this.testElementCollectionSize = testElementCollectionSize;
    }

    public void resetTestElementCollectionSize() {
        this.testElementCollectionSize = DEFAULT_TEST_ELEMENT_COLLECTION_SIZE;
    }

    public int getTestElementCollectionSize() {
        return testElementCollectionSize;
    }

}
