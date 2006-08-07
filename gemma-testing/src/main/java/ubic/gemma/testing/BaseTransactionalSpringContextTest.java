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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.context.SecurityContextImpl;
import org.acegisecurity.providers.ProviderManager;
import org.acegisecurity.providers.TestingAuthenticationProvider;
import org.acegisecurity.providers.TestingAuthenticationToken;
import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.FlushMode;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.AbstractTransactionalSpringContextTests;
import org.springframework.web.context.support.XmlWebApplicationContext;

import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactDao;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryDao;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeDao;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayDao;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceDao;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.model.genome.TaxonDao;
import uk.ltd.getahead.dwr.create.SpringCreator;

/**
 * Override this test class for tests that need the container and use the database.
 * <p>
 * In your test class, define any dependencies you have on beans "as usual" for setter injection - by defining the field
 * and making a public setter. This base class also provides a PersisterHelper, which is often needed by tests.
 * <p>
 * If your setup requires putting data in the database, override onSetUpInTransaction() instead of putting such code in
 * setUp(). Be sure to call super.onSetUpInTransaction() to make the authority available to your subclass.
 * <p>
 * Any changes you make to the database will be undone at the end of the test, so no cleanup code is needed. If you
 * don't want this behavior, call setComplete() in your test.
 * <p>
 * Be careful! Running long tasks in transactions can trigger odd behavior. Use this for tests that run quickly.
 * 
 * @see org.springframework.test.AbstractTransactionalSpringContextTests
 * @author pavlidis
 * @version $Id$
 */
abstract public class BaseTransactionalSpringContextTest extends AbstractTransactionalSpringContextTests {

    protected static final int RANDOM_STRING_LENGTH = 10;
    protected CompositeConfiguration config;
    protected ResourceBundle resourceBundle;
    protected Log log = LogFactory.getLog( getClass() );
    protected static final int TEST_ELEMENT_COLLECTION_SIZE = 20;

    protected ExternalDatabaseDao externalDatabaseDao;

    protected DatabaseEntryDao databaseEntryDao;

    protected ContactDao contactDao;

    protected TaxonDao taxonDao;

    protected ArrayDesignDao arrayDesignDao;

    protected QuantitationTypeDao quantitationTypeDao;

    protected ExpressionExperimentDao expressionExperimentDao;

    protected CompositeSequenceDao compositeSequenceDao;

    protected BioAssayDao bioAssayDao;

    private boolean testEnvDisabled = false;

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * 
     * @return
     */
    protected BioAssay getTestPersistentBioAssay( ArrayDesign ad ) {
        BioAssay ba = ubic.gemma.model.expression.bioAssay.BioAssay.Factory.newInstance();
        ba.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        ba = ( BioAssay ) bioAssayDao.create( ba );
        if ( ad != null ) ba.getArrayDesignsUsed().add( ad );
        flushSession();
        return ba;
    }

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     * 
     * @return
     */
    protected DatabaseEntry getTestPersistentDatabaseEntry() {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        /* set the accession of database entry to the pubmed id. */
        result.setAccession( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );

        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_testdb" );

        ed = ( ExternalDatabase ) externalDatabaseDao.create( ed );

        result.setExternalDatabase( ed );
        result = databaseEntryDao.create( result );
        flushSession();
        return result;
    }

    /**
     * Convenience method to provide a Contact that can be used to fill non-nullable associations in test objects.
     * 
     * @return
     */
    protected Contact getTestPersistentContact() {
        Contact c = Contact.Factory.newInstance();
        c.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        c = ( Contact ) contactDao.create( c );
        flushSession();
        return c;
    }

    /**
     * Convenience method to provide a QuantitationType that can be used to fill non-nullable associations in test
     * objects.
     * 
     * @return
     */
    protected QuantitationType getTestPersistentQuantitationType() {
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        qt.setIsBackground( false );
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.MEASUREDSIGNAL );
        qt.setScale( ScaleType.LINEAR );
        qt = ( QuantitationType ) quantitationTypeDao.create( qt );
        flushSession();
        return qt;
    }

    /**
     * Convenience method to provide an ExpressionExperiment that can be used to fill non-nullable associations in test
     * objects.
     * 
     * @return
     */
    protected ExpressionExperiment getTestPersistentExpressionExperiment() {
        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
        ee = ( ExpressionExperiment ) expressionExperimentDao.create( ee );
        flushSession();
        return ee;
    }

    /**
     * Convenience method to provide an ArrayDesign that can be used to fill non-nullable associations in test objects.
     * The ArrayDesign is provided with some CompositeSequenece DesignElements if desired.
     * 
     * @param numCompositeSequences The number of CompositeSequences to populate the ArrayDesign with.
     * @param randomNames If true, probe names will be random strings; otherwise they will be 0_at....N_at
     * @return
     */
    protected ArrayDesign getTestPersistentArrayDesign( int numCompositeSequences, boolean randomNames ) {
        ArrayDesign ad = ArrayDesign.Factory.newInstance();

        for ( int i = 0; i < numCompositeSequences; i++ ) {
            CompositeSequence de = CompositeSequence.Factory.newInstance();
            if ( randomNames ) {
                de.setName( RandomStringUtils.randomNumeric( RANDOM_STRING_LENGTH ) + "_test" );
            } else {
                de.setName( i + "_at" );
            }
            de = ( CompositeSequence ) compositeSequenceDao.create( de );
            ad.getCompositeSequences().add( de );
        }

        ad = ( ArrayDesign ) arrayDesignDao.create( ad );

        for ( CompositeSequence cs : ad.getCompositeSequences() ) {
            cs.setArrayDesign( ad );
        }

        flushSession();
        return ad;
    }

    /**
     * Force the hibernate session to flush.
     */
    public void flushSession() {
        try {
            ( ( SessionFactory ) this.getBean( "sessionFactory" ) ).getCurrentSession().flush();
        } catch ( HibernateException e ) {
            throw new RuntimeException( "While trying to flush the session", e );
        }
    }

    /**
     * Force the hibernate session to flush and clear.
     */
    public void flushAndClearSession() {
        try {
            flushSession();
            ( ( SessionFactory ) this.getBean( "sessionFactory" ) ).getCurrentSession().clear();
        } catch ( HibernateException e ) {
            throw new RuntimeException( "While trying to flush the session", e );
        }
    }

    /**
     * 
     * 
     */
    public BaseTransactionalSpringContextTest() {
        super();

        setAutowireMode( AutowireCapableBeanFactory.AUTOWIRE_BY_NAME );

        // Since a ResourceBundle is not required for each class, just
        // do a simple check to see if one exists
        String className = this.getClass().getName();

        try {
            config = new CompositeConfiguration();
            config.addConfiguration( new SystemConfiguration() );
            config.addConfiguration( new PropertiesConfiguration( "build.properties" ) );
            resourceBundle = ResourceBundle.getBundle( className ); // will look for <className>.properties
        } catch ( MissingResourceException mre ) {
            // log.warn("No resource bundle found for: " + className);
        } catch ( ConfigurationException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * Convenience method to obtain instance of any bean by name. Use this only when necessary, you should wire your
     * tests by injection instead.
     * 
     * @param name
     * @return
     */
    protected Object getBean( String name ) {
        if ( isTestEnvDisabled() ) return getContext( getStandardLocations() ).getBean( name );
        return getContext( getConfigLocations() ).getBean( name );
    }

    /**
     * Call this method in setUpDuringTransaction to grant permissions to your test.
     */
    @SuppressWarnings("unchecked")
    private void grantAuthority() {

        ProviderManager providerManager = ( ProviderManager ) getBean( "authenticationManager" );
        providerManager.getProviders().add( new TestingAuthenticationProvider() );

        // Grant all roles to test user.
        TestingAuthenticationToken token = new TestingAuthenticationToken( "pavlab", "pavlab", new GrantedAuthority[] {
                new GrantedAuthorityImpl( "user" ), new GrantedAuthorityImpl( "admin" ) } );

        // Create and store the Acegi SecureContext into the ContextHolder.
        SecurityContextImpl secureContext = new SecurityContextImpl();
        secureContext.setAuthentication( token );
        SecurityContextHolder.setContext( secureContext );
    }

    /**
     * Returns config locations needed for test environment.
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] { "classpath*:/ubic/gemma/localTestDataSource.xml",
                "classpath*:/ubic/gemma/applicationContext-*.xml", "*-servlet.xml" };
    }

    /**
     * Use these locations when overriding the test config locations.
     * 
     * @return
     */
    private Object getStandardLocations() {
        // ResourceBundle db = ResourceBundle.getBundle( "Gemma" );
        // String daoType = db.getString( "dao.type" );
        // String servletContext = db.getString( "servlet.name.0" );
        return new String[] { "classpath:/ubic/gemma/localDataSource.xml",
                "classpath*:/ubic/gemma/applicationContext-*.xml", "*-servlet.xml" };
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
        grantAuthority();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractTransactionalSpringContextTests#onTearDownInTransaction()
     */
    @Override
    protected void onTearDownInTransaction() throws Exception {
        super.onTearDownInTransaction();
        // flushSession();
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
     * Option to override use of the test environment.
     * 
     * @param disableTestEnv
     */
    public void setDisableTestEnv( boolean testEnvDisabled ) {
        this.testEnvDisabled = testEnvDisabled;
    }

    /**
     * @param databaseEntryDao The databaseEntryDao to set.
     */
    public void setDatabaseEntryDao( DatabaseEntryDao databaseEntryDao ) {
        this.databaseEntryDao = databaseEntryDao;
    }

    /**
     * @param externalDatabaseDao The externalDatabaseDao to set.
     */
    public void setExternalDatabaseDao( ExternalDatabaseDao externalDatabaseDao ) {
        this.externalDatabaseDao = externalDatabaseDao;
    }

    /**
     * @param contactDao The contactDao to set.
     */
    public void setContactDao( ContactDao contactDao ) {
        this.contactDao = contactDao;
    }

    /**
     * @param taxonDao The taxonDao to set.
     */
    public void setTaxonDao( TaxonDao taxonDao ) {
        this.taxonDao = taxonDao;
    }

    /**
     * @return
     */
    public boolean isTestEnvDisabled() {
        return testEnvDisabled;
    }

    public void setArrayDesignDao( ArrayDesignDao arrayDesignDao ) {
        this.arrayDesignDao = arrayDesignDao;
    }

    public void setExpressionExperimentDao( ExpressionExperimentDao expressionExperimentDao ) {
        this.expressionExperimentDao = expressionExperimentDao;
    }

    public void setQuantitationTypeDao( QuantitationTypeDao quantitationTypeDao ) {
        this.quantitationTypeDao = quantitationTypeDao;
    }

    public void setCompositeSequenceDao( CompositeSequenceDao compositeSequenceDao ) {
        this.compositeSequenceDao = compositeSequenceDao;
    }

    public void setBioAssayDao( BioAssayDao bioAssayDao ) {
        this.bioAssayDao = bioAssayDao;
    }
}
