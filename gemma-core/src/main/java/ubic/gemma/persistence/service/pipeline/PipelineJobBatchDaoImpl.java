/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.persistence.service.pipeline;

import org.hibernate.SessionFactory;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.List;

@Repository
public class PipelineJobBatchDaoImpl extends AbstractDao<PipelineJobBatch> implements PipelineJobBatchDao {

    @Autowired
    public PipelineJobBatchDaoImpl( SessionFactory sessionFactory ) {
        super( PipelineJobBatch.class, sessionFactory );
    }

    @Override
    public List<PipelineJobBatch> findByOwner( Long contactId, @Nullable PipelineJobBatch.BatchState state, int limit ) {
        String hql = "select b from PipelineJobBatch b where b.submittedBy.id = :cid"
                + ( state != null ? " and b.state = :st" : "" )
                + " order by b.submittedAt desc";
        //noinspection unchecked
        Query<PipelineJobBatch> q = this.getSessionFactory().getCurrentSession().createQuery( hql );
        q.setParameter( "cid", contactId );
        if ( state != null ) {
            q.setParameter( "st", state );
        }
        // HB6 rejects setMaxResults(<0); treat <=0 as no limit.
        return q.setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE ).list();
    }

    @Override
    public List<PipelineJobBatch> findDispatchable() {
        String hql = "select distinct b from PipelineJobBatch b join b.jobs j"
                + " where b.state = :open and b.held = false"
                + " and j.state = :pending and j.supersededBy is null";
        //noinspection unchecked
        Query<PipelineJobBatch> q = this.getSessionFactory().getCurrentSession().createQuery( hql );
        q.setParameter( "open", PipelineJobBatch.BatchState.OPEN );
        q.setParameter( "pending", ubic.gemma.model.pipeline.JobState.PENDING );
        return q.list();
    }
}
