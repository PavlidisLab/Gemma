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
import ubic.gemma.persistence.util.EntityUtils;

import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

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
        Map<Long, ExpressionExperiment> eeById = EntityUtils.getIdMap( ees );
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
    public Map<String, Long> countCharacteristicsByValueUriGroupedByNormalizedValue( Collection<String> uris ) {
        if ( uris.isEmpty() )
            return Collections.emptyMap();
        Query q = this.getSessionFactory().getCurrentSession()
                .createQuery( "select lower(coalesce(char.valueUri, char.value)), count(char) from Characteristic char "
                        + "where char.valueUri in :uris "
                        + "group by coalesce(char.valueUri, char.value)" );
        return streamByBatch( q, "uris", uris, 2048, Object[].class )
                .collect( Collectors.groupingBy( row -> ( String ) row[0], Collectors.summingLong( row -> ( Long ) row[1] ) ) );
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
                .setParameter( "search", search )
                .list();
    }

    @Override
    public Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<?>> parentClasses, int maxResults ) {
        if ( characteristics.isEmpty() ) {
            return Collections.emptyMap();
        }

        Set<Long> characteristicIds = characteristics.stream().map( Characteristic::getId ).collect( Collectors.toSet() );
        Class<?>[] classes = { BioMaterial.class, ExpressionExperiment.class, ExperimentalDesign.class, FactorValue.class, GeneSet.class };
        String[] foreignKeys = { "BIO_MATERIAL_FK", "INVESTIGATION_FK", "EXPERIMENTAL_DESIGN_FK", "FACTOR_VALUE_FK", "GENE_SET_FK" };

        // ensure that at least one of the parentClass-associated column is non-null
        Set<String> foreignKeyToRestrictOn = null;
        if ( parentClasses != null ) {
            foreignKeyToRestrictOn = new HashSet<>();
            for ( int i = 0; i < classes.length; i++ ) {
                final int j = i;
                if ( parentClasses.stream().anyMatch( pc -> pc.isAssignableFrom( classes[j] ) ) ) {
                    foreignKeyToRestrictOn.add( foreignKeys[i] );
                }
            }
        }

        //  boolean gene2GoOk = parentClasses == null || parentClasses.stream().anyMatch( pc -> pc.isAssignableFrom( Gene2GOAssociation.class ) );
        boolean efOK = parentClasses == null || parentClasses.stream().anyMatch( pc -> pc.isAssignableFrom( ExperimentalFactor.class ) );

        String extraClause;
        if ( foreignKeyToRestrictOn != null ) {
            if ( foreignKeyToRestrictOn.isEmpty() ) {
                // ensure that all columns are NULL
                //language=HQL
                extraClause = " and (" + Arrays.stream( foreignKeys ).map( fk -> "C." + fk + " is NULL" ).collect( Collectors.joining( " and " ) ) + ")";
            } else {
                //language=HQL
                extraClause = " and (" + foreignKeyToRestrictOn.stream().map( fk -> "C." + fk + " is not NULL" ).collect( Collectors.joining( " or " ) ) + ")";
            }
        } else {
            extraClause = "";
        }

        //noinspection unchecked
        List<Object[]> result = getSessionFactory().getCurrentSession()
                .createSQLQuery( "select C.ID, C.BIO_MATERIAL_FK, C.INVESTIGATION_FK, C.EXPERIMENTAL_DESIGN_FK, C.FACTOR_VALUE_FK, C.GENE_SET_FK from CHARACTERISTIC C "
                        + "left join INVESTIGATION I on C.INVESTIGATION_FK = I.ID "
                        + "where C.ID in :ids "
                        + "and (I.class is NULL or I.class = 'ExpressionExperiment') " // for investigations, only retrieve EEs
                        + extraClause )
                .setParameterList( "ids", optimizeParameterList( characteristicIds ) )
                .setMaxResults( maxResults )
                .list();
        Set<Characteristic> characteristicsNotFound = new HashSet<>();
        Map<Long, Characteristic> charById = EntityUtils.getIdMap( characteristics );
        Map<Characteristic, Identifiable> charToParent = new HashMap<>();
        for ( Object[] row : result ) {
            Characteristic c = charById.get( ( ( BigInteger ) row[0] ).longValue() );
            if ( c == null ) {
                log.warn( "Could not find characteristic with ID " + row[0] + " in the database." );
                continue;
            }
            boolean found = false;
            for ( int i = 0; i < classes.length; i++ ) {
                if ( row[i + 1] != null ) {
                    Identifiable parentObject = ( Identifiable ) getSessionFactory().getCurrentSession().load( classes[i], ( ( BigInteger ) row[i + 1] ).longValue() );
                    charToParent.put( c, parentObject );
                    found = true;
                    break;
                }
            }
            if ( !found ) {
                // none matched in the CHARACTERISTIC table, check one-to-one relations later
                characteristicsNotFound.add( c );
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

        if ( !characteristicsNotFound.isEmpty() ) {
            //noinspection unchecked
// GO Terms doesn't have a use case
//            if (gene2GoOk) {
//                List<Object[]> g2gResults = getSessionFactory().getCurrentSession()
//                        .createQuery( "select g2g, g2g.ontologyEntry from Gene2GOAssociation g2g where g2g.ontologyEntry in :characteristics" )
//                        .setParameterList( "characteristics", characteristicsNotFound )
//                        .list();
//                for ( Object[] row : g2gResults ) {
//                    charToParent.put( ( Characteristic ) row[1], ( Identifiable ) row[0] );
//                    characteristicsNotFound.remove( ( Characteristic ) row[1] );
//                }
//            }

            if ( efOK ) {
                //noinspection unchecked
                List<Object[]> efResults = getSessionFactory().getCurrentSession()
                        .createQuery( "select ef, ef.category from ExperimentalFactor ef where ef.category in :characteristics" )
                        .setParameterList( "characteristics", optimizeParameterList( characteristicsNotFound ) )
                        .list();

                for ( Object[] row : efResults ) {
                    charToParent.put( ( Characteristic ) row[1], ( Identifiable ) row[0] );
                    characteristicsNotFound.remove( ( Characteristic ) row[1] );
                }
            }
        }

        if ( !characteristicsNotFound.isEmpty() ) {
            log.warn( String.format( "Could not find parents for the following characteristics: %s.",
                    characteristicsNotFound.stream().map( Characteristic::getId ).map( String::valueOf ).collect( Collectors.joining( ", " ) ) ) );
        }

        return charToParent;
    }

    @Override
    protected CharacteristicValueObject doLoadValueObject( Characteristic entity ) {
        return new CharacteristicValueObject( entity );
    }

}