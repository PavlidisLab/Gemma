package ubic.gemma.persistence.service.common.auditAndSecurity;

import ubic.gemma.persistence.service.BaseDao;
import ubic.gemma.model.common.auditAndSecurity.JobInfo;

import java.util.Collection;

public interface JobInfoService {

    /**
     * @param entity
     * @return
     * @see BaseDao#create(java.lang.Object)
     */
    public abstract JobInfo create( JobInfo entity );

    public abstract Collection<JobInfo> getUsersJobs( String userName );

    /**
     * @param ids
     * @return
     * @see BaseDao#load(java.util.Collection)
     */
    public abstract Collection<? extends JobInfo> load( Collection<Long> ids );

    /**
     * @param id
     * @return
     * @see BaseDao#load(java.lang.Long)
     */
    public abstract JobInfo load( Long id );

    /**
     * @param entities
     * @see BaseDao#remove(java.util.Collection)
     */
    public abstract void remove( Collection<? extends JobInfo> entities );

    /**
     * @param id
     * @see BaseDao#remove(java.lang.Long)
     */
    public abstract void remove( Long id );

    /**
     * @param entity
     * @see BaseDao#update(java.lang.Object)
     */
    public abstract void update( JobInfo entity );

}