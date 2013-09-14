/*
 * The Gemma project.
 * 
 * Copyright (c) 2006-2007 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

/**
 * Base Spring DAO Class: is able to create, update, remove, load, and find objects of type <code>JobInfo</code>.
 * 
 * @see JobInfo
 * @version $Id$
 */
public abstract class JobInfoDaoBase extends org.springframework.orm.hibernate3.support.HibernateDaoSupport implements
        JobInfoDao {

    /**
     * @see JobInfoDao#create(int, java.util.Collection)
     */
    @Override
    public java.util.Collection<? extends JobInfo> create( final java.util.Collection<? extends JobInfo> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "JobInfo.create - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends JobInfo> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            create( entityIterator.next() );
                        }
                        return null;
                    }
                } );
        return entities;
    }

    /**
     * @see JobInfoDao#create(int transform, JobInfo)
     */
    @Override
    public JobInfo create( final JobInfo jobInfo ) {
        if ( jobInfo == null ) {
            throw new IllegalArgumentException( "JobInfo.create - 'jobInfo' can not be null" );
        }
        this.getHibernateTemplate().save( jobInfo );
        return jobInfo;
    }

    @Override
    public Collection<JobInfo> load( Collection<Long> ids ) {
        return this.getHibernateTemplate().findByNamedParam( "from JobInfoImpl where id in (:ids)", "ids", ids );
    }

    /**
     * @see JobInfoDao#load(int, java.lang.Long)
     */
    @Override
    public JobInfo load( final java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "JobInfo.load - 'id' can not be null" );
        }
        final Object entity = this.getHibernateTemplate().get( JobInfoImpl.class, id );
        return ( JobInfo ) entity;
    }

    /**
     * @see JobInfoDao#loadAll(int)
     */
    @Override
    public java.util.Collection<? extends JobInfo> loadAll() {
        return this.getHibernateTemplate().loadAll( JobInfoImpl.class );
    }

    /**
     * @see JobInfoDao#remove(java.lang.Long)
     */
    @Override
    public void remove( java.lang.Long id ) {
        if ( id == null ) {
            throw new IllegalArgumentException( "JobInfo.remove - 'id' can not be null" );
        }
        JobInfo entity = this.load( id );
        if ( entity != null ) {
            this.remove( entity );
        }
    }

    /**
     * @see JobInfoDao#remove(java.util.Collection)
     */
    @Override
    public void remove( java.util.Collection<? extends JobInfo> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "JobInfo.remove - 'entities' can not be null" );
        }
        this.getHibernateTemplate().deleteAll( entities );
    }

    /**
     * @see JobInfoDao#remove(JobInfo)
     */
    @Override
    public void remove( JobInfo jobInfo ) {
        if ( jobInfo == null ) {
            throw new IllegalArgumentException( "JobInfo.remove - 'jobInfo' can not be null" );
        }
        this.getHibernateTemplate().delete( jobInfo );
    }

    /**
     * @see JobInfoDao#update(java.util.Collection)
     */
    @Override
    public void update( final java.util.Collection<? extends JobInfo> entities ) {
        if ( entities == null ) {
            throw new IllegalArgumentException( "JobInfo.update - 'entities' can not be null" );
        }
        this.getHibernateTemplate().executeWithNativeSession(
                new org.springframework.orm.hibernate3.HibernateCallback<Object>() {
                    @Override
                    public Object doInHibernate( org.hibernate.Session session )
                            throws org.hibernate.HibernateException {
                        for ( java.util.Iterator<? extends JobInfo> entityIterator = entities.iterator(); entityIterator
                                .hasNext(); ) {
                            update( entityIterator.next() );
                        }
                        return null;
                    }
                } );
    }

    /**
     * @see JobInfoDao#update(JobInfo)
     */
    @Override
    public void update( JobInfo jobInfo ) {
        if ( jobInfo == null ) {
            throw new IllegalArgumentException( "JobInfo.update - 'jobInfo' can not be null" );
        }
        this.getHibernateTemplate().update( jobInfo );
    }

}