package ubic.gemma.persistence.service.common.auditAndSecurity.curation;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.common.auditAndSecurity.AuditEvent;
import ubic.gemma.model.common.auditAndSecurity.curation.Curatable;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;

import java.util.List;

@Repository
public class GenericCuratableDaoImpl implements GenericCuratableDao {

    @Autowired
    private ArrayDesignDao arrayDesignDao;

    @Autowired
    private ExpressionExperimentDao expressionExperimentDao;

    @Override
    public List<Long> loadTroubledIds() {
        throw new UnsupportedOperationException( "Cannot load troubled IDs for an unknown entity." );
    }

    @Override
    public void updateCurationDetailsFromAuditEvent( Curatable auditable, AuditEvent auditEvent ) {
        if ( auditable instanceof ArrayDesign ) {
            arrayDesignDao.updateCurationDetailsFromAuditEvent( ( ArrayDesign ) auditable, auditEvent );
        } else if ( auditable instanceof ExpressionExperiment ) {
            expressionExperimentDao.updateCurationDetailsFromAuditEvent( ( ExpressionExperiment ) auditable, auditEvent );
        } else {
            throw new UnsupportedOperationException( String.format( "Updating curation details of %s is not supported.",
                    auditable.getClass().getName() ) );
        }
    }
}
