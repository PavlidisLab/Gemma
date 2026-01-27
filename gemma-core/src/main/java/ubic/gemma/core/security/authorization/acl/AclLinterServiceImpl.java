package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.metadata.ClassMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CommonsLog
public class AclLinterServiceImpl implements AclLinterService {

    @Autowired
    private AclService aclService;
    @Autowired
    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy;
    @Autowired
    private SessionFactory sessionFactory;

    @Override
    @Transactional
    public Collection<LintResult> lintAcls( AclLinterConfig config ) {
        //noinspection unchecked
        Set<Class<? extends Securable>> classes = sessionFactory.getAllClassMetadata().values().stream()
                .map( ClassMetadata::getMappedClass )
                .filter( c -> !Modifier.isAbstract( c.getModifiers() ) )
                .filter( Securable.class::isAssignableFrom )
                .map( c -> ( Class<? extends Securable> ) c )
                .collect( Collectors.toSet() );
        return lintAcls( classes, null, config );
    }

    @Override
    @Transactional
    public Collection<LintResult> lintAcls( Class<? extends Securable> clazz, AclLinterConfig config ) {
        return lintAcls( Collections.singleton( clazz ), null, config );
    }

    @Override
    @Transactional
    public Collection<LintResult> lintAcls( Class<? extends Securable> clazz, Long identifier, AclLinterConfig config ) {
        return lintAcls( Collections.singleton( clazz ), identifier, config );
    }

    private Collection<LintResult> lintAcls( Collection<Class<? extends Securable>> classes, @Nullable Long identifier, AclLinterConfig config ) {
        Collection<LintResult> results = new LinkedHashSet<>();
        for ( Class<? extends Securable> clazz : classes ) {
            if ( config.isLintDanglingIdentities() && identifier == null ) {
                lintAclObjectIdentityLackingSecurable( clazz, config, results );
            }
            if ( config.isLintSecurablesLackingIdentities() ) {
                if ( identifier != null ) {
                    lintSecurableLackingObjectIdentity( clazz, identifier, config, results );
                } else {
                    lintSecurableLackingObjectIdentity( clazz, config, results );
                }
            }
            if ( config.isLintChildWithoutParent() && SecuredChild.class.isAssignableFrom( clazz ) ) {
                if ( identifier != null ) {
                    //noinspection unchecked
                    lintSecuredChildWithoutParent( ( Class<? extends SecuredChild<?>> ) clazz, identifier, config, results );
                } else {
                    //noinspection unchecked
                    lintSecuredChildWithoutParent( ( Class<? extends SecuredChild<?>> ) clazz, config, results );
                }
            }
            if ( config.isLintNotChildWithParent() && SecuredNotChild.class.isAssignableFrom( clazz ) ) {
                if ( identifier != null ) {
                    //noinspection unchecked
                    lintSecuredNotChildWithParent( ( Class<? extends SecuredNotChild> ) clazz, identifier, config, results );
                } else {
                    //noinspection unchecked
                    lintSecuredNotChildWithParent( ( Class<? extends SecuredNotChild> ) clazz, config, results );
                }
            }
            if ( config.isLintPermissions() ) {
                lintPermissions( clazz, identifier, config, results );
            }
        }
        return results;
    }

