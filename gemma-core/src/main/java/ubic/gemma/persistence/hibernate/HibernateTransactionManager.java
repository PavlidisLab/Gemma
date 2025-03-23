package ubic.gemma.persistence.hibernate;

import org.hibernate.SessionFactory;

/**
 * The sole purpose of this class is to limit usages of {@code org.springframework.orm} to this package.
 *
 * @author poirigui
 */
public class HibernateTransactionManager extends org.springframework.orm.hibernate4.HibernateTransactionManager {

    public HibernateTransactionManager( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }
}
