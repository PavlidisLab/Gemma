package ubic.gemma.persistence.service.expression.experiment;

import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.BaseDao;

import java.util.Collection;

public interface StatementDao extends BaseDao<Statement> {

    Collection<Statement> findByPredicate( String value );

    Collection<Statement> findByPredicateLike( String s );

    Collection<Statement> findByPredicateUri( String uri );

    Collection<Statement> findByObject( String value );

    Collection<Statement> findByObjectLike( String s );

    Collection<Statement> findByObjectUri( String uri );
}
