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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationImpl;
import ubic.gemma.model.expression.biomaterial.TreatmentImpl;
import ubic.gemma.model.expression.experiment.ExperimentalFactorImpl;
import ubic.gemma.util.EntityUtils;
import ubic.gemma.util.NativeQueryUtils;

/**
 * @author Luke
 * @author Paul
 * @see ubic.gemma.model.common.description.Characteristic
 * @version $Id$
 */
@Repository
public class CharacteristicDaoImpl extends ubic.gemma.model.common.description.CharacteristicDaoBase {

    private static Log log = LogFactory.getLog( CharacteristicDaoImpl.class );

    private static final int BATCH_SIZE = 1000;

    @Autowired
    public CharacteristicDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDao#browse(java.lang.Integer, java.lang.Integer)
     */
    @Override
    public List<Characteristic> browse( Integer start, Integer limit ) {
        Query query = this.getSession().createQuery( "from CharacteristicImpl where value not like 'GO_%'" );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDao#browse(java.lang.Integer, java.lang.Integer,
     * java.lang.String, boolean)
     */
    @Override
    public List<Characteristic> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        Query query = this.getSession().createQuery(
                "from CharacteristicImpl where value not like 'GO_%' order by " + orderField + " "
                        + ( descending ? "desc" : "" ) );
        query.setMaxResults( limit );
        query.setFirstResult( start );
        return query.list();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDao#count()
     */
    @Override
    public Integer count() {
        return ( ( Long ) getHibernateTemplate()
                .find( "select count(*) from CharacteristicImpl where value not like 'GO_%'" ).iterator().next() )
                .intValue();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByUri(java.util.Collection, java.util.Collection)
     */
    @Override
    public Collection<Characteristic> findByUri( Collection<Class<?>> classes, Collection<String> characteristicUris ) {
        HashSet<Characteristic> result = new HashSet<Characteristic>();
        if ( classes.isEmpty() ) {
            return result;
        }

        if ( characteristicUris.isEmpty() ) {
            throw new IllegalArgumentException( "No uris were provided" );
        }

        for ( Class<?> clazz : classes ) {

            String field = getCharactersticFieldName( clazz );

            final String queryString = "select char from " + EntityUtils.getImplClass( clazz ).getSimpleName()
                    + " as parent " + " join parent." + field + " as char where char.valueUri in  (:uriStrings) ";

            result.addAll( getHibernateTemplate().findByNamedParam( queryString, "uriStrings", characteristicUris ) );

        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByUri(java.util.Collection, java.lang.String)
     */
    @Override
    public Collection<Characteristic> findByUri( Collection<Class<?>> classesToFilterOn, String uriString ) {
        HashSet<Characteristic> result = new HashSet<Characteristic>();

        if ( classesToFilterOn.isEmpty() ) {
            return result;
        }

        for ( Class<?> clazz : classesToFilterOn ) {

            String field = getCharactersticFieldName( clazz );

            final String queryString = "select char from " + EntityUtils.getImplClass( clazz ).getSimpleName()
                    + " as parent " + " join parent." + field + " as char " + "where char.valueUri = :uriString";

            result.addAll( getHibernateTemplate().findByNamedParam( queryString, "uriString", uriString ) );

        }

        return result;

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByParentClass(java.lang.Class)
     */
    @Override
    protected Map<Characteristic, Object> handleFindByParentClass( Class<?> parentClass ) {
        String field = getCharactersticFieldName( parentClass );

        final String queryString = "select parent, char from "
                + EntityUtils.getImplClass( parentClass ).getSimpleName() + " as parent " + "inner join parent."
                + field + " as char";

        Map<Characteristic, Object> charToParent = new HashMap<Characteristic, Object>();
        for ( Object o : getHibernateTemplate().find( queryString ) ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
        return charToParent;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByUri(java.util.Collection)
     */
    @Override
    protected Collection<Characteristic> handleFindByUri( Collection<String> uris ) {
        int batchSize = 1000; // to avoid HQL parser barfing
        Collection<String> batch = new HashSet<String>();
        Collection<Characteristic> results = new HashSet<Characteristic>();
        final String queryString = "from VocabCharacteristicImpl where valueUri in (:uris)";

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

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByUri(java.lang.String)
     */
    @Override
    protected Collection<Characteristic> handleFindByUri( String searchString ) {
        final String queryString = "select char from VocabCharacteristicImpl as char where  char.valueUri = :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", searchString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByvalue(java.lang.String)
     */
    @Override
    protected Collection<Characteristic> handleFindByValue( String search ) {
        final String queryString = "select distinct char from CharacteristicImpl as char where char.value like :search";
        StopWatch timer = new StopWatch();
        timer.start();

        try {
            return getHibernateTemplate().findByNamedParam( queryString, "search", search + "%" );
        } finally {
            if ( timer.getTime() > 100 ) {
                log.info( "Characteristic search for " + search + ": " + timer.getTime() + "ms" );
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDao#findByValue(java.util.Collection, java.lang.String)
     */
    @Override
    public Collection<Characteristic> findByValue( Collection<Class<?>> classes, String string ) {
        HashSet<Characteristic> result = new HashSet<Characteristic>();

        if ( classes.isEmpty() ) {
            return result;
        }

        for ( Class<?> clazz : classes ) {

            String field = getCharactersticFieldName( clazz );

            final String queryString = "select char from " + EntityUtils.getImplClass( clazz ).getSimpleName()
                    + " as parent " + " join parent." + field + " as char " + "where char.value like :v";

            result.addAll( getHibernateTemplate().findByNamedParam( queryString, "v", string + "%" ) );

        }

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindParents(java.lang.Class,
     * java.util.Collection)
     */
    @Override
    protected Map<Characteristic, Object> handleGetParents( Class<?> parentClass,
            Collection<Characteristic> characteristics ) {

        Map<Characteristic, Object> charToParent = new HashMap<Characteristic, Object>();
        if ( characteristics == null || characteristics.size() == 0 ) {
            return charToParent;
        }

        // FIXME temporary debugging code
        Collection<String> uris = new HashSet<String>();
        for ( Characteristic c : characteristics ) {
            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                if ( vc.getValueUri() == null ) continue;
                uris.add( vc.getValueUri() );
            }
        }

        log.info( "For class=" + parentClass.getSimpleName() + ": " + characteristics.size()
                + " Characteristics have URIS:\n" + StringUtils.join( uris, "\n" ) );

        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Characteristic> batch : new BatchIterator<Characteristic>( characteristics, BATCH_SIZE ) ) {
            batchGetParents( parentClass, batch, charToParent );
        }

        if ( timer.getTime() > 1000 ) {
            log.info( "Fetch parents of characteristics: " + timer.getTime() + "ms for " + characteristics.size()
                    + " elements for class=" + parentClass.getSimpleName() );
        }

        return charToParent;
    }

    /**
     * @param parentClass
     * @param characteristics
     * @param charToParent
     */
    private void batchGetParents( Class<?> parentClass, Collection<Characteristic> characteristics,
            Map<Characteristic, Object> charToParent ) {
        if ( characteristics.isEmpty() ) return;

        String field = getCharactersticFieldName( parentClass );

        final String queryString = "select parent, char from "
                + EntityUtils.getImplClass( parentClass ).getSimpleName() + " as parent " + " join parent." + field
                + " as char " + "where char  in (:chars)";

        if ( characteristics.size() > 500 ) {
            log.info( "Characteristic->parent query: "
                    + NativeQueryUtils.toSql( this.getHibernateTemplate(), queryString ) );
        }

        for ( Object o : getHibernateTemplate().findByNamedParam( queryString, "chars", characteristics ) ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
    }

    /**
     * @param parentClass
     * @return
     */
    private String getCharactersticFieldName( Class<?> parentClass ) {
        String field = "characteristics";
        if ( parentClass.isAssignableFrom( ExperimentalFactorImpl.class ) )
            field = "category";
        else if ( parentClass.isAssignableFrom( Gene2GOAssociationImpl.class ) )
            field = "ontologyEntry";
        else if ( parentClass.isAssignableFrom( PhenotypeAssociationImpl.class ) ) {
            field = "phenotypes";
        } else if ( parentClass.isAssignableFrom( TreatmentImpl.class ) ) {
            field = "action";
        }
        return field;
    }

}