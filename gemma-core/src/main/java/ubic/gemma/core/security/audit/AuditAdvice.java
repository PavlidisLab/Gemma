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

import lombok.extern.apachecommons.CommonsLog;
import org.apache.commons.collections4.CollectionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.engine.spi.CascadingAction;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ubic.gemma.core.security.authentication.UserManager;
import ubic.gemma.model.common.auditAndSecurity.*;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.persistence.service.common.auditAndSecurity.curation.GenericCuratableDao;
import ubic.gemma.persistence.util.Pointcuts;

import javax.annotation.ParametersAreNonnullByDefault;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ubic.gemma.core.util.StringUtils.abbreviateInBytes;

/**
 * Manage audit trails on objects.
 * <p>
 * When an auditable entity is created, updated or deleted, this advice will automatically populate the audit trail with
 * appropriate audit events before the operation occurs.
 * @author pavlidis
 * @author poirigui
 */
@Aspect
@Component
@ParametersAreNonnullByDefault
// Note that we have a special logger configured for this class, so remove events get stored.
@CommonsLog
public class AuditAdvice {

    private enum OperationType {
        CREATE,
        UPDATE,
        SAVE,
        DELETE
    }

    @Autowired
    private UserManager userManager;

    @Autowired
    private GenericCuratableDao curatableDao;

    @Autowired
    private SessionFactory sessionFactory;

    private final AuditLogger auditLogger = new AuditLogger();

    /**
     * Perform the audit advice on when entities are created.
     * <p>
     * This audit will cascade on {@link CascadeStyle#PERSIST}.
     *
     * @see Pointcuts#creator()
     * @see ubic.gemma.persistence.service.BaseDao#create(Object)
     * @see ubic.gemma.persistence.service.BaseDao#create(Collection)
     */
    @Order(4)
    @Before("ubic.gemma.persistence.util.Pointcuts.creator()")
    public void doCreateAdvice( JoinPoint pjp ) {
        doAuditAdvice( pjp, OperationType.CREATE );
    }

    /**
     * Perform auditing when entities are updated.
     * <p>
     * This audit will cascade on {@link CascadeStyle#UPDATE}.
     *
     * @see Pointcuts#updater()
     * @see ubic.gemma.persistence.service.BaseDao#update(Object)
     * @see ubic.gemma.persistence.service.BaseDao#update(Collection)
     */
    @Order(4)
    @Before("ubic.gemma.persistence.util.Pointcuts.updater()")
    public void doUpdateAdvice( JoinPoint pjp ) {
        doAuditAdvice( pjp, OperationType.UPDATE );
    }

    /**
     * Perform auditing when entities are saved.
     * <p>
     * This audit will cascade on {@link CascadeStyle#PERSIST} if the audited entity is transient else
     * {@link CascadeStyle#MERGE}.
     *
     * @see Pointcuts#saver()
     * @see ubic.gemma.persistence.service.BaseDao#save(Object)
     * @see ubic.gemma.persistence.service.BaseDao#save(Collection)
     */
    @Order(4)
    @Before("ubic.gemma.persistence.util.Pointcuts.saver()")
    public void doSaveAdvice( JoinPoint pjp ) {
        doAuditAdvice( pjp, OperationType.SAVE );
    }

    /**
     * Perform auditing when entities are deleted.
     * <p>
     * This audit will cascade on {@link CascadeStyle#DELETE}.
     *
     * @see Pointcuts#deleter()
     * @see ubic.gemma.persistence.service.BaseDao#remove(Object)
     * @see ubic.gemma.persistence.service.BaseDao#remove(Collection)
     */
    @Order(4)
    @Before("ubic.gemma.persistence.util.Pointcuts.deleter()")
    public void doDeleteAdvice( JoinPoint pjp ) {
        doAuditAdvice( pjp, OperationType.DELETE );
    }

