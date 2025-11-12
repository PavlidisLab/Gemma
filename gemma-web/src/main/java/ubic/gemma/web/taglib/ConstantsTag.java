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
package ubic.gemma.web.taglib;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import ubic.gemma.web.util.Constants;

import javax.annotation.Nullable;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.TagSupport;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * This class is designed to put all the public variables in a class to a specified scope - designed for exposing a
 * Constants class to Tag Libraries.
 * </p>
 * It is designed to be used as follows:
 * <pre>
 * &lt;tag:constants /&gt;
 * </pre>
 * <p>
 * Optional values are "className" (fully qualified) and "scope".
 * </p>
 * <a href="BaseAction.java.html"><i>View Source</i></a>
 *
 * @author <a href="mailto:matt@raibledesigns.com">Matt Raible</a>, originally.
 * @author pavlidis modified
 */
@Setter
@Getter
@CommonsLog
public class ConstantsTag extends TagSupport {

    /**
     * Maps lowercase JSP scope names to their PageContext integer constant values.
     */
    private static final Map<String, Integer> scopes = new HashMap<>();

    static {
        scopes.put( "page", PageContext.PAGE_SCOPE );
        scopes.put( "request", PageContext.REQUEST_SCOPE );
        scopes.put( "session", PageContext.SESSION_SCOPE );
        scopes.put( "application", PageContext.APPLICATION_SCOPE );
    }

    /**
     * The class to expose the variables from.
     */
    private String className = Constants.class.getName();

    /**
     * The scope to be put the variable in.
     */
    @Nullable
    private String scope = null;

    /**
     * The single variable to expose.
     */
    @Nullable
    private String var = null;

    @Override
    public int doStartTag() throws JspException {
        // Using reflection, get the available field names in the class
        Class<?> c;
        int toScope = PageContext.PAGE_SCOPE;

        if ( scope != null ) {
            toScope = getScope( scope );
        }

        try {
            c = Class.forName( className );
        } catch ( ClassNotFoundException cnf ) {
            log.error( "ClassNotFound - maybe a typo?" );
            throw new JspException( cnf.getMessage() );
        }

        try {
            // if var is null, expose all variables
            if ( var == null ) {
                Field[] fields = c.getDeclaredFields();

                AccessibleObject.setAccessible( fields, true );

                for ( Field field : fields ) {
                    /*
                     * if (log.isDebugEnabled()) { log.debug("putting '" + fields[i].getName() + "=" +
                     * fields[i].get(this) + "' into " + scope + " scope"); }
                     */
                    pageContext.setAttribute( field.getName(), field.get( this ), toScope );
                }
            } else {
                try {
                    String value = ( String ) c.getField( var ).get( this );
                    pageContext.setAttribute( c.getField( var ).getName(), value, toScope );
                } catch ( NoSuchFieldException nsf ) {
                    log.error( nsf.getMessage() );
                    throw new JspException( nsf );
                }
            }
        } catch ( IllegalAccessException iae ) {
            log.error( "Illegal Access Exception - maybe a classloader issue?" );
            throw new JspException( iae );
        }

        // Continue processing this page
        return ( SKIP_BODY );
    }

    /**
     * Converts the scope name into its corresponding PageContext constant value.
     *
     * @param scopeName Can be "page", "request", "session", or "application" in any case.
     * @return The constant representing the scope (ie. PageContext.REQUEST_SCOPE).
     * @throws JspException if the scopeName is not a valid name.
     */
    public int getScope( String scopeName ) throws JspException {
        Integer localScope = scopes.get( scopeName.toLowerCase() );

        if ( localScope == null ) {
            throw new JspException( "Scope '" + scopeName + "' not a valid option" );
        }

        return localScope;
    }

    /**
     * Release all allocated resources.
     */
    @Override
    public void release() {
        super.release();
        className = null;
        scope = Constants.class.getName();
    }
}
