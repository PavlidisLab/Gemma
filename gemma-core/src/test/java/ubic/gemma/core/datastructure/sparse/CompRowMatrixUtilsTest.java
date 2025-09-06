package ubic.gemma.core.datastructure.sparse;

import no.uib.cipr.matrix.io.MatrixVectorReader;
import no.uib.cipr.matrix.sparse.CompRowMatrix;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class CompRowMatrixUtilsTest {

    @Test
    public void testSelectRows() throws IOException {
        CompRowMatrix matrix = new CompRowMatrix( new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( getClass().getResourceAsStream( "/data/loader/expression/singleCell/GSE224438/GSM7022367_1_matrix.mtx.gz" ) ) ) ) );
        CompRowMatrix newMatrix = CompRowMatrixUtils.selectRows( matrix, new int[] { 23, 150, 827 } );
        assertThat( newMatrix.numRows() ).isEqualTo( 3 );
        assertThat( newMatrix.numColumns() ).isEqualTo( 1000 );
        assertThat( newMatrix.getRowPointers() ).hasSize( 4 )
                .containsExactly( 0, 2, 6, 269 );
        assertThat( newMatrix.getColumnIndices() )
                .hasSize( 269 )
                .containsExactly( 103, 165, 224, 280, 282, 756, 9, 13, 19, 25, 28, 31, 32,
                        33, 42, 43, 48, 58, 66, 72, 80, 83, 84, 85, 89, 95,
                        97, 100, 106, 116, 130, 133, 140, 141, 148, 152, 156, 158, 165,
                        167, 168, 177, 183, 186, 187, 189, 192, 193, 195, 199, 205, 207,
                        211, 216, 218, 221, 223, 228, 234, 236, 238, 240, 241, 242, 249,
                        250, 258, 259, 263, 264, 266, 270, 277, 286, 288, 290, 295, 299,
                        304, 308, 316, 318, 320, 321, 322, 329, 331, 333, 337, 341, 352,
                        354, 357, 362, 363, 373, 375, 376, 378, 381, 395, 396, 402, 405,
                        408, 419, 420, 421, 423, 424, 431, 445, 448, 450, 452, 454, 456,
                        457, 460, 468, 479, 486, 487, 493, 495, 496, 497, 499, 500, 501,
                        504, 509, 515, 516, 518, 519, 521, 522, 523, 528, 530, 531, 532,
                        536, 541, 545, 550, 555, 557, 559, 573, 576, 581, 584, 588, 591,
                        596, 599, 604, 606, 607, 608, 610, 614, 615, 617, 622, 630, 632,
                        633, 636, 643, 644, 645, 646, 648, 650, 651, 661, 671, 672, 674,
                        677, 683, 689, 692, 694, 701, 703, 712, 718, 722, 728, 734, 735,
                        736, 739, 741, 749, 760, 761, 762, 763, 765, 768, 770, 771, 772,
                        775, 777, 779, 783, 808, 813, 822, 824, 827, 832, 835, 838, 839,
                        840, 841, 845, 846, 848, 849, 850, 853, 857, 860, 864, 867, 870,
                        879, 882, 885, 889, 898, 899, 900, 902, 903, 906, 907, 910, 911,
                        914, 915, 924, 925, 927, 929, 930, 932, 933, 938, 941, 942, 943,
                        948, 951, 953, 955, 962, 970, 987, 991, 993 );
        assertThat( newMatrix.getData() )
                .hasSize( 269 )
                .containsExactly(
                        1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 3., 1., 1., 1., 1., 2.,
                        1., 1., 3., 3., 1., 1., 1., 3., 2., 1., 1., 1., 1., 1., 1., 1., 1.,
                        1., 2., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 2., 1., 1., 1., 1.,
                        1., 1., 3., 1., 1., 1., 1., 1., 1., 1., 2., 1., 2., 1., 2., 1., 1.,
                        1., 1., 2., 1., 1., 1., 1., 1., 1., 1., 2., 1., 1., 1., 2., 1., 1.,
                        1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 5., 1.,
                        1., 2., 1., 4., 1., 1., 1., 1., 2., 1., 1., 2., 1., 1., 2., 1., 1.,
                        1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 2., 1., 1., 1., 1., 1., 2.,
                        1., 2., 1., 1., 1., 2., 1., 1., 1., 2., 1., 1., 2., 1., 2., 1., 2.,
                        1., 1., 1., 1., 2., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 2.,
                        1., 1., 1., 1., 1., 1., 1., 3., 2., 2., 3., 1., 1., 2., 1., 2., 1.,
                        1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 3., 1.,
                        1., 1., 1., 1., 1., 1., 1., 1., 2., 3., 1., 1., 1., 2., 1., 2., 2.,
                        1., 1., 3., 2., 1., 1., 1., 1., 1., 1., 1., 1., 2., 1., 1., 1., 1.,
                        2., 1., 2., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 3., 2.,
                        2., 1., 1., 1., 1., 1., 1., 1., 1., 2., 1., 3., 1., 1.
                );
    }

    @Test
    public void testSelectColumns() throws IOException {
        CompRowMatrix matrix = new CompRowMatrix( new MatrixVectorReader( new InputStreamReader( new GZIPInputStream( getClass().getResourceAsStream( "/data/loader/expression/singleCell/GSE224438/GSM7022367_1_matrix.mtx.gz" ) ) ) ) );
        CompRowMatrix newMatrix = CompRowMatrixUtils.selectColumns( matrix, new int[] { 23, 150, 827 } );
        assertThat( newMatrix.numRows() ).isEqualTo( 1000 );
        assertThat( newMatrix.numColumns() ).isEqualTo( 3 );
        assertThat( newMatrix.getRowPointers() ).hasSize( 1001 );
        assertThat( newMatrix.getColumnIndices() )
                .hasSize( 156 ).containsExactly(
                        2, 1, 2, 0, 1, 2, 2, 2, 2, 2, 2, 1, 2, 2, 1, 0, 0, 0, 2, 0, 0, 0,
                        1, 2, 2, 0, 2, 2, 2, 0, 2, 0, 0, 2, 1, 0, 1, 1, 0, 1, 1, 0, 0, 2,
                        2, 2, 2, 0, 0, 2, 0, 1, 2, 1, 2, 0, 0, 2, 0, 0, 0, 1, 0, 0, 2, 0,
                        2, 1, 2, 1, 0, 1, 2, 1, 0, 0, 1, 2, 1, 2, 0, 0, 1, 1, 2, 2, 0, 1,
                        2, 2, 0, 2, 0, 2, 0, 1, 2, 0, 0, 1, 2, 0, 2, 0, 2, 2, 0, 1, 0, 1,
                        2, 0, 1, 2, 2, 0, 2, 1, 0, 0, 2, 1, 2, 0, 2, 2, 0, 2, 1, 0, 2, 2,
                        2, 2, 0, 0, 1, 2, 0, 0, 1, 2, 0, 0, 1, 2, 0, 0, 1, 2, 0, 2, 1, 0,
                        1, 2
                );
        assertThat( newMatrix.getData() )
                .hasSize( 156 )
                .containsExactly(
                        1., 2., 1., 2., 1., 1., 1., 2., 3., 2., 1.,
                        1., 1., 2., 1., 1., 1., 1., 1., 1., 2., 6.,
                        3., 3., 1., 33., 4., 1., 1., 1., 1., 3., 2.,
                        2., 1., 1., 1., 1., 1., 1., 1., 2., 1., 1.,
                        1., 1., 1., 1., 1., 1., 1., 1., 1., 1., 2.,
                        2., 1., 1., 2., 1., 1., 1., 3., 1., 2., 1.,
                        1., 5., 15., 1., 3., 1., 1., 1., 1., 4., 1.,
                        1., 1., 2., 2., 1., 1., 1., 1., 1., 1., 1.,
                        1., 1., 2., 1., 1., 1., 2., 1., 1., 1., 7.,
                        2., 5., 2., 1., 2., 2., 1., 1., 1., 1., 2.,
                        1., 1., 1., 2., 1., 1., 1., 1., 1., 1., 1.,
                        1., 1., 1., 1., 1., 1., 1., 1., 4., 1., 1.,
                        1., 1., 1., 1., 1., 1., 1., 2., 3., 5., 2.,
                        461., 24., 640., 4., 1., 1., 1., 1., 1., 1., 18.,
                        6., 18.
                );
    }
}