    private void doAuditAdvice( JoinPoint pjp, OperationType operationType ) {
        MethodSignature signature = ( MethodSignature ) pjp.getSignature();
        if ( signature.getMethod().getAnnotation( IgnoreAudit.class ) != null ) {
            AuditAdvice.log.trace( String.format( "Not auditing %s annotated with %s.", signature, IgnoreAudit.class.getName() ) );
            return;
        }
        Object[] args = pjp.getArgs();
        // only audit the first argument
        if ( args.length < 1 )
            return;
        Object arg = args[0];
        if ( arg == null ) {
            AuditAdvice.log.warn( String.format( "Cannot audit a null object passed as first argument of %s.", signature ) );
            return;
        }
        // Hibernate might decide to flush the session when retrieving the current user. This is not desirable because
        // the entity being updated might not be fully initialized.
        // See https://github.com/PavlidisLab/Gemma/issues/1093 for an example of this happening.
        User user;
        FlushMode previousFlushMode = sessionFactory.getCurrentSession().getFlushMode();
        try {
            sessionFactory.getCurrentSession().setFlushMode( FlushMode.MANUAL );
            user = userManager.getCurrentUser();
        } finally {
            sessionFactory.getCurrentSession().setFlushMode( previousFlushMode );
        }
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
    private void processAuditable( Signature method, OperationType operationType, Auditable auditable, User user, Date date ) {
        if ( AuditAdvice.log.isTraceEnabled() ) {
            AuditAdvice.log.trace( String.format( "***********  Start Audit %s of %s by %s (via %s) *************", operationType, auditable, user.getUserName(), method ) );
        }
        if ( operationType == OperationType.CREATE ) {
            this.addCreateAuditEvent( method, auditable, user, date );
        } else if ( operationType == OperationType.UPDATE ) {
            this.addUpdateAuditEvent( method, auditable, user, date );
        } else if ( operationType == OperationType.SAVE ) {
            this.addSaveAuditEvent( method, auditable, user, date );
        } else if ( operationType == OperationType.DELETE ) {
            this.addDeleteAuditEvent( method, auditable, user, date );
        } else {
            throw new IllegalArgumentException( String.format( "Unsupported operation type %s.", operationType ) );
        }
        if ( AuditAdvice.log.isTraceEnabled() )
            AuditAdvice.log.trace( String.format( "============  End Audit %s of %s by %s (via %s) ==============", operationType, auditable, user.getUserName(), method ) );
    }


    /**
     * Adds 'create' AuditEvent to audit trail of the passed Auditable.
     */
    private void addCreateAuditEvent( Signature method, Auditable auditable, User user, Date date ) {
        addAuditEvent( method, auditable, AuditAction.CREATE, "", user, date );
        cascadeAuditEvent( method, AuditAction.CREATE, auditable, user, date, CascadingAction.PERSIST );
    }

    private void addSaveAuditEvent( Signature method, Auditable auditable, User user, Date date ) {
        AuditAction auditAction;
        CascadingAction cascadingAction;
        if ( auditable.getId() != null ) {
            auditAction = AuditAction.UPDATE;
            cascadingAction = CascadingAction.MERGE;
        } else {
            auditAction = AuditAction.CREATE;
            cascadingAction = CascadingAction.PERSIST;
        }
        addAuditEvent( method, auditable, auditAction, "", user, date );
        // we only propagate a CREATE event through cascade for entities that were created in the save
        // Note: CREATE events are skipped if the audit trail already contains one
        cascadeAuditEvent( method, AuditAction.CREATE, auditable, user, date, cascadingAction );
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

                if ( propertyValue == null ) {
                    continue;
                }

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
                this.addAuditEvent( method, ( Auditable ) object, auditAction, String.format( " - %s by cascade from %s", auditAction.name(), auditable ), user, date );
            }
        }
    }

    /**
     * Add an audit event.
     */
    private void addAuditEvent( Signature method, Auditable auditable, AuditAction auditAction, String
            cascadedNote, User user, Date date ) {
        String note = String.format( "%s event on entity %s:%d [%s] by %s via %s on %s%s", auditAction, auditable.getClass().getName(), auditable.getId(), auditable, user.getUserName(), method, date, cascadedNote );
        if ( auditable.getAuditTrail().getId() != null ) {
            // persistent, but let's make sure it is part of this session
            auditable.setAuditTrail( ( AuditTrail ) sessionFactory.getCurrentSession().merge( auditable.getAuditTrail() ) );
        }
        if ( auditAction.equals( AuditAction.CREATE ) && !auditable.getAuditTrail().getEvents().isEmpty() ) {
            AuditAdvice.log.trace( String.format( "Skipped %s on %s since its audit trail has already been filled.",
                    AuditAction.CREATE, auditable ) );
            return;
        }
        AuditEvent auditEvent = AuditEvent.Factory.newInstance( date, auditAction, abbreviateInBytes( note, "…", AuditEvent.MAX_NOTE_LENGTH, StandardCharsets.UTF_8 ), null, user, null );
        auditable.getAuditTrail().getEvents().add( auditEvent );
        if ( auditable instanceof Curatable && auditAction == AuditAction.UPDATE ) {
            curatableDao.updateCurationDetailsFromAuditEvent( ( Curatable ) auditable, auditEvent );
        }
        auditLogger.log( auditable, auditEvent );
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
