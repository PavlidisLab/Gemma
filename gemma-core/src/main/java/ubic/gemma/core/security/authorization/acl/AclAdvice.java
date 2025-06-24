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
package ubic.gemma.core.security.authorization.acl;

import gemma.gsec.acl.BaseAclAdvice;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.model.GroupAuthority;
import gemma.gsec.model.Securable;
import gemma.gsec.model.User;
import gemma.gsec.model.UserGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.analysis.SingleExperimentAnalysis;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Pointcuts;

import javax.annotation.Nullable;
import java.util.Collection;

/**
 * For permissions modification to be triggered, the method name must match certain patterns, which include "create", or
 * "remove". These patterns are defined in the {@link Pointcuts}. Other methods that would require
 * changes to permissions will not work without modifying the source code. *
 *
 * @author Paul
 */
@Component
public class AclAdvice extends BaseAclAdvice {

    private static final Log log = LogFactory.getLog( AclAdvice.class );

    @Autowired
    public AclAdvice( AclService aclService, SessionFactory sessionFactory, ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy ) {
        super( aclService, sessionFactory, objectIdentityRetrievalStrategy );
    }

    @Override
    protected boolean canSkipAclCheck( Object object ) {
        return AuditTrail.class.isAssignableFrom( object.getClass() ) || CurationDetails.class
                .isAssignableFrom( object.getClass() );
    }

    @Override
    protected boolean canSkipAssociationCheck( Object object, String propertyName ) {

        /*
         * If this is an expression experiment, don't go down the data vectors - it has no securable associations and
         * would be expensive to traverse.
         */
        if ( ExpressionExperiment.class.isAssignableFrom( object.getClass() )
                && ( propertyName.equals( "rawExpressionDataVectors" )
                || propertyName.equals( "processedExpressionDataVectors" )
                || propertyName.equals( "singleCellExpressionDataVectors" ) ) ) {
            if ( AclAdvice.log.isTraceEnabled() )
                AclAdvice.log.trace( "Skipping checking acl on vectors on " + object );
            return true;
        }

        /*
         * Array design has some non (directly) securable associations that would be expensive to load
         */
        if ( ArrayDesign.class.isAssignableFrom( object.getClass() ) && propertyName.equals( "compositeSequences" ) ) {
            if ( AclAdvice.log.isTraceEnabled() )
                AclAdvice.log.trace( "Skipping checking acl on probes on " + object );
            return true;
        }

        return false;
    }

    @Override
    protected void createOrUpdateAclSpecialCases( MutableAcl acl, @Nullable Acl parentAcl, Sid sid, Securable object ) {

        // Treating Analyses as special case. It'll inherit ACL from ExpressionExperiment
        // If aclParent is passed to this method we overwrite it.
        if ( SingleExperimentAnalysis.class.isAssignableFrom( object.getClass() ) ) {
            SingleExperimentAnalysis experimentAnalysis = ( SingleExperimentAnalysis ) object;

            BioAssaySet bioAssaySet = experimentAnalysis.getExperimentAnalyzed();
            ObjectIdentity oi_temp = this.makeObjectIdentity( bioAssaySet );

            parentAcl = this.getAclService().readAclById( oi_temp );
            if ( parentAcl == null ) {
                // This is possible if making an EESubSet is part of the transaction.
                parentAcl = this.getAclService().createAcl( oi_temp );
            }
            acl.setEntriesInheriting( true );
            acl.setParent( parentAcl );
            //noinspection UnusedAssignment //Owner of the experiment owns analyses even if administrator ran them.
            sid = parentAcl.getOwner();
        }

    }

    @Override
    protected GrantedAuthority getUserGroupGrantedAuthority( Securable object ) {
        Collection<? extends GroupAuthority> authorities = ( ( UserGroup ) object ).getAuthorities();
        assert authorities.size() == 1;
        return new SimpleGrantedAuthority( authorities.iterator().next().getAuthority() );
    }

    @Override
    protected String getUserName( Securable user ) {
        return ( ( User ) user ).getUserName();
    }

    @Override
    protected boolean objectIsUser( Securable object ) {
        return User.class.isAssignableFrom( object.getClass() );
    }

    @Override
    protected boolean objectIsUserGroup( Securable object ) {
        return UserGroup.class.isAssignableFrom( object.getClass() );
    }

    @Override
    protected boolean specialCaseForAssociationFollow( Object object, String property ) {
        return BioAssay.class.isAssignableFrom( object.getClass() ) && ( property.equals( "sampleUsed" ) || property
                .equals( "arrayDesignUsed" ) );
    }

    @Override
    protected boolean specialCaseToKeepPrivateOnCreation( Securable object ) {
        return super.specialCaseToKeepPrivateOnCreation( object )
                || object instanceof UserGroup
                || object instanceof User
                || object instanceof Investigation;
    }
}
