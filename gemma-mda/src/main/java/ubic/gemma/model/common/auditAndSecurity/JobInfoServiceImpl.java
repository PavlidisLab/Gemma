/*
 * The Gemma project
 * 
 * Copyright (c) 2010 University of British Columbia
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author paul
 * @version $Id$
 */
@Service
public class JobInfoServiceImpl implements JobInfoService {

    @Autowired
    private JobInfoDao jobInfoDao;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.JobInfoServiceff#create(ubic.gemma.model.common.auditAndSecurity.JobInfo
     * )
     */
    @Override
    @Transactional
    public JobInfo create( JobInfo entity ) {
        return jobInfoDao.create( entity );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.JobInfoServiceff#getJobInfoDao()
     */
    public JobInfoDao getJobInfoDao() {
        return jobInfoDao;
    }

    @Override
    public Collection<JobInfo> getUsersJobs( String userName ) {
        return jobInfoDao.getUsersJob( userName );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.JobInfoServiceff#load(java.util.Collection)
     */
    @Override
    @Transactional(readOnly = true)
    public Collection<? extends JobInfo> load( Collection<Long> ids ) {
        return jobInfoDao.load( ids );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.JobInfoServiceff#load(java.lang.Long)
     */
    @Override
    @Transactional(readOnly = true)
    public JobInfo load( Long id ) {
        return jobInfoDao.load( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.JobInfoServiceff#remove(java.util.Collection)
     */
    @Override
    @Transactional
    public void remove( Collection<? extends JobInfo> entities ) {
        jobInfoDao.remove( entities );
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.model.common.auditAndSecurity.JobInfoServiceff#remove(java.lang.Long)
     */
    @Override
    @Transactional
    public void remove( Long id ) {
        jobInfoDao.remove( id );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.model.common.auditAndSecurity.JobInfoServiceff#update(ubic.gemma.model.common.auditAndSecurity.JobInfo
     * )
     */
    @Override
    @Transactional
    public void update( JobInfo entity ) {
        jobInfoDao.update( entity );
    }

}
