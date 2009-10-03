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
package ubic.gemma.model.common.description;

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.common.description.ExternalDatabase
 */
public interface ExternalDatabaseDao extends BaseDao<ExternalDatabase> {
    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.common.description.ExternalDatabase)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #find(int, ubic.gemma.model.common.description.ExternalDatabase
     * externalDatabase)}.
     * </p>
     */
    public Object find( int transform, String queryString,
            ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.common.description.ExternalDatabase)} with an additional
     * flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object find( int transform, ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.common.description.ExternalDatabase)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #find(ubic.gemma.model.common.description.ExternalDatabase)}.
     * </p>
     */
    public ubic.gemma.model.common.description.ExternalDatabase find( String queryString,
            ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

    /**
     * 
     */
    public ubic.gemma.model.common.description.ExternalDatabase find(
            ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

    /**
     * <p>
     * Does the same thing as {@link #findByLocalDbInstallName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection findByLocalDbInstallName( int transform, java.lang.String localInstallDBName );

    /**
     * <p>
     * Does the same thing as {@link #findByLocalDbInstallName(boolean, java.lang.String)} with an additional argument
     * called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string
     * defined in {@link #findByLocalDbInstallName(int, java.lang.String localInstallDBName)}.
     * </p>
     */
    public java.util.Collection findByLocalDbInstallName( int transform, String queryString,
            java.lang.String localInstallDBName );

    /**
     * 
     */
    public java.util.Collection findByLocalDbInstallName( java.lang.String localInstallDBName );

    /**
     * <p>
     * Does the same thing as {@link #findByLocalDbInstallName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByLocalDbInstallName(java.lang.String)}.
     * </p>
     */
    public java.util.Collection findByLocalDbInstallName( String queryString, java.lang.String localInstallDBName );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByName( int transform, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(int, java.lang.String name)}.
     * </p>
     */
    public Object findByName( int transform, String queryString, java.lang.String name );

    /**
     * 
     */
    public ubic.gemma.model.common.description.ExternalDatabase findByName( java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findByName(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByName(java.lang.String)}.
     * </p>
     */
    public ubic.gemma.model.common.description.ExternalDatabase findByName( String queryString, java.lang.String name );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, ubic.gemma.model.common.description.ExternalDatabase)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(int,
     * ubic.gemma.model.common.description.ExternalDatabase externalDatabase)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString,
            ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.common.description.ExternalDatabase)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform, ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.common.description.ExternalDatabase)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(ubic.gemma.model.common.description.ExternalDatabase)}.
     * </p>
     */
    public ubic.gemma.model.common.description.ExternalDatabase findOrCreate( String queryString,
            ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

    /**
     * 
     */
    public ubic.gemma.model.common.description.ExternalDatabase findOrCreate(
            ubic.gemma.model.common.description.ExternalDatabase externalDatabase );

}
