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
package ubic.gemma.web.propertyeditor;

import java.beans.PropertyEditorSupport;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.StringUtils;

import ubic.gemma.model.common.auditAndSecurity.UserRole;
import ubic.gemma.model.common.auditAndSecurity.UserRoleImpl;

/**
 * A custom property editor to make spring binding on roles easier to deal with. The roles (actually, the role names)
 * for a given user are displayed as a comma delimited string.
 * <p>
 * When using the spring bind funtionality on a {@link UserRole} in a form, the method getAsText is called on form entry
 * (ie. in a GET) and displays the roles (actually, role names) as a comma separated list of roles. On a POST (for
 * example, when editing the user profile and clicking submit), the comma delimited string of role names are converted
 * to a collection of roles.
 * 
 * @author keshav
 * @version $Id$
 * @deprecated since we don't use Roles, we instead use the Group concept
 */
@Deprecated
public class UserRolesPropertyEditor extends PropertyEditorSupport {

    private static final String COMMA_DELIM = ",";

    private Log log = LogFactory.getLog( this.getClass() );

    Collection<UserRole> roles = null;

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.PropertyEditorSupport#getAsText()
     */
    @Override
    public String getAsText() {
        Object value = getValue();

        String roleNames = "";

        if ( value != null && ( value instanceof Collection<?> ) ) {
            Collection<?> r = ( Collection<?> ) value;
            for ( Object next : r ) {
                if ( next instanceof UserRole ) {
                    UserRole role = ( UserRole ) next;
                    if ( org.apache.commons.lang3.StringUtils.isEmpty( roleNames ) )
                        roleNames = role.getName();
                    else
                        roleNames = roleNames + COMMA_DELIM + role.getName();
                }
            }
        }
        return roleNames;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.beans.PropertyEditorSupport#setAsText(java.lang.String)
     */
    @Override
    public void setAsText( String text ) throws IllegalArgumentException {
        if ( !StringUtils.hasText( text ) ) {
            setValue( null );
        } else {
            roles = new HashSet<UserRole>();
            String trim = text.trim();
            String[] roleNames = null;
            if ( trim.contains( COMMA_DELIM ) )
                roleNames = StringUtils.split( trim, COMMA_DELIM );
            else
                roleNames = new String[] { trim };
            if ( roleNames != null && roleNames.length != 0 ) {
                for ( String roleName : roleNames ) {
                    log.debug( roleName );
                    UserRole role = new UserRoleImpl();
                    role.setName( roleName );
                    roles.add( role );
                }
                setValue( roles );
            } else {
                throw new IllegalArgumentException( "Could not parse role(s): " + text );
            }
        }

    }
}
