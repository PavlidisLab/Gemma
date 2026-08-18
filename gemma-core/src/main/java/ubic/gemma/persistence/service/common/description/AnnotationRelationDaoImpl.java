/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.common.description;

import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.NativeQuery;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.description.AnnotationRelation;
import ubic.gemma.model.common.description.AnnotationRelationBasis;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.AbstractDao;
import ubic.gemma.persistence.util.EE2CAclQueryUtils;

import static ubic.gemma.persistence.util.QueryUtils.optimizeParameterList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Hibernate implementation of {@link AnnotationRelationDao}.
 *
 * <p>Native SQL throughout. The reads are aggregations over a derived table with an ACL clause that
 * has to be composed per user class, and the ACL helper emits SQL, not HQL - the same reason
 * {@code CharacteristicDaoImpl}'s EE2C reads are native.</p>
 *
 * <p><b>The expensive part happens offline.</b> Every query here is an indexed lookup plus a group-by
 * over a small purpose-built table. There is deliberately no candidate cut, no self-join over
 * EE2C, and no second query stitched to the first by a string key in Java: those existed only to fit
 * a corpus-wide derivation inside a request, and once the derivation runs in the maintenance job
 * there is nothing left to approximate around.</p>
 */
@Repository
public class AnnotationRelationDaoImpl extends AbstractDao<AnnotationRelation> implements AnnotationRelationDao {

    /**
     * Class names as {@code EXPRESSION_EXPERIMENT2CHARACTERISTIC.LEVEL} spells them; the harvest copies
     * the column across verbatim, so the same strings identify where an attesting annotation sat.
     */
    private static final String LEVEL_EE = ExpressionExperiment.class.getName();
    private static final String LEVEL_FV = FactorValue.class.getName();
    private static final String LEVEL_BM = BioMaterial.class.getName();

    @Autowired
    public AnnotationRelationDaoImpl( SessionFactory sessionFactory ) {
        super( AnnotationRelation.class, sessionFactory );
    }

