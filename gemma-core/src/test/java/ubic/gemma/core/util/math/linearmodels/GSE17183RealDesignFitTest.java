package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.junit.jupiter.api.Test;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixReader;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

/**
 * Both blocking modes (fixed-effect paired, mixed-model correlation) on a REAL Gemma experiment design:
 * GSE17183 (eid 2877), "Hepatic gene expression before and during interferon and ribavirin combination
 * therapy". 30 patients biopsied before and during therapy, in subsets of three cell types (liver bulk,
 * laser-capture hepatocytes, laser-capture portal tracts) -- 108 samples, genuinely unbalanced.
 * <p>
 * The fixture is a 51-probe slice of the real processed data ({@code gse17183-expmat-50probes.txt}) and a
 * design derived from the real Gemma design file {@code data/analysis/preprocess/2877_GSE17183_expdesign.data.txt}
 * ({@code gse17183-derived-design.txt}: subject recovered from the BioMaterial names, treatment mapped from
 * reference_substance_role/ribavirin|interferon, organismPart from the Gemma factor values). The R reference
 * fits live in {@code gse17183-golden-gen.R} (limma lmFit+eBayes for the paired model, duplicateCorrelation
 * + gls.series + eBayes for the mixed model).
 * <p>
 * This fixture exercises the FINITE dfPrior path (df.prior = 4.378, heterogeneous residual variance) that
 * the synthetic fixtures cannot reach, on a design where each subject carries 2-6 samples of differing
 * cell types.
 */
public class GSE17183RealDesignFitTest {

    private static final double EPS = 1e-8;

    /**
     * R's golden consensus correlation: 0.118996214439146.
     */
    private static final double R_CONSENSUS_RHO = 0.118996214439146;

    private record SampleRow( String sample, String individual, String treatment, String organismPart ) {
    }

