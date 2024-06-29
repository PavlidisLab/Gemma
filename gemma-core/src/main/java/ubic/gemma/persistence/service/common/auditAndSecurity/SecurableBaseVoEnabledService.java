package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import ubic.gemma.core.lang.Nullable;
import java.util.Collection;
import java.util.List;

public interface SecurableBaseVoEnabledService<C extends Securable, VO extends IdentifiableValueObject<C>> extends BaseVoEnabledService<C, VO>,
        SecurableBaseReadOnlyService<C> {

    @Override
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    VO loadValueObject( C entity );

    @Override
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    VO loadValueObjectById( Long entityId );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadValueObjects( Collection<C> entities );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadValueObjectsByIds( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadAllValueObjects();
}
