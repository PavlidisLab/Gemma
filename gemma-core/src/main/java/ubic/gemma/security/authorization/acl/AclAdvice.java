/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.security.authorization.acl;

import java.beans.PropertyDescriptor;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.hibernate.Hibernate;
import org.hibernate.LazyInitializationException;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.engine.CascadeStyle;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.ObjectIdentityRetrievalStrategyImpl;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.acls.model.AccessControlEntry;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.AuditableAcl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.MutableAclService;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.security.acls.model.Permission;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.coexpression.ProbeCoexpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.SecuredChild;
import ubic.gemma.model.common.auditAndSecurity.SecuredNotChild;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.CrudUtils;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.audit.AuditAdvice;
import ubic.gemma.util.AuthorityConstants;
import ubic.gemma.util.ReflectionUtil;

/**
 * Adds security controls to newly created objects, and removes them for objects that are deleted. Methods in this
 * interceptor are run for all new objects (to add security if needed) and when objects are deleted. This is not used to
 * modify permissions on existing objects.
 * <p>
 * Implementation Note: For permissions modification to be triggered, the method name must match certain patterns, which
 * include "create", or "remove". These patterns are defined in the {@link AclPointcut}. Other methods that would
 * require changes to permissions will not work without modifying the source code.
 * 
 * @author keshav
 * @author pavlidis
 * @version $Id$
 * @see ubic.gemma.security.authorization.acl.AclPointcut
 */
@Repository
public class AclAdvice extends HibernateDaoSupport {

    private static Log log = LogFactory.getLog( AclAdvice.class );

    @Autowired
    private MutableAclService aclService;

    @Autowired
    private CrudUtils crudUtils;

    private ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy = new ObjectIdentityRetrievalStrategyImpl();

    private final TransactionTemplate transactionTemplate;

    @Autowired
    public AclAdvice( SessionFactory sessionFactory, PlatformTransactionManager transactionManager ) {
        transactionTemplate = new TransactionTemplate( transactionManager );
        super.setSessionFactory( sessionFactory );
    }

    /**
     * @param jp
     * @param retValue
     * @throws Throwable
     */
    public void doAclAdvice( JoinPoint jp, Object retValue ) throws Throwable {

        final Object[] args = jp.getArgs();
        Signature signature = jp.getSignature();
        final String methodName = signature.getName();

        assert args != null;
        final Object persistentObject = getPersistentObject( retValue, methodName, args );

        if ( persistentObject == null ) return;

        final boolean isUpdate = CrudUtils.methodIsUpdate( methodName );
        final boolean isDelete = CrudUtils.methodIsDelete( methodName );

        // Case 1: collection of securables.
        if ( Collection.class.isAssignableFrom( persistentObject.getClass() ) ) {
            for ( final Object o : ( Collection<?> ) persistentObject ) {
                if ( !isEligibleForAcl( o ) ) {
                    continue; // possibly could return, if we assume collection is homogenous in type.
                }
                process( o, methodName, isUpdate, isDelete );
            }
        } else {
            // Case 2: single securable
            if ( !isEligibleForAcl( persistentObject ) ) {
                return;
            }
            process( persistentObject, methodName, isUpdate, isDelete );
        }

    }

    public void setAclService( MutableAclService mutableAclService ) {
        this.aclService = mutableAclService;
    }

    /**
     * @param crudUtils the crudUtils to set
     */
    public void setCrudUtils( CrudUtils crudUtils ) {
        this.crudUtils = crudUtils;
    }

