package ubic.gemma.web.services.rest.util.args;

import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;

import java.util.Arrays;
import java.util.List;

public class FactorValueArrayArg extends AbstractEntityArrayArg<FactorValue, FactorValueService> {

    public FactorValueArrayArg( List<String> values ) {
        super( values );
    }

    @Override
    protected Class<? extends AbstractEntityArg> getEntityArgClass() {
        return FactorValueArg.class;
    }

    @Override
    protected String getObjectDaoAlias() {
        return null;
    }

    public static FactorValueArrayArg valueOf( String s ) {
        return new FactorValueArrayArg( Arrays.asList( splitString( s ) ) );
    }

}
