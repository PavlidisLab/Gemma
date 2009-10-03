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
package ubic.gemma.model.genome.sequenceAnalysis;

/**
 * @see ubic.gemma.model.genome.sequenceAnalysis.BlatResult
 */
public interface BlatResultDao extends ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResultDao {
    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with an additional
     * flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the returned entity
     * will <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here then the result
     * <strong>WILL BE</strong> passed through an operation which can optionally transform the entities (into value
     * objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection create( int transform, java.util.Collection entities );

    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with an additional
     * flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the returned entity
     * will <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here then the result
     * <strong>WILL BE</strong> passed through an operation which can optionally transform the entity (into a value
     * object for example). By default, transformation does not occur.
     * </p>
     */
    public Object create( int transform, ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * Creates a new instance of ubic.gemma.model.genome.sequenceAnalysis.BlatResult and adds from the passed in
     * <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.genome.sequenceAnalysis.BlatResult instances to create.
     * @return the created instances.
     */
    public java.util.Collection create( java.util.Collection entities );

    /**
     * Creates an instance of ubic.gemma.model.genome.sequenceAnalysis.BlatResult and adds it to the persistent store.
     */
    public ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult create(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #find(int, ubic.gemma.model.genome.sequenceAnalysis.BlatResult
     * blatResult)}.
     * </p>
     */
    public Object find( int transform, String queryString,
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with an additional flag
     * called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object find( int transform, ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #find(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)}.
     * </p>
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult find( String queryString,
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * 
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult find(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * <p>
     * Does the same thing as {@link #findByBioSequence(boolean, ubic.gemma.model.genome.biosequence.BioSequence)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByBioSequence(int,
     * ubic.gemma.model.genome.biosequence.BioSequence bioSequence)}.
     * </p>
     */
    public java.util.Collection findByBioSequence( int transform, String queryString,
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection findByBioSequence( int transform,
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findByBioSequence(ubic.gemma.model.genome.biosequence.BioSequence)}.
     * </p>
     */
    public java.util.Collection findByBioSequence( String queryString,
            ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * 
     */
    public java.util.Collection findByBioSequence( ubic.gemma.model.genome.biosequence.BioSequence bioSequence );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with
     * an additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(int,
     * ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString,
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with an
     * additional flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder
     * results will <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants
     * defined here then finder results <strong>WILL BE</strong> passed through an operation which can optionally
     * transform the entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform, ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(ubic.gemma.model.genome.sequenceAnalysis.BlatResult)}.
     * </p>
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult findOrCreate( String queryString,
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * 
     */
    public ubic.gemma.model.genome.sequenceAnalysis.BlatResult findOrCreate(
            ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

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
     * Loads an instance of ubic.gemma.model.genome.sequenceAnalysis.BlatResult from the persistent store.
     */
    public ubic.gemma.model.genome.sequenceAnalysis.SequenceSimilaritySearchResult load( java.lang.Long id );

    /**
     * 
     */
    public java.util.Collection load( java.util.Collection ids );

    /**
     * Loads all entities of type {@link ubic.gemma.model.genome.sequenceAnalysis.BlatResult}.
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
     * Removes the instance of ubic.gemma.model.genome.sequenceAnalysis.BlatResult having the given
     * <code>identifier</code> from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection entities );

    /**
     * Removes the instance of ubic.gemma.model.genome.sequenceAnalysis.BlatResult from the persistent store.
     */
    public void remove( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection entities );

    /**
     * Updates the <code>blatResult</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.genome.sequenceAnalysis.BlatResult blatResult );

}
