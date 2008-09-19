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

import org.springframework.security.afterinvocation.AfterInvocationProvider;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.Securable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * For this particular AfterInvocationProvider, composite sequence authorization is determined based on the secured
 * array design acl. ie. composite sequence security is determined from an owning array desgin's security.
 * 
 * @author keshav (based in part on code from Acegi)
 * @version $Id: BasicAclEntryAfterInvocationArrayDesignCollectionFilteringProvider.java,v 1.2 2005/08/17 21:46:32
 *          keshav Exp $
 * @see AfterInvocationProvider
 */
public class AclAfterCollectionCompSeqByArrayDesignFilter extends ByAssociatedObjectFilter {

    protected static final Log logger = LogFactory.getLog( AclAfterCollectionCompSeqByArrayDesignFilter.class );

    public String getProcessConfigAttribute() {
        return "AFTER_ACL_ARRAYDESIGN_COLLECTION_READ";
    }

    /**
     * @param targetDomainObject
     * @return
     */
    protected Securable getDomainObject( Object targetDomainObject ) {
        ArrayDesign domainObject = ( ( CompositeSequence ) targetDomainObject ).getArrayDesign();
        return domainObject;
    }

}
