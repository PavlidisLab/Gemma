package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.core.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

/**
 * Created by tesarst on 24/05/17.
 * Long argument type for taxon API, referencing the Taxon ID.
 */
public class DatasetIdArg extends DatasetArg<Long> {

    /**
     * @param l intentionally primitive type, so the value property can never be null.
     */
    DatasetIdArg( long l ) {
        this.value = l;
    }

    @Override
    protected ExpressionExperiment getPersistentObject( ExpressionExperimentService service ) {
        return service.load( this.value );
    }

    @Override
    protected ExpressionExperimentValueObject getValueObject( ExpressionExperimentService service ) {
        return service.loadValueObject( value );
    }
}
