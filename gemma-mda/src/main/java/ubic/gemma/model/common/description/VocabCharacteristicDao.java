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

import java.util.Map;

import ubic.gemma.persistence.BaseDao;

/**
 * @see ubic.gemma.model.common.description.VocabCharacteristic
 */
public interface VocabCharacteristicDao extends BaseDao<VocabCharacteristic> {

    /**
     * <p>
     * Finds all characteristics whose parent object is of the specified class. Returns a map of characteristics to
     * parent objects.
     * </p>
     */
    public Map<Characteristic, Object> findByParentClass( java.lang.Class<?> parentClass );

    /**
     * 
     */
    public java.util.Collection<Characteristic> findByUri( java.lang.String searchString );

    /**
     * 
     */
    public java.util.Collection<Characteristic> findByUri( java.util.Collection<String> uris );

    /**
     * <p>
     * Finds all Characteristics whose value match the given search term
     * </p>
     */
    public java.util.Collection<Characteristic> findByValue( java.lang.String search );

    /**
     * <p>
     * Returns a map of the specified characteristics to their parent objects.
     * </p>
     */
    public java.util.Map<Characteristic, Object> getParents( java.lang.Class<?> parentClass,
            java.util.Collection<Characteristic> characteristics );
}
