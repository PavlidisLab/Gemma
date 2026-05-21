/*
 * The gemma-core project
 *
 * Copyright (c) 2013 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.core.security.acl;

import ubic.gemma.core.security.AuthorityConstants;
import ubic.gemma.core.security.acl.domain.AclService;
import org.springframework.security.acls.domain.GrantedAuthoritySid;
import org.springframework.security.acls.domain.PrincipalSid;
import ubic.gemma.core.security.model.Securable;
import ubic.gemma.core.security.model.SecuredChild;
import ubic.gemma.core.security.model.SecuredNotChild;
import ubic.gemma.core.security.util.SecurityUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.security.access.vote.AuthenticatedVoter;
import org.springframework.security.acls.domain.BasePermission;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import javax.annotation.Nullable;

/**
 * Adds security controls to newly created objects (including those created by updates to other objects via cascades),
 * and removes them for objects that are deleted. Methods in this interceptor are run for all new objects (to add
 * security if needed) and when objects are deleted. This is not used to modify permissions on existing objects.
 * <p>
 * This is designed to be reusable, but it's not trivial; it requires substantial care from the implementer who override
 * the protected methods. Looking at the AclAdvice in Gemma can give some ideas of what kinds of things have to be
 * handled.
 *
 * @author keshav
 * @author pavlidis
 * @version $Id: BaseAclAdvice.java,v 1.1 2013/09/14 16:56:03 paul Exp $
 */
@SuppressWarnings("unused")
public abstract class BaseAclAdvice {

    private static final Log log = LogFactory.getLog( BaseAclAdvice.class );

    private final AclService aclService;
    private final SessionFactory sessionFactory;
    private final ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy;

    protected BaseAclAdvice( AclService aclService, SessionFactory sessionFactory, ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy ) {
        this.aclService = aclService;
        this.sessionFactory = sessionFactory;
        this.objectIdentityRetrievalStrategy = objectIdentityRetrievalStrategy;
    }

    /**
     * Check for special cases of objects that don't need to be examined for associations at all, for efficiency when
     * following associations. Default implementation always returns false.
     */
    protected boolean canSkipAclCheck( Object object ) {
        return false;
    }

    /**
     * Subclasses can modify this to address special cases. Default implementation is a no-op.
     * <p>
     * FIXME this might not be necessary.
     *
     * @param acl       may be modified by this call
     * @param parentAcl value may be changed by this call
     * @param sid       value may be changed by this call
     */
    protected void createOrUpdateAclSpecialCases( MutableAcl acl, @Nullable Acl parentAcl, Sid sid, Securable object ) {
    }

    protected boolean currentUserIsAdmin() {
        return SecurityUtil.isUserAdmin();
    }

    protected boolean currentUserIsAnonymous() {
        return SecurityUtil.isUserAnonymous();
    }

    protected boolean currentUserIsRunningAsAdmin() {
        return SecurityUtil.isRunningAsAdmin();
    }

    /**
     * For use by other overridden methods.
     */
    protected final AclService getAclService() {
        return aclService;
    }

    protected abstract GrantedAuthority getUserGroupGrantedAuthority( Securable object );

    protected abstract String getUserName( Securable object );

    /**
     * Called during create. Default implementation returns null unless the object is a SecuredChild, in which case it
     * returns the value of s.getSecurityOwner() (which will be null unless it is filled in)
     * <p>
     * For some cases, we want to find the parent so we don't have to rely on updates later to catch it and fill it in.
     * Implementers must decide which cases can be handled this way. Care is required: the parent might not be created
     * yet, in which case the cascade to s is surely going to fix it later. The best situation is when s has an accessor
     * to reach the parent.
     *
     * @param s which might have a parent already in the system
     */
    protected Acl locateParentAcl( SecuredChild s ) {
        Securable parent = locateSecuredParent( s );

        if ( parent != null ) return this.getAclService().readAclById( makeObjectIdentity( parent ) );

        return null;
    }

