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
package ubic.gemma.model.common.measurement;


/**
 * @see ubic.gemma.model.common.measurement.Measurement
 */
public interface MeasurementDao {
    /**
     * This constant is used as a transformation flag; entities can be converted automatically into value objects or
     * other types, different methods in a class implementing this interface support this feature: look for an
     * <code>int</code> parameter called <code>transform</code>.
     * <p/>
     * This specific flag denotes no transformation will occur.
     */
    public final static int TRANSFORM_NONE = 0;

    /**
     * Creates a new instance of ubic.gemma.model.common.measurement.Measurement and adds from the passed in
     * <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.common.measurement.Measurement instances to create.
     * @return the created instances.
     */
    public java.util.Collection<Measurement> create( java.util.Collection<Measurement> entities );

    /**
     * Creates an instance of ubic.gemma.model.common.measurement.Measurement and adds it to the persistent store.
     */
    public ubic.gemma.model.common.measurement.Measurement create(
            ubic.gemma.model.common.measurement.Measurement measurement );

    /**
     * Loads an instance of ubic.gemma.model.common.measurement.Measurement from the persistent store.
     */
    public ubic.gemma.model.common.measurement.Measurement load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.common.measurement.Measurement}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection<Measurement> loadAll();

    /**
     * Removes the instance of ubic.gemma.model.common.measurement.Measurement having the given <code>identifier</code>
     * from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection<Measurement> entities );

    /**
     * Removes the instance of ubic.gemma.model.common.measurement.Measurement from the persistent store.
     */
    public void remove( ubic.gemma.model.common.measurement.Measurement measurement );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection<Measurement> entities );

    /**
     * Updates the <code>measurement</code> instance in the persistent store.
     */
    public void update( ubic.gemma.model.common.measurement.Measurement measurement );

}
