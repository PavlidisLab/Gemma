package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;

@Service
public class DatabaseEntryArgService extends AbstractEntityArgService<DatabaseEntry, DatabaseEntryService> {

    @Autowired
    public DatabaseEntryArgService( DatabaseEntryService service ) {
        super( service );
    }
}
