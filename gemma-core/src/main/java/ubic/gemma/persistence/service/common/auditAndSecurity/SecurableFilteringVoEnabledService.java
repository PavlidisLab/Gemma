package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A {@link FilteringVoEnabledService} for {@link gemma.gsec.model.Securable} entities with sensible defaults.
 * @see FilteringVoEnabledService
 * @author poirigui
 */
public interface SecurableFilteringVoEnabledService<O extends Identifiable, VO extends IdentifiableValueObject<O>> extends SecurableVoEnabledService<O, VO>, FilteringVoEnabledService<O, VO> {

    // The methods below using Filters do not require AFTER_ACL_COLLECTION_READ because the ACL filtering is performed
    // in the query itself.

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<O> load( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Slice<O> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

}
