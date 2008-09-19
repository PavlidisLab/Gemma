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

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.context.HttpSessionContextIntegrationFilter;
import org.springframework.security.context.SecurityContext;

/**
 * UserCounterListener class used to count the current number of active users for the applications. Does this by
 * counting how many user objects are stuffed into the session. It also grabs these users and exposes them in the
 * servlet context.
 * <p>
 * Contains code from Appfuse.
 * 
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @web.listener
 */
public class UserCounterListener implements ServletContextListener, HttpSessionAttributeListener {
    public static final String COUNT_KEY = "userCounter";
    public static final String USERS_KEY = "userNames";
    public static final String EVENT_KEY = HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY;
    private final transient Log log = LogFactory.getLog( UserCounterListener.class );
    private transient ServletContext servletContext;
    private int counter;
    private Set users;

    public synchronized void contextInitialized( ServletContextEvent sce ) {
        servletContext = sce.getServletContext();
        servletContext.setAttribute( ( COUNT_KEY ), Integer.toString( counter ) );
    }

    @SuppressWarnings("unused")
    public synchronized void contextDestroyed( ServletContextEvent event ) {
        servletContext = null;
        users = null;
        counter = 0;
    }

    synchronized void incrementUserCounter() {
        counter = Integer.parseInt( ( String ) servletContext.getAttribute( COUNT_KEY ) );
        counter++;
        servletContext.setAttribute( COUNT_KEY, Integer.toString( counter ) );

        if ( log.isDebugEnabled() ) {
            log.debug( "User Count: " + counter );
        }
    }

    synchronized void decrementUserCounter() {
        counter = Integer.parseInt( ( String ) servletContext.getAttribute( COUNT_KEY ) );
        counter--;

        if ( counter < 0 ) {
            counter = 0;
        }

        servletContext.setAttribute( COUNT_KEY, Integer.toString( counter ) );

        if ( log.isDebugEnabled() ) {
            log.debug( "User Count: " + counter );
        }
    }

    @SuppressWarnings("unchecked")
    synchronized void addUsername( Object user ) {
        users = ( Set ) servletContext.getAttribute( USERS_KEY );

        if ( users == null ) {
            users = new HashSet();
        }

        if ( log.isDebugEnabled() ) {
            if ( users.contains( user ) ) {
                log.debug( "User already logged in, adding anyway..." );
            }
        }

        users.add( user );
        servletContext.setAttribute( USERS_KEY, users );
        incrementUserCounter();
    }

    synchronized void removeUsername( Object user ) {
        users = ( Set ) servletContext.getAttribute( USERS_KEY );

        if ( users != null ) {
            users.remove( user );
        }

        servletContext.setAttribute( USERS_KEY, users );
        decrementUserCounter();
    }

    /**
     * This method is designed to catch when users login and record their name
     * 
     * @see javax.servlet.http.HttpSessionAttributeListener#attributeAdded(javax.servlet.http.HttpSessionBindingEvent)
     */
    public void attributeAdded( HttpSessionBindingEvent event ) {
        log.debug( "event.name: " + event.getName() );
        if ( event.getName().equals( EVENT_KEY ) ) {
            SecurityContext securityContext = ( SecurityContext ) event.getValue();
            /*
             * The user returned here is not a Gemma User, not a acegi userdetails object, but a String.
             */

            if ( securityContext == null || securityContext.getAuthentication() == null ) {
                log.warn( "No security context or no authentication object" );
                return;
            }
            Object user = securityContext.getAuthentication().getPrincipal();

            addUsername( user );
        }
    }

    /**
     * When users logout, remove their name from the hashMap
     * 
     * @see javax.servlet.http.HttpSessionAttributeListener#attributeRemoved(javax.servlet.http.HttpSessionBindingEvent)
     */
    public void attributeRemoved( HttpSessionBindingEvent event ) {
        if ( event.getName().equals( EVENT_KEY ) ) {
            SecurityContext securityContext = ( SecurityContext ) event.getValue();
            if ( securityContext == null || securityContext.getAuthentication() == null ) {
                log.warn( "No security context or no authentication object" );
                return;
            }
            Object user = securityContext.getAuthentication().getPrincipal();
            removeUsername( user );
        }
    }

    /**
     * @see javax.servlet.http.HttpSessionAttributeListener#attributeReplaced(javax.servlet.http.HttpSessionBindingEvent)
     */
    @SuppressWarnings("unused")
    public void attributeReplaced( HttpSessionBindingEvent event ) {
        // I don't really care if the user changes their information
    }

}
