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

import java.util.Collection;
import java.util.HashSet;

import org.acegisecurity.providers.encoding.ShaPasswordEncoder;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.io.ByteArrayConverter;
import ubic.gemma.Constants;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.Reporter;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatAssociation;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.ConfigUtils;

/**
 * Used to generate test data.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class TestPersistentObjectHelper {

    /**
     * 
     */
    private static final int NUM_FACTOR_VALUES = 2;
    /**
     * 
     */
    private static final int NUM_EXPERIMENTAL_FACTORS = 3;
    private static final int NUM_QUANTITATION_TYPES = 2;
    private static final int NUM_BIOMATERIALS = 2;
    protected static final int RANDOM_STRING_LENGTH = 10;
    protected static final int TEST_ELEMENT_COLLECTION_SIZE = 6;
    private PersisterHelper persisterHelper;
    private ExternalDatabaseService externalDatabaseService;

    private TaxonService taxonService;

    private ExternalDatabase geo;
    private ExternalDatabase genbank;

    // private Taxon testTaxon;

    private ExternalDatabase pubmed;
    protected Log log = LogFactory.getLog( getClass() );

    /**
     * @return
     */
    Taxon testTaxon;

    /**
     * @return Collection
     */
    public Collection<BioAssay> getBioAssays( ArrayDesign ad ) {
        Collection<BioAssay> baCol = new HashSet<BioAssay>();
        // one biomaterial for each set of bioassays
        for ( int j = 0; j < NUM_BIOMATERIALS; j++ ) {
            BioMaterial bm = this.getTestPersistentBioMaterial();
            for ( int i = 0; i < TEST_ELEMENT_COLLECTION_SIZE; i++ ) {
                BioAssay ba = this.getTestPersistentBioAssay( ad, bm );
                baCol.add( ba );
            }
        }

        return baCol;
    }

    public Collection<DesignElementDataVector> getDesignElementDataVectors( ExpressionExperiment ee, ArrayDesign ad ) {

        Collection<BioAssay> bioAssays = ee.getBioAssays();
        BioAssayDimension baDim = BioAssayDimension.Factory.newInstance( RandomStringUtils.randomAlphanumeric( 20 ),
                null, bioAssays, null );

        baDim = ( BioAssayDimension ) persisterHelper.persist( baDim );

        Collection<DesignElementDataVector> vectors = new HashSet<DesignElementDataVector>();
        for ( int quantitationTypeNum = 0; quantitationTypeNum < NUM_QUANTITATION_TYPES; quantitationTypeNum++ ) {
            QuantitationType quantType = this.getTestPersistentQuantitationType();
            for ( CompositeSequence cs : ad.getCompositeSequences() ) {
                DesignElementDataVector vector = DesignElementDataVector.Factory.newInstance();
                double[] data = new double[TEST_ELEMENT_COLLECTION_SIZE / 2];
                for ( int j = 0; j < data.length; j++ ) {
                    data[j] = RandomUtils.nextDouble();
                }
                ByteArrayConverter bconverter = new ByteArrayConverter();
                byte[] bdata = bconverter.doubleArrayToBytes( data );
                vector.setData( bdata );

                vector.setDesignElement( cs );

                assert cs.getArrayDesign() != null;

                vector.setExpressionExperiment( ee );

                vector.setQuantitationType( quantType );
                vector.setBioAssayDimension( baDim );

                // we're only creating one vector here, but each design element can have more than one.
                vectors.add( vector );
                cs.setDesignElementDataVectors( vectors );
            }
        }
        return vectors;
    }

    public ExperimentalDesign getExperimentalDesign() {

        ExperimentalDesign ed = ExperimentalDesign.Factory.newInstance();
        ed.setName( "Experimental Design " + RandomStringUtils.randomNumeric( 10 ) );
        ed.setDescription( RandomStringUtils.randomNumeric( 10 ) + ": A test experimental design." );
        log.debug( "experimental design => experimental factors" );
        ed.setExperimentalFactors( getExperimentalFactors() ); // set test experimental factors
        return ed;
    }

    /**
     * @return
     */
    public Collection<ExperimentalFactor> getExperimentalFactors() {
        Collection<ExperimentalFactor> efCol = new HashSet<ExperimentalFactor>();
        for ( int i = 0; i < NUM_EXPERIMENTAL_FACTORS; i++ ) {
            ExperimentalFactor ef = ExperimentalFactor.Factory.newInstance();
            ef.setName( "Experimental Factor " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            ef.setDescription( i + ": A test experimental factor" );
            log.debug( "experimental factor => factor values" );
            ef.setFactorValues( getFactorValues() );
            efCol.add( ef );
        }
        return efCol;
    }

    /**
     * @return Collection
     */
    public Collection<FactorValue> getFactorValues() {
        Collection<FactorValue> fvCol = new HashSet<FactorValue>();
        for ( int i = 0; i < NUM_FACTOR_VALUES; i++ ) {
            FactorValue fv = FactorValue.Factory.newInstance();
            fv.setValue( "Factor value " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );
            fvCol.add( fv );
        }
        return fvCol;
    }

    /**
     * Add an expressionExperiment to the database for testing purposes. Includes associations.
     */
    public ExpressionExperiment getTestExpressionExperimentWithAllDependencies() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ArrayDesign ad = this.getTestPersistentArrayDesign( TEST_ELEMENT_COLLECTION_SIZE, false );

        ee.setName( "Expression Experiment " + RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) );

        ee.setDescription( "A test expression experiment" );

        ee.setSource( "http://www.ncbi.nlm.nih.gov/geo/" );

        DatabaseEntry de1 = this.getTestPersistentDatabaseEntry( geo );

        log.debug( "expression experiment => database entry" );
        ee.setAccession( de1 );

        log.debug( "expression experiment => bioassays" );
        ee.setBioAssays( getBioAssays( ad ) );

        log.debug( ee + " => experimentalDesign" );
        ee.setExperimentalDesign( getExperimentalDesign() );

        log.debug( "expression experiment -> owner " );

        ee.setOwner( this.getTestPersistentContact() );

        log.debug( "expression experiment => design element data vectors" );
        ee.setDesignElementDataVectors( getDesignElementDataVectors( ee, ad ) );

        return ( ExpressionExperiment ) persisterHelper.persist( ee );
    }

    /**
     * @return
     */
    public Gene getTestPeristentGene() {
        Gene gene = Gene.Factory.newInstance();
        gene.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        gene.setOfficialName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        gene.setOfficialSymbol( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ).toUpperCase() );
        gene.setTaxon( this.getTestNonPersistentTaxon() );
        return ( Gene ) persisterHelper.persist( gene );
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     * The ArrayDesign is provided with some CompositeSequenece DesignElements if desired. If composite seequences are
     * created, they are each associated with a single generated Reporter.
     * 
     * @param numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param randomNames If true, probe names will be random strings; otherwise they will be 0_probe_at....N_probe_at
     * @return ArrayDesign
     */
    @SuppressWarnings("unchecked")
    public ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();

        ad.setName( "arrayDesign_" + RandomStringUtils.randomAlphabetic( RANDOM_STRING_LENGTH ) );
        ad = ( ArrayDesign ) persisterHelper.persist( ad );

        for ( int i = 0; i < numCompositeSequences; i++ ) {

            Reporter reporter = Reporter.Factory.newInstance();
            CompositeSequence compositeSequence = CompositeSequence.Factory.newInstance();

            if ( randomNames ) {
                reporter.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testreporter" );
            } else {
                reporter.setName( i + "_probe_at" );
            }

            reporter.setCompositeSequence( compositeSequence );

            if ( randomNames ) {
                compositeSequence.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testcs" );
            } else {
                compositeSequence.setName( "probeset_" + i );
            }

            compositeSequence.getComponentReporters().add( reporter );
            compositeSequence.setArrayDesign( ad );
            ad.getCompositeSequences().add( compositeSequence );

            BioSequence bioSequence = getTestPersistentBioSequence();
            compositeSequence.setBiologicalCharacteristic( bioSequence );

            bioSequence.setBioSequence2GeneProduct( this.getTestPersistentBioSequence2GeneProducts( bioSequence ) );
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
        BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
        ba.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testbioassay" );
        ba.getSamplesUsed().add( bm );
        ba.setArrayDesignUsed( ad );
        ba = ( BioAssay ) persisterHelper.persist( ba );
        return ba;
    }

    /**
     * @return
     */
    public BioMaterial getTestPersistentBioMaterial() {
        BioMaterial bm = BioMaterial.Factory.newInstance();
        bm.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testbiomaterial" );
        if ( geo == null ) {
            geo = externalDatabaseService.find( "GEO" );
            assert geo != null;
        }
        bm.setSourceTaxon( getTestNonPersistentTaxon() );
        bm.setExternalAccession( this.getTestPersistentDatabaseEntry( geo ) );
        bm = ( BioMaterial ) persisterHelper.persist( bm );
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
    private Collection getTestPersistentBioSequence2GeneProducts( BioSequence bioSequence ) {

        Collection b2gCol = new HashSet<BioSequence2GeneProduct>();
        for ( int i = 0; i < TEST_ELEMENT_COLLECTION_SIZE; i++ ) {
            BlatAssociation b2g = BlatAssociation.Factory.newInstance();
            b2g.setScore( RandomUtils.nextDouble() );
            b2g.setBioSequence( bioSequence );
            b2g.setGeneProduct( this.getTestPersistentGeneProduct( this.getTestPeristentGene() ) );
            // FIXME is this the right biosequence for the blat?
            b2g.setBlatResult( this.getTestPersistentBlatResult( bioSequence ) );

            b2gCol.add( b2g );
        }

        return persisterHelper.persist( b2gCol );
    }

    /**
     * @param querySequence
     * @return
     */
    public BlatResult getTestPersistentBlatResult( BioSequence querySequence ) {
        BlatResult br = BlatResult.Factory.newInstance();
        br.setQuerySequence( querySequence );
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
        result = ( DatabaseEntry ) persisterHelper.persist( result );
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
     * @param gene
     * @return
     */
    public GeneProduct getTestPersistentGeneProduct( Gene gene ) {
        GeneProduct gp = GeneProduct.Factory.newInstance();

        return ( GeneProduct ) persisterHelper.persist( gp );
    }

    /**
     * Convenience method to provide a QuantitationType that can be used to fill non-nullable associations in test
     * objects.
     * 
     * @return
     */
    public QuantitationType getTestPersistentQuantitationType() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testqt" );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsBackground( false );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.MEASUREDSIGNAL );
        qt.setScale( ScaleType.LINEAR );
        qt = ( QuantitationType ) persisterHelper.persist( qt );
        return qt;
    }

    public Taxon getTestPersistentTaxon() {
        if ( testTaxon == null ) {
            testTaxon = Taxon.Factory.newInstance();
            testTaxon.setCommonName( "elephant" );
            testTaxon.setScientificName( "Loxodonta" );
            testTaxon.setNcbiId( 1245 );
            testTaxon = taxonService.findOrCreate( testTaxon );
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

        return t;
    }

    public User getTestPersistentUser() {
        return getTestPersistentUser( RandomStringUtils.randomAlphabetic( 6 ), ConfigUtils
                .getString( "gemma.admin.password" ) );
    }

    /**
     * @return
     */
    public User getTestPersistentUser( String username, String password ) {
        User testUser = User.Factory.newInstance();

        testUser.setName( "Foo" );
        testUser.setLastName( "Bar" );
        testUser.setEnabled( Boolean.TRUE );
        testUser.setUserName( username );
        testUser.setEmail( RandomStringUtils.randomAlphabetic( 6 ).toLowerCase() + "@gemma.org" );

        ShaPasswordEncoder encoder = new ShaPasswordEncoder();
        String encryptedPassword = encoder.encodePassword( password, ConfigUtils.getProperty( "gemma.salt" ) );

        UserRole ur = UserRole.Factory.newInstance( username, Constants.USER_ROLE, "regular user" );

        testUser.getRoles().add( ur );
        testUser.setPassword( encryptedPassword );
        testUser.setPasswordHint( "I am an idiot" );

        return ( User ) persisterHelper.persist( testUser );

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
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

}
