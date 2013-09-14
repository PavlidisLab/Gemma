package ubic.gemma.model.common.auditAndSecurity;

import java.util.Collection;

public interface JobInfoService {

    /**
     * @param entity
     * @return
     * @see ubic.gemma.persistence.BaseDao#create(java.lang.Object)
     */
    public abstract JobInfo create( JobInfo entity );

    public abstract Collection<JobInfo> getUsersJobs( String userName );

    /**
     * @param ids
     * @return
     * @see ubic.gemma.persistence.BaseDao#load(java.util.Collection)
     */
    public abstract Collection<? extends JobInfo> load( Collection<Long> ids );

    /**
     * @param id
     * @return
     * @see ubic.gemma.persistence.BaseDao#load(java.lang.Long)
     */
    public abstract JobInfo load( Long id );

    /**
     * @param entities
     * @see ubic.gemma.persistence.BaseDao#remove(java.util.Collection)
     */
    public abstract void remove( Collection<? extends JobInfo> entities );

    /**
     * @param id
     * @see ubic.gemma.persistence.BaseDao#remove(java.lang.Long)
     */
    public abstract void remove( Long id );

    /**
     * @param entity
     * @see ubic.gemma.persistence.BaseDao#update(java.lang.Object)
     */
    public abstract void update( JobInfo entity );

}