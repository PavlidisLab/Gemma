package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;

@Service
public class PlatformArgService extends AbstractEntityArgService<ArrayDesign, ArrayDesignService> {
    @Autowired
    public PlatformArgService( ArrayDesignService service ) {
        super( service );
    }
}
