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
package ubic.gemma.model.common.auditAndSecurity;

/**
 * Implementation of UserRole services.
 * 
 * @author pavlidis
 * @version $Id$
 */
public class UserRoleServiceImpl extends ubic.gemma.model.common.auditAndSecurity.UserRoleServiceBase {

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserRoleService#getRoles()
     */
    protected java.util.Collection handleGetRoles() throws java.lang.Exception {
        return this.getUserRoleDao().loadAll();
    }

    /**
     * Just send back an object with the right role set.
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.UserRoleService#getRole(java.lang.String)
     */
    protected ubic.gemma.model.common.auditAndSecurity.UserRole handleGetRole( java.lang.String roleName )
            throws java.lang.Exception {
        // Collection roles = this.getUserRoleDao().findRolesByRoleName( roleName );
        // if (roles.size() == 0) throw new NoSuchElementException("No such UserRole: " + roleName);
        // return ( UserRole ) roles.iterator().next();
        UserRole newRole = UserRole.Factory.newInstance();
        newRole.setName( roleName );
        return newRole;
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserRoleService#saveRole(ubic.gemma.model.common.auditAndSecurity.UserRole)
     */
    protected UserRole handleSaveRole( ubic.gemma.model.common.auditAndSecurity.UserRole role )
            throws java.lang.Exception {
        return ( UserRole ) this.getUserRoleDao().create( role );
    }

    /**
     * @see ubic.gemma.model.common.auditAndSecurity.UserRoleService#removeRole(java.lang.String)
     */
    protected void handleRemoveRole( java.lang.String roleName ) throws java.lang.Exception {
        this.getUserRoleDao().remove( this.getUserRoleDao().findRolesByRoleName( roleName ) );
    }
}