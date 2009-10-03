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

import java.util.Collection;

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.common.description.BibliographicReference
 */
public interface BibliographicReferenceDao extends BaseDao<BibliographicReference> {
    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.common.description.BibliographicReference)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #find(int, ubic.gemma.model.common.description.BibliographicReference
     * bibliographicReference)}.
     * </p>
     */
    public Object find( int transform, String queryString,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.common.description.BibliographicReference)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object find( int transform, ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.common.description.BibliographicReference)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #find(ubic.gemma.model.common.description.BibliographicReference)}.
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference find( String queryString,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * 
     */
    public ubic.gemma.model.common.description.BibliographicReference find(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * <p>
     * Does the same thing as {@link #findByExternalId(java.lang.String, java.lang.String)} with an additional flag
     * called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByExternalId( int transform, java.lang.String id, java.lang.String databaseName );

    /**
     * <p>
     * Does the same thing as {@link #findByExternalId(boolean, java.lang.String, java.lang.String)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #findByExternalId(int, java.lang.String id, java.lang.String databaseName)}.
     * </p>
     */
    public Object findByExternalId( int transform, String queryString, java.lang.String id,
            java.lang.String databaseName );

    /**
     * <p>
     * Does the same thing as {@link #findByExternalId(boolean, ubic.gemma.model.common.description.DatabaseEntry)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByExternalId(int,
     * ubic.gemma.model.common.description.DatabaseEntry externalId)}.
     * </p>
     */
    public Object findByExternalId( int transform, String queryString,
            ubic.gemma.model.common.description.DatabaseEntry externalId );

    /**
     * <p>
     * Does the same thing as {@link #findByExternalId(ubic.gemma.model.common.description.DatabaseEntry)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByExternalId( int transform, ubic.gemma.model.common.description.DatabaseEntry externalId );

    /**
     * 
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( java.lang.String id,
            java.lang.String databaseName );

    /**
     * <p>
     * Does the same thing as {@link #findByExternalId(java.lang.String, java.lang.String)} with an additional argument
     * called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string
     * defined in {@link #findByExternalId(java.lang.String, java.lang.String)}.
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( String queryString,
            java.lang.String id, java.lang.String databaseName );

    /**
     * <p>
     * Does the same thing as {@link #findByExternalId(ubic.gemma.model.common.description.DatabaseEntry)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByExternalId(ubic.gemma.model.common.description.DatabaseEntry)}
     * .
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId( String queryString,
            ubic.gemma.model.common.description.DatabaseEntry externalId );

    /**
     * <p>
     * Find by the external database id, such as for PubMed
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference findByExternalId(
            ubic.gemma.model.common.description.DatabaseEntry externalId );

    /**
     * <p>
     * Does the same thing as {@link #findByTitle(java.lang.String)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByTitle( int transform, java.lang.String title );

    /**
     * <p>
     * Does the same thing as {@link #findByTitle(boolean, java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByTitle(int, java.lang.String title)}.
     * </p>
     */
    public Object findByTitle( int transform, String queryString, java.lang.String title );

    /**
     * 
     */
    public ubic.gemma.model.common.description.BibliographicReference findByTitle( java.lang.String title );

    /**
     * <p>
     * Does the same thing as {@link #findByTitle(java.lang.String)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByTitle(java.lang.String)}.
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference findByTitle( String queryString,
            java.lang.String title );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, ubic.gemma.model.common.description.BibliographicReference)}
     * with an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(int,
     * ubic.gemma.model.common.description.BibliographicReference bibliographicReference)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.common.description.BibliographicReference)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.common.description.BibliographicReference)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in
     * {@link #findOrCreate(ubic.gemma.model.common.description.BibliographicReference)}.
     * </p>
     */
    public ubic.gemma.model.common.description.BibliographicReference findOrCreate( String queryString,
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * 
     */
    public ubic.gemma.model.common.description.BibliographicReference findOrCreate(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    /**
     * 
     */
    public java.util.Collection getAllExperimentLinkedReferences();

    /**
     * 
     */
    public java.util.Collection getRelatedExperiments(
            ubic.gemma.model.common.description.BibliographicReference bibliographicReference );

    public Collection<BibliographicReference> load( Collection<Long> ids );

}
