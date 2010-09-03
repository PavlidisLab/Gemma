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
package ubic.gemma.model.genome.gene;

import java.util.Collection;

/**
 * @see ubic.gemma.model.genome.gene.GeneProduct
 */
public interface GeneProductDao extends ubic.gemma.model.genome.ChromosomeFeatureDao<GeneProduct> {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes entities must be transformed into objects of type
     * {@link ubic.gemma.model.genome.gene.GeneProductValueObject}.
     */
    public final static int TRANSFORM_GENEPRODUCTVALUEOBJECT = 1;

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * <p>
     * Does the same thing as {@link #find(boolean, ubic.gemma.model.genome.gene.GeneProduct)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #find(int, ubic.gemma.model.genome.gene.GeneProduct geneProduct)}.
     * </p>
     */
    public Object find( int transform, String queryString, ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.genome.gene.GeneProduct)} with an additional flag called
     * <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object find( int transform, ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * <p>
     * Does the same thing as {@link #find(ubic.gemma.model.genome.gene.GeneProduct)} with an additional argument called
     * <code>queryString</code>. This <code>queryString</code> argument allows you to override the query string defined
     * in {@link #find(ubic.gemma.model.genome.gene.GeneProduct)}.
     * </p>
     */
    public ubic.gemma.model.genome.gene.GeneProduct find( String queryString,
            ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * 
     */
    public ubic.gemma.model.genome.gene.GeneProduct find( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(boolean, ubic.gemma.model.genome.gene.GeneProduct)} with an
     * additional argument called <code>queryString</code>. This <code>queryString</code> argument allows you to
     * override the query string defined in {@link #findOrCreate(int, ubic.gemma.model.genome.gene.GeneProduct
     * geneProduct)}.
     * </p>
     */
    public Object findOrCreate( int transform, String queryString, ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)} with an additional flag
     * called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then finder results will
     * <strong>NOT</strong> be transformed during retrieval. If this flag is any of the other constants defined here
     * then finder results <strong>WILL BE</strong> passed through an operation which can optionally transform the
     * entities (into value objects for example). By default, transformation does not occur.
     * </p>
     */
    public Object findOrCreate( int transform, ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * <p>
     * Does the same thing as {@link #findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)} with an additional
     * argument called <code>queryString</code>. This <code>queryString</code> argument allows you to override the query
     * string defined in {@link #findOrCreate(ubic.gemma.model.genome.gene.GeneProduct)}.
     * </p>
     */
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate( String queryString,
            ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * 
     */
    public ubic.gemma.model.genome.gene.GeneProduct findOrCreate( ubic.gemma.model.genome.gene.GeneProduct geneProduct );

    /**
     * Converts an instance of type {@link ubic.gemma.model.genome.gene.GeneProductValueObject} to this DAO's entity.
     */
    public ubic.gemma.model.genome.gene.GeneProduct geneProductValueObjectToEntity(
            ubic.gemma.model.genome.gene.GeneProductValueObject geneProductValueObject );

    /**
     * Copies the fields of {@link ubic.gemma.model.genome.gene.GeneProductValueObject} to the specified entity.
     * 
     * @param copyIfNull If FALSE, the value object's field will not be copied to the entity if the value is NULL. If
     *        TRUE, it will be copied regardless of its value.
     */
    public void geneProductValueObjectToEntity( ubic.gemma.model.genome.gene.GeneProductValueObject sourceVO,
            ubic.gemma.model.genome.gene.GeneProduct targetEntity, boolean copyIfNull );

    /**
     * Converts a Collection of instances of type {@link ubic.gemma.model.genome.gene.GeneProductValueObject} to this
     * DAO's entity.
     */
    public void geneProductValueObjectToEntityCollection( java.util.Collection<GeneProductValueObject> instances );

    /**
     * 
     */
    public java.util.Collection getGenesByName( java.lang.String search );

    /**
     * 
     */
    public java.util.Collection getGenesByNcbiId( java.lang.String search );

    public Collection<GeneProduct> load( Collection<Long> ids );

    /**
     * Converts this DAO's entity to an object of type {@link ubic.gemma.model.genome.gene.GeneProductValueObject}.
     */
    public ubic.gemma.model.genome.gene.GeneProductValueObject toGeneProductValueObject(
            ubic.gemma.model.genome.gene.GeneProduct entity );

    /**
     * Copies the fields of the specified entity to the target value object. This method is similar to
     * toGeneProductValueObject(), but it does not handle any attributes in the target value object that are "read-only"
     * (as those do not have setter methods exposed).
     */
    public void toGeneProductValueObject( ubic.gemma.model.genome.gene.GeneProduct sourceEntity,
            ubic.gemma.model.genome.gene.GeneProductValueObject targetVO );

    /**
     * Converts this DAO's entity to a Collection of instances of type
     * {@link ubic.gemma.model.genome.gene.GeneProductValueObject}.
     */
    public void toGeneProductValueObjectCollection( java.util.Collection<GeneProduct> entities );

    public GeneProduct thaw( GeneProduct existing );

}
