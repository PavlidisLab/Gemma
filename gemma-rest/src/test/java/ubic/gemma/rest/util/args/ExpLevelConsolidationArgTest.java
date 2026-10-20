package ubic.gemma.rest.util.args;

import org.junit.jupiter.api.Test;
import ubic.gemma.model.expression.bioAssayData.ExperimentExpressionLevelsValueObject;
import ubic.gemma.rest.util.MalformedArgException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ExpLevelConsolidationArgTest {

    @Test
    public void testValueOfPickMax() {
        assertThat( ExpLevelConsolidationArg.valueOf( ExperimentExpressionLevelsValueObject.OPT_PICK_MAX ).getValue() )
                .isEqualTo( ExperimentExpressionLevelsValueObject.OPT_PICK_MAX );
    }

    @Test
    public void testValueOfPickVar() {
        assertThat( ExpLevelConsolidationArg.valueOf( ExperimentExpressionLevelsValueObject.OPT_PICK_VAR ).getValue() )
                .isEqualTo( ExperimentExpressionLevelsValueObject.OPT_PICK_VAR );
    }

    @Test
    public void testValueOfAvg() {
        assertThat( ExpLevelConsolidationArg.valueOf( ExperimentExpressionLevelsValueObject.OPT_AVG ).getValue() )
                .isEqualTo( ExperimentExpressionLevelsValueObject.OPT_AVG );
    }

    @Test
    public void testValueOfBogusRaises() {
        assertThatThrownBy( () -> ExpLevelConsolidationArg.valueOf( "BOGUS_OPTION" ) )
                .isInstanceOf( MalformedArgException.class );
    }
}
