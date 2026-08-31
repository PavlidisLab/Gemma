package ubic.gemma.persistence.service.expression.experiment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.security.SecurityService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the taxon rule {@link ExpressionExperimentSetValueObjectHelperImpl#updateMembers}
 * applies when replacing a set's membership.
 * <p>
 * The set's taxon is a constraint the set opted into, not a property every set has: a set that
 * declares one refuses a member of another, and a set that declares none may span them. The check
 * here ran unconditionally, so on a set with no taxon every candidate member failed
 * {@code eeTaxon.equals(null)} and the membership could never be replaced.
 *
 * @author claude
 */
public class ExpressionExperimentSetValueObjectHelperImplTest {

    private ExpressionExperimentSetService expressionExperimentSetService;
    private ExpressionExperimentService expressionExperimentService;
    private ExpressionExperimentSetValueObjectHelperImpl helper;

    private Taxon human;
    private Taxon mouse;
    private ExpressionExperiment humanEe;
    private ExpressionExperiment mouseEe;

    @BeforeEach
    public void setUp() {
        expressionExperimentSetService = mock( ExpressionExperimentSetService.class );
        expressionExperimentService = mock( ExpressionExperimentService.class );
        helper = new ExpressionExperimentSetValueObjectHelperImpl( expressionExperimentSetService,
                expressionExperimentService, mock( TaxonService.class ), mock( SecurityService.class ) );

        human = Taxon.Factory.newInstance( "human" );
        human.setId( 1L );
        mouse = Taxon.Factory.newInstance( "mouse" );
        mouse.setId( 2L );

        humanEe = new ExpressionExperiment();
        humanEe.setId( 10L );
        mouseEe = new ExpressionExperiment();
        mouseEe.setId( 20L );

        when( expressionExperimentService.getTaxon( humanEe ) ).thenReturn( human );
        when( expressionExperimentService.getTaxon( mouseEe ) ).thenReturn( mouse );
    }

    @Test
    public void testUpdateMembersOfSetWithoutTaxonAcceptsMixedTaxa() {
        ExpressionExperimentSet mixed = new ExpressionExperimentSet();
        mixed.setId( 100L );
        mixed.setName( "Reference cohort spanning taxa" );
        mixed.setTaxon( null );
        mixed.getExperiments().add( humanEe );

        Collection<Long> newMemberIds = Arrays.asList( 10L, 20L );
        when( expressionExperimentSetService.load( 100L ) ).thenReturn( mixed );
        when( expressionExperimentService.load( newMemberIds ) ).thenReturn( Arrays.asList( humanEe, mouseEe ) );

        helper.updateMembers( 100L, newMemberIds );

        assertThat( mixed.getExperiments() ).containsExactlyInAnyOrder( humanEe, mouseEe );
        verify( expressionExperimentSetService ).update( mixed );
    }

    /**
     * Control: a set that declares a taxon still refuses a member of another.
     */
    @Test
    public void testUpdateMembersOfSetWithTaxonRefusesAnother() {
        ExpressionExperimentSet scoped = new ExpressionExperimentSet();
        scoped.setId( 200L );
        scoped.setName( "Human only" );
        scoped.setTaxon( human );
        scoped.getExperiments().add( humanEe );

        Collection<Long> newMemberIds = Arrays.asList( 10L, 20L );
        when( expressionExperimentSetService.load( 200L ) ).thenReturn( scoped );
        when( expressionExperimentService.load( newMemberIds ) ).thenReturn( Arrays.asList( humanEe, mouseEe ) );

        assertThatThrownBy( () -> helper.updateMembers( 200L, newMemberIds ) )
                .isInstanceOf( IllegalArgumentException.class );
    }
}
