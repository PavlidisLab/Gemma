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
import ubic.gemma.model.expression.experiment.AgentCurationKind;
import ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject;
import ubic.gemma.model.expression.experiment.AgentProposal;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    public AgentProposal findByInvestigationAndKindAndRunId( Investigation investigation,
            AgentCurationKind kind, String runId ) {
        return ( AgentProposal ) getSessionFactory().getCurrentSession()
                .createQuery( "from AgentProposal p where p.investigation = :inv "
                        + "and p.kind = :kind and p.runId = :runId" )
                .setParameter( "inv", investigation )
                .setParameter( "kind", kind )
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

    @Override
    @SuppressWarnings("unchecked")
    public List<AgentCurationSummaryValueObject> findSummariesByInvestigation( Investigation investigation,
            AgentCurationKind kindFilter ) {
        // Thin projection: omit payloadJson, emit cast(length(payloadJson) as long) as payloadSize.
        // The cast to long is necessary because HQL `length(...)` is typed as Integer and the
        // AgentCurationSummaryValueObject constructor takes Long; Hibernate's NEW dispatch is
        // strict about parameter types.
        return getSessionFactory().getCurrentSession()
                .createQuery( "select new ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject("
                        + " p.id, p.kind, p.runId, p.agentVersion, p.model, p.ranAt, p.investigation.id,"
                        + " cast(length(p.payloadJson) as long) )"
                        + " from AgentProposal p"
                        + " where p.investigation = :inv"
                        + " and ( :kind is null or p.kind = :kind )"
                        + " order by p.ranAt desc, p.id desc" )
                .setParameter( "inv", investigation )
                .setParameter( "kind", kindFilter )
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

    @Override
    @SuppressWarnings("unchecked")
    public List<AgentCurationSummaryValueObject> listSummaries( @Nullable AgentCurationKind kindFilter,
            @Nullable List<Long> investigationIds, int offset, int limit ) {
        boolean restrictByInv = investigationIds != null && !investigationIds.isEmpty();
        StringBuilder hql = new StringBuilder()
                .append( "select new ubic.gemma.model.expression.experiment.AgentCurationSummaryValueObject(" )
                .append( " p.id, p.kind, p.runId, p.agentVersion, p.model, p.ranAt, p.investigation.id," )
                .append( " cast(length(p.payloadJson) as long) )" )
                .append( " from AgentProposal p" )
                .append( " where ( :kind is null or p.kind = :kind )" );
        if ( restrictByInv ) {
            hql.append( " and p.investigation.id in (:invIds)" );
        }
        hql.append( " order by p.ranAt desc, p.id desc" );
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() )
                .setParameter( "kind", kindFilter )
                .setFirstResult( Math.max( 0, offset ) )
                .setMaxResults( Math.max( 1, limit ) );
        if ( restrictByInv ) {
            q.setParameterList( "invIds", investigationIds );
        }
        return ( List<AgentCurationSummaryValueObject> ) q.list();
    }

    @Override
    public long countSummaries( @Nullable AgentCurationKind kindFilter,
            @Nullable List<Long> investigationIds ) {
        boolean restrictByInv = investigationIds != null && !investigationIds.isEmpty();
        StringBuilder hql = new StringBuilder( "select count(p) from AgentProposal p" )
                .append( " where ( :kind is null or p.kind = :kind )" );
        if ( restrictByInv ) {
            hql.append( " and p.investigation.id in (:invIds)" );
        }
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() )
                .setParameter( "kind", kindFilter );
        if ( restrictByInv ) {
            q.setParameterList( "invIds", investigationIds );
        }
        Long n = ( Long ) q.uniqueResult();
        return n == null ? 0L : n;
    }

    @Override
    public long countSince( @Nullable Date since ) {
        StringBuilder hql = new StringBuilder( "select count(p) from AgentProposal p" );
        if ( since != null ) {
            hql.append( " where p.ranAt >= :since" );
        }
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        Long n = ( Long ) q.uniqueResult();
        return n == null ? 0L : n;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Long> countByStatusSince( @Nullable Date since ) {
        StringBuilder hql = new StringBuilder(
                "select p.status, count(p) from AgentProposal p" );
        if ( since != null ) {
            hql.append( " where p.ranAt >= :since" );
        }
        hql.append( " group by p.status" );
        org.hibernate.query.Query<Object[]> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString(), Object[].class );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        List<Object[]> rows = q.list();
        Map<String, Long> out = new LinkedHashMap<>( rows.size() );
        for ( Object[] row : rows ) {
            String status = row[0] != null ? row[0].toString() : "null";
            Long count = ( Long ) row[1];
            out.put( status, count == null ? 0L : count );
        }
        return out;
    }

    @Override
    public long countDistinctRunIdsSince( @Nullable Date since ) {
        StringBuilder hql = new StringBuilder(
                "select count(distinct p.runId) from AgentProposal p" );
        if ( since != null ) {
            hql.append( " where p.ranAt >= :since" );
        }
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        Long n = ( Long ) q.uniqueResult();
        return n == null ? 0L : n;
    }

    @Nullable
    @Override
    public Date findLatestRanAt() {
        return ( Date ) getSessionFactory().getCurrentSession()
                .createQuery( "select max(p.ranAt) from AgentProposal p" )
                .uniqueResult();
    }
}
