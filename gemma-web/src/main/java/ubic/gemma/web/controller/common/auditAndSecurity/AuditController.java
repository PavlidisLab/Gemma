/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditEventValueObject;
import ubic.gemma.model.common.auditAndSecurity.Auditable;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.util.EntityDelegator;

import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * This is required solely for exposing auditables to remote services would try to marshall the abstract class
 * Auditable.
 *
 * @author pavlidis
 */
@Controller
public class AuditController {

    private static final Log log = LogFactory.getLog( AuditController.class.getName() );

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @SuppressWarnings("unchecked")
    public void addAuditEvent( EntityDelegator<? extends Auditable> e, String auditEventType, String comment, String detail ) {
        Auditable entity = this.getAuditable( e );
        if ( entity == null ) {
            AuditController.log.warn( "Couldn't find Auditable represented by " + e );
            return;
        }

        Class<?> clazz;
        try {
            clazz = Class.forName( "ubic.gemma.model.common.auditAndSecurity.eventType." + auditEventType );
        } catch ( ClassNotFoundException e1 ) {
            throw new RuntimeException( "Unknown event type: " + auditEventType );
        }

        AuditEvent auditEvent = auditTrailService.addUpdateEvent( entity, ( Class<? extends AuditEventType> ) clazz, comment, detail );
        if ( auditEvent == null ) {
            AuditController.log.error( "Persisting the audit event failed! On auditable id " + entity.getId() );
        } else {
            AuditController.log.info( String.format( "created new event: %s on %s.", auditEvent, entity ) );
        }
    }

    public Collection<AuditEventValueObject> getEvents( EntityDelegator<? extends Auditable> e ) {
        Collection<AuditEventValueObject> result = new HashSet<>();

        Auditable entity = this.getAuditable( e );

        if ( entity == null ) {
            return result;
        }
        assert entity.getAuditTrail().getId() != null;

        return auditEventService.getEventsWithType( entity )
                .stream()
                .map( AuditEventValueObject::new )
                .collect( Collectors.toSet() );
    }

    private Auditable getAuditable( EntityDelegator<? extends Auditable> e ) {
        if ( e == null || e.getId() == null )
            return null;
        if ( e.getClassDelegatingFor() == null )
            return null;

        Class<?> clazz;
        Auditable result;
        try {
            clazz = Class.forName( e.getClassDelegatingFor() );
        } catch ( ClassNotFoundException e1 ) {
            throw new RuntimeException( e1 );
        }
        if ( ExpressionExperiment.class.isAssignableFrom( clazz ) ) {
            result = expressionExperimentService.load( e.getId() );
        } else if ( ArrayDesign.class.isAssignableFrom( clazz ) ) {
            result = arrayDesignService.load( e.getId() );
        } else {
            AuditController.log.warn( "We don't support that class yet, sorry" );
            return null;
        }

        if ( result == null ) {
            AuditController.log.warn( "Entity with id = " + e.getId() + " not found" );
        }
        return result;
    }
}
