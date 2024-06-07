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
import org.hibernate.SessionFactory;
import org.junit.Rule;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import ubic.gemma.model.analysis.Analysis;
import ubic.gemma.model.association.BioSequence2GeneProduct;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * Add a few utilities on top of {@link BaseIntegrationTest}.
 * @author pavlidis
 * @deprecated favour the simpler {@link BaseIntegrationTest} for new tests
 */
@Deprecated
@SuppressWarnings({ "WeakerAccess", "SameParameterValue", "unused" }) // Better left as is for future convenience
public abstract class BaseSpringContextTest extends BaseIntegrationTest {

    /* shared fixtures */
    private static ArrayDesign readOnlyAd = null;
    private static ExpressionExperiment readOnlyEe = null;

    protected final Log log = LogFactory.getLog( this.getClass() );

    /**
     * This allows the usage of {@link org.mockito.Mock} annotation to create mocks.
     */
    @Rule
    public MockitoRule rule = MockitoJUnit.rule();

    /**
     * The data source as defined in ubic/gemma/applicationContext-dataSource.xml
     */
    @Autowired
    private DataSource dataSource;
    @Autowired
    protected ExternalDatabaseService externalDatabaseService;
    @Autowired
    protected PersisterHelper persisterHelper;
    @Autowired
    protected TaxonService taxonService;
    @Autowired
    protected PersistentDummyObjectHelper testHelper;
    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    /**
     * The SimpleJdbcTemplate that this base class manages, available to subclasses. (Datasource; autowired at setter)
     */
    private JdbcTemplate jdbcTemplate;

    protected JdbcTemplate getJdbcTemplate() {
        if ( jdbcTemplate == null ) {
            jdbcTemplate = new JdbcTemplate( dataSource );
        }
        return jdbcTemplate;
    }

    /**
     * Obtain a taxon by its common name.
     * <p>
     * See {@code sql/init-data.sql} for a list of available taxa to use in tests.
     */
    public Taxon getTaxon( String commonName ) {
        return requireNonNull( this.taxonService.findByCommonName( commonName ),
                String.format( "Unknown taxa with common name %s, has the test data been loaded?", commonName ) );
    }

    public Gene getTestPersistentGene( Taxon taxon ) {
        return testHelper.getTestPersistentGene( taxon );
    }

    public Set<BioSequence2GeneProduct> getTestPersistentBioSequence2GeneProducts( BioSequence bioSequence ) {
        return testHelper.getTestPersistentBioSequence2GeneProducts( bioSequence );
    }

    /**
     * Convenience shortcut for RandomStringUtils.randomAlphabetic( 10 ) (or something similar to that)
     *
     * @return random alphabetic string
     */
    public String randomName() {
        return RandomStringUtils.randomAlphabetic( 10 );
    }

    /**
     * Create test {@link Analysis} for the given expression experiment.
     *
     * @param ee expression experiment to use for creating test analyses
     * @return a collection of persisted analyses that were created
     */
    protected Collection<Analysis> addTestAnalyses( ExpressionExperiment ee ) {
        return testHelper.addTestAnalyses( ee );
    }

    /**
     * Count the rows in the given table.
     *
     * @param tableName table name to count rows in
     * @return the number of rows in the table
     */
    protected int countRowsInTable( String tableName ) {
        return JdbcTestUtils.countRowsInTable( this.jdbcTemplate, tableName );
    }

    /**
     * Convenience method for deleting all rows from the specified tables. Use with caution outside of a transaction!
     *
     * @param names the names of the tables from which to remove
     * @return the total number of rows deleted from all specified tables
     */
    protected int deleteFromTables( String... names ) {
        return JdbcTestUtils.deleteFromTables( this.jdbcTemplate, names );
    }

