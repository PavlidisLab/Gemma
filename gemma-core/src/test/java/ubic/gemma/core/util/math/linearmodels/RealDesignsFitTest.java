package ubic.gemma.core.util.math.linearmodels;

import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ubic.gemma.core.util.matrix.DoubleMatrix;
import ubic.gemma.core.util.matrix.DoubleMatrixReader;
import ubic.gemma.core.util.matrix.ObjectMatrix;
import ubic.gemma.core.util.matrix.ObjectMatrixImpl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Paired / repeated-measures fits against limma on REAL Gemma designs, one fixture per design shape (see
 * {@code data/stat-tests/real-designs/fetch_real_designs.py} for how each was chosen, and
 * {@code real-designs-golden-gen.R} for the reference fits; the golden values are read from each
 * {@code golden.txt}, not copied here).
 * <ul>
 * <li>GSE33658: 11 patients x 2, balanced pairs</li>
 * <li>GSE7400: 5 pairs, 4 residual dof</li>
 * <li>GSE95298: 20 cell lines, block sizes 2 and 4, rho near its upper clamp (0.978)</li>
 * <li>GSE246769: 8 subjects, 3-4 samples each, a four-level within-subject factor</li>
 * <li>GSE32473: 10 patients x 3 arms, a three-level within-subject factor, rho 0.83</li>
 * <li>GSE63857: RNA-seq with a singleton block among blocks of 2 and 4</li>
 * <li>GSE137: 35 x 2 with no recorded condition (intercept-only design plus block)</li>
 * <li>GSE55468: every individual measured once (no repeated measures)</li>
 * </ul>
 */
public class RealDesignsFitTest {

    private static final String DIR = "/data/stat-tests/real-designs/";

    private static final String[] PAIRED = { "GSE33658", "GSE7400", "GSE95298", "GSE246769", "GSE32473", "GSE63857" };

    private record Golden( Map<String, Double> header, Map<String, List<String[]>> rows, Map<String, String> text ) {
    }

    private static class Fixture {
        List<String> samples = new ArrayList<>();
        List<String> block = new ArrayList<>();
        List<String> cond = new ArrayList<>();
        DoubleMatrix<String, String> data;
        Golden golden;

        boolean hasCond() {
            return cond.stream().anyMatch( c -> !c.isEmpty() );
        }

        String[] blockIds() {
            return block.toArray( new String[0] );
        }

        /**
         * Baseline: the level R's golden script puts first (pre / ctrl / wt / d0) -- one of them is always present.
         */
        String condBaseline() {
            for ( String b : List.of( "pre", "ctrl", "wt", "d0" ) ) {
                if ( cond.contains( b ) ) return b;
            }
            throw new IllegalStateException();
        }

        String blockBaseline() {
            return new TreeSet<>( block ).first();
        }

        ObjectMatrix<String, String, Object> info( boolean withBlock ) {
            List<String> cols = new ArrayList<>();
            if ( withBlock ) cols.add( "block" );
            if ( hasCond() ) cols.add( "cond" );
            ObjectMatrix<String, String, Object> m = new ObjectMatrixImpl<>( samples.size(), cols.size() );
            m.setColumnNames( cols );
            m.setRowNames( data.getColNames() );
            for ( int i = 0; i < samples.size(); i++ ) {
                int c = 0;
                if ( withBlock ) m.set( i, c++, block.get( i ) );
                if ( hasCond() ) m.set( i, c, cond.get( i ) );
            }
            return m;
        }

        DesignMatrix design( boolean withBlock ) {
            DesignMatrix dm = new DesignMatrix( info( withBlock ), true );
            if ( withBlock ) dm.setBaseline( "block", blockBaseline() );
            if ( hasCond() ) dm.setBaseline( "cond", condBaseline() );
            return dm;
        }
    }

