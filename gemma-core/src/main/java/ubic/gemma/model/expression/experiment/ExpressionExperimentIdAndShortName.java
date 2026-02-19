package ubic.gemma.model.expression.experiment;

import lombok.Value;
import ubic.gemma.model.common.Identifiable;

/**
 * A minimalistic projection of an {@link ExpressionExperiment}.
 *
 * @author poirigui
 */
@Value
public class ExpressionExperimentIdAndShortName implements Identifiable {

    Long id;
    String shortName;
}
