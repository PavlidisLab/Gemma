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
    public ubic.gemma.model.common.description.Characteristic create(
            final ubic.gemma.model.common.description.Characteristic c ) {
        try {
            return this.handleCreate( c );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.create(ubic.gemma.model.common.description.Characteristic c)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#delete(java.lang.Long)
     */
    public void delete( final java.lang.Long id ) {
        try {
            this.handleDelete( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.delete(java.lang.Long id)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#delete(ubic.gemma.model.common.description.Characteristic)
     */
    public void delete( final ubic.gemma.model.common.description.Characteristic c ) {
        try {
            this.handleDelete( c );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.delete(ubic.gemma.model.common.description.Characteristic c)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByParentClass(java.lang.Class)
     */
    public java.util.Map findByParentClass( final java.lang.Class parentClass ) {
        try {
            return this.handleFindByParentClass( parentClass );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.findByParentClass(java.lang.Class parentClass)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByUri(java.lang.String)
     */
    public java.util.Collection<Characteristic> findByUri( final java.lang.String searchString ) {
        try {
            return this.handleFindByUri( searchString );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.findByUri(java.lang.String searchString)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByUri(java.util.Collection)
     */
    public java.util.Collection<Characteristic> findByUri( final java.util.Collection uris ) {
        try {
            return this.handleFindByUri( uris );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.findByUri(java.util.Collection uris)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#findByValue(java.lang.String)
     */
    public java.util.Collection findByValue( final java.lang.String search ) {
        try {
            return this.handleFindByValue( search );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.findByValue(java.lang.String search)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#getParents(java.util.Collection)
     */
    public java.util.Map getParents( final java.util.Collection characteristics ) {
        try {
            return this.handleGetParents( characteristics );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.getParents(java.util.Collection characteristics)' --> "
                            + th, th );
        }
    }

    /**
     * @see ubic.gemma.model.common.description.CharacteristicService#load(java.lang.Long)
     */
    public ubic.gemma.model.common.description.Characteristic load( final java.lang.Long id ) {
        try {
            return this.handleLoad( id );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.load(java.lang.Long id)' --> "
                            + th, th );
        }
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
    public void update( final ubic.gemma.model.common.description.Characteristic c ) {
        try {
            this.handleUpdate( c );
        } catch ( Throwable th ) {
            throw new ubic.gemma.model.common.description.CharacteristicServiceException(
                    "Error performing 'ubic.gemma.model.common.description.CharacteristicService.update(ubic.gemma.model.common.description.Characteristic c)' --> "
                            + th, th );
        }
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
            ubic.gemma.model.common.description.Characteristic c ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(java.lang.Long)}
     */
    protected abstract void handleDelete( java.lang.Long id ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #delete(ubic.gemma.model.common.description.Characteristic)}
     */
    protected abstract void handleDelete( ubic.gemma.model.common.description.Characteristic c )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByParentClass(java.lang.Class)}
     */
    protected abstract java.util.Map handleFindByParentClass( java.lang.Class parentClass ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUri(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.lang.String searchString )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByUri(java.util.Collection)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByUri( java.util.Collection<String> uris )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #findByValue(java.lang.String)}
     */
    protected abstract java.util.Collection<Characteristic> handleFindByValue( java.lang.String search )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getParent(ubic.gemma.model.common.description.Characteristic)}
     */
    protected abstract java.lang.Object handleGetParent(
            ubic.gemma.model.common.description.Characteristic characteristic ) throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #getParents(java.util.Collection)}
     */
    protected abstract java.util.Map handleGetParents( java.util.Collection<Characteristic> characteristics )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #load(java.lang.Long)}
     */
    protected abstract ubic.gemma.model.common.description.Characteristic handleLoad( java.lang.Long id )
            throws java.lang.Exception;

    /**
     * Performs the core logic for {@link #update(ubic.gemma.model.common.description.Characteristic)}
     */
    protected abstract void handleUpdate( ubic.gemma.model.common.description.Characteristic c )
            throws java.lang.Exception;

}