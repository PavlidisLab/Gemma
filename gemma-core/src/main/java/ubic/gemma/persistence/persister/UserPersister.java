package ubic.gemma.persistence.persister;

import gemma.gsec.model.User;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserPersister extends AbstractPersister<User> {

    @Autowired
    public UserPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public <S extends User> S persist( S entity ) {
        return null;
    }
}