    // TODO: Work in progress. This is a refactoring of addOrUpdateAcl logic. I'm keeping old one for now but it's
    // getting too messy.
    // private boolean isObjectPublicWhenCreatedByAdmin( Object object ) {
    // return !( User.class.isAssignableFrom( object.getClass() ) || UserGroup.class.isAssignableFrom( object
    // .getClass() ) );
    // }
    //
    /*   
       * 
       * 
       * 
       */
    // private AuditableAcl addAcl (Securable object, ObjectIdentity oi, Acl parentAcl) {
    // // Assumption: doesn't exist
    // AuditableAcl acl = null;
    // Sid sid = null;
    //        
    // // FIXME: better definition
    // // Object is TOP_PARENT (ROOT of permissions inheritance tree)
    // boolean isTopParent = ;
    //        
    // /*
    // * The logic here is: if we're supposed to inherit from the parent, but none is provided (can easily happen), we
    // * have to put in ACEs. Same goes if we're not supposed to inherit. Objects which are not supposed to have their
    // * own ACLs (SecurableChild)
    // */
    // if ( isTopParent ) {
    //
    // /*
    // * All top level objects must have administration GROUP_ADMIN permissions.
    // */
    // if ( log.isDebugEnabled() ) log.debug( "Making administratable by GROUP_ADMIN: " + oi );
    // grant( acl, BasePermission.ADMINISTRATION, new GrantedAuthoritySid( new GrantedAuthorityImpl(
    // AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) );
    //
    // /*
    // * GROUP_AGENT can read anything.
    // */
    // if ( log.isDebugEnabled() ) log.debug( "Making readable by GROUP_AGENT: " + oi );
    // grant( acl, BasePermission.READ, new GrantedAuthoritySid( new GrantedAuthorityImpl(
    // AuthorityConstants.AGENT_GROUP_AUTHORITY ) ) );
    //
    // /*
    // * If object is created by admin and the object is not a user or group, make it readable by anonymous (public).
    // */
    // if ( SecurityService.isUserAdmin() & isObjectPublicWhenCreatedByAdmin ( object ) ) {
    // if ( log.isDebugEnabled() ) log.debug( "Making readable by IS_AUTHENTICATED_ANONYMOUSLY: " + oi );
    // grant( acl, BasePermission.READ, new GrantedAuthoritySid( new GrantedAuthorityImpl(
    // AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) ) );
    // }
    //
    // /*
    // * The user who created the object can read/write it, if admin then skip since admin has all permission already
    // anyway.
    // */
    // if ( SecurityService.isUserLoggedIn() && !SecurityService.isUserAdmin() ) {
    // if ( log.isDebugEnabled() ) log.debug( "Giving read/write permissions on " + oi + " to " + sid );
    // grant( acl, BasePermission.READ, sid );
    // grant( acl, BasePermission.WRITE, sid );
    // }
    // }
    //
    // //
    // // SPECIAL CASES
    // //
    //        
    // /*
    // * NEW USER CREATION AT REGISTRATION
    // * Normal case: We expect anonymous user running with GROUP_RUN_AS_ADMIN privileges.
    // */
    // if ( SecurityService.isRunningAsAdmin() && User.class.isAssignableFrom( object.getClass() ) ) {
    // User u = ( User ) object;
    // sid = new PrincipalSid( u.getUserName() );
    //
    // grant( acl, BasePermission.READ, sid );
    // grant( acl, BasePermission.WRITE, sid );
    //
    // if ( log.isDebugEnabled() ) log.debug( "New User: given read/write permissions on " + oi + " to " + sid );
    // }
    //
    // // Treating Analyses as special case. It'll inherit ACL from ExpressionExperiment
    // // If aclParent is passed to this method we overwrite it.
    // if ( DifferentialExpressionAnalysis.class.isAssignableFrom( object.getClass() ) ) {
    //
    // DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) object;
    // ExpressionExperimentSet eeSet = dea.getExpressionExperimentSetAnalyzed();
    // Collection<BioAssaySet> experiments = eeSet.getExperiments();
    //
    // if ( experiments.size() != 1 )
    // throw new RuntimeException( "We do not support Diff. Expr. Analyses based on multiple datasets." );
    //
    // BioAssaySet e = experiments.iterator().next();
    // ObjectIdentity oi_temp = makeObjectIdentity( e );
    //
    // parentAcl = aclService.readAclById( oi_temp );
    // acl.setEntriesInheriting( true );
    // }
    //                
    // acl.setEntriesInheriting( inheritParent );
    // acl.setOwner( sid );
    //
    // assert !acl.equals( parentAcl );
    //
    // if ( parentAcl != null ) {
    // if ( log.isTraceEnabled() ) log.trace( "Setting parent to: " + parentAcl + " <--- " + acl );
    // acl.setParent( parentAcl );
    // }
    //
    // return ( AuditableAcl ) aclService.updateAcl( acl );
    //        
    // }
    //
    // private AuditableAcl updateAcl( AuditableAcl acl, Securable object, Acl parentAcl ) {
    // // Assumption: already exists
    //
    // /*
    // * Could be findOrCreate, or could be a second pass that will let us fill in parent ACLs for associated objects
    // * missed earlier in a persist cycle. E.g. BioMaterial
    // */
    // try {
    // maybeSetParentACL( object, acl, parentAcl );
    // return acl;
    // } catch ( NotFoundException nfe ) {
    // log.error( nfe, nfe );
    // }
    //
    // return null;
    // }

