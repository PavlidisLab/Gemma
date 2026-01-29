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
    private static final Map<Class<? extends SecuredChild<?>>, Class<? extends Securable>> securedChildToParentTypeMap = new HashMap<>();

    /**
     * A snippet of HQL that resolves the parent identifier for a given secured child.
     * <p>
     * The query has access to the following bindings: {@code aoi} (an {@link AclObjectIdentity}), {@code :type} and
     * {@code :parentType}.
     */
    private static final Map<Class<? extends SecuredChild<?>>, List<String>> securedChildToParentIdQueryMap = new HashMap<>();

    /**
     *
     * @param clazz              a secured child class
     * @param parentClazz        an expected parent class
     * @param childToParentIdHql optional HQL snippets to resolve the parent identifier from the child, a parent is
     *                           considered incorrect if any of the queries produce a mismatch. A query may produce an
     *                           empty result which will not be considered a mismatch.
     */
    private static void addSecuredChildToParent( Class<? extends SecuredChild<?>> clazz, Class<? extends Securable> parentClazz, @Nullable String... childToParentIdHql ) {
        securedChildToParentTypeMap.put( clazz, parentClazz );
        if ( childToParentIdHql != null ) {
            securedChildToParentIdQueryMap.put( clazz, Arrays.asList( childToParentIdHql ) );
        }
    }

    static {
        // FIXME: handle sub-assays and sub-biomaterials in the child to parent query, or recursively resolve parents as
        //        a special case
        // this cover cases where the BioAssay is attached to a EE or a subset of an EE
        addSecuredChildToParent( BioAssay.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.bioAssays ba where ba.id = aoi.identifier group by ee",
                //language=HQL
                "select eess.sourceExperiment.id from ExpressionExperimentSubSet eess join eess.bioAssays ba where ba.id = aoi.identifier group by eess.sourceExperiment" );
        addSecuredChildToParent( BioMaterial.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.bioAssays ba where ba.sampleUsed.id = aoi.identifier group by ee",
                //language=HQL
                "select eess.sourceExperiment.id from ExpressionExperimentSubSet eess join eess.bioAssays ba where ba.sampleUsed.id = aoi.identifier group by eess.sourceExperiment" );
        addSecuredChildToParent( ExpressionExperimentSubSet.class, ExpressionExperiment.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = aoi.identifier" );
        addSecuredChildToParent( MeanVarianceRelation.class, ExpressionExperiment.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = aoi.identifier" );
        addSecuredChildToParent( ExperimentalDesign.class, ExpressionExperiment.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = aoi.identifier" );
        addSecuredChildToParent( ExperimentalFactor.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.experimentalDesign ed join ed.experimentalFactors ef where ef.id = aoi.identifier group by ee" );
        addSecuredChildToParent( FactorValue.class, ExpressionExperiment.class,
                //language=HQL
                "select ee.id from ExpressionExperiment ee join ee.experimentalDesign ed join ed.experimentalFactors ef join ef.factorValues fv where fv.id = aoi.identifier group by ee" );
        addSecuredChildToParent( DifferentialExpressionAnalysis.class, ExpressionExperiment.class,
                //language=HQL
                "select coalesce(ea.sourceExperiment.id, ea.id) from DifferentialExpressionAnalysis dea join dea.experimentAnalyzed ea where dea.id = aoi.identifier" );
        // result set belong to the analysis which in turn belong to the EE
        addSecuredChildToParent( ExpressionAnalysisResultSet.class, DifferentialExpressionAnalysis.class,
                //language=HQL
                "select ears.analysis.id from ExpressionAnalysisResultSet ears where ears.id = aoi.identifier" );
        addSecuredChildToParent( SampleCoexpressionAnalysis.class, ExpressionExperiment.class,
                //language=HQL
                "select coalesce(ea.sourceExperiment.id, ea.id) from SampleCoexpressionAnalysis sca join sca.experimentAnalyzed ea where sca.id = aoi.identifier" );
        addSecuredChildToParent( CoexpressionAnalysis.class, ExpressionExperiment.class,
                //language=HQL
                "select coalesce(ea.sourceExperiment.id, ea.id) from CoexpressionAnalysis ca join ca.experimentAnalyzed ea where ca.id = aoi.identifier" );
        addSecuredChildToParent( PrincipalComponentAnalysis.class, ExpressionExperiment.class,
                //language=HQL
                "select coalesce(ea.sourceExperiment.id, ea.id) from PrincipalComponentAnalysis pca join pca.experimentAnalyzed ea where pca.id = aoi.identifier" );
    }

    @Autowired
    private AclService aclService;
    @Autowired
    private SessionFactory sessionFactory;
    @Autowired
    private ParentIdentityRetrievalStrategy parentIdentityRetrievalStrategy;

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
                } else {
                    lintSecuredChildWithoutParent( scc, config, results );
                }
            }
            if ( config.isLintChildWithIncorrectParent() && SecuredChild.class.isAssignableFrom( clazz ) ) {
                //noinspection unchecked
                Class<? extends SecuredChild<?>> scc = ( Class<? extends SecuredChild<?>> ) clazz;
                if ( securedChildToParentTypeMap.containsKey( scc ) ) {
                    if ( identifier != null ) {
                        lintSecuredChildWithIncorrectParent( scc, securedChildToParentTypeMap.get( scc ), securedChildToParentIdQueryMap.get( scc ), identifier, config, results );
                    } else {
                        lintSecuredChildWithIncorrectParent( scc, securedChildToParentTypeMap.get( scc ), securedChildToParentIdQueryMap.get( scc ), config, results );
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

    private void lintSecurableLackingObjectIdentity( Class<? extends Securable> clazz, Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        Boolean hasAoi = ( Boolean ) sessionFactory.getCurrentSession()
                .createQuery( "select count(*) > 0 from " + clazz.getName() + " e "
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
        int i = 0;
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
                if ( ( ++i % 100 ) == 0 ) {
                    // flush to prevent SecuredChild to pile up in memory
                    sessionFactory.getCurrentSession().flush();
                    sessionFactory.getCurrentSession().clear();
                }
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

    private void lintSecuredChildWithIncorrectParent( Class<? extends SecuredChild<?>> clazz, Class<? extends Securable> expectedParentClass, @Nullable List<String> expectedParentIdHqls, AclLinterConfig config, Collection<LintResult> results ) {
        log.info( "Linting " + clazz.getSimpleName() + " with incorrect parent ACL identities..." );
        //noinspection unchecked
        List<AclObjectIdentity> list = sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi join aoi.parentObject parentAoi "
                        + "where aoi.type = :type "
                        + "and (parentAoi.type <> :parentType"
                        + ( expectedParentIdHqls != null ? expectedParentIdHqls.stream().map( expectedParentIdHql -> " or parentAoi.identifier <> (" + expectedParentIdHql + ")" ).collect( Collectors.joining() ) : "" )
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
        int i = 0;
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
                if ( ( ++i % 100 ) == 0 ) {
                    // flush to prevent SecuredChild to pile up in memory
                    sessionFactory.getCurrentSession().flush();
                    sessionFactory.getCurrentSession().clear();
                }
            } else {
                results.add( new LintResult( clazz, aoi.getIdentifier(), "Entity does not have a correct parent ACL identity.", false ) );
            }
        }
    }

    private void lintSecuredChildWithIncorrectParent( Class<? extends SecuredChild<?>> clazz,
            Class<? extends Securable> expectedParentClass,
            @Nullable List<String> expectedParentIdHqls,
            Long identifier, AclLinterConfig config, Collection<LintResult> results ) {
        AclObjectIdentity aoi = ( AclObjectIdentity ) sessionFactory.getCurrentSession()
                .createQuery( "select aoi from AclObjectIdentity aoi join aoi.parentObject as parentAoi "
                        + "where aoi.identifier = :identifier and aoi.type = :type "
                        + "and (parentAoi.type <> :parentType"
                        + ( expectedParentIdHqls != null ? expectedParentIdHqls.stream().map( expectedParentIdHql -> " or parentAoi.identifier <> (" + expectedParentIdHql + ")" ).collect( Collectors.joining() ) : "" )
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
        List<Object[]> list = query.list();
        if ( list.isEmpty() ) {
            log.info( "All permissions are correct for " + formatEntity( clazz, identifier ) + "." );
        } else {
            log.warn( "There are " + list.size() + " permission issues for " + formatEntity( clazz, identifier ) + "." );
        }
        for ( Object[] row : list ) {
            String type = ( String ) row[0];
            Long identifier_ = ( ( BigInteger ) row[1] ).longValue();
            if ( config.isApplyFixes() ) {
                MutableAcl acl = ( MutableAcl ) aclService.readAclById( new AclObjectIdentity( type, identifier_ ) );
                acl.insertAce( acl.getEntries().size(), permission, new AclGrantedAuthoritySid( grantedAuthority ), granting );
                aclService.updateAcl( acl );
                String fixMessage = "Added missing permissions for " + grantedAuthority + ".";
                log.info( formatEntity( clazz, identifier_ ) + ": " + fixMessage );
                result.add( new LintResult( clazz, identifier_, fixMessage, true ) );
            } else {
                String problem = String.format( "Entity lacks an ACL entry with %s to %s%s.", permission,
                        grantedAuthority, granting ? " with granting" : "" );
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
