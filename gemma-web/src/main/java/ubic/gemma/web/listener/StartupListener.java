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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

import javax.servlet.ServletContextEvent;
import java.util.concurrent.TimeUnit;

import static org.springframework.web.context.support.WebApplicationContextUtils.getRequiredWebApplicationContext;

/**
 * Slight variant of {@link ContextLoaderListener} that logs the time taken to initialize the context.
 * @author keshav
 * @author pavlidis
 * @author Matt Raible (original version)
 * @author poirigui
 */
@CommonsLog
public class StartupListener extends ContextLoaderListener {

    @Override
    public void contextInitialized( ServletContextEvent event ) {
        StopWatch sw = StopWatch.createStarted();
        // call Spring's context ContextLoaderListener to initialize
        // all the context files specified in web.xml
        super.contextInitialized( event );
        ApplicationContext ctx = getRequiredWebApplicationContext( event.getServletContext() );
        StartupListener.log.info( String.format( "Initialization of Gemma Web context took %d s. The following profiles are active: %s.",
                sw.getTime( TimeUnit.SECONDS ),
                String.join( ", ", ctx.getEnvironment().getActiveProfiles() ) ) );
    }
}