    /**
     * Creates the acl_permission object and the acl_object_identity object.
     * 
     * @param object The domain object.
     * @return true if an ACL was created, false otherwise.
     */
    private AuditableAcl addOrUpdateAcl( Securable object, Acl parentAcl ) {
        // Flow
        // exists vs doesn't exist
        // updateAcl addAcl

        // inherits from parent vs doesn't inherit
        //
        // Special handling under some privileges
        // Special handling for some classes
        // (default permissions rules)
        // Root (top_parent) of acl inheritance tree
        // Enforce SecuredChild, SecuredNotChild rules

        // FIXME: Maybe getObjectIdentity? since the method is addOrUpdate object identity may already exist
        // and this method doesn't create a new one.
        ObjectIdentity oi = makeObjectIdentity( object );

        // FIXME: Maybe through exception? since there's no way to recover from this.
        if ( oi == null ) {
            log.warn( "Null object identity for : " + object );
            return null;
        }

        AuditableAcl acl = null;

        boolean exists = false;
        try {
            acl = ( AuditableAcl ) aclService.readAclById( oi ); // throws exception if not found
            exists = true;
        } catch ( NotFoundException nfe ) {
            acl = ( AuditableAcl ) aclService.createAcl( oi );
        }

        if ( exists ) {
            /*
             * Could be findOrCreate, or could be a second pass that will let us fill in parent ACLs for associated
             * objects missed earlier in a persist cycle. E.g. BioMaterial
             */
            try {
                maybeSetParentACL( object, acl, parentAcl );
                return acl;
            } catch ( NotFoundException nfe ) {
                log.error( nfe, nfe );
            }
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if ( authentication == null ) {
            throw new IllegalStateException( "No authentication found in the security context" );
        }

        Object p = authentication.getPrincipal();

        if ( p == null ) {
            throw new IllegalStateException( "Principal was null for " + authentication );
        }

        Sid sid = new PrincipalSid( p.toString() );

        boolean isAdmin = SecurityService.isUserAdmin();

        boolean isRunningAsAdmin = SecurityService.isRunningAsAdmin();

        boolean isAnonymous = SecurityService.isUserAnonymous();

        boolean objectIsAUser = User.class.isAssignableFrom( object.getClass() );

        boolean objectIsAGroup = UserGroup.class.isAssignableFrom( object.getClass() );

        /*
         * The only case where we absolutely disallow inheritance is for SecuredNotChild.
         */
        boolean inheritFromParent = parentAcl != null && !SecuredNotChild.class.isAssignableFrom( object.getClass() );

        boolean missingParent = parentAcl == null & SecuredChild.class.isAssignableFrom( object.getClass() );

        if ( missingParent ) {
            // This easily happens, it's not a problem as we go back through to recheck objects.
            log.debug( "Object should have a parent during ACL setup: " + object );
        }

        acl.setEntriesInheriting( inheritFromParent );

        /*
         * The logic here is: if we're supposed to inherit from the parent, but none is provided (can easily happen), we
         * have to put in ACEs. Same goes if we're not supposed to inherit. Objects which are not supposed to have their
         * own ACLs (SecurableChild)
         */
        if ( !inheritFromParent || parentAcl == null ) {

            /*
             * All objects must have administration permissions on them.
             */
            if ( log.isDebugEnabled() ) log.debug( "Making administratable by GROUP_ADMIN: " + oi );
            grant( acl, BasePermission.ADMINISTRATION, new GrantedAuthoritySid( new GrantedAuthorityImpl(
                    AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) );

            /*
             * Let agent read anything
             */
            if ( log.isDebugEnabled() ) log.debug( "Making readable by GROUP_AGENT: " + oi );
            grant( acl, BasePermission.READ, new GrantedAuthoritySid( new GrantedAuthorityImpl(
                    AuthorityConstants.AGENT_GROUP_AUTHORITY ) ) );

            /*
             * If admin, and the object is not a user or group, make it readable by anonymous.
             */
            boolean makeAnonymousReadable = isAdmin && !objectIsAUser && !objectIsAGroup;

            if ( makeAnonymousReadable ) {
                if ( log.isDebugEnabled() ) log.debug( "Making readable by IS_AUTHENTICATED_ANONYMOUSLY: " + oi );
                grant( acl, BasePermission.READ, new GrantedAuthoritySid( new GrantedAuthorityImpl(
                        AuthorityConstants.IS_AUTHENTICATED_ANONYMOUSLY ) ) );
            }

            /*
             * Don't add more permissions for the administrator. But whatever it is, the person who created it can
             * read/write it. User will only be anonymous if they are registering (AFAIK)
             */
            if ( !isAdmin && !isAnonymous ) {

                if ( log.isDebugEnabled() ) log.debug( "Giving read/write permissions on " + oi + " to " + sid );
                grant( acl, BasePermission.READ, sid );

                /*
                 * User who created something can edit it.
                 */
                grant( acl, BasePermission.WRITE, sid );

            }
        }

        /*
         * If the object is a user, make sure that user gets permissions even if the current user is not the same! In
         * fact, user creation runs with GROUP_RUN_AS_ADMIN privileges.
         */

        if ( objectIsAUser ) {
            User u = ( User ) object;
            if ( ( ( PrincipalSid ) sid ).getPrincipal().equals( u.getUserName() ) ) {
                /*
                 * This case should actually never happen. "we" are the user who is creating this user. We've already
                 * adding the READ/WRITE permissions above.
                 */
                log.warn( "Somehow...a user created themselves: " + oi );

            } else {

                if ( log.isDebugEnabled() )
                    log.debug( "New User: given read/write permissions on " + oi + " to " + sid );

                if ( isRunningAsAdmin ) {
                    /*
                     * Important: we expect this to normally be the case.
                     */
                    sid = new PrincipalSid( u.getUserName() );
                }

                /*
                 * See org.springframework.security.acls.domain.AclAuthorizationStrategy.
                 */
                grant( acl, BasePermission.READ, sid );
                grant( acl, BasePermission.WRITE, sid );

            }
        }

        // Treating Analyses as special case. It'll inherit ACL from ExpressionExperiment
        // If aclParent is passed to this method we overwrite it.
        if ( DifferentialExpressionAnalysis.class.isAssignableFrom( object.getClass() ) ) {

            DifferentialExpressionAnalysis dea = ( DifferentialExpressionAnalysis ) object;

            BioAssaySet e = dea.getExperimentAnalyzed();
            ObjectIdentity oi_temp = makeObjectIdentity( e );
            acl.setEntriesInheriting( true );
            parentAcl = aclService.readAclById( oi_temp );
            // Owner of the experiment owns analyses even if administrator ran them.
            sid = parentAcl.getOwner();
        } else if ( ProbeCoexpressionAnalysis.class.isAssignableFrom( object.getClass() ) ) {

            ProbeCoexpressionAnalysis pcea = ( ProbeCoexpressionAnalysis ) object;

            BioAssaySet e = pcea.getExperimentAnalyzed();
            ObjectIdentity oi_temp = makeObjectIdentity( e );
            acl.setEntriesInheriting( true );
            parentAcl = aclService.readAclById( oi_temp );
            // Owner of the experiment owns analyses even if administrator ran them.
            sid = parentAcl.getOwner();
        }

        acl.setOwner( sid ); // this might be the 'user' now.

        assert !acl.equals( parentAcl );

        if ( parentAcl != null ) {
            if ( log.isTraceEnabled() ) log.trace( "Setting parent to: " + parentAcl + " <--- " + acl );
            acl.setParent( parentAcl );
        }

        return ( AuditableAcl ) aclService.updateAcl( acl );

    }

    /**
     * Check for special cases of objects that don't need to be examined.
     * 
     * @param object
     * @return
     */
    private boolean canSkipAclCheck( Object object ) {
        return AuditTrail.class.isAssignableFrom( object.getClass() );
    }

    /**
     * Check if the association may be skipped.
     * 
     * @param object
     * @param propertyName
     * @return
     */
    private boolean canSkipAssociationCheck( Object object, String propertyName ) {

        /*
         * If this is an expression experiment, don't go down the data vectors - it has no securable associations and
         * would be expensive to traverse.F
         */
        if ( ExpressionExperiment.class.isAssignableFrom( object.getClass() )
                && ( propertyName.equals( "rawExpressionDataVectors" ) || propertyName
                        .equals( "processedExpressionDataVectors" ) ) ) {
            log.trace( "Skipping vectors" );
            return true;
        }

        /*
         * Array design has some non (directly) securable associations that would be expensive to load
         */
        if ( ArrayDesign.class.isAssignableFrom( object.getClass() )
                && ( propertyName.equals( "compositeSequences" ) || propertyName.equals( "reporters" ) ) ) {
            log.trace( "Skipping probes" );
            return true;
        }

        return false;
    }

    /**
     * Determine which ACL is going to be the parent.
     * 
     * @param object
     * @param previousParent
     * @return
     */
    private Acl chooseParent( Object object, Acl previousParent ) {
        Acl parentAcl;
        if ( previousParent == null && Securable.class.isAssignableFrom( object.getClass() )
                && !SecuredChild.class.isAssignableFrom( object.getClass() ) ) {

            parentAcl = getAcl( ( Securable ) object );

        } else {
            /*
             * Keep the previous parent. This means we 'pass through' and the parent is basically going to be the
             * top-most object: there isn't a hierarchy of parenthood. This also means that the parent might be kept as
             * null.
             */
            parentAcl = previousParent;

        }
        return parentAcl;
    }

    /**
     * Delete acl permissions for an object.
     * 
     * @param object
     * @throws IllegalArgumentException
     * @throws DataAccessException
     */
    private void deleteAcl( Securable object ) throws DataAccessException, IllegalArgumentException {
        ObjectIdentity oi = makeObjectIdentity( object );

        if ( oi == null ) {
            log.warn( "Null object identity for : " + object );
        }

        if ( log.isDebugEnabled() ) {
            log.debug( "Deleting ACL for " + object );
        }

        /*
         * This deletes children with the second parameter = true.
         */
        this.aclService.deleteAcl( oi, true );
    }

    /**
     * @param retValue
     * @param m
     * @param args
     * @return
     */
    private Object getPersistentObject( Object retValue, String methodName, Object[] args ) {
        if ( CrudUtils.methodIsDelete( methodName ) || CrudUtils.methodIsUpdate( methodName ) ) {

            /*
             * Only deal with single-argument update methods.
             */
            if ( args.length > 1 ) return null;

            assert args.length > 0;
            return args[0];
        }
        return retValue;
    }

    /**
     * Add ACE granting permission to sid to ACL (does not persist the change, you have to call update!)
     * 
     * @param acl which object
     * @param permission which permission
     * @param sid which principal
     */
    private void grant( AuditableAcl acl, Permission permission, Sid sid ) {
        acl.insertAce( acl.getEntries().size(), permission, sid, true );

        /*
         * This is a problem if the object is created by a regular user. Only admins can set auditing on objects.
         */

        // acl.updateAuditing( acl.getEntries().size() - 1, true, true );
    }

    /**
     * @param class1
     * @return
     */
    private boolean isEligibleForAcl( Object c ) {

        if ( c == null ) return false;

        if ( Securable.class.isAssignableFrom( c.getClass() ) ) {
            return true;
        }

        return false;
    }

    /**
     * Forms the object identity to be inserted in acl_object_identity table.
     * 
     * @param object
     * @return object identity.
     */
    private ObjectIdentity makeObjectIdentity( Securable object ) {

        if ( object.getId() == null ) {
            return null;
        }

        return objectIdentityRetrievalStrategy.getObjectIdentity( object );
    }

    /**
     * When setting the parent, we check to see if we can delete the ACEs on the 'child', if any. This is because we
     * want permissions to be managed by the parent. Check that the ACEs on the child are exactly equivalent to the ones
     * on the parent.
     * 
     * @param parentAcl -- careful with the order!
     * @param object
     * @param acl
     * @param true if ACEs were cleared.
     */
    private boolean maybeClearACEsOnChild( Securable object, MutableAcl childAcl, Acl parentAcl ) {
        int aceCount = childAcl.getEntries().size();

        if ( aceCount == 0 ) {

            if ( parentAcl.getEntries().size() == 0 ) {
                throw new IllegalStateException( "Either the child or the parent has to have ACEs" );
            }
            return false;
        }

        if ( parentAcl.getEntries().size() == aceCount ) {

            boolean oktoClearACEs = true;

            // check for exact match of all ACEs
            for ( AccessControlEntry ace : parentAcl.getEntries() ) {
                boolean found = false;
                for ( AccessControlEntry childAce : childAcl.getEntries() ) {
                    if ( childAce.getPermission().equals( ace.getPermission() )
                            && childAce.getSid().equals( ace.getSid() ) ) {
                        found = true;
                        break;
                    }
                }

                if ( !found ) {
                    oktoClearACEs = false;
                    break;
                }
            }

            if ( oktoClearACEs ) {
                if ( log.isTraceEnabled() ) log.trace( "Erasing ACEs from child " + object );

                while ( childAcl.getEntries().size() > 0 ) {
                    childAcl.deleteAce( 0 );
                }

                return true;
            }

        }
        return false;
    }

    /**
     * This is used when rechecking objects that are detached from a parent. Typically these are {@link SecuredChild}ren
     * like BioAssays.
     * <p>
     * Be careful with the argument order!
     * 
     * @param object
     * @param acl - the potential child
     * @param parentAcl - the potential parent
     * @return the parentAcl (can be null)
     */
    private Acl maybeSetParentACL( final Securable object, MutableAcl childAcl, final Acl parentAcl ) {
        if ( parentAcl != null && !SecuredNotChild.class.isAssignableFrom( object.getClass() ) ) {

            Acl currentParentAcl = childAcl.getParentAcl();

            if ( currentParentAcl != null && !currentParentAcl.equals( parentAcl ) ) {
                throw new IllegalStateException(
                        "Cannot change parentAcl from  non-null ACL to another or null: Current parent:"
                                + currentParentAcl + " != Proposed parent:" + parentAcl );
            }

            boolean changed = false;
            if ( currentParentAcl == null ) {
                childAcl.setParent( parentAcl );
                childAcl.setEntriesInheriting( true );
                changed = true;
            }

            changed = changed || maybeClearACEsOnChild( object, childAcl, parentAcl );
            if ( changed ) {
                aclService.updateAcl( childAcl );
            }
        }
        return childAcl.getParentAcl();
    }

    /**
     * Do necessary ACL operations on the object.
     * 
     * @param o
     * @param methodName
     * @param isUpdate
     * @param isDelete
     */
    private void process( final Object o, final String methodName, final boolean isUpdate, final boolean isDelete ) {
        transactionTemplate.execute( new TransactionCallbackWithoutResult() {

            @SuppressWarnings("synthetic-access")
            @Override
            protected void doInTransactionWithoutResult( TransactionStatus status ) {
                if ( log.isTraceEnabled() ) log.trace( "***********  Start ACL *************" );

                Securable s = ( Securable ) o;

                assert s != null;

                Session session = getSessionFactory().getCurrentSession();

                /*
                 * See AuditAdvice.process for a discussion of how difficult this is.
                 */
                if ( !CrudUtils.methodIsDelete( methodName ) ) {
                    session.lock( s, LockMode.NONE );
                }

                Hibernate.initialize( s );

                if ( isUpdate ) {
                    startUpdate( methodName, s );
                } else if ( isDelete ) {
                    deleteAcl( s );
                } else {
                    startCreate( methodName, s );
                }

                if ( log.isTraceEnabled() ) log.trace( "*========* End ACL *=========*" );
            }
        } );
    }

    /**
     * Walk the tree of associations and add (or update) acls.
     * 
     * @param methodName method name
     * @param object
     * @param previousParent The parent ACL of the given object (if it is a Securable) or of the last visited Securable.
     * @see AuditAdvice for similar code for Auditing
     */
    @SuppressWarnings("unchecked")
    private void processAssociations( String methodName, Object object, Acl previousParent ) {

        if ( canSkipAclCheck( object ) ) {
            return;
        }

        EntityPersister persister = crudUtils.getEntityPersister( object );
        if ( persister == null ) {
            log.error( "No Entity Persister found for " + object.getClass().getName() );
            return;
        }
        CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
        String[] propertyNames = persister.getPropertyNames();

        Acl parentAcl = chooseParent( object, previousParent );

        for ( int j = 0; j < propertyNames.length; j++ ) {

            CascadeStyle cs = cascadeStyles[j];
            String propertyName = propertyNames[j];

            if ( ( canSkipAssociationCheck( object, propertyName ) || !crudUtils.needCascade( methodName, cs ) )
                    && !specialCaseForAssociationFollow( object, propertyName ) ) {
                continue;
            }

            PropertyDescriptor descriptor = BeanUtils.getPropertyDescriptor( object.getClass(), propertyName );

            Object associatedObject = null;
            try {
                associatedObject = ReflectionUtil.getProperty( object, descriptor );
            } catch ( Exception e ) {
                log.error( "Error while processing: " + object.getClass() + " --> " + propertyName );
                throw ( new RuntimeException( e ) );
            }

            if ( associatedObject == null ) continue;

            Class<?> propertyType = descriptor.getPropertyType();

            if ( associatedObject instanceof Collection ) {
                Collection<Object> associatedObjects = ( Collection<Object> ) associatedObject;

                try {
                    for ( Object object2 : associatedObjects ) {

                        if ( Securable.class.isAssignableFrom( object2.getClass() ) ) {
                            addOrUpdateAcl( ( Securable ) object2, parentAcl );
                        }
                        processAssociations( methodName, object2, parentAcl );
                    }
                } catch ( LazyInitializationException ok ) {
                    /*
                     * This is not a problem. If this was reached via a create, the associated objects must not be new
                     * so they should already have acls.
                     */
                }

            } else {

                if ( Securable.class.isAssignableFrom( propertyType ) ) {
                    addOrUpdateAcl( ( Securable ) associatedObject, parentAcl );
                }
                processAssociations( methodName, associatedObject, parentAcl );
            }
        }
    }

    /**
     * For cases where don't have a cascade but the other end is securable, so we <em>must</em> check the association.
     * <p>
     * Implementation note. This is kind of inelegant, but the alternative is to check _every_ association, which will
     * often not be reachable.
     * 
     * @param object we are checking
     * @param property of the object
     * @return true if the association should be followed (even though it might not be based on cascade status)
     * @see AuditAdvice for similar code for Auditing
     */
    private boolean specialCaseForAssociationFollow( Object object, String property ) {

        if ( BioAssay.class.isAssignableFrom( object.getClass() ) && property.equals( "samplesUsed" ) ) {
            return true;
        }

        return false;

    }

    /**
     * @param methodName
     * @param s
     */
    private void startCreate( String methodName, Securable s ) {

        /*
         * Note that if the method is findOrCreate, we'll return quickly.
         */

        ObjectIdentity oi = makeObjectIdentity( s );

        if ( oi == null ) {
            throw new IllegalStateException(
                    "On 'create' methods, object should have a valid objectIdentity available. Method=" + methodName
                            + " on " + s );
        }

        addOrUpdateAcl( s, null );

        processAssociations( methodName, s, null );
    }

    /**
     * Kick off an update. This is executed when we call fooService.update(s). The basic issue is to add permissions for
     * any <em>new</em> associated objects.
     * 
     * @param m the update method
     * @param s the securable being updated.
     */
    private void startUpdate( String m, Securable s ) {

        ObjectIdentity oi = makeObjectIdentity( s );

        if ( oi == null ) {
            throw new IllegalStateException(
                    "On 'update' methods, object should have a valid objectIdentity available. Method=" + m + " on "
                            + s );
        }

        Acl parentAcl = null;
        try {
            Acl acl = aclService.readAclById( oi );
            parentAcl = acl.getParentAcl(); // can be null.

        } catch ( NotFoundException nfe ) {
            /*
             * Then, this shouldn't be an update.
             */
            log.warn( "On 'update' methods, there should be a ACL on the passed object already. Method=" + m + " on "
                    + s );
        }

        addOrUpdateAcl( s, parentAcl );
        processAssociations( m, s, parentAcl );
    }

    private MutableAcl getAcl( Securable s ) {
        ObjectIdentity oi = objectIdentityRetrievalStrategy.getObjectIdentity( s );

        try {
            return ( MutableAcl ) aclService.readAclById( oi );
        } catch ( NotFoundException e ) {
            return null;
        }
    }

}
