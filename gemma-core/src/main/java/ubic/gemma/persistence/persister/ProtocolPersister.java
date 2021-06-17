package ubic.gemma.persistence.persister;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.common.protocol.Protocol;
import ubic.gemma.persistence.service.common.protocol.ProtocolDao;

@Service
public class ProtocolPersister extends AbstractPersister<Protocol> {

    @Autowired
    private ProtocolDao protocolDao;

    @Autowired
    public ProtocolPersister( SessionFactory sessionFactory ) {
        super( sessionFactory );
    }

    @Override
    @Transactional
    public Protocol persist( Protocol protocol ) {
        if ( protocol == null )
            return null;
        this.fillInProtocol( protocol );
        // I changed this to create instead of findOrCreate because in
        // practice protocols are not shared; we use them to store information about analyses we run. PP2017
        return protocolDao.create( protocol );
    }

    private void fillInProtocol( Protocol protocol ) {
        if ( !this.isTransient( protocol ) )
            return;
        if ( protocol == null ) {
            AbstractPersister.log.warn( "Null protocol" );
        }

    }
}
