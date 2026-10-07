package ubic.gemma.core.util.math.linearmodels;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixReader;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import java.io.InputStream;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Paired design: a subject factor as a fixed block, treatment effect adjusted for it.
 * <p>
 * The R reference fits live in {@code data/stat-tests/paired-golden-gen.R} (run with Rscript; limma
 * {@code lmFit + eBayes} on {@code model.matrix(~ subject + treatment)}), and the fixture data are
 * {@code paired-test-data.txt} with {@code paired-design.txt}. The fixture is 100 probes over 12 samples
 * (6 subjects, ctrl/treat per subject), with per-probe subject shifts and a treatment effect on the last
 * 40 probes.
 * <p>
 * Gemma must reproduce the R treatment coefficient, its unscaled standard error, the moderated t and p, and
 * sigma for every probe asserted below. The subject columns are exactly the nuisance term limma adjusts for:
 * if this test passes with them in the design, a paired analysis in the analyzer differs from an unpaired
 * one only by which factors get reported.
 */
public class PairedDesignFitTest {

    private static final double EPS = 1e-8;

    private ObjectMatrix<String, String, Object> design() throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/paired-design.txt" ) ) {
            assert is != null;
            String content = new String( is.readAllBytes() );
            String[] lines = content.split( "\n" );
            ObjectMatrix<String, String, Object> design = new ObjectMatrixImpl<>( lines.length - 1, 2 );
            design.setColumnNames( Arrays.asList( "subject", "treatment" ) );
            design.setRowNames( Arrays.asList( Arrays.copyOfRange( lines, 1, lines.length ) ).stream()
                    .map( l -> l.split( "\t" )[0] ).toList() );
            for ( int i = 1; i < lines.length; i++ ) {
                String[] fields = lines[i].split( "\t" );
                design.set( i - 1, 0, fields[1] );
                design.set( i - 1, 1, fields[2] );
            }
            return design;
        }
    }

    private DoubleMatrix<String, String> data() throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/paired-test-data.txt" ) ) {
            assert is != null;
            return new DoubleMatrixReader().read( is );
        }
    }

    private LeastSquaresFit fit() throws Exception {
        DesignMatrix dm = new DesignMatrix( design(), true );
        // treatment baseline ctrl, so the treatmenttreat coefficient is the ctrl->treat difference
        // (Gemma drops the FIRST level, R drops the first alphabetically -- ctrl, same thing here).
        dm.setBaseline( "treatment", "ctrl" );
        dm.setBaseline( "subject", "s1" );
        LeastSquaresFit fit = new LeastSquaresFit( dm, data() );
        ModeratedTstat.ebayes( fit );
        return fit;
    }

    /**
     * probe_0: treatment coefficient -0.178524952015744 (unmoderated; the t/p below are the eBayes-moderated
     * ones, which is what Gemma reports by default). Contrast maps are keyed by coefficient name, which the
     * design matrix builds as factorName + level ("treatment" + "treat" -> "treatmenttreat").
     */
    @Test
    public void treatmentCoefficientMatchesLimmaForAFlatProbe() throws Exception {
        LeastSquaresFit fit = fit();
        LinearModelSummary s = fit.summarizeByKeys( false ).get( "probe_0" );

        assertThat( s.getContrastCoefficients( "treatment" ).get( "treatmenttreat" ) )
                .as( "R: -0.178524952015744" )
                .isCloseTo( -0.178524952015744, offset( EPS ) );
        assertThat( s.getContrastCoefficientStderr( "treatment" ).get( "treatmenttreat" ) )
                .as( "Gemma stores the UNSCALED SE in that column (see data/stat-tests/contrast-coding.R); "
                        + "R: sqrt(diag(cov.unscaled)) = 0.577350269189626" )
                .isCloseTo( 0.577350269189626, offset( EPS ) );
    }

    /**
     * The eBayes-moderated treatment t and p on a DE probe (true effect 1.2): R gives t = 5.4699580709098,
     * p = 7.1248077534004e-08 for probe_98. With a subject factor in the design the residual dof is 5
     * (12 samples - 7 coefficients) -- an unpaired fit would have had 10 and given a different answer.
     */
    @Test
    public void moderatedTreatmentStatisticsMatchLimmaForADeProbe() throws Exception {
        LeastSquaresFit fit = fit();
        LinearModelSummary s = fit.summarizeByKeys( false ).get( "probe_98" );

        assertThat( s.getContrastTStats( "treatment" ).get( "treatmenttreat" ) )
                .as( "R eBayes t for probe_98" )
                .isCloseTo( 5.4699580709098, offset( 1e-6 ) );
        assertThat( s.getContrastPValues( "treatment" ).get( "treatmenttreat" ) )
                .as( "R eBayes p for probe_98" )
                .isCloseTo( 7.1248077534004e-08, offset( 1e-13 ) );
    }

    /**
     * Residual dof: 12 samples, 7 estimated coefficients (intercept, 5 subject, 1 treatment), so 5 -- and
     * sigma matches the R fit through that dof (R sigma for probe_0 = 0.445584101416172).
     */
    @Test
    public void residualDofAccountsForTheSubjectColumns() throws Exception {
        LeastSquaresFit fit = fit();
        assertThat( fit.getResidualDof() ).isEqualTo( 5 );
        LinearModelSummary s = fit.summarizeByKeys( false ).get( "probe_0" );
        assertThat( s.getResidualsDof() ).isEqualTo( 5 );
        assertThat( s.getSigma() )
                .as( "R sigma for probe_0" )
                .isCloseTo( 0.445584101416172, offset( 1e-10 ) );
    }

    /**
     * The pairing actually changes the answer: fitting the same data WITHOUT the subject factor must give a
     * different treatment t for the DE probe. This is what distinguishes a paired from an unpaired analysis
     * and it is the regression guard against a refactor that silently drops the block.
     */
    @Test
    public void unpairedFitOfTheSameDataGivesADifferentTreatmentT() throws Exception {
        // unpaired design: treatment only
        ObjectMatrix<String, String, Object> unpaired = new ObjectMatrixImpl<>( 12, 1 );
        unpaired.setColumnNames( Arrays.asList( "treatment" ) );
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/paired-design.txt" ) ) {
            assert is != null;
            String[] lines = new String( is.readAllBytes() ).split( "\n" );
            unpaired.setRowNames( Arrays.asList( Arrays.copyOfRange( lines, 1, lines.length ) ).stream()
                    .map( l -> l.split( "\t" )[0] ).toList() );
            for ( int i = 1; i < lines.length; i++ ) {
                unpaired.set( i - 1, 0, lines[i].split( "\t" )[2] );
            }
        }
        DesignMatrix dm = new DesignMatrix( unpaired, true );
        dm.setBaseline( "treatment", "ctrl" );
        LeastSquaresFit unpairedFit = new LeastSquaresFit( dm, data() );
        ModeratedTstat.ebayes( unpairedFit );

        LinearModelSummary paired = fit().summarizeByKeys( false ).get( "probe_98" );
        LinearModelSummary unpairedSummary = unpairedFit.summarizeByKeys( false ).get( "probe_98" );

        assertThat( paired.getContrastTStats( "treatment" ).get( "treatmenttreat" ) )
                .as( "the paired t on a DE probe must be larger than the unpaired one: adjusting for the "
                        + "subject-to-subject spread removes variance the unpaired fit attributes to noise" )
                .isGreaterThan( unpairedSummary.getContrastTStats( "treatment" ).get( "treatmenttreat" ) );
    }
}