    protected Gene getTestPersistentGene() {
        return testHelper.getTestPersistentGene();
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     * The ArrayDesign is provided with some CompositeSequence DesignElements if desired. If composite sequences are
     * created, they are each associated with a single generated Reporter.
     *
     * @param numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param randomNames           If true, probe names will be random strings; otherwise they will be 0_at....N_at
     * @return array design
     */
    protected ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames ) {
        return testHelper.getTestPersistentArrayDesign( numCompositeSequences, randomNames, true );
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     * The ArrayDesign is provided with some CompositeSequence DesignElements if desired. If composite sequences are
     * created, they are each associated with a single generated Reporter.
     *
     * @param numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param randomNames           If true, probe names will be random strings; otherwise they will be 0_at....N_at
     * @param doSequence            add sequences to the array design that is created. Faster to avoid if you can.
     * @param readOnly              read only
     * @return array design
     */
    protected ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames,
            boolean doSequence, boolean readOnly ) {
        if ( readOnly ) {
            if ( BaseSpringContextTest.readOnlyAd == null ) {
                BaseSpringContextTest.readOnlyAd = testHelper
                        .getTestPersistentArrayDesign( numCompositeSequences, randomNames, doSequence );
            }
            return BaseSpringContextTest.readOnlyAd;
        }
        return testHelper.getTestPersistentArrayDesign( numCompositeSequences, randomNames, doSequence );
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     *
     * @param probeNames will be assigned to each CompositeSequence in the ArrayDesign
     * @param taxon      of the ArrayDesign
     * @return ArrayDesign with no TechnologyType
     */
    protected ArrayDesign getTestPersistentArrayDesign( List<String> probeNames, Taxon taxon ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();

        ad.setShortName( "Generic_" + taxon.getCommonName() + "_" + RandomStringUtils.randomAlphabetic( 10 ) );
        ad.setName( "Generic test platform for " + taxon.getCommonName() );
        ad.setTechnologyType( TechnologyType.GENELIST );
        ad.setPrimaryTaxon( taxon );

        for ( String probeName : probeNames ) {

            // Reporter reporter = Reporter.Factory.newInstance();
            CompositeSequence compositeSequence = CompositeSequence.Factory.newInstance();

            compositeSequence.setName( probeName );

            // compositeSequence.getComponentReporters().add( reporter );
            compositeSequence.setArrayDesign( ad );
            ad.getCompositeSequences().add( compositeSequence );

            BioSequence bioSequence = this.getTestPersistentBioSequence();
            compositeSequence.setBiologicalCharacteristic( bioSequence );
            bioSequence.setBioSequence2GeneProduct( this.getTestPersistentBioSequence2GeneProducts( bioSequence ) );

        }

        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            cs.setArrayDesign( ad );
        }
        assert ( ad.getCompositeSequences().size() == probeNames.size() );

        return ( ArrayDesign ) persisterHelper.persist( ad );
    }

    /**
     * @return EE with no data; just bioassays, biomaterials, quantitation types and (minimal) array designs.
     */
    protected ExpressionExperiment getTestPersistentBasicExpressionExperiment() {
        return testHelper.getTestPersistentBasicExpressionExperiment();
    }

    /**
     * @param arrayDesign platform
     * @return EE with no data; just bioassays, biomaterials, quantitation types and provided arrayDesign
     */
    protected ExpressionExperiment getTestPersistentBasicExpressionExperiment( ArrayDesign arrayDesign ) {
        return testHelper.getTestPersistentBasicExpressionExperiment( arrayDesign );
    }

