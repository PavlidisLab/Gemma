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
import ubic.gemma.model.common.description.AnnotationRelationStatus;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
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

    /**
     * 🛑 {@link ExperimentalDesign}, not {@link FactorValue}.
     *
     * <p>A statement lives on a factor value, so the obvious constant here is {@code FactorValue} --
     * and it matches nothing. EE2C resolves design-level annotations under
     * {@code ExperimentalDesign} and never writes {@code FactorValue} as a level at all, so the entire
     * evidence split silently reported zero for every row. Nothing failed; the numbers were simply
     * always 0, which reads as "this relation is attested nowhere in particular".</p>
     */
    private static final String LEVEL_FV = ExperimentalDesign.class.getName();

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

        // 🛑 An inference may never rest on a refutation. This path is the suppression gate, so a
        // REFUTED row reaching it would license exactly the claim its source denied -- there is no
        // caller for whom that is the right answer, which is why this one has no opt-in.
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
            where.append( alreadyCarriedConstraint( q.getSeedDirection() ) );
        }
        if ( q.getTaxonId() != null ) {
            // a relation with no taxon is not taxon-specific, so it answers every taxon's question
            where.append( " and (R.TAXON_FK is null or R.TAXON_FK = :taxonId)" );
        }
        if ( !q.getExcludedExperimentIds().isEmpty() ) {
            where.append( " and (R.EXPRESSION_EXPERIMENT_FK is null or R.EXPRESSION_EXPERIMENT_FK not in (:excludedIds))" );
        }
        if ( !q.isIncludeRefuted() ) {
            // 🛑 A refuted row states the opposite of an asserted one, so it is out of the default
            // read. "Absent" and "denied" are different answers, and only a caller who asks by name
            // gets the second.
            where.append( " and R.STATUS = :assertedStatus" );
        }
        where.append( aclClause() );

        NativeQuery<?> query = getSessionFactory().getCurrentSession().createNativeQuery(
                        "select R.SUBJECT_VALUE as SV, R.SUBJECT_VALUE_URI as SVU, min(R.SUBJECT_CATEGORY) as SC, R.SUBJECT_CATEGORY_URI as SCU, "
                                + "min(R.PREDICATE) as P, R.PREDICATE_URI as PU, "
                                + "R.OBJECT_VALUE as OV, R.OBJECT_VALUE_URI as OVU, min(R.OBJECT_CATEGORY) as OC, R.OBJECT_CATEGORY_URI as OCU, "
                                + "X.ID as TID, X.COMMON_NAME as TCN, X.NCBI_ID as TNCBI, "
                                + "R.BASIS as B, R.SOURCE as SRC, R.SOURCE_VERSION as SRCV, "
                                // asserted rows have no experiment, so this is 0 for them by construction
                                + "count(distinct R.EXPRESSION_EXPERIMENT_FK) as N, "
                                // the evidence split: a factor value (the property varies across samples) and a
                                // whole-experiment tag are not the same claim, and a client renders them apart
                                + "count(distinct case when R.`LEVEL` = :fvLevel then R.EXPRESSION_EXPERIMENT_FK end) as NFV, "
                                + "count(distinct case when R.`LEVEL` = :eeLevel then R.EXPRESSION_EXPERIMENT_FK end) as NTAG, "
                                + "count(distinct case when R.`LEVEL` = :bmLevel then R.EXPRESSION_EXPERIMENT_FK end) as NBM, "
                                + "min(R.EXPRESSION_EXPERIMENT_FK) as EX, "
                                // appended LAST on purpose: the row mapping below is positional, so a
                                // column inserted mid-projection silently re-reads every field after it
                                + "R.STATUS as ST, "
                                // min() like the other non-grouped text columns: rows sharing a triple,
                                // basis, source and status are one statement, so any of them will do
                                + "min(R.EVIDENCE) as EV "
                                + "from ANNOTATION_RELATION R "
                                + "left join TAXON X on X.ID = R.TAXON_FK"
                                + where
                                // ONLY_FULL_GROUP_BY: every projected non-aggregate is grouped. Taxon is part of
                                // the grain because it decides what the relation says, and BASIS is part of it
                                // because two bases naming different terms must not be collapsed into one row.
                                + " group by R.SUBJECT_VALUE, R.SUBJECT_VALUE_URI, lower(trim(R.SUBJECT_CATEGORY)), R.SUBJECT_CATEGORY_URI, "
                                // grouped on the normalized LABEL, not the raw one. `Disease model` and
                                // `disease model` share TGEMO_00101 and `toward`/`towards` share
                                // RO_0002503; splitting on the spelling fragments one relation's support
                                // across two rows, so every ranking built on it ranks fragments.
                                + "lower(trim(R.PREDICATE)), R.PREDICATE_URI, "
                                + "R.OBJECT_VALUE, R.OBJECT_VALUE_URI, lower(trim(R.OBJECT_CATEGORY)), R.OBJECT_CATEGORY_URI, "
                                // STATUS is part of the grain: an assertion and a refutation of the same
                                // triple are two things a source said, and collapsing them would let one
                                // hide the other
                                + "X.ID, X.COMMON_NAME, X.NCBI_ID, R.BASIS, R.SOURCE, R.SOURCE_VERSION, R.STATUS" )
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
                .addScalar( "ST", StandardBasicTypes.STRING )
                .addScalar( "EV", StandardBasicTypes.STRING )
                .addSynchronizedEntityClass( AnnotationRelation.class )
                .addSynchronizedEntityClass( ExpressionExperiment.class );

        query.setParameterList( "bases", q.getBases().stream().map( Enum::name ).collect( Collectors.toList() ) )
                .setParameter( "fvLevel", LEVEL_FV )
                .setParameter( "eeLevel", LEVEL_EE )
                .setParameter( "bmLevel", LEVEL_BM );
        if ( !q.isIncludeRefuted() ) {
            query.setParameter( "assertedStatus", AnnotationRelationStatus.ASSERTED.name() );
        }
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

        Map<String, Long> breadth = findObjectBreadth( rows.stream()
                .map( r -> ( String ) r[6] )
                .filter( Objects::nonNull )
                .distinct()
                .collect( Collectors.toList() ) );
        Map<String, Long> subjectBreadth = findSubjectBreadth( rows.stream()
                .map( r -> ( String ) r[0] )
                .filter( Objects::nonNull )
                .distinct()
                .collect( Collectors.toList() ) );

        List<RelationSummary> result = new ArrayList<>( rows.size() );
        for ( Object[] r : rows ) {
            AnnotationRelationBasis basis = AnnotationRelationBasis.valueOf( ( String ) r[13] );
            long total = basis == AnnotationRelationBasis.CORPUS
                    ? subjectTotals.getOrDefault( subjectKey( ( String ) r[0], ( String ) r[1] ), 0L )
                    : 0L;
            // Named rather than positional: the projection is read by index, so this is the one place
            // where a column and a field have to be lined up by hand, and it is worth being able to
            // see that they are.
            RelationSummary s = RelationSummary.builder()
                    .subjectValue( ( String ) r[0] )
                    .subjectValueUri( ( String ) r[1] )
                    .subjectCategory( ( String ) r[2] )
                    .subjectCategoryUri( ( String ) r[3] )
                    .predicate( ( String ) r[4] )
                    .predicateUri( ( String ) r[5] )
                    .objectValue( ( String ) r[6] )
                    .objectValueUri( ( String ) r[7] )
                    .objectCategory( ( String ) r[8] )
                    .objectCategoryUri( ( String ) r[9] )
                    .taxonId( ( Long ) r[10] )
                    .taxonCommonName( ( String ) r[11] )
                    .taxonNcbiId( ( Integer ) r[12] )
                    .basis( basis )
                    .status( AnnotationRelationStatus.valueOf( ( String ) r[21] ) )
                    .source( ( String ) r[14] )
                    .sourceVersion( ( String ) r[15] )
                    .numberOfExperiments( asLong( r[16] ) )
                    .numberOfExperimentsAtFactorValue( asLong( r[17] ) )
                    .numberOfExperimentsAtTag( asLong( r[18] ) )
                    .numberOfExperimentsAtBioMaterial( asLong( r[19] ) )
                    .exampleExperimentId( ( Long ) r[20] )
                    .numberOfExperimentsWithSubject( total )
                    .objectBreadth( breadth.getOrDefault( breadthKey( ( String ) r[6] ), 0L ) )
                    // subject breadth is per predicate, so the key carries the row's predicate --
                    // its URI where it has one, matching the coalesce the count grouped on
                    .subjectBreadth( subjectBreadth.getOrDefault(
                            breadthKey( ( String ) r[0], r[5] != null ? ( String ) r[5] : ( String ) r[4] ), 0L ) )
                    .evidence( ( String ) r[22] )
                    .build();
            if ( s.getNumberOfExperiments() < q.getMinimumSupport() && !basis.isSelfSufficient() ) {
                continue;
            }
            if ( q.getMaximumObjectBreadth() > 0 && s.getObjectBreadth() > q.getMaximumObjectBreadth() ) {
                continue;
            }
            if ( q.getMaximumSubjectBreadth() > 0 && s.getSubjectBreadth() > q.getMaximumSubjectBreadth() ) {
                continue;
            }
            if ( q.isTermLevelOnly()
                    && s.getTopicality() != ubic.gemma.model.common.description.RelationTopicality.TERM_LEVEL ) {
                continue;
            }
            if ( q.getMinimumSpecificity() > 0 && !basis.isSelfSufficient()
                    && s.getSpecificity() < q.getMinimumSpecificity() ) {
                continue;
            }
            // 🛑 An experiment-seeded read may only walk FORWARDS. The seed has to be the end that
            // implies -- the subject of a SUBJECT_IMPLIES_OBJECT row, the object of an
            // OBJECT_IMPLIES_SUBJECT one -- and the seed side is fixed by the direction the caller
            // seeded from, so `licenses` is asked exactly the question the store is symmetric about.
            //
            // Reported by Paul on GSE315959, 2026-08-27. Its one grounded annotation is `prostate
            // gland`, which is the CONCLUSION of `CLO_0037208 derives from anatomic part`; seeded from
            // the object side the query read 169 Cellosaurus lines back out of it. A cell line implies
            // its organ and an organ implies no cell line, so those rows were never the dataset's to
            // draw.
            //
            // NEITHER is dropped with them, which is the same rule and not a second one: a row that
            // licenses no inference is not an inferred concept. It costs nothing by default -- an
            // EXPERIMENT_LEVEL row is already out via `termLevelOnly` -- but a caller that passes
            // `includeExperimentLevel` alongside `dataset` now gets nothing back for it.
            if ( q.getSeedFromExperimentId() != null
                    && !s.getInferenceDirection().licenses( q.getSeedDirection() == Direction.SUBJECT_TO_OBJECT ) ) {
                continue;
            }
            result.add( s );
        }
        // 🛑 Breadth ASCENDING breaks ties the score left, and only those. An asserted basis carries no
        // support -- an ontology's claim holds independently of anything Gemma stores -- so every row
        // of it scores identically and the sort used to fall through to alphabetical. uib measured the
        // result on `imatinib` after CHEBI's roles came back whole: ten roles, all support 0, ordered
        // a-z, so `antihypertensive agent` (borne by 487 chemicals) led and `tyrosine kinase inhibitor`
        // (44) -- the one role that identifies the compound -- sat tenth, behind a "+5 more".
        //
        // Specific-before-generic is the same advice we gave for reading roles at all, applied at the
        // only place a client cannot apply it for itself: inside a `?limit=`. It cannot reorder rows
        // the score separated, so an assertion still outranks an attestation and a well-supported row
        // still outranks a thin one.
        //
        // Object breadth, not subject: a subject-seeded query is the shape this fixes. Seeded from the
        // object side every row shares one object, breadth is constant, and the sort falls through to
        // the alphabetical tiebreakers exactly as before.
        result.sort( Comparator.comparingDouble( RelationSummary::getScore ).reversed()
                .thenComparingLong( RelationSummary::getObjectBreadth )
                .thenComparing( RelationSummary::getSubjectValue )
                .thenComparing( RelationSummary::getObjectValue ) );
        return q.getMaxResults() > 0 && result.size() > q.getMaxResults()
                ? new ArrayList<>( result.subList( 0, q.getMaxResults() ) )
                : result;
    }

    @Override
    public List<String[]> findRelatedTerms( Collection<String> seedValueUris, Collection<String> seedValues,
            Direction direction, Set<AnnotationRelationBasis> bases, Collection<String> predicateUris,
            @Nullable Long taxonId, Collection<Long> excludedExperimentIds, int maximumObjectBreadth,
            int maxResults ) {
        if ( seedValueUris.isEmpty() && seedValues.isEmpty() || bases.isEmpty() ) {
            return Collections.emptyList();
        }
        String seedSide = direction == Direction.SUBJECT_TO_OBJECT ? "SUBJECT" : "OBJECT";
        String returnSide = direction == Direction.SUBJECT_TO_OBJECT ? "OBJECT" : "SUBJECT";

        StringBuilder where = new StringBuilder( " where R.BASIS in (:bases) and R.STATUS = :assertedStatus" );
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
        query.setParameter( "assertedStatus", AnnotationRelationStatus.ASSERTED.name() );
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
        // 🛑 THE CAP GOES ON AFTER THE CUT, NEVER BEFORE IT. Capping in SQL and then dropping the rows
        // that fail the breadth bar answers "an arbitrary N, minus the ones that failed" when what was
        // asked for is "the best N that pass" -- so raising the bar THINNED the answer instead of
        // filling it with better terms, which is the opposite of what a threshold is for, and a gate
        // could miss a term that qualifies because an arbitrary cap had already excluded it. Arbitrary
        // is literal: this query carries no ORDER BY, so which N came back was whatever the plan
        // produced. (oganm found the same defect in the disease-model endpoint of #1685, where the
        // specificity cut ran after the row cap. Same shape, different filter.)
        boolean cutsBeforeCapping = maximumObjectBreadth > 0;
        if ( maxResults > 0 && !cutsBeforeCapping ) {
            query.setMaxResults( maxResults );
        }
        //noinspection unchecked
        List<Object[]> rows = ( List<Object[]> ) query.list();
        List<String[]> terms = rows.stream()
                .map( r -> new String[] { ( String ) r[0], ( String ) r[1] } )
                .collect( Collectors.toList() );
        if ( !cutsBeforeCapping || terms.isEmpty() ) {
            return terms;
        }
        // Filtered after the fetch rather than as a correlated subquery per row: two indexed queries
        // beat one that re-counts the whole relation set for every candidate. Unbounded here on
        // purpose -- the query is seeded from a specific term, so the candidate set is that term's own
        // relations, and it returns two columns.
        Map<String, Long> breadth = findObjectBreadth( terms.stream()
                .map( t -> t[0] ).filter( Objects::nonNull ).distinct().collect( Collectors.toList() ) );
        List<String[]> kept = terms.stream()
                .filter( t -> breadth.getOrDefault( breadthKey( t[0] ), 0L ) <= maximumObjectBreadth )
                // most specific first, so a cap that still bites takes the least identifying terms
                // rather than whichever ones the plan happened to emit last
                .sorted( Comparator.comparingLong( t -> breadth.getOrDefault( breadthKey( t[0] ), 0L ) ) )
                .collect( Collectors.toList() );
        return maxResults > 0 && kept.size() > maxResults
                ? new ArrayList<>( kept.subList( 0, maxResults ) )
                : kept;
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
     * How many distinct subjects each object relates to, corpus-wide.
     *
     * <p>Read from the relation table rather than EE2C, because the question is about the relations
     * themselves and not about annotation frequency: {@code 24 h} is a common annotation AND a broad
     * object, but {@code C57BL/6J} is a common annotation and a narrow object, and only the second
     * number tells a gate anything.</p>
     *
     * <p>🛑 The engines disagree here and only one of them is production. MySQL's collation is
     * case-insensitive, so the {@code in} clause matches case variants and the group-by would collapse
     * them under an arbitrary spelling; H2 in {@code MODE=MYSQL} is case-SENSITIVE and does neither.
     * Grouping on the normalized value makes the returned key deterministic on both, which is why the
     * key is computed in SQL rather than trusted from the raw column. <b>The collation half of this
     * cannot be covered by the H2 tests</b> — they would pass against a version that is wrong in
     * production.</p>
     *
     * <p>Deliberately NOT ACL-filtered. Breadth is a property of the relation set, not a count of
     * anyone's datasets, and filtering it per user would make the same relation look identifying to
     * one caller and generic to another.</p>
     */
    private Map<String, Long> findObjectBreadth( Collection<String> objectValues ) {
        return findBreadth( objectValues, "OBJECT_VALUE", "SUBJECT_VALUE", false );
    }

    /**
     * Distinct objects per subject <b>under one predicate</b> — {@link #findObjectBreadth} with the
     * ends swapped, which is exactly what uib asked for and the reason the query below is parameterized
     * on its columns rather than copied. Two near-identical native queries would be two places for the
     * case-collation trap to be fixed in, and it would get fixed in one.
     *
     * <p>🛑 <b>The predicate is part of the grain here and is not on the object side.</b> Swapping the
     * ends alone counted every object a subject relates to under any predicate, and that number does
     * not separate the shape it was added for. Measured on gemma2 2026-08-27, the unscoped count read
     * {@code dimethyl sulfoxide} 9, {@code BRCA1} 13, {@code biotin} 15 and {@code epithelial cell} 20
     * — the one row a reader wanted ({@code BRCA1 --has disease--> breast cancer}) sitting between two
     * ontology closures nobody wanted, with no bar able to separate them. Asking the same endpoint one
     * predicate at a time returned 8, 1, 15 and 3 objects for those four.</p>
     */
    private Map<String, Long> findSubjectBreadth( Collection<String> subjectValues ) {
        return findBreadth( subjectValues, "SUBJECT_VALUE", "OBJECT_VALUE", true );
    }

    /**
     * @param keyColumn    the end being asked about; the values passed in are its values
     * @param countColumn  the other end, counted distinctly. Both are literals from this class, never
     *                     caller input.
     * @param perPredicate count within each predicate separately, keying the result on
     *                     {@link #breadthKey(String, String)} rather than on the value alone
     */
    private Map<String, Long> findBreadth( Collection<String> values, String keyColumn, String countColumn,
            boolean perPredicate ) {
        if ( values.isEmpty() ) {
            return Collections.emptyMap();
        }
        // the predicate is identified by its URI where it has one and by its label where it does not,
        // the same rule RelationSummary#getTripleKey uses, and normalized for the same collation reason
        // as the value column beside it
        String predicateKey = "lower(trim(coalesce(R.PREDICATE_URI, R.PREDICATE)))";
        NativeQuery<?> query = getSessionFactory().getCurrentSession().createNativeQuery(
                        "select lower(trim(R." + keyColumn + ")) as V, "
                                + ( perPredicate ? predicateKey + " as P, " : "" )
                                + "count(distinct R." + countColumn + ") as N "
                                + "from ANNOTATION_RELATION R where R." + keyColumn + " in (:values) "
                                // grouped on the normalized value, not the raw one: MySQL would otherwise
                                // collapse case variants itself and hand back an arbitrary spelling,
                                // which the Java-side lookup then misses
                                + "group by lower(trim(R." + keyColumn + "))"
                                + ( perPredicate ? ", " + predicateKey : "" ) )
                .addScalar( "V", StandardBasicTypes.STRING );
        if ( perPredicate ) {
            query.addScalar( "P", StandardBasicTypes.STRING );
        }
        query.addScalar( "N", StandardBasicTypes.LONG )
                .addSynchronizedEntityClass( AnnotationRelation.class );
        query.setParameterList( "values", optimizeParameterList( values ) );
        query.setCacheable( true );
        //noinspection unchecked
        List<Object[]> rows = ( List<Object[]> ) query.list();
        // 🛑 Keyed case-insensitively, because MySQL's collation is. The group-by collapses case
        // variants into ONE group and picks a representative spelling arbitrarily, so a map keyed on
        // that spelling misses when the outer query's row spells it differently -- and the miss reads
        // as breadth 0, i.e. maximally specific. That fails OPEN: maxObjectBreadth would keep exactly
        // the rows whose value is dirty enough to have case variants. Seen live on
        // `familial Alzheimer's disease` and `intermediate`.
        Map<String, Long> out = new LinkedHashMap<>();
        for ( Object[] r : rows ) {
            out.merge( breadthKey( ( String ) r[0], perPredicate ? ( String ) r[1] : null ),
                    asLong( r[perPredicate ? 2 : 1] ), Long::max );
        }
        return out;
    }

    /**
     * Normalized lookup key for {@link #findObjectBreadth}, matching how the database compared the
     * values in the first place.
     */
    private static String breadthKey( @Nullable String value ) {
        return breadthKey( value, null );
    }

    /**
     * @param predicate the predicate URI, or its label where there is no URI; null for a count that
     *                  was not taken per predicate
     * @see #findSubjectBreadth(Collection)
     */
    private static String breadthKey( @Nullable String value, @Nullable String predicate ) {
        String v = value != null ? value.trim().toLowerCase( java.util.Locale.ROOT ) : "";
        if ( predicate == null ) {
            return v;
        }
        // NUL as the separator, so no pair of (value, predicate) can collide with another by running
        // together across it -- values and predicate labels both contain spaces
        return v + '\0' + predicate.trim().toLowerCase( java.util.Locale.ROOT );
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
        // 🛑 Two uncorrelated IN subqueries, NOT one correlated EXISTS. The EXISTS form reads better and
        // costs 65x: it correlates on R, so MySQL runs it once per row of ANNOTATION_RELATION, and since
        // nothing else in the WHERE is selective (BASIS matches every row) the plan is a full scan of all
        // ~322k rows with a dependent subquery on each -- measured 42.7s on prod for ee 1699 against 0.65s
        // for this form, which is uniform across datasets where the EXISTS form ranged 4.5s to 47.6s.
        // Written this way the seed set is materialized once and the scan becomes a lookup against it.
        //
        // The two branches are the EXISTS form's two disjuncts exactly: `S2.VALUE_URI = R.<side>_VALUE_URI`
        // can only hold when both are non-null, and the second disjunct is the both-null-URI case matched
        // on the label. Verified byte-identical output on ee 1699, 1035 and 3333.
        return " and (R." + side + "_VALUE_URI in (select distinct S2.VALUE_URI"
                + " from EXPRESSION_EXPERIMENT2CHARACTERISTIC S2"
                + " where S2.EXPRESSION_EXPERIMENT_FK = :seedEeId and S2.VALUE_URI is not null)"
                + " or (R." + side + "_VALUE_URI is null and R." + side + "_VALUE in (select distinct S2B.`VALUE`"
                + " from EXPRESSION_EXPERIMENT2CHARACTERISTIC S2B"
                + " where S2B.EXPRESSION_EXPERIMENT_FK = :seedEeId and S2B.VALUE_URI is null)))";
    }

    /**
     * Drop a relation whose CONCLUSION is a term the seed experiment already carries.
     *
     * <p>An experiment learns nothing from being told what it already asserts. Reported by Paul on
     * GSE315959, 2026-08-27: the dataset is annotated {@code organism part: prostate gland}, and the
     * card offered it 169 Cellosaurus prostate lines, every one of them concluding {@code prostate
     * gland} — its own annotation, restated 195 times.</p>
     *
     * <p>The conclusion sits on the side the seed did NOT match, always. That is not an assumption
     * about the data; it is what {@link RelationSummary#getInferenceDirection()} is checked for one
     * method over — a row whose implying end is not the seed side is dropped there, so among the rows
     * that survive, the seed is the premise and the other end is the conclusion. Which makes this the
     * mirror of {@link #seedFromExperimentConstraint}: same experiment, same URI-then-value matching,
     * other side, negated.</p>
     *
     * <p>In SQL rather than in the Java filter loop so it runs before the aggregation and before the
     * breadth and support subqueries, which on the reported case is 195 rows and 169 distinct subjects
     * that never need counting.</p>
     */
    private String alreadyCarriedConstraint( Direction direction ) {
        String side = direction == Direction.SUBJECT_TO_OBJECT ? "OBJECT" : "SUBJECT";
        return " and not exists (select 1 from EXPRESSION_EXPERIMENT2CHARACTERISTIC S3"
                + " where S3.EXPRESSION_EXPERIMENT_FK = :seedEeId"
                + " and (S3.VALUE_URI = R." + side + "_VALUE_URI"
                + " or (S3.VALUE_URI is null and R." + side + "_VALUE_URI is null and S3.`VALUE` = R." + side + "_VALUE)))";
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
