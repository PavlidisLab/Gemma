package ubic.gemma.core.util.math.linearmodels;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * What centring the interaction column does to a two-way model, on an unbalanced 2x2 where the answer is known.
 * <p>
 * Cell means (disease x sex): control/male 0, case/male 1, control/female 0.5, case/female 3. The disease effect is
 * therefore 1 in males and 2.5 in females, so the equal-weighted mean over sex is 1.75 and the sample-weighted one
 * is something else again (the cells have 10, 3, 5 and 12 samples). Treatment coding reports the male value, 1.
 */
public class CenteredInteractionTest {

    private static final double EQUAL_WEIGHTED_DISEASE_EFFECT = 1.75;
    private static final double DISEASE_EFFECT_IN_MALES = 1.0;
    private static final double DIFFERENCE_OF_DIFFERENCES = 1.5;

    /**
     * 🛑 The point of the change: with an interaction in the model, the main effect of disease is no longer the
     * effect in whichever sex was nominated as the baseline.
     */
    @Test
    public void mainEffectIsTheEqualWeightedMeanOverTheOtherFactorWhenCentred() {
        double treatment = diseaseCoefficient( false );
        double centred = diseaseCoefficient( true );

        assertThat( treatment ).as( "treatment coding: disease effect at the baseline sex" )
                .isEqualTo( DISEASE_EFFECT_IN_MALES, offset( 1e-9 ) );
        assertThat( centred ).as( "centred: equal-weighted over sex, whatever the cell sizes" )
                .isEqualTo( EQUAL_WEIGHTED_DISEASE_EFFECT, offset( 1e-9 ) );
    }

    /**
     * Centring spans the same model: the subtracted terms are main-effect columns and the intercept. So the
     * interaction coefficient and the fit are untouched, and an interaction p-value stored before and after
     * agree.
     */
    @Test
    public void interactionCoefficientAndFitAreUnchanged() {
        LeastSquaresFit before = fit( false );
        LeastSquaresFit after = fit( true );

        assertThat( interactionCoefficient( after ) ).isEqualTo( DIFFERENCE_OF_DIFFERENCES, offset( 1e-9 ) );
        assertThat( interactionCoefficient( after ) ).isEqualTo( interactionCoefficient( before ), offset( 1e-9 ) );
        // the whole row: estimate, standard error, t and p
        DoubleMatrix<String, String> rowsBefore = before.summarize().get( 0 ).getContrastCoefficients();
        DoubleMatrix<String, String> rowsAfter = after.summarize().get( 0 ).getContrastCoefficients();
        int rb = rowsBefore.getRowIndexByName( interactionRowName( rowsBefore ) );
        int ra = rowsAfter.getRowIndexByName( interactionRowName( rowsAfter ) );
        for ( int j = 0; j < rowsAfter.columns(); j++ ) {
            assertThat( rowsAfter.get( ra, j ) ).isEqualTo( rowsBefore.get( rb, j ), offset( 1e-9 ) );
        }
    }

    /** The other main effect is averaged too, not just the first. */
    @Test
    public void bothMainEffectsAreAveraged() {
        // sex effect: 0.5 in controls, 2 in cases; equal-weighted 1.25
        DoubleMatrix<String, String> rows = fit( true ).summarize().get( 0 ).getContrastCoefficients();
        assertThat( rows.get( rows.getRowIndexByName( "sexfemale" ), 0 ) ).isEqualTo( 1.25, offset( 1e-9 ) );
    }

    /**
     * 🛑 The interaction's contrasts are found whichever order its factors were named in. The term was named from
     * the order given and its columns from the sorted order, so a term built from a HashSet iteration could match
     * no column: the interaction kept its p-value and lost every contrast, silently. A redo hit it because it
     * rebuilds the interaction from the old result sets' factors.
     */
    @Test
    public void interactionContrastsAreFoundWhateverOrderTheFactorsWereNamedIn() {
        // names as the analyzer writes them: fact.<id> for a factor, fv_<id> for a level
        for ( boolean center : new boolean[] { false, true } ) {
            for ( String[] order : new String[][] { { "fact.1", "fact.2" }, { "fact.2", "fact.1" } } ) {
                ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( 30, 2 );
                design.setColumnNames( java.util.Arrays.asList( "fact.1", "fact.2" ) );
                List<String> rowNames = new ArrayList<>();
                int[] cells = { 10, 3, 5, 12 };
                int row = 0;
                for ( int cell = 0; cell < 4; cell++ ) {
                    for ( int i = 0; i < cells[cell]; i++, row++ ) {
                        rowNames.add( "s" + row );
                        design.set( row, 0, cell % 2 == 0 ? "fv_10" : "fv_11" );
                        design.set( row, 1, cell < 2 ? "fv_20" : "fv_21" );
                    }
                }
                design.setRowNames( rowNames );
                DesignMatrix dm = new DesignMatrix( design, true );
                dm.setCenterInteractions( center );
                dm.addInteraction( order[0], order[1] );
                dm.setBaseline( "fact.1", "fv_10" );
                dm.setBaseline( "fact.2", "fv_20" );

                LinearModelSummary s = new LeastSquaresFit( dm, response() ).summarize().get( 0 );
                String interactionTerm = s.getFactorNames().stream().filter( n -> n.contains( ":" ) ).findFirst()
                        .orElseThrow( () -> new AssertionError( "no interaction term in " + s.getFactorNames() ) );
                assertThat( s.getContrastPValues( interactionTerm ) )
                        .as( "centred=%s, given as %s:%s", center, order[0], order[1] )
                        .isNotEmpty();
            }
        }
    }

