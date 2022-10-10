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
package ubic.gemma.core.security.audit;

import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.*;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.core.util.Pointcuts;
import ubic.gemma.model.common.Auditable;
import ubic.gemma.model.common.auditAndSecurity.AuditAction;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.AuditTrail;
import ubic.gemma.model.common.auditAndSecurity.User;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

/**
 * Manage audit trails on objects.
 * <p>
 * When an auditable entity is created, updated or deleted, this advice will automatically populate the audit trail with
 * appropriate audit events before the operation occurs.
 * <p>
 * The propagation of audit events respects the cascading style. However, since there's no way to determine the cascade
 * style on the original entity, we use {@link CascadingAction#PERSIST} for a {@link Pointcuts#creator()},
 * {@link CascadingAction#SAVE_UPDATE} for an {@link Pointcuts#updater()} and {@link CascadingAction#DELETE} for a
 * {@link Pointcuts#deleter()}.
 *
 * @author pavlidis
 */
@Aspect
@Component
@ParametersAreNonnullByDefault
public class AuditAdvice {

    // Note that we have a special logger configured for this class, so remove events get stored.
    private static final Logger log = LoggerFactory.getLogger( AuditAdvice.class.getName() );

    @Autowired
    private UserManager userManager;

    @Autowired
    private SessionFactory sessionFactory;

    @Before("ubic.gemma.core.util.Pointcuts.creator()")
    public void doCreateAdvice( JoinPoint pjp ) {
        doAuditAdvice( pjp, AuditAction.CREATE );
    }

    @Before("ubic.gemma.core.util.Pointcuts.updater()")
    public void doUpdateAdvice( JoinPoint pjp ) {
        doAuditAdvice( pjp, AuditAction.UPDATE );
    }

    @Before("ubic.gemma.core.util.Pointcuts.deleter()")
    public void doDeleteAdvice( JoinPoint pjp ) {
        doAuditAdvice( pjp, AuditAction.DELETE );
    }

    private void doAuditAdvice( JoinPoint pjp, AuditAction operationType ) {
        Signature signature = pjp.getSignature();
        Object[] args = pjp.getArgs();
        // only audit the first argument
        if ( args.length < 1 )
            return;
        Object arg = args[0];
        if ( arg == null ) {
            AuditAdvice.log.warn( String.format( "Cannot audit a null object passed as first argument of %s.", signature ) );
            return;
        }
        User user = userManager.getCurrentUser();
        if ( user == null ) {
            AuditAdvice.log.info( String.format( "User could not be determined (anonymous?), audit will be skipped for %s.", signature ) );
            return;
        }
        // ensures that all created audit event happens at the same time
        Date date = new Date();
        for ( Auditable auditable : extractAuditables( arg ) ) {
            this.processAuditable( signature, operationType, auditable, user, date );
        }
    }

    /**
     * Process auditing on the object.
     */
    private void processAuditable( Signature method, AuditAction auditAction, Auditable auditable, User user, Date date ) {
        if ( AuditAdvice.log.isTraceEnabled() ) {
            AuditAdvice.log.trace( String.format( "***********  Start Audit %s of %s by %s (via %s) *************", auditAction, auditable, user.getUserName(), method ) );
        }
        if ( AuditAction.CREATE.equals( auditAction ) ) {
            this.addCreateAuditEvent( method, auditable, user, date );
        } else if ( AuditAction.UPDATE.equals( auditAction ) ) {
            this.addUpdateAuditEvent( method, auditable, user, date );
        } else if ( AuditAction.DELETE.equals( auditAction ) ) {
            this.addDeleteAuditEvent( method, auditable, user, date );
        } else {
            throw new IllegalArgumentException( String.format( "Unsupported audit action %s.", auditAction ) );
        }
        if ( AuditAdvice.log.isTraceEnabled() )
            AuditAdvice.log.trace( String.format( "============  End Audit %s of %s by %s (via %s) ==============", auditAction, auditable, user.getUserName(), method ) );
    }


    /**
     * Adds 'create' AuditEvent to audit trail of the passed Auditable.
     */
    private void addCreateAuditEvent( Signature method, Auditable auditable, User user, Date date ) {
        addAuditEvent( method, auditable, AuditAction.CREATE, "", user, date );
        cascadeAuditEvent( method, AuditAction.CREATE, auditable, user, date, CascadingAction.PERSIST );
    }

    /**
     * Add an 'update' AuditEvent to the audit trail of the given Auditable entity.
     * <p>
     * This method cascades a {@link AuditAction#CREATE} to make sure that entities created in the process have their
     * initial create event. Thus, if the update is on an expression experiment that has a new Characteristic, the
     * Characteristic will have a 'create' event, and the EE will get an added update event (via the addUpdateAuditEvent
     * call elsewhere, not here).
     */
    private void addUpdateAuditEvent( Signature method, Auditable auditable, User user, Date date ) {
        if ( auditable.getId() == null ) {
            throw new IllegalArgumentException( String.format( "Transient instance passed to update auditing [%s on %s by %s]", method, auditable, user.getUserName() ) );
        }
        addAuditEvent( method, auditable, AuditAction.UPDATE, "", user, date );
        // we only propagate a CREATE event through cascade for entities that were created in the update
        // Note: CREATE events are skipped if the audit trail already contains one
        cascadeAuditEvent( method, AuditAction.CREATE, auditable, user, date, CascadingAction.SAVE_UPDATE );
    }

