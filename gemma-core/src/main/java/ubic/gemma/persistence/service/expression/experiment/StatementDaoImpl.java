package ubic.gemma.persistence.service.expression.experiment;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import ubic.gemma.model.expression.experiment.Statement;
import ubic.gemma.persistence.service.AbstractDao;

import java.util.Collection;
import java.util.List;

@Repository
public class StatementDaoImpl extends AbstractDao<Statement> implements StatementDao {

    @Autowired
    public StatementDaoImpl( SessionFactory sessionFactory ) {
        super( Statement.class, sessionFactory );
    }

    @Override
    public Collection<Statement> findByPredicate( String value ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select s from Statement s where s.predicate = :value or s.secondPredicate = :value" )
                .setParameter( "value", value )
                .list();
    }

    @Override
    public Collection<Statement> findByPredicateUri( String uri ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select s from Statement s where s.predicateUri = :uri or s.secondPredicateUri = :uri" )
                .setParameter( "uri", uri )
                .list();
    }

    @Override
    public Collection<Statement> findByObject( String value ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select s from Statement s where s.object = :value or s.secondObject = :value" )
                .setParameter( "value", value )
                .list();
    }

    @Override
    public Collection<Statement> findByObjectUri( String uri ) {
        //noinspection unchecked
        return getSessionFactory().getCurrentSession()
                .createQuery( "select s from Statement s where s.objectUri = :uri or s.secondObjectUri = :uri" )
                .setParameter( "uri", uri )
                .list();
    }
}
