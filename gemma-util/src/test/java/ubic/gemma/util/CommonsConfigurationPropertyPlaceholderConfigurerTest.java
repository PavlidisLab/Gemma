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
package ubic.gemma.util;

import junit.framework.TestCase;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import ubic.basecode.util.FileTools;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CommonsConfigurationPropertyPlaceholderConfigurerTest extends TestCase {

    ConfigurableListableBeanFactory bf;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CompositeConfiguration config = new CompositeConfiguration();
        config.addConfiguration( new SystemConfiguration() );
        config.addConfiguration( new PropertiesConfiguration( "Gemma.properties" ) );

        // InputStream doesn't work for maven...get 'don't read twice' error.
        Resource testConfig = new FileSystemResource( FileTools.resourceToPath( "/test.spring.config.xml" ) );

        bf = new XmlBeanFactory( testConfig );
    }

    /**
     * Test method for {@link ubic.gemma.util.CommonsConfigurationPropertyPlaceholderConfigurer#mergeProperties()}.
     */
    public final void testMergeProperties() throws Exception {

        PropertiesConfiguration pc = ( PropertiesConfiguration ) bf.getBean( "buildProperties" );

        assertEquals( "foo", pc.getProperty( "testProperty" ) );

        CommonsConfigurationPropertyPlaceholderConfigurer cc = ( CommonsConfigurationPropertyPlaceholderConfigurer ) bf
                .getBean( "configurationPropertyConfigurer" );

        assert cc != null;
        cc.postProcessBeanFactory( bf ); // when does this normally get called? Maybe an XmlWebApplicationContext
        // thing.

        JustATestBean o = ( JustATestBean ) bf.getBean( "testConfigured" );
        assertEquals( "foo", o.getMyValue() );

    }
}
