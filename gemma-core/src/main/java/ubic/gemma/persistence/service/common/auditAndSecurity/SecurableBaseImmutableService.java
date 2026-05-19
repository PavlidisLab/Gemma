package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PostAuthorize;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.persistence.service.BaseImmutableService;

import java.util.Collection;

public interface SecurableBaseImmutableService<C extends Securable> extends BaseImmutableService<C>,
        SecurableBaseReadOnlyService<C> {

    @Override
    @Secured({ "GROUP_USER" })
    @PostAuthorize("returnObject == null or hasPermission(returnObject, 'READ') or hasPermission(returnObject, 'ADMINISTRATION')")
    C findOrCreate( C entity );

    @Override
    @Secured({ "GROUP_USER" })
    Collection<C> create( Collection<C> entities );

    @Override
    @Secured({ "GROUP_USER" })
    C create( C entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void remove( C entity );

    @Override
    @Secured({ "GROUP_USER", "ACL_SECURABLE_COLLECTION_EDIT" })
    void remove( Collection<C> entities );
}
