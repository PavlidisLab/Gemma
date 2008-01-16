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

import java.util.ResourceBundle;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.test.AbstractTransactionalSpringContextTests;
import org.springframework.web.context.support.XmlWebApplicationContext;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.User;
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
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.BlatResult;
import ubic.gemma.persistence.PersisterHelper;
import ubic.gemma.util.CompassUtils;
import ubic.gemma.util.ConfigUtils;
import ubic.gemma.util.SpringContextUtil;
import org.directwebremoting.spring.SpringCreator;

/**
 * Override this test class for tests that need the container and use the database and you want to leave the database
 * unchanged. Note that if you call flush in your transaction the database will still be modified.
 * <p>
 * In your test class, define any dependencies you have on beans "as usual" for setter injection - by defining the field
 * and making a public setter. This base class also provides a PersisterHelper, which is often needed by tests.
 * <p>
 * If your setup requires putting data in the database, override onSetUpInTransaction() instead of putting such code in
 * setUp(). Be sure to call super.onSetUpInTransaction() to make the authority available to your subclass.
 * <p>
 * Any changes you make to the database will be undone at the end of the test, so no cleanup code is needed. If you
 * don't want this behavior, call setComplete() in your test.
 * 
 * @see org.springframework.test.AbstractTransactionalSpringContextTests
 * @author pavlidis
 * @version $Id$
 */
abstract public class BaseSpringContextTest extends AbstractTransactionalSpringContextTests {

    protected static final int RANDOM_STRING_LENGTH = 10;
    protected static final int TEST_ELEMENT_COLLECTION_SIZE = 5;

    private boolean testEnvDisabled;
    protected ResourceBundle resourceBundle;

    protected Log log = LogFactory.getLog( getClass() );

    protected TestPersistentObjectHelper testHelper;
    protected PersisterHelper persisterHelper;

    HibernateDaoSupport hibernateSupport = new HibernateDaoSupport() {
    };

    private ConfigurableApplicationContext context;
    private static ExpressionExperiment readOnlyee = null;

    /**
     * 
     * 
     */
    public BaseSpringContextTest() {
        super();
        setAutowireMode( AutowireCapableBeanFactory.AUTOWIRE_BY_NAME );
    }

    /**
     * Force the hibernate session to flush and clear.
     */
    public void flushAndClearSession() {
        flushSession();
        hibernateSupport.getHibernateTemplate().clear();
    }

    public void deleteSingleObject( Object entity ) {
        hibernateSupport.getHibernateTemplate().delete( entity );
    }

    public void thawSingleObject( Object entity ) {
        hibernateSupport.getHibernateTemplate().update( entity );
    }

    /**
     * Force the hibernate session to flush.
     */
    public void flushSession() {
        hibernateSupport.getHibernateTemplate().flush();
    }

    /**
     * @return
     */
    public boolean isTestEnvDisabled() {
        return testEnvDisabled;
    }

    /**
     * Option to override use of the test environment.
     * 
     * @param disableTestEnv
     */
    public void setDisableTestEnv( boolean testEnvDisabled ) {
        this.testEnvDisabled = testEnvDisabled;
    }

    /**
     * @param persisterHelper the persisterHelper to set
     */
    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * Use these locations when overriding the test config locations.
     * 
     * @return
     */
    private Object getStandardLocations() {
        return SpringContextUtil.getConfigLocations( false, false, false, this.isWebapp() );
    }

    /**
     * Guess if this is a test that needs the action-servlet.xml
     * <p>
     * Implementation note: this words on the assumption the class under test in in the ubic.gemma.web package
     * hierarchy.
     * 
     * @return
     */
    private boolean isWebapp() {
        return this.getClass().getPackage().getName().contains( ".web." );
    }

