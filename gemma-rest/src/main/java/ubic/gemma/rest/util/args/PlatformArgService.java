package ubic.gemma.rest.util.args;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filter;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.Slice;

@Service
public class PlatformArgService extends AbstractEntityArgService<ArrayDesign, ArrayDesignService> {

    private final ExpressionExperimentService eeService;
    private final CompositeSequenceService csService;

    @Autowired
    public PlatformArgService( ArrayDesignService service, ExpressionExperimentService eeService, CompositeSequenceService csService ) {
        super( service );
        this.eeService = eeService;
        this.csService = csService;
    }

    /**
     * Retrieves the Datasets of the Platform that this argument represents.
     *
     * @return a collection of Datasets that the platform represented by this argument contains.
     */
    public Slice<ExpressionExperimentValueObject> getExperiments( PlatformArg<?> arg, int limit, int offset ) {
        ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( eeService.getFilter( "bioAssays.arrayDesignUsed.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        return eeService.loadValueObjects( filters, eeService.getSort( "bioAssays.arrayDesignUsed.id", null ), offset, limit );
    }

    /**
     * Retrieves the Elements of the Platform that this argument represents.
     *
     * @param service service that will be used to retrieve the persistent AD object.
     * @return a collection of Composite Sequence VOs that the platform represented by this argument contains.
     */
    public Slice<CompositeSequenceValueObject> getElements( PlatformArg<?> arg, int limit, int offset ) {
        final ArrayDesign ad = this.getEntity( arg );
        Filters filters = Filters.by( csService.getFilter( "arrayDesign.id", Long.class, Filter.Operator.eq, ad.getId() ) );
        return csService.loadValueObjects( filters, null, offset, limit );
    }
}
