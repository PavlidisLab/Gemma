package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.FilteringService;

@Service
public class GeneArgService extends AbstractEntityArgService<Gene, GeneService> {

    @Autowired
    public GeneArgService( GeneService service ) {
        super( service );
    }
}