    /**
     * Convenience method to obtain instance of any bean by name. Use this only when necessary, you should wire your
     * tests by injection instead.
     * 
     * @param name
     * @return
     */
    protected Object getBean( String name ) {
        try {
            if ( isTestEnvDisabled() ) return getContext( getStandardLocations() ).getBean( name );
            return getContext( getConfigLocations() ).getBean( name );
        } catch ( BeansException e ) {
            throw new RuntimeException( e );
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected String[] getConfigLocations() {
        return SpringContextUtil.getConfigLocations( true, true, false, this.isWebapp() );
    }

    /**
     * @return
     */
    protected Gene getTestPeristentGene() {
        return testHelper.getTestPeristentGene();
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
            boolean doSequence ) {
        return testHelper.getTestPersistentArrayDesign( numCompositeSequences, randomNames, doSequence );
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
     * @return
     */
    protected BioMaterial getTestPersistentBioMaterial() {
        return testHelper.getTestPersistentBioMaterial();
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
        return testHelper.getTestExpressionExperimentWithAllDependencies();
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

    protected User getTestPersistentUser() {
        return getTestPersistentUser( RandomStringUtils.randomAlphabetic( 6 ), ConfigUtils
                .getString( "gemma.admin.password" ) );
    }

    /**
     * @return
     */
    protected User getTestPersistentUser( String username, String password ) {
        return testHelper.getTestPersistentUser( username, password );

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#loadContextLocations(java.lang.String[])
     */
    @Override
    protected ConfigurableApplicationContext loadContextLocations( String[] locations ) {

        if ( log.isDebugEnabled() ) {
            for ( int i = 0; i < locations.length; i++ ) {
                String string = locations[i];
                log.debug( "Location: " + string );
            }
        }

        ConfigurableApplicationContext ctx = new XmlWebApplicationContext();

        /*
         * Needed for DWR support only. When running in a web container this is taken care of by
         * org.springframework.web.context.ContextLoaderListener
         */
        SpringCreator.setOverrideBeanFactory( ctx );

        ( ( XmlWebApplicationContext ) ctx ).setConfigLocations( locations );
        ( ( XmlWebApplicationContext ) ctx ).setServletContext( new MockServletContext( "" ) );
        ( ( XmlWebApplicationContext ) ctx ).refresh();

        this.context = ctx;

        return ctx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onSetUpInTransaction()
     */
    @Override
    protected void onSetUpInTransaction() throws Exception {
        super.onSetUpInTransaction();

        hibernateSupport.setSessionFactory( ( SessionFactory ) this.getBean( "sessionFactory" ) );
        CompassUtils.deleteCompassLocks();
        SpringTestUtil.grantAuthority( this.getContext( this.getConfigLocations() ) );
        this.testHelper = new TestPersistentObjectHelper();

        ExternalDatabaseService externalDatabaseService = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
        persisterHelper = ( PersisterHelper ) getBean( "persisterHelper" ); // beans not injected yet, have to do
        // explicitly?
        testHelper.setPersisterHelper( persisterHelper );
        testHelper.setExternalDatabaseService( externalDatabaseService );
    }

    /**
     * The test user does not have admin privileges.
     * 
     * @param username - Allows you to create different users, each with user (not admin privileges).
     * @throws Exception
     */
    protected void onSetUpInTransactionGrantingUserAuthority( String username ) throws Exception {
        super.onSetUpInTransaction();
        hibernateSupport.setSessionFactory( ( SessionFactory ) this.getBean( "sessionFactory" ) );
        CompassUtils.deleteCompassLocks();
        SpringTestUtil.grantUserAuthority( this.getContext( this.getConfigLocations() ), username );
        this.testHelper = new TestPersistentObjectHelper();

        ExternalDatabaseService externalDatabaseService = ( ExternalDatabaseService ) getBean( "externalDatabaseService" );
        persisterHelper = ( PersisterHelper ) getBean( "persisterHelper" ); // beans not injected yet, have to do
        // explicitly?
        testHelper.setPersisterHelper( persisterHelper );
        testHelper.setExternalDatabaseService( externalDatabaseService );
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onTearDownInTransaction()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
    }

    /**
     * Call this method near the start of your test to avoid "Stale data" errors ("Cannot synchronize session with
     * persistent store..."). FlushMode.COMMIT means that the cache will never be flushed before queries, only at the
     * end of the transaction. If you are getting errors when "find" methods are used, this could help.
     * <p>
     * It can also improve performance during tests, at the possible cost of a big hit at the end.
     * <p>
     * Use this carefully -- if your test is going to 'find' data that hasn't been flushed yet, you'll have problems.
     */
    protected void setFlushModeCommit() {
        ( ( SessionFactory ) this.getBean( "sessionFactory" ) ).getCurrentSession().setFlushMode( FlushMode.COMMIT );
    }

    /**
     * @return the context
     */
    public ConfigurableApplicationContext getContext() {
        return this.context;
    }

}