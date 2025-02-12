package ubic.gemma.core.analysis.singleCell.aggregate;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleCellExpressionExperimentSplitAndAggregateServiceTest extends BaseIntegrationTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentSplitAndAggregateService splitAndAggregateService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    private ExpressionExperiment ee;

    @Before
    public void setUp() {
        ee = testHelper.getTestPersistentSingleCellExpressionExperiment();
    }

    @After
    public void cleanUp() {
        // FIXME: experiment with single-cell data cannot be deleted due to some constraint violation
        // if ( ee != null ) {
        //     expressionExperimentService.remove( ee );
        // }
    }

    @Test
    public void testRedoAggregate() {
        assertThat( ee.getQuantitationTypes() )
                .hasSize( 1 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 100 );

        SplitConfig splitConfig = SplitConfig.builder().build();
        AggregateConfig config = AggregateConfig.builder().makePreferred( true ).build();
        QuantitationType qt = splitAndAggregateService.splitAndAggregateByCellType( ee, splitConfig, config );

        ee = expressionExperimentService.thawLite( ee );
        assertThat( ee.getQuantitationTypes() ).contains( qt );

        BioAssayDimension dim = expressionExperimentService.getBioAssayDimension( ee, qt, RawExpressionDataVector.class );
        assertThat( dim ).isNotNull();
        QuantitationType newQt = splitAndAggregateService.redoAggregateByCellType( ee, dim, qt, config );
        BioAssayDimension newBad = expressionExperimentService.getBioAssayDimension( ee, newQt, RawExpressionDataVector.class );
        assertThat( newBad ).isEqualTo( dim );
        assertThat( quantitationTypeService.load( qt.getId() ) ).isNull();
        assertThat( newQt.getIsPreferred() ).isTrue();

        ee = expressionExperimentService.thawLite( ee );
        assertThat( ee.getQuantitationTypes() ).contains( newQt );
    }
}