    /**
     * Forms the object identity to be inserted in acl_object_identity table. Note that this does not add an
     * ObjectIdentity to the database; it just calls 'new'.
     *
     * @param object A persistent object
     * @return object identity.
     */
    protected final ObjectIdentity makeObjectIdentity( Securable object ) {
        return objectIdentityRetrievalStrategy.getObjectIdentity( object );
    }

    protected abstract boolean objectIsUser( Securable object );

    protected abstract boolean objectIsUserGroup( Securable object );

    /**
     * Called when objects are first created in the system and need their permissions initialized. Insert the access
     * control entries that all objects should have (unless they inherit from another object).
     * <p>
     * Default implementation does the following:
     * <ul>
     * <li>All objects are administratable by GROUP_ADMIN
     * <li>GROUP_AGENT has READ permissions on all objects
     * <li>If the current user is an adminisrator, and keepPrivateEvenWhenAdmin is false, the object gets READ
     * permissions for ANONYMOUS.
     * <li>If the current user is a "regular user" (non-admin) give them read/write permissions.
     */
    protected void setupBaseAces( MutableAcl acl, ObjectIdentity oi, Sid sid, boolean keepPrivateEvenWhenAdmin ) {

        /*
         * All objects must have administration permissions on them.
         */
        if ( log.isDebugEnabled() ) log.debug( "Making administratable by GROUP_ADMIN: " + oi );
        grant( acl, BasePermission.ADMINISTRATION, new GrantedAuthoritySid( new SimpleGrantedAuthority(
            AuthorityConstants.ADMIN_GROUP_AUTHORITY ) ) );

        /*
         * Let agent read anything
         */
        if ( log.isDebugEnabled() ) log.debug( "Making readable by GROUP_AGENT: " + oi );
        grant( acl, BasePermission.READ, new GrantedAuthoritySid( new SimpleGrantedAuthority(
            AuthorityConstants.AGENT_GROUP_AUTHORITY ) ) );

        /*
         * If admin, and the object is not a user or group, make it readable by anonymous.
         */
        boolean makeAnonymousReadable = this.currentUserIsAdmin() && !keepPrivateEvenWhenAdmin;

        if ( makeAnonymousReadable ) {
            if ( log.isDebugEnabled() ) log.debug( "Making readable by IS_AUTHENTICATED_ANONYMOUSLY: " + oi );
            grant( acl, BasePermission.READ, new GrantedAuthoritySid( new SimpleGrantedAuthority(
                AuthenticatedVoter.IS_AUTHENTICATED_ANONYMOUSLY ) ) );
        }

        /*
         * Don't add more permissions for the administrator. But whatever it is, the person who created it can
         * read/write it. User will only be anonymous if they are registering (AFAIK)
         */
        if ( !this.currentUserIsAdmin() && !this.currentUserIsAnonymous() ) {

            if ( log.isDebugEnabled() ) log.debug( "Giving read/write permissions on " + oi + " to " + sid );
            grant( acl, BasePermission.READ, sid );

            /*
             * User who created something can edit it.
             */
            grant( acl, BasePermission.WRITE, sid );

        }

    }

    /**
     * For cases in which the object is not a SecuredChild, but we still want to erase ACEs on it when it has a parent.
     * Implementers will check the class of the object, and the class of the parent (e.g. using <code>Class.forName(
     * parentAcl.getObjectIdentity().getType() )</code>) and decide what to do.
     *
     * @return false if ACEs should be retained. True if ACEs should be removed (if possible).
     */
    protected boolean specialCaseToAllowRemovingAcesFromChild( Securable object, Acl parentAcl ) {
        return false;
    }

    /**
     * Indicate if the given object should not be made public immediately on creation by administrators.
     * <p>
     * The default implementation returns true if the object is a {@link SecuredChild}; otherwise false.
     *
     * @return true if it's a special case to be kept private on creation.
     */
    protected boolean specialCaseToKeepPrivateOnCreation( Securable object ) {
        return object instanceof SecuredChild;
    }

