package ubic.gemma.persistence.persister.analysis;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.coexpression.CoexpressionAnalysis;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.persistence.persister.AbstractPersister;
import ubic.gemma.persistence.persister.Persister;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisDao;

@Service
public class CoexpressionAnalysisPersister extends AbstractPersister<CoexpressionAnalysis> {

    @Autowired
    private CoexpressionAnalysisDao coexpressionAnalysisDao;

    @Autowired
    private Persister<Protocol> protocolPersister;

    @Autowired
    private Persister<BioAssaySet> bioAssaySetPersister;

    @Autowired
    public CoexpressionAnalysisPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public CoexpressionAnalysis persist( CoexpressionAnalysis entity ) {
        if ( entity == null )
            return null;
        if ( !this.isTransient( entity ) )
            return entity;

        entity.setProtocol( this.protocolPersister.persist( entity.getProtocol() ) );
        if ( bioAssaySetPersister.isTransient( entity.getExperimentAnalyzed() ) ) {
            throw new IllegalArgumentException( "Persist the experiment before running analyses on it" );
        }

        return coexpressionAnalysisDao.create( entity );
    }
}
