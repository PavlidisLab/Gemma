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

import uk.ltd.getahead.dwr.create.SpringCreator;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.ContactDao;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.DatabaseEntryDao;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabaseDao;

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
 * 
 * @see org.springframework.test.AbstractTransactionalSpringContextTests
 * @author pavlidis
 * @version $Id$
 */
abstract public class BaseTransactionalSpringContextTest extends AbstractTransactionalSpringContextTests {

    protected CompositeConfiguration config;

    protected ResourceBundle resourceBundle;
    protected Log log = LogFactory.getLog( getClass() );

    private ExternalDatabaseDao externalDatabaseDao;

    private DatabaseEntryDao databaseEntryDao;

    private ContactDao contactDao;

    /**
     * Convenience method to provide a DatabaseEntry that can be used to fill non-nullable associations in test objects.
     * The accession and ExternalDatabase name are set to random strings.
     * 
     * @return
     */
    public DatabaseEntry getTestPersistentDatabaseEntry() {
        DatabaseEntry result = DatabaseEntry.Factory.newInstance();

        /* set the accession of database entry to the pubmed id. */
        result.setAccession( RandomStringUtils.random( 10 ) + "_test" );

        ExternalDatabase ed = ExternalDatabase.Factory.newInstance();
        ed.setName( RandomStringUtils.random( 10 ) + "_testdb" );

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
    public Contact getTestPersistentContact() {
        Contact c = Contact.Factory.newInstance();
        c.setName( RandomStringUtils.random( 10 ) + "_test" );
        c = ( Contact ) contactDao.create( c );
        flushSession();
        return c;
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
     * Force the hibernate session to flush.
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

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
     */
    @Override
    protected String[] getConfigLocations() {
        return new String[] { "classpath:/localTestDataSource.xml", "classpath*:/ubic/gemma/applicationContext-*.xml",
                "*-servlet.xml" };
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#loadContextLocations(java.lang.String[])
     */
    @Override
    protected ConfigurableApplicationContext loadContextLocations( String[] locations ) {
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
     * persistent store..."). FlushMode.COMMIT means that the cache will never be flush before queries, only at the end
     * of the transaction. If you are getting errors when "find" methods are used, this could help.
     */
    protected void setFlushModeCommit() {
        ( ( SessionFactory ) this.getBean( "sessionFactory" ) ).getCurrentSession().setFlushMode( FlushMode.COMMIT );
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
}
