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
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDecision;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.List;

/**
 * Hibernate implementation of {@link CurationDecisionDao}.
 */
@Repository
public class CurationDecisionDaoImpl extends AbstractDao<CurationDecision>
        implements CurationDecisionDao {

    /**
     * Recency ordering with id as the tiebreaker, so a same-millisecond pair
     * resolves to one answer rather than to whichever row the database
     * happened to return first.
     */
    private static final String NEWEST_FIRST = " order by d.decidedAt desc, d.id desc";

    @Autowired
    public CurationDecisionDaoImpl( SessionFactory sessionFactory ) {
        super( CurationDecision.class, sessionFactory );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CurationDecision> findByInvestigation( Investigation investigation ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from CurationDecision d where d.investigation = :inv" + NEWEST_FIRST )
                .setParameter( "inv", investigation )
                .list();
    }

    @Override
    public List<CurationDecision> findStandingByInvestigation( Investigation investigation ) {
        // The fold, scope included, is defined on the entity so an in-memory read and a query
        // cannot disagree about what supersedes what.
        return CurationDecision.standing( findByInvestigation( investigation ) );
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CurationDecision> findByInvestigationAndKey( Investigation investigation, String decisionKey ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from CurationDecision d where d.investigation = :inv"
                        + " and d.decisionKey = :key" + NEWEST_FIRST )
                .setParameter( "inv", investigation )
                .setParameter( "key", decisionKey )
                .list();
    }
}
