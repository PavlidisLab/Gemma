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

import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSet;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetRole;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSource;
import ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSummaryValueObject;
import ubic.gemma.persistence.service.AbstractDao;

/**
 * Hibernate implementation of {@link AnnotationSetDao}.
 */
@Repository
public class AnnotationSetDaoImpl extends AbstractDao<AnnotationSet>
        implements AnnotationSetDao {

    private static final String SUMMARY_PROJECTION =
            "select new ubic.gemma.model.common.auditAndSecurity.curation.AnnotationSetSummaryValueObject("
                    + " a.id, a.role, a.source, a.kind, a.runId, a.createdBy,"
                    + " a.createdAt, a.updatedAt, a.finalizedAt, a.finalizedBy,"
                    + " a.agentVersion, a.model, a.runSha, a.agentName, a.ranAt,"
                    + " a.investigation.id, a.parent.id,"
                    + " cast(length(a.payloadJson) as long) )";

    @Autowired
    public AnnotationSetDaoImpl( SessionFactory sessionFactory ) {
        super( AnnotationSet.class, sessionFactory );
    }

    @Nullable
    @Override
    public AnnotationSet findByInvestigationAndRoleAndRunId( Investigation investigation,
            AnnotationSetRole role, String runId ) {
        return ( AnnotationSet ) getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSet a where a.investigation = :inv "
                        + "and a.role = :role and a.runId = :runId" )
                .setParameter( "inv", investigation )
                .setParameter( "role", role )
                .setParameter( "runId", runId )
                .uniqueResult();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationSet> findByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSet a where a.investigation = :inv "
                        + "and ( :role is null or a.role = :role ) "
                        + "order by a.createdAt desc, a.id desc" )
                .setParameter( "inv", investigation )
                .setParameter( "role", roleFilter )
                .list();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationSetSummaryValueObject> findSummariesByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( SUMMARY_PROJECTION
                        + " from AnnotationSet a"
                        + " where a.investigation = :inv"
                        + " and ( :role is null or a.role = :role )"
                        + " order by a.createdAt desc, a.id desc" )
                .setParameter( "inv", investigation )
                .setParameter( "role", roleFilter )
                .list();
    }

    @Nullable
    @Override
    public AnnotationSet findLatestByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        @SuppressWarnings("unchecked")
        List<AnnotationSet> rows = getSessionFactory().getCurrentSession()
                .createQuery( "from AnnotationSet a where a.investigation = :inv "
                        + "and ( :role is null or a.role = :role ) "
                        + "order by a.createdAt desc, a.id desc" )
                .setParameter( "inv", investigation )
                .setParameter( "role", roleFilter )
                .setMaxResults( 1 )
                .list();
        return rows.isEmpty() ? null : rows.get( 0 );
    }

    @Override
    public long countByInvestigation( Investigation investigation,
            @Nullable AnnotationSetRole roleFilter ) {
        Long n = ( Long ) getSessionFactory().getCurrentSession()
                .createQuery( "select count(a) from AnnotationSet a where a.investigation = :inv "
                        + "and ( :role is null or a.role = :role )" )
                .setParameter( "inv", investigation )
                .setParameter( "role", roleFilter )
                .uniqueResult();
        return n == null ? 0L : n;
    }

    @Override
    public int rebindInvestigation( Investigation from, Investigation to ) {
        return getSessionFactory().getCurrentSession()
                .createQuery( "update AnnotationSet a set a.investigation = :to where a.investigation = :from" )
                .setParameter( "from", from )
                .setParameter( "to", to )
                .executeUpdate();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AnnotationSetSummaryValueObject> listSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable List<Long> investigationIds, int offset, int limit,
            @Nullable SummarySort sort, boolean descending ) {
        boolean restrictByInv = investigationIds != null && !investigationIds.isEmpty();
        StringBuilder hql = new StringBuilder( SUMMARY_PROJECTION )
                .append( " from AnnotationSet a" )
                .append( " where ( :role is null or a.role = :role )" )
                .append( " and ( :source is null or a.source = :source )" )
                .append( " and ( :createdBy is null or a.createdBy = :createdBy )" );
        if ( restrictByInv ) {
            hql.append( " and a.investigation.id in (:invIds)" );
        }
        // the field comes from an enum, never from the caller's string, so it cannot reach HQL unchecked
        String orderField;
        switch ( sort != null ? sort : SummarySort.CREATED_AT ) {
            case RAN_AT:
                orderField = "a.ranAt";
                break;
            case ID:
                orderField = "a.id";
                break;
            default:
                orderField = "a.createdAt";
        }
        String direction = descending ? " desc" : " asc";
        hql.append( " order by " ).append( orderField ).append( direction )
                .append( ", a.id" ).append( direction );
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() )
                .setParameter( "role", roleFilter )
                .setParameter( "source", sourceFilter )
                .setParameter( "createdBy", createdByFilter )
                .setFirstResult( Math.max( 0, offset ) )
                .setMaxResults( Math.max( 1, limit ) );
        if ( restrictByInv ) {
            q.setParameterList( "invIds", investigationIds );
        }
        return ( List<AnnotationSetSummaryValueObject> ) q.list();
    }

    @Override
    public long countSummaries( @Nullable AnnotationSetRole roleFilter,
            @Nullable AnnotationSetSource sourceFilter,
            @Nullable String createdByFilter,
            @Nullable List<Long> investigationIds ) {
        boolean restrictByInv = investigationIds != null && !investigationIds.isEmpty();
        StringBuilder hql = new StringBuilder( "select count(a) from AnnotationSet a" )
                .append( " where ( :role is null or a.role = :role )" )
                .append( " and ( :source is null or a.source = :source )" )
                .append( " and ( :createdBy is null or a.createdBy = :createdBy )" );
        if ( restrictByInv ) {
            hql.append( " and a.investigation.id in (:invIds)" );
        }
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() )
                .setParameter( "role", roleFilter )
                .setParameter( "source", sourceFilter )
                .setParameter( "createdBy", createdByFilter );
        if ( restrictByInv ) {
            q.setParameterList( "invIds", investigationIds );
        }
        Long n = ( Long ) q.uniqueResult();
        return n == null ? 0L : n;
    }

    @Override
    public long countSince( @Nullable Date since, @Nullable AnnotationSetRole roleFilter ) {
        StringBuilder hql = new StringBuilder( "select count(a) from AnnotationSet a where 1=1" );
        if ( since != null ) {
            hql.append( " and a.createdAt >= :since" );
        }
        if ( roleFilter != null ) {
            hql.append( " and a.role = :role" );
        }
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        if ( roleFilter != null ) {
            q.setParameter( "role", roleFilter );
        }
        Long n = ( Long ) q.uniqueResult();
        return n == null ? 0L : n;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<AnnotationSetRole, Long> countByRoleSince( @Nullable Date since ) {
        StringBuilder hql = new StringBuilder( "select a.role, count(a) from AnnotationSet a" );
        if ( since != null ) {
            hql.append( " where a.createdAt >= :since" );
        }
        hql.append( " group by a.role" );
        org.hibernate.query.Query<Object[]> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString(), Object[].class );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        List<Object[]> rows = q.list();
        Map<AnnotationSetRole, Long> out = new EnumMap<>( AnnotationSetRole.class );
        for ( Object[] row : rows ) {
            AnnotationSetRole r = ( AnnotationSetRole ) row[0];
            Long count = ( Long ) row[1];
            out.put( r, count == null ? 0L : count );
        }
        return out;
    }

    @Override
    public long countDistinctRunIdsSince( @Nullable Date since,
            @Nullable AnnotationSetRole roleFilter ) {
        StringBuilder hql = new StringBuilder( "select count(distinct a.runId) from AnnotationSet a where 1=1" );
        if ( since != null ) {
            hql.append( " and a.createdAt >= :since" );
        }
        if ( roleFilter != null ) {
            hql.append( " and a.role = :role" );
        }
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        if ( roleFilter != null ) {
            q.setParameter( "role", roleFilter );
        }
        Long n = ( Long ) q.uniqueResult();
        return n == null ? 0L : n;
    }

    @Nullable
    @Override
    public Date findLatestCreatedAt( @Nullable AnnotationSetRole roleFilter ) {
        StringBuilder hql = new StringBuilder( "select max(a.createdAt) from AnnotationSet a" );
        if ( roleFilter != null ) {
            hql.append( " where a.role = :role" );
        }
        org.hibernate.query.Query<?> q = getSessionFactory().getCurrentSession()
                .createQuery( hql.toString() );
        if ( roleFilter != null ) {
            q.setParameter( "role", roleFilter );
        }
        return ( Date ) q.uniqueResult();
    }
}