    @Override
    public List<RelationSummary> findRelations( RelationQuery q ) {
        if ( !q.isSeeded() ) {
            // refuse to enumerate the table; every caller knows one end of the relation
            return Collections.emptyList();
        }

        StringBuilder where = new StringBuilder( " where R.BASIS in (:bases)" );
        where.append( uriOrValueConstraint( "SUBJECT", q.getSubjectValueUris(), q.getSubjectValues() ) );
        where.append( uriOrValueConstraint( "OBJECT", q.getObjectValueUris(), q.getObjectValues() ) );
        if ( !q.getPredicateUris().isEmpty() ) {
            where.append( " and R.PREDICATE_URI in (:predicateUris)" );
        }
        if ( !q.getSubjectCategoryUris().isEmpty() ) {
            where.append( " and R.SUBJECT_CATEGORY_URI in (:subjectCategoryUris)" );
        }
        if ( !q.getObjectCategoryUris().isEmpty() ) {
            where.append( " and R.OBJECT_CATEGORY_URI in (:objectCategoryUris)" );
        }
        if ( q.getSeedFromExperimentId() != null ) {
            where.append( seedFromExperimentConstraint( q.getSeedDirection() ) );
        }
        if ( q.getTaxonId() != null ) {
            // a relation with no taxon is not taxon-specific, so it answers every taxon's question
            where.append( " and (R.TAXON_FK is null or R.TAXON_FK = :taxonId)" );
        }
        if ( !q.getExcludedExperimentIds().isEmpty() ) {
            where.append( " and (R.EXPRESSION_EXPERIMENT_FK is null or R.EXPRESSION_EXPERIMENT_FK not in (:excludedIds))" );
        }
        where.append( aclClause() );

        NativeQuery<?> query = getSessionFactory().getCurrentSession().createNativeQuery(
                        "select R.SUBJECT_VALUE as SV, R.SUBJECT_VALUE_URI as SVU, R.SUBJECT_CATEGORY as SC, R.SUBJECT_CATEGORY_URI as SCU, "
                                + "R.PREDICATE as P, R.PREDICATE_URI as PU, "
                                + "R.OBJECT_VALUE as OV, R.OBJECT_VALUE_URI as OVU, R.OBJECT_CATEGORY as OC, R.OBJECT_CATEGORY_URI as OCU, "
                                + "X.ID as TID, X.COMMON_NAME as TCN, X.NCBI_ID as TNCBI, "
                                + "R.BASIS as B, R.SOURCE as SRC, R.SOURCE_VERSION as SRCV, "
                                // asserted rows have no experiment, so this is 0 for them by construction
                                + "count(distinct R.EXPRESSION_EXPERIMENT_FK) as N, "
                                // the evidence split: a factor value (the property varies across samples) and a
                                // whole-experiment tag are not the same claim, and a client renders them apart
                                + "count(distinct case when R.`LEVEL` = :fvLevel then R.EXPRESSION_EXPERIMENT_FK end) as NFV, "
                                + "count(distinct case when R.`LEVEL` = :eeLevel then R.EXPRESSION_EXPERIMENT_FK end) as NTAG, "
                                + "count(distinct case when R.`LEVEL` = :bmLevel then R.EXPRESSION_EXPERIMENT_FK end) as NBM, "
                                + "min(R.EXPRESSION_EXPERIMENT_FK) as EX "
                                + "from ANNOTATION_RELATION R "
                                + "left join TAXON X on X.ID = R.TAXON_FK"
                                + where
                                // ONLY_FULL_GROUP_BY: every projected non-aggregate is grouped. Taxon is part of
                                // the grain because it decides what the relation says, and BASIS is part of it
                                // because two bases naming different terms must not be collapsed into one row.
                                + " group by R.SUBJECT_VALUE, R.SUBJECT_VALUE_URI, R.SUBJECT_CATEGORY, R.SUBJECT_CATEGORY_URI, "
                                + "R.PREDICATE, R.PREDICATE_URI, "
                                + "R.OBJECT_VALUE, R.OBJECT_VALUE_URI, R.OBJECT_CATEGORY, R.OBJECT_CATEGORY_URI, "
                                + "X.ID, X.COMMON_NAME, X.NCBI_ID, R.BASIS, R.SOURCE, R.SOURCE_VERSION" )
                .addScalar( "SV", StandardBasicTypes.STRING )
                .addScalar( "SVU", StandardBasicTypes.STRING )
                .addScalar( "SC", StandardBasicTypes.STRING )
                .addScalar( "SCU", StandardBasicTypes.STRING )
                .addScalar( "P", StandardBasicTypes.STRING )
                .addScalar( "PU", StandardBasicTypes.STRING )
                .addScalar( "OV", StandardBasicTypes.STRING )
                .addScalar( "OVU", StandardBasicTypes.STRING )
                .addScalar( "OC", StandardBasicTypes.STRING )
                .addScalar( "OCU", StandardBasicTypes.STRING )
                .addScalar( "TID", StandardBasicTypes.LONG )
                .addScalar( "TCN", StandardBasicTypes.STRING )
                .addScalar( "TNCBI", StandardBasicTypes.INTEGER )
                .addScalar( "B", StandardBasicTypes.STRING )
                .addScalar( "SRC", StandardBasicTypes.STRING )
                .addScalar( "SRCV", StandardBasicTypes.STRING )
                .addScalar( "N", StandardBasicTypes.LONG )
                .addScalar( "NFV", StandardBasicTypes.LONG )
                .addScalar( "NTAG", StandardBasicTypes.LONG )
                .addScalar( "NBM", StandardBasicTypes.LONG )
                .addScalar( "EX", StandardBasicTypes.LONG )
                .addSynchronizedEntityClass( AnnotationRelation.class )
                .addSynchronizedEntityClass( ExpressionExperiment.class );

        query.setParameterList( "bases", q.getBases().stream().map( Enum::name ).collect( Collectors.toList() ) )
                .setParameter( "fvLevel", LEVEL_FV )
                .setParameter( "eeLevel", LEVEL_EE )
                .setParameter( "bmLevel", LEVEL_BM );
        bindUriOrValue( query, "subject", q.getSubjectValueUris(), q.getSubjectValues() );
        bindUriOrValue( query, "object", q.getObjectValueUris(), q.getObjectValues() );
        if ( !q.getPredicateUris().isEmpty() ) {
            query.setParameterList( "predicateUris", q.getPredicateUris() );
        }
        if ( !q.getSubjectCategoryUris().isEmpty() ) {
            query.setParameterList( "subjectCategoryUris", q.getSubjectCategoryUris() );
        }
        if ( !q.getObjectCategoryUris().isEmpty() ) {
            query.setParameterList( "objectCategoryUris", q.getObjectCategoryUris() );
        }
        if ( q.getSeedFromExperimentId() != null ) {
            query.setParameter( "seedEeId", q.getSeedFromExperimentId() );
        }
        if ( q.getTaxonId() != null ) {
            query.setParameter( "taxonId", q.getTaxonId() );
        }
        if ( !q.getExcludedExperimentIds().isEmpty() ) {
            query.setParameterList( "excludedIds", optimizeParameterList( q.getExcludedExperimentIds() ) );
        }
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        query.setCacheable( true );

        //noinspection unchecked
        List<Object[]> rows = ( List<Object[]> ) query.list();
        if ( rows.isEmpty() ) {
            return Collections.emptyList();
        }

        // The specificity denominator is only computed when a CORPUS row is present, and it is the only
        // basis it means anything for. This is the whole reason the common path stays one query: where
        // a relation is asserted - which is most of them, since curators wrote 10,040 datasets' worth
        // of them - there is nothing to divide by and nothing to ask.
        boolean anyCorpus = rows.stream().anyMatch( r -> AnnotationRelationBasis.CORPUS.name().equals( r[13] ) );
        Map<String, Long> subjectTotals = anyCorpus
                ? countExperimentsBySubject( rows.stream()
                .filter( r -> AnnotationRelationBasis.CORPUS.name().equals( r[13] ) )
                .map( r -> new String[] { ( String ) r[0], ( String ) r[1] } )
                .collect( Collectors.toList() ), q.getTaxonId(), q.getExcludedExperimentIds() )
                : Collections.emptyMap();

        List<RelationSummary> result = new ArrayList<>( rows.size() );
        for ( Object[] r : rows ) {
            AnnotationRelationBasis basis = AnnotationRelationBasis.valueOf( ( String ) r[13] );
            long total = basis == AnnotationRelationBasis.CORPUS
                    ? subjectTotals.getOrDefault( subjectKey( ( String ) r[0], ( String ) r[1] ), 0L )
                    : 0L;
            RelationSummary s = new RelationSummary(
                    ( String ) r[0], ( String ) r[1], ( String ) r[2], ( String ) r[3],
                    ( String ) r[4], ( String ) r[5],
                    ( String ) r[6], ( String ) r[7], ( String ) r[8], ( String ) r[9],
                    ( Long ) r[10], ( String ) r[11], ( Integer ) r[12],
                    basis, ( String ) r[14], ( String ) r[15],
                    asLong( r[16] ), asLong( r[17] ), asLong( r[18] ), asLong( r[19] ),
                    ( Long ) r[20], total );
            if ( s.getNumberOfExperiments() < q.getMinimumSupport() && !basis.isSelfSufficient() ) {
                continue;
            }
            if ( q.getMinimumSpecificity() > 0 && !basis.isSelfSufficient()
                    && s.getSpecificity() < q.getMinimumSpecificity() ) {
                continue;
            }
            result.add( s );
        }
        result.sort( Comparator.comparingDouble( RelationSummary::getScore ).reversed()
                .thenComparing( RelationSummary::getSubjectValue )
                .thenComparing( RelationSummary::getObjectValue ) );
        return q.getMaxResults() > 0 && result.size() > q.getMaxResults()
                ? new ArrayList<>( result.subList( 0, q.getMaxResults() ) )
                : result;
    }

