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

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 * @see ubic.gemma.model.common.auditAndSecurity.JobInfo
 */
@Repository
public class JobInfoDaoImpl extends ubic.gemma.model.common.auditAndSecurity.JobInfoDaoBase {

    @Autowired
    public JobInfoDaoImpl( SessionFactory sessionFactory ) {
        super.setSessionFactory( sessionFactory );
    }

    public Collection<JobInfo> getUsersJob( String userName ) {
        return this.getHibernateTemplate().findByNamedParam(
                "select j from JobInfoImpl j inner join j.user on u.userName = :un", "un", "userName" );
    }

}