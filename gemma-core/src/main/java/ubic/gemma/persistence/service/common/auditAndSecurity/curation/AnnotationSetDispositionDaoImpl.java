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
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetDisposition;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hibernate implementation of {@link AnnotationSetDispositionDao}.
 */
@Repository
public class AnnotationSetDispositionDaoImpl extends AbstractDao<AnnotationSetDisposition>
        implements AnnotationSetDispositionDao {

    /**
     * Recency ordering, with id as the tiebreaker.
     * <p>
     * Two rulings can share a {@code decidedAt} — the column is millisecond
     * precision and a curator working a list can rule twice inside one — and
     * without the tiebreaker "the standing ruling" would be whichever row the
     * database happened to return first, which is not stable across runs. The
     * same ordering is spelled out in memory as
     * {@link AnnotationSetDisposition#NEWEST_FIRST} so an in-memory fold
     * cannot disagree with a query.
     */
    private static final String NEWEST_FIRST = " order by d.decidedAt desc, d.id desc";

    @Autowired
    public AnnotationSetDispositionDaoImpl( SessionFactory sessionFactory ) {
        super( AnnotationSetDisposition.class, sessionFactory );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationSetDisposition> findBySet( AnnotationSet annotationSet ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetDisposition d where d.annotationSet = :set" + NEWEST_FIRST )
                .setParameter( "set", annotationSet )
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationSetDisposition> findBySetAndTarget( AnnotationSet annotationSet, String targetId ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetDisposition d where d.annotationSet = :set"
                        + " and d.targetId = :target" + NEWEST_FIRST )
                .setParameter( "set", annotationSet )
                .setParameter( "target", targetId )
                .list();
    }

    @Override
    public List<AnnotationSetDisposition> findStandingBySet( AnnotationSet annotationSet ) {
        // findBySet is already newest-first, so the fold keeps the first row seen under each
        // standing key. Delegating to the entity keeps one definition of what supersedes what.
        return AnnotationSetDisposition.standing( findBySet( annotationSet ) );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<Long, List<AnnotationSetDisposition>> findStandingBySetIds(
            Collection<Long> annotationSetIds ) {
        if ( annotationSetIds == null || annotationSetIds.isEmpty() ) {
            return Collections.emptyMap();
        }
        // One newest-first query over every set in the page, then keep the first row seen per
        // (set, target). A correlated "where decidedAt = (select max(...))" subquery would need
        // the id tiebreaker repeated inside it to stay deterministic, and would still return two
        // rows for a millisecond tie.
        List<AnnotationSetDisposition> all = getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSetDisposition d where d.annotationSet.id in (:ids)"
                        + NEWEST_FIRST )
                .setParameterList( "ids", annotationSetIds )
                .list();
        Map<Long, List<AnnotationSetDisposition>> perSet = new HashMap<>();
        for ( AnnotationSetDisposition d : all ) {
            perSet.computeIfAbsent( d.getAnnotationSet().getId(), k -> new ArrayList<>() ).add( d );
        }
        Map<Long, List<AnnotationSetDisposition>> out = new HashMap<>();
        for ( Map.Entry<Long, List<AnnotationSetDisposition>> e : perSet.entrySet() ) {
            out.put( e.getKey(), AnnotationSetDisposition.standing( e.getValue() ) );
        }
        return out;
    }

    @Override
    public int deleteBySet( AnnotationSet annotationSet ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "delete from AnnotationSetDisposition d where d.annotationSet = :set" )
                .setParameter( "set", annotationSet )
                .executeUpdate();
    }
}
