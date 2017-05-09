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
package ubic.gemma.persistence.service.expression.bioAssayData;

import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;

/**
 * @see ubic.gemma.model.expression.bioAssayData.DesignElementDataVector
 */
@Repository
public interface DesignElementDataVectorDao<T extends DesignElementDataVector> {

    /**
     * 
     */
    public java.lang.Integer countAll();

    /**
     * Creates a new instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector and adds from the
     * passed in <code>entities</code> collection
     * 
     * @param entities the collection of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector instances to
     *        create.
     * @return the created instances.
     */
    public java.util.Collection<T> create( java.util.Collection<T> entities );

    /**
     * Creates an instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector and adds it to the
     * persistent store.
     */
    public T create( T designElementDataVector );

    /**
     * Loads an instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector from the persistent store.
     */
    public T load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.expression.bioAssayData.DesignElementDataVector}.
     * 
     * @return the loaded entities.
     */
    public java.util.Collection<? extends T> loadAll();

    /**
     * Removes the instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector having the given
     * <code>identifier</code> from the persistent store.
     */
    public void remove( java.lang.Long id );

    /**
     * Removes all entities in the given <code>entities<code> collection.
     */
    public void remove( java.util.Collection<T> entities );

    /**
     * Removes the instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector from the persistent
     * store.
     */
    public void remove( T designElementDataVector );

    /**
     * 
     */
    public void thaw( java.util.Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * <p>
     * Thaws associations of the given DesignElementDataVector
     * </p>
     */
    public void thaw( T designElementDataVector );

    /**
     * Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    public void update( java.util.Collection<T> entities );

    /**
     * Updates the <code>designElementDataVector</code> instance in the persistent store.
     */
    public void update( T designElementDataVector );

}
