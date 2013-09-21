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

import gemma.gsec.acl.BaseAclAdvice;
import gemma.gsec.model.Securable;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.MutableAcl;
import org.springframework.security.acls.model.NotFoundException;
import org.springframework.security.acls.model.ObjectIdentity;
import org.springframework.security.acls.model.Sid;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.GroupAuthority;
import ubic.gemma.model.common.auditAndSecurity.StatusImpl;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.auditAndSecurity.UserGroup;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.util.SystemArchitectureAspect;

/**
 * For permissions modification to be triggered, the method name must match certain patterns, which include "create", or
 * "remove". These patterns are defined in the {@link SystemArchitectureAspect}. Other methods that would require
 * changes to permissions will not work without modifying the source code. *
 * 
 * @author Paul
 * @version $Id$
 */
@Component
public class AclAdvice extends BaseAclAdvice {

    private static Log log = LogFactory.getLog( AclAdvice.class );

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#canSkipAclCheck(java.lang.Object)
     */
    @Override
    protected boolean canSkipAclCheck( Object object ) {
        return AuditTrail.class.isAssignableFrom( object.getClass() )
                || StatusImpl.class.isAssignableFrom( object.getClass() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#canSkipAssociationCheck(java.lang.Object, java.lang.String)
     */
    @Override
    protected boolean canSkipAssociationCheck( Object object, String propertyName ) {

        /*
         * If this is an expression experiment, don't go down the data vectors - it has no securable associations and
         * would be expensive to traverse.F
         */
        if ( ExpressionExperiment.class.isAssignableFrom( object.getClass() )
                && ( propertyName.equals( "rawExpressionDataVectors" ) || propertyName
                        .equals( "processedExpressionDataVectors" ) ) ) {
            if ( log.isTraceEnabled() ) log.trace( "Skipping checking acl on vectors on " + object );
            return true;
        }

        /*
         * Array design has some non (directly) securable associations that would be expensive to load
         */
        if ( ArrayDesign.class.isAssignableFrom( object.getClass() )
                && ( propertyName.equals( "compositeSequences" ) || propertyName.equals( "reporters" ) ) ) {
            if ( log.isTraceEnabled() ) log.trace( "Skipping checking acl on probes on " + object );
            return true;
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gemma.gsec.acl.BaseAclAdvice#createOrUpdateAclSpecialCases(org.springframework.security.acls.model.MutableAcl,
     * org.springframework.security.acls.model.Acl, org.springframework.security.acls.model.Sid,
     * gemma.gsec.model.Securable)
     */
    @Override
    protected void createOrUpdateAclSpecialCases( MutableAcl acl, Acl parentAcl, Sid sid, Securable object ) {

        /*
         * FIXME this might not be necessary.
         */

        // Treating Analyses as special case. It'll inherit ACL from ExpressionExperiment
        // If aclParent is passed to this method we overwrite it.
        if ( SingleExperimentAnalysis.class.isAssignableFrom( object.getClass() ) ) {
            SingleExperimentAnalysis experimentAnalysis = ( SingleExperimentAnalysis ) object;

            BioAssaySet bioAssaySet = experimentAnalysis.getExperimentAnalyzed();
            ObjectIdentity oi_temp = makeObjectIdentity( bioAssaySet );

            try {
                parentAcl = getAclService().readAclById( oi_temp );
            } catch ( NotFoundException nfe ) {
                // This is possible if making an EESubSet is part of the transaction.
                parentAcl = getAclService().createAcl( oi_temp );
            }

            acl.setEntriesInheriting( true );
            acl.setParent( parentAcl );

            // Owner of the experiment owns analyses even if administrator ran them.
            sid = parentAcl.getOwner();
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#getUserGroupGrantedAuthority(gemma.gsec.model.Securable)
     */
    @Override
    protected GrantedAuthority getUserGroupGrantedAuthority( Securable object ) {
        Collection<GroupAuthority> authorities = ( ( UserGroup ) object ).getAuthorities();
        assert authorities.size() == 1;
        GrantedAuthority ga = new SimpleGrantedAuthority( authorities.iterator().next().getAuthority() );
        return ga;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#getUserName(gemma.gsec.model.Securable)
     */
    @Override
    protected String getUserName( Securable user ) {
        return ( ( User ) user ).getUserName();
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#objectIsUser(gemma.gsec.model.Securable)
     */
    @Override
    protected boolean objectIsUser( Securable object ) {
        return User.class.isAssignableFrom( object.getClass() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#objectIsUserGroup(gemma.gsec.model.Securable)
     */
    @Override
    protected boolean objectIsUserGroup( Securable object ) {
        return UserGroup.class.isAssignableFrom( object.getClass() );
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#specialCaseForAssociationFollow(java.lang.Object, java.lang.String)
     */
    @Override
    protected boolean specialCaseForAssociationFollow( Object object, String property ) {
        if ( BioAssay.class.isAssignableFrom( object.getClass() )
                && ( property.equals( "sampleUsed" ) || property.equals( "arrayDesignUsed" ) ) ) {
            return true;
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#specialCaseToAllowRemovingAcesFromChild(gemma.gsec.model.Securable,
     * org.springframework.security.acls.model.Acl)
     */
    @Override
    protected boolean specialCaseToAllowRemovingAcesFromChild( Securable object, Acl parentAcl ) {

        Class<?> parentClassType;
        try {
            parentClassType = Class.forName( parentAcl.getObjectIdentity().getType() );

            /*
             * Localfiles are not SecuredChild - but they can be children of an experiment, so we have to let them be
             * deleted. Otherwise we end up with cruft.
             */
            if ( LocalFile.class.isAssignableFrom( object.getClass() )
                    && Investigation.class.isAssignableFrom( parentClassType ) ) {
                return true;
            }
        } catch ( ClassNotFoundException e ) {
            throw new IllegalStateException( "Tried to identify class from name: "
                    + parentAcl.getObjectIdentity().getType(), e );
        }

        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see gemma.gsec.acl.BaseAclAdvice#specialCaseToKeepPrivateOnCreation(java.lang.Class)
     */
    @Override
    protected boolean specialCaseToKeepPrivateOnCreation( Class<? extends Securable> clazz ) {

        if ( super.specialCaseToKeepPrivateOnCreation( clazz ) ) {
            return true;
        }

        if ( UserGroup.class.isAssignableFrom( clazz ) ) {
            return true;
        }

        if ( User.class.isAssignableFrom( clazz ) ) {
            return true;
        }

        // important if we return true for SecuredChild.
        if ( Investigation.class.isAssignableFrom( clazz ) ) {
            return true;
        }

        return false;
    }
}
