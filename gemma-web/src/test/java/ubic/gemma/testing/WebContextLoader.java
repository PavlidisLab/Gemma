/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.support.AbstractContextLoader;
import org.springframework.util.StringUtils;
import org.springframework.web.context.ConfigurableWebApplicationContext;
import org.springframework.web.context.support.XmlWebApplicationContext;

/**
 * See {@link http://jira.springframework.org/browse/SPR-5243} and {@link http
 * ://dhruba.name/2008/10/25/using-mockservletcontext-and-contextloader-with-spring-25-testcontext-framework/}.
 * 
 * @author paul
 * @version $Id$
 * @see BaseSpringWebTest
 */
public class WebContextLoader extends AbstractContextLoader {
    protected static final Log logger = LogFactory.getLog( WebContextLoader.class );

    @Override
    public ApplicationContext loadContext( String... locations ) throws Exception {
        if ( logger.isDebugEnabled() ) {
            logger.debug( "Loading WebApplicationContext for locations ["
                    + StringUtils.arrayToCommaDelimitedString( locations ) + "]." );
        }
        ConfigurableWebApplicationContext context = new XmlWebApplicationContext();
        context.setConfigLocations( locations );
        context.setServletContext( new MockServletContext( "" ) );
        context.refresh();

        AnnotationConfigUtils.registerAnnotationConfigProcessors( ( BeanDefinitionRegistry ) context.getBeanFactory() );

        context.registerShutdownHook();
        return context;
    }

    @Override
    protected String getResourceSuffix() {
        return "-context.xml";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.test.context.SmartContextLoader#loadContext(org.springframework.test.context.
     * MergedContextConfiguration)
     */
    @Override
    public ApplicationContext loadContext( MergedContextConfiguration arg0 ) throws Exception {
        String[] locations = arg0.getLocations();
        return this.loadContext( locations );
    }

}
