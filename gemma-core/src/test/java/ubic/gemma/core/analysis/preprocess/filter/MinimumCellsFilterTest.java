package ubic.gemma.core.analysis.preprocess.filter;

import org.junit.Before;
import org.junit.Test;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.bioAssayData.RandomExpressionDataMatrixUtils;
import ubic.gemma.persistence.service.expression.experiment.RandomExpressionExperimentUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class MinimumCellsFilterTest {

    private ExpressionDataDoubleMatrix matrix;

    @Before
    public void setUp() {
        Taxon taxon = new Taxon();
        ArrayDesign ad = new ArrayDesign();
        ad.setPrimaryTaxon( taxon );
        for ( int i = 0; i < 100; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance( "cs" + ( i + 1 ), ad );
            ad.getCompositeSequences().add( cs );
        }
        ExpressionExperiment ee = RandomExpressionExperimentUtils.randomExpressionExperiment( taxon, 8, ad );
        matrix = RandomExpressionDataMatrixUtils.randomCountMatrix( ee );
    }

    @Test
    public void testFilterWithNumberOfCellsInMatrix() throws FilteringException {
        int[][] numberOfCells = new int[100][8];
        for ( int i = 0; i < 100; i++ ) {
            for ( int j = 0; j < 8; j++ ) {
                numberOfCells[i][j] = 1;
            }
        }
        numberOfCells[0][0] = 0; // 99 in first column
        MinimumCellsFilter filter = new MinimumCellsFilter();
        assertThat( filter.filter( matrix.withNumberOfCells( numberOfCells ) ) )
                .satisfies( filteredMatrix -> {
                    assertThat( filteredMatrix.columns() ).isEqualTo( 8 );
                    assertThat( filteredMatrix.get( 0, 0 ) ).isNaN();
                    assertThat( filteredMatrix.getQuantitationType() ).isSameAs( matrix.getQuantitationType() );
                    assertThat( filteredMatrix.getBioAssayDimension() ).isSameAs( matrix.getBioAssayDimension() );
                } );
    }

    @Test
    public void testFilterWithNumberOfCellsInAssays() throws FilteringException {
        // QT must be preferred for the filter to consider BioAssay.numberOfCells
        matrix.getQuantitationType().setIsPreferred( true );
        assertThat( matrix.getExpressionExperiment() ).isNotNull();
        for ( BioAssay ba : matrix.getExpressionExperiment().getBioAssays() ) {
            if ( ba.getName().equals( "ba1" ) ) {
                ba.setNumberOfCells( 99 );
            } else {
                ba.setNumberOfCells( 100 );
            }
        }
        MinimumCellsFilter filter = new MinimumCellsFilter();
        assertThat( filter.filter( matrix ) ).satisfies( filteredMatrix -> {
            assertThat( filteredMatrix.columns() ).isEqualTo( 8 );
            assertThat( filteredMatrix.get( 0, 0 ) ).isNaN();
            assertThat( filteredMatrix.getQuantitationType() ).isSameAs( matrix.getQuantitationType() );
            assertThat( filteredMatrix.getBioAssayDimension() ).isSameAs( matrix.getBioAssayDimension() );
        } );
    }

    @Test
    public void testFilterWithSliceColumns() throws FilteringException {
        // QT must be preferred for the filter to consider BioAssay.numberOfCells
        matrix.getQuantitationType().setIsPreferred( true );
        assertThat( matrix.getExpressionExperiment() ).isNotNull();
        for ( BioAssay ba : matrix.getExpressionExperiment().getBioAssays() ) {
            if ( ba.getName().equals( "ba1" ) ) {
                ba.setNumberOfCells( 99 );
            } else {
                ba.setNumberOfCells( 100 );
            }
        }
        MinimumCellsFilter filter = new MinimumCellsFilter();
        filter.setAllowSlicingColumns( true );
        assertThat( filter.filter( matrix ) ).satisfies( filteredMatrix -> {
            assertThat( filteredMatrix.columns() ).isEqualTo( 7 );
            assertThat( filteredMatrix.get( 0, 0 ) ).isNotNaN();
            assertThat( filteredMatrix.getQuantitationType() ).isSameAs( matrix.getQuantitationType() );
            assertThat( filteredMatrix.getBioAssayDimension() ).isNotSameAs( matrix.getBioAssayDimension() );
            assertThat( filteredMatrix.getBioAssayDimension().getBioAssays() ).hasSize( 7 );
        } );
    }
}
