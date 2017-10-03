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

    java.lang.Integer countAll();

    /**
     * Creates a new instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector and adds from the
     * passed in <code>entities</code> collection
     *
     * @param entities the collection of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector instances to
     *                 create.
     * @return the created instances.
     */
    java.util.Collection<T> create( java.util.Collection<T> entities );

    /**
     * @param designElementDataVector DE data vector
     * @return Creates an instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector and adds it to the
     * persistent store.
     */
    T create( T designElementDataVector );

    /**
     * @param id id
     * @return Loads an instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector from the persistent store.
     */
    T load( java.lang.Long id );

    /**
     * Loads all entities of type {@link ubic.gemma.model.expression.bioAssayData.DesignElementDataVector}.
     *
     * @return the loaded entities.
     */
    java.util.Collection<? extends T> loadAll();

    /**
     * @param id Removes the instance of ubic.gemma.model.expression.bioAssayData.DesignElementDataVector having the given
     *           <code>identifier</code> from the persistent store.
     */
    void remove( java.lang.Long id );

    void remove( java.util.Collection<T> entities );

    void remove( T designElementDataVector );

    void thaw( java.util.Collection<? extends DesignElementDataVector> designElementDataVectors );

    /**
     * @param designElementDataVector Thaws associations of the given DesignElementDataVector
     */
    void thaw( T designElementDataVector );

    /**
     * @param entities Updates all instances in the <code>entities</code> collection in the persistent store.
     */
    void update( java.util.Collection<T> entities );

    /**
     * @param designElementDataVector Updates the <code>designElementDataVector</code> instance in the persistent store.
     */
    void update( T designElementDataVector );

}
