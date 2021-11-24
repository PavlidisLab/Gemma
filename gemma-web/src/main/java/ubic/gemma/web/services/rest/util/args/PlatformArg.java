package ubic.gemma.web.services.rest.util.args;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.ObjectFilterException;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.Filters;
import ubic.gemma.persistence.util.ObjectFilter;
import ubic.gemma.persistence.util.Slice;
import ubic.gemma.persistence.util.Sort;

import java.lang.reflect.Array;

/**
 * Mutable argument type base class for dataset (ExpressionExperiment) API.
 *
 * @author tesarst
 */
@Schema(subTypes = { PlatformIdArg.class, PlatformStringArg.class })
public abstract class PlatformArg<T> extends AbstractEntityArg<T, ArrayDesign, ArrayDesignService> {

    protected PlatformArg( T value ) {
        super( ArrayDesign.class, value );
    }

    public PlatformArg( String message, Throwable cause ) {
        super( ArrayDesign.class, message, cause );
    }

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param  s the request dataset argument.
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static PlatformArg<?> valueOf( final String s ) {
        if ( StringUtils.isBlank( s ) ) {
            return new PlatformStringArg( "Platform identifier cannot be null or empty.", null );
        }
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

        Filters filters = new Filters();
        filters.add( new ObjectFilter[] { service.getObjectFilter( "id", ObjectFilter.Operator.eq, ad.getId().toString() ) } );
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
        Filters filters = new Filters() {
            {
                add( new ObjectFilter[] {
                        new ObjectFilter( csService.getObjectAlias(), "arrayDesign", ArrayDesign.class, ObjectFilter.Operator.eq, ad ) } );
            }
        };
        return csService.loadValueObjectsPreFilter( filters, null, offset, limit );

    }
}
