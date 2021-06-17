package ubic.gemma.persistence.persister.analysis;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.analysis.expression.ExpressionAnalysis;
import ubic.gemma.persistence.persister.AbstractPersister;

@Service
public class ExpressionAnalysisPersister extends AbstractPersister<ExpressionAnalysis> {

    @Autowired
    public ExpressionAnalysisPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public <S extends ExpressionAnalysis> S persist( S entity ) {
        return null;
    }
}
