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
package ubic.gemma.web.controller.common.auditAndSecurity;

import gemma.gsec.AuthorityConstants;
import gemma.gsec.SecurityService;
import gemma.gsec.authentication.UserDetailsImpl;
import gemma.gsec.util.SecurityUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.MailEngine;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.analysis.expression.diff.GeneDifferentialExpressionMetaAnalysis;
import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.persistence.service.analysis.expression.diff.GeneDiffExMetaAnalysisService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.web.controller.util.EntityDelegator;
import ubic.gemma.web.controller.util.EntityNotFoundException;

import javax.servlet.ServletContext;
import java.util.*;

/**
 * Manages data-level security (ie. can make data private).
 */
@SuppressWarnings("unused")
@Controller
public class SecurityController {

    private static final Log log = LogFactory.getLog( SecurityController.class );

    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private ExpressionExperimentSetService expressionExperimentSetService;
    @Autowired
    private GeneDiffExMetaAnalysisService geneDiffExMetaAnalysisService;
    @Autowired
    private GeneSetService geneSetService;
    @Autowired
    private MailEngine mailEngine;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private UserManager userManager;
    @Autowired
    private ServletContext servletContext;

    @Value("${gemma.hosturl}")
    private String hostUrl;

    public boolean addUserToGroup( String userName, String groupName ) {

        User userTakingAction = userManager.getCurrentUser();

        if ( userTakingAction == null ) {
            throw new AccessDeniedException( "Cannot add user to group when user is not logged in" );
        }

        User u;
        if ( userManager.userExists( userName ) ) {
            u = userManager.findByUserName( userName );
            if ( !u.isEnabled() ) {
                throw new IllegalArgumentException( "Sorry, that user's account is not enabled." );
            }

            securityService.addUserToGroup( userName, groupName );
        } else if ( userManager.userWithEmailExists( userName ) ) {
            u = userManager.findByEmail( userName );
            if ( !u.isEnabled() ) {
                throw new IllegalArgumentException( "Sorry, that user's account is not enabled." );
            }

            String uname = u.getUserName();
            securityService.addUserToGroup( uname, groupName );
        } else {
            throw new EntityNotFoundException( "Sorry, there is no matching user." );
        }

        /*
         * send the user an email.
         */
        String emailAddress = u.getEmail();
        if ( StringUtils.isNotBlank( emailAddress ) ) {
            SecurityController.log.debug( "Sending email notification to " + emailAddress );
            String manageGroupsUrl = hostUrl + servletContext.getContextPath() + "/manageGroups.html";
            String body = userTakingAction.getUserName() + " has added you to the group '" + groupName
                    + "'.\nTo view groups you belong to, visit " + manageGroupsUrl
                    + "\n\nIf you believe you received this email in error, contact " + mailEngine.getAdminEmailAddress()
                    + ".";
            mailEngine.sendMessage( emailAddress, "You have been added to a group on Gemma", body );
        }

        return true;
    }

    public String createGroup( String groupName ) {

        if ( StringUtils.isBlank( groupName ) || groupName.length() < 3 || !StringUtils.isAlpha( groupName ) ) {
            throw new IllegalArgumentException(
                    "Group name must contain only letters and must be at least 3 letters long." );
        }

        securityService.createGroup( groupName );
        return groupName;
    }

    public void deleteGroup( String groupName ) {

        if ( !this.getGroupsUserCanEdit().contains( groupName ) ) {
            throw new IllegalArgumentException( "You don't have permission to modify that group" );
        }
        /*
         * Additional checks for ability to remove group handled by ss.
         */
        userManager.deleteGroup( groupName );
    }

    public Integer getAuthenticatedUserCount() {
        return securityService.getAuthenticatedUserCount();
    }

    public Collection<String> getAuthenticatedUserNames() {
        return securityService.getAuthenticatedUserNames();
    }

