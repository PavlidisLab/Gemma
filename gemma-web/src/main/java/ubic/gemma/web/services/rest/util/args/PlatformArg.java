package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.SneakyThrows;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.ObjectFilterException;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable argument type base class for dataset (ExpressionExperiment) API.
 *
 * @author tesarst
 */
@Schema(anyOf = { PlatformIdArg.class, PlatformStringArg.class })
public abstract class PlatformArg<T> extends AbstractEntityArg<T, ArrayDesign, ArrayDesignService> {

    PlatformArg( T value ) {
        super( value );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param  s the request dataset argument.
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static PlatformArg<?> valueOf( final String s ) {
        try {
            return new PlatformIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new PlatformStringArg( s );
        }
    }

    /**
     * Retrieves the Datasets of the Platform that this argument represents.
     *
     * @param  service   service that will be used to retrieve the persistent AD object.
     * @param  eeService service to use to retrieve the EEs.
     * @return a collection of Datasets that the platform represented by this argument contains.
     */
    @SneakyThrows(ObjectFilterException.class)
    public Slice<ExpressionExperimentValueObject> getExperiments( ArrayDesignService service,
            ExpressionExperimentService eeService, int limit, int offset ) {
        ArrayDesign ad = this.getEntity( service );

        List<ObjectFilter[]> filters = new ArrayList<>( 1 );
        filters.add( new ObjectFilter[] { service.getObjectFilter( "id", ObjectFilter.Operator.is, ad.getId().toString() ) } );
        return eeService.loadValueObjectsPreFilter( filters, Sort.by( "id" ), offset, limit );
    }

    /**
     * Retrieves the Elements of the Platform that this argument represents.
     *
     * @param  service service that will be used to retrieve the persistent AD object.
     * @return a collection of Composite Sequence VOs that the platform represented by this argument contains.
     */
    public Slice<CompositeSequenceValueObject> getElements( ArrayDesignService service,
            CompositeSequenceService csService, int limit, int offset ) {
        final ArrayDesign ad = this.getEntity( service );
        ArrayList<ObjectFilter[]> filters = new ArrayList<ObjectFilter[]>() {
            {
                add( new ObjectFilter[] {
                        new ObjectFilter( csService.getObjectAlias(), "arrayDesign", ArrayDesign.class, ObjectFilter.Operator.is, ad ) } );
            }
        };
        return csService.loadValueObjectsPreFilter( filters, null, offset, limit );

    }

    @Override
    public String getEntityName() {
        return "Platform";
    }
}
