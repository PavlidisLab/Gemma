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
import org.hibernate.Query;
import org.hibernate.SQLQuery;
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
import ubic.gemma.model.association.Gene2GOAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.expression.bioAssayData.CellTypeAssignment;
import ubic.gemma.model.expression.bioAssayData.GenericCellLevelCharacteristics;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.AbstractNoopFilteringVoEnabledDao;
import ubic.gemma.persistence.util.EE2CAclQueryUtils;
import ubic.gemma.persistence.util.IdentifiableUtils;
import ubic.gemma.persistence.util.QueryUtils;

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
        Class<? extends Identifiable> owningClass;
        String tableName;
        String foreignKey;
        boolean isForeignKeyInCharacteristicTable;
        @Nullable
        String discriminator;
    }

    /**
     * List of all entities that own characteristics.
     * TODO: detect those automatically from the class metadata
     */
    private static final OwningEntity[] OWNING_ENTITIES = new OwningEntity[] {
            new OwningEntity( BioMaterial.class, "BIO_MATERIAL", "BIO_MATERIAL_FK", true, null ),
            new OwningEntity( ExpressionExperiment.class, "INVESTIGATION", "INVESTIGATION_FK", true, "ExpressionExperiment" ),
            new OwningEntity( ExpressionExperimentSubSet.class, "INVESTIGATION", "INVESTIGATION_FK", true, "ExpressionExperimentSubSet" ),
            new OwningEntity( ExperimentalDesign.class, "EXPERIMENTAL_DESIGN", "EXPERIMENTAL_DESIGN_FK", true, null ),
            new OwningEntity( ExperimentalFactor.class, "EXPERIMENTAL_FACTOR", "CATEGORY_FK", false, null ),
            // via ExperimentalFactor.annotations
            new OwningEntity( ExperimentalFactor.class, "EXPERIMENTAL_FACTOR", "EXPERIMENTAL_FACTOR_FK", true, null ),
            new OwningEntity( BibliographicReference.class, "BIBLIOGRAPHIC_REFERENCE", "BIBLIOGRAPHIC_REFERENCE_FK", true, null ),
            new OwningEntity( FactorValue.class, "FACTOR_VALUE", "FACTOR_VALUE_FK", true, null ),
            new OwningEntity( GeneSet.class, "GENE_SET", "GENE_SET_FK", true, null ),
            new OwningEntity( CellTypeAssignment.class, "INVESTIGATION", "CELL_TYPE_ASSIGNMENT_FK", true, null ),
            new OwningEntity( GenericCellLevelCharacteristics.class, "CELL_LEVEL_CHARACTERISTICS", "CELL_LEVEL_CHARACTERISTICS_FK", true, null ),
            new OwningEntity( Gene2GOAssociation.class, "GENE2GO_ASSOCIATION", "ONTOLOGY_ENTRY_FK", false, null )
    };

    private static final Set<Class<? extends Identifiable>> OWNING_ENTITIES_CLASSES = Arrays.stream( OWNING_ENTITIES )
            .map( OwningEntity::getOwningClass )
            .collect( Collectors.toSet() );

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
    public Collection<Characteristic> findByCategoryLike( String query, int maxResults ) {
        //noinspection unchecked
        return ( Collection<Characteristic> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.category like :search" )
                .setParameter( "search", query )
                .setMaxResults( maxResults )
                .list();
    }

    @Override
    public Collection<Characteristic> findByCategoryUri( String uri, int maxResults ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.categoryUri = :uri" )
                .setParameter( "uri", uri )
                .setMaxResults( maxResults )
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
    public Collection<Characteristic> findByUri( String uri, @Nullable String category, int maxResults ) {
        if ( StringUtils.isBlank( uri ) )
            return new HashSet<>();
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.valueUri = :uri"
                        + ( category != null ? " and char.category = :category" : "" ) )
                .setParameter( "uri", uri );
        if ( category != null ) {
            q.setParameter( "category", category );
        }
        //noinspection unchecked
        return q.setMaxResults( maxResults ).list();
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
    public Map<String, Characteristic> findByValueUriGroupedByNormalizedValue( String valueUri, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        if ( isParentClassesEmpty( parentClasses, includeNoParents ) ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        return ( ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, {C.*} from CHARACTERISTIC {C} "
                        + "where VALUE_URI = :valueUri "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )

                .addScalar( "V", StandardBasicTypes.STRING )
                .addEntity( "C", Characteristic.class )
                .setParameter( "valueUri", valueUri )
                .list() )
                .stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> ( Characteristic ) row[1] ) );
    }

    @Override
    public Map<String, Characteristic> findByValueLikeGroupedByNormalizedValue( String valueLike, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        if ( isParentClassesEmpty( parentClasses, includeNoParents ) ) {
            return Collections.emptyMap();
        }
        //noinspection unchecked
        return ( ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, {C.*} from CHARACTERISTIC {C} "
                        + "where `VALUE` like :valueLike "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )

                .addScalar( "V", StandardBasicTypes.STRING )
                .addEntity( "C", Characteristic.class )
                .setParameter( "valueLike", valueLike )
                .list() )
                .stream()
                .collect( Collectors.toMap( row -> ( String ) row[0], row -> ( Characteristic ) row[1] ) );
    }

    @Override
    public Map<String, Long> countByValueUriGroupedByNormalizedValue( Collection<String> uris, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        if ( isParentClassesEmpty( parentClasses, includeNoParents ) ) {
            return Collections.emptyMap();
        }
        Query q = this.getSessionFactory().getCurrentSession()
                .createSQLQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, count(*) as COUNT from CHARACTERISTIC C "
                        + "where VALUE_URI in :uris "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" )
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
    public Collection<Characteristic> findByValueLike( String search, @Nullable String category, int maxResults ) {
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select char from Characteristic as char where char.value like :search"
                        + ( category != null ? " and char.category = :category" : "" ) )
                .setParameter( "search", search );
        if ( category != null ) {
            q.setParameter( "category", category );
        }
        //noinspection unchecked
        return q.setMaxResults( maxResults )
                .list();
    }

    @Override
    public Collection<Class<? extends Identifiable>> getParentClasses() {
        return OWNING_ENTITIES_CLASSES;
    }

    @Override
    public Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        Assert.isTrue( parentClasses == null || OWNING_ENTITIES_CLASSES.containsAll( parentClasses ) );

        if ( characteristics.isEmpty() || isParentClassesEmpty( parentClasses, includeNoParents ) ) {
            return Collections.emptyMap();
        }

        Map<Long, Characteristic> charById = IdentifiableUtils.getIdMap( characteristics );

        List<OwningEntity> oe = Arrays.stream( OWNING_ENTITIES )
                .filter( fk -> parentClasses == null || parentClasses.contains( fk.getOwningClass() ) )
                .collect( Collectors.toList() );

        SQLQuery query = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select C.ID" + createOwningEntitySelect( oe, includeNoParents ) + " from CHARACTERISTIC C "
                        + "where C.ID in :ids"
                        + ( !oe.isEmpty() || includeNoParents ? " and " + createOwningEntityConstraint( oe, includeNoParents ) : "" ) );

        List<Object[]> result = QueryUtils.listByBatch( query, "ids", charById.keySet(), MAX_PARAMETER_LIST_SIZE );
        Map<Characteristic, Identifiable> charToParent = new HashMap<>();
        for ( Object[] row : result ) {
            BigInteger charId = ( BigInteger ) row[0];
            Characteristic c = charById.get( charId.longValue() );
            Collection<Identifiable> parentObjects = new ArrayList<>( 1 );
            for ( int i = 0; i < oe.size(); i++ ) {
                OwningEntity owningEntity = oe.get( i );
                BigInteger entityId = ( BigInteger ) row[i + 1];
                if ( entityId != null ) {
                    parentObjects.add( ( Identifiable ) getSessionFactory().getCurrentSession()
                            .load( owningEntity.getOwningClass(), entityId.longValue() ) );
                }
            }
            if ( parentObjects.size() == 1 ) {
                charToParent.put( c, parentObjects.iterator().next() );
            } else if ( parentObjects.size() > 1 ) {
                log.warn( "Found multiple parents for characteristic " + c + ", it will not be included in the results:\n\t"
                        + parentObjects.stream().map( Identifiable::toString ).collect( Collectors.joining( "\n\t" ) ) );
            } else if ( includeNoParents ) {
                charToParent.put( c, null );
            } else {
                throw new IllegalStateException( "Could not find a parent for " + c + "." );
            }
        }

        return charToParent;
    }

    private boolean isParentClassesEmpty( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        return parentClasses != null && parentClasses.isEmpty() && !includeNoParents;
    }

    private String createOwningEntitySelect( List<OwningEntity> owningEntities, boolean includeNoParents ) {
        String selectOwningEntities = owningEntities.stream()
                .map( fk -> {
                    if ( fk.isForeignKeyInCharacteristicTable() ) {
                        if ( fk.getDiscriminator() != null ) {
                            return "(select E.ID from " + fk.getTableName() + " E "
                                    + "join CHARACTERISTIC C2 on E.ID = C2." + fk.getForeignKey() + " "
                                    + "where E.class = '" + fk.getDiscriminator() + "' "
                                    + "and C2.ID = C.ID)";
                        }
                        return "C." + fk.getForeignKey();
                    } else {
                        return "(select E.ID from " + fk.getTableName() + " E "
                                + "where E." + fk.getForeignKey() + " = C.ID"
                                + ( fk.getDiscriminator() != null ? " and E.class = '" + fk.getDiscriminator() + "'" : "" )
                                + ")";
                    }
                } )
                .map( s -> ", " + s )
                .collect( Collectors.joining() );
        if ( includeNoParents ) {
            selectOwningEntities += ", 0";
        }
        return selectOwningEntities;
    }

    /**
     * Create a SQL constrait to ensure that the characteristic is owned by an entity of the given class.
     */
    private String createOwningEntityConstraint( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        if ( parentClasses != null && !OWNING_ENTITIES_CLASSES.containsAll( parentClasses ) ) {
            throw new IllegalArgumentException( "Parent classes must be chosen among: " + OWNING_ENTITIES_CLASSES.stream()
                    .map( Class::getName ).sorted().collect( Collectors.joining( ", " ) ) + "." );
        }
        return createOwningEntityConstraint( Arrays.stream( OWNING_ENTITIES )
                .filter( oe -> parentClasses == null || parentClasses.contains( oe.getOwningClass() ) )
                .collect( Collectors.toList() ), includeNoParents );
    }

    private String createOwningEntityConstraint( List<OwningEntity> owningEntities, boolean includeNoParents ) {
        Assert.isTrue( !owningEntities.isEmpty() || includeNoParents,
                "At least one parent class (or lack thereof) must be requested." );
        if ( owningEntities.size() == OWNING_ENTITIES.length && includeNoParents ) {
            // everything is included, no need to create a constraint
            return "true";
        }
        List<String> constraints = new ArrayList<>( createConstraints( owningEntities, false ) );
        if ( includeNoParents ) {
            // add a clause for characteristics that do not have a parent
            constraints.add( "(" + String.join( " and ", createConstraints( Arrays.asList( OWNING_ENTITIES ), true ) ) + ")" );
        }
        return "(" + String.join( " or ", constraints ) + ")";
    }

    private List<String> createConstraints( List<OwningEntity> owningEntities, boolean invert ) {
        List<String> constraints = new ArrayList<>( owningEntities.size() );
        for ( OwningEntity owningEntity : owningEntities ) {
            if ( owningEntity.isForeignKeyInCharacteristicTable() ) {
                if ( owningEntity.getDiscriminator() != null ) {
                    constraints.add( "(C." + owningEntity.getForeignKey() + " " + ( invert ? "is" : "is not" ) + " NULL "
                            + ( invert ? " or " : " and " )
                            + "C." + owningEntity.getForeignKey() + " " + ( invert ? "not in" : "in" )
                            + " (select E.ID from " + owningEntity.getTableName() + " E where E.class = '" + owningEntity.getDiscriminator() + "'))" );
                } else {
                    constraints.add( "C." + owningEntity.getForeignKey() + " " + ( invert ? "is" : "is not" ) + " NULL" );
                }
            } else {
                // use a sub-query
                constraints.add( "C.ID " + ( invert ? "not in" : "in" ) + " (select E." + owningEntity.getForeignKey() + " from " + owningEntity.getTableName() + " E" +
                        ( owningEntity.getDiscriminator() != null ? " and E.class = '" + owningEntity.getDiscriminator() + "'" : "" ) + ")" );
            }
        }
        return constraints;
    }

    @Override
    protected CharacteristicValueObject doLoadValueObject( Characteristic entity ) {
        return new CharacteristicValueObject( entity );
    }

}