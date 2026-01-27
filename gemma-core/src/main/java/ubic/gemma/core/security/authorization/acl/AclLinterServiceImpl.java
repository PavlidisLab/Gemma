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
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.coexpression.SampleCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.MeanVarianceRelation;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

@Service
@CommonsLog
public class AclLinterServiceImpl implements AclLinterService {

    /**
     * A pre-defined mapping of all known secured child classes to their parent securable class.
     * FIXME: this should be part of the model metadata
     */
    private static final Map<Class<? extends SecuredChild<?>>, Class<? extends Securable>> securedChildToParentMap = new HashMap<>();

    static {
        securedChildToParentMap.put( ExpressionAnalysisResultSet.class, DifferentialExpressionAnalysis.class );
        securedChildToParentMap.put( BioAssay.class, ExpressionExperiment.class );
        securedChildToParentMap.put( BioMaterial.class, ExpressionExperiment.class );
        securedChildToParentMap.put( ExpressionExperimentSubSet.class, ExpressionExperiment.class );
        securedChildToParentMap.put( MeanVarianceRelation.class, ExpressionExperiment.class );
        securedChildToParentMap.put( ExperimentalDesign.class, ExpressionExperiment.class );
        securedChildToParentMap.put( ExperimentalFactor.class, ExpressionExperiment.class );
        securedChildToParentMap.put( FactorValue.class, ExpressionExperiment.class );
        securedChildToParentMap.put( DifferentialExpressionAnalysis.class, ExpressionExperiment.class );
        securedChildToParentMap.put( SampleCoexpressionAnalysis.class, ExpressionExperiment.class );
        securedChildToParentMap.put( CoexpressionAnalysis.class, ExpressionExperiment.class );
        securedChildToParentMap.put( PrincipalComponentAnalysis.class, ExpressionExperiment.class );
    }

