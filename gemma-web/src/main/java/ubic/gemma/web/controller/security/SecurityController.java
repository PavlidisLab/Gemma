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
package ubic.gemma.web.controller.security;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.Describable;
import ubic.gemma.model.common.auditAndSecurity.Securable;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.security.SecurityService;
import ubic.gemma.security.authentication.UserManager;
import ubic.gemma.util.AuthorityConstants;
import ubic.gemma.web.remote.EntityDelegator;

/**
 * Manages data-level security (ie. can make data private).
 * 
 * @author keshav
 * @version $Id$
 */
@Controller
public class SecurityController {

    private static Log log = LogFactory.getLog( SecurityController.class );

    @Autowired
    private ExpressionExperimentService expressionExperimentService = null;

    @Autowired
    private SecurityService securityService = null;

    @Autowired
    private UserManager userManager = null;

    /**
     * AJAX
     * 
     * @param userName
     * @param groupName
     * @return
     */
    public boolean addUserToGroup( String userName, String groupName ) {
        securityService.addUserToGroup( userName, groupName );
        return true;
    }

    /**
     *AJAX
     * 
     * @param groupName
     * @return
     */
    public String createGroup( String groupName ) {
        securityService.createGroup( groupName );
        return groupName;
    }

    /**
     * AJAX
     * 
     * @return List of group names the user can add members to and/or give permissions on objects to.
     */
    public Collection<String> getAvailableGroups() {
        return userManager.findGroupsForUser( userManager.getCurrentUsername() );
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public SecurityInfoValueObject getSecurityInfo( EntityDelegator ed ) {

        Securable s = getSecurable( ed );

        boolean isPublic = securityService.isPublic( s );
        boolean isShared = securityService.isShared( s );

        SecurityInfoValueObject result = new SecurityInfoValueObject( s );

        result.setAvailableGroups( getAvailableGroups() );

        result.setPubliclyReadable( isPublic );
        result.setGroupsThatCanRead( securityService.getGroupsReadableBy( s ) );
        result.setGroupsThatCanWrite( securityService.getGroupsEditableBy( s ) );

        /*
         * Summary: if it's readable by someone other than admin, it's 'shared'.
         */
        result.setShared( isShared );

        if ( Describable.class.isAssignableFrom( s.getClass() ) ) {
            result.setEntityDescription( ( ( Describable ) s ).getDescription() );
            result.setEntityName( ( ( Describable ) s ).getName() );
        }

        return result;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean makeGroupReadable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeReadableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean makeGroupWriteable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeWriteableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public boolean makePrivate( EntityDelegator ed ) {
        Securable s = getSecurable( ed );
        securityService.makePrivate( s );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @return
     */
    public boolean makePublic( EntityDelegator ed ) {
        Securable s = getSecurable( ed );
        securityService.makePublic( s );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean removeGroupReadable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeUnreadableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param ed
     * @param groupName
     * @return
     */
    public boolean removeGroupWriteable( EntityDelegator ed, String groupName ) {
        Securable s = getSecurable( ed );
        securityService.makeUnwriteableByGroup( s, groupName );
        return true;
    }

    /**
     * AJAX
     * 
     * @param userName
     * @param groupName
     * @return
     */
    public boolean removeUserFromGroup( String userName, String groupName ) {
        securityService.removeUserFromGroup( userName, groupName );
        return true;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    /**
     * @param securityService
     */
    public void setSecurityService( SecurityService securityService ) {
        this.securityService = securityService;
    }

    /**
     * @param userManager the userManager to set
     */
    public void setUserManager( UserManager userManager ) {
        this.userManager = userManager;
    }

    /**
     * AJAX
     * 
     * @param settings
     */
    @Transactional
    public void updatePermissions( SecurityInfoValueObject settings ) {
        EntityDelegator sd = new EntityDelegator();
        sd.setId( settings.getEntityId() );
        sd.setClassDelegatingFor( settings.getEntityClazz() );
        Securable s = getSecurable( sd );

        if ( settings.isPubliclyReadable() ) {
            securityService.makePublic( s );
        } else {
            securityService.makePrivate( s );
        }

        /*
         * Remove all group permissions
         */
        for ( String groupName : getAvailableGroups() ) {

            if ( groupName.equals( AuthorityConstants.ADMIN_GROUP_NAME ) ) {
                // never changes this.
                continue;
            }

            securityService.makeUnreadableByGroup( s, groupName );
            securityService.makeUnwriteableByGroup( s, groupName );
        }

        /*
         * Add selected ones back
         */
        for ( String reader : settings.getGroupsThatCanRead() ) {
            if ( reader.equals( AuthorityConstants.ADMIN_GROUP_NAME ) ) {
                // never changes this.
                continue;
            }
            securityService.makeReadableByGroup( s, reader );
        }
        for ( String writer : settings.getGroupsThatCanWrite() ) {
            if ( writer.equals( AuthorityConstants.ADMIN_GROUP_NAME ) ) {
                // never changes this.
                continue;
            }
            securityService.makeWriteableByGroup( s, writer );
        }

        log.info( "Updated permissions on " + s );
    }

    /**
     * @param ed
     * @return
     * @throws IllegalArgumentException if the Securable cannot be loaded
     */
    private Securable getSecurable( EntityDelegator ed ) {
        String classDelegatingFor = ed.getClassDelegatingFor();

        Class<?> clazz;
        Securable s = null;
        try {
            clazz = Class.forName( classDelegatingFor );
        } catch ( ClassNotFoundException e1 ) {
            throw new RuntimeException( e1 );
        }
        if ( ExpressionExperiment.class.isAssignableFrom( clazz ) ) {
            s = expressionExperimentService.load( ed.getId() );
        } else {
            throw new UnsupportedOperationException( clazz + " not supported by security controller yet" );
        }

        if ( s == null ) {
            throw new IllegalArgumentException( "Entity does not exist or user does not have access." );
        }
        return s;
    }

}
