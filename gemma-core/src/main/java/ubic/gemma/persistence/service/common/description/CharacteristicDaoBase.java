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
package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.VoEnabledDao;

import java.util.Collection;
import java.util.Map;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Characteristic</code>.
 * </p>
 *
 * @see Characteristic
 */
public abstract class CharacteristicDaoBase extends VoEnabledDao<Characteristic, CharacteristicValueObject> implements CharacteristicDao {

    public CharacteristicDaoBase( SessionFactory sessionFactory ) {
        super( Characteristic.class, sessionFactory );
    }

    /**
     * @see CharacteristicDao#findByParentClass(Class)
     */
    @Override
    public Map<Characteristic, Object> findByParentClass( final Class<?> parentClass ) {
        return this.handleFindByParentClass( parentClass );

    }

    /**
     * @see CharacteristicDao#findByUri(String)
     */
    @Override
    public Collection<Characteristic> findByUri( final String searchString ) {
        return this.handleFindByUri( searchString );

    }

    /**
     * @see CharacteristicDao#findByUri(Collection)
     */
    @Override
    public Collection<Characteristic> findByUri( final Collection<String> uris ) {
        return this.handleFindByUri( uris );

    }

    /**
     * @see CharacteristicDao#findByValue(String)
     */
    @Override
    public Collection<Characteristic> findByValue( final String search ) {
        return this.handleFindByValue( search );

    }

    /**
     * @see CharacteristicDao#getParents(Class, Collection)
     */
    @Override
    public Map<Characteristic, Object> getParents( final Class<?> parentClass,
            final Collection<Characteristic> characteristics ) {
        return this.handleGetParents( parentClass, characteristics );
    }

    /**
     * Performs the core logic for {@link #findByParentClass(Class)}
     */
    protected abstract Map<Characteristic, Object> handleFindByParentClass( Class<?> parentClass );

    /**
     * Performs the core logic for {@link #findByUri(String)}
     */
    protected abstract Collection<Characteristic> handleFindByUri( String searchString );

    /**
     * Performs the core logic for {@link #findByUri(Collection)}
     */
    protected abstract Collection<Characteristic> handleFindByUri( Collection<String> uris );

    /**
     * Performs the core logic for {@link #findByValue(String)}
     */
    protected abstract Collection<Characteristic> handleFindByValue( String search );

    /**
     * Performs the core logic for {@link #getParents(Class, Collection)}
     */
    protected abstract Map<Characteristic, Object> handleGetParents( Class<?> parentClass,
            Collection<Characteristic> characteristics );

}