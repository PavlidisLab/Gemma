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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type
 * <code>ubic.gemma.model.common.description.VocabCharacteristic</code>.
 *
 * @see ubic.gemma.model.common.description.VocabCharacteristic
 */
@Repository
public class VocabCharacteristicDaoImpl extends AbstractDao<VocabCharacteristic> implements VocabCharacteristicDao {

    private static final int BATCH_SIZE = 1000;

    @Autowired
    public VocabCharacteristicDaoImpl( SessionFactory sessionFactory ) {
        super( VocabCharacteristic.class, sessionFactory );
    }

    @Override
    public java.util.Map<Characteristic, Object> findByParentClass( final java.lang.Class<?> parentClass ) {
        return this.handleFindByParentClass( parentClass );
    }

    @Override
    public java.util.Collection<Characteristic> findByUri( final String searchString ) {
        return this.handleFindByUri( searchString );
    }

    @Override
    public java.util.Collection<Characteristic> findByUri( final Collection<String> uris ) {
        return this.handleFindByUri( uris );
    }

    @Override
    public java.util.Collection<Characteristic> findByValue( final java.lang.String search ) {
        return this.handleFindByValue( search );
    }

    @Override
    public Map<Characteristic, Object> getParents( final java.lang.Class<?> parentClass,
            final java.util.Collection<Characteristic> characteristics ) {
        return this.handleGetParents( parentClass, characteristics );
    }

    private Map<Characteristic, Object> handleFindByParentClass( Class<?> parentClass ) {
        String field = "characteristics";
        if ( parentClass == ExperimentalFactor.class )
            field = "category";
        else if ( parentClass == Gene2GOAssociationImpl.class )
            field = "ontologyEntry";

        final String queryString =
                "select parent, char from " + parentClass.getSimpleName() + " as parent " + "inner join parent." + field
                        + " as char";

        Map<Characteristic, Object> charToParent = new HashMap<>();
        for ( Object o : getHibernateTemplate().find( queryString ) ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
        return charToParent;
    }

    private Collection<Characteristic> handleFindByUri( Collection<String> uris ) {
        int batchSize = 1000; // to avoid HQL parser barfing
        Collection<String> batch = new HashSet<>();
        Collection<Characteristic> results = new HashSet<>();
        final String queryString = "from VocabCharacteristic where valueUri in (:uris)";

        for ( String uri : uris ) {
            batch.add( uri );
            if ( batch.size() >= batchSize ) {
                results.addAll( getHibernateTemplate().findByNamedParam( queryString, "uris", batch ) );
                batch.clear();
            }
        }
        if ( batch.size() > 0 ) {
            results.addAll( getHibernateTemplate().findByNamedParam( queryString, "uris", batch ) );
        }
        return results;
    }

    private Collection<Characteristic> handleFindByUri( String searchString ) {
        final String queryString = "select char from VocabCharacteristic as char where  char.valueUri = :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", searchString );
    }

    private Collection<Characteristic> handleFindByValue( String search ) {
        final String queryString = "select distinct char from Characteristic as char where char.value like :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", search );
    }

    private Map<Characteristic, Object> handleGetParents( Class<?> parentClass,
            Collection<Characteristic> characteristics ) {
        Collection<Characteristic> batch = new HashSet<>();
        Map<Characteristic, Object> charToParent = new HashMap<>();
        for ( Characteristic c : characteristics ) {
            batch.add( c );
            if ( batch.size() == BATCH_SIZE ) {
                batchGetParents( parentClass, batch, charToParent );
                batch.clear();
            }
        }
        batchGetParents( parentClass, batch, charToParent );
        return charToParent;
    }

    private void batchGetParents( Class<?> parentClass, Collection<Characteristic> characteristics,
            Map<Characteristic, Object> charToParent ) {
        if ( characteristics.isEmpty() )
            return;

        String field = "characteristics";
        if ( parentClass == ExperimentalFactor.class )
            field = "category";
        else if ( parentClass == Gene2GOAssociationImpl.class )
            field = "ontologyEntry";

        final String queryString =
                "select parent, char from " + parentClass.getSimpleName() + " as parent " + "inner join parent." + field
                        + " as char " + "where char in (:chars)";

        for ( Object o : getHibernateTemplate().findByNamedParam( queryString, "chars", characteristics ) ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
    }

    @Override
    public void thaw( VocabCharacteristic entity ) {
    }

    @Override
    public VocabCharacteristic find( VocabCharacteristic entity ) {
        return load( entity.getId() );
    }
}