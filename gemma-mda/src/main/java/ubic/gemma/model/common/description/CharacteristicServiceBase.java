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

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service base class for <code>ubic.gemma.model.common.description.CharacteristicService</code>, provides access to all
 * services and entities referenced by this service.
 * 
 * @see ubic.gemma.model.common.description.CharacteristicService
 */
public abstract class CharacteristicServiceBase implements ubic.gemma.model.common.description.CharacteristicService {

    @Autowired
    private ubic.gemma.model.common.description.CharacteristicDao characteristicDao;

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#create(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    public Characteristic create( final Characteristic c ) {
        return this.handleCreate( c );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#delete(java.lang.Long)
     */
    @Override
    public void delete( final java.lang.Long id ) {
        this.handleDelete( id );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#delete(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    public void delete( final Characteristic c ) {
        this.handleDelete( c );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByUri(java.lang.String)
     */
    @Override
    public java.util.Collection<Characteristic> findByUri( final java.lang.String searchString ) {
        return this.handleFindByUri( searchString );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByUri(java.util.Collection)
     */
    @Override
    public java.util.Collection<Characteristic> findByUri( final java.util.Collection<String> uris ) {
        return this.handleFindByUri( uris );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByValue(java.lang.String)
     */
    @Override
    public java.util.Collection<Characteristic> findByValue( final java.lang.String search ) {
        return this.handleFindByValue( search );
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#getParents(java.util.Collection)
     */
    @Override
    public java.util.Map<Characteristic, Object> getParents( final java.util.Collection<Characteristic> characteristics ) {
        return this.handleGetParents( characteristics );

    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#load(java.lang.Long)
     */
    @Override
    public ubic.gemma.model.common.description.Characteristic load( final java.lang.Long id ) {
        return this.handleLoad( id );
    }

    /**
     * Sets the reference to <code>characteristic</code>'s DAO.
     */
    public void setCharacteristicDao( ubic.gemma.model.common.description.CharacteristicDao characteristicDao ) {
        this.characteristicDao = characteristicDao;
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#update(ubic.gemma.model.common.description.Characteristic)
     */
    @Override
    public void update( final ubic.gemma.model.common.description.Characteristic c ) {
        this.handleUpdate( c );
    }

    /**
     * Gets the reference to <code>characteristic</code>'s DAO.
     */
    protected ubic.gemma.model.common.description.CharacteristicDao getCharacteristicDao() {
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