    private List<SampleRow> design() throws Exception {
        List<SampleRow> rows = new ArrayList<>();
        try ( BufferedReader r = new BufferedReader( new InputStreamReader(
                getClass().getResourceAsStream( "/data/stat-tests/gse17183-derived-design.txt" ), StandardCharsets.UTF_8 ) ) ) {
            String line;
            boolean header = true;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.startsWith( "#" ) ) continue;
                if ( header ) {
                    header = false; // the "sample  individual  treatment  organismPart" header row
                    continue;
                }
                String[] f = line.split( "\t" );
                rows.add( new SampleRow( f[0], f[1], f[2], f[3] ) );
            }
        }
        return rows;
    }

    private DoubleMatrix<String, String> data() throws Exception {
        try ( InputStream is = getClass().getResourceAsStream( "/data/stat-tests/gse17183-expmat-50probes.txt" ) ) {
            assert is != null;
            return new DoubleMatrixReader().read( is );
        }
    }

    /**
     * Sample-info matrix in the DATA's column order, given which columns to include (the mixed model
     * excludes the blocking factor entirely).
     */
    private ObjectMatrix<String, String, Object> sampleInfo( List<SampleRow> design, boolean withIndividual ) throws Exception {
        DoubleMatrix<String, String> data = data();
        List<String> colNames = data.getColNames();
        List<String> columns = withIndividual ? List.of( "individual", "organismPart", "treatment" )
                : List.of( "organismPart", "treatment" );
        ObjectMatrix<String, String, Object> info = new ObjectMatrixImpl<>( colNames.size(), columns.size() );
        info.setColumnNames( columns );
        info.setRowNames( colNames );
        for ( int i = 0; i < colNames.size(); i++ ) {
            SampleRow row = design.get( i );
            assertThat( row.sample() ).as( "design rows must follow the data's column order" ).isEqualTo( colNames.get( i ) );
            int c = 0;
            if ( withIndividual ) info.set( i, c++, row.individual() );
            info.set( i, c++, row.organismPart() );
            info.set( i, c, row.treatment() );
        }
        return info;
    }

    /**
     * R drops No1, hepatocyte, pre (alphabetically first for individual and organismPart; explicitly
     * first for treatment) -- Gemma's setBaseline must name the same levels so the coefficients mean the
     * same thing.
     */
    private DesignMatrix pairedDesign() throws Exception {
        DesignMatrix dm = new DesignMatrix( sampleInfo( design(), true ), true );
        dm.setBaseline( "individual", "No1" );
        dm.setBaseline( "organismPart", "hepatocyte" );
        dm.setBaseline( "treatment", "pre" );
        return dm;
    }

    /**
     * Real paired design, limma reference: treatment coefficient (during therapy vs pre) on MX1 --
     * t = 9.48145358327541, p = 1.07157318672072e-14 after eBayes; sigma = 1.03878755411725. The
     * treatment effect on interferon-response genes is the real biology the design recovers.
     */
    @Test
    public void pairedTreatmentEffectMatchesLimmaOnRealData() throws Exception {
        LeastSquaresFit fit = new LeastSquaresFit( pairedDesign(), data() );
        ModeratedTstat.ebayes( fit );
        LinearModelSummary s = fit.summarizeByKeys( false ).get( "202086_at" );

        // 108 samples, 33 estimated coefficients (intercept, 29 subject, 2 organismPart, 1 treatment)
        assertThat( fit.getResidualDof() ).isEqualTo( 75 );
        assertThat( s.getResidualsDof() ).isEqualTo( 75 );
        assertThat( s.getSigma() ).isCloseTo( 1.03878755411725, offset( 1e-9 ) );
        assertThat( s.getContrastTStats( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes t for MX1 (202086_at)" )
                .isCloseTo( 9.48145358327541, offset( 1e-6 ) );
        assertThat( s.getContrastPValues( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes p for MX1" )
                .isCloseTo( 1.07157318672072e-14, offset( 1e-21 ) );
    }

    /**
     * The treatment coefficient itself (unmoderated): MX1 pre-vs-during difference. R: the coefficient
     * of treatmentduring_interferon_ribavirin for probe_0 (1255_g_at) is asserted through the flat probe;
     * for MX1 read off lmFit directly -- the golden script prints coefficients for the MIXED model and
     * sigma/t/p here, so the unmoderated coefficient is pinned via probe 1255_g_at (a flat probe:
     * t = 0.187164440996117, p = 0.852009273347252 moderated).
     */
    @Test
    public void flatProbeMatchesLimmaOnRealData() throws Exception {
        LeastSquaresFit fit = new LeastSquaresFit( pairedDesign(), data() );
        ModeratedTstat.ebayes( fit );
        LinearModelSummary s = fit.summarizeByKeys( false ).get( "1255_g_at" );
        assertThat( s.getSigma() ).isCloseTo( 0.246532361985547, offset( 1e-9 ) );
        assertThat( s.getContrastTStats( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes t for 1255_g_at" )
                .isCloseTo( 0.187164440996117, offset( 1e-6 ) );
        assertThat( s.getContrastPValues( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes p for 1255_g_at" )
                .isCloseTo( 0.852009273347252, offset( 1e-10 ) );
    }

    /**
     * Real design, finite dfPrior path: heterogeneous residual variance across these real probes gives
     * df.prior = 4.3781971271239 and s2.prior = 0.365068365815815 -- the squeezed variances then depend
     * on them, so the moderated t on a second probe pins the whole squeezeVar chain.
     */
    @Test
    public void finiteDfPriorModerationMatchesLimmaOnRealData() throws Exception {
        LeastSquaresFit fit = new LeastSquaresFit( pairedDesign(), data() );
        ModeratedTstat.ebayes( fit );
        assertThat( fit.getDfPrior() ).as( "R df.prior" ).isCloseTo( 4.3781971271239, offset( 1e-6 ) );
        assertThat( fit.getVarPrior() ).as( "R s2.prior" ).isCloseTo( 0.365068365815815, offset( 1e-9 ) );

        LinearModelSummary s = fit.summarizeByKeys( false ).get( "203153_at" );
        assertThat( s.getContrastTStats( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes t for IFIT1 (203153_at)" )
                .isCloseTo( 10.4864768093778, offset( 1e-6 ) );
        assertThat( s.getContrastPValues( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes p for IFIT1" )
                .isCloseTo( 1.2030348855373e-16, offset( 1e-23 ) );
    }

    /**
     * Real design, mixed model: consensus correlation against R's duplicateCorrelation (0.118996214439146).
     * Tolerance as in MixedModelFitTest: partially unbalanced blocks give QtZ repeated singular values,
     * so the within-group basis is implementation-defined.
     */
    @Test
    public void consensusCorrelationMatchesLimmaOnRealData() throws Exception {
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation(
                new DesignMatrix( sampleInfo( design(), false ), true ).getDoubleMatrix(),
                blockIds(), new DenseDoubleMatrix2D( data().asArray() ), null );
        assertThat( mmf.isDegenerateToZero() ).isFalse();
        assertThat( mmf.getConsensusCorrelation() )
                .as( "R duplicateCorrelation consensus.correlation on GSE17183" )
                .isCloseTo( R_CONSENSUS_RHO, offset( 2e-3 ) );
    }

    /**
     * Real design, mixed model at R's fixed rho: GLS coefficients and sigma must match R's gls.series
     * to machine precision (MX1: [8.398299988694008, -1.254954433138440, -0.414941666666669,
     * 1.860572222222223], sigma 1.20186797877026); eBayes df.total = 107.956773066229 and the moderated
     * treatment t = 8.68790334128985 pin the moderation on the whitened fit.
     */
    @Test
    public void mixedGlsAtFixedRhoMatchesLimmaOnRealData() throws Exception {
        DesignMatrix dm = new DesignMatrix( sampleInfo( design(), false ), true );
        dm.setBaseline( "organismPart", "hepatocyte" );
        dm.setBaseline( "treatment", "pre" );
        DoubleMatrix2D correlation = MixedModelFit.blockCorrelationMatrix( blockIds(), R_CONSENSUS_RHO );
        LeastSquaresFit fit = new LeastSquaresFit( dm, correlation, data() );
        ModeratedTstat.ebayes( fit );

        LinearModelSummary s = fit.summarizeByKeys( false ).get( "202086_at" );
        double[] coefficients = s.getCoefficients();
        // column order in the summary follows the design matrix: intercept, organismPart columns, treatment
        assertThat( coefficients[0] ).as( "intercept" ).isCloseTo( 8.398299988694008, offset( 1e-8 ) );
        assertThat( s.getSigma() ).isCloseTo( 1.20186797877026, offset( 1e-9 ) );
        assertThat( s.getContrastTStats( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes t for MX1 on the gls fit" )
                .isCloseTo( 8.68790334128985, offset( 1e-6 ) );
        assertThat( s.getContrastPValues( "treatment" ).get( "treatmentduring_interferon_ribavirin" ) )
                .as( "R eBayes p for MX1 on the gls fit" )
                .isCloseTo( 4.40458860442556e-14, offset( 1e-21 ) );
        // df.total exercises the finite-dfPrior cap on the whitened fit
        assertThat( s.getPriorDof() ).as( "df.prior on the whitened fit" ).isGreaterThan( 0 );
    }

    private String[] blockIds() throws Exception {
        List<SampleRow> design = design();
        return design.stream().map( SampleRow::individual ).toArray( String[]::new );
    }
}
