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
package ubic.gemma.web.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.context.support.ApplicationObjectSupport;
import org.springframework.stereotype.Component;

/**
 * 
 * @author pavlidis
 * @version $Id$
 */
@Component
public class MessageUtilImpl extends ApplicationObjectSupport implements MessageUtil {

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#getText(java.lang.String, java.util.Locale)
     */
    @Override
    public String getText( String msgKey, Locale locale ) {
        return getMessageSourceAccessor().getMessage( msgKey, locale );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#getText(java.lang.String, java.lang.Object[], java.util.Locale)
     */
    @Override
    public String getText( String msgKey, Object[] args, Locale locale ) {
        return getMessageSourceAccessor().getMessage( msgKey, args, locale );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#getText(java.lang.String, java.lang.String, java.util.Locale)
     */
    @Override
    public String getText( String msgKey, String arg, Locale locale ) {
        return getText( msgKey, new Object[] { arg }, locale );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String)
     */
    @Override
    public void saveMessage( HttpServletRequest request, String msg ) {
        this.saveMessage( request.getSession(), msg );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpSession, java.lang.String)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void saveMessage( HttpSession session, String msg ) {
        Object sessAttr = session.getAttribute( "messages" );
        List<String> messages;

        if ( sessAttr == null ) {
            messages = new ArrayList<String>();
        } else {
            if ( sessAttr instanceof String ) {
                messages = new ArrayList<String>();
                messages.add( ( String ) sessAttr );
            } else {
                messages = ( List<String> ) sessAttr;
            }
        }

        messages.add( msg );
        session.setAttribute( "messages", messages );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.Object[], java.lang.String)
     */
    @Override
    public void saveMessage( HttpServletRequest request, String key, Object[] parameters, String defaultMessage ) {
        String newMessage = getText( key, parameters, request.getLocale() );
        if ( newMessage == null ) newMessage = defaultMessage;
        saveMessage( request, newMessage );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.Object, java.lang.String)
     */
    @Override
    public void saveMessage( HttpServletRequest request, String key, Object parameter, String defaultMessage ) {
        String newMessage = getText( key, new Object[] { parameter }, request.getLocale() );
        if ( newMessage == null ) newMessage = defaultMessage;
        saveMessage( request, newMessage );
    }

    /* (non-Javadoc)
     * @see ubic.gemma.web.util.MessageUtil#saveMessage(javax.servlet.http.HttpServletRequest, java.lang.String, java.lang.String)
     */
    @Override
    public void saveMessage( HttpServletRequest request, String key, String defaultMessage ) {
        String newMessage = getText( key, new Object[] {}, request.getLocale() );
        if ( newMessage == null ) newMessage = defaultMessage;
        saveMessage( request, newMessage );
    }

}
