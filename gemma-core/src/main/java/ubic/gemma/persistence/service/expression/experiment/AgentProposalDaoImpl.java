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
package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.List;

/**
 * Hibernate implementation of {@link AgentProposalDao}.
 */
@Repository
public class AgentProposalDaoImpl extends AbstractDao<AgentProposal>
        implements AgentProposalDao {

    @Autowired
    public AgentProposalDaoImpl( SessionFactory sessionFactory ) {
        super( AgentProposal.class, sessionFactory );
    }

    @Nullable
    @Override
    public AgentProposal findByInvestigationAndRunId( Investigation investigation, String runId ) {
        return ( AgentProposal ) getSessionFactory().getCurrentSession()
                .createQuery( "from AgentProposal p where p.investigation = :inv and p.runId = :runId" )
                .setParameter( "inv", investigation )
                .setParameter( "runId", runId )
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AgentProposal> findByInvestigation( Investigation investigation ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from AgentProposal p where p.investigation = :inv "
                        + "order by p.ranAt desc, p.id desc" )
                .setParameter( "inv", investigation )
                .list();
    }

    @Nullable
    @Override
    public AgentProposal findLatestByInvestigation( Investigation investigation ) {
        @SuppressWarnings("unchecked")
        List<AgentProposal> rows = getSessionFactory().getCurrentSession()
                .createQuery( "from AgentProposal p where p.investigation = :inv "
                        + "order by p.ranAt desc, p.id desc" )
                .setParameter( "inv", investigation )
                .setMaxResults( 1 )
                .list();
        return rows.isEmpty() ? null : rows.get( 0 );
    }

    @Override
    public long countByInvestigation( Investigation investigation ) {
        Long n = ( Long ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(p) from AgentProposal p where p.investigation = :inv" )
                .setParameter( "inv", investigation )
                .uniqueResult();
        return n == null ? 0L : n;
    }

    @Override
    public int rebindInvestigation( Investigation from, Investigation to ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "update AgentProposal p set p.investigation = :to where p.investigation = :from" )
                .setParameter( "from", from )
                .setParameter( "to", to )
                .executeUpdate();
    }
}
