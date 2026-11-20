package ubic.gemma.persistence.service.analysis.expression.pca;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DenseDoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.model.analysis.expression.pca.PrincipalComponentAnalysis;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * How {@link PrincipalComponentAnalysisServiceImpl#create} turns a decomposition into a stored PCA. The DAO is mocked;
 * what is under test is the entity it is handed.
 */
public class PrincipalComponentAnalysisServiceImplTest {

    private PrincipalComponentAnalysisServiceImpl service;
    private final ExpressionExperiment ee = new ExpressionExperiment();
    private final BioAssayDimension bad = new BioAssayDimension();
    private DoubleMatrix<CompositeSequence, Integer> u;
    private DoubleMatrix<BioMaterial, Integer> v;

    @BeforeEach
    public void setUp() {
        PrincipalComponentAnalysisDao dao = mock();
        when( dao.create( any( PrincipalComponentAnalysis.class ) ) ).thenAnswer( a -> a.getArgument( 0 ) );
        service = new PrincipalComponentAnalysisServiceImpl( dao );

        List<CompositeSequence> probes = new ArrayList<>();
        for ( int i = 0; i < 3; i++ ) {
            CompositeSequence cs = CompositeSequence.Factory.newInstance();
            cs.setName( "probe" + i );
            probes.add( cs );
        }
        u = new DenseDoubleMatrix<>( new double[][] { { 0.6, 0.1 }, { -0.7, 0.2 }, { 0.3, -0.9 } } );
        u.setRowNames( probes );
        u.setColumnNames( Arrays.asList( 0, 1 ) );

        List<BioMaterial> samples = new ArrayList<>();
        for ( int i = 0; i < 4; i++ ) {
            BioMaterial bm = BioMaterial.Factory.newInstance();
            bm.setName( "sample" + i );
            samples.add( bm );
        }
        v = new DenseDoubleMatrix<>( new double[][] { { 0.5, 0.5 }, { 0.5, -0.5 }, { -0.5, 0.5 }, { -0.5, -0.5 } } );
        v.setRowNames( samples );
        v.setColumnNames( Arrays.asList( 0, 1 ) );
    }

    /**
     * Without an eigenvalue sum, the given eigenvalues are taken to be all of them, as for a full decomposition.
     */
    @Test
    public void testVarianceFractionsOfAFullDecomposition() {
        PrincipalComponentAnalysis pca = service.create( ee, u, new double[] { 3.0, 1.0 }, v, bad, 5, 1000 );
        assertArrayEquals( new double[] { 0.75, 0.25 }, pca.getVarianceFractions(), 1e-15 );
    }

    /**
     * A truncated decomposition supplies the sum over every component, so the fractions of the leading ones stay
     * fractions of the total variance rather than of the components computed.
     */
    @Test
    public void testVarianceFractionsOfATruncatedDecomposition() {
        PrincipalComponentAnalysis pca = service.create( ee, u, new double[] { 3.0, 1.0 }, 8.0, v, bad, 5, 1000 );
        assertArrayEquals( new double[] { 0.375, 0.125 }, pca.getVarianceFractions(), 1e-15 );
        assertEquals( 2, pca.getEigenValues().size() );
        assertEquals( 2, pca.getEigenVectors().size() );
        assertEquals( 2, pca.getNumComponentsStored() );
    }
}
