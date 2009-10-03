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

/**
 * @see ubic.gemma.model.common.description.LocalFile
 */
public interface LocalFileDao {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes no transformation will occur.
     */
    public final static int TRANSFORM_NONE = 0;

    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.common.description.LocalFile)} with an additional flag
     * called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the returned entity will
     * <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here then the result
     * <strong>WILL BE</strong> passed through an operation which can optionally transform the entities (into value
     * objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection create( int transform, java.util.Collection entities );

    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.common.description.LocalFile)} with an additional flag
     * called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the returned entity will
     * <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here then the result
     * <strong>WILL BE</strong> passed through an operation which can optionally transform the entity (into a value
     * object for example). By default, transformation does not occur.
     * </p>
     */
    public Object create( int transform, ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * Creates a new instance of ubic.gemma.model.common.description.LocalFile and adds from the passed in
     * <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.common.description.LocalFile instances to create.
     * @return the created instances.
     */
    public java.util.Collection create( java.util.Collection entities );

    /**
     * Creates an instance of ubic.gemma.model.common.description.LocalFile and adds it to the persistent store.
     */
    public ubic.gemma.model.common.description.LocalFile create( ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.common.description.LocalFile)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #find(int, ubic.gemma.model.common.description.LocalFile localFile)}.
     * </p>
     */
    public Object find( int transform, String queryString, ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.common.description.LocalFile)} with an additional flag
     * called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object find( int transform, ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.common.description.LocalFile)} with an additional argument
     * called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string
     * defined in {@link #find(ubic.gemma.model.common.description.LocalFile)}.
     * </p>
     */
    public ubic.gemma.model.common.description.LocalFile find( String queryString,
            ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * 
     */
    public ubic.gemma.model.common.description.LocalFile find( ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * <p>
     * Does the same thing as {@link #findByLocalURL(java.net.URL, java.lang.Long)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByLocalURL( int transform, java.net.URL url, java.lang.Long size );

    /**
     * <p>
     * Does the same thing as {@link #findByLocalURL(boolean, java.net.URL, java.lang.Long)} with an additional argument
     * called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string
     * defined in {@link #findByLocalURL(int, java.net.URL url, java.lang.Long size)}.
     * </p>
     */
    public Object findByLocalURL( int transform, String queryString, java.net.URL url, java.lang.Long size );

    /**
     * 
     */
    public ubic.gemma.model.common.description.LocalFile findByLocalURL( java.net.URL url, java.lang.Long size );

    /**
     * <p>
     * Does the same thing as {@link #findByLocalURL(java.net.URL, java.lang.Long)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByLocalURL(java.net.URL, java.lang.Long)}.
     * </p>
     */
    public ubic.gemma.model.common.description.LocalFile findByLocalURL( String queryString, java.net.URL url,
            java.lang.Long size );

    /**
     * <p>
     * Does the same thing as {@link #findByRemoteURL(java.net.URL, java.lang.Long)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findByRemoteURL( int transform, java.net.URL url, java.lang.Long size );

    /**
     * <p>
     * Does the same thing as {@link #findByRemoteURL(boolean, java.net.URL, java.lang.Long)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #findByRemoteURL(int, java.net.URL url, java.lang.Long size)}.
     * </p>
     */
    public Object findByRemoteURL( int transform, String queryString, java.net.URL url, java.lang.Long size );

    /**
     * 
     */
    public ubic.gemma.model.common.description.LocalFile findByRemoteURL( java.net.URL url, java.lang.Long size );

    /**
     * <p>
     * Does the same thing as {@link #findByRemoteURL(java.net.URL, java.lang.Long)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #findByRemoteURL(java.net.URL, java.lang.Long)}.
     * </p>
     */
    public ubic.gemma.model.common.description.LocalFile findByRemoteURL( String queryString, java.net.URL url,
            java.lang.Long size );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, ubic.gemma.model.common.description.LocalFile)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(int, ubic.gemma.model.common.description.LocalFile
     * localFile)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString,
            ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.common.description.LocalFile)} with an additional
     * flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform, ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.common.description.LocalFile)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #findOrCreate(ubic.gemma.model.common.description.LocalFile)}.
     * </p>
     */
    public ubic.gemma.model.common.description.LocalFile findOrCreate( String queryString,
            ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * 
     */
    public ubic.gemma.model.common.description.LocalFile findOrCreate(
            ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * <p>
     * Does the same thing as {@link #load(java.lang.Long)} with an additional flag called <code>transform</code>. If
     * this flag is set to <code>TRANSFORM_NONE</code> then the returned entity will <strong>NOT</strong> be
     * transformed. If this flag is any of the other constants defined in this class then the result <strong>WILL
     * BE</strong> passed through an operation which can optionally transform the entity (into a value object for
     * example). By default, transformation does not occur.
     * </p>
     * 
     * @param id the identifier of the entity to load.
     * @return either the entity or the object transformed from the entity.
     */
    public Object load( int transform, java.lang.Long id );

    /**
     * Loads an instance of ubic.gemma.model.common.description.LocalFile from the persistent store.
     */
    public ubic.gemma.model.common.description.LocalFile load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.common.description.LocalFile}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection loadAll();

    /**
     * <p>
     * Does the same thing as {@link #loadAll()} with an additional flag called <code>transform</code>. If this flag is
     * set to <code>TRANSFORM_NONE</code> then the returned entity will <strong>NOT</strong> be transformed. If this
     * flag is any of the other constants defined here then the result <strong>WILL BE</strong> passed through an
     * operation which can optionally transform the entity (into a value object for example). By default, transformation
     * does not occur.
     * </p>
     * 
     * @param transform the flag indicating what transformation to use.
     * @return the loaded entities.
     */
    public java.util.Collection loadAll( final int transform );

    /**
     * Removes the instance of ubic.gemma.model.common.description.LocalFile having the given <code>identifier</code>
     * from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection entities );

    /**
     * Removes the instance of ubic.gemma.model.common.description.LocalFile from the persistent store.
     */
    public void remove( ubic.gemma.model.common.description.LocalFile localFile );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection entities );

    /**
     * Updates the <code>localFile</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.common.description.LocalFile localFile );

}