    /**
     * Creates the Acl object.
     *
     * @param acl       If non-null we're in update mode, possibly setting the parent.
     * @param object    The domain object.
     * @param parentAcl can be null
     */
    /**
     * Renovations Phase 3: visibility relaxed so {@link AclEventListener} (in the same package)
     * can drive ACL maintenance directly off Hibernate insert events without going through the
     * AOP advice's entity-graph walk.
     */
    void addOrUpdateAcl( @Nullable MutableAcl acl, Securable object, @Nullable Acl parentAcl ) {

        if ( object.getId() == null ) {
            // Renovations Phase 2: defensive safety net. doAclAdvice now swaps the input arg for
            // the session's managed copy on updates (see resolveManaged), so the walk usually
            // traverses a cascade-flushed graph with assigned child ids. This branch still fires
            // for Securables that genuinely reach the advice with a transient instance (e.g. some
            // create-path callers that pass objects not yet known to the session). Force a
            // persist+flush so the id is assigned and we can build the ACL on this same advice
            // pass. Caller-side flows that try to create() the same entity later are protected
            // by the matching idempotent check in AbstractDao.create.
            if ( sessionFactory != null ) {
                try {
                    sessionFactory.getCurrentSession().persist( object );
                    sessionFactory.getCurrentSession().flush();
                } catch ( RuntimeException ex ) {
                    log.warn( "Failed to force-persist transient Securable " + object + ": " + ex, ex );
                }
            }
            if ( object.getId() == null ) {
                log.warn( "ACLs cannot be added or updated on non-persistent object: " + object );
                return;
            }
        }

        if ( log.isTraceEnabled() ) log.trace( "Checking for ACLS on " + object );
        ObjectIdentity oi = makeObjectIdentity( object );

        boolean create = false;
        if ( acl == null ) {
            // usually create, but could be update.
            try {
                // this is probably redundant. We shouldn't have ACLs already.
                acl = ( MutableAcl ) getAclService().readAclById( oi ); // throws exception if not found
                /*
                 * If we get here, we're in update mode after all. Could be findOrCreate, or could be a second pass that
                 * will let us fill in parent ACLs for associated objects missed earlier in a persist cycle. E.g.
                 * BioMaterial
                 */
                try {
                    maybeSetParentACL( object, acl, parentAcl );
                    return;
                } catch ( NotFoundException nfe ) {
                    log.error( nfe, nfe );
                }
            } catch ( NotFoundException nfe ) {
                // the current user will be the owner.
                acl = getAclService().createAcl( oi );
                create = true;
                assert acl != null;
                assert acl.getOwner() != null;
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

        PrincipalSid sid = new PrincipalSid( p.toString() );

        boolean isAdmin = currentUserIsAdmin();

        boolean isRunningAsAdmin = currentUserIsRunningAsAdmin();

        boolean objectIsAUser = objectIsUser( object );

        boolean objectIsAGroup = objectIsUserGroup( object );

        boolean keepPrivateEvenWhenAdmin = this.specialCaseToKeepPrivateOnCreation( object );

        /*
         * The only case where we absolutely disallow inheritance is for SecuredNotChild.
         */
        boolean inheritFromParent = parentAcl != null && !( object instanceof SecuredNotChild );

        boolean missingParent = parentAcl == null && object instanceof SecuredChild;

        if ( missingParent ) {
            // This easily happens, it's not a problem as we go back through to recheck objects. Example: analysis,
            // before associated with experiment.
            if ( log.isDebugEnabled() ) log.debug( "Object did not have a parent during ACL setup: " + object );
        }

        /*
         * The logic here is: if we're supposed to inherit from the parent, but none is provided (can easily happen), we
         * have to put in ACEs. Same goes if we're not supposed to inherit. Objects which are not supposed to have their
         * own ACLs (SecurableChild)
         */
        if ( create && !inheritFromParent ) {
            setupBaseAces( acl, oi, sid, keepPrivateEvenWhenAdmin );

            /*
             * Make sure user groups can be read by future members of the group
             */
            if ( objectIsAGroup ) {
                GrantedAuthority ga = getUserGroupGrantedAuthority( object );
                if ( log.isDebugEnabled() ) log.debug( "Making group readable by " + ga + ": " + oi );
                grant( acl, BasePermission.READ, new GrantedAuthoritySid( ga ) );
            }

        } else {
            assert !acl.getEntries().isEmpty()
                || ( parentAcl != null && !parentAcl.getEntries().isEmpty() ) : "Failed to get valid ace for acl or parents: "
                + acl + " parent=" + parentAcl;
        }

        /*
         * If the object is a user, make sure that user gets permissions even if the current user is not the same! In
         * fact, user creation runs with RUN_AS_ADMIN privileges.
         */

        if ( create && objectIsAUser ) {
            String userName = getUserName( object );
            if ( sid.getPrincipal().equals( userName ) ) {
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
                     * Important: we expect this to normally be the case, that users are added while running in
                     * temporarily elevated status.
                     */
                    sid = new PrincipalSid( userName );
                    acl.setOwner( sid );
                }

                /*
                 * See org.springframework.security.acls.domain.AclAuthorizationStrategy.
                 */
                grant( acl, BasePermission.READ, sid );
                grant( acl, BasePermission.WRITE, sid );

            }
        }

        createOrUpdateAclSpecialCases( acl, parentAcl, sid, object );

        /*
         * Only the owner or an administrator can do these operations, and only in those cases would they be necessary
         * anyway (primarily in creating the objects in the first place, there's nearly no conceivable reason to change
         * these after creation.)
         */
        if ( sid.equals( acl.getOwner() ) || isAdmin ) {

            if ( isAdmin && acl.getOwner() == null ) {
                // don't change the owner.
                acl.setOwner( sid );
            }

            if ( parentAcl != null && inheritFromParent ) {
                if ( log.isTraceEnabled() )
                    log.trace( "Setting parent to: " + parentAcl.getObjectIdentity() + " <--- "
                        + acl.getObjectIdentity() );
                acl.setParent( parentAcl );
            }
            acl.setEntriesInheriting( inheritFromParent );
            this.maybeClearACEsOnChild( object, acl, parentAcl );
        }

        // finalize.
        getAclService().updateAcl( acl );

    }

    /**
     * Delete acl permissions for an object.
     * <p>
     * Renovations Phase 3: visibility relaxed so {@link AclEventListener} (in the same package)
     * can drive ACL deletion directly off Hibernate delete events.
     */
    void deleteAcl( Securable object ) throws DataAccessException, IllegalArgumentException {
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
        this.getAclService().deleteAcl( oi, true );
    }

    /**
     * Add ACE granting permission to sid to ACL (does not persist the change, you have to call update!)
     *
     * @param acl        which object
     * @param permission which permission
     * @param sid        which principal
     */
    private void grant( MutableAcl acl, Permission permission, Sid sid ) {
        acl.insertAce( acl.getEntries().size(), permission, sid, true );
    }

    /**
     * Locate the immediate secured parent: the value of {@link SecuredChild#getSecurityOwner()}.
     * <p>
     * Returns the direct security owner even when it is itself a {@link SecuredChild}; Spring's
     * ACL framework already handles transitive permission inheritance through the parent chain
     * (e.g. ExpressionAnalysisResultSet -> DifferentialExpressionAnalysis -> ExpressionExperiment),
     * so the parent ACL pointer should be the immediate owner — flattening to the top-level
     * Securable here breaks AclClassMetadata's registered child->parent type contract
     * (e.g. ExpressionAnalysisResultSet's parent is registered as DifferentialExpressionAnalysis,
     * not as ExpressionExperiment).
     */
    private Securable locateSecuredParent( SecuredChild s ) {
        return s.getSecurityOwner();
    }

    /**
     * When setting the parent, we check to see if we can delete the ACEs on the 'child', if any. This is because we
     * want permissions to be managed by the parent, so ACEs on the child are redundant and possibly a source of later
     * trouble. Special cases are handled by specialCaseToAllowRemovingAcesFromChild.
     * <p>
     * Before deleting anything, we check that the ACEs on the child are exactly equivalent to the ones on the parent.
     * If they aren't, it implies the child was not correctly synchronized with the parent in the first place.
     *
     * @param parentAcl -- careful with the order!
     * @throws IllegalStateException if the parent has no ACEs.
     */
    private boolean maybeClearACEsOnChild( Securable object, MutableAcl childAcl, @Nullable Acl parentAcl ) {
        if ( parentAcl == null ) return false;
        if ( object instanceof SecuredNotChild ) return false;

        int aceCount = childAcl.getEntries().size();

        if ( aceCount == 0 ) {
            if ( parentAcl.getEntries().size() == 0 ) {
                throw new IllegalStateException( "Either the child or the parent has to have ACEs" );
            }
            return false;
        }

        boolean force = specialCaseToAllowRemovingAcesFromChild( object, parentAcl );

        if ( parentAcl.getEntries().size() == aceCount || force ) {

            boolean oktoClearACEs = true;

            // check for exact match of all ACEs
            for ( AccessControlEntry ace : parentAcl.getEntries() ) {
                boolean found = false;
                for ( AccessControlEntry childAce : childAcl.getEntries() ) {
                    if ( childAce.getPermission().equals( ace.getPermission() )
                        && childAce.getSid().equals( ace.getSid() ) ) {
                        found = true;
                        log.trace( "Removing ace from child: " + ace );
                        break;
                    }
                }

                if ( !found ) {
                    log.warn( "Didn't find matching permission for " + ace + " from parent "
                        + parentAcl.getObjectIdentity() );
                    log.warn( "Parent acl: " + parentAcl );
                    oktoClearACEs = false;
                    break;
                }
            }

            if ( force || oktoClearACEs ) {
                assert childAcl.getParentAcl() != null : "Child lacks parent " + childAcl + " force=" + force;

                if ( log.isTraceEnabled() ) log.trace( "Erasing ACEs from child " + object );

                while ( childAcl.getEntries().size() > 0 ) {
                    childAcl.deleteAce( 0 );
                }

                return true;
            }

        } else {
            /*
             * This should often be an error condition. The child should typically have the same permissions as the
             * parent, if they are out of synch that's a special situation.
             *
             * For example: a differential expression analysis should not be public when the experiment is private. That
             * won't work!
             */
            log.warn( "Could not clear aces on child" );
            log.warn( "Parent: " + parentAcl );
            log.warn( "Child: " + childAcl );
            // throw new IllegalStateException( "Could not clear aces on child: " + childAcl.getObjectIdentity() );

        }

        return false;
    }

    /**
     * This is used when rechecking objects that are detached from a parent. Typically these are {@link SecuredChild}ren
     * like BioAssays.
     * <p>
     * Be careful with the argument order!
     *
     * @param childAcl  - the potential child
     * @param parentAcl - the potential parent
     */
    private void maybeSetParentACL( final Securable object, MutableAcl childAcl, @Nullable final Acl parentAcl ) {
        if ( parentAcl != null && !SecuredNotChild.class.isAssignableFrom( object.getClass() ) ) {

            Acl currentParentAcl = childAcl.getParentAcl();

            if ( currentParentAcl != null && !currentParentAcl.equals( parentAcl ) ) {
                throw new IllegalStateException( "Cannot change parentAcl on " + object
                    + " once it has ben set:\n Current parent: " + currentParentAcl + " != \nProposed parent:"
                    + parentAcl );
            }

            boolean changedParentAcl = false;
            if ( currentParentAcl == null ) {
                log.trace( "Setting parent ACL to child=" + childAcl + " parent=" + parentAcl );
                childAcl.setParent( parentAcl );
                childAcl.setEntriesInheriting( true );
                changedParentAcl = true;
            }

            boolean clearedACEs = maybeClearACEsOnChild( object, childAcl, parentAcl );

            if ( changedParentAcl || clearedACEs ) {
                getAclService().updateAcl( childAcl );
            }
        }
        childAcl.getParentAcl();
    }

}