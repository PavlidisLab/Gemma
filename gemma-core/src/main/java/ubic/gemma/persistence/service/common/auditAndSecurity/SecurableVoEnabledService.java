package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.BaseVoEnabledService;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;

/**
 * A {@link BaseVoEnabledService} for {@link gemma.gsec.model.Securable} entities with sensible defaults.
 * @see BaseVoEnabledService
 */
public interface SecurableVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends SecurableService<O>, BaseVoEnabledService<O, VO> {

    @Override
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    VO loadValueObject( O entity );

    @Override
    @Nullable
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_READ" })
    VO loadValueObjectById( Long entityId );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadValueObjects( Collection<O> entities );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadValueObjectsByIds( Collection<Long> ids );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_VALUE_OBJECT_COLLECTION_READ" })
    List<VO> loadAllValueObjects();
}
