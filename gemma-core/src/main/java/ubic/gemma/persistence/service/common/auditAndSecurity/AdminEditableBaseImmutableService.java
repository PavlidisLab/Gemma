package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseImmutableService;

import java.util.Collection;

/**
 * Interface for services of immutable entities that can only be edited by admins.
 *
 * @param <O>
 */
public interface AdminEditableBaseImmutableService<O extends Identifiable> extends BaseImmutableService<O> {

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
    void remove( O gene );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Long id );

    @Override
    @Secured({ "GROUP_ADMIN" })
    void remove( Collection<O> entities );
}
