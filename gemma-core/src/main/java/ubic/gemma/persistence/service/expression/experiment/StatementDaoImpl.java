package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.AbstractDao;

@Repository
public class StatementDaoImpl extends AbstractDao<Statement> implements StatementDao {

    @Autowired
    public StatementDaoImpl( SessionFactory sessionFactory ) {
        super( Statement.class, sessionFactory );
    }
}
