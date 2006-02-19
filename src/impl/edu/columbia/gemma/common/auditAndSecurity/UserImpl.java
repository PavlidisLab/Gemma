/*
 * The Gemma project.
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 */
package edu.columbia.gemma.common.auditAndSecurity;

import java.util.ArrayList;
import java.util.List;

import edu.columbia.gemma.util.LabelValue;

/**
 * @author pavlidis
 * @version $Id$
 * @see edu.columbia.gemma.common.auditAndSecurity.User
 */
public class UserImpl extends edu.columbia.gemma.common.auditAndSecurity.User implements java.io.Serializable {
    /** The serial version UID of this class. Needed for serialization. */
    private static final long serialVersionUID = 2992079491831675752L;

    /**
     * Convert user roles to LabelValue objects for convenience in display in views.
     */
    public List<LabelValue> getRoleList() {
        List<LabelValue> userRoles = new ArrayList<LabelValue>();

        if ( this.getRoles() != null ) {
            for ( UserRole role : this.getRoles() ) {
                // convert the user's roles to LabelValue Objects
                userRoles.add( new LabelValue( role.getName(), role.getName() ) );
            }
        }
        return userRoles;
    }

}