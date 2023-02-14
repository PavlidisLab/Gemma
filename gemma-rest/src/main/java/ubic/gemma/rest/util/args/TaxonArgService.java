package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

@Service
public class TaxonArgService extends AbstractEntityArgService<Taxon, TaxonService> {
    @Autowired
    public TaxonArgService( TaxonService service ) {
        super( service );
    }
}
