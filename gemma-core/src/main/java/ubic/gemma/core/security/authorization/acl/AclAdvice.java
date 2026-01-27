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
import gemma.gsec.acl.ObjectTransientnessRetrievalStrategy;
import gemma.gsec.acl.ParentIdentityRetrievalStrategy;
import gemma.gsec.acl.domain.AclService;
import gemma.gsec.model.Securable;
import gemma.gsec.model.User;
import gemma.gsec.model.UserGroup;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.acls.model.ObjectIdentityRetrievalStrategy;
import org.springframework.stereotype.Component;
import ubic.gemma.model.analysis.Investigation;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.curation.CurationDetails;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.util.Pointcuts;

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
    public AclAdvice( AclService aclService, SessionFactory sessionFactory,
            ObjectIdentityRetrievalStrategy objectIdentityRetrievalStrategy,
            ParentIdentityRetrievalStrategy parentIdentityRetrievalStrategy,
            ObjectTransientnessRetrievalStrategy objectTransientnessRetrievalStrategy ) {
        super( aclService, sessionFactory, objectIdentityRetrievalStrategy, parentIdentityRetrievalStrategy,
                objectTransientnessRetrievalStrategy );
    }

    @Override
    protected boolean canSkipAclCheck( Object object ) {
        return object instanceof AuditTrail || object instanceof CurationDetails;
    }

    @Override
    protected boolean canSkipAssociationCheck( Object object, String propertyName ) {

        /*
         * If this is an expression experiment, don't go down the data vectors - it has no securable associations and
         * would be expensive to traverse.
         */
        if ( object instanceof ExpressionExperiment
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
        if ( object instanceof ArrayDesign && propertyName.equals( "compositeSequences" ) ) {
            if ( AclAdvice.log.isTraceEnabled() )
                AclAdvice.log.trace( "Skipping checking acl on probes on " + object );
            return true;
        }

        return false;
    }

    @Override
    protected boolean canFollowAssociation( Object object, String property ) {
        return object instanceof BioAssay && ( property.equals( "sampleUsed" ) || property.equals( "arrayDesignUsed" ) );
    }

    @Override
    protected boolean isKeepPrivateOnCreation( Securable object ) {
        return super.isKeepPrivateOnCreation( object )
                || object instanceof UserGroup
                || object instanceof User
                || object instanceof Investigation;
    }
}