    /** Off, the design is the one every earlier analysis used. */
    @Test
    public void offIsTheOldProductOfIndicators() {
        DesignMatrix dm = design( false );
        DoubleMatrix<String, String> m = dm.getMatrix();
        int last = m.columns() - 1;
        for ( int i = 0; i < m.rows(); i++ ) {
            assertThat( m.get( i, last ) ).isEqualTo( m.get( i, 1 ) * m.get( i, 2 ) );
        }
    }

    private static double diseaseCoefficient( boolean center ) {
        DoubleMatrix<String, String> rows = fit( center ).summarize().get( 0 ).getContrastCoefficients();
        return rows.get( rows.getRowIndexByName( "diseasecase" ), 0 );
    }

    private static double interactionCoefficient( LeastSquaresFit fit ) {
        DoubleMatrix<String, String> rows = fit.summarize().get( 0 ).getContrastCoefficients();
        return rows.get( rows.getRowIndexByName( interactionRowName( rows ) ), 0 );
    }

    private static String interactionRowName( DoubleMatrix<String, String> rows ) {
        for ( String name : rows.getRowNames() ) {
            if ( name.contains( ":" ) ) {
                return name;
            }
        }
        throw new AssertionError( "no interaction row in " + rows.getRowNames() );
    }

    private static LeastSquaresFit fit( boolean center ) {
        DesignMatrix dm = design( center );
        return new LeastSquaresFit( dm, response() );
    }

    private static DesignMatrix design( boolean center ) {
        return design( center, "disease", "sex" );
    }

    private static DesignMatrix design( boolean center, String first, String second ) {
        List<String> disease = new ArrayList<>();
        List<String> sex = new ArrayList<>();
        addCell( disease, sex, "control", "male", 10 );
        addCell( disease, sex, "case", "male", 3 );
        addCell( disease, sex, "control", "female", 5 );
        addCell( disease, sex, "case", "female", 12 );
        ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( disease.size(), 2 );
        design.setColumnNames( java.util.Arrays.asList( "disease", "sex" ) );
        List<String> rowNames = new ArrayList<>();
        for ( int i = 0; i < disease.size(); i++ ) {
            rowNames.add( "s" + i );
            design.set( i, 0, disease.get( i ) );
            design.set( i, 1, sex.get( i ) );
        }
        design.setRowNames( rowNames );

        DesignMatrix dm = new DesignMatrix( design, true );
        dm.setCenterInteractions( center );
        dm.addInteraction( first, second );
        dm.setBaseline( "disease", "control" );
        dm.setBaseline( "sex", "male" );
        return dm;
    }

    private static void addCell( List<String> disease, List<String> sex, String d, String s, int n ) {
        for ( int i = 0; i < n; i++ ) {
            disease.add( d );
            sex.add( s );
        }
    }

    /** Cell means as in the class comment, with a wobble that sums to zero within each cell so the means stay exact. */
    private static DoubleMatrix<String, String> response() {
        double[] y = new double[30];
        int k = 0;
        double[][] cells = { { 0.0, 10 }, { 1.0, 3 }, { 0.5, 5 }, { 3.0, 12 } };
        for ( double[] cell : cells ) {
            int n = ( int ) cell[1];
            for ( int i = 0; i < n; i++ ) {
                boolean unpaired = n % 2 == 1 && i == n - 1;
                y[k++] = cell[0] + ( unpaired ? 0.0 : i % 2 == 0 ? 0.1 : -0.1 );
            }
        }
        DoubleMatrix<String, String> m = new ubic.gemma.core.util.matrix.DenseDoubleMatrix<>( 1, y.length );
        m.setRowName( "probe", 0 );
        for ( int j = 0; j < y.length; j++ ) {
            m.setColumnName( "s" + j, j );
            m.set( 0, j, y[j] );
        }
        return m;
    }
}
