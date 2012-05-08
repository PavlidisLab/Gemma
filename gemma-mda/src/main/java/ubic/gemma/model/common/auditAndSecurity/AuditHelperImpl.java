package ubic.gemma.model.common.auditAndSecurity;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.model.common.Auditable;

@Component
public class AuditHelperImpl implements AuditHelper {

    @Autowired
    AuditTrailDao auditTrailDao;

    @Autowired
    StatusDao statusDao;
       
    public AuditEvent addCreateAuditEvent( Auditable auditable, String note, User user ) {

        this.addAuditTrailIfNeeded( auditable );
        this.addStatusIfNeeded( auditable );

        assert auditable.getAuditTrail() != null;
        assert auditable.getAuditTrail().getEvents().size() == 0;
        
        try {
            AuditEvent auditEvent = AuditEvent.Factory.newInstance();
            auditEvent.setDate( new Date() );
            auditEvent.setAction( AuditAction.CREATE );
            auditEvent.setNote( note );
            this.statusDao.update( auditable, null );
            
            return this.auditTrailDao.addEvent( auditable, auditEvent );
        } catch ( Throwable th ) {
            throw new AuditTrailServiceException(
                    "Error performing 'AuditTrailService.addUpdateEvent(Auditable auditable, String note)' --> "
                    + th, th );
        }
        
    }
       
    private AuditTrail addAuditTrailIfNeeded( Auditable auditable ) {
        if ( auditable.getAuditTrail() == null ) {

            try {

                AuditTrail auditTrail =  AuditTrail.Factory.newInstance(); 
                auditable.setAuditTrail( auditTrailDao.create( auditTrail ) );

            } catch ( Exception e ) {

                /*
                 * This can happen if we hit an auditable during a read-only event: programming error.
                 */
                throw new IllegalStateException( "Invalid attempt to create an audit trail on: " + auditable, e );
            }

        }

        return auditable.getAuditTrail();
    }
   
    private void addStatusIfNeeded( Auditable auditable ) {
        if ( auditable.getStatus() == null ) {
            statusDao.initializeStatus( auditable );
            assert auditable.getStatus() != null && auditable.getStatus().getId() != null;
        }
    }


    @Override
    public AuditEvent addUpdateAuditEvent( Auditable auditable, String note, User user ) {
        AuditEvent auditEvent = AuditEvent.Factory.newInstance();
        auditEvent.setDate( new Date() );
        auditEvent.setAction( AuditAction.UPDATE );
        auditEvent.setNote( note );
        this.statusDao.update( auditable, null );
        return this.auditTrailDao.addEvent( auditable, auditEvent );
    }

    
}