    @Autowired
    private AclService aclService;
    @Autowired
    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;

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
                //noinspection unchecked
                Class<? extends SecuredChild<?>> scc = ( Class<? extends SecuredChild<?>> ) clazz;
                if ( identifier != null ) {
                    lintSecuredChildWithoutParent( scc, identifier, config, results );
                    if ( securedChildToParentMap.containsKey( scc ) ) {
                        lintSecuredChildWithIncorrectParent( scc, securedChildToParentMap.get( scc ), identifier, config, results );
                    }
                } else {
                    lintSecuredChildWithoutParent( scc, config, results );
                    if ( securedChildToParentMap.containsKey( scc ) ) {
                        lintSecuredChildWithIncorrectParent( scc, securedChildToParentMap.get( scc ), config, results );
                    }
                }
            }
            if ( config.isLintChildWithIncorrectParent() && SecuredChild.class.isAssignableFrom( clazz ) ) {
                //noinspection unchecked
                Class<? extends SecuredChild<?>> scc = ( Class<? extends SecuredChild<?>> ) clazz;
                if ( securedChildToParentMap.containsKey( scc ) ) {
                    if ( identifier != null ) {
                        lintSecuredChildWithIncorrectParent( scc, securedChildToParentMap.get( scc ), identifier, config, results );
                    } else {
                        lintSecuredChildWithIncorrectParent( scc, securedChildToParentMap.get( scc ), config, results );
                    }
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
            results.add( new LintResult( s.getClass(), s.getId(), s + " lacks an ACL identity.", true ) );
        } else {
            results.add( new LintResult( s.getClass(), s.getId(), s + " lacks an ACL identity.", false ) );
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
                boolean fixed = fixMissingOrIncorrectParent( clazz, aoi );
                results.add( new LintResult( clazz, aoi.getIdentifier(), aoi.getType() + " Id=" + aoi.getIdentifier() + " has no parent ACL identity.", fixed ) );
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
            boolean fixed = fixMissingOrIncorrectParent( clazz, aoi );
            results.add( new LintResult( clazz, aoi.getIdentifier(), s + " has no parent ACL identity.", fixed ) );
        } else {
            results.add( new LintResult( clazz, aoi.getIdentifier(), s + " has no parent ACL identity.", false ) );
        }
    }

    /**
     * TODO: also lint for incorrect parent ID
     *
     * @param clazz
     * @param expectedParentClass
     * @param config
     * @param results
     */
    private void lintSecuredChildWithIncorrectParent( Class<? extends SecuredChild<?>> clazz, Class<? extends Securable> expectedParentClass, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting " + clazz.getSimpleName() + " with incorrect parent ACL identities..." );
        //noinspection unchecked
        List<AclObjectIdentity> list = sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi join aoi.parentObject parentAoi "
                        + "where aoi.type = :type "
                        + "and parentAoi.type <> :parentType" )
                .setParameter( "type", clazz.getName() )
                .setParameter( "parentType", expectedParentClass.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .list();
        if ( list.isEmpty() ) {
            log.info( "All " + clazz.getSimpleName() + " have parent ACL identities." );
        } else {
            log.warn( "There are " + list.size() + " " + clazz.getSimpleName() + " lacking parent ACL identities." );
        }
        for ( AclObjectIdentity aoi : list ) {
            String p = String.format( "%s Id=%d does not have a parent ACL identity of type %s.", aoi.getType(),
                    aoi.getIdentifier(), expectedParentClass.getSimpleName() );
            if ( config.isApplyFixes() ) {
                boolean fixed = fixMissingOrIncorrectParent( clazz, aoi );
                results.add( new LintResult( clazz, aoi.getIdentifier(), p, fixed ) );
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), p, false ) );
            }
        }
    }

    private void lintSecuredChildWithIncorrectParent( Class<? extends SecuredChild<?>> clazz, Class<? extends Securable> expectedParentClass, Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        AclObjectIdentity aoi = ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi join aoi.parentObject as parentAoi "
                        + "where aoi.identifier = :identifier and aoi.type = :type "
                        + "and parentAoi.type <> :parentType" )
                .setParameter( "identifier", identifier )
                .setParameter( "type", clazz.getName() )
                .setParameter( "parentType", expectedParentClass.getName() )
                .setReadOnly( !config.isApplyFixes() )
                .uniqueResult();
        String s = clazz.getSimpleName() + " Id=" + identifier;
        if ( aoi == null ) {
            log.info( s + " has a parent ACL identity." );
            return;
        }
        if ( config.isApplyFixes() ) {
            boolean fixed = fixMissingOrIncorrectParent( clazz, aoi );
            results.add( new LintResult( clazz, aoi.getId(), aoi.getType() + " Id=" + aoi.getIdentifier() + " has no parent ACL identity.", fixed ) );
        } else {
            results.add( new LintResult( clazz, aoi.getIdentifier(), s + " does not have a parent ACL identity of type " + expectedParentClass.getSimpleName() + ".", false ) );
        }
    }

    private boolean fixMissingOrIncorrectParent( Class<? extends SecuredChild<?>> clazz, AclObjectIdentity aoi ) {
        Class<? extends Securable> parentType;
        Long parentIdentifier;
        if ( ExperimentalFactor.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            ExperimentalFactor factor = ( ExperimentalFactor ) sessionFactory.getCurrentSession()
                    .get( ExperimentalFactor.class, aoi.getIdentifier() );
            ExpressionExperiment ee = expressionExperimentService.findByFactor( factor );
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( FactorValue.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            FactorValue factor = ( FactorValue ) sessionFactory.getCurrentSession()
                    .get( FactorValue.class, aoi.getIdentifier() );
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( factor );
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( BioAssay.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            BioAssay ba = ( BioAssay ) sessionFactory.getCurrentSession()
                    .get( BioAssay.class, aoi.getIdentifier() );
            ExpressionExperiment ee = expressionExperimentService.findByBioAssay( ba );
            parentIdentifier = ee != null ? ee.getId() : null;
        } else if ( BioMaterial.class.isAssignableFrom( clazz ) ) {
            parentType = ExpressionExperiment.class;
            BioMaterial bm = ( BioMaterial ) sessionFactory.getCurrentSession()
                    .get( BioMaterial.class, aoi.getIdentifier() );
            Collection<ExpressionExperiment> ees = expressionExperimentService.findByBioMaterial( bm );
            if ( ees.size() == 1 ) {
                parentIdentifier = ees.iterator().next().getId();
            } else if ( ees.size() > 1 ) {
                log.warn( "More than one ExpressionExperiment refer to " + bm + ", cannot pick its parent ACL identity." );
                parentIdentifier = null;
            } else {
                parentIdentifier = null;
            }
        } else if ( MeanVarianceRelation.class.isAssignableFrom( clazz ) ) {
            MeanVarianceRelation mvr = ( MeanVarianceRelation ) sessionFactory.getCurrentSession()
                    .get( MeanVarianceRelation.class, aoi.getIdentifier() );
            ExpressionExperiment ee = expressionExperimentService.findByMeanVarianceRelation( mvr );
            parentType = ExpressionExperiment.class;
            parentIdentifier = ee != null ? ee.getId() : null;
        } else {
            // try automated!
            SecuredChild<?> sc = ( SecuredChild<?> ) sessionFactory.getCurrentSession()
                    .get( clazz, aoi.getIdentifier() );
            if ( sc.getSecurityOwner() != null ) {
                parentType = sc.getSecurityOwner().getClass();
                parentIdentifier = sc.getSecurityOwner().getId();
            } else {
                log.warn( "Cannot resolve parent ACL identity for " + aoi.getType() + "." );
                parentType = null;
                parentIdentifier = null;
            }
        }

        if ( parentIdentifier != null ) {
            AclObjectIdentity parentAoi = ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                    .createQuery( "select aoi from AclObjectIdentity aoi where aoi.type = :type and aoi.identifier = :identifier" )
                    .setParameter( "type", parentType )
                    .setParameter( "identifier", parentIdentifier )
                    .uniqueResult();
            if ( parentAoi != null ) {
                aoi.setParentObject( parentAoi );
                return true;
            } else {
                log.warn( "Could not resolve ACL identity for " + parentType.getSimpleName() + " Id=" + parentIdentifier + "." );
                return false;
            }
        } else {
            return false;
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
