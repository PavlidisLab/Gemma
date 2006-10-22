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
package ubic.gemma.web.listener;

import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.mock.web.MockServletContext;
import org.springframework.util.ClassUtils;
import org.springframework.web.context.ContextLoader;
import org.springframework.web.context.WebApplicationContext;

import ubic.gemma.Constants;

/**
 * This class tests the StartupListener class to verify that variables are placed into the application context.
 * 
 * @author keshav
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @version $Id$
 */
public class StartupListenerTest extends TestCase {
    private Log log = LogFactory.getLog( this.getClass() );
    private MockServletContext sc = null;
    private ServletContextListener listener = null;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        listener = new StartupListener();

        sc = new MockServletContext( "" );
        sc.addInitParameter( "theme", "simplicity" );

        // initialize Spring
        String pkg = ClassUtils.classPackageAsResourcePath( Constants.class );
        sc.addInitParameter( ContextLoader.CONFIG_LOCATION_PARAM, "classpath*:/" + pkg + "/applicationContext-*.xml,"
                + "classpath*:/" + pkg + "/localTestDataSource.xml" + ",classpath*:/" + pkg + "/compasstest.xml" );
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        listener = null;
        sc = null;
    }

    public void testContextInitialized() {
        ServletContextEvent event = new ServletContextEvent( sc );
        listener.contextInitialized( event );

        log.warn( sc.getAttribute( Constants.CONFIG ) );
        assertTrue( sc.getAttribute( Constants.CONFIG ) != null );
        Map config = ( Map ) sc.getAttribute( Constants.CONFIG );
        assertNotNull( config.get( "theme" ) );

        log.warn( sc.getAttribute( WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE ) );
        assertTrue( sc.getAttribute( WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE ) != null );

        log.warn( sc.getAttribute( Constants.AVAILABLE_ROLES ) );
        assertTrue( sc.getAttribute( Constants.AVAILABLE_ROLES ) != null );
    }
}
