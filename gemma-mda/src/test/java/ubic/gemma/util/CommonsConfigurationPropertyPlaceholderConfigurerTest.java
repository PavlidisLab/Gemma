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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.commons.configuration.CompositeConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.configuration.SystemConfiguration;
import org.apache.commons.configuration.io.FileHandler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author pavlidis
 * @version $Id$
 */
public class CommonsConfigurationPropertyPlaceholderConfigurerTest {

    ConfigurableListableBeanFactory bf;
    ClassPathXmlApplicationContext ap;

    /*
     * (non-Javadoc)
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Before
    public void setUp() throws Exception {
        CompositeConfiguration config = new CompositeConfiguration();
        config.addConfiguration( new SystemConfiguration() );
        PropertiesConfiguration pc = new PropertiesConfiguration();
        FileHandler handler = new FileHandler( pc );
        handler.setFileName( "Gemma.properties" );
        handler.load();
        config.addConfiguration( pc );

        ap = new ClassPathXmlApplicationContext( "classpath:/test.spring.config.xml" );
        bf = ap.getBeanFactory();

    }

    @After
    public void tearDown() {
        ap.close();
    }

    /**
     * Test method for {@link ubic.gemma.util.CommonsConfigurationPropertyPlaceholderConfigurer#mergeProperties()}.
     */
    @Test
    public final void testMergeProperties() throws Exception {
        FileHandler bpfh = ( FileHandler ) bf.getBean( "buildPropertiesFileHandler" );
        assertNotNull( bpfh );

        PropertiesConfiguration pc = ( PropertiesConfiguration ) bf.getBean( "buildProperties" );

        assertEquals( "foo", pc.getProperty( "testProperty" ) );

        CommonsConfigurationPropertyPlaceholderConfigurer cc = bf
                .getBean( CommonsConfigurationPropertyPlaceholderConfigurer.class );

        assert cc != null;
        cc.postProcessBeanFactory( bf ); // when does this normally get called? Maybe an XmlWebApplicationContext
        // thing.

        JustATestBean o = ( JustATestBean ) bf.getBean( "testConfigured" );
        assertEquals( "foo", o.getMyValue() );

    }
}
