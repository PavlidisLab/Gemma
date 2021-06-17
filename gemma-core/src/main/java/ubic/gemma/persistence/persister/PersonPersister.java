package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Person;
import ubic.gemma.persistence.service.common.auditAndSecurity.PersonDao;

@Service
public class PersonPersister extends AbstractPersister<Person> {

    @Autowired
    private PersonDao personDao;

    @Autowired
    public PersonPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Transactional
    public Person persist( Person person ) {
        if ( person == null )
            return null;
        return this.personDao.findOrCreate( person );
    }
}
