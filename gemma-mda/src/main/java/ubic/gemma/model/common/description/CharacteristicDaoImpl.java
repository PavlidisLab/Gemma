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

import org.apache.commons.lang.time.StopWatch;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.association.phenotype.PhenotypeAssociationImpl;
import ubic.gemma.model.expression.experiment.ExperimentalFactorImpl;

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
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByParentClass(java.lang.Class)
     */
    @Override
    protected Map handleFindByParentClass( Class parentClass ) throws Exception {
        String field = "characteristics";
        if ( parentClass == ExperimentalFactorImpl.class )
            field = "category";
        else if ( parentClass == Gene2GOAssociationImpl.class ) field = "ontologyEntry";

        final String queryString = "select parent, char from " + parentClass.getSimpleName() + " as parent "
                + "inner join parent." + field + " as char";

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
    protected Collection<Characteristic> handleFindByUri( Collection<String> uris ) throws Exception {
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
    protected Collection<Characteristic> handleFindByUri( String searchString ) throws Exception {
        final String queryString = "select char from VocabCharacteristicImpl as char where  char.valueUri = :search";
        return getHibernateTemplate().findByNamedParam( queryString, "search", searchString );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindByvalue(java.lang.String)
     */
    @Override
    protected Collection<Characteristic> handleFindByValue( String search ) throws Exception {
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
     * @see ubic.gemma.model.common.description.CharacteristicDaoBase#handleFindParents(java.lang.Class,
     * java.util.Collection)
     */
    @Override
    protected Map handleGetParents( Class parentClass, Collection<Characteristic> characteristics ) throws Exception {
        Collection<Characteristic> batch = new HashSet<Characteristic>();
        Map<Characteristic, Object> charToParent = new HashMap<Characteristic, Object>();
        if ( characteristics == null || characteristics.size() == 0 ) {
            return charToParent;
        }
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

    /**
     * @param parentClass
     * @param characteristics
     * @param charToParent
     */
    private void batchGetParents( Class<?> parentClass, Collection<Characteristic> characteristics,
            Map<Characteristic, Object> charToParent ) {
        if ( characteristics.isEmpty() ) return;

        String field = "characteristics";
        if ( parentClass == ExperimentalFactorImpl.class )
            field = "category";
        else if ( parentClass == Gene2GOAssociationImpl.class ) field = "ontologyEntry";
        else if ( parentClass == PhenotypeAssociationImpl.class){
            field = "phenotypes";            
        }

        final String queryString = "select parent, char from " + parentClass.getSimpleName() + " as parent "
                + "inner join parent." + field + " as char " + "where char in (:chars)";

        for ( Object o : getHibernateTemplate().findByNamedParam( queryString, "chars", characteristics ) ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
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

}