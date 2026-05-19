package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.domain.AclObjectIdentity;
import gemma.gsec.acl.domain.AclService;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.SessionFactory;
import org.hibernate.query.NativeQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

import org.springframework.lang.Nullable;
import javax.sql.DataSource;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
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

    /**
     * Renovations Phase 3: gsec HQL deprecation. Direct JdbcTemplate access to the canonical
     * Spring Security ACL tables ({@code acl_class}, {@code acl_object_identity}, {@code acl_sid},
     * {@code acl_entry}) lets us retire HQL references to {@code gemma.gsec.acl.domain.*} entity
     * mappings one query at a time. Initialised lazily from the {@link DataSource} so the field
     * can be {@code @Autowired} without forcing the wiring into a constructor.
     */
    private JdbcTemplate jdbcTemplate;

    @Autowired
    public void setDataSource( DataSource dataSource ) {
        this.jdbcTemplate = new JdbcTemplate( dataSource );
    }

    @Override
    @Transactional
    public Collection<LintResult> lintAcls( AclLinterConfig config ) {
        //noinspection unchecked
        // Hibernate 5: getAllClassMetadata() throws UnsupportedOperationException; use the JPA metamodel.
        Set<Class<? extends Securable>> classes = sessionFactory.getMetamodel().getEntities().stream()
                .map( jakarta.persistence.metamodel.EntityType::getJavaType )
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
        // Phase 3 gsec HQL deprecation: pull existing AOI identifiers for this class via raw SQL
        // against acl_object_identity + acl_class (canonical Spring Security schema), then compute
        // the set-difference against entity ids in Java. Replaces the previous HQL "not in"
        // subquery against gsec's AclObjectIdentity entity mapping. The downstream consumer
        // (aclService.deleteAcl) accepts any ObjectIdentity; we construct a fresh
        // AclObjectIdentity(class, identifier) for the deletion call rather than carrying around
        // a managed entity (the gsec mapping is mutable="false" anyway, so the prior path's
        // "managed" status bought us nothing).
        List<Long> aoiIdentifiers = jdbcTemplate.queryForList(
                "select aoi.object_id_identity "
                        + "from acl_object_identity aoi "
                        + "join acl_class cls on aoi.object_id_class = cls.id "
                        + "where cls.class = ?",
                Long.class, clazz.getName() );
        //noinspection unchecked
        Set<Long> entityIds = new HashSet<>( sessionFactory.getCurrentSession()
                .createQuery( "select e.id from " + sessionFactory.getMetamodel().entity( clazz ).getName() + " e" )
                .list() );
        List<Long> danglingIdentifiers = new ArrayList<>();
        for ( Long aoiId : aoiIdentifiers ) {
            if ( !entityIds.contains( aoiId ) ) {
                danglingIdentifiers.add( aoiId );
            }
        }
        if ( danglingIdentifiers.isEmpty() ) {
            log.info( "There are no dangling ACL object identities for " + clazz.getSimpleName() + "." );
        } else {
            log.warn( "There are " + danglingIdentifiers.size() + " dangling ACL object identities for " + clazz.getSimpleName() + "." );
        }
        for ( Long identifier : danglingIdentifiers ) {
            if ( config.isApplyFixes() ) {
                AclObjectIdentity aoi = new AclObjectIdentity( clazz, identifier );
                aclService.deleteAcl( aoi, true );
                log.info( "Deleted dangling " + aoi + "." );
                results.add( new LintResult( clazz, identifier, "Deleted dangling ACL identity.", true ) );
            } else {
                results.add( new LintResult( clazz, identifier, String.format( "ACL identity has no corresponding entity with ID %d.", identifier ), false ) );
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
        // Phase 3 gsec HQL deprecation: pull the existing AOI identifiers for this class via raw
        // SQL against acl_object_identity + acl_class (canonical Spring Security schema), then
        // compute the set-difference against the entity ids in Java. Replaces the previous HQL
        // subquery against gsec's AclObjectIdentity entity mapping.
        Set<Long> existingAoiIdentifiers = new HashSet<>( jdbcTemplate.queryForList(
                "select aoi.object_id_identity "
                        + "from acl_object_identity aoi "
                        + "join acl_class cls on aoi.object_id_class = cls.id "
                        + "where cls.class = ?",
                Long.class, clazz.getName() ) );
        //noinspection unchecked
        List<Long> allEntityIds = sessionFactory.getCurrentSession()
                .createQuery( "select e.id from " + sessionFactory.getMetamodel().entity( clazz ).getName() + " e" )
                .list();
        List<Long> list = new ArrayList<>();
        for ( Long id : allEntityIds ) {
            if ( !existingAoiIdentifiers.contains( id ) ) {
                list.add( id );
            }
        }
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
        // Phase 3 gsec HQL deprecation: existence check against acl_object_identity + acl_class
        // via raw SQL. Replaces the previous HQL form that used a {@code not in (select
        // aoi.identifier from AclObjectIdentity ...)} subquery, inverted in Java.
        Integer aoiCount = jdbcTemplate.queryForObject(
                "select count(*) "
                        + "from acl_object_identity aoi "
                        + "join acl_class cls on aoi.object_id_class = cls.id "
                        + "where cls.class = ? and aoi.object_id_identity = ?",
                Integer.class, clazz.getName(), identifier );
        boolean hasAoi = aoiCount != null && aoiCount > 0;
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
     * <p>
     * Phase 3 gsec HQL deprecation: read the AOI identifiers via raw SQL against the canonical
     * Spring Security tables ({@code acl_object_identity} JOIN {@code acl_class}), then drive the
     * fix through {@link AclService#updateAcl(MutableAcl)}. This replaces the previous HQL select
     * + Hibernate dirty-flush path. Crucially, gsec's {@code AclObjectIdentity} mapping is
     * {@code mutable="false"}: the prior {@code aoi.setParentObject(null)} call was a silent
     * no-op — fixes never persisted. Routing through {@code aclService.updateAcl} (which delegates
     * to {@link org.springframework.security.acls.jdbc.JdbcMutableAclService}) makes the fix
     * actually land, and gets cache invalidation for free.
     */
    private void lintSecuredNotChildWithParent( Class<? extends SecuredNotChild> clazz, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting " + clazz.getSimpleName() + " with parent ACL identities..." );
        List<Long> identifiers = jdbcTemplate.queryForList(
                "select aoi.object_id_identity "
                        + "from acl_object_identity aoi "
                        + "join acl_class cls on aoi.object_id_class = cls.id "
                        + "where cls.class = ? and aoi.parent_object is not null",
                Long.class, clazz.getName() );
        if ( identifiers.isEmpty() ) {
            log.info( "No " + clazz.getSimpleName() + " have parent ACL identities; this is expected as it implements the SecuredNotChild interface." );
        } else {
            log.warn( "There are " + identifiers.size() + " " + clazz.getSimpleName() + " with parent ACL identities; this is not expected as it implements the SecuredNotChild interface." );
        }
        for ( Long identifier : identifiers ) {
            if ( config.isApplyFixes() ) {
                MutableAcl acl = ( MutableAcl ) aclService.readAclById( new AclObjectIdentity( clazz, identifier ) );
                acl.setParent( null );
                aclService.updateAcl( acl );
                String fixMessage = "Detached parent ACL identity.";
                log.info( formatEntity( clazz, identifier ) + ": " + fixMessage );
                results.add( new LintResult( clazz, identifier, fixMessage, true ) );
            } else {
                results.add( new LintResult( clazz, identifier, "Entity has a parent ACL identity, but it implements the SecuredNotChild interface.", false ) );
            }
        }
    }

    /**
     * Lint for securable entities that are explicitly not children, but have a parent object set in their ACL identity.
     * <p>
     * The fix is to detach them from their parent.
     * <p>
     * Phase 3 gsec HQL deprecation: single-id variant of the bulk method above. Same conversion
     * pattern: raw-SQL existence check, then route the fix through {@link AclService#updateAcl}.
     */
    private void lintSecuredNotChildWithParent( Class<? extends SecuredNotChild> clazz, Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        Integer hasParentCount = jdbcTemplate.queryForObject(
                "select count(*) "
                        + "from acl_object_identity aoi "
                        + "join acl_class cls on aoi.object_id_class = cls.id "
                        + "where cls.class = ? and aoi.object_id_identity = ? and aoi.parent_object is not null",
                Integer.class, clazz.getName(), identifier );
        if ( hasParentCount == null || hasParentCount == 0 ) {
            log.info( formatEntity( clazz, identifier ) + " has no parent ACL identity; this is expected as it implements the SecuredNotChild interface." );
            return;
        }
        if ( config.isApplyFixes() ) {
            MutableAcl acl = ( MutableAcl ) aclService.readAclById( new AclObjectIdentity( clazz, identifier ) );
            acl.setParent( null );
            aclService.updateAcl( acl );
            String fixMessage = "Detached parent ACL identity.";
            log.info( formatEntity( clazz, identifier ) + ": " + fixMessage );
            results.add( new LintResult( clazz, identifier, fixMessage, true ) );
        } else {
            results.add( new LintResult( clazz, identifier, "Entity has a parent ACL identity, but it implements the SecuredNotChild interface.", false ) );
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
        // Dialect-aware bitwise AND (MySQL: "ace.MASK & N", H2: "BITAND(ace.MASK, N)").
        String renderedMask = ubic.gemma.persistence.util.BitwiseUtils.bitand(
                ( (org.hibernate.engine.spi.SessionFactoryImplementor) sessionFactory ).getJdbcServices().getDialect(),
                "ace.mask", String.valueOf( permission.getMask() ) );
        @SuppressWarnings("rawtypes")
        NativeQuery query = sessionFactory.getCurrentSession()
                .createNativeQuery( "select cls.class, aoi.object_id_identity "
                        + "from acl_object_identity aoi "
                        + "join acl_class cls on aoi.object_id_class = cls.id "
                        + "where cls.class = :type "
                        + ( identifier != null ? " and aoi.object_id_identity = :identifier " : "" )
                        + "and aoi.id not in "
                        + "(select ace.acl_object_identity "
                        + "from acl_entry ace "
                        // aoi2 is only there to limit the size of the subquery
                        + "join acl_object_identity aoi2 on ace.acl_object_identity = aoi2.id "
                        + "join acl_class cls2 on aoi2.object_id_class = cls2.id "
                        + "join acl_sid sid on ace.sid = sid.id "
                        + "where cls2.class = :type "
                        + ( identifier != null ? " and aoi2.object_id_identity = :identifier " : "" )
                        + "and sid.sid = :grantedAuthority "
                        + "and sid.principal = 0 "
                        + "and ace.granting = :granting "
                        + "and " + renderedMask + " <> 0)" )
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
            Long identifier_ = ( ( Number ) row[1] ).longValue();
            if ( config.isApplyFixes() ) {
                MutableAcl acl = ( MutableAcl ) aclService.readAclById( new AclObjectIdentity( type, identifier_ ) );
                // Phase B of gsec absorption: use Spring's stock GrantedAuthoritySid. The previous
                // call constructed a gsec AclGrantedAuthoritySid (now no longer a Spring Sid) which
                // would fail the {@code AclImpl.insertAce} downcast path historically and silently
                // mismatch in JdbcMutableAclService's sid lookup (instanceof PrincipalSid /
                // GrantedAuthoritySid checks in createOrRetrieveSidPrimaryKey both returned false).
                // That made applyFixes a silent no-op for the inserted ACE.
                acl.insertAce( acl.getEntries().size(), permission,
                        new org.springframework.security.acls.domain.GrantedAuthoritySid( grantedAuthority ), granting );
                aclService.updateAcl( acl );
                // The legacy gsec AclDaoImpl.convert() used to mutate the managed AOI's entries
                // collection during update(), conflicting with JdbcMutableAclService's already-
                // persisted rows on the next Hibernate flush. Production is on JdbcMutableAclService
                // now, but we still clear the session here so any HQL-loaded AclObjectIdentity rows
                // (used elsewhere in the linter) don't fight a stale snapshot of acl_entry.
                sessionFactory.getCurrentSession().clear();
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