    private Fixture load( String gse ) throws Exception {
        Fixture f = new Fixture();
        try ( BufferedReader r = reader( DIR + gse + "/design.txt" ) ) {
            String line;
            boolean header = true;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.startsWith( "#" ) ) continue;
                if ( header ) {
                    header = false;
                    continue;
                }
                String[] p = line.split( "\t", -1 );
                f.samples.add( p[0] );
                f.block.add( p[1] );
                f.cond.add( p.length > 2 ? p[2] : "" );
            }
        }
        try ( InputStream is = getClass().getResourceAsStream( DIR + gse + "/expmat.txt" ) ) {
            f.data = new DoubleMatrixReader().read( is );
        }
        assertThat( f.data.getColNames() ).as( "design rows follow the data's column order" ).isEqualTo( f.samples );
        f.golden = golden( gse );
        return f;
    }

    private BufferedReader reader( String resource ) {
        return new BufferedReader( new InputStreamReader( getClass().getResourceAsStream( resource ), StandardCharsets.UTF_8 ) );
    }

    private Golden golden( String gse ) throws Exception {
        Map<String, Double> header = new HashMap<>();
        Map<String, String> text = new HashMap<>();
        Map<String, List<String[]>> rows = new HashMap<>();
        try ( BufferedReader r = reader( DIR + gse + "/golden.txt" ) ) {
            String line;
            while ( ( line = r.readLine() ) != null ) {
                if ( line.startsWith( "# " ) && line.contains( ": " ) ) {
                    String k = line.substring( 2, line.indexOf( ": " ) );
                    String v = line.substring( line.indexOf( ": " ) + 2 );
                    text.put( k, v );
                    try {
                        header.put( k, Double.parseDouble( v ) );
                    } catch ( NumberFormatException e ) {
                        // a term list
                    }
                } else if ( !line.startsWith( "#" ) && !line.startsWith( "model\t" ) && !line.isBlank() ) {
                    String[] p = line.split( "\t" );
                    rows.computeIfAbsent( p[0], k -> new ArrayList<>() ).add( p );
                }
            }
        }
        return new Golden( header, rows, text );
    }

    private static void assertClose( double actual, double expected, double rel, String what ) {
        assertThat( Math.abs( actual - expected ) )
                .as( what + ": actual " + actual + " vs limma " + expected )
                .isLessThanOrEqualTo( Math.max( 1e-10, rel * Math.abs( expected ) ) );
    }

    /**
     * Fixed-effect blocking (subject as design columns) reproduces limma lmFit + eBayes: coefficient, sigma,
     * moderated t and p for every factor level, on every probe.
     */
    @ParameterizedTest
    @ValueSource(strings = { "GSE33658", "GSE7400", "GSE95298", "GSE246769", "GSE32473", "GSE63857" })
    void pairedFitMatchesLimma( String gse ) throws Exception {
        Fixture f = load( gse );
        LeastSquaresFit fit = new LeastSquaresFit( f.design( true ), f.data );
        ModeratedTstat.ebayes( fit );

        assertThat( ( double ) fit.getResidualDof() ).isEqualTo( f.golden.header().get( "paired.df.residual" ) );
        assertClose( fit.getDfPrior(), f.golden.header().get( "paired.df.prior" ), 1e-6, "df.prior" );
        assertClose( fit.getVarPrior(), f.golden.header().get( "paired.s2.prior" ), 1e-6, "s2.prior" );

        Map<String, LinearModelSummary> summaries = fit.summarizeByKeys( false );
        List<String[]> rows = f.golden.rows().get( "paired" );
        assertThat( rows ).hasSize( 50 * f.golden.text().get( "paired.terms" ).split( "," ).length );
        for ( String[] r : rows ) {
            LinearModelSummary s = summaries.get( r[1] );
            String term = r[2];
            assertClose( s.getContrastCoefficients( "cond" ).get( term ), Double.parseDouble( r[3] ), 1e-7, gse + " " + r[1] + " " + term + " coef" );
            assertClose( s.getSigma(), Double.parseDouble( r[4] ), 1e-7, gse + " " + r[1] + " sigma" );
            assertClose( s.getContrastTStats( "cond" ).get( term ), Double.parseDouble( r[5] ), 1e-6, gse + " " + r[1] + " " + term + " t" );
            assertClose( s.getContrastPValues( "cond" ).get( term ), Double.parseDouble( r[6] ), 1e-5, gse + " " + r[1] + " " + term + " p" );
        }
    }

    /**
     * The consensus correlation Gemma estimates agrees with limma's duplicateCorrelation. Unbalanced blocks
     * leave repeated singular values whose basis is implementation-defined, hence the tolerance (as in
     * GSE17183RealDesignFitTest).
     */
    @ParameterizedTest
    @ValueSource(strings = { "GSE33658", "GSE7400", "GSE95298", "GSE246769", "GSE32473", "GSE63857", "GSE137" })
    void consensusCorrelationMatchesLimma( String gse ) throws Exception {
        Fixture f = load( gse );
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( blockFreeDesign( f ), f.blockIds(),
                new DenseDoubleMatrix2D( f.data.asArray() ), null );
        assertThat( mmf.isDegenerateToZero() ).isFalse();
        assertThat( mmf.getConsensusCorrelation() )
                .as( gse + " consensus correlation vs limma duplicateCorrelation" )
                .isCloseTo( f.golden.header().get( "mixed.consensus.correlation" ), org.assertj.core.data.Offset.offset( 2e-3 ) );
    }

    /**
     * The GLS fit at limma's own rho reproduces gls.series + eBayes: coefficients, sigma, moderated t and p.
     */
    @ParameterizedTest
    @ValueSource(strings = { "GSE33658", "GSE7400", "GSE95298", "GSE246769", "GSE32473", "GSE63857" })
    void mixedGlsAtLimmaRhoMatchesLimma( String gse ) throws Exception {
        Fixture f = load( gse );
        double rho = f.golden.header().get( "mixed.consensus.correlation" );
        LeastSquaresFit fit = new LeastSquaresFit( f.design( false ),
                MixedModelFit.blockCorrelationMatrix( f.blockIds(), rho ), f.data );
        ModeratedTstat.ebayes( fit );

        Map<String, LinearModelSummary> summaries = fit.summarizeByKeys( false );
        for ( String[] r : f.golden.rows().get( "mixed" ) ) {
            LinearModelSummary s = summaries.get( r[1] );
            String term = r[2];
            assertClose( s.getContrastCoefficients( "cond" ).get( term ), Double.parseDouble( r[3] ), 1e-7, gse + " " + r[1] + " " + term + " coef" );
            assertClose( s.getSigma(), Double.parseDouble( r[4] ), 1e-7, gse + " " + r[1] + " sigma" );
            assertClose( s.getContrastTStats( "cond" ).get( term ), Double.parseDouble( r[5] ), 1e-6, gse + " " + r[1] + " " + term + " t" );
            assertClose( s.getContrastPValues( "cond" ).get( term ), Double.parseDouble( r[6] ), 1e-5, gse + " " + r[1] + " " + term + " p" );
        }
    }

    /**
     * Intercept-only design with a block (35 individuals x 2, no recorded condition): the correlation is
     * estimable and nothing is encoded in the design, so there is no back-off to zero.
     */
    @Test
    void blockWithInterceptOnlyDesignHasACorrelation() throws Exception {
        Fixture f = load( "GSE137" );
        assertThat( f.hasCond() ).isFalse();
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( blockFreeDesign( f ), f.blockIds(),
                new DenseDoubleMatrix2D( f.data.asArray() ), null );
        assertThat( mmf.isDegenerateToZero() ).isFalse();
        assertThat( mmf.getConsensusCorrelation() ).isGreaterThan( 0.2 );
    }

    /**
     * Every individual measured once: no repeated measures. Gemma backs off to rho = 0 exactly as limma does
     * (consensus 0), and the fixed-effect model with the subject as columns has no residual dof.
     */
    @Test
    void everyIndividualMeasuredOnceDegeneratesToZero() throws Exception {
        Fixture f = load( "GSE55468" );
        assertThat( new HashSet<>( f.block ) ).hasSameSizeAs( f.block );
        MixedModelFit mmf = new MixedModelFit().estimateCorrelation( blockFreeDesign( f ), f.blockIds(),
                new DenseDoubleMatrix2D( f.data.asArray() ), null );
        assertThat( mmf.isDegenerateToZero() ).isTrue();
        assertThat( mmf.getConsensusCorrelation() ).isEqualTo( f.golden.header().get( "mixed.consensus.correlation" ) ).isZero();

        // subject columns plus intercept exceed the sample count: refused, not an index-out-of-bounds
        DesignMatrix paired = new DesignMatrix( f.info( true ), true );
        assertThatThrownBy( () -> new LeastSquaresFit( paired, f.data ) ).isInstanceOf( RuntimeException.class );
    }

    /**
     * rho = 0 in the GLS constructor is the ordinary least-squares fit, on a real design.
     */
    @Test
    void zeroCorrelationGlsEqualsOlsOnRealData() throws Exception {
        Fixture f = load( "GSE33658" );
        LeastSquaresFit gls = new LeastSquaresFit( f.design( false ),
                MixedModelFit.blockCorrelationMatrix( f.blockIds(), 0.0 ), f.data );
        LeastSquaresFit ols = new LeastSquaresFit( f.design( false ), f.data );
        for ( String probe : f.data.getRowNames() ) {
            assertClose( gls.summarizeByKeys( false ).get( probe ).getSigma(), ols.summarizeByKeys( false ).get( probe ).getSigma(), 1e-10, probe );
        }
    }

    private DoubleMatrix2D blockFreeDesign( Fixture f ) {
        if ( !f.hasCond() ) {
            DoubleMatrix2D ones = new DenseDoubleMatrix2D( f.samples.size(), 1 );
            ones.assign( 1.0 );
            return ones;
        }
        return f.design( false ).getDoubleMatrix();
    }
}
