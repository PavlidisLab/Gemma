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

import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.EE2CAclQueryUtils;
import ubic.gemma.persistence.util.IdentifiableUtils;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

import static org.hibernate.criterion.Restrictions.like;
import static org.hibernate.criterion.Restrictions.not;
import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.EE2C_QUERY_SPACE;
import static ubic.gemma.persistence.util.IdentifiableUtils.toIdentifiableSet;
import static ubic.gemma.persistence.util.QueryUtils.*;

/**
 * @author Luke
 * @author Paul
 * @see    Characteristic
 */
@Repository
public class CharacteristicDaoImpl extends AbstractNoopFilteringVoEnabledDao<Characteristic, CharacteristicValueObject>
        implements CharacteristicDao {

    @Value
    private static class OwningEntity {
        Class<?> owningClass;
        String tableName;
        String foreignKey;
        boolean isForeignKeyInCharacteristicTable;
    }

    // TODO
    private static final OwningEntity[] OWNING_ENTITIES = new OwningEntity[] {
            new OwningEntity( BioMaterial.class, "CHARACTERISTIC", "BIO_MATERIAL_FK", true ),
            new OwningEntity( ExpressionExperiment.class, "CHARACTERISTIC", "INVESTIGATION_FK", true ),
            new OwningEntity( ExperimentalDesign.class, "CHARACTERISTIC", "EXPERIMENTAL_DESIGN_FK", true ),
            new OwningEntity( ExperimentalFactor.class, "EXPERIMENTAL_FACTOR", "CATEGORY_FK", false ),
            new OwningEntity( FactorValue.class, "CHARACTERISTIC", "FACTOR_VALUE_FK", true ),
            new OwningEntity( GeneSet.class, "CHARACTERISTIC", "GENE_SET_FK", true )
            // TODO: new OwningEntity( Gene2GOAssociation.class, "GENE2GO_ASSOCIATION", "ONTOLOGY_ENTRY_FK", false )
    };

    @Autowired
    public CharacteristicDaoImpl( SessionFactory sessionFactory ) {
        super( Characteristic.class, sessionFactory );
    }

    @Override
    public List<Characteristic> browse( int start, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( Characteristic.class )
                .add( not( like( "valueUri", OntologyUtils.BASE_PURL_URI + "GO_", MatchMode.START ) ) )
                .setFirstResult( start )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public List<Characteristic> browse( int start, int limit, String orderField, boolean descending ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createCriteria( Characteristic.class )
                .add( not( like( "valueUri", OntologyUtils.BASE_PURL_URI + "GO_", MatchMode.START ) ) )
                .addOrder( descending ? Order.desc( orderField ) : Order.asc( orderField ) )
                .setFirstResult( start )
                .setMaxResults( limit )
                .list();
    }

    @Override
    public Collection<Characteristic> findByCategory( String value ) {
        //noinspection unchecked
        return ( Collection<Characteristic> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.category = :value" )
                .setParameter( "value", value )
                .list();
    }

    @Override
    public Collection<Characteristic> findByCategoryLike( String query ) {
        //noinspection unchecked
        return ( Collection<Characteristic> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.category like :search" )
                .setParameter( "search", query )
                .list();
    }

    @Override
    public Collection<Characteristic> findByCategoryUri( String uri ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.categoryUri = :uri" )
                .setParameter( "uri", uri )
                .list();
    }

    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, @Nullable Taxon taxon, int limit, boolean rankByLevel ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        // no need to rank if there is no limit since we're collecting in a mapping
        List<Object[]> result = findExperimentsByUrisInternal( uris, taxon, limit > 0 && rankByLevel, limit );
        if ( result.isEmpty() ) {
            return Collections.emptyMap();
        }
        Set<Long> ids = result.stream().map( row -> ( Long ) row[2] ).collect( Collectors.toSet() );
        //noinspection unchecked
        List<ExpressionExperiment> ees = getSessionFactory().getCurrentSession()
                .createCriteria( ExpressionExperiment.class )
                .add( Restrictions.in( "id", ids ) )
                .list();
        Map<Long, ExpressionExperiment> eeById = IdentifiableUtils.getIdMap( ees );
        //noinspection unchecked
        return result.stream()
                .filter( row -> eeById.containsKey( ( Long ) row[2] ) )
                .collect( Collectors.groupingBy(
                        row -> ( Class<? extends Identifiable> ) row[0],
                        Collectors.groupingBy(
                                row -> ( String ) row[1],
                                Collectors.mapping(
                                        row -> eeById.get( ( Long ) row[2] ),
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
        return findExperimentsByUrisInternal( uris, taxon, limit > 0 && rankByLevel, limit ).stream().collect( Collectors.groupingBy(
                row -> ( Class<? extends Identifiable> ) row[0],
                Collectors.groupingBy(
                        row -> ( String ) row[1],
                        Collectors.mapping(
                                row -> ( ExpressionExperiment ) getSessionFactory().getCurrentSession().load( ExpressionExperiment.class, ( Long ) row[2] ),
                                toIdentifiableSet() ) ) ) );
    }

    private List<Object[]> findExperimentsByUrisInternal( Collection<String> uris, @Nullable Taxon taxon, boolean rankByLevel, int limit ) {
        String qs = "select T.`LEVEL`, T.VALUE_URI, T.EXPRESSION_EXPERIMENT_FK from EXPRESSION_EXPERIMENT2CHARACTERISTIC T"
                + ( taxon != null ? " join INVESTIGATION I on T.EXPRESSION_EXPERIMENT_FK = I.ID " : "" )
                + EE2CAclQueryUtils.formNativeAclJoinClause( "T.EXPRESSION_EXPERIMENT_FK" ) + " "
                + "where T.VALUE_URI in :uris"
                + ( taxon != null ? " and I.TAXON_FK = :taxonId" : "" )
                + EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "T.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" )
                + ( rankByLevel ? " order by FIELD(T.LEVEL, :eeClass, :edClass, :bmClass)" : "" );

        Query query = getSessionFactory().getCurrentSession().createSQLQuery( qs )
                .addScalar( "LEVEL", StandardBasicTypes.CLASS )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "EXPRESSION_EXPERIMENT_FK", StandardBasicTypes.LONG )
                // invalidate the cache when the EE2C table is updated
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                // invalidate the cache when EEs are added/removed
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                // invalidate the cache when new characteristics are added/removed
                .addSynchronizedEntityClass( Characteristic.class );

        if ( rankByLevel ) {
            query.setParameter( "eeClass", ExpressionExperiment.class );
            query.setParameter( "edClass", ExperimentalDesign.class );
            query.setParameter( "bmClass", BioMaterial.class );
        }

        if ( taxon != null ) {
            query.setParameter( "taxonId", taxon.getId() );
        }

        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );

        query.setCacheable( true );

        List<Object[]> result;
        if ( uris.size() > MAX_PARAMETER_LIST_SIZE ) {
            if ( limit > 0 && rankByLevel ) {
                // query is limited and order is important, we have to sort the results in memory
                result = streamByBatch( query, "uris", uris, 2048, Object[].class )
                        .sorted( Comparator.comparing( row -> rankClass( ( Class<?> ) row[0] ) ) )
                        .limit( limit )
                        .collect( Collectors.toList() );
            } else {
                // query is either unlimited or there is no ordering, batching will not affect the output
                result = listByBatch( query, "uris", uris, 2048, limit );
            }
        } else {
            //noinspection unchecked
            result = query
                    .setParameterList( "uris", optimizeParameterList( uris ) )
                    .list();
        }

        return result;
    }

    private int rankClass( Class<?> clazz ) {
        if ( clazz == ExpressionExperiment.class ) {
            return 0;
        } else if ( clazz == ExperimentalDesign.class ) {
            return 1;
        } else if ( clazz == BioMaterial.class ) {
            return 2;
        } else {
            return 3;
        }
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

        for ( Collection<String> batch : batchParameterList( uniqueUris, getBatchSize() ) ) {
            //noinspection unchecked
            results.addAll( this.getSessionFactory().getCurrentSession()
                    .createQuery( "from Characteristic where valueUri in (:uris)" )
                    .setParameterList( "uris", batch )
                    .list() );
        }

        return results;
    }

    @Override
    public Collection<Characteristic> findByUri( String uri ) {
        if ( StringUtils.isBlank( uri ) )
            return new HashSet<>();
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where  char.valueUri = :uri" )
                .setParameter( "uri", uri ).list();
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
    public Map<String, Characteristic> findByValueUriGroupedByNormalizedValue( String valueUri, @Nullable Collection<Class<?>> owningEntityClasses ) {
        if ( owningEntityClasses != null && owningEntityClasses.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        return ( ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, {C.*} from CHARACTERISTIC {C} "
                        + "where VALUE_URI = :valueUri "
                        + ( owningEntityClasses != null ? "and " + createOwningEntityConstraint( owningEntityClasses ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )

                .addScalar( "V", StandardBasicTypes.STRING )
                .addEntity( "C", Characteristic.class )
                .setParameter( "valueUri", valueUri )
                .list() )
                .stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> ( Characteristic ) row[1] ) );
    }

    @Override
    public Map<String, Characteristic> findByValueLikeGroupedByNormalizedValue( String valueLike, @Nullable Collection<Class<?>> owningEntityClasses ) {
        if ( owningEntityClasses != null && owningEntityClasses.isEmpty() ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        return ( ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, {C.*} from CHARACTERISTIC {C} "
                        + "where `VALUE` like :valueLike "
                        + ( owningEntityClasses != null ? "and " + createOwningEntityConstraint( owningEntityClasses ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )

                .addScalar( "V", StandardBasicTypes.STRING )
                .addEntity( "C", Characteristic.class )
                .setParameter( "valueLike", valueLike )
                .list() )
                .stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> ( Characteristic ) row[1] ) );
    }

    @Override
    public Map<String, Long> countByValueUriGroupedByNormalizedValue( Collection<String> uris, @Nullable Collection<Class<?>> owningEntityClasses ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        if ( owningEntityClasses != null && owningEntityClasses.isEmpty() ) {
            return Collections.emptyMap();
        }
        Query q = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, count(*) as COUNT from CHARACTERISTIC C "
                        + "where VALUE_URI in :uris "
                        + ( owningEntityClasses != null ? "and " + createOwningEntityConstraint( owningEntityClasses ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "COUNT", StandardBasicTypes.LONG );
        return streamByBatch( q, "uris", uris, 2048, Object[].class )
                .collect( Collectors.groupingBy( row -> ( String ) row[0], Collectors.summingLong( row -> ( Long ) row[1] ) ) );
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
    public Collection<Characteristic> findByValue( String value ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.value = :value" )
                .setParameter( "value", value )
                .list();
    }

    @Override
    public Collection<Characteristic> findByValueLike( String search ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.value like :search " )
                .setParameter( "search", search )
                .list();
    }

    @Override
    public Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<?>> owningEntityClass, int maxResults ) {
        if ( characteristics.isEmpty() ) {
            return Collections.emptyMap();
        }

        if ( owningEntityClass != null && owningEntityClass.isEmpty() ) {
            return Collections.emptyMap();
        }

        Map<Long, Characteristic> charById = IdentifiableUtils.getIdMap( characteristics );

        String s = Arrays.stream( OWNING_ENTITIES )
                .map( fk -> {
                    if ( fk.isForeignKeyInCharacteristicTable() ) {
                        return "C." + fk.getForeignKey();
                    } else {
                        return "(select E." + fk.getForeignKey() + " from " + fk.getTableName() + " E where E.ID = C.ID)";
                    }
                } )
                .collect( Collectors.joining( ", " ) );
        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select C.ID, " + s + " from CHARACTERISTIC C "
                        + "left join INVESTIGATION I on C.INVESTIGATION_FK = I.ID "
                        + "where C.ID in :ids "
                        + "and (I.class is NULL or I.class = 'ExpressionExperiment')" // for investigations, only retrieve EEs
                        + ( owningEntityClass != null ? " and " + createOwningEntityConstraint( owningEntityClass ) : "" ) )
                .setParameterList( "ids", optimizeParameterList( charById.keySet() ) )
                .setMaxResults( maxResults )
                .list();
        Map<Characteristic, Identifiable> charToParent = new HashMap<>();
        for ( Object[] row : result ) {
            Characteristic c = charById.get( ( ( BigInteger ) row[0] ).longValue() );
            if ( c == null ) {
                log.warn( "Could not find characteristic with ID " + row[0] + " in the database." );
                continue;
            }
            for ( int i = 0; i < OWNING_ENTITIES.length; i++ ) {
                OwningEntity owningEntity = OWNING_ENTITIES[i];
                BigInteger entityId = ( BigInteger ) row[i + 1];
                if ( entityId != null ) {
                    Identifiable parentObject = ( Identifiable ) getSessionFactory().getCurrentSession()
                            .load( owningEntity.getOwningClass(), entityId.longValue() );
                    charToParent.put( c, parentObject );
                    break;
                }
            }
        }

        // batch-load all the proxies
        for ( Map.Entry<Characteristic, Identifiable> entry : charToParent.entrySet() ) {
            Identifiable parent = entry.getValue();
            Hibernate.initialize( parent );
            if ( parent instanceof FactorValue ) {
                Hibernate.initialize( ( ( FactorValue ) parent ).getExperimentalFactor() );
            }
        }

        if ( charToParent.size() < charById.size() ) {
            Set<Characteristic> characteristicsNotFound = new HashSet<>( characteristics );
            characteristicsNotFound.removeAll( charToParent.keySet() );
            log.warn( String.format( "Could not find parents for the following characteristics: %s.",
                    characteristicsNotFound.stream().map( Characteristic::getId ).map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );
        }

        return charToParent;
    }

    /**
     * Create a SQL constrait to ensure that the characteristic is owned by an entity of the given class.
     */
    private String createOwningEntityConstraint( Collection<Class<?>> owningEntityClasses ) {
        Assert.isTrue( !owningEntityClasses.isEmpty(), "At least one parent class must be requested." );
        TreeSet<String> constraints = new TreeSet<>();
        for ( Class<?> owningEntityClass : owningEntityClasses ) {
            boolean found = false;
            for ( OwningEntity owningEntity : OWNING_ENTITIES ) {
                if ( owningEntity.getOwningClass().isAssignableFrom( owningEntityClass ) ) {
                    if ( owningEntity.isForeignKeyInCharacteristicTable() ) {
                        constraints.add( "C." + owningEntity.getForeignKey() + " is not NULL" );
                    } else {
                        // use a sub-query
                        constraints.add( "C.ID in (select E." + owningEntity.getForeignKey() + " from " + owningEntity.getTableName() + " E)" );
                    }
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                throw new IllegalArgumentException( "Cannot create constraint for " + owningEntityClass.getName() );
            }
        }
        return "(" + String.join( " or ", constraints ) + ")";
    }

    @Override
    protected CharacteristicValueObject doLoadValueObject( Characteristic entity ) {
        return new CharacteristicValueObject( entity );
    }

}