    public Collection<UserGroupValueObject> getAvailableGroups() {
        Collection<String> editableGroups = this.getGroupsUserCanEdit();
        Collection<String> groupsUserIsIn = this.getGroupsForCurrentUser();

        Collection<String> allGroups;
        try {
            // administrator...
            allGroups = userManager.findAllGroups();
        } catch ( AccessDeniedException e ) {
            allGroups = groupsUserIsIn;
        }

        Collection<UserGroupValueObject> result = new HashSet<>();
        for ( String g : allGroups ) {
            UserGroupValueObject gvo = new UserGroupValueObject();
            gvo.setCanEdit( editableGroups.contains( g ) );
            gvo.setMember( groupsUserIsIn.contains( g ) );
            gvo.setGroupName( g );
            result.add( gvo );
        }

        return result;
    }

    public Collection<SidValueObject> getAvailablePrincipalSids() {
        List<SidValueObject> results = new ArrayList<>();
        try {
            for ( Sid s : securityService.getAvailableSids() ) {
                SidValueObject sv = new SidValueObject( s );
                if ( sv.isPrincipal() ) {
                    results.add( sv );
                }
            }
        } catch ( AccessDeniedException e ) {
            results.clear();
        }

        Collections.sort( results );
        return results;
    }

    public Collection<SidValueObject> getAvailableSids() {
        List<SidValueObject> results = new ArrayList<>();
        try {
            for ( Sid s : securityService.getAvailableSids() ) {
                SidValueObject sv = new SidValueObject( s );

                results.add( sv );
            }
        } catch ( AccessDeniedException e ) {
            results.clear();
        }

        Collections.sort( results );

        return results;
    }

    public Collection<UserValueObject> getGroupMembers( String groupName ) {
        Collection<UserValueObject> result = new HashSet<>();

        // happens if user is not in any displayed groups.
        if ( StringUtils.isBlank( groupName ) ) {
            return result;
        }
        List<String> usersInGroup = userManager.findUsersInGroup( groupName );

        for ( String userName : usersInGroup ) {
            UserDetails details = userManager.loadUserByUsername( userName );

            UserValueObject uvo = new UserValueObject();

            uvo.setUserName( details.getUsername() );

            if ( details instanceof UserDetailsImpl ) {
                uvo.setEmail( ( ( UserDetailsImpl ) details ).getEmail() );
            }

            uvo.setCurrentGroup( groupName );
            uvo.setInGroup( true );

            uvo.setAllowModification( true );

            /*
             * You can't remove yourself from a group, or remove users from the USER group.
             */
            if ( userName.equals( userManager.getCurrentUsername() ) || groupName
                    .equals( AuthorityConstants.USER_GROUP_NAME ) ) {
                uvo.setAllowModification( false );
            }

            result.add( uvo );
        }

        return result;
    }

    public SecurityInfoValueObject getSecurityInfo( EntityDelegator<? extends Securable> ed ) {

        // TODO Figure out why Transaction(readOnly = true) throws an error when this method is called from
        // SecurityManager.js (Bug 3941)

        Securable s = this.getSecurable( ed );

        return this.securable2VO( s );
    }

    public Collection<SecurityInfoValueObject> getUsersData( String currentGroup, boolean privateOnly ) {
        // Add experiments.
        Collection<Securable> secs = new HashSet<>( this.getUsersExperiments( privateOnly ) );

        Collection<SecurityInfoValueObject> result = this.securables2VOs( secs, currentGroup );

        result.addAll(
                this.securables2VOs( geneSetService.getUsersGeneGroups( privateOnly, null, true ), currentGroup ) );

        result.addAll( this.securables2VOs( this.getUsersExperimentSets( privateOnly ), currentGroup ) );

        /*
         * add other types of securables here.
         */

        return result;
    }

    public boolean makeGroupReadable( EntityDelegator<? extends Securable> ed, String groupName ) {
        Securable s = this.getSecurable( ed );
        securityService.makeReadableByGroup( s, groupName );
        return true;
    }

    public boolean makeGroupWriteable( EntityDelegator<? extends Securable> ed, String groupName ) {
        Securable s = this.getSecurable( ed );
        securityService.makeEditableByGroup( s, groupName );
        return true;
    }

    public boolean makePrivate( EntityDelegator<? extends Securable> ed ) {
        Securable s = this.getSecurable( ed );
        securityService.makePrivate( s );
        return true;
    }