    /**
     * Lint for ACL object identities that lack a corresponding securable entity.
     * <p>
     * In this case, the fix is to remove the dangling ACL identity.
     */
    private void lintAclObjectIdentityLackingSecurable( Class<? extends Securable> clazz, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting ACL object identities lacking associated " + clazz.getSimpleName() + "..." );
        //noinspection unchecked
        List<AclObjectIdentity> list = sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi "
                        + "where aoi.type = :type and aoi.identifier not in (select e.id from " + clazz.getName() + " e)" )
                .setParameter( "type", clazz.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .list();
        if ( list.isEmpty() ) {
            log.info( "There are no dangling ACL object identities for " + clazz.getSimpleName() + "." );
        } else {
            log.warn( "There are " + list.size() + " dangling ACL object identities for " + clazz.getSimpleName() + "." );
        }
        for ( AclObjectIdentity aoi : list ) {
            if ( config.isApplyFixes() ) {
                // not removing child, but we will visit it later if it's also dangling
                aclService.deleteAcl( aoi, true );
                log.info( "Deleted dangling " + aoi + "." );
                results.add( new LintResult( clazz, aoi.getIdentifier(), String.format( "%s has no corresponding %s entity with ID %d.", aoi, clazz.getName(), aoi.getIdentifier() ), true ) );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), String.format( "%s has no corresponding %s entity with ID %d.", aoi, clazz.getName(), aoi.getIdentifier() ), false ) );
            }
        }
    }

    /**
     * Lint for securable entities that lack an ACL object identity.
     * <p>
     * The fix is to create the missing ACL identity.
     */
    private void lintSecurableLackingObjectIdentity( Class<? extends Securable> clazz, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting " + clazz.getSimpleName() + " lacking ACL object identities..." );
        //noinspection unchecked
        List<? extends Securable> list = sessionFactory.getCurrentSession()
                .createQuery( "select e from " + clazz.getName() + " e "
                        + "where e.id not in ( " + "select aoi.identifier from AclObjectIdentity aoi where aoi.type = :type" + ")" )
                .setParameter( "type", clazz.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .list();
        if ( list.isEmpty() ) {
            log.info( "All " + clazz.getSimpleName() + " have ACL identities." );
        } else {
            log.warn( "There are " + list.size() + " " + clazz.getSimpleName() + " lacking ACL identities." );
        }
        for ( Securable s : list ) {
            if ( config.isApplyFixes() ) {
                aclService.createAcl( objectIdentityRetrievalStrategy.getObjectIdentity( s ) );
                log.info( "Created missing ACL identity for " + s + "." );
                results.add( new LintResult( s.getClass(), s.getId(), s + " lacks an ACL identity.", true ) );
            } else {
                results.add( new LintResult( s.getClass(), s.getId(), s + " lacks an ACL identity.", false ) );
            }
        }
    }

    private void lintSecurableLackingObjectIdentity( Class<? extends Securable> clazz, Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        Securable s = ( Securable ) sessionFactory.getCurrentSession()
                .createQuery( "select e from " + clazz.getName() + " e "
                        + "where e.id = :identifier and e.id not in ( " + "select aoi.identifier from AclObjectIdentity aoi where aoi.type = :type" + ")" )
                .setParameter( "identifier", identifier )
                .setParameter( "type", clazz.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .uniqueResult();
        if ( s == null ) {
            log.info( clazz.getSimpleName() + " Id=" + identifier + " has an ACL identity." );
            return;
        }
        if ( config.isApplyFixes() ) {
            aclService.createAcl( objectIdentityRetrievalStrategy.getObjectIdentity( s ) );
            log.info( "Created missing ACL identity for " + s + "." );
        } else {
            results.add( new LintResult( s.getClass(), s.getId(), s + " lacks an ACL identity.", false ) );
        }
    }

    /**
     * Lint for secured children that lack a parent ACL identity.
     * TODO: implement a fix, but that require knowing how to resolve the parent.
     */
    private void lintSecuredChildWithoutParent( Class<? extends SecuredChild<?>> clazz, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting " + clazz.getSimpleName() + " lacking parent ACL identities..." );
        //noinspection unchecked
        List<AclObjectIdentity> list = sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi "
                        + "where aoi.type = :type "
                        + "and aoi.parentObject is null" )
                .setParameter( "type", clazz.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .list();
        if ( list.isEmpty() ) {
            log.info( "All " + clazz.getSimpleName() + " have parent ACL identities." );
        } else {
            log.warn( "There are " + list.size() + " " + clazz.getSimpleName() + " lacking parent ACL identities." );
        }
        for ( AclObjectIdentity aoi : list ) {
            if ( config.isApplyFixes() ) {
                fixMissingParent( clazz, aoi, results );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), aoi.getType() + " Id=" + aoi.getIdentifier() + " has no parent ACL identity.", false ) );
            }
        }
    }

    private void lintSecuredChildWithoutParent( Class<? extends SecuredChild<?>> clazz, Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        AclObjectIdentity aoi = ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi "
                        + "where aoi.identifier = :identifier and aoi.type = :type "
                        + "and aoi.parentObject is null" )
                .setParameter( "identifier", identifier )
                .setParameter( "type", clazz.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .uniqueResult();
        String s = clazz.getSimpleName() + " Id=" + identifier;
        if ( aoi == null ) {
            log.info( s + " has a parent ACL identity." );
            return;
        }
        if ( config.isApplyFixes() ) {
            fixMissingParent( clazz, aoi, results );
        } else {
            results.add( new LintResult( clazz, aoi.getIdentifier(), s + " has no parent ACL identity.", false ) );
        }
    }

    private void fixMissingParent( Class<? extends SecuredChild<?>> clazz, AclObjectIdentity aoi, Collection<LintResult> results ) {
        log.warn( "Cannot fix missing parent ACL identity for " + aoi.getType() + "." );
        results.add( new LintResult( clazz, aoi.getId(), aoi.getType() + " Id=" + aoi.getIdentifier() + " has no parent ACL identity.", false ) );
    }

    /**
     * Lint for securable entities that are explicitly not children, but have a parent object set in their ACL identity.
     * <p>
     * The fix is to detach them from their parent.
     */
    private void lintSecuredNotChildWithParent( Class<? extends SecuredNotChild> clazz, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting " + clazz.getSimpleName() + " with parent ACL identities..." );
        //noinspection unchecked
        List<AclObjectIdentity> list = sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi "
                        + "where aoi.type = :type "
                        + "and aoi.parentObject is not null" )
                .setParameter( "type", clazz.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .list();
        if ( list.isEmpty() ) {
            log.info( "No " + clazz.getSimpleName() + " have parent ACL identities; this is expected as it implements the SecuredNotChild interface." );
        } else {
            log.warn( "There are " + list.size() + " " + clazz.getSimpleName() + " with parent ACL identities; this is not expected as it implements the SecuredNotChild interface." );
        }
        for ( AclObjectIdentity aoi : list ) {
            String s = clazz.getSimpleName() + " Id=" + aoi.getIdentifier();
            if ( config.isApplyFixes() ) {
                aoi.setParentObject( null );
                log.info( "Detached parent ACL identity from " + s + "." );
                results.add( new LintResult( clazz, aoi.getIdentifier(), s + " has a parent ACL identity, but it implements the SecuredNotChild interface.", true ) );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), s + " has a parent ACL identity, but it implements the SecuredNotChild interface.", false ) );
            }
        }
    }

    /**
     * Lint for securable entities that are explicitly not children, but have a parent object set in their ACL identity.
     * <p>
     * The fix is to detach them from their parent.
     */
    private void lintSecuredNotChildWithParent( Class<? extends SecuredNotChild> clazz, Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        AclObjectIdentity aoi = ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi "
                        + "where aoi.identifier = :identifier and aoi.type = :type "
                        + "and aoi.parentObject is not null" )
                .setParameter( "identifier", identifier )
                .setParameter( "type", clazz.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .uniqueResult();
        String s = clazz.getSimpleName() + " Id=" + identifier;
        if ( aoi == null ) {
            log.info( s + " has no parent ACL identity; this is expected as it implements the SecuredNotChild interface." );
            return;
        }
        if ( config.isApplyFixes() ) {
            aoi.setParentObject( null );
            log.info( "Detached parent ACL identity from " + s + "." );
            results.add( new LintResult( clazz, aoi.getIdentifier(), s + " has a parent ACL identity, but it implements the SecuredNotChild interface.", true ) );
        } else {
            results.add( new LintResult( clazz, aoi.getIdentifier(), s + " has a parent ACL identity, but it implements the SecuredNotChild interface.", false ) );
        }
    }

    /**
     * Lint permissions.
     */
    private void lintPermissions( Class<? extends Securable> clazz, @Nullable Long identifier, AclLinterConfig config, Collection<LintResult> result ) {
        log.info( "Linting permissions for " + clazz.getSimpleName() + "..." );
        lintPermissions( clazz, identifier, "GROUP_ADMIN", BasePermission.ADMINISTRATION, true, config, result );
        lintPermissions( clazz, identifier, "GROUP_AGENT", BasePermission.READ, true, config, result );
    }

    private void lintPermissions( Class<? extends Securable> clazz, @Nullable Long identifier, String grantedAuthority, Permission permission, @SuppressWarnings("SameParameterValue") boolean granting, AclLinterConfig config, Collection<LintResult> result ) {
        Query query = sessionFactory.getCurrentSession()
                .createSQLQuery( "select aoi.OBJECT_CLASS, aoi.OBJECT_ID "
                        + "from ACLOBJECTIDENTITY aoi "
                        + "where aoi.OBJECT_CLASS = :type "
                        + ( identifier != null ? " and aoi.OBJECT_ID = :identifier " : "" )
                        + "and aoi.ID not in "
                        + "(select ace.OBJECTIDENTITY_FK "
                        + "from ACLENTRY ace "
                        // aoi2 is only there to limit the size of the subquery
                        + "join ACLOBJECTIDENTITY aoi2 on ace.OBJECTIDENTITY_FK = aoi2.ID "
                        + "join ACLSID sid on ace.SID_FK = sid.ID "
                        + "where aoi2.OBJECT_CLASS = :type "
                        + ( identifier != null ? " and aoi2.OBJECT_ID = :identifier " : "" )
                        + "and sid.GRANTED_AUTHORITY = :grantedAuthority "
                        + "and ace.GRANTING = :granting "
                        + "and (ace.MASK & :permissionMask) <> 0)" )
                .setParameter( "type", clazz.getName() )
                .setParameter( "grantedAuthority", grantedAuthority )
                .setParameter( "granting", granting )
                .setParameter( "permissionMask", permission.getMask() );
        if ( identifier != null ) {
            query.setParameter( "identifier", identifier );
        }
        //noinspection unchecked
        List<Object[]> list = query
                .setReadOnly( true )
                .list();
        if ( list.isEmpty() ) {
            log.info( "All permissions are correct for " + clazz.getSimpleName() + ( identifier != null ? " Id=" + identifier : "" ) + "." );
        } else {
            log.warn( "There are " + list.size() + " permission issues for " + clazz.getSimpleName() + ( identifier != null ? " Id=" + identifier : "" ) + "." );
        }
        for ( Object[] row : list ) {
            String type = ( String ) row[0];
            Long identifier_ = ( ( BigInteger ) row[1] ).longValue();
            Securable s = ( Securable ) sessionFactory.getCurrentSession().get( type, identifier_ );
            String problem = String.format( "%s lacks an ACL entry with %s to %s%s.", s, permission, grantedAuthority,
                    granting ? " with granting" : "" );
            if ( config.isApplyFixes() ) {
                MutableAcl acl = ( MutableAcl ) aclService.readAclById( new AclObjectIdentity( type, identifier_ ) );
                acl.insertAce( acl.getEntries().size(), permission, new AclGrantedAuthoritySid( grantedAuthority ), granting );
                aclService.updateAcl( acl );
                log.info( "Added missing permissions for " + grantedAuthority + " to " + s + "." );
                result.add( new LintResult( s.getClass(), identifier_, problem, true ) );
            } else {
                result.add( new LintResult( s.getClass(), identifier_, problem, false ) );
            }
        }
    }
}
