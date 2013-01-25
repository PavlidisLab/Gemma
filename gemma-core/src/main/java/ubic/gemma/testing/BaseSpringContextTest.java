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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.TestingAuthenticationProvider;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;

import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.Persister;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.CompassUtils;

/**
 * subclass for tests that need the container and use the database
 * 
 * @author pavlidis
 * @version $Id$
 */
@ContextConfiguration(locations = { "classpath*:ubic/gemma/testDataSource.xml",
        "classpath*:ubic/gemma/applicationContext-security.xml", "classpath*:ubic/gemma/applicationContext-search.xml",
        "classpath*:ubic/gemma/applicationContext-hibernate.xml",
        "classpath*:ubic/gemma/applicationContext-jms.xml",
        "classpath*:ubic/gemma/applicationContext-serviceBeans.xml",
        "classpath*:ubic/gemma/applicationContext-schedule.xml" })
public abstract class BaseSpringContextTest extends AbstractJUnit4SpringContextTests implements InitializingBean {

    protected static final int RANDOM_STRING_LENGTH = 10;
    protected static final int TEST_ELEMENT_COLLECTION_SIZE = 5;

    private static ArrayDesign readOnlyad = null;

    private static ExpressionExperiment readOnlyee = null;

    @Autowired
    protected ExternalDatabaseService externalDatabaseService;

    protected HibernateDaoSupport hibernateSupport = new HibernateDaoSupport() {
    };

    protected Log log = LogFactory.getLog( getClass() );

    @Autowired
    protected Persister persisterHelper;

    /**
     * The SimpleJdbcTemplate that this base class manages, available to subclasses. (Datasource; autowired at setteer)
     */
    protected SimpleJdbcTemplate simpleJdbcTemplate;

    @Autowired
    protected TaxonService taxonService;

    private AuthenticationTestingUtil authenticationTestingUtil;

    @Autowired
    protected PersistentDummyObjectHelper testHelper;

    private String sqlScriptEncoding;

    /**
     * @param commonName e.g. mouse,human,rat
     * @return
     */
    public Taxon getTaxon( String commonName ) {
        return this.taxonService.findByCommonName( commonName );
    }

    /**
     * @throws Exception
     */
    @Override
    final public void afterPropertiesSet() throws Exception {
        SecurityContextHolder.setStrategyName( SecurityContextHolder.MODE_INHERITABLETHREADLOCAL );
        hibernateSupport.setSessionFactory( this.getBean( SessionFactory.class ) );

        CompassUtils.deleteCompassLocks();

        this.authenticationTestingUtil = new AuthenticationTestingUtil();
        this.authenticationTestingUtil.setUserManager( this.getBean( UserManager.class ) );

        runAsAdmin();

    }

    /**
     * Run as a regular user.
     * 
     * @param userName
     */
    protected final void runAsUser( String userName ) {
        authenticationTestingUtil.switchToUser( this.applicationContext, userName );
    }

    /**
     * Elevate to administrative privileges (tests normally run this way, this can be used to set it back if you called
     * runAsUser). This gets called before each test, no need to run it yourself otherwise.
     */
    protected final void runAsAdmin() {
        authenticationTestingUtil.grantAdminAuthority( this.applicationContext );
    }

