package ubic.gemma.core.security.authorization.acl;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@Slf4j
public class AclLinterHelperServiceImpl implements AclLinterHelperService {

    @Autowired
    private AclService aclService;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ParentIdentityRetrievalStrategy parentIdentityRetrievalStrategy;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Collection<AclLinterService.LintResult> linkParentsInNewTransaction( Class<? extends SecuredChild<?>> clazz, List<Long> identifiers ) {
        List<AclLinterService.LintResult> results = new ArrayList<>( identifiers.size() );
        for ( Long identifier : identifiers ) {
            SecuredChild<?> sc = ( SecuredChild<?> ) sessionFactory.getCurrentSession().get( clazz, identifier );
            if ( sc == null ) {
                log.warn( "Could not find " + clazz.getSimpleName() + " Id=" + identifier + "." );
                results.add( new AclLinterService.LintResult( clazz, identifier, "Entity is a SecuredChild with no parent ACL identity. The fix could not be applied because the entity could not be found.", false ) );
                continue;
            }
            AclObjectIdentity parentAoi = ( AclObjectIdentity ) parentIdentityRetrievalStrategy.getParentIdentity( sc );
            if ( parentAoi != null ) {
                setParentAcl( clazz, identifier, parentAoi );
                String fixMessage = "Parent ACL identity was set to " + parentAoi + ".";
                log.info( clazz.getSimpleName() + " Id=" + identifier + ": " + fixMessage );
                results.add( new AclLinterService.LintResult( clazz, identifier, fixMessage, true ) );
            } else {
                results.add( new AclLinterService.LintResult( clazz, identifier, "Entity is a SecuredChild with no parent ACL identity. The fix could not be applied because the parent ACL identity could not be found.", false ) );
            }
            // remove to prevent SecuredChild from piling up in memory
            sessionFactory.getCurrentSession().evict( sc );
        }
        return results;
    }

    /**
     * This MUST go through {@link AclService#updateAcl(MutableAcl)} rather than
     * {@link AclObjectIdentity#setParentObject(AclObjectIdentity)}. {@code AclObjectIdentity} is
     * annotated {@code @Immutable}, so Hibernate silently discards the dirty state and the fix never
     * reaches the database — no exception, no UPDATE. On 2026-08-06 a production repair run reported
     * 548 successful parent assignments while writing none of them. Routing through
     * {@code JdbcMutableAclService} makes the write land and evicts the ACL cache.
     * <p>
     * entries_inheriting has to be turned on with the parent, not just the parent set. Spring only
     * walks to the parent when it is on, so a link written without it grants nothing and the run
     * still reports a fix. It is off on exactly the rows this repairs: BaseAclAdvice sets it from
     * inheritFromParent, which is false when no parent was discoverable at insert time — the same
     * branch that gives the child its own ACEs. Those ACEs stay, and correctly so: Spring checks an
     * ACL's own entries before the parent's. AclEventListener.handleChild sets both for the same
     * reason.
     */
    @Override
    @Transactional
    public void setParentAcl( Class<? extends Securable> clazz, Long identifier, AclObjectIdentity parentAoi ) {
        MutableAcl acl = ( MutableAcl ) aclService.readAclById( new AclObjectIdentity( clazz, identifier ) );
        acl.setParent( aclService.readAclById( parentAoi ) );
        acl.setEntriesInheriting( true );
        aclService.updateAcl( acl );
    }
}