    public boolean makePublic( EntityDelegator<? extends Securable> ed ) {
        Securable s = this.getSecurable( ed );
        securityService.makePublic( s );
        return true;
    }

    public boolean removeGroupReadable( EntityDelegator<? extends Securable> ed, String groupName ) {
        Securable s = this.getSecurable( ed );
        securityService.makeUnreadableByGroup( s, groupName );
        return true;
    }

    public boolean removeGroupWriteable( EntityDelegator<? extends Securable> ed, String groupName ) {
        Securable s = this.getSecurable( ed );
        securityService.makeUneditableByGroup( s, groupName );
        return true;
    }

    public boolean removeUsersFromGroup( String[] userNames, String groupName ) {
        for ( String userName : userNames ) {

            securityService.removeUserFromGroup( userName, groupName );
        }
        return true;
    }

    public SecurityInfoValueObject updatePermission( SecurityInfoValueObject settings ) {
        EntityDelegator<? extends Securable> sd = new EntityDelegator<>();
        sd.setId( settings.getEntityId() );
        sd.setClassDelegatingFor( settings.getEntityClazz() );
        Securable s = this.getSecurable( sd );

        if ( settings.isPubliclyReadable() ) {
            securityService.makePublic( s );
        } else {
            securityService.makePrivate( s );
        }

        try {
            if ( settings.getOwner().isPrincipal() ) {
                securityService.makeOwnedByUser( s, settings.getOwner().getAuthority() );
            } else {
                // this warning is not even worth issuing if we are not an administrator.
                if ( SecurityUtil.isUserAdmin() )
                    SecurityController.log.warn( "Can't make groupauthority " + settings.getOwner().getAuthority()
                            + " owner, not implemented" );
            }
        } catch ( AccessDeniedException e ) {
            SecurityController.log.warn( "Non-administrators cannot change the owner of an entity" );
            // okay, only works if you are administrator.
        }

        /*
         * This works in one of two ways. If settings.currentGroup is non-null, we just update the permissions for that
         * group - this may leave them unchanged. Otherwise, we update them all based on
         * groupsThatCanRead/groupsThatCanWrite
         */
        String currentGroupName = settings.getCurrentGroup();
        if ( StringUtils.isNotBlank( currentGroupName ) && !(
                currentGroupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) || currentGroupName
                        .equals( AuthorityConstants.AGENT_GROUP_NAME ) ) ) {

            // this test only makes sense for changing the group's name, not for changing the permissions
            // of potentially shared entities
            // if ( !getGroupsUserCanEdit().contains( currentGroupName ) ) {
            // throw new AccessDeniedException( "Access denied to permissions for group=" + currentGroupName );
            // }

            boolean readable = settings.isCurrentGroupCanRead();
            boolean writeable = settings.isCurrentGroupCanWrite();

            if ( readable ) {
                securityService.makeReadableByGroup( s, currentGroupName );
            } else {

                securityService.makeUnreadableByGroup( s, currentGroupName );
            }

            if ( writeable ) {
                // if writable should be readable
                securityService.makeReadableByGroup( s, currentGroupName );
                securityService.makeEditableByGroup( s, currentGroupName );
            } else {
                securityService.makeUneditableByGroup( s, currentGroupName );
            }

        } else {
            /*
             * Remove all group permissions - we'll set them back to what was requested. Exception: we don't allow
             * changes to admin or agent permissions by this route.
             */
            for ( String groupName : this.getGroupsUserCanEdit() ) {

                if ( groupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) || groupName
                        .equals( AuthorityConstants.AGENT_GROUP_NAME ) ) {
                    // never changes this.
                    continue;
                }

                securityService.makeUnreadableByGroup( s, groupName );
                securityService.makeUneditableByGroup( s, groupName );
            }

