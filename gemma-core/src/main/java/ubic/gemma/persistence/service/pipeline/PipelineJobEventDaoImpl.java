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
import ubic.gemma.model.pipeline.PipelineJob;
import ubic.gemma.model.pipeline.PipelineJobEvent;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Date;
import java.util.List;

@Repository
public class PipelineJobEventDaoImpl extends AbstractDao<PipelineJobEvent> implements PipelineJobEventDao {

    @Autowired
    public PipelineJobEventDaoImpl( SessionFactory sessionFactory ) {
        super( PipelineJobEvent.class, sessionFactory );
    }

    @Override
    public List<PipelineJobEvent> findByJob( PipelineJob job, @Nullable Date since, int limit ) {
        String hql = "select e from PipelineJobEvent e where e.job = :j"
                + ( since != null ? " and e.occurredAt > :since" : "" )
                + " order by e.occurredAt asc, e.id asc";
        //noinspection unchecked
        Query<PipelineJobEvent> q = this.getSessionFactory().getCurrentSession().createQuery( hql );
        q.setParameter( "j", job );
        if ( since != null ) {
            q.setParameter( "since", since );
        }
        return q.setMaxResults( limit > 0 ? limit : Integer.MAX_VALUE ).list();
    }
}
