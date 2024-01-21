package ubic.gemma.core.security.authorization.acl;

import lombok.Value;
import org.springframework.security.access.annotation.Secured;
import ubic.gemma.model.common.auditAndSecurity.Securable;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * Lint and possibly fix ACL issues.
 *
 * @author poirigui
 */
public interface AclLinterService {

    @Secured("GROUP_ADMIN")
    Collection<LintResult> lintAcls( AclLinterConfig config );

    @Secured("GROUP_ADMIN")
    Collection<LintResult> lintAcls( Class<? extends Securable> clazz, AclLinterConfig config );

    @Secured("GROUP_ADMIN")
    Collection<LintResult> lintAcls( Class<? extends Securable> clazz, Long identifier, AclLinterConfig config );

    @Value
    class LintResult {
        Class<? extends Securable> type;
        @Nullable
        Long identifier;
        String problem;
        /**
         * Indicate if the issue was fixed or not.
         */
        boolean fixed;
    }
}
