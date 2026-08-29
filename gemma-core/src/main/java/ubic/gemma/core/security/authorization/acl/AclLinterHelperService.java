package ubic.gemma.core.security.authorization.acl;

import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;

import java.util.Collection;
import java.util.List;

/**
 * Transaction boundary for the ACL linter's parent-linking repair.
 * <p>
 * This exists so a bulk repair does not have to be one transaction. {@link AclLinterService}'s
 * entry points are {@code @Transactional}, and the fix for a whole type used to run inside that
 * single call: on production that is 631,709 BioAssay identities and as many BioMaterial ones, so
 * a failure anywhere threw away hours of committed-nothing work. Batches go through this bean with
 * {@code REQUIRES_NEW} instead, which needs a separate bean — a self-invocation is not proxied and
 * would silently join the caller's transaction.
 * <p>
 * Batching is safe here only because the repair is idempotent: a linked identity stops matching
 * {@code parent_object IS NULL}, so re-running after a failure picks up exactly what is left.
 */
public interface AclLinterHelperService {

    /**
     * Link each of {@code identifiers} to the parent ACL identity its entity resolves to, in a new
     * transaction that commits independently of the caller's.
     * <p>
     * Identifiers whose entity cannot be found, or whose parent cannot be resolved, are reported as
     * unfixed rather than failing the batch — a BioMaterial shared by more than one experiment has
     * no single parent to pick, and there is no point losing the rest of the batch over it.
     */
    Collection<AclLinterService.LintResult> linkParentsInNewTransaction( Class<? extends SecuredChild<?>> clazz, List<Long> identifiers );

    /**
     * Attach the ACL identity of {@code identifier} to {@code parentAoi}, joining the caller's
     * transaction.
     */
    void setParentAcl( Class<? extends Securable> clazz, Long identifier, AclObjectIdentity parentAoi );
}
