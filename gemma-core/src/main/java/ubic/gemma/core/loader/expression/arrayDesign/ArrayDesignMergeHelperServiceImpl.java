/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.core.loader.expression.arrayDesign;

import java.util.Collection;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignMergeEventImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.persister.PersisterHelper;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.designElement.CompositeSequence;

/**
 * @author Paul
 */
@Service
public class ArrayDesignMergeHelperServiceImpl implements ArrayDesignMergeHelperService {

    @Autowired
    private ArrayDesignService arrayDesignService;
    @Autowired
    private Persister arrayDesignPersiter;

    @Override
    @Transactional
    public ArrayDesign persistMerging( ArrayDesign result, ArrayDesign arrayDesign,
            Collection<ArrayDesign> otherArrayDesigns, boolean mergeWithExisting,
            Collection<CompositeSequence> newProbes ) {

        for ( ArrayDesign otherArrayDesign : otherArrayDesigns ) {
            otherArrayDesign.setMergedInto( result );
            audit( otherArrayDesign, "Merged into " + result );
        }

        result.getMergees().addAll( otherArrayDesigns );
        result.getCompositeSequences().addAll( newProbes );

        if ( mergeWithExisting ) {
            /* we're merging into the given arrayDesign. */
            assert result.equals( arrayDesign );
            assert result.getId() != null;
            assert !result.getCompositeSequences().isEmpty();

            audit( result, "More array design(s) added to merge" );

            arrayDesignService.update( result );
        } else {
            /* we're making a new one. In this case arrayDesign is treated just like the other ones, so we pile it in. */

            assert result.getId() == null;

            result.getMergees().add( arrayDesign );
            arrayDesign.setMergedInto( result );
            audit( arrayDesign, "Merged into " + result );

            result = ( ArrayDesign ) arrayDesignPersiter.persist( result );
        }

        return result;
    }

    /**
     * Add an ArrayDesignMergeEvent event to the audit trail. Does not persist it.
     * 
     * @param arrayDesign
     */
    private void audit( ArrayDesign arrayDesign, String note ) {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance( new Date(), AuditAction.UPDATE, note, null, null,
                new ArrayDesignMergeEventImpl() );
        arrayDesign.getAuditTrail().addEvent( auditEvent );
    }

}
