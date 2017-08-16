package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.util.ObjectFilter;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Mutable argument type base class for dataset (ExpressionExperiment) API.
 *
 * @author tesarst
 */
public abstract class PlatformArg<T> extends MutableArg<T, ArrayDesign, ArrayDesignService, ArrayDesignValueObject> {

    /**
     * Used by RS to parse value of request parameters.
     *
     * @param s the request dataset argument.
     * @return instance of appropriate implementation of DatasetArg based on the actual Type the argument represents.
     */
    @SuppressWarnings("unused")
    public static PlatformArg valueOf( final String s ) {
        try {
            return new PlatformIdArg( Long.parseLong( s.trim() ) );
        } catch ( NumberFormatException e ) {
            return new PlatformStringArg( s );
        }
    }

    /**
     * Retrieves the Datasets of the Platform that this argument represents.
     *
     * @param service   service that will be used to retrieve the persistent AD object.
     * @param eeService service to use to retrieve the EEs.
     * @return a collection of Datasets that the platform represented by this argument contains.
     */
    public Collection<ExpressionExperimentValueObject> getExperiments( ArrayDesignService service,
            ExpressionExperimentService eeService, int limit, int offset ) {
        ArrayDesign ad = this.getPersistentObject( service );

        ArrayList<ObjectFilter[]> filters = new ArrayList<>( 1 );
        filters.add( new ObjectFilter[] { new ObjectFilter( "id", ad.getId(), ObjectFilter.is, ObjectFilter.DAO_AD_ALIAS ) } );
        return eeService.loadValueObjectsPreFilter( offset, limit, "id", true, filters );
    }

    /**
     * Retrieves the Elements of the Platform that this argument represents.
     *
     * @param service service that will be used to retrieve the persistent AD object.
     * @return a collection of Composite Sequence VOs that the platform represented by this argument contains.
     */
    public Collection<CompositeSequenceValueObject> getElements( ArrayDesignService service,
            CompositeSequenceService csService, int limit, int offset ) {
        ArrayDesign ad = this.getPersistentObject( service );
        Collection<CompositeSequence> css = ad == null ? null : service.getCompositeSequences( ad, limit, offset );
        Collection<CompositeSequenceValueObject> csVos = new ArrayList<>( css != null ? css.size() : 0 );
        if ( css == null )
            return csVos;
        for ( CompositeSequence cs : css ) {
            CompositeSequenceValueObject csVo = csService.loadValueObject( cs );
            csVo.setGeneMappingSummaries( csService.getGeneMappingSummary( cs ) );
            csVos.add( csVo );
        }
        return csVos;

    }

}
