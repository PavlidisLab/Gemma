package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.Characteristic;

@Service
public class CharacteristicPersister extends AbstractPersister<Characteristic> {

    @Autowired
    public CharacteristicPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    public <S extends Characteristic> S persist( S entity ) {
        return null;
    }
}
