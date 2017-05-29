package ubic.gemma.persistence.service.common.auditAndSecurity;

import ubic.gemma.model.common.auditAndSecurity.JobInfo;

import java.util.Collection;

public interface JobInfoService {

    JobInfo create( JobInfo entity );

    Collection<JobInfo> getUsersJobs( String userName );

    Collection<? extends JobInfo> load( Collection<Long> ids );

    JobInfo load( Long id );

    void remove( Collection<JobInfo> entities );

    void remove( Long id );

    void update( JobInfo entity );

}