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
import org.hibernate.query.Query;
import org.hibernate.query.NativeQuery;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import ubic.gemma.core.ontology.OntologyUtils;
import ubic.gemma.model.annotations.MayBeUninitialized;
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

import org.springframework.lang.Nullable;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.persistence.service.maintenance.TableMaintenanceUtil.EE2C_QUERY_SPACE;
import static ubic.gemma.persistence.util.IdentifiableUtils.toIdentifiableSet;
import static ubic.gemma.persistence.util.QueryUtils.*;

/**
 * @author Luke
 * @author Paul
 * @see Characteristic
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

    /**
     * Whitelist of column names that {@link #browse(int, int, String, boolean)} accepts as
     * the ORDER BY target. Mirrors the three properties the web controller exposes; values
     * not in this set are rejected so the ORDER BY clause cannot absorb arbitrary caller
     * input.
     */
    private static final Set<String> BROWSE_SORTABLE_FIELDS = new HashSet<>( Arrays.asList(
            "category", "value", "evidenceCode" ) );

    @Autowired
    public CharacteristicDaoImpl( SessionFactory sessionFactory ) {
        super( Characteristic.class, sessionFactory );
    }

    @Override
    public List<Characteristic> browse( int start, int limit ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from Characteristic c where c.valueUri not like :p" )
                .setParameter( "p", OntologyUtils.BASE_PURL_URI + "GO_%" )
                .setFirstResult( start )
                // HB6 rejects setMaxResults(<0); browse contract treats <=0 as "no limit".
                .setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE )
                .list();
    }

    @Override
    public List<Characteristic> browse( int start, int limit, String orderField, boolean descending ) {
        // ORDER BY column names cannot be bound as HQL parameters; whitelist + inject so
        // the column reference comes from a fixed alphabet. Pushes the guard into the DAO
        // so any future caller gets a clear IllegalArgumentException instead of relying on
        // Hibernate 6's UnknownPathException wrapping at runtime — mirrors the
        // BibliographicReferenceDaoImpl.browse pattern (HQL_SQL_AUDIT C1).
        if ( !BROWSE_SORTABLE_FIELDS.contains( orderField ) ) {
            throw new IllegalArgumentException( "Unsupported Characteristic sort field: " + orderField
                    + " (allowed: " + BROWSE_SORTABLE_FIELDS + ")" );
        }
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from Characteristic c where c.valueUri not like :p "
                        + "order by c." + orderField + ( descending ? " desc" : " asc" ) )
                .setParameter( "p", OntologyUtils.BASE_PURL_URI + "GO_%" )
                .setFirstResult( start )
                // HB6 rejects setMaxResults(<0); browse contract treats <=0 as "no limit".
                .setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE )
                .list();
    }

    @Override
    public Collection<Characteristic> findByParentClasses( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, @Nullable String category, int maxResults ) {
        Query<?> q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select C.ID from CHARACTERISTIC as C "
                        + "where " + createOwningEntityConstraint( parentClasses, includeNoParents )
                        + ( category != null ? " and " + createCategoryConstraint( "C", "category", category ) : "" ) )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE );
        if ( category != null ) {
            q.setParameter( "category", category );
        }
        //noinspection unchecked
        return loadByIds( ( List<Long> ) q.list() );
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
    public Collection<Characteristic> findByCategoryLike( String query, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        Query<?> q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select C.ID from CHARACTERISTIC as C where C.CATEGORY like :search"
                        + ( parentClasses != null || !includeNoParents ? " and " + createOwningEntityConstraint( parentClasses, includeNoParents ) : "" ) )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setParameter( "search", query )
                .setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE );
        //noinspection unchecked
        return loadByIds( ( List<Long> ) q.list() );
    }

    @Override
    public Collection<Characteristic> findByCategoryUri( String uri, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        Query<?> q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select C.ID from CHARACTERISTIC as C where C.CATEGORY_URI = :uri"
                        + ( parentClasses != null || !includeNoParents ? " and " + createOwningEntityConstraint( parentClasses, includeNoParents ) : "" ) )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setParameter( "uri", uri )
                .setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE );
        //noinspection unchecked
        return loadByIds( ( List<Long> ) q.list() );
    }

    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> findExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean rankByLevel ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        // no need to rank if there is no limit since we're collecting in a mapping
        Map<Class<? extends Identifiable>, Map<String, Set<Long>>> result = findExperimentsByUrisInternal( uris, includeSubjects, includePredicates, includeObjects, taxon, limit > 0 && rankByLevel, limit );
        if ( result.isEmpty() ) {
            return Collections.emptyMap();
        }
        Set<Long> ids = result.values().stream()
                .map( Map::values )
                .flatMap( Collection::stream )
                .flatMap( Collection::stream )
                .collect( Collectors.toSet() );
        //noinspection unchecked
        List<ExpressionExperiment> ees = getSessionFactory().getCurrentSession()
                .createQuery( "select ee from ExpressionExperiment ee where ee.id in :ids", ExpressionExperiment.class )
                .setParameterList( "ids", ids )
                .list();

        Map<Long, ExpressionExperiment> eeById = IdentifiableUtils.getIdMap( ees );
        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> result2 = new HashMap<>();
        for ( Map.Entry<Class<? extends Identifiable>, Map<String, Set<Long>>> entry : result.entrySet() ) {
            Class<? extends Identifiable> clazz = entry.getKey();
            for ( Map.Entry<String, Set<Long>> subEntry : entry.getValue().entrySet() ) {
                String uri = subEntry.getKey();
                result2.computeIfAbsent( clazz, k -> new HashMap<>() )
                        .computeIfAbsent( uri, row -> subEntry.getValue().stream()
                                .map( eeById::get )
                                .filter( Objects::nonNull )
                                .collect( Collectors.toSet() ) );
            }
        }
        return result2;
    }

    /**
     * Since proxies are returned, they cannot be collected in a {@link HashSet} which would otherwise cause their
     * initialization by accessing {@link Object#hashCode()}. Thus we need to create a {@link TreeSet} over the EE IDs.
     */
    @Override
    public Map<Class<? extends Identifiable>, Map<String, Set<@MayBeUninitialized ExpressionExperiment>>> findExperimentReferencesByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, int limit, boolean rankByLevel ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        Map<Class<? extends Identifiable>, Map<String, Set<Long>>> result = findExperimentsByUrisInternal( uris, includeSubjects, includePredicates, includeObjects, taxon, limit > 0 && rankByLevel, limit );

        Map<Class<? extends Identifiable>, Map<String, Set<ExpressionExperiment>>> result2 = new HashMap<>();
        for ( Map.Entry<Class<? extends Identifiable>, Map<String, Set<Long>>> entry : result.entrySet() ) {
            Class<? extends Identifiable> clazz = entry.getKey();
            for ( Map.Entry<String, Set<Long>> subEntry : entry.getValue().entrySet() ) {
                String uri = subEntry.getKey();
                result2.computeIfAbsent( clazz, k -> new HashMap<>() )
                        .computeIfAbsent( uri, row -> subEntry.getValue().stream()
                                .map( eeId -> ( ExpressionExperiment ) getSessionFactory().getCurrentSession().getReference( ExpressionExperiment.class, eeId ) )
                                .collect( toIdentifiableSet() ) );
            }
        }
        return result2;
    }

    @Override
    public Map<String, Long> countExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, Collection<Long> excludedExperimentIds ) {
        return countExperimentsByUris( uris, includeSubjects, includePredicates, includeObjects, false, taxon, excludedExperimentIds );
    }

    @Override
    public Map<String, Long> countExperimentsByUris( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, boolean includeCategories, @Nullable Taxon taxon, Collection<Long> excludedExperimentIds ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        // The URI columns collate case-insensitively, so two spellings of one URI form a single
        // group in the database. Collapse them here too: left alone they could be split across two
        // batches and come back as two rows carrying the same key, which the loop below would then
        // resolve by overwriting rather than by adding.
        Set<String> distinctUris = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
        distinctUris.addAll( uris );
        String qs = buildCountExperimentsByUrisUnionAll( includeSubjects, includePredicates, includeObjects, includeCategories, taxon, !excludedExperimentIds.isEmpty() );

        Query query = getSessionFactory().getCurrentSession().createNativeQuery( qs )
                .addScalar( "URI", StandardBasicTypes.STRING )
                .addScalar( "N", StandardBasicTypes.LONG )
                // invalidate the cache when the EE2C table is updated
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                // invalidate the cache when EEs are added/removed
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                // invalidate the cache when new characteristics are added/removed
                .addSynchronizedEntityClass( Characteristic.class );

        if ( taxon != null ) {
            query.setParameter( "taxonId", taxon.getId() );
        }

        if ( !excludedExperimentIds.isEmpty() ) {
            // padding repeats the largest id, which a NOT IN cannot be changed by
            query.setParameterList( "excludedEeIds", optimizeParameterList( excludedExperimentIds ) );
        }

        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );

        query.setCacheable( true );

        // Batching is safe for an aggregate here only because the grouping key IS the batching key:
        // every row for a given URI is produced by the batch that URI belongs to, so each group is
        // complete within its batch. Do not reuse this shape for an aggregate grouped by anything
        // else.
        Map<String, Long> counts = new HashMap<>();
        QueryUtils.<String, Object[]>streamByBatch( query, "uris", distinctUris, 2048 )
                .forEach( row -> counts.put( ( String ) row[0], ( Long ) row[1] ) );
        return counts;
    }

    /**
     * Build the {@code countExperimentsByUris} query: the same {@code UNION ALL} of per-column range
     * scans as {@link #buildFindExperimentsByUrisUnionAll}, projecting only the matched URI and the
     * experiment id, wrapped in a {@code group by} that counts distinct experiments per URI.
     * <p>
     * The {@code count(distinct ...)} sits OUTSIDE the union so an EE2C row whose URI appears in
     * more than one column contributes once, which is what the caller-side {@code Set<Long>} of
     * experiment ids used to guarantee.
     */
    private String buildCountExperimentsByUrisUnionAll( boolean includeSubjects, boolean includePredicates, boolean includeObjects, boolean includeCategories, @Nullable Taxon taxon, boolean excludeExperiments ) {
        Assert.isTrue( includeSubjects || includePredicates || includeObjects || includeCategories, "At least one of the source URIs must be included." );
        List<String> uriColumns = new ArrayList<>( 6 );
        if ( includeSubjects ) {
            uriColumns.add( "VALUE_URI" );
        }
        if ( includePredicates ) {
            uriColumns.add( "PREDICATE_URI" );
            uriColumns.add( "SECOND_PREDICATE_URI" );
        }
        if ( includeObjects ) {
            uriColumns.add( "OBJECT_URI" );
            uriColumns.add( "SECOND_OBJECT_URI" );
        }
        if ( includeCategories ) {
            // EE2C_CATEGORY_URI_CATEGORY_VALUE_URI_VALUE leads with CATEGORY_URI, so this arm gets the same
            // range scan as the others rather than falling back to a table scan.
            uriColumns.add( "CATEGORY_URI" );
        }
        String aclWhere = EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "T.EXPRESSION_EXPERIMENT_FK", "T.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" );
        String taxonJoin = taxon != null ? " join INVESTIGATION I on T.EXPRESSION_EXPERIMENT_FK = I.ID " : "";
        String taxonWhere = taxon != null ? " and I.TAXON_FK = :taxonId" : "";
        String excludedWhere = excludeExperiments ? " and T.EXPRESSION_EXPERIMENT_FK not in (:excludedEeIds)" : "";
        StringBuilder sb = new StringBuilder();
        sb.append( "select U.URI as URI, count(distinct U.EE) as N from (" );
        for ( int i = 0; i < uriColumns.size(); i++ ) {
            if ( i > 0 ) {
                sb.append( " union all " );
            }
            sb.append( "select T." ).append( uriColumns.get( i ) ).append( " as URI, T.EXPRESSION_EXPERIMENT_FK as EE" )
                    .append( " from EXPRESSION_EXPERIMENT2CHARACTERISTIC T" )
                    .append( taxonJoin )
                    .append( " where T." ).append( uriColumns.get( i ) ).append( " in (:uris)" )
                    .append( taxonWhere )
                    .append( aclWhere )
                    .append( excludedWhere );
        }
        sb.append( ") U group by U.URI" );
        return sb.toString();
    }

    private Map<Class<? extends Identifiable>, Map<String, Set<Long>>> findExperimentsByUrisInternal( Collection<String> uris, boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, boolean rankByLevel, int limit ) {
        // Use UNION ALL of per-column range scans instead of a 5-column OR.
        // MySQL gives up on index_merge once the per-column row estimate exceeds a threshold
        // and falls back to a full scan on EE2C (~2.18M rows). Each UNION arm picks the
        // arm-specific composite index (EE2C_VALUE_URI_VALUE, EE2C_OBJECT_URI_OBJECT, ...)
        // and runs as a 'range' lookup. Live-measured: 10 URIs 3.07s -> 0.16s (19x);
        // 200 URIs 3.94s -> 1.08s (3.6x). See PERF_PROBE_SEARCH.md.
        //
        // Duplicate-row note: a single EE2C row whose URI appears in multiple columns is
        // emitted once per matching arm. The downstream loop (lines below) deduplicates
        // implicitly because it builds Set<Long> of EE IDs per (clazz, uri).
        String qs = buildFindExperimentsByUrisUnionAll( includeSubjects, includePredicates, includeObjects, taxon, rankByLevel );

        Query query = getSessionFactory().getCurrentSession().createNativeQuery( qs )
                .addScalar( "LEVEL", StandardBasicTypes.CLASS )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "PREDICATE_URI", StandardBasicTypes.STRING )
                .addScalar( "OBJECT_URI", StandardBasicTypes.STRING )
                .addScalar( "SECOND_PREDICATE_URI", StandardBasicTypes.STRING )
                .addScalar( "SECOND_OBJECT_URI", StandardBasicTypes.STRING )
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
                result = QueryUtils.<String, Object[]>streamByBatch( query, "uris", uris, 2048 )
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

        Map<Class<? extends Identifiable>, Map<String, Set<Long>>> result2 = new HashMap<>();

        TreeSet<String> urisIgnoreCase = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
        urisIgnoreCase.addAll( uris );
        for ( Object[] row : result ) {
            //noinspection unchecked
            Class<? extends Identifiable> clazz = ( Class<? extends Identifiable> ) row[0];
            Long eeId = ( Long ) row[6];
            for ( int i = 1; i < 6; i++ ) {
                if ( row[i] != null ) {
                    String uri = ( String ) row[i];
                    if ( urisIgnoreCase.contains( uri ) ) {
                        result2.computeIfAbsent( clazz, k -> new HashMap<>() )
                                .computeIfAbsent( uri, k -> new HashSet<>() )
                                .add( eeId );
                    }
                }
            }
        }

        return result2;
    }

    /**
     * Build the {@code findExperimentsByUris} query as a {@code UNION ALL} of per-column
     * range scans, one arm per URI column the caller asked for. Each arm references the
     * shared {@code :uris} parameter binding (and {@code :taxonId} / ACL params when set),
     * so the parameter list signature matches the legacy single-statement form.
     */
    private String buildFindExperimentsByUrisUnionAll( boolean includeSubjects, boolean includePredicates, boolean includeObjects, @Nullable Taxon taxon, boolean rankByLevel ) {
        Assert.isTrue( includeSubjects || includePredicates || includeObjects, "At least one of the source URIs must be included." );
        List<String> uriColumns = new ArrayList<>( 5 );
        if ( includeSubjects ) {
            uriColumns.add( "VALUE_URI" );
        }
        if ( includePredicates ) {
            uriColumns.add( "PREDICATE_URI" );
            uriColumns.add( "SECOND_PREDICATE_URI" );
        }
        if ( includeObjects ) {
            uriColumns.add( "OBJECT_URI" );
            uriColumns.add( "SECOND_OBJECT_URI" );
        }
        String aclWhere = EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "T.EXPRESSION_EXPERIMENT_FK", "T.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" );
        String taxonJoin = taxon != null ? " join INVESTIGATION I on T.EXPRESSION_EXPERIMENT_FK = I.ID " : "";
        String taxonWhere = taxon != null ? " and I.TAXON_FK = :taxonId" : "";
        StringBuilder sb = new StringBuilder();
        if ( rankByLevel ) {
            // wrap the UNION ALL so ORDER BY applies to the combined result
            sb.append( "select U.`LEVEL`, U.VALUE_URI, U.PREDICATE_URI, U.OBJECT_URI, U.SECOND_PREDICATE_URI, U.SECOND_OBJECT_URI, U.EXPRESSION_EXPERIMENT_FK from (" );
        }
        for ( int i = 0; i < uriColumns.size(); i++ ) {
            if ( i > 0 ) {
                sb.append( " union all " );
            }
            sb.append( "select T.`LEVEL`, T.VALUE_URI, T.PREDICATE_URI, T.OBJECT_URI, T.SECOND_PREDICATE_URI, T.SECOND_OBJECT_URI, T.EXPRESSION_EXPERIMENT_FK from EXPRESSION_EXPERIMENT2CHARACTERISTIC T" )
                    .append( taxonJoin )
                    .append( " where T." ).append( uriColumns.get( i ) ).append( " in (:uris)" )
                    .append( taxonWhere )
                    .append( aclWhere );
        }
        if ( rankByLevel ) {
            sb.append( ") U order by FIELD(U.`LEVEL`, :eeClass, :edClass, :bmClass)" );
        }
        return sb.toString();
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
    public Collection<Characteristic> findByUri( String uri, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        if ( StringUtils.isBlank( uri ) )
            return new HashSet<>();
        Query<?> q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select C.ID from CHARACTERISTIC as C where C.VALUE_URI = :uri"
                        + ( category != null ? " and " + createCategoryConstraint( "C", "category", category ) : "" )
                        + ( parentClasses != null || !includeNoParents ? " and " + createOwningEntityConstraint( parentClasses, includeNoParents ) : "" ) )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setParameter( "uri", uri );
        if ( category != null ) {
            q.setParameter( "category", category );
        }
        q.setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE );
        //noinspection unchecked
        return loadByIds( ( List<Long> ) q.list() );
    }

    @Override
    public Collection<Characteristic> findByUriInAnySlot( String uri ) {
        if ( StringUtils.isBlank( uri ) ) {
            return new HashSet<>();
        }
        Query<?> q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select C.ID from CHARACTERISTIC as C where "
                        + "C.VALUE_URI = :uri or C.CATEGORY_URI = :uri or C.PREDICATE_URI = :uri "
                        + "or C.SECOND_PREDICATE_URI = :uri or C.OBJECT_URI = :uri or C.SECOND_OBJECT_URI = :uri" )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setParameter( "uri", uri );
        //noinspection unchecked
        return loadByIds( ( List<Long> ) q.list() );
    }

    @Override
    public Collection<Long> findExperimentIdsByUriInAnySlot( String uri ) {
        if ( StringUtils.isBlank( uri ) ) {
            return new HashSet<>();
        }
        //noinspection unchecked
        List<Long> ids = ( List<Long> ) this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select distinct T.EXPRESSION_EXPERIMENT_FK as ID "
                        + "from EXPRESSION_EXPERIMENT2CHARACTERISTIC T where T.EXPRESSION_EXPERIMENT_FK is not null "
                        + "and (T.VALUE_URI = :uri or T.CATEGORY_URI = :uri or T.PREDICATE_URI = :uri "
                        + "or T.SECOND_PREDICATE_URI = :uri or T.OBJECT_URI = :uri or T.SECOND_OBJECT_URI = :uri)" )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setParameter( "uri", uri )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .list();
        return new LinkedHashSet<>( ids );
    }

    @Override
    public Map<String, CharacteristicDao.UsageExample> findRepresentativeUsageByValueUris( Collection<String> valueUris ) {
        if ( valueUris == null || valueUris.isEmpty() ) {
            return Collections.emptyMap();
        }
        // ACL-restricted: exposes a specific dataset + statement, so it goes through the same EE2C ACL clause
        // as the usage-frequency queries. Matches on VALUE_URI (the term as a tag value / statement subject).
        String aclWhere = EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(), "T.EXPRESSION_EXPERIMENT_FK", "T.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" );
        Query query = getSessionFactory().getCurrentSession().createNativeQuery(
                        // VALUE / PREDICATE / OBJECT (and the SECOND_ variants) are reserved words — backtick
                        // them like LEVEL so both MySQL and H2 (MODE=MYSQL) parse the projection.
                        "select T.`LEVEL`, T.CATEGORY, T.CATEGORY_URI, T.`VALUE`, T.VALUE_URI, "
                                + "T.`PREDICATE`, T.PREDICATE_URI, T.`OBJECT`, T.OBJECT_URI, "
                                + "T.`SECOND_PREDICATE`, T.SECOND_PREDICATE_URI, T.`SECOND_OBJECT`, T.SECOND_OBJECT_URI, "
                                + "T.EXPRESSION_EXPERIMENT_FK "
                                + "from EXPRESSION_EXPERIMENT2CHARACTERISTIC T "
                                + "where T.VALUE_URI in (:uris)" + aclWhere
                                // deterministic representative pick: lowest accessible experiment id per URI
                                + " order by T.EXPRESSION_EXPERIMENT_FK" )
                .addScalar( "LEVEL", StandardBasicTypes.CLASS )
                .addScalar( "CATEGORY", StandardBasicTypes.STRING )
                .addScalar( "CATEGORY_URI", StandardBasicTypes.STRING )
                .addScalar( "VALUE", StandardBasicTypes.STRING )
                .addScalar( "VALUE_URI", StandardBasicTypes.STRING )
                .addScalar( "PREDICATE", StandardBasicTypes.STRING )
                .addScalar( "PREDICATE_URI", StandardBasicTypes.STRING )
                .addScalar( "OBJECT", StandardBasicTypes.STRING )
                .addScalar( "OBJECT_URI", StandardBasicTypes.STRING )
                .addScalar( "SECOND_PREDICATE", StandardBasicTypes.STRING )
                .addScalar( "SECOND_PREDICATE_URI", StandardBasicTypes.STRING )
                .addScalar( "SECOND_OBJECT", StandardBasicTypes.STRING )
                .addScalar( "SECOND_OBJECT_URI", StandardBasicTypes.STRING )
                .addScalar( "EXPRESSION_EXPERIMENT_FK", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( Characteristic.class );
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        query.setCacheable( true );

        List<Object[]> rows;
        if ( valueUris.size() > MAX_PARAMETER_LIST_SIZE ) {
            rows = listByBatch( query, "uris", valueUris, 2048, -1 );
        } else {
            //noinspection unchecked
            rows = query.setParameterList( "uris", optimizeParameterList( valueUris ) ).list();
        }

        // First accessible row per VALUE_URI wins (rows are ordered by experiment id). Case-insensitive key
        // match mirrors findExperimentsByUrisInternal so a URI cased differently in the request still lands.
        TreeSet<String> urisIgnoreCase = new TreeSet<>( String.CASE_INSENSITIVE_ORDER );
        urisIgnoreCase.addAll( valueUris );
        Map<String, CharacteristicDao.UsageExample> result = new HashMap<>();
        for ( Object[] r : rows ) {
            String valueUri = ( String ) r[4];
            if ( valueUri == null || !urisIgnoreCase.contains( valueUri ) || result.containsKey( valueUri ) ) {
                continue;
            }
            //noinspection unchecked
            result.put( valueUri, new CharacteristicDao.UsageExample(
                    ( Class<? extends Identifiable> ) r[0], ( String ) r[1], ( String ) r[2], ( String ) r[3], valueUri,
                    ( String ) r[5], ( String ) r[6], ( String ) r[7], ( String ) r[8],
                    ( String ) r[9], ( String ) r[10], ( String ) r[11], ( String ) r[12],
                    r[13] != null ? ( Long ) r[13] : 0L ) );
        }
        return result;
    }

    @Override
    public Characteristic findBestByUri( String uri ) {
        return ( Characteristic ) getSessionFactory().getCurrentSession()
                .createQuery( "select c from Characteristic c "
                        + "where valueUri = :uri "
                        + "group by c.value "
                        + "having c.value is not null "
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
        // Two-query group-by-then-load: the prior shape projected {C.*} alongside a GROUP BY on
        // coalesce(VALUE_URI, VALUE), which strict ONLY_FULL_GROUP_BY (MySQL 5.7+ default sql_mode)
        // rejects and which silently picked an arbitrary row per group under relaxed mode. The
        // aggregate query below yields (normalized-key, MIN(ID)) pairs — strict-mode safe and
        // deterministic — then load() reassembles the Characteristic per representative ID.
        //noinspection unchecked
        List<Object[]> keys = ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, MIN(C.ID) as REP_ID from CHARACTERISTIC C "
                        + "where VALUE_URI = :valueUri "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "REP_ID", StandardBasicTypes.LONG )
                .setParameter( "valueUri", valueUri )
                .list();
        return reassembleByRepresentativeId( keys );
    }

    @Override
    public Map<String, Characteristic> findByValueLikeGroupedByNormalizedValue( String valueLike, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        if ( isParentClassesEmpty( parentClasses, includeNoParents ) ) {
            return Collections.emptyMap();
        }
        // See findByValueUriGroupedByNormalizedValue for rationale: aggregate by normalized key,
        // pick MIN(ID) as the per-group representative, then bulk-load and rebuild the map.
        //noinspection unchecked
        List<Object[]> keys = ( List<Object[]> ) this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, MIN(C.ID) as REP_ID from CHARACTERISTIC C "
                        + "where `VALUE` like :valueLike "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "REP_ID", StandardBasicTypes.LONG )
                .setParameter( "valueLike", valueLike )
                .list();
        return reassembleByRepresentativeId( keys );
    }

    /**
     * Resolve (normalized-key, representative-ID) tuples from the group-by-then-load pattern into
     * a Map keyed by normalized value with the matching {@link Characteristic} entity as the value.
     * Uses a direct {@code WHERE id IN (...)} HQL query so deleted IDs are silently omitted rather
     * than causing {@code ObjectNotFoundException} via the L1/L2-cache fast path in
     * {@link #load(Collection)}.
     */
    private Map<String, Characteristic> reassembleByRepresentativeId( List<Object[]> keys ) {
        if ( keys.isEmpty() ) {
            return Collections.emptyMap();
        }
        Set<Long> repIds = new HashSet<>( keys.size() );
        for ( Object[] row : keys ) {
            repIds.add( ( Long ) row[1] );
        }
        // Use a direct WHERE-IN query rather than load(Collection) to ensure rows deleted
        // since the aggregate query ran are silently skipped (no ObjectNotFoundException).
        //noinspection unchecked
        List<Characteristic> loaded = ( List<Characteristic> ) getSessionFactory().getCurrentSession()
                .createQuery( "select c from Characteristic c where c.id in :ids" )
                .setParameterList( "ids", repIds )
                .list();
        Map<Long, Characteristic> byId = new HashMap<>( loaded.size() );
        for ( Characteristic c : loaded ) {
            byId.put( c.getId(), c );
        }
        Map<String, Characteristic> result = new HashMap<>( keys.size() );
        for ( Object[] row : keys ) {
            Characteristic c = byId.get( ( Long ) row[1] );
            if ( c != null ) {
                result.put( ( String ) row[0], c );
            }
        }
        return result;
    }

    @Override
    public Map<String, Map<String, Long>> findEeCountsByUriGroupedByCategory( Collection<String> uris ) {
        if ( uris.isEmpty() ) {
            return Collections.emptyMap();
        }
        // EE2C-rooted aggregation: VALUE_URI is indexed (EE2C_VALUE_URI_VALUE) so the IN-clause
        // is a range scan; GROUP BY (VALUE_URI, CATEGORY) tallies distinct experiments per (URI,
        // category) pair. Same magnitude as the usageCount probe — typeahead-friendly for the
        // top-N kept URIs. Cacheable + synchronised on the EE2C query space so curator-driven
        // tag changes invalidate cleanly.
        Query q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select VALUE_URI as V, CATEGORY as C, count(distinct EXPRESSION_EXPERIMENT_FK) as N "
                        + "from EXPRESSION_EXPERIMENT2CHARACTERISTIC "
                        + "where VALUE_URI in :uris and CATEGORY is not null "
                        + "group by VALUE_URI, CATEGORY" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "C", StandardBasicTypes.STRING )
                .addScalar( "N", StandardBasicTypes.LONG )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( Characteristic.class )
                .setCacheable( true );
        Map<String, Map<String, Long>> out = new HashMap<>();
        QueryUtils.<String, Object[]>streamByBatch( q, "uris", uris, 2048 )
                .forEach( row -> {
                    String uri = ( String ) row[0];
                    String category = ( String ) row[1];
                    Long count = ( Long ) row[2];
                    out.computeIfAbsent( uri, k -> new HashMap<>() ).put( category, count );
                } );
        return out;
    }

    @Override
    public Map<String, Long> findEeCountsByUriForOriginalValue( Collection<String> uris, String originalValue ) {
        return findEeCountsByUriForOriginalValue( uris, originalValue, Collections.emptySet() );
    }

    @Override
    public Map<String, Long> findEeCountsByUriForOriginalValue( Collection<String> uris, String originalValue,
            Collection<Long> excludedExperimentIds ) {
        String wanted = originalValue != null ? originalValue.trim().toLowerCase( Locale.ROOT ) : "";
        if ( uris.isEmpty() || wanted.isEmpty() ) {
            return Collections.emptyMap();
        }
        // A value with no letters in it is a quantity, not a name — a dose, a timepoint, a
        // concentration, a replicate number. `24` appears as an original value across unrelated
        // experiments meaning 24 hours, 24 degrees and 24 samples, so a count of it says nothing
        // about which term anyone meant and would hand whichever candidate happened to collect
        // those rows a decisive-looking score. Refuse to form a prior from one.
        if ( wanted.chars().noneMatch( Character::isLetter ) ) {
            return Collections.emptyMap();
        }
        // GEO ships the submitter's string with the characteristic field it came from glued on the
        // front — `treatment: DMSO`, `agent: DMSO`, `vehicle: DMSO` — and on the production corpus
        // that prefixed form is by far the COMMON one (439 of DMSO's 508 experiments; the bare
        // string accounts for 24). Counting only the bare string would miss most of the evidence,
        // so the suffix pattern picks up any `<field>: <value>` framing. Trailing content is
        // deliberately not matched: whoever wrote `0.3% DMSO` or `DMSO for 24h` wrote a different
        // string, and folding those in would credit this string with annotations nobody spelled
        // this way.
        //
        // lower() on both sides rather than leaning on MySQL's case-insensitive collation, so the
        // H2 test path agrees with production. It costs nothing here: there is no index on
        // ORIGINAL_VALUE to forfeit, and the indexed VALUE_URI IN-clause is what bounds the scan.
        boolean excluding = excludedExperimentIds != null && !excludedExperimentIds.isEmpty();
        Query q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select VALUE_URI as V, count(distinct EXPRESSION_EXPERIMENT_FK) as N "
                        + "from EXPRESSION_EXPERIMENT2CHARACTERISTIC "
                        + "where VALUE_URI in :uris and ORIGINAL_VALUE is not null "
                        + ( excluding ? "and EXPRESSION_EXPERIMENT_FK not in :excludedEeIds " : "" )
                        + "and (lower(ORIGINAL_VALUE) = :wanted "
                        + "or lower(ORIGINAL_VALUE) like :prefixed escape '" + LIKE_ESCAPE + "') "
                        + "group by VALUE_URI" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "N", StandardBasicTypes.LONG )
                .setParameter( "wanted", wanted )
                .setParameter( "prefixed", "%: " + escapeForLike( wanted ) )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( Characteristic.class )
                .setCacheable( true );
        if ( excluding ) {
            q.setParameterList( "excludedEeIds", excludedExperimentIds );
        }
        Map<String, Long> out = new HashMap<>();
        QueryUtils.<String, Object[]>streamByBatch( q, "uris", uris, 2048 )
                // A URI can only appear once per batch, but a URI set larger than the batch size is
                // split across queries, so merge rather than overwrite.
                .forEach( row -> out.merge( ( String ) row[0], ( Long ) row[1], Long::sum ) );
        return out;
    }

    @Override
    public List<PriorCurationUsage> findPriorCurationByOriginalValue( String originalValue, int maxResults ) {
        return findPriorCurationByOriginalValue( originalValue, maxResults, Collections.emptySet() );
    }

    @Override
    public List<PriorCurationUsage> findPriorCurationByOriginalValue( String originalValue, int maxResults,
            Collection<Long> excludedExperimentIds ) {
        String wanted = originalValue != null ? originalValue.trim().toLowerCase( Locale.ROOT ) : "";
        if ( wanted.isEmpty() || wanted.chars().noneMatch( Character::isLetter ) ) {
            return Collections.emptyList();
        }
        // No VALUE_URI restriction here, unlike the sibling method — the whole point is to find
        // terms the caller does NOT already have in hand. That costs a scan of EE2C rather than an
        // index range (~1s against production's 2.5M rows), which is why this is opt-in at the web
        // layer and cached on the string alone. The result depends on nothing but the string, so
        // unlike the candidate-restricted tally a cached entry is always complete.
        //
        // The label comes off EE2C rather than from resolving the term, which keeps the result
        // readable for ontologies that are not loaded — including the flat lexical catalogues.
        // MIN picks it: the stored VALUE for a given URI is the ontology label and barely varies,
        // and MIN is both deterministic and portable, where a most-frequent pick would need
        // MySQL-only aggregation and break the H2 test path.
        boolean excluding = excludedExperimentIds != null && !excludedExperimentIds.isEmpty();
        Query q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select VALUE_URI as V, min(`VALUE`) as L, "
                        + "count(distinct EXPRESSION_EXPERIMENT_FK) as N "
                        + "from EXPRESSION_EXPERIMENT2CHARACTERISTIC "
                        + "where VALUE_URI is not null and ORIGINAL_VALUE is not null "
                        + ( excluding ? "and EXPRESSION_EXPERIMENT_FK not in :excludedEeIds " : "" )
                        + "and (lower(ORIGINAL_VALUE) = :wanted "
                        + "or lower(ORIGINAL_VALUE) like :prefixed escape '" + LIKE_ESCAPE + "') "
                        + "group by VALUE_URI order by N desc" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "L", StandardBasicTypes.STRING )
                .addScalar( "N", StandardBasicTypes.LONG )
                .setParameter( "wanted", wanted )
                .setParameter( "prefixed", "%: " + escapeForLike( wanted ) )
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( ExpressionExperiment.class )
                .addSynchronizedEntityClass( Characteristic.class )
                .setCacheable( true );
        if ( excluding ) {
            q.setParameterList( "excludedEeIds", excludedExperimentIds );
        }
        // Deliberately NOT capped in SQL: the cap is applied below, after the total is known, so
        // that `agreement` is a share of everything curators did with this string rather than of
        // the handful of rows that survived truncation. A string maps to few distinct terms in
        // practice (`dmso` 2, the worst offender `control` 24), so reading them all is cheap.
        //noinspection unchecked
        List<Object[]> rows = q.list();
        long total = 0;
        for ( Object[] row : rows ) {
            total += ( Long ) row[2];
        }
        List<PriorCurationUsage> out = new ArrayList<>( rows.size() );
        for ( Object[] row : rows ) {
            if ( maxResults > 0 && out.size() >= maxResults ) {
                break;
            }
            long n = ( Long ) row[2];
            out.add( new PriorCurationUsage( ( String ) row[0], ( String ) row[1], n,
                    total > 0 ? ( double ) n / total : 0.0 ) );
        }
        return out;
    }

    /**
     * Escape character for {@code LIKE} patterns built from caller-supplied text. Backslash is
     * avoided because it is also MySQL's string-literal escape, which makes the doubling rules
     * ambiguous to read and easy to get wrong.
     */
    private static final String LIKE_ESCAPE = "!";

    /**
     * Neutralise {@code LIKE} wildcards in caller-supplied text. Without this, a query string
     * containing {@code _} (common in annotation values — {@code TNF_alpha}) would match any
     * single character, and one containing {@code %} would match anything at all.
     */
    private static String escapeForLike( String s ) {
        return s.replace( LIKE_ESCAPE, LIKE_ESCAPE + LIKE_ESCAPE )
                .replace( "%", LIKE_ESCAPE + "%" )
                .replace( "_", LIKE_ESCAPE + "_" );
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
                .createNativeQuery( "select lower(coalesce(VALUE_URI, `VALUE`)) as V, count(*) as COUNT from CHARACTERISTIC C "
                        + "where VALUE_URI in :uris "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" )
                        + "group by coalesce(VALUE_URI, `VALUE`)" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "COUNT", StandardBasicTypes.LONG );
        return QueryUtils.<String, Object[]>streamByBatch( q, "uris", uris, 2048 )
                .collect( Collectors.groupingBy( row -> ( String ) row[0], Collectors.summingLong( row -> ( Long ) row[1] ) ) );
    }

    @Override
    public Map<String, String> findValueGroupedByValueUri( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, boolean includePredicates, boolean includeObjects, int maxResults ) {
        Map<String, String> result = new HashMap<>();
        //noinspection unchecked
        // MAX(`VALUE`) makes this ONLY_FULL_GROUP_BY-compliant (MySQL 5.7+ default sql_mode).
        // The method's contract is "one representative VALUE per VALUE_URI" — different rows
        // carrying the same URI may have stylistic differences (case, whitespace) in the free-
        // text VALUE column; any single representative satisfies the caller, and MAX picks
        // deterministically across runs (the prior relaxed-mode picker was implementation-
        // defined).
        List<Object[]> result1 = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select VALUE_URI, MAX(`VALUE`) from CHARACTERISTIC C "
                        + "where VALUE_URI is not null "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" ) + " "
                        + "group by VALUE_URI" )
                .setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE )
                .list();
        for ( Object[] row : result1 ) {
            result.put( ( String ) row[0], ( String ) row[1] );
        }
        if ( includePredicates ) {
            //noinspection unchecked
            // MAX(PREDICATE) / MAX(SECOND_PREDICATE) make this ONLY_FULL_GROUP_BY-compliant.
            // Semantics match the VALUE branch: one representative label per (predicate URI,
            // second-predicate URI) pair.
            List<Object[]> result2 = this.getSessionFactory().getCurrentSession()
                    .createNativeQuery( "select PREDICATE_URI, MAX(PREDICATE), SECOND_PREDICATE_URI, MAX(SECOND_PREDICATE) from CHARACTERISTIC C "
                            + "where PREDICATE_URI is not null or SECOND_PREDICATE_URI is not null "
                            + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" ) + " "
                            + "group by PREDICATE_URI, SECOND_PREDICATE_URI" )
                    .setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE )
                    .list();
            for ( Object[] row : result2 ) {
                if ( row[0] != null ) {
                    result.put( ( String ) row[0], ( String ) row[1] );
                }
                if ( row[2] != null ) {
                    result.put( ( String ) row[2], ( String ) row[3] );
                }
            }
        }
        if ( includeObjects ) {
            //noinspection unchecked
            // MAX(OBJECT) / MAX(SECOND_OBJECT) make this ONLY_FULL_GROUP_BY-compliant.
            // Semantics match the VALUE branch: one representative label per (object URI,
            // second-object URI) pair.
            List<Object[]> result3 = this.getSessionFactory().getCurrentSession()
                    .createNativeQuery( "select OBJECT_URI, MAX(OBJECT), SECOND_OBJECT_URI, MAX(SECOND_OBJECT) from CHARACTERISTIC C "
                            + "where OBJECT_URI is not null or SECOND_OBJECT_URI is not null "
                            + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" ) + " "
                            + "group by OBJECT_URI, SECOND_OBJECT_URI" )
                    .setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE )
                    .list();
            for ( Object[] row : result3 ) {
                if ( row[0] != null ) {
                    result.put( ( String ) row[0], ( String ) row[1] );
                }
                if ( row[2] != null ) {
                    result.put( ( String ) row[2], ( String ) row[3] );
                }
            }
        }
        return result;
    }

    @Override
    public Map<String, String> findCategoryGroupedByCategoryUri( @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        Map<String, String> result = new HashMap<>();
        //noinspection unchecked
        // MAX(CATEGORY) keeps this ONLY_FULL_GROUP_BY-compliant, same as the VALUE branch of
        // findValueGroupedByValueUri: one representative label per CATEGORY_URI.
        List<Object[]> rows = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select CATEGORY_URI, MAX(CATEGORY) from CHARACTERISTIC C "
                        + "where CATEGORY_URI is not null "
                        + ( parentClasses != null || includeNoParents ? "and " + createOwningEntityConstraint( parentClasses, includeNoParents ) + " " : "" ) + " "
                        + "group by CATEGORY_URI" )
                .setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE )
                .list();
        for ( Object[] row : rows ) {
            result.put( ( String ) row[0], ( String ) row[1] );
        }
        return result;
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
    public Collection<Characteristic> findByValueLike( String search, @Nullable String category, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents, int maxResults ) {
        // Fast path: EE-scoped autocomplete (the OntologyServiceImpl callsite). Route the
        // VALUE LIKE prefix scan through the denormalized EE2C table (~2.5M rows) instead of
        // CHARACTERISTIC (~12M rows). EE2C is populated by TableMaintenanceUtilImpl with one
        // row per (EE, category, value) triple where the characteristic resolves to an
        // ExpressionExperiment - the exact set the legacy createOwningEntityConstraint with
        // parentClasses={ExpressionExperiment} produces (via the EE2C_EE_QUERY population).
        // EE2C carries the same VALUE / CATEGORY / *_URI columns as CHARACTERISTIC so the
        // CharacteristicValueObject the autocomplete builds is unaffected.
        if ( parentClasses != null && parentClasses.size() == 1
                && parentClasses.contains( ExpressionExperiment.class )
                && !includeNoParents ) {
            return findByValueLikeViaEE2C( search, category, maxResults );
        }
        Query<?> q = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select C.ID from CHARACTERISTIC as C where C.`VALUE` like :search"
                        + ( category != null ? " and " + createCategoryConstraint( "C", "category", category ) : "" )
                        + ( parentClasses != null || !includeNoParents ? " and " + createOwningEntityConstraint( parentClasses, includeNoParents ) : "" ) )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setParameter( "search", search );
        if ( category != null ) {
            q.setParameter( "category", category );
        }
        q.setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE );
        //noinspection unchecked
        return loadByIds( ( List<Long> ) q.list() );
    }

    /**
     * Two-step EE2C-rooted autocomplete: probe EE2C.VALUE (already EE-scoped, ~5x smaller
     * than CHARACTERISTIC.VALUE), collect distinct Characteristic IDs, then load the
     * Characteristic entities by ID. The second step is bounded by maxResults so the
     * Characteristic load is a cheap by-id batch.
     */
    private Collection<Characteristic> findByValueLikeViaEE2C( String search, @Nullable String category, int maxResults ) {
        NativeQuery<?> idQuery = this.getSessionFactory().getCurrentSession()
                .createNativeQuery( "select distinct T.ID from EXPRESSION_EXPERIMENT2CHARACTERISTIC T where T.`VALUE` like :search and T.LEVEL = :level"
                        + ( category != null ? " and " + createCategoryConstraint( "T", "category", category ) : "" ) )
                .addScalar( "ID", StandardBasicTypes.LONG )
                .setParameter( "search", search )
                .setParameter( "level", ExpressionExperiment.class )
                // invalidate when EE2C is repopulated
                .addSynchronizedQuerySpace( EE2C_QUERY_SPACE )
                .addSynchronizedEntityClass( Characteristic.class );
        if ( category != null ) {
            idQuery.setParameter( "category", category );
        }
        idQuery.setMaxResults( maxResults > 0 ? maxResults : Integer.MAX_VALUE );
        //noinspection unchecked
        List<Long> ids = ( List<Long> ) idQuery.list();
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return ( Collection<Characteristic> ) this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from Characteristic c where c.id in :ids" )
                .setParameterList( "ids", ids )
                .list();
    }

    /**
     * Hydrate characteristics by ID through HQL.
     * <p>
     * The finders above filter rows in native SQL because their owning-entity constraints
     * reference physical CHARACTERISTIC foreign-key columns (BIO_MATERIAL_FK, INVESTIGATION_FK,
     * ...) that are not navigable properties on the Characteristic entity — see
     * {@link #createOwningEntityConstraint}. Hydration, however, goes back through HQL rather
     * than a native {@code {C.*}} + {@code addEntity} mapping: the latter mishandles the root
     * {@code @DiscriminatorValue("null")} on MySQL (it throws "Unable to find column position by
     * name: class"), whereas an HQL load resolves the discriminator consistently across
     * dialects and returns {@link Statement} rows as their concrete subtype. This id-then-load
     * split mirrors {@link #findByValueLikeViaEE2C} and {@link #getParents}.
     */
    private Collection<Characteristic> loadByIds( List<Long> ids ) {
        if ( ids.isEmpty() ) {
            return Collections.emptyList();
        }
        Query<Characteristic> query = this.getSessionFactory().getCurrentSession()
                .createQuery( "select c from Characteristic c where c.id in :ids", Characteristic.class );
        return listByBatch( query, "ids", ids, MAX_PARAMETER_LIST_SIZE );
    }

    @Override
    public Collection<Class<? extends Identifiable>> getParentClasses() {
        return OWNING_ENTITIES_CLASSES;
    }

    @Override
    public Map<Characteristic, Identifiable> getParents( Collection<Characteristic> characteristics, @Nullable Collection<Class<? extends Identifiable>> parentClasses, boolean includeNoParents ) {
        Assert.isTrue( parentClasses == null || OWNING_ENTITIES_CLASSES.containsAll( parentClasses ) , "expected true");

        if ( characteristics.isEmpty() || isParentClassesEmpty( parentClasses, includeNoParents ) ) {
            return Collections.emptyMap();
        }

        Map<Long, Characteristic> charById = IdentifiableUtils.getIdMap( characteristics );

        List<OwningEntity> oe = Arrays.stream( OWNING_ENTITIES )
                .filter( fk -> parentClasses == null || parentClasses.contains( fk.getOwningClass() ) )
                .collect( Collectors.toList() );

        NativeQuery<?> query = getSessionFactory().getCurrentSession()
                .createNativeQuery( "select C.ID" + createOwningEntitySelect( oe, includeNoParents ) + " from CHARACTERISTIC C "
                        + "where C.ID in :ids"
                        + ( !oe.isEmpty() || includeNoParents ? " and " + createOwningEntityConstraint( oe, includeNoParents ) : "" ) );

        List<Object[]> result = QueryUtils.listByBatch( query, "ids", charById.keySet(), MAX_PARAMETER_LIST_SIZE );
        // TreeMap, not HashMap: Characteristic.hashCode() is constant (it has to be -- curation edits
        // the fields a content hash would use), so a HashMap keyed by Characteristic puts every entry
        // in one bucket. This map is as large as the batch handed in. The comparator's id tiebreaker
        // keeps distinct persisted characteristics distinct.
        Map<Characteristic, Identifiable> charToParent = new TreeMap<>( Characteristic.getComparator() );
        for ( Object[] row : result ) {
            Number charId = ( Number ) row[0];
            Characteristic c = charById.get( charId.longValue() );
            Collection<Identifiable> parentObjects = new ArrayList<>( 1 );
            for ( int i = 0; i < oe.size(); i++ ) {
                OwningEntity owningEntity = oe.get( i );
                Number entityId = ( Number ) row[i + 1];
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

    @SuppressWarnings("SameParameterValue")
    private String createCategoryConstraint( String alias, String paramName, String category ) {
        return alias + "." + ( category.startsWith( "http://" ) ? "CATEGORY_URI" : "CATEGORY" ) + " = :" + paramName;
    }

    @Override
    protected CharacteristicValueObject doLoadValueObject( Characteristic entity ) {
        return new CharacteristicValueObject( entity );
    }

}