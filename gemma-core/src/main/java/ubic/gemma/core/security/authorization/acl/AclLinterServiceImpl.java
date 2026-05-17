package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclGrantedAuthoritySid;
import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import lombok.extern.apachecommons.CommonsLog;
import org.hibernate.Query;
import org.hibernate.SessionFactory;
import org.hibernate.dialect.function.SQLFunction;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metadata.ClassMetadata;
import org.hibernate.type.IntegerType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.protocol.Protocol;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CommonsLog
public class AclLinterServiceImpl implements AclLinterService {

    /**
     * A set of securable classes that should always be publicly accessible.
     */
    private static final Set<Class<? extends Securable>> shouldBePublic = new HashSet<>();

    static {
        shouldBePublic.add( ExternalDatabase.class );
        shouldBePublic.add( Protocol.class );
    }

    @Autowired
    private AclService aclService;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ParentIdentityRetrievalStrategy parentIdentityRetrievalStrategy;
    @Autowired
    private AclClassMetadata aclClassMetadata;

    @Override
    @Transactional
    public Collection<LintResult> lintAcls( AclLinterConfig config ) {
        //noinspection unchecked
        // Hibernate 5: getAllClassMetadata() throws UnsupportedOperationException; use the JPA metamodel.
        Set<Class<? extends Securable>> classes = sessionFactory.getMetamodel().getEntities().stream()
                .map( javax.persistence.metamodel.EntityType::getJavaType )
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
                //noinspection unchecked
                Class<? extends SecuredChild<?>> scc = ( Class<? extends SecuredChild<?>> ) clazz;
                if ( identifier != null ) {
                    lintSecuredChildWithoutParent( scc, identifier, config, results );
                } else {
                    lintSecuredChildWithoutParent( scc, config, results );
                }
            }
            if ( config.isLintChildWithIncorrectParent() && SecuredChild.class.isAssignableFrom( clazz ) ) {
                //noinspection unchecked
                Class<? extends SecuredChild<?>> scc = ( Class<? extends SecuredChild<?>> ) clazz;
                Class<? extends Securable> parentType = aclClassMetadata.getSecurityOwnerClass( scc );
                List<String> parentIdQueries = aclClassMetadata.getSecurityOwnerIdQueries( scc, "aoi.identifier" );
                if ( identifier != null ) {
                    lintSecuredChildWithIncorrectParent( scc, parentType, parentIdQueries, identifier, config, results );
                } else {
                    lintSecuredChildWithIncorrectParent( scc, parentType, parentIdQueries, config, results );
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
                        + "where aoi.type = :type and aoi.identifier not in (select e.id from " + sessionFactory.getClassMetadata( clazz ).getEntityName() + " e)" )
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
                aclService.deleteAcl( aoi, true );
                log.info( "Deleted dangling " + aoi + "." );
                results.add( new LintResult( clazz, aoi.getIdentifier(), "Deleted dangling ACL identity.", true ) );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), String.format( "ACL identity has no corresponding entity with ID %d.", aoi.getIdentifier() ), false ) );
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
        List<Long> list = sessionFactory.getCurrentSession()
                .createQuery( "select e.id from " + sessionFactory.getClassMetadata( clazz ).getEntityName() + " e "
                        + "where e.id not in (select aoi.identifier from AclObjectIdentity aoi where aoi.type = :type)" )
                .setParameter( "type", clazz.getName() )
                .list();
        if ( list.isEmpty() ) {
            log.info( "All " + clazz.getSimpleName() + " have ACL identities." );
        } else {
            log.warn( "There are " + list.size() + " " + clazz.getSimpleName() + " lacking ACL identities." );
        }
        for ( Long identifier : list ) {
            if ( config.isApplyFixes() ) {
                aclService.createAcl( new AclObjectIdentity( clazz, identifier ) );
                log.info( "Created missing ACL identity for " + formatEntity( clazz, identifier ) + "." );
                results.add( new LintResult( clazz, identifier, "ACL identity was created.", true ) );
            } else {
                results.add( new LintResult( clazz, identifier, "Entity lacks an ACL identity.", false ) );
            }
        }
    }

    /**
     * Lint for a securable entity that lacks an ACL object identity.
     * <p>
     * The fix is to create the missing ACL identity.
     */
    private void lintSecurableLackingObjectIdentity( Class<? extends Securable> clazz, Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        Boolean hasAoi = ( Boolean ) sessionFactory.getCurrentSession()
                .createQuery( "select count(*) > 0 from " + sessionFactory.getClassMetadata( clazz ).getEntityName() + " e "
                        + "where e.id = :identifier and e.id not in (select aoi.identifier from AclObjectIdentity aoi where aoi.type = :type and aoi.identifier = :identifier)" )
                .setParameter( "identifier", identifier )
                .setParameter( "type", clazz.getName() )
                .uniqueResult();
        if ( hasAoi ) {
            log.info( formatEntity( clazz, identifier ) + " has an ACL identity." );
            return;
        }
        if ( config.isApplyFixes() ) {
            aclService.createAcl( new AclObjectIdentity( clazz, identifier ) );
            log.info( "Created missing ACL identity for " + formatEntity( clazz, identifier ) + "." );
            results.add( new LintResult( clazz, identifier, "ACL identity was created.", true ) );
        } else {
            results.add( new LintResult( clazz, identifier, "Entity lacks an ACL identity.", false ) );
        }
    }

    /**
     * Lint for secured children that lack a parent ACL identity.
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
                SecuredChild<?> sc = getSecuredChild( clazz, aoi.getIdentifier() );
                if ( sc == null ) {
                    log.warn( "Could not find " + formatEntity( clazz, aoi ) + "." );
                    results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity is a SecuredChild with no parent ACL identity. The fix could not be applied because the entity could not be found.", false ) );
                    continue;
                }
                AclObjectIdentity parentAoi = ( AclObjectIdentity ) parentIdentityRetrievalStrategy.getParentIdentity( sc );
                if ( parentAoi != null ) {
                    aoi.setParentObject( parentAoi );
                    String fixMessage = "Parent ACL identity was set to " + parentAoi + ".";
                    log.info( formatEntity( clazz, aoi ) + ": " + fixMessage );
                    results.add( new LintResult( clazz, aoi.getIdentifier(), fixMessage, true ) );
                } else {
                    results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity is a SecuredChild with no parent ACL identity. The fix could not be applied because the parent ACL identity could not be found.", false ) );
                }
                // remove to prevent SecuredChild to pile up in memory
                sessionFactory.getCurrentSession().evict( sc );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity is a SecuredChild with no parent ACL identity.", false ) );
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
        if ( aoi == null ) {
            log.info( formatEntity( clazz, identifier ) + " has an ACL parent identity." );
            return;
        }
        if ( config.isApplyFixes() ) {
            SecuredChild<?> sc = getSecuredChild( clazz, aoi.getIdentifier() );
            if ( sc == null ) {
                log.warn( "Could not find " + formatEntity( clazz, aoi ) + "." );
                results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity is a SecuredChild with no parent ACL identity. The fix could not be applied because the entity could not be found.", false ) );
                return;
            }
            AclObjectIdentity parentAoi = ( AclObjectIdentity ) parentIdentityRetrievalStrategy.getParentIdentity( sc );
            if ( parentAoi != null ) {
                aoi.setParentObject( parentAoi );
                String fixMessage = "Parent ACL identity was set to " + parentAoi + ".";
                log.info( formatEntity( clazz, aoi ) + ": " + fixMessage );
                results.add( new LintResult( clazz, aoi.getIdentifier(), fixMessage, true ) );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity is a SecuredChild with no parent ACL identity.", false ) );
            }
        } else {
            results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity is a SecuredChild with no parent ACL identity.", false ) );
        }
    }

    /**
     * @param expectedParentClass     expected parent type
     * @param expectedParentIdQueries expected
     */
    private void lintSecuredChildWithIncorrectParent( Class<? extends SecuredChild<?>> clazz, Class<? extends Securable> expectedParentClass, @Nullable List<String> expectedParentIdQueries, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting " + clazz.getSimpleName() + " with incorrect parent ACL identities..." );
        //noinspection unchecked
        List<AclObjectIdentity> list = sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi join aoi.parentObject parentAoi "
                        + "where aoi.type = :type "
                        + "and (parentAoi.type <> :parentType"
                        + ( expectedParentIdQueries != null ? expectedParentIdQueries.stream().map( expectedParentIdQuery -> " or parentAoi.identifier <> (" + expectedParentIdQuery + ")" ).collect( Collectors.joining() ) : "" )
                        + ")" )
                .setParameter( "type", clazz.getName() )
                .setParameter( "parentType", expectedParentClass.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .list();
        if ( list.isEmpty() ) {
            log.info( "All " + clazz.getSimpleName() + " have correct parent ACL identities." );
        } else {
            log.warn( "There are " + list.size() + " " + clazz.getSimpleName() + " with incorrect parent ACL identities." );
        }
        for ( AclObjectIdentity aoi : list ) {
            if ( config.isApplyFixes() ) {
                SecuredChild<?> sc = getSecuredChild( clazz, aoi.getIdentifier() );
                if ( sc == null ) {
                    log.warn( "Could not find " + formatEntity( clazz, aoi ) + "." );
                    continue;
                }
                AclObjectIdentity parentAoi = ( AclObjectIdentity ) parentIdentityRetrievalStrategy.getParentIdentity( sc );
                if ( parentAoi != null ) {
                    aoi.setParentObject( parentAoi );
                    String fixMessage = "Parent ACL identity was set to " + parentAoi + ".";
                    log.info( formatEntity( clazz, aoi ) + ": " + fixMessage );
                    results.add( new LintResult( clazz, aoi.getIdentifier(), fixMessage, true ) );
                } else {
                    results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity does not have a correct parent ACL identity.", false ) );
                }
                sessionFactory.getCurrentSession().evict( sc );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity does not have a correct parent ACL identity.", false ) );
            }
        }
    }

    private void lintSecuredChildWithIncorrectParent( Class<? extends SecuredChild<?>> clazz,
            Class<? extends Securable> expectedParentClass,
            @Nullable List<String> expectedParentIdQueries,
            Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        AclObjectIdentity aoi = ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi join aoi.parentObject as parentAoi "
                        + "where aoi.identifier = :identifier and aoi.type = :type "
                        + "and (parentAoi.type <> :parentType"
                        + ( expectedParentIdQueries != null ? expectedParentIdQueries.stream().map( expectedParentIdHql -> " or parentAoi.identifier <> (" + expectedParentIdHql + ")" ).collect( Collectors.joining() ) : "" )
                        + ")" )
                .setParameter( "identifier", identifier )
                .setParameter( "type", clazz.getName() )
                .setParameter( "parentType", expectedParentClass.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .uniqueResult();
        if ( aoi == null ) {
            log.info( formatEntity( clazz, identifier ) + " has a correct parent ACL identity." );
            return;
        }
        if ( config.isApplyFixes() ) {
            SecuredChild<?> sc = getSecuredChild( clazz, aoi.getIdentifier() );
            if ( sc == null ) {
                log.warn( "Could not find " + formatEntity( clazz, aoi ) + "." );
                return;
            }
            AclObjectIdentity parentAoi = ( AclObjectIdentity ) parentIdentityRetrievalStrategy.getParentIdentity( sc );
            if ( parentAoi != null ) {
                aoi.setParentObject( parentAoi );
                String fixMessage = "Parent ACL identity was set to " + parentAoi + ".";
                log.info( formatEntity( clazz, aoi ) + ": " + fixMessage );
                results.add( new LintResult( clazz, aoi.getIdentifier(), fixMessage, true ) );
            } else {
                results.add( new LintResult( clazz, identifier, "Entity does not have a correct parent ACL identity.", false ) );
            }
        } else {
            results.add( new LintResult( clazz, identifier, "Entity does not have a correct parent ACL identity.", false ) );
        }
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
            if ( config.isApplyFixes() ) {
                aoi.setParentObject( null );
                String fixMessage = "Detached parent ACL identity.";
                log.info( formatEntity( clazz, aoi ) + ": " + fixMessage );
                results.add( new LintResult( clazz, aoi.getIdentifier(), fixMessage, true ) );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity has a parent ACL identity, but it implements the SecuredNotChild interface.", false ) );
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
        if ( aoi == null ) {
            log.info( formatEntity( clazz, identifier ) + " has no parent ACL identity; this is expected as it implements the SecuredNotChild interface." );
            return;
        }
        if ( config.isApplyFixes() ) {
            aoi.setParentObject( null );
            String fixMessage = "Detached parent ACL identity.";
            log.info( formatEntity( clazz, identifier ) + ": " + fixMessage );
            results.add( new LintResult( clazz, aoi.getIdentifier(), fixMessage, true ) );
        } else {
            results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity has a parent ACL identity, but it implements the SecuredNotChild interface.", false ) );
        }
    }

    /**
     * Lint permissions.
     */
    private void lintPermissions( Class<? extends Securable> clazz, @Nullable Long identifier, AclLinterConfig config, Collection<LintResult> result ) {
        log.info( "Linting permissions for " + clazz.getSimpleName() + "..." );
        if ( SecuredChild.class.isAssignableFrom( clazz ) ) {
            // permissions are inherited from parent
            log.info( "No need to lint permissions for " + clazz.getSimpleName() + " as it is a SecuredChild and permissions are inherited from the parent." );
            return;
        }
        // see BaseAclAdvice.setupBaseAces()
        lintPermissions( clazz, identifier, "GROUP_ADMIN", BasePermission.ADMINISTRATION, true, config, result );
        lintPermissions( clazz, identifier, "GROUP_AGENT", BasePermission.READ, true, config, result );
        if ( shouldBePublic.contains( clazz ) ) {
            lintPermissions( clazz, identifier, "IS_AUTHENTICATED_ANONYMOUSLY", BasePermission.READ, true, config, result );
        }
    }

    private void lintPermissions( Class<? extends Securable> clazz, @Nullable Long identifier, String grantedAuthority, Permission permission, @SuppressWarnings("SameParameterValue") boolean granting, AclLinterConfig config, Collection<LintResult> result ) {
        SQLFunction bitwiseAnd = ( ( SessionFactoryImplementor ) sessionFactory )
                .getSqlFunctionRegistry()
                .findSQLFunction( "bitwise_and" );
        String renderedMask = bitwiseAnd.render( new IntegerType(), Arrays.asList( "ace.MASK", permission.getMask() ),
                ( SessionFactoryImplementor ) sessionFactory );
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
                        + "and (" + renderedMask + ") <> 0)" )
                .setParameter( "type", clazz.getName() )
                .setParameter( "grantedAuthority", grantedAuthority )
                .setParameter( "granting", granting );
        if ( identifier != null ) {
            query.setParameter( "identifier", identifier );
        }
        String aclEntryDescription = String.format( "ACL entry with %s for %s%s", permission, grantedAuthority,
                granting ? " with granting" : "" );
        //noinspection unchecked
        List<Object[]> list = query.list();
        if ( list.isEmpty() ) {
            log.info( String.format( "%s have an %s.",
                    identifier != null ? formatEntity( clazz, identifier ) : "All " + clazz.getSimpleName(),
                    aclEntryDescription ) );
        } else {
            log.warn( String.format( "%s lack an %s.",
                    identifier != null ? formatEntity( clazz, identifier ) : list.size() + " " + clazz.getSimpleName(),
                    aclEntryDescription ) );
        }
        for ( Object[] row : list ) {
            String type = ( String ) row[0];
            Long identifier_ = ( ( BigInteger ) row[1] ).longValue();
            if ( config.isApplyFixes() ) {
                MutableAcl acl = ( MutableAcl ) aclService.readAclById( new AclObjectIdentity( type, identifier_ ) );
                acl.insertAce( acl.getEntries().size(), permission, new AclGrantedAuthoritySid( grantedAuthority ), granting );
                aclService.updateAcl( acl );
                String fixMessage = "Added missing " + aclEntryDescription + ".";
                log.info( formatEntity( clazz, identifier_ ) + ": " + fixMessage );
                result.add( new LintResult( clazz, identifier_, fixMessage, true ) );
            } else {
                String problem = String.format( "Entity lacks an " + aclEntryDescription + "." );
                result.add( new LintResult( clazz, identifier_, problem, false ) );
            }
        }
    }

    @Nullable
    private SecuredChild<?> getSecuredChild( Class<? extends SecuredChild<?>> clazz, Long identifier ) {
        return ( SecuredChild<?> ) sessionFactory.getCurrentSession().get( clazz, identifier );
    }

    private String formatEntity( Class<?> clazz, AclObjectIdentity aoi ) {
        return formatEntity( clazz, aoi.getIdentifier() );
    }

    private String formatEntity( Class<?> clazz, @Nullable Long identifier ) {
        return clazz.getSimpleName() + ( identifier != null ? " Id=" + identifier : "" );
    }
}
