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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.description.Characteristic;

/**
 * Service base class for <code>CharacteristicService</code>, provides access to all
 * services and entities referenced by this service.
 * 
 * @see CharacteristicService
 */
public abstract class CharacteristicServiceBase implements CharacteristicService {

    @Autowired
    private CharacteristicDao characteristicDao;

    /**
     * @see CharacteristicService#create(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    @Transactional
    public Characteristic create( final Characteristic c ) {
        return this.handleCreate( c );
    }

    /**
     * @see CharacteristicService#delete(java.lang.Long)
     */
    @Override
    @Transactional
    public void delete( final java.lang.Long id ) {
        this.handleDelete( id );
    }

    /**
     * @see CharacteristicService#delete(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    @Transactional
    public void delete( final Characteristic c ) {
        this.handleDelete( c );
    }

    /**
     * @see CharacteristicService#findByUri(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Characteristic> findByUri( final java.lang.String searchString ) {
        return this.handleFindByUri( searchString );
    }

    /**
     * @see CharacteristicService#findByUri(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Characteristic> findByUri( final java.util.Collection<String> uris ) {
        return this.handleFindByUri( uris );
    }

    /**
     * @see CharacteristicService#findByValue(java.lang.String)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Collection<Characteristic> findByValue( final java.lang.String search ) {
        return this.handleFindByValue( search );
    }

    /**
     * @see CharacteristicService#getParents(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Map<Characteristic, Object> getParents( final java.util.Collection<Characteristic> characteristics ) {
        return this.handleGetParents( characteristics );

    }

    /**
     * @see CharacteristicService#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public ubic.gemma.model.common.description.Characteristic load( final java.lang.Long id ) {
        return this.handleLoad( id );
    }

    /**
     * Sets the reference to <code>characteristic</code>'s DAO.
     */
    public void setCharacteristicDao( CharacteristicDao characteristicDao ) {
        this.characteristicDao = characteristicDao;
    }

    /**
     * @see CharacteristicService#update(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    @Transactional
    public void update( final ubic.gemma.model.common.description.Characteristic c ) {
        this.handleUpdate( c );
    }

    /**
     * Gets the reference to <code>characteristic</code>'s DAO.
     */
    protected CharacteristicDao getCharacteristicDao() {
        return this.characteristicDao;
    }

    /**
     * Performs the core logic for {@link #create(ubic.gemma.model.common.description.Characteristic)}
     */
    protected abstract ubic.gemma.model.common.description.Characteristic handleCreate(
            ubic.gemma.model.common.description.Characteristic c );

    /**
     * Performs the core logic for {@link #delete(java.lang.Long)}
     */
    protected abstract void handleDelete( java.lang.Long id );

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.common.description.Characteristic)}
     */
    protected abstract void handleDelete( ubic.gemma.model.common.description.Characteristic c );

    /**
     * Performs the core logic for {@link #findByUri(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.lang.String searchString );

    /**
     * Performs the core logic for {@link #findByUri(java.util.Collection)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.util.Collection<String> uris );

    /**
     * Performs the core logic for {@link #findByValue(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByValue( java.lang.String search );

    /**
     * Performs the core logic for {@link #getParents(java.util.Collection)}
     */
    protected abstract java.util.Map<Characteristic, Object> handleGetParents(
            java.util.Collection<Characteristic> characteristics );

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.common.description.Characteristic handleLoad( java.lang.Long id );

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.description.Characteristic)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.description.Characteristic c );

}