    /**
     * Set the DataSource, typically provided via Dependency Injection.
     */
    @Autowired
    public void setDataSource( DataSource dataSource ) {
        this.simpleJdbcTemplate = new SimpleJdbcTemplate( dataSource );
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( Persister persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * Specify the encoding for SQL scripts, if different from the platform encoding.
     * 
     * @see #executeSqlScript
     */
    public void setSqlScriptEncoding( String sqlScriptEncoding ) {
        this.sqlScriptEncoding = sqlScriptEncoding;
    }

    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * Convenience shortcut for RandomStringUtils.randomAlphabetic( 10 ) (or something similar to that)
     * 
     * @return
     */
    public String randomName() {
        return RandomStringUtils.randomAlphabetic( 10 );
    }

    protected void addTestAnalyses( ExpressionExperiment ee ) {
        testHelper.addTestAnalyses( ee );
    }

    /**
     * Count the rows in the given table.
     * 
     * @param tableName table name to count rows in
     * @return the number of rows in the table
     */
    protected int countRowsInTable( String tableName ) {
        return SimpleJdbcTestUtils.countRowsInTable( this.simpleJdbcTemplate, tableName );
    }

    /**
     * Convenience method for deleting all rows from the specified tables. Use with caution outside of a transaction!
     * 
     * @param names the names of the tables from which to delete
     * @return the total number of rows deleted from all specified tables
     */
    protected int deleteFromTables( String... names ) {
        return SimpleJdbcTestUtils.deleteFromTables( this.simpleJdbcTemplate, names );
    }

    /**
     * Execute the given SQL script. Use with caution outside of a transaction!
     * <p>
     * The script will normally be loaded by classpath. There should be one statement per line. Any semicolons will be
     * removed. <b>Do not use this method to execute DDL if you expect rollback.</b>
     * 
     * @param sqlResourcePath the Spring resource path for the SQL script
     * @param continueOnError whether or not to continue without throwing an exception in the event of an error
     * @throws DataAccessException if there is an error executing a statement and continueOnError was <code>false</code>
     */
    protected void executeSqlScript( String sqlResourcePath, boolean continueOnError ) throws DataAccessException {

        Resource resource = this.applicationContext.getResource( sqlResourcePath );
        SimpleJdbcTestUtils.executeSqlScript( this.simpleJdbcTemplate, new EncodedResource( resource,
                this.sqlScriptEncoding ), continueOnError );
    }

    /**
     * Convenience method to obtain instance of any bean by name. Use this only when necessary, you should wire your
     * tests by injection instead.
     * 
     * @param name
     * @return
     */
    protected <T> T getBean( String name, Class<T> t ) {
        try {
            return this.applicationContext.getBean( name, t );
        } catch ( BeansException e ) {
            throw new RuntimeException( e );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param t
     * @return
     */
    protected <T> T getBean( Class<T> t ) {
        return this.applicationContext.getBean( t );
    }

    /**
     * @return
     */
    protected Gene getTestPeristentGene() {
        return testHelper.getTestPeristentGene();
    }

    public Gene getTestPeristentGene( Taxon taxon ) {
        return testHelper.getTestPeristentGene( taxon );
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     * The ArrayDesign is provided with some CompositeSequenece DesignElements if desired. If composite seequences are
     * created, they are each associated with a single generated Reporter.
     * 
     * @param numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param randomNames If true, probe names will be random strings; otherwise they will be 0_at....N_at
     * @return
     */
    protected ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames ) {
        return testHelper.getTestPersistentArrayDesign( numCompositeSequences, randomNames, true );
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     * The ArrayDesign is provided with some CompositeSequenece DesignElements if desired. If composite seequences are
     * created, they are each associated with a single generated Reporter.
     * 
     * @param numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param randomNames If true, probe names will be random strings; otherwise they will be 0_at....N_at
     * @param doSequence add sequences to the array design that is created. Faster to avoid if you can.
     * @return
     */
    protected ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames,
            boolean doSequence, boolean readOnly ) {
        if ( readOnly ) {
            if ( readOnlyad == null ) {
                readOnlyad = testHelper.getTestPersistentArrayDesign( numCompositeSequences, randomNames, doSequence );
            }
            return readOnlyad;
        }
        return testHelper.getTestPersistentArrayDesign( numCompositeSequences, randomNames, doSequence );
    }

    /**
     * @return EE with no data; just bioassays, biomaterials, quantitation types and (minimal) array designs.
     */
    protected ExpressionExperiment getTestPersistentBasicExpressionExperiment() {
        return testHelper.getTestPersistentBasicExpressionExperiment();
    }

    protected BibliographicReference getTestPersistentBibliographicReference( String accession ) {
        return testHelper.getTestPersistentBibliographicReference( accession );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * 
     * @return
     */
    protected BioAssay getTestPersistentBioAssay( ArrayDesign ad ) {
        return testHelper.getTestPersistentBioAssay( ad );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * 
     * @return
     */
    protected BioAssay getTestPersistentBioAssay( ArrayDesign ad, BioMaterial bm ) {
        return testHelper.getTestPersistentBioAssay( ad, bm );
    }

    /**
     * @return
     */
    protected BioMaterial getTestPersistentBioMaterial() {
        return testHelper.getTestPersistentBioMaterial();
    }

    /**
     * @return
     */
    protected BioMaterial getTestPersistentBioMaterial( Taxon tax ) {
        return testHelper.getTestPersistentBioMaterial( tax );
    }

    /**
     * @return
     */
    protected BioSequence getTestPersistentBioSequence() {
        return testHelper.getTestPersistentBioSequence();
    }

    /**
     * @param querySequence
     * @return
     */
    protected BlatResult getTestPersistentBlatResult( BioSequence querySequence ) {
        return testHelper.getTestPersistentBlatResult( querySequence );
    }

    /**
     * Convenience method to get a (fairly) complete randomly generated persisted expression experiment.
     * 
     * @param readOnly If the test only needs to read, a new data set might not be created.
     * @return
     */
    protected ExpressionExperiment getTestPersistentCompleteExpressionExperiment( boolean readOnly ) {
        if ( readOnly ) {
            if ( readOnlyee == null ) {
                log.info( "Initializing test expression experimement (one-time for read-only tests)" );
                readOnlyee = testHelper.getTestExpressionExperimentWithAllDependencies();
            }
            return readOnlyee;
        }
        ExpressionExperiment ee = testHelper.getTestExpressionExperimentWithAllDependencies();

        return ee;
    }

    /**
     * Convenience method to get a (fairly) complete randomly generated persisted expression experiment.
     * 
     * @param doSequence Should the Arraydesign sequence information be filled in? (slower)
     * @return
     */
    protected ExpressionExperiment getTestPersistentCompleteExpressionExperimentWithSequences() {
        return testHelper.getTestExpressionExperimentWithAllDependencies( true );
    }

    /**
     * Convenience method to provide a Contact that can be used to fill non-nullable associations in test objects.
     * 
     * @return
     */
    protected Contact getTestPersistentContact() {
        return testHelper.getTestPersistentContact();
    }

    /**
     * Change the number of elements created in collections (basically controls the size of test data sets). This
     * needn't be called unless the test needs larger data sets. FCall {@link resetTestCollectionSize} after you are
     * done.
     * 
     * @param size
     */
    protected void setTestCollectionSize( int size ) {
        testHelper.setTestElementCollectionSize( size );
    }

    /**
     * Restore to default.
     */
    protected void resetTestCollectionSize() {
        testHelper.resetTestElementCollectionSize();
    }

    /**
     * Get a database entry from a fictitious database.
     * 
     * @return
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry() {
        return getTestPersistentDatabaseEntry( null, RandomStringUtils.randomAlphabetic( 10 ) );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     * 
     * @return
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry( ExternalDatabase ed ) {
        return getTestPersistentDatabaseEntry( null, ed );
    }

    protected DatabaseEntry getTestPersistentDatabaseEntry( String ed ) {
        return getTestPersistentDatabaseEntry( null, ed );
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     * 
     * @return
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
     * @return
     */
    protected ExpressionExperiment getTestPersistentExpressionExperiment() {
        return testHelper.getTestPersistentExpressionExperiment();
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object.
     * 
     * @return
     */
    protected ExpressionExperiment getTestPersistentExpressionExperiment( Collection<BioAssay> bioAssays ) {
        return testHelper.getTestPersistentExpressionExperiment( bioAssays );
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects. This implementation does NOT fill in associations of the created object except for the creation of
     * persistent BioMaterials and BioAssays so that database taxon lookups for this experiment will work.
     * 
     * @return
     */
    protected ExpressionExperiment getTestPersistentExpressionExperiment( Taxon taxon ) {
        return testHelper.getTestPersistentExpressionExperiment( taxon );
    }

    /**
     * @param gene
     * @return
     */
    protected GeneProduct getTestPersistentGeneProduct( Gene gene ) {
        return testHelper.getTestPersistentGeneProduct( gene );
    }

    /**
     * Convenience method to provide a QuantitationType that can be used to fill non-nullable associations in test
     * objects.
     * 
     * @return
     */
    protected QuantitationType getTestPersistentQuantitationType() {
        return testHelper.getTestPersistentQuantitationType();
    }

}

final class AuthenticationTestingUtil {

    /**
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager userManager ) {
        this.userManager = userManager;
    }

    private UserManager userManager;

    /**
     * Grant authority to a test user, with admin privileges, and put the token in the context. This means your tests
     * will be authorized to do anything an administrator would be able to do.
     */
    protected void grantAdminAuthority( ApplicationContext ctx ) {
        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "administrator", "administrator",
                new GrantedAuthority[] { new GrantedAuthorityImpl( "GROUP_ADMIN" ) } );

        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    /**
     * Grant authority to a test user, with regular user privileges, and put the token in the context. This means your
     * tests will be authorized to do anything that user could do
     */
    protected void switchToUser( ApplicationContext ctx, String username ) {

        UserDetails user = userManager.loadUserByUsername( username );

        List<GrantedAuthority> authrs = new ArrayList<GrantedAuthority>( user.getAuthorities() );

        ProviderManager providerManager = ( ProviderManager ) ctx.getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        TestingAuthenticationToken token = new TestingAuthenticationToken( username, "testing", authrs );
        token.setAuthenticated( true );

        putTokenInContext( token );
    }

    /**
     * @param token
     */
    private static void putTokenInContext( AbstractAuthenticationToken token ) {
        SecurityContextHolder.getContext().setAuthentication( token );
    }
}