    private void addDeleteAuditEvent( Signature method, Auditable auditable, User user, Date date ) {
        if ( auditable.getId() == null ) {
            throw new IllegalArgumentException( String.format( "Transient instance passed to delete auditing [%s on %s by %s]", method, auditable, user.getUserName() ) );
        }
        addAuditEvent( method, auditable, AuditAction.DELETE, "", user, date );
        cascadeAuditEvent( method, AuditAction.DELETE, auditable, user, date, CascadingAction.DELETE );
    }

    /**
     * Cascade a given audit event through the object structure by navigating auditable entities and collection of
     * auditable entities.
     *
     * @param cascadingAction the Hibernate {@link CascadingAction} that that should be used to determine how auditing
     *                        should cascade to associated entities.
     */
    private void cascadeAuditEvent( Signature method, AuditAction auditAction, Auditable auditable, User user, Date date, CascadingAction cascadingAction ) {
        // use identity hashcode since auditable might rely on a potentially null ID for hashing
        Set<Object> visited = Collections.newSetFromMap( new IdentityHashMap<>() );

        // necessary as ArrayQueue does not accept nulls
        Queue<Object> fringe = new LinkedList<>();
        fringe.add( auditable );

        while ( !fringe.isEmpty() ) {
            Object object = fringe.remove();
            if ( object == null )
                continue;
            if ( visited.contains( object ) )
                continue;
            visited.add( object );
            EntityPersister persister = ( EntityPersister ) sessionFactory.getClassMetadata( Hibernate.getClass( object ) );
            CascadeStyle[] cascadeStyles = persister.getPropertyCascadeStyles();
            String[] propertyNames = persister.getPropertyNames();
            Object[] propertyValues = persister.getPropertyValues( object );
            Type[] propertyTypes = persister.getPropertyTypes();
            for ( int j = 0; j < propertyNames.length; j++ ) {
                CascadeStyle cs = cascadeStyles[j];
                Object propertyValue = propertyValues[j];
                Type propertyType = propertyTypes[j];

                // ensure that the operation performed on the original object cascades as per JPA definition
                // events don't cascade through uninitialized properties
                if ( !cs.doCascade( cascadingAction ) || !Hibernate.isInitialized( propertyValue ) ) {
                    continue;
                }

                if ( propertyType.isEntityType() ) {
                    fringe.add( propertyValue );
                } else if ( propertyType.isCollectionType() ) {
                    fringe.addAll( ( Collection<?> ) propertyValue );
                }
            }
        }

        for ( Object object : visited ) {
            if ( object instanceof Auditable ) {
                this.addAuditEvent( method, ( Auditable ) object, auditAction, String.format( " - %s by cascade from %s", auditAction.getValue(), auditable ), user, date );
            }
        }
    }

    /**
     * Add an audit event.
     */
    private void addAuditEvent( Signature method, Auditable auditable, AuditAction auditAction, String
            cascadedNote, User user, Date date ) {
        String note = String.format( "%s event on entity %s:%d [%s] by %s via %s on %s%s", auditAction, auditable.getClass().getName(), auditable.getId(), auditable, user.getUserName(), method, date, cascadedNote );
        if ( auditable.getAuditTrail() == null ) {
            // transient
            auditable.setAuditTrail( AuditTrail.Factory.newInstance() );
        } else if ( auditable.getAuditTrail().getId() != null ) {
            // persistent, but let's make sure it is part of this session
            auditable.setAuditTrail( ( AuditTrail ) sessionFactory.getCurrentSession().merge( auditable.getAuditTrail() ) );
        }
        if ( auditAction.equals( AuditAction.CREATE ) && !auditable.getAuditTrail().getEvents().isEmpty() ) {
            AuditAdvice.log.trace( String.format( "Skipped %s on %s since its audit trail has already been filled.",
                    AuditAction.CREATE, auditable ) );
            return;
        }
        auditable.getAuditTrail().getEvents().add( AuditEvent.Factory.newInstance( date, auditAction, note, null, user, null ) );
        if ( AuditAdvice.log.isTraceEnabled() ) {
            AuditAdvice.log.trace( String.format( "Audited event: %s on %s:%d by %s",
                    note.length() > 0 ? note : "[no note]", auditable.getClass().getSimpleName(), auditable.getId(), user.getUserName() ) );
        }
    }

    /**
     * Efficiently extract all auditable of a given type in an object's tree.
     * <p>
     * This method traverses {@link Map}, {@link Collection}, {@link Iterable} and Java arrays, but not properties and
     * fields of objects.
     */
    public static Collection<Auditable> extractAuditables( Object object ) {
        // necessary as ArrayQueue does not accept nulls
        Queue<Object> fringe = new LinkedList<>();
        // use identity hashcode since auditable might rely on a potentially null ID for hashing
        Set<Object> visited = Collections.newSetFromMap( new IdentityHashMap<>() );
        Collection<Auditable> found = new ArrayList<>();
        fringe.add( object );
        while ( !fringe.isEmpty() ) {
            Object o = fringe.remove();
            if ( o == null )
                continue;
            if ( visited.contains( o ) )
                continue;
            visited.add( o );
            if ( o instanceof Auditable ) {
                found.add( ( Auditable ) o );
            } else if ( o.getClass().isArray() ) {
                CollectionUtils.addAll( fringe, ( Object[] ) o );
            } else if ( o instanceof Iterable ) {
                CollectionUtils.addAll( fringe, ( Iterable<?> ) o );
            } else if ( o instanceof Map ) {
                CollectionUtils.addAll( fringe, ( ( Map<?, ?> ) o ).keySet() );
                CollectionUtils.addAll( fringe, ( ( Map<?, ?> ) o ).values() );
            }
        }
        return found;
    }
}
