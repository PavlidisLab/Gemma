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

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * <p>
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>Characteristic</code>.
 * </p>
 * 
 * @see Characteristic
 */
public abstract class CharacteristicDaoBase extends HibernateDaoSupport implements CharacteristicDao {

    /**
     * @see CharacteristicDao#create(int, Collection)
     */
    @Override
    public Collection<? extends Characteristic> create( final Collection<? extends Characteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Characteristic.create - 'entities' can not be null" );
        }

        for ( Iterator<? extends Characteristic> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
            create( entityIterator.next() );
        }

        return entities;
    }

    @Override
    public Collection<? extends Characteristic> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from CharacteristicImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see CharacteristicDao#create(int transform, Characteristic)
     */
    @Override
    public Characteristic create( final Characteristic characteristic ) {
        if ( characteristic == null ) {
            throw new IllegalArgumentException( "Characteristic.create - 'characteristic' can not be null" );
        }
        this.getHibernateTemplate().save( characteristic );
        return characteristic;
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
     * @see CharacteristicDao#load(int, Long)
     */

    @Override
    public Characteristic load( final Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Characteristic.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( CharacteristicImpl.class, id );
        return ( Characteristic ) entity;
    }

    /**
     * @see CharacteristicDao#loadAll(int)
     */

    @Override
    @SuppressWarnings("unchecked")
    public Collection<Characteristic> loadAll() {
        final Collection<?> results = this.getHibernateTemplate().loadAll( CharacteristicImpl.class );
        return ( Collection<Characteristic> ) results;
    }

    /**
     * @see CharacteristicDao#remove(Long)
     */

    @Override
    public void remove( Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "Characteristic.remove - 'id' can not be null" );
        }
        Characteristic entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#remove(Collection)
     */

    @Override
    public void remove( Collection<? extends Characteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Characteristic.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see CharacteristicDao#remove(Characteristic)
     */
    @Override
    public void remove( Characteristic characteristic ) {
        if ( characteristic == null ) {
            throw new IllegalArgumentException( "Characteristic.remove - 'characteristic' can not be null" );
        }
        this.getHibernateTemplate().delete( characteristic );
    }

    /**
     * @see ubic.gemma.model.common.SecurableDao#update(Collection)
     */

    @Override
    public void update( final Collection<? extends Characteristic> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "Characteristic.update - 'entities' can not be null" );
        }

        for ( Iterator<? extends Characteristic> entityIterator = entities.iterator(); entityIterator.hasNext(); ) {
            update( entityIterator.next() );
        }

    }

    /**
     * @see CharacteristicDao#update(Characteristic)
     */
    @Override
    public void update( Characteristic characteristic ) {
        if ( characteristic == null ) {
            throw new IllegalArgumentException( "Characteristic.update - 'characteristic' can not be null" );
        }
        this.getHibernateTemplate().update( characteristic );
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