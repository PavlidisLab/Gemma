package ubic.gemma.persistence.service.expression.experiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseSpringContextTest5;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExperimentalDesignUpdatedEvent;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The experiment-scoped {@link FactorValueDeletion} methods behind {@code deleteFactorValues}: selecting by factor and
 * displayed value, refusing a factor value of another experiment, and recording the change on the experiment.
 */
public class FactorValueDeletionTest extends BaseSpringContextTest5 {

    @Autowired
    private FactorValueDeletion factorValueDeletion;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private AuditEventService auditEventService;

    private ExpressionExperiment ee;
    private ExperimentalFactor factor;
    private FactorValue fv;

    @BeforeEach
    public void setUp() {
        ee = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        // a factor value with a displayed value, so that it can be selected by it
        fv = ee.getExperimentalDesign().getExperimentalFactors().stream()
                .flatMap( ef -> ef.getFactorValues().stream() )
                .filter( f -> FactorValueUtils.getValue( f, ", " ) != null )
                .findFirst().orElseThrow( () -> new IllegalStateException( "The test experiment has no factor value with a displayed value." ) );
        factor = fv.getExperimentalFactor();
    }

    @Test
    public void testResolveByFactorNameAndValue() {
        String label = FactorValueUtils.getValue( fv, ", " );
        assertThat( factorValueDeletion.resolveFactorValueIds( ee, factor.getName(), Collections.singletonList( label.toUpperCase() ) ) )
                .containsExactly( fv.getId() );
    }

    @Test
    public void testAnUnknownFactorOrValueIsRefused() {
        assertThatThrownBy( () -> factorValueDeletion.resolveFactorValueIds( ee, "no such factor", Collections.singletonList( "x" ) ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "no factor named no such factor" );
        assertThatThrownBy( () -> factorValueDeletion.resolveFactorValueIds( ee, factor.getName(), Collections.singletonList( "no such value" ) ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "matches 0 factor values" );
    }

    @Test
    public void testAFactorValueOfAnotherExperimentIsRefused() {
        ExpressionExperiment other = testHelper.getTestExpressionExperimentWithAllDependencies( false );
        FactorValue foreign = other.getExperimentalDesign().getExperimentalFactors().iterator().next().getFactorValues().iterator().next();
        assertThatThrownBy( () -> factorValueDeletion.deleteFactorValues( ee, Collections.singletonList( foreign.getId() ) ) )
                .isInstanceOf( IllegalArgumentException.class )
                .hasMessageContaining( "does not belong to the design of" );
        assertThat( factorValueService.load( foreign.getId() ) ).isNotNull();
    }

    @Test
    public void testDeleteRemovesTheFactorValueAndRecordsADesignUpdate() {
        assertThat( factorValueDeletion.describeDeletion( ee, Collections.singletonList( fv.getId() ) ) )
                .contains( "Factor value " + fv.getId() + " of " + factor.getName() );
        factorValueDeletion.deleteFactorValues( ee, Collections.singletonList( fv.getId() ) );
        assertThat( factorValueService.load( fv.getId() ) ).isNull();
        assertThat( auditEventService.getEvents( ee ) )
                .anySatisfy( e -> {
                    assertThat( e.getEventType() ).isInstanceOf( ExperimentalDesignUpdatedEvent.class );
                    assertThat( e.getNote() ).contains( String.valueOf( fv.getId() ) );
                } );
    }
}
