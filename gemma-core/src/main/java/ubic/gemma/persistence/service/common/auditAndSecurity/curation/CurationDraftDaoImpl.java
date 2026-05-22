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
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDraft;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Date;
import java.util.List;

/**
 * Hibernate implementation of {@link CurationDraftDao}.
 */
@Repository
public class CurationDraftDaoImpl extends AbstractDao<CurationDraft>
        implements CurationDraftDao {

    @Autowired
    public CurationDraftDaoImpl( SessionFactory sessionFactory ) {
        super( CurationDraft.class, sessionFactory );
    }

    @Nullable
    @Override
    public CurationDraft findByInvestigationAndCurator( Investigation investigation, Contact curator ) {
        return ( CurationDraft ) getSessionFactory().getCurrentSession()
                .createQuery( "from CurationDraft d "
                        + "where d.investigation = :inv and d.curator = :curator" )
                .setParameter( "inv", investigation )
                .setParameter( "curator", curator )
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CurationDraft> findByCurator( Contact curator, @Nullable Date since, int offset, int limit ) {
        String hql = "from CurationDraft d where d.curator = :curator"
                + ( since != null ? " and d.lastEditedAt >= :since" : "" )
                + " order by d.lastEditedAt desc, d.id desc";
        Query<CurationDraft> q = getSessionFactory().getCurrentSession()
                .createQuery( hql, CurationDraft.class )
                .setParameter( "curator", curator );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        if ( offset > 0 ) {
            q.setFirstResult( offset );
        }
        if ( limit > 0 ) {
            q.setMaxResults( limit );
        }
        return q.list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<CurationDraft> findByProposal( Long proposalId ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from CurationDraft d where d.proposal.id = :pid "
                        + "order by d.lastEditedAt desc, d.id desc" )
                .setParameter( "pid", proposalId )
                .list();
    }
}
