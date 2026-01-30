package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseService;

import java.util.Collection;

/**
 * Interface for services of entities that can only be edited by admins.
 *
 * @param <O>
 */
public interface AdminEditableBaseService<O extends Identifiable> extends BaseService<O> {

    @Override
    @Secured({ "GROUP_ADMIN" })
    O findOrCreate( O entity );

    @Override
    @Secured({ "GROUP_ADMIN" })
    O create( O gene );

    @Override
    @Secured({ "GROUP_ADMIN" })
    Collection<O> create( Collection<O> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void update( O entity );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void update( Collection<O> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    O save( O entity );

    @Override
    @Secured({ "GROUP_ADMIN" })
    Collection<O> save( Collection<O> entities );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( O gene );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<O> entities );
}
