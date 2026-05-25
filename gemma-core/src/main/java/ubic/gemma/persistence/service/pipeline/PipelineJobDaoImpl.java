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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.pipeline.JobState;
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobBatch;
import ubic.gemma.model.pipeline.SchedulerKind;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;

@Repository
public class PipelineJobDaoImpl extends AbstractDao<PipelineJob> implements PipelineJobDao {

    private static final EnumSet<JobState> ACTIVE_STATES = EnumSet.of(
            JobState.PENDING, JobState.QUEUED, JobState.RUNNING, JobState.CANCELLING );

    @Autowired
    public PipelineJobDaoImpl( SessionFactory sessionFactory ) {
        super( PipelineJob.class, sessionFactory );
    }

    @Override
    public List<PipelineJob> findByBatch( Long batchId ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select j from PipelineJob j where j.batch.id = :bid order by j.id asc" )
                .setParameter( "bid", batchId )
                .list();
    }

    @Override
    public List<PipelineJob> findActiveByExperiment( Long experimentId ) {
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select j from PipelineJob j where j.experiment.id = :eid and j.state in :states" )
                .setParameter( "eid", experimentId )
                .setParameterList( "states", ACTIVE_STATES )
                .list();
    }

    @Nullable
    @Override
    public PipelineJob findBySchedulerHandle( SchedulerKind kind, String schedulerHandle ) {
        return ( PipelineJob ) this.getSessionFactory().getCurrentSession().createQuery(
                        "select j from PipelineJob j where j.schedulerKind = :k and j.schedulerHandle = :h" )
                .setParameter( "k", kind )
                .setParameter( "h", schedulerHandle )
                .uniqueResult();
    }

    @Override
    public List<PipelineJob> findStaleJobs( JobState state, Date staleSinceCutoff, int limit ) {
        // Composite (state, lastEventAt) index IDX_PIPELINE_JOB_RECONCILE backs this query.
        // Null lastEventAt also matches — jobs in `state` that never reported are by
        // definition stale and the reconciler should poll them.
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select j from PipelineJob j "
                                + "where j.state = :s "
                                + "and (j.lastEventAt is null or j.lastEventAt <= :cutoff) "
                                + "order by j.lastEventAt asc nulls first, j.id asc" )
                .setParameter( "s", state )
                .setParameter( "cutoff", staleSinceCutoff )
                .setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE )
                .list();
    }

    @Override
    public List<PipelineJob> findByBatchAndStates( PipelineJobBatch batch, Collection<JobState> states ) {
        if ( states.isEmpty() ) {
            return Collections.emptyList();
        }
        //noinspection unchecked
        return this.getSessionFactory().getCurrentSession().createQuery(
                        "select j from PipelineJob j where j.batch = :b and j.state in :states" )
                .setParameter( "b", batch )
                .setParameterList( "states", states )
                .list();
    }
}
