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

/**
 * @see ubic.gemma.model.common.auditAndSecurity.AuditTrail
 */
public interface AuditTrailDao {
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
     * Add the given event to the audit trail of the given Auditable entity
     * </p>
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditEvent addEvent( ubic.gemma.model.common.Auditable auditable,
            ubic.gemma.model.common.auditAndSecurity.AuditEvent auditEvent );

    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.common.auditAndSecurity.AuditTrail)} with an additional
     * flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the returned entity
     * will <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here then the result
     * <strong>WILL BE</strong> passed through an operation which can optionally transform the entities (into value
     * objects for example). By default, transformation does not occur.
     * </p>
     */
    public java.util.Collection create( int transform, java.util.Collection entities );

    /**
     * <p>
     * Does the same thing as {@link #create(ubic.gemma.model.common.auditAndSecurity.AuditTrail)} with an additional
     * flag called <code>transform</code>. If this flag is set to <code>TRANSFORM_NONE</code> then the returned entity
     * will <strong>NOT</strong> be transformed. If this flag is any of the other constants defined here then the result
     * <strong>WILL BE</strong> passed through an operation which can optionally transform the entity (into a value
     * object for example). By default, transformation does not occur.
     * </p>
     */
    public Object create( int transform, ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );

    /**
     * Creates a new instance of ubic.gemma.model.common.auditAndSecurity.AuditTrail and adds from the passed in
     * <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.common.auditAndSecurity.AuditTrail instances to create.
     * @return the created instances.
     */
    public java.util.Collection create( java.util.Collection entities );

    /**
     * Creates an instance of ubic.gemma.model.common.auditAndSecurity.AuditTrail and adds it to the persistent store.
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditTrail create(
            ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );

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
     * Loads an instance of ubic.gemma.model.common.auditAndSecurity.AuditTrail from the persistent store.
     */
    public ubic.gemma.model.common.auditAndSecurity.AuditTrail load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.common.auditAndSecurity.AuditTrail}.
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
     * Removes the instance of ubic.gemma.model.common.auditAndSecurity.AuditTrail having the given
     * <code>identifier</code> from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection entities );

    /**
     * Removes the instance of ubic.gemma.model.common.auditAndSecurity.AuditTrail from the persistent store.
     */
    public void remove( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );

    /**
     * 
     */
    public void thaw( ubic.gemma.model.common.Auditable auditable );

    /**
     * <p>
     * thaws the given audit trail
     * </p>
     */
    public void thaw( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection entities );

    /**
     * Updates the <code>auditTrail</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.common.auditAndSecurity.AuditTrail auditTrail );

}
