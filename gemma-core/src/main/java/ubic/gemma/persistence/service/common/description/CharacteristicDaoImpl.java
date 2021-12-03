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

import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.association.Gene2GOAssociationImpl;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractVoEnabledDao;
import ubic.gemma.persistence.util.EntityUtils;

/**
 * @author Luke
 * @author Paul
 * @see    Characteristic
 */
@Repository
public class CharacteristicDaoImpl extends AbstractVoEnabledDao<Characteristic, CharacteristicValueObject>
        implements CharacteristicDao {

    private static final int BATCH_SIZE = 500;

    @Autowired
    public CharacteristicDaoImpl( SessionFactory sessionFactory ) {
        super( Characteristic.class, sessionFactory );
    }

    @Override
    public List<Characteristic> browse( Integer start, Integer limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Characteristic where value not like 'GO_%'" ).setMaxResults( limit )
                .setFirstResult( start ).list();
    }

    @Override
    public List<Characteristic> browse( Integer start, Integer limit, String orderField, boolean descending ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                "from Characteristic where value not like 'GO_%' order by " + orderField + " " + ( descending ? "desc" : "" ) ).setMaxResults( limit )
                .setFirstResult( start ).list();
    }

    @Override
    public Collection<? extends Characteristic> findByCategory( String query ) {

        //noinspection unchecked
        return this.getSession()
                .createQuery( "select distinct char from Characteristic as char where char.category like :search" )
                .setParameter( "search", query + "%" ).list();
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<Class<?>> classes, Collection<String> characteristicUris ) {

        Collection<Characteristic> result = new HashSet<>();

        if ( characteristicUris == null || characteristicUris.isEmpty() )
            return result;

        for ( Class<?> clazz : classes ) {
            String field = this.getCharacteristicFieldName( clazz );
            final String queryString = "select char from " + EntityUtils.getImplClass( clazz ).getSimpleName() + " as parent "
                    + " join parent." + field + " as char where char.valueUri in (:uriStrings) ";
            //noinspection unchecked
            result.addAll( this.getSessionFactory().getCurrentSession().createQuery( queryString )
                    .setParameterList( "uriStrings", characteristicUris ).list() );
        }

        return result;
    }

    @SuppressWarnings({ "rawtypes", "unchecked", "cast" })
    @Override
    public Map<Class<?>, Map<String, Collection<Long>>> findExperimentsByUris( Collection<String> uriStrings, Taxon t, int limit ) {
        Map<Class<?>, Map<String, Collection<Long>>> result = new HashMap<>();

        if ( uriStrings.isEmpty() )
            return result;

        // Note that the limit isn't strictly adhered to; we just stop querying when we have enough. We avoid duplicates
        Query q;
        Set<Long> seenIDs = new HashSet<>();

        // direct associations
        // language=HQL
        q = this.getEEsByUriQuery( "select distinct ee.id, c.valueUri from ExpressionExperiment ee "
                + "join ee.characteristics c", uriStrings, t, seenIDs );
        result.put( ExpressionExperiment.class, toEEsByUri( q.list(), seenIDs ) );

        if ( limit > 0 && seenIDs.size() >= limit ) {
            return result;
        }

        // via experimental factor
        // language=HQL
        q = this.getEEsByUriQuery( "select distinct ee.id, c.valueUri from ExpressionExperiment ee "
                + "join ee.experimentalDesign ed join ed.experimentalFactors ef "
                + "join ef.factorValues fv join fv.characteristics c", uriStrings, t, seenIDs );
        result.put( FactorValue.class, toEEsByUri( q.list(), seenIDs ) );

        if ( limit > 0 && seenIDs.size() >= limit ) {
            return result;
        }

        // via biomaterial
        // language=HQL
        q = this.getEEsByUriQuery( "select distinct ee.id, c.valueUri  from ExpressionExperiment ee "
                        + "join ee.bioAssays ba join ba.sampleUsed bm join bm.characteristics c",
                uriStrings, t, seenIDs );
        result.put( BioMaterial.class, toEEsByUri( q.list(), seenIDs ) );

        return result;
    }

    private Query getEEsByUriQuery( String query, Collection<String> uriStrings, Taxon t, Set<Long> seenIDs ) {
        query += " where c.valueUri in (:uriStrings) ";

        // don't retrieve EE IDs that we have already fetched otherwise
        if ( !seenIDs.isEmpty() ) {
            query += "and ee.id not in (:seenids) ";
        }

        // by taxon
        if ( t != null ) {
            query += "and ee.taxon = :t";
        }

        Query q = getSessionFactory().getCurrentSession().createQuery( query );
        q.setParameterList( "uriStrings", uriStrings );
        if ( !seenIDs.isEmpty() )
            q.setParameterList( "seenids", seenIDs );
        if ( t != null )
            q.setParameter( "t", t );
        return q;
    }

    private Map<String, Collection<Long>> toEEsByUri( List<Object[]> r, Set<Long> seenIDs ) {
        if ( r.isEmpty() ) {
            return Collections.emptyMap();
        }
        Map<String, Collection<Long>> c = new HashMap<>();
        for ( Object[] o : r ) {
            Long eeID = ( Long ) o[0];
            if ( seenIDs.contains( eeID ) ) continue;

            String uri = ( String ) o[1];

            if ( !c.containsKey( uri ) ) {
                c.put( uri, new HashSet<>() );
            }
            c.get( uri ).add( eeID );
            seenIDs.add( eeID );
        }
        return c;
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<String> uris ) {
        int batchSize = 1000; // to avoid HQL parser barfing
        Collection<String> batch = new HashSet<>();
        Collection<Characteristic> results = new HashSet<>();
        if ( uris.isEmpty() )
            return results;

        //language=HQL
        final String queryString = "from Characteristic where valueUri in (:uris)";

        for ( String uri : uris ) {
            batch.add( uri );
            if ( batch.size() >= batchSize ) {
                results.addAll( this.getBatchList( batch, queryString ) );
                batch.clear();
            }
        }
        if ( batch.size() > 0 ) {
            results.addAll( this.getBatchList( batch, queryString ) );
        }
        return results;
    }

    @Override
    public Collection<Characteristic> findByUri( String searchString ) {
        if ( StringUtils.isBlank( searchString ) )
            return new HashSet<>();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where  char.valueUri = :search" )
                .setParameter( "search", searchString ).list();
    }

    @Override
    public Collection<Characteristic> findByValue( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.value like :search " )
                .setParameter( "search", search.endsWith( "%" ) ? search : search + "%" ).list();
    }

    @Override
    public Map<Characteristic, Object> getParents( Class<?> parentClass, Collection<Characteristic> characteristics ) {

        Map<Characteristic, Object> charToParent = new HashMap<>();
        if ( characteristics == null || characteristics.size() == 0 ) {
            return charToParent;
        }
        if ( AbstractDao.log.isDebugEnabled() ) {
            Collection<String> uris = new HashSet<>();
            for ( Characteristic c : characteristics ) {

                if ( c.getValueUri() == null )
                    continue;
                uris.add( c.getValueUri() );

            }
            AbstractDao.log.debug( "For class=" + parentClass.getSimpleName() + ": " + characteristics.size()
                    + " Characteristics have URIS:\n" + StringUtils.join( uris, "\n" ) );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Characteristic> batch : new BatchIterator<>( characteristics,
                CharacteristicDaoImpl.BATCH_SIZE ) ) {
            this.batchGetParents( parentClass, batch, charToParent );
        }

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log
                    .info( "Fetch parents of characteristics: " + timer.getTime() + "ms for " + characteristics.size()
                            + " elements for class=" + parentClass.getSimpleName() );
        }

        return charToParent;
    }

    @Override
    public Map<Characteristic, Long> getParentIds( Class<?> parentClass, Collection<Characteristic> characteristics ) {

        Map<Characteristic, Long> charToParent = new HashMap<>();
        if ( characteristics == null || characteristics.size() == 0 ) {
            return charToParent;
        }
        if ( AbstractDao.log.isDebugEnabled() ) {
            Collection<String> uris = new HashSet<>();
            for ( Characteristic c : characteristics ) {

                if ( c.getValueUri() == null )
                    continue;
                uris.add( c.getValueUri() );

            }
            AbstractDao.log.debug( "For class=" + parentClass.getSimpleName() + ": " + characteristics.size()
                    + " Characteristics have URIS:\n" + StringUtils.join( uris, "\n" ) );
        }

        StopWatch timer = new StopWatch();
        timer.start();
        for ( Collection<Characteristic> batch : new BatchIterator<>( characteristics,
                CharacteristicDaoImpl.BATCH_SIZE ) ) {
            this.batchGetParentIds( parentClass, batch, charToParent );
        }

        if ( timer.getTime() > 1000 ) {
            AbstractDao.log
                    .info( "Fetch parents of characteristics: " + timer.getTime() + "ms for " + characteristics.size()
                            + " elements for class=" + parentClass.getSimpleName() );
        }

        return charToParent;

    }

    @Override
    public CharacteristicValueObject loadValueObject( Characteristic entity ) {
        return new CharacteristicValueObject( entity );
    }

    @SuppressWarnings("SameParameterValue") // Better for general use
    private Collection<Characteristic> getBatchList( Collection<String> batch, String queryString ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery( queryString ).setParameterList( "uris", batch )
                .list();
    }

    /*
     * Retrieve the objects that have these associated characteristics. Time-critical.
     */
    private void batchGetParents( Class<?> parentClass, Collection<Characteristic> characteristics,
            Map<Characteristic, Object> charToParent ) {
        if ( characteristics.isEmpty() )
            return;

        String field = this.getCharacteristicFieldName( parentClass );
        String queryString = "select parent, char from " + parentClass.getSimpleName() + " as parent " + " join parent." + field
                + " as char " + "where char in (:chars)";

        List<?> results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "chars", characteristics ).list();
        for ( Object o : results ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], row[0] );
        }
    }

    /*
     * Retrieve the objects that have these associated characteristics. Time-critical.
     */
    private void batchGetParentIds( Class<?> parentClass, Collection<Characteristic> characteristics,
            Map<Characteristic, Long> charToParent ) {
        if ( characteristics.isEmpty() )
            return;

        String field = this.getCharacteristicFieldName( parentClass );
        String queryString = "select parent.id, char from " + parentClass.getSimpleName() + " as parent " + " join parent." + field
                + " as char " + "where char in (:chars)";

        List<?> results = this.getSessionFactory().getCurrentSession().createQuery( queryString )
                .setParameterList( "chars", characteristics ).list();
        for ( Object o : results ) {
            Object[] row = ( Object[] ) o;
            charToParent.put( ( Characteristic ) row[1], ( Long ) row[0] );
        }
    }

    private String getCharacteristicFieldName( Class<?> parentClass ) {
        String field = "characteristics";
        if ( parentClass.isAssignableFrom( ExperimentalFactor.class ) )
            field = "category";
        else if ( parentClass.isAssignableFrom( Gene2GOAssociationImpl.class ) )
            field = "ontologyEntry";
        else if ( parentClass.isAssignableFrom( PhenotypeAssociation.class ) ) {
            field = "phenotypes";
        } else if ( parentClass.isAssignableFrom( Treatment.class ) ) {
            field = "action";
        } else if ( parentClass.isAssignableFrom( BioMaterial.class ) ) {
            field = "characteristics";
        }
        return field;
    }
}