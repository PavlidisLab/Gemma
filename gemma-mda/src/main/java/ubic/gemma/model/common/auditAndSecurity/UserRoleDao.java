/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity;

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.UserRole
 */
public interface UserRoleDao extends BaseDao<UserRole> {
    /**
     * <p>
     * Does the same thing as {@link #findRolesByRoleName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection<UserRole> findRolesByRoleName( int transform, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findRolesByRoleName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findRolesByRoleName(int, java.lang.String name)}.
     * </p>
     */
    public java.util.Collection<UserRole> findRolesByRoleName( int transform, String queryString, java.lang.String name );

    /**
     * 
     */
    public java.util.Collection<UserRole> findRolesByRoleName( java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findRolesByRoleName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findRolesByRoleName(java.lang.String)}.
     * </p>
     */
    public java.util.Collection<UserRole> findRolesByRoleName( String queryString, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findRolesByUserName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection<UserRole> findRolesByUserName( int transform, java.lang.String userName );

    /**
     * <p>
     * Does the same thing as {@link #findRolesByUserName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findRolesByUserName(int, java.lang.String userName)}.
     * </p>
     */
    public java.util.Collection<UserRole> findRolesByUserName( int transform, String queryString,
            java.lang.String userName );

    /**
     * <p>
     * Return all roles
     * </p>
     */
    public java.util.Collection<UserRole> findRolesByUserName( java.lang.String userName );

    /**
     * <p>
     * Does the same thing as {@link #findRolesByUserName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findRolesByUserName(java.lang.String)}.
     * </p>
     */
    public java.util.Collection<UserRole> findRolesByUserName( String queryString, java.lang.String userName );

}
