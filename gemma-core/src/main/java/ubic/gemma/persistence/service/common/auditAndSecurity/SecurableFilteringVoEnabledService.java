package ubic.gemma.persistence.service.common.auditAndSecurity;

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.IdentifiableValueObject;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.persistence.service.FilteringVoEnabledService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import javax.annotation.Nullable;
import java.util.List;

/**
 * A base service for securable entities with filtering and VO capabilities.
 *
 * @author poirigui
 * @see FilteringVoEnabledService
 */
public interface SecurableFilteringVoEnabledService<C extends Securable, VO extends IdentifiableValueObject<C>> extends
        FilteringVoEnabledService<C, VO>,
        SecurableBaseVoEnabledService<C, VO> {

    // The methods below using Filters do not require AFTER_ACL_COLLECTION_READ because the ACL filtering is performed
    // in the query itself.

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<Long> loadIds( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<C> load( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Slice<C> load( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    List<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort );

    @Override
    @Secured({ "IS_AUTHENTICATED_ANONYMOUSLY" })
    Slice<VO> loadValueObjects( @Nullable Filters filters, @Nullable Sort sort, int offset, int limit );
}
