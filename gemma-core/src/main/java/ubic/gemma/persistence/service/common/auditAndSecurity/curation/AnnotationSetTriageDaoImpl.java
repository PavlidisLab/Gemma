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
package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetTriage;
import ubic.gemma.model.common.auditAndSecurity.curation.TriageVerdict;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hibernate implementation of {@link AnnotationSetTriageDao}.
 */
@Repository
public class AnnotationSetTriageDaoImpl extends AbstractDao<AnnotationSetTriage>
        implements AnnotationSetTriageDao {

    /**
     * Recency ordering, with id as the tiebreaker.
     * <p>
     * Two judgements can share a {@code judgedAt} — the column is millisecond
     * precision and an agent batch writes fast — and without the tiebreaker
     * "the effective verdict" would be whichever row the database happened to
     * return first, which is not stable across runs.
     */
    private static final String NEWEST_FIRST = " order by t.judgedAt desc, t.id desc";

    @Autowired
    public AnnotationSetTriageDaoImpl( SessionFactory sessionFactory ) {
        super( AnnotationSetTriage.class, sessionFactory );
    }

    @Nullable
    @Override
    public AnnotationSetTriage findBySetAndJudge( AnnotationSet annotationSet, String judgedBy ) {
        return ( AnnotationSetTriage ) getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetTriage t where t.annotationSet = :set and t.judgedBy = :judge" )
                .setParameter( "set", annotationSet )
                .setParameter( "judge", judgedBy )
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationSetTriage> findBySet( AnnotationSet annotationSet ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetTriage t where t.annotationSet = :set" + NEWEST_FIRST )
                .setParameter( "set", annotationSet )
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationSetTriage> findByInvestigation( Investigation investigation ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetTriage t where t.annotationSet.investigation = :inv" + NEWEST_FIRST )
                .setParameter( "inv", investigation )
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, AnnotationSetTriage> findEffectiveBySetIds( Collection<Long> annotationSetIds ) {
        if ( annotationSetIds == null || annotationSetIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // One query ordered newest-first, then keep the first row seen per set.
        // A correlated "where judgedAt = (select max(...))" subquery would need
        // the same id tiebreaker repeated inside it to stay deterministic, and
        // would still return two rows for a millisecond tie.
        List<AnnotationSetTriage> all = getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetTriage t where t.annotationSet.id in (:ids)" + NEWEST_FIRST )
                .setParameterList( "ids", annotationSetIds )
                .list();
        Map<Long, AnnotationSetTriage> out = new HashMap<>();
        for ( AnnotationSetTriage t : all ) {
            out.putIfAbsent( t.getAnnotationSet().getId(), t );
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, AnnotationSetTriage> findEffectiveByInvestigationIds( Collection<Long> investigationIds ) {
        if ( investigationIds == null || investigationIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // Same shape as findEffectiveBySetIds: one newest-first query, keep the first row seen
        // per investigation. A dataset can own several annotation sets, so the winner is the most
        // recent ruling across all of them — which is the rule effectiveFor already applies to a
        // single set, not a new one.
        List<AnnotationSetTriage> all = getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetTriage t where t.annotationSet.investigation.id in (:ids)" + NEWEST_FIRST )
                .setParameterList( "ids", investigationIds )
                .list();
        Map<Long, AnnotationSetTriage> out = new HashMap<>();
        for ( AnnotationSetTriage t : all ) {
            out.putIfAbsent( t.getAnnotationSet().getInvestigation().getId(), t );
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<TriageVerdict, Long> countByVerdict() {
        List<Object[]> rows = getSessionFactory().getCurrentSession()
                .createQuery( "select t.verdict, count(*) from AnnotationSetTriage t group by t.verdict" )
                .list();
        Map<TriageVerdict, Long> out = new EnumMap<>( TriageVerdict.class );
        for ( Object[] row : rows ) {
            out.put( ( TriageVerdict ) row[0], ( Long ) row[1] );
        }
        return out;
    }
}
