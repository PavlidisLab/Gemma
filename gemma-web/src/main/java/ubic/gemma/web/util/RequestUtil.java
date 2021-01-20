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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * RequestUtil utility class
 * <p>
 * Good ol' copy-n-paste from <a href="http://www.javaworld.com/javaworld/jw-02-2002/ssl/utilityclass.txt">
 * http://www.javaworld.com/javaworld/jw-02-2002/ssl/utilityclass.txt</a> which is referenced in the following article:
 * <a href="http://www.javaworld.com/javaworld/jw-02-2002/jw-0215-ssl.html">
 * http://www.javaworld.com/javaworld/jw-02-2002/jw-0215-ssl.html</a>
 * </p>
 * From Appfuse.
 *
 * @author pavlidis
 */
public class RequestUtil {
    private static final String STOWED_REQUEST_ATTRIBS = "ssl.redirect.attrib.stowed";
    private final transient static Log log = LogFactory.getLog( RequestUtil.class );

    /**
     * Builds a query string from a given map of parameters
     *
     * @param m         A map of parameters
     * @param ampersand String to use for ampersands (e.g. "&amp;" or "&amp;amp;" )
     * @return query string (with no leading "?")
     */
    public static StringBuffer createQueryStringFromMap(Map<String, String[]> m, String ampersand ) {
        StringBuffer aReturn = new StringBuffer( "" );
        Set<Entry<String, String[]>> aEntryS = m.entrySet();

        for ( Entry<String, String[]> aEntry : aEntryS ) {
            String[] o = aEntry.getValue();

            if ( o.length == 0 ) {
                RequestUtil.append( aEntry.getKey(), "", aReturn, ampersand );
            } else {
                String[] aValues = o;

                for ( String aValue : aValues ) {
                    RequestUtil.append( aEntry.getKey(), aValue, aReturn, ampersand );
                }
            }
        }

        return aReturn;
    }

    /**
     * Convenience method for deleting a cookie by name
     *
     * @param response the current web response
     * @param cookie   the cookie to remove
     * @param path     the path on which the cookie was set (i.e. /appfuse)
     */
    public static void deleteCookie( HttpServletResponse response, Cookie cookie, String path ) {
        if ( cookie != null ) {
            // Delete the cookie by setting its maximum age to zero
            cookie.setMaxAge( 0 );
            cookie.setPath( path );
            response.addCookie( cookie );
        }
    }

    /**
     * Convenience method to get the application's URL based on request variables. NOTE: this is pretty useless if
     * running behind a proxy.
     *
     * @param request request
     * @return app url
     */
    public static String getAppURL( HttpServletRequest request ) {
        StringBuilder url = new StringBuilder();
        int port = request.getServerPort();
        if ( port < 0 ) {
            port = 80; // Work around java.net.URL bug
        }
        String scheme = request.getScheme();
        url.append( scheme );
        url.append( "://" );
        url.append( request.getServerName() );
        if ( ( scheme.equals( "http" ) && ( port != 80 ) ) || ( scheme.equals( "https" ) && ( port != 443 ) ) ) {
            url.append( ':' );
            url.append( port );
        }
        url.append( request.getContextPath() );
        return url.toString();
    }

    /**
     * Convenience method to get a cookie by name
     *
     * @param request the current request
     * @param name    the name of the cookie to find
     * @return the cookie (if found), null if not found
     */
    public static Cookie getCookie( HttpServletRequest request, String name ) {
        Cookie[] cookies = request.getCookies();
        Cookie returnCookie = null;

        if ( cookies == null ) {
            return null;
        }

        for ( Cookie thisCookie : cookies ) {
            if ( thisCookie.getName().equals( name ) ) {
                // cookies with no value do me no good!
                if ( !thisCookie.getValue().equals( "" ) ) {
                    returnCookie = thisCookie;

                    break;
                }
            }
        }

        return returnCookie;
    }

    /**
     * @param aRequest request
     * @return Creates query String from request body parameters
     */
    public static String getRequestParameters( HttpServletRequest aRequest ) {
        // set the ALGORIGTHM as defined for the application
        // ALGORITHM = (String) aRequest.getAttribute(Constants.ENC_ALGORITHM);
        Map<String, String[]> m = aRequest.getParameterMap();

        return RequestUtil.createQueryStringFromMap( m, "&" ).toString();
    }

    /**
     * Returns request attributes from session to request
     *
     * @param aRequest DOCUMENT ME!
     */
    public static void reclaimRequestAttributes( HttpServletRequest aRequest ) {
        @SuppressWarnings("unchecked") Map<String, Object> map = ( Map<String, Object> ) aRequest.getSession()
                .getAttribute( RequestUtil.STOWED_REQUEST_ATTRIBS );

        if ( map == null ) {
            return;
        }

        for ( String name : map.keySet() ) {
            aRequest.setAttribute( name, map.get( name ) );
        }

        aRequest.getSession().removeAttribute( RequestUtil.STOWED_REQUEST_ATTRIBS );
    }

    public static void setCookie( HttpServletResponse response, String name, String value, String path ) {
        if ( RequestUtil.log.isDebugEnabled() ) {
            RequestUtil.log.debug( "Setting cookie '" + name + "' on path '" + path + "'" );
        }

        Cookie cookie = new Cookie( name, value );
        cookie.setSecure( false );
        cookie.setPath( path );
        cookie.setMaxAge( 3600 * 24 * 30 ); // 30 days

        response.addCookie( cookie );
    }

    /**
     * Stores request attributes in session
     *
     * @param aRequest the current request
     */
    public static void stowRequestAttributes( HttpServletRequest aRequest ) {
        if ( aRequest.getSession().getAttribute( RequestUtil.STOWED_REQUEST_ATTRIBS ) != null ) {
            return;
        }

        Enumeration<String> e = aRequest.getAttributeNames();
        Map<String, Object> map = new HashMap<>();

        while ( e.hasMoreElements() ) {
            String name = e.nextElement();
            map.put( name, aRequest.getAttribute( name ) );
        }

        aRequest.getSession().setAttribute( RequestUtil.STOWED_REQUEST_ATTRIBS, map );
    }

    /**
     * Appends new key and value pair to query string
     *
     * @param key         parameter name
     * @param value       value of parameter
     * @param queryString existing query string
     * @param ampersand   string to use for ampersand (e.g. "&" or "&amp;")
     * @return query string (with no leading "?")
     */
    private static StringBuffer append( Object key, Object value, StringBuffer queryString, String ampersand ) {
        if ( queryString.length() > 0 ) {
            queryString.append( ampersand );
        }

        try {
            queryString.append( URLEncoder.encode( key.toString(), "UTF-8" ) );
            queryString.append( "=" );
            queryString.append( URLEncoder.encode( value.toString(), "UTF-8" ) );
        } catch ( UnsupportedEncodingException e ) {
            // won't happen since we're hard-coding UTF-8
        }
        return queryString;
    }
}