    protected BibliographicReference getTestPersistentBibliographicReference( String accession ) {
        return testHelper.getTestPersistentBibliographicReference( accession );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     *
     * @param ad paltform
     * @return bio assay
     */
    protected BioAssay getTestPersistentBioAssay( ArrayDesign ad ) {
        return testHelper.getTestPersistentBioAssay( ad );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     *
     * @param ad platform
     * @param bm material
     * @return bio assay
     */
    protected BioAssay getTestPersistentBioAssay( ArrayDesign ad, BioMaterial bm ) {
        return testHelper.getTestPersistentBioAssay( ad, bm );
    }

    protected BioMaterial getTestPersistentBioMaterial() {
        return testHelper.getTestPersistentBioMaterial();
    }

    protected BioMaterial getTestPersistentBioMaterial( Taxon tax ) {
        return testHelper.getTestPersistentBioMaterial( tax );
    }

    protected BioSequence getTestPersistentBioSequence() {
        return testHelper.getTestPersistentBioSequence();
    }

    protected BioSequence getTestPersistentBioSequence( Taxon t ) {
        return testHelper.getTestPersistentBioSequence( t );
    }

    protected BioSequence getTestNonPersistentBioSequence( Taxon t ) {
        return PersistentDummyObjectHelper.getTestNonPersistentBioSequence( t );
    }

    protected BlatResult getTestPersistentBlatResult( BioSequence querySequence, Taxon taxon ) {
        return testHelper.getTestPersistentBlatResult( querySequence, taxon );
    }

    protected BlatResult getTestPersistentBlatResult( BioSequence querySequence ) {
        return testHelper.getTestPersistentBlatResult( querySequence, null );
    }

    /**
     * Convenience method to get a (fairly) complete randomly generated persisted expression experiment.
     *
     * @param readOnly If the test only needs to read, a new data set might not be created.
     * @return EE
     */
    protected ExpressionExperiment getTestPersistentCompleteExpressionExperiment( boolean readOnly ) {
        if ( readOnly ) {
            if ( BaseSpringContextTest.readOnlyEe == null ) {
                log.info( "Initializing test expression experiment (one-time for read-only tests)" );
                BaseSpringContextTest.readOnlyEe = testHelper.getTestExpressionExperimentWithAllDependencies();
            }
            return BaseSpringContextTest.readOnlyEe;
        }

        return testHelper.getTestExpressionExperimentWithAllDependencies();
    }

    /**
     * Convenience method to get a (fairly) complete randomly generated persisted expression experiment.
     *
     * @return EE
     */
    protected ExpressionExperiment getTestPersistentCompleteExpressionExperimentWithSequences() {
        testHelper.resetSeed();
        return testHelper.getTestExpressionExperimentWithAllDependencies( true );
    }

    protected ExpressionExperiment getNewTestPersistentCompleteExpressionExperiment() {
        testHelper.resetSeed();
        return testHelper.getTestExpressionExperimentWithAllDependencies( false );
    }

    /**
     * @param prototype used to choose the platform
     * @return EE
     */
    protected ExpressionExperiment getTestPersistentCompleteExpressionExperimentWithSequences(
            ExpressionExperiment prototype ) {
        testHelper.resetSeed();
        return testHelper.getTestExpressionExperimentWithAllDependencies( prototype );
    }

    /**
     * Convenience method to provide a Contact that can be used to fill non-nullable associations in test objects.
     *
     * @return Contact
     */
    protected Contact getTestPersistentContact() {
        return testHelper.getTestPersistentContact();
    }

    /**
     * Get a database entry from a fictitious database.
     *
     * @return Db entry
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry() {
        return this.getTestPersistentDatabaseEntry( null, RandomStringUtils.randomAlphabetic( 10 ) );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     *
     * @param ed external Db
     * @return Db entry
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry( ExternalDatabase ed ) {
        return this.getTestPersistentDatabaseEntry( null, ed );
    }

    protected DatabaseEntry getTestPersistentDatabaseEntry( String ed ) {
        return this.getTestPersistentDatabaseEntry( null, ed );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     *
     * @param accession accession
     * @param ed        database
     * @return Db entry
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry( String accession, ExternalDatabase ed ) {
        return testHelper.getTestPersistentDatabaseEntry( accession, ed );
    }

    protected DatabaseEntry getTestPersistentDatabaseEntry( String accession, String ed ) {
        return testHelper.getTestPersistentDatabaseEntry( accession, ed );
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object.
     *
     * @return EE
     */
    protected ExpressionExperiment getTestPersistentExpressionExperiment() {
        return testHelper.getTestPersistentExpressionExperiment();
    }


    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object except for the creation of
     * persistent BioMaterials and BioAssays so that database taxon lookups for this experiment will work.
     *
     * @param taxon taxon
     * @return EE
     */
    protected ExpressionExperiment getTestPersistentExpressionExperiment( Taxon taxon ) {
        return testHelper.getTestPersistentExpressionExperiment( taxon );
    }

    protected GeneProduct getTestPersistentGeneProduct( Gene gene ) {
        return testHelper.getTestPersistentGeneProduct( gene );
    }

    /**
     * Convenience method to provide a QuantitationType that can be used to fill non-nullable associations in test
     * objects.
     *
     * @return QT
     */
    protected QuantitationType getTestPersistentQuantitationType() {
        return testHelper.getTestPersistentQuantitationType();
    }

    /**
     * Restore to default.
     */
    protected void resetTestCollectionSize() {
        testHelper.resetTestElementCollectionSize();
    }

    /**
     * @see TestAuthenticationUtils#runAsAdmin()
     */
    protected void runAsAdmin() {
        testAuthenticationUtils.runAsAdmin();
    }

    /**
     * @see TestAuthenticationUtils#runAsAgent()
     */
    protected void runAsAgent() {
        testAuthenticationUtils.runAsAgent();
    }

    /**
     * @see TestAuthenticationUtils#runAsUser(String, boolean)
     */
    protected void runAsUser( String userName ) {
        testAuthenticationUtils.runAsUser( userName, true );
    }

    /**
     * @see TestAuthenticationUtils#runAsUser(String, boolean)
     */
    protected void runAsUser( String userName, boolean createIfMissing ) {
        testAuthenticationUtils.runAsUser( userName, createIfMissing );
    }

    /**
     * @see TestAuthenticationUtils#runAsAnonymous()
     */
    protected void runAsAnonymous() {
        testAuthenticationUtils.runAsAnonymous();
    }

    /**
     * Change the number of elements created in collections (basically controls the size of test data sets).
     * This need not be called unless the test needs larger data sets. Call resetTestCollectionSize
     * after you are done.
     *
     * @param size size
     */
    protected void setTestCollectionSize( int size ) {
        testHelper.setTestElementCollectionSize( size );
    }
}