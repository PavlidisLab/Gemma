package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.auditAndSecurity.Contact;
import ubic.gemma.persistence.service.common.auditAndSecurity.ContactDao;

@Service
public class ContactPersister extends AbstractPersister<Contact> {

    @Autowired
    private ContactDao contactDao;

    @Autowired
    public ContactPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public Contact persist( Contact contact ) {
        if ( contact == null )
            return null;
        return this.contactDao.findOrCreate( contact );
    }
}
