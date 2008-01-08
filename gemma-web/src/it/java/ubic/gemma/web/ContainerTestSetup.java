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
package ubic.gemma.web;

import java.io.File;
import java.net.URL;

import junit.extensions.TestSetup;
import junit.framework.Test;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.installer.Installer;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.generic.DefaultContainerFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import ubic.gemma.util.ConfigUtils;

/**
 * Base TestSetup for running tests that need a Tomcat 5.x web container, using Cargo (see
 * {@link http://cargo.codehaus.org/Functional+testing}). If you are using maven to start the container, you don't need
 * to use this class.
 * <p>
 * To use with jWebUnit see {@link http://jwebunit.sourceforge.net/quickstart.html}).
 * 
 * @author pavlidis
 * @version $Id$
 * @deprecated Start the container manually or with maven instead.
 */
public final class ContainerTestSetup extends TestSetup {

    private static Log log = LogFactory.getLog( ContainerTestSetup.class.getName() );
    protected LocalConfiguration configuration;
    protected InstalledLocalContainer container;

    /**
     * @return the configuration
     */
    public LocalConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * @return the container
     */
    public InstalledLocalContainer getContainer() {
        return this.container;
    }

    public ContainerTestSetup( Test test ) {
        super( test );
    }

    @Override
    protected void setUp() throws Exception {
        // Optional step to install the container from a URL pointing to its distribution
        log.info( "Installing Tomcat (if necessary)" );
        Installer installer = new ZipURLInstaller( new URL(
                "http://www.apache.org/dist/tomcat/tomcat-5/v5.5.17/bin/apache-tomcat-5.5.17.zip" ) );
        installer.install();
        assert installer.getHome() != null;
        log.info( "Installation is in " + installer.getHome() );

        // Create the Cargo Container instance wrapping our physical container
        log.info( "Creating container..." );
        configuration = ( LocalConfiguration ) new DefaultConfigurationFactory().createConfiguration( "tomcat5x",
                ConfigurationType.STANDALONE );
        container = ( InstalledLocalContainer ) new DefaultContainerFactory().createContainer( "tomcat5x",
                ContainerType.INSTALLED, configuration );
        container.setHome( installer.getHome() );

        if ( log.isDebugEnabled() ) {
            for ( Object o : container.getConfiguration().getProperties().keySet() ) {
                log.debug( o + "=" + container.getConfiguration().getProperties().get( o ) );
            }
        }

        log.info( "deploying" );
        File war = new File( ConfigUtils.getString( "gemma.home" ) + "/gemma-web/target/Gemma.war" );
        assert ( war.canRead() );
        WAR gemmaWar = new WAR( war.getAbsolutePath() );

        configuration.addDeployable( gemmaWar );
        assert configuration.getDeployables().size() == 1;

        log.info( "Starting container..." );
        container.start();
        log.info( "Container started!" );

        // Thread.sleep( 1000000 );

    }

    @Override
    protected void tearDown() throws Exception {
        log.info( "Stopping container..." );
        container.stop();
        log.info( "Container stopped" );
    }

}
