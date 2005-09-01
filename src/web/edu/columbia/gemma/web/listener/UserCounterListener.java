/*
 * The Gemma project
 * 
 * Copyright (c) 2005 Columbia University
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
package edu.columbia.gemma.web.listener;

import java.util.HashSet;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionBindingEvent;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.columbia.gemma.web.Constants;

/**
 * UserCounterListener class used to count the current number of active users for the applications. Does this by
 * counting how many user objects are stuffed into the session. It also grabs these users and exposes them in the
 * servlet context.
 * <p>
 * Contains code from Appfuse.
 * <hr>
 * <p>
 * Copyright (c) 2004 - 2005 Columbia University
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
    private int counter;
    private final transient Log log = LogFactory.getLog( UserCounterListener.class );
    private transient ServletContext servletContext;
    private Set<Object> users;

    /**
     * This method is designed to catch when user's login and record their name
     * 
     * @see javax.servlet.http.HttpSessionAttributeListener#attributeAdded(javax.servlet.http.HttpSessionBindingEvent)
     */
    public void attributeAdded( HttpSessionBindingEvent event ) {
        if ( event.getName().equals( Constants.USER_KEY ) ) {
            addUsername( event.getValue() );
        }
    }

    /**
     * When users logout, remove their name from the hashMap
     * 
     * @see javax.servlet.http.HttpSessionAttributeListener#attributeRemoved(javax.servlet.http.HttpSessionBindingEvent)
     */
    public void attributeRemoved( HttpSessionBindingEvent event ) {
        if ( event.getName().equals( Constants.USER_KEY ) ) {
            removeUsername( event.getValue() );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.http.HttpSessionAttributeListener#attributeReplaced(javax.servlet.http.HttpSessionBindingEvent)
     */
    @SuppressWarnings("unused")
    public void attributeReplaced( HttpSessionBindingEvent event ) {
        // I don't really care if the user changes their information
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @SuppressWarnings("unused")
    public synchronized void contextDestroyed( ServletContextEvent event ) {
        servletContext = null;
        users = null;
        counter = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    public synchronized void contextInitialized( ServletContextEvent sce ) {
        servletContext = sce.getServletContext();
        servletContext.setAttribute( ( COUNT_KEY ), Integer.toString( counter ) );
    }

    /**
     * @param user
     */
    @SuppressWarnings("unchecked")
    synchronized void addUsername( Object user ) {
        assert servletContext.getAttribute( USERS_KEY ) instanceof Set : "USERS_KEY is not a java.util.Set";
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

    /**
     * 
     */
    synchronized void decrementUserCounter() {
        // N.B. I'm pretty sure this was a bug, it was hiding the class property 'counter'. PP 8/28/2005
        counter = Integer.parseInt( ( String ) servletContext.getAttribute( COUNT_KEY ) );
        counter--;

        if ( counter < 0 ) {
            log.warn( "User count went negative" );
            counter = 0;
        }

        servletContext.setAttribute( COUNT_KEY, Integer.toString( counter ) );

        if ( log.isDebugEnabled() ) {
            log.debug( "User Count: " + counter );
        }
    }

    /**
     * 
     */
    synchronized void incrementUserCounter() {
        counter = Integer.parseInt( ( String ) servletContext.getAttribute( COUNT_KEY ) );
        counter++;
        servletContext.setAttribute( COUNT_KEY, Integer.toString( counter ) );

        if ( log.isDebugEnabled() ) {
            log.debug( "User Count: " + counter );
        }
    }

    /**
     * @param user
     */
    @SuppressWarnings("unchecked")
    synchronized void removeUsername( Object user ) {
        assert servletContext.getAttribute( USERS_KEY ) instanceof Set : "USERS_KEY is not a java.util.Set";
        users = ( Set ) servletContext.getAttribute( USERS_KEY );

        if ( users != null ) {
            users.remove( user );
        }

        servletContext.setAttribute( USERS_KEY, users );
        decrementUserCounter();
    }
}
