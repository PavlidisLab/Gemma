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

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.*;

import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Luke
 * @author Paul
 * @see    Characteristic
 */
@Repository
public class CharacteristicDaoImpl extends AbstractNoopFilteringVoEnabledDao<Characteristic, CharacteristicValueObject>
        implements CharacteristicDao {

    private static final int BATCH_SIZE = 500;

    @Autowired
    public CharacteristicDaoImpl( SessionFactory sessionFactory ) {
        super( Characteristic.class, sessionFactory );
    }

    @Override
    public List<Characteristic> browse( int start, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "from Characteristic where value not like 'GO_%'" ).setMaxResults( limit )
                .setFirstResult( start ).list();
    }

    @Override
    public List<Characteristic> browse( int start, int limit, String orderField, boolean descending ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( elementClass )
                .add( Restrictions.not( Restrictions.like( "value", "GO_", MatchMode.START ) ) )
                .addOrder( descending ? Order.desc( orderField ) : Order.asc( orderField ) )
                .setFirstResult( start )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public Collection<? extends Characteristic> findByCategory( String query ) {

        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select distinct char from Characteristic as char where char.category like :search" )
                .setParameter( "search", query + "%" ).list();
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<Class<?>> classes, @Nullable Collection<String> characteristicUris ) {

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

    @SuppressWarnings({ "rawtypes", "cast" })
    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit ) {
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> result = new HashMap<>();

        if ( uris.isEmpty() )
            return result;

        // Note that the limit isn't strictly adhered to; we just stop querying when we have enough. We avoid duplicates
        Set<ExpressionExperiment> seenEEs = new HashSet<>();

        // direct associations
        // language=HQL
        result.put( ExpressionExperiment.class, queryAndMarkAsSeen(
                "select distinct ee, c.valueUri from ExpressionExperiment ee "
                        + "join ee.characteristics c",
                uris, taxon, seenEEs, limit ) );

        // via experimental factor
        // language=HQL
        result.put( FactorValue.class, queryAndMarkAsSeen( "select distinct ee, c.valueUri from ExpressionExperiment ee "
                + "join ee.experimentalDesign ed join ed.experimentalFactors ef "
                + "join ef.factorValues fv join fv.characteristics c", uris, taxon, seenEEs, limit ) );

        // via biomaterial
        // language=HQL
        result.put( BioMaterial.class, queryAndMarkAsSeen( "select distinct ee, c.valueUri from ExpressionExperiment ee "
                        + "join ee.bioAssays ba join ba.sampleUsed bm join bm.characteristics c",
                uris, taxon, seenEEs, limit ) );

        return result;
    }

    private Map<String, Set<ExpressionExperiment>> queryAndMarkAsSeen( String query, Collection<String> uris, @Nullable Taxon taxon, Set<ExpressionExperiment> seenEEs, int limit ) {
        if ( limit > 0 && seenEEs.size() > limit ) {
            return Collections.emptyMap();
        }

        query += AclQueryUtils.formAclJoinClause( "ee" );
        query += AclQueryUtils.formAclRestrictionClause();

        query += " and c.valueUri in (:uriStrings)";

        // don't retrieve EE IDs that we have already fetched otherwise
        if ( !seenEEs.isEmpty() ) {
            query += " and ee not in (:seenEEs)";
        }

        // by taxon
        if ( taxon != null ) {
            query += " and ee.taxon = :t";
        }

        Query q = getSessionFactory().getCurrentSession()
                .createQuery( query )
                .setParameterList( "uriStrings", uris );
        if ( !seenEEs.isEmpty() )
            q.setParameterList( "seenEEs", seenEEs );
        if ( taxon != null )
            q.setParameter( "t", taxon );
        AclQueryUtils.addAclJoinParameters( q, ExpressionExperiment.class );
        AclQueryUtils.addAclRestrictionParameters( q );

        //noinspection unchecked
        List<Object[]> results = q.list();

        Map<String, Set<ExpressionExperiment>> map = new HashMap<>();
        for ( Object[] row : results ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) row[0];
            String uri = ( String ) row[1];
            if ( seenEEs.contains( ee ) ) {
                continue;
            }
            if ( !map.containsKey( uri ) ) {
                map.put( uri, new HashSet<>() );
            }
            map.get( uri ).add( ee );
            seenEEs.add( ee );
        }

        return map;
    }

    @Override
    public Collection<Characteristic> findByUri( Collection<String> uris ) {
        Collection<Characteristic> results = new HashSet<>();

        if ( uris.isEmpty() )
            return results;

        List<String> uniqueUris = uris.stream()
                .distinct()
                .sorted()
                .collect( Collectors.toList() );

        for ( List<String> batch : ListUtils.partition( uniqueUris, 100 ) ) {
            //noinspection unchecked
            results.addAll( this.getSessionFactory().getCurrentSession()
                    .createQuery( "from Characteristic where valueUri in (:uris)" )
                    .setParameterList( "uris", batch )
                    .list() );
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
    public Map<String, CharacteristicByValueUriOrValueCount> countCharacteristicValueUriInByNormalizedValue( Collection<String> uris ) {
        List<Object[]> results = new ArrayList<>();
        List<String> uniqueUris = uris.stream().distinct().sorted().collect( Collectors.toList() );
        if ( uniqueUris.isEmpty() )
            return Collections.emptyMap();
        for ( List<String> batch : ListUtils.partition( uniqueUris, 100 ) ) {
            //noinspection unchecked
            results.addAll( this.getSessionFactory().getCurrentSession()
                    .createQuery( "select lower(coalesce(nullif(char.valueUri, ''), char.value)), max(char.valueUri), max(char.value), count(char) from Characteristic char "
                            + "where char.valueUri in :batch "
                            + "group by char.valueUri, char.value" )
                    .setParameterList( "batch", batch )
                    .list() );
        }
        return results.stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> new CharacteristicByValueUriOrValueCount( ( String ) row[1], ( String ) row[2], ( Long ) row[3] ) ) );
    }

    @Override
    public Map<String, CharacteristicByValueUriOrValueCount> countCharacteristicValueLikeByNormalizedValue( String value ) {
        //noinspection unchecked
        return ( ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select lower(coalesce(nullif(char.valueUri, ''), char.value)), max(char.valueUri), max(char.value), count(char) as cnt from Characteristic char "
                        + "where char.value like :value "
                        + "group by char.valueUri, char.value" )
                .setParameter( "value", value )
                .list() )
                .stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> new CharacteristicByValueUriOrValueCount( ( String ) row[1], ( String ) row[2], ( Long ) row[3] ) ) );
    }

    @Override
    public Collection<Characteristic> findByValue( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.value like :search " )
                .setParameter( "search", search.endsWith( "%" ) ? search : search + "%" ).list();
    }

    @Override
    public Map<Characteristic, Object> getParents( Class<?> parentClass, @Nullable Collection<Characteristic> characteristics ) {

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
    public Map<Characteristic, Long> getParentIds( Class<?> parentClass, @Nullable Collection<Characteristic> characteristics ) {

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
    protected CharacteristicValueObject doLoadValueObject( Characteristic entity ) {
        return new CharacteristicValueObject( entity );
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
        else if ( parentClass.isAssignableFrom( Gene2GOAssociation.class ) )
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