package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;

@Service
public class CompositeSequenceArgService extends AbstractEntityArgService<CompositeSequence, CompositeSequenceService> {
    @Autowired
    public CompositeSequenceArgService( CompositeSequenceService service ) {
        super( service );
    }

    public CompositeSequence getEntityWithPlatform( CompositeSequenceArg<?> probeArg, ArrayDesign platform ) {
        return checkEntity( probeArg, probeArg.getEntityWithPlatform( service, platform ) );
    }
}
