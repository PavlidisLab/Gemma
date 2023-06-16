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
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.basecode.util.BatchIterator;
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.Treatment;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.AclQueryUtils;
import ubic.gemma.persistence.util.EntityUtils;

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

    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit, boolean rankByLevel ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        List<Object[]> result = prepareExperimentsByUrisQuery( uris, taxon, limit > 0 && rankByLevel )
                .setMaxResults( limit )
                .setCacheable( true )
                .list();
        if ( result.isEmpty() ) {
            return Collections.emptyMap();
        }
        Set<Long> ids = result.stream().map( row -> ( Long ) row[2] ).collect( Collectors.toSet() );
        //noinspection unchecked
        List<ExpressionExperiment> ees = getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionExperiment.class )
                .add( Restrictions.in( "id", ids ) )
                .list();
        Map<Long, ExpressionExperiment> eeById = EntityUtils.getIdMap( ees );
        //noinspection unchecked
        return result.stream().collect( Collectors.groupingBy(
                row -> ( Class<? extends Identifiable> ) row[0],
                Collectors.groupingBy(
                        row -> ( String ) row[1],
                        Collectors.mapping(
                                row -> Objects.requireNonNull( eeById.get( ( Long ) row[2] ), "No ExpressionExperiment with ID " + row[2] + "." ),
                                Collectors.toCollection( HashSet::new ) ) ) ) );
    }

    /**
     * Since proxies are returned, they cannot be collected in a {@link HashSet} which would otherwise cause their
     * initialization by accessing {@link Object#hashCode()}. Thus we need to create a {@link TreeSet} over the EE IDs.
     */
    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentReferencesByUris( Collection<String> uris, @Nullable Taxon taxon, int limit, boolean rankByLevel ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        List<Object[]> result = prepareExperimentsByUrisQuery( uris, taxon, limit > 0 && rankByLevel )
                .setMaxResults( limit )
                .setCacheable( true )
                .list();
        //noinspection unchecked
        return result.stream().collect( Collectors.groupingBy(
                row -> ( Class<? extends Identifiable> ) row[0],
                Collectors.groupingBy(
                        row -> ( String ) row[1],
                        Collectors.mapping(
                                row -> ( ExpressionExperiment ) getSessionFactory().getCurrentSession().load( ExpressionExperiment.class, ( Long ) row[2] ),
                                Collectors.toCollection( () -> new TreeSet<>( Comparator.comparing( ExpressionExperiment::getId ) ) ) ) ) ) );
    }

    private Query prepareExperimentsByUrisQuery( Collection<String> uris, @Nullable Taxon taxon, boolean rankByLevel ) {
        String qs = "select T.`LEVEL`, T.VALUE_URI, T.EXPRESSION_EXPERIMENT_FK from EXPRESSION_EXPERIMENT2CHARACTERISTIC T"
                + ( taxon != null ? " join INVESTIGATION I on T.EXPRESSION_EXPERIMENT_FK = I.ID " : "" )
                + AclQueryUtils.formNativeAclJoinClause( "T.EXPRESSION_EXPERIMENT_FK" ) + " "
                + "where T.VALUE_URI in :uris"
                + ( taxon != null ? " and I.TAXON_FK = :taxonId" : "" )
                + AclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory() )
                + ( rankByLevel ? " order by FIELD(T.LEVEL, :eeClass, :edClass, :bmClass)" : "" );

        Query query = getSessionFactory().getCurrentSession().createSQLQuery( qs )
                .addScalar( "LEVEL", StandardBasicTypes.CLASS )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "EXPRESSION_EXPERIMENT_FK", StandardBasicTypes.LONG );

        if ( rankByLevel ) {
            query.setParameter( "eeClass", ExpressionExperiment.class );
            query.setParameter( "edClass", ExperimentalDesign.class );
            query.setParameter( "bmClass", BioMaterial.class );
        }

        query.setParameterList( "uris", uris );

        if ( taxon != null ) {
            query.setParameter( "taxonId", taxon.getId() );
        }

        AclQueryUtils.addAclParameters( query, ExpressionExperiment.class );

        return query;
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
    public Characteristic findBestByUri( String uri ) {
        return ( Characteristic ) getSessionFactory().getCurrentSession()
                .createQuery( "select c from Characteristic c "
                        + "where valueUri = :uri "
                        + "group by c.value "
                        + "having c.value <> null "
                        + "order by count(*) desc" )
                .setParameter( "uri", uri )
                .setMaxResults( 1 )
                .uniqueResult();
    }

    @Override
    public Map<String, Long> countCharacteristicsByValueUriGroupedByNormalizedValue( Collection<String> uris ) {
        List<String> uniqueUris = uris.stream().distinct().sorted().collect( Collectors.toList() );
        if ( uniqueUris.isEmpty() )
            return Collections.emptyMap();
        //noinspection unchecked
        return ( ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select lower(coalesce(char.valueUri, char.value)), count(char) from Characteristic char "
                        + "where char.valueUri in :uris "
                        + "group by coalesce(char.valueUri, char.value)" )
                .setParameterList( "uris", uniqueUris )
                .list() )
                .stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> ( Long ) row[1] ) );
    }

    @Override
    public Map<String, Characteristic> findCharacteristicsByValueUriOrValueLikeGroupedByNormalizedValue( String value ) {
        //noinspection unchecked
        return ( ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select lower(coalesce(char.valueUri, char.value)), max(char) from Characteristic char "
                        + "where char.valueUri = :value or char.value like :value "
                        + "group by coalesce(char.valueUri, char.value)" )
                .setParameter( "value", value )
                .list() )
                .stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> ( Characteristic ) row[1] ) );
    }

    @Override
    public String normalizeByValue( Characteristic characteristic ) {
        if ( characteristic.getValueUri() != null ) {
            return characteristic.getValueUri().toLowerCase();
        } else if ( characteristic.getValue() != null ) {
            return characteristic.getValue().toLowerCase();
        } else {
            return null;
        }
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