    @Override
    public List<String[]> findRelatedTerms( Collection<String> seedValueUris, Collection<String> seedValues,
            Direction direction, Set<AnnotationRelationBasis> bases, Collection<String> predicateUris,
            @Nullable Long taxonId, Collection<Long> excludedExperimentIds, int maxResults ) {
        if ( seedValueUris.isEmpty() && seedValues.isEmpty() || bases.isEmpty() ) {
            return Collections.emptyList();
        }
        String seedSide = direction == Direction.SUBJECT_TO_OBJECT ? "SUBJECT" : "OBJECT";
        String returnSide = direction == Direction.SUBJECT_TO_OBJECT ? "OBJECT" : "SUBJECT";

        StringBuilder where = new StringBuilder( " where R.BASIS in (:bases)" );
        where.append( uriOrValueConstraint( seedSide, seedValueUris, seedValues ) );
        if ( !predicateUris.isEmpty() ) {
            where.append( " and R.PREDICATE_URI in (:predicateUris)" );
        }
        if ( taxonId != null ) {
            where.append( " and (R.TAXON_FK is null or R.TAXON_FK = :taxonId)" );
        }
        if ( !excludedExperimentIds.isEmpty() ) {
            where.append( " and (R.EXPRESSION_EXPERIMENT_FK is null or R.EXPRESSION_EXPERIMENT_FK not in (:excludedIds))" );
        }
        where.append( aclClause() );

        NativeQuery<?> query = getSessionFactory().getCurrentSession().createNativeQuery(
                        "select distinct R." + returnSide + "_VALUE as V, R." + returnSide + "_VALUE_URI as VU "
                                + "from ANNOTATION_RELATION R" + where )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "VU", StandardBasicTypes.STRING )
                .addSynchronizedEntityClass( AnnotationRelation.class )
                .addSynchronizedEntityClass( ExpressionExperiment.class );
        query.setParameterList( "bases", bases.stream().map( Enum::name ).collect( Collectors.toList() ) );
        bindUriOrValue( query, seedSide.toLowerCase(), seedValueUris, seedValues );
        if ( !predicateUris.isEmpty() ) {
            query.setParameterList( "predicateUris", predicateUris );
        }
        if ( taxonId != null ) {
            query.setParameter( "taxonId", taxonId );
        }
        if ( !excludedExperimentIds.isEmpty() ) {
            query.setParameterList( "excludedIds", optimizeParameterList( excludedExperimentIds ) );
        }
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        query.setCacheable( true );
        if ( maxResults > 0 ) {
            query.setMaxResults( maxResults );
        }
        //noinspection unchecked
        List<Object[]> rows = ( List<Object[]> ) query.list();
        return rows.stream()
                .map( r -> new String[] { ( String ) r[0], ( String ) r[1] } )
                .collect( Collectors.toList() );
    }

    @Override
    public int removeByBasis( AnnotationRelationBasis basis, @Nullable Long experimentId ) {
        return removeByBasis( basis, experimentId, null );
    }

    @Override
    public int removeByBasis( AnnotationRelationBasis basis, @Nullable Long experimentId, @Nullable String source ) {
        String sql = "delete from ANNOTATION_RELATION where BASIS = :basis"
                + ( experimentId != null ? " and EXPRESSION_EXPERIMENT_FK = :eeId" : "" )
                + ( source != null ? " and SOURCE = :source" : "" );
        NativeQuery<?> q = getSessionFactory().getCurrentSession().createNativeQuery( sql )
                .addSynchronizedEntityClass( AnnotationRelation.class );
        q.setParameter( "basis", basis.name() );
        if ( experimentId != null ) {
            q.setParameter( "eeId", experimentId );
        }
        if ( source != null ) {
            q.setParameter( "source", source );
        }
        return q.executeUpdate();
    }

    /**
     * How many experiments carry each subject value at all - the denominator behind
     * {@link RelationSummary#getSpecificity()}, read from EE2C because that is where every annotation
     * on every experiment already is.
     *
     * <p>Taxon is in the grain here and not only in the numerator. Getting that wrong understates
     * specificity for exactly the cross-taxon values that matter most: a value split by taxon on one
     * side and divided by a combined denominator on the other looks less specific than it is, and
     * {@code LRRK2 G2019S} - annotated in both human lines and mouse models - is precisely such a
     * case.</p>
     *
     * <p>Keyed on value <i>and</i> value URI so the same string grounded two different ways stays
     * apart.</p>
     */
    private Map<String, Long> countExperimentsBySubject( List<String[]> subjects, @Nullable Long taxonId,
            Collection<Long> excludedExperimentIds ) {
        List<String> values = subjects.stream()
                .map( s -> s[0] )
                .filter( Objects::nonNull )
                .distinct()
                .collect( Collectors.toList() );
        if ( values.isEmpty() ) {
            return Collections.emptyMap();
        }
        String acl = EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(),
                "S.EXPRESSION_EXPERIMENT_FK", "S.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" );
        NativeQuery<?> query = getSessionFactory().getCurrentSession().createNativeQuery(
                        "select S.`VALUE` as V, S.VALUE_URI as VU, count(distinct S.EXPRESSION_EXPERIMENT_FK) as N "
                                + "from EXPRESSION_EXPERIMENT2CHARACTERISTIC S "
                                + ( taxonId != null ? "join INVESTIGATION I on I.ID = S.EXPRESSION_EXPERIMENT_FK " : "" )
                                + "where S.`VALUE` in (:values)"
                                + ( taxonId != null ? " and I.TAXON_FK = :taxonId" : "" )
                                + ( excludedExperimentIds.isEmpty() ? "" : " and S.EXPRESSION_EXPERIMENT_FK not in (:excludedIds)" )
                                + acl
                                + " group by S.`VALUE`, S.VALUE_URI" )
                .addScalar( "V", StandardBasicTypes.STRING )
                .addScalar( "VU", StandardBasicTypes.STRING )
                .addScalar( "N", StandardBasicTypes.LONG )
                .addSynchronizedEntityClass( ExpressionExperiment.class );
        query.setParameterList( "values", values );
        if ( taxonId != null ) {
            query.setParameter( "taxonId", taxonId );
        }
        if ( !excludedExperimentIds.isEmpty() ) {
            query.setParameterList( "excludedIds", optimizeParameterList( excludedExperimentIds ) );
        }
        EE2CAclQueryUtils.addAclParameters( query, ExpressionExperiment.class );
        query.setCacheable( true );
        //noinspection unchecked
        List<Object[]> rows = ( List<Object[]> ) query.list();
        Map<String, Long> byKey = new LinkedHashMap<>();
        for ( Object[] r : rows ) {
            byKey.merge( subjectKey( ( String ) r[0], ( String ) r[1] ), asLong( r[2] ), Long::sum );
        }
        return byKey;
    }

    /**
     * Seed from everything one experiment is annotated with, matched inside the query.
     *
     * <p>An {@code exists} against EE2C rather than a prior round trip to collect the experiment's
     * annotations: this runs on an experiment page, and a query-per-page-load to build a parameter
     * list is the kind of cost that grows silently with traffic. EE2C is already indexed on the
     * columns being matched.</p>
     *
     * <p>The URI leg and the value leg are not interchangeable. A grounded annotation matches on its
     * URI; an ungrounded one has only its string, and that is common enough on the object side
     * ({@code aortic banding}) that dropping it would lose real relations. Matching an ungrounded
     * seed against a grounded relation would be a different claim, so the value leg only fires when
     * the seed itself has no URI.</p>
     */
    private String seedFromExperimentConstraint( Direction direction ) {
        String side = direction == Direction.SUBJECT_TO_OBJECT ? "SUBJECT" : "OBJECT";
        return " and exists (select 1 from EXPRESSION_EXPERIMENT2CHARACTERISTIC S2"
                + " where S2.EXPRESSION_EXPERIMENT_FK = :seedEeId"
                + " and (S2.VALUE_URI = R." + side + "_VALUE_URI"
                + " or (S2.VALUE_URI is null and R." + side + "_VALUE_URI is null and S2.`VALUE` = R." + side + "_VALUE)))";
    }

    /**
     * ACL, composed so that <b>asserted rows survive it</b>.
     *
     * <p>A row with no experiment is a claim by an ontology or an outside resource. It reveals nothing
     * about what Gemma holds, so filtering it by the caller's dataset permissions would hide public
     * knowledge for no gain. Attested rows go through the same clause EE2C uses, on the same two
     * columns, so support counted here is exactly what the caller may see.</p>
     */
    private String aclClause() {
        String acl = EE2CAclQueryUtils.formNativeAclRestrictionClause( ( SessionFactoryImplementor ) getSessionFactory(),
                "R.EXPRESSION_EXPERIMENT_FK", "R.ACL_IS_AUTHENTICATED_ANONYMOUSLY_MASK" );
        if ( acl.isEmpty() ) {
            // admin: no restriction emitted at all
            return acl;
        }
        // the helper's output starts with " and ", so 1=1 gives it something to attach to
        return " and (R.EXPRESSION_EXPERIMENT_FK is null or (1=1" + acl + "))";
    }

    /**
     * Match a seed on its URI when it has one and on its value string otherwise, OR-ed.
     *
     * <p>The value leg is not a fallback for bad data - it is how ungrounded terms are addressed at
     * all. {@code aortic banding} is a perfectly good curated statement object with no URI, and so are
     * {@code APP/PS1} and {@code 5xFAD} on the subject side.</p>
     */
    private String uriOrValueConstraint( String side, Collection<String> uris, Collection<String> values ) {
        if ( uris.isEmpty() && values.isEmpty() ) {
            return "";
        }
        String p = side.toLowerCase();
        List<String> legs = new ArrayList<>( 2 );
        if ( !uris.isEmpty() ) {
            legs.add( "R." + side + "_VALUE_URI in (:" + p + "Uris)" );
        }
        if ( !values.isEmpty() ) {
            legs.add( "R." + side + "_VALUE in (:" + p + "Values)" );
        }
        return " and (" + String.join( " or ", legs ) + ")";
    }

    private void bindUriOrValue( NativeQuery<?> query, String prefix, Collection<String> uris, Collection<String> values ) {
        if ( !uris.isEmpty() ) {
            query.setParameterList( prefix + "Uris", uris );
        }
        if ( !values.isEmpty() ) {
            query.setParameterList( prefix + "Values", values );
        }
    }

    private static String subjectKey( @Nullable String value, @Nullable String valueUri ) {
        return ( value != null ? value : "" ) + " " + ( valueUri != null ? valueUri : "" );
    }

    private static long asLong( @Nullable Object o ) {
        return o != null ? ( Long ) o : 0L;
    }
}
