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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.web.context.support.XmlWebApplicationContext;

import ubic.gemma.util.SpringContextUtil;
import uk.ltd.getahead.dwr.create.SpringCreator;

/**
 * Tests that subclass this do not run in a transaction and can leave the database modified.
 * 
 * @author pavlidis
 * @version $Id$
 */
abstract public class BaseSpringContextTest extends AbstractDependencyInjectionSpringContextTests {

    protected ResourceBundle resourceBundle;
    protected Log log = LogFactory.getLog( getClass() );

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#onSetUp()
     */
    @Override
    protected void onSetUp() throws Exception {
        super.onSetUp();
        SpringTestUtil.grantAuthority( this.getContext( this.getConfigLocations() ) );
    }

    /**
     * 
     *
     */
    public BaseSpringContextTest() {
        super();

        setAutowireMode( AutowireCapableBeanFactory.AUTOWIRE_BY_NAME );

        String className = this.getClass().getName();

        try {

            resourceBundle = ResourceBundle.getBundle( className ); // will look for <className>.properties
        } catch ( MissingResourceException mre ) {
            // log.warn("No resource bundle found for: " + className);
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
     * Returns config locations needed for test environment.
     * <p>
     * FIXME this duplicates some code in the SpringContextUtil.
     * 
     * @see org.springframework.test.AbstractDependencyInjectionSpringContextTests#getConfigLocations()
     */
    @Override
    protected String[] getConfigLocations() {
        return SpringContextUtil.getConfigLocations( true, this.isWebapp() );
    }

    /**
     * Guess if this is a test that needs the action-servlet.xml
     * 
     * @return
     */
    private boolean isWebapp() {
        boolean result = this.getClass().getPackage().getName().contains( ".web." );
        if ( result ) {
            log.info( this.getClass().getPackage().getName() + " needs action-servlet.xml" );
        }
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.Abstra
     *      ctDependencyInjectionSpringContextTests#loadContextLocations(java.lang.String[])
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

}