            /*
             * Add selected ones back
             */
            for ( String reader : settings.getGroupsThatCanRead() ) {
                if ( reader.equals( AuthorityConstants.ADMIN_GROUP_NAME ) || reader
                        .equals( AuthorityConstants.AGENT_GROUP_NAME ) ) {
                    // never changes this.
                    continue;
                }

                securityService.makeReadableByGroup( s, reader );
            }
            for ( String writer : settings.getGroupsThatCanWrite() ) {
                if ( writer.equals( AuthorityConstants.ADMIN_GROUP_NAME ) || writer
                        .equals( AuthorityConstants.AGENT_GROUP_NAME ) ) {
                    // never changes this.
                    continue;
                }
                // when it is writable it should be readable
                securityService.makeReadableByGroup( s, writer );
                securityService.makeEditableByGroup( s, writer );
            }
        }

        SecurityController.log.info( "Updated permissions on " + s );
        return this.securable2VO( s );
    }

    public void updatePermissions( SecurityInfoValueObject[] settings ) {
        for ( SecurityInfoValueObject so : settings ) {
            this.updatePermission( so );
        }
    }

    /**
     * @return groups the user can edit (not just the ones they are in!)
     */
    private Collection<String> getGroupsForCurrentUser() {
        return userManager.findAllGroups();
        // return userManager.findGroupsForUser( userManager.getCurrentUsername() );
    }

    private Collection<String> getGroupsForUser( String username ) {

        if ( username == null ) {
            return new HashSet<>();
        }

        Collection<String> results;

        try {
            results = userManager.findGroupsForUser( username );
        } catch ( UsernameNotFoundException e ) {
            return new HashSet<>();
        }
        return results;
    }

    private Collection<String> getGroupsUserCanEdit() {
        String username = userManager.getCurrentUsername();
        return username != null ? securityService.getGroupsUserCanEdit( username ) : Collections.emptyList();
    }

    /**
     * @param ed ed
     * @return securable
     * @throws IllegalArgumentException if the Securable cannot be loaded
     */
    private Securable getSecurable( EntityDelegator<? extends Securable> ed ) {
        Securable s;
        if ( ed.holds( ExpressionExperiment.class ) ) {
            s = expressionExperimentService.load( ed.getId() );
        } else if ( ed.holds( GeneSet.class ) ) {
            s = geneSetService.load( ed.getId() );
        } else if ( ed.holds( ExpressionExperimentSet.class ) ) {
            s = expressionExperimentSetService.load( ed.getId() );
        } else if ( ed.holds( GeneDifferentialExpressionMetaAnalysis.class ) ) {
            s = geneDiffExMetaAnalysisService.load( ed.getId() );
        } else {
            throw new IllegalArgumentException( "Delegating " + ed.getClassDelegatingFor() + " is not supported by security controller yet." );
        }
        if ( s == null ) {
            throw new EntityNotFoundException( "Entity does not exist or user does not have access." );
        }
        return s;
    }

    private Collection<Securable> getUsersExperiments( boolean privateOnly ) {
        Collection<ExpressionExperiment> ees = expressionExperimentService.loadAll();

        Collection<Securable> secs = new HashSet<>();

        if ( privateOnly ) {
            try {
                secs.addAll( securityService.choosePrivate( ees ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else {
            Collection<ExpressionExperiment> usersEEs = expressionExperimentService.loadAll();
            secs.addAll( ees );
            secs.addAll( usersEEs );
        }
        return secs;
    }

    private Collection<Securable> getUsersExperimentSets( boolean privateOnly ) {
        Collection<Securable> secs = new HashSet<>();

        Collection<ExpressionExperimentSet> eeSets = expressionExperimentSetService.loadAllExperimentSetsWithTaxon();
        if ( privateOnly ) {
            try {
                secs.addAll( securityService.choosePrivate( eeSets ) );
            } catch ( AccessDeniedException e ) {
                // okay, they just aren't allowed to see those.
            }
        } else {
            secs.addAll( eeSets );
        }
        return secs;
    }

    /**
     * Create a fully-populated value object for the given securable.
     *
     * @param s securable
     * @return security info VO
     */
    private SecurityInfoValueObject securable2VO( Securable s ) {
        /*
         * Problem: this is quite slow. Can probably improve by not loading the securable at all, just load a
         * SecuredValueObject, but it doesn't currently have all this information.
         */
        boolean isPublic = securityService.isPublic( s );
        boolean isShared = securityService.isShared( s );
        boolean canWrite = securityService.isEditableByCurrentUser( s );

        SecurityInfoValueObject result = new SecurityInfoValueObject( s );

        result.setAvailableGroups( this.getGroupsForCurrentUser() );
        result.setPubliclyReadable( isPublic );
        result.setGroupsThatCanRead( securityService.getGroupsReadableBy( s ) );
        result.setGroupsThatCanWrite( securityService.getGroupsEditableBy( s ) );
        result.setShared( isShared );
        result.setOwner( new SidValueObject( securityService.getOwner( s ) ) );
        result.setOwnersGroups( this.getGroupsForUser( result.getOwner().getAuthority() ) );
        result.setCurrentUserOwns( securityService.isOwnedByCurrentUser( s ) );
        result.setCurrentUserCanwrite( canWrite );

        if ( Describable.class.isAssignableFrom( s.getClass() ) ) {
            result.setEntityDescription( ( ( Describable ) s ).getDescription() );
            result.setEntityName( ( ( Describable ) s ).getName() );
        }
        return result;
    }

    /**
     * @param securables   securables
     * @param currentGroup A specific group that we're focusing on. Can be null
     * @return security info VOs
     */
    private <T extends Securable> Collection<SecurityInfoValueObject> securables2VOs( Collection<T> securables,
            String currentGroup ) {

        Collection<SecurityInfoValueObject> result = new HashSet<>();

        if ( securables.isEmpty() ) {
            return result;
        }

        /*
         * Fast computations out-of-loop
         */
        Collection<String> groupsForCurrentUser = this.getGroupsForCurrentUser();
        Map<T, Boolean> privacy = securityService.arePrivate( securables );
        Map<T, Boolean> sharedness = securityService.areShared( securables );
        Map<T, Sid> owners = securityService.getOwners( securables );
        Map<T, Collection<String>> groupsReadableBy = securityService.getGroupsReadableBy( securables );
        Map<T, Collection<String>> groupsEditableBy = securityService.getGroupsEditableBy( securables );

        // int i = 0; // TESTING
        for ( T s : securables ) {

            Collection<String> groupsThatCanRead = groupsReadableBy.get( s );
            Collection<String> groupsThatCanWrite = groupsEditableBy.get( s );

            SecurityInfoValueObject vo = new SecurityInfoValueObject( s );
            vo.setCurrentGroup( currentGroup );
            vo.setAvailableGroups( groupsForCurrentUser );
            vo.setPubliclyReadable( !privacy.get( s ) );
            vo.setShared( sharedness.get( s ) );
            vo.setOwner( new SidValueObject( owners.get( s ) ) );

            // FIXME this does not seem to be used in the UI and it fixes issue #41: https://github.com/ppavlidis/Gemma/issues/41
            vo.setCurrentUserOwns( false );//securityService.isOwnedByCurrentUser( s ) );
            vo.setCurrentUserCanwrite( securityService.isEditableByCurrentUser( s ) );

            vo.setGroupsThatCanRead( groupsThatCanRead == null ? new HashSet<>() : groupsThatCanRead );
            vo.setGroupsThatCanWrite( groupsThatCanWrite == null ? new HashSet<>() : groupsThatCanWrite );

            vo.setEntityClazz( s.getClass().getName() );

            if ( currentGroup != null ) {
                vo.setCurrentGroupCanRead( groupsThatCanRead != null && groupsThatCanRead.contains( currentGroup ) );
                vo.setCurrentGroupCanWrite( groupsThatCanWrite != null && groupsThatCanWrite.contains( currentGroup ) );
            }

            if ( ExpressionExperiment.class.isAssignableFrom( s.getClass() ) ) {
                vo.setEntityShortName( ( ( ExpressionExperiment ) s ).getShortName() );
                vo.setEntityName( ( ( ExpressionExperiment ) s ).getName() );
            } else if ( Describable.class.isAssignableFrom( s.getClass() ) ) {
                vo.setEntityShortName( ( ( Describable ) s ).getName() );
                vo.setEntityName( ( ( Describable ) s ).getDescription() );
            }

            result.add( vo );
            // if ( ++i > 10 ) break; // TESTING
        }
        return result;
    }

}
