/*
 * The Gemma project
 * 
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.security.afterInvocation;

import org.acegisecurity.afterinvocation.AfterInvocationProvider;

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.expression.bioAssayData.DataVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * Filter collections of DesignElementDataVectors or DataVectorValueObjects based on the permissions of the associated
 * ExpressionExperiment(s).
 * 
 * @author pavlidis (based in part on code from Acegi)
 * @version $Id: BasicAclEntryAfterInvocationArrayDesignCollectionFilteringProvider.java,v 1.2 2005/08/17 21:46:32
 *          keshav Exp $
 * @see AfterInvocationProvider
 */
public class AclAfterCollectionDataVectorByExpressionExperimentFilter extends ByAssociatedObjectFilter {

    public String getProcessConfigAttribute() {
        return "AFTER_ACL_DATAVECTOR_COLLECTION_READ";
    }

    /**
     * @param targetDomainObject
     * @return
     */
    protected Securable getDomainObject( Object targetDomainObject ) {
        ExpressionExperiment domainObject = null;
        if ( targetDomainObject instanceof DesignElementDataVector ) {
            domainObject = ( ( DesignElementDataVector ) targetDomainObject ).getExpressionExperiment();
        } else if ( targetDomainObject instanceof DataVectorValueObject ) {
            domainObject = ( ( DataVectorValueObject ) targetDomainObject ).getExpressionExperiment();
        }
        return domainObject;
    }

}
