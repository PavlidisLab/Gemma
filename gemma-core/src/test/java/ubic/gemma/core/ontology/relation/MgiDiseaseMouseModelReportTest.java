package ubic.gemma.core.ontology.relation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reading MGI's disease-first model report.
 *
 * <p>The fixture is real rows from {@code MGI_DiseaseMouseModel.rpt} as fetched 2026-08-30, in the
 * report's own thirteen-column shape, including the four things that make it different from the
 * genotype report it sits beside: a {@code NOT} column instead of a second file, one statement spread
 * over several allele-pair/background rows, a disease listed with no model at all, and no PubMed
 * column anywhere.</p>
 */
class MgiDiseaseMouseModelReportTest {

    /**
     * Cp: two rows, one statement — same allele and disease on two allele-pair combinations. Fgfr3:
     * a {@code NOT} model. Apoe: the same pair stated both ways. 2-hydroxyglutaric aciduria: a disease
     * whose allele columns are simply empty.
     */
    private static final String REPORT = String.join( "\n",
            "# This report queried MGI data as follows:",
            "# NOT model: NOT appears when the authors have determined that the genotype does not"
                    + " sufficiently model the disease.",
            row( "aceruloplasminemia", "DOID:0050711", "", "Cp<tm1Hrs>/Cp<tm1Hrs>",
                    "involves: 129X1/SvJ * Black Swiss", "Cp<tm1Hrs>", "MGI:1857981", "Cp", "MGI:88476" ),
            row( "aceruloplasminemia", "DOID:0050711", "", "Cp<tm1Hrs>/Cp<tm1Hrs>Heph<sla>/Y",
                    "involves: 129X1/SvJ * C57BL/6", "Cp<tm1Hrs>", "MGI:1857981", "Cp", "MGI:88476" ),
            row( "achondroplasia", "DOID:4480", "NOT", "Fgfr3<tm1Dor>/Fgfr3<tm1Dor>",
                    "involves: 129S6/SvEvTac * C57BL/6", "Fgfr3<tm1Dor>", "MGI:1931521", "Fgfr3", "MGI:95524" ),
            row( "coronary artery disease", "DOID:3393", "", "Apoe<tm1Unc>/Apoe<tm1Unc>Scarb1<tm1Kri>",
                    "involves: 129P2/OlaHsd * BALB/c", "Apoe<tm1Unc>", "MGI:1857129", "Apoe", "MGI:88057" ),
            row( "coronary artery disease", "DOID:3393", "NOT", "Apoe<tm1Unc>/Apoe<tm1Unc>Svep1<em1Nost>",
                    "involves: 129P2/OlaHsd * C57BL/6", "Apoe<tm1Unc>", "MGI:1857129", "Apoe", "MGI:88057" ),
            // 7,006 of the real file's 15,471 rows look like this: a disease with no mouse model
            "2-hydroxyglutaric aciduria\tDOID:0050573\t\t\t\t\t\t\t\t\t\t\t\t",
            "short\tline\twith\tfew\tcolumns",
            "" ) + "\n";

    /** The thirteen columns the report's own header block documents, in order. */
    private static String row( String doName, String doid, String not, String allelePairs, String background,
            String alleleSymbol, String alleleId, String markerSymbol, String markerId ) {
        return String.join( "\t", doName, doid, not, allelePairs, background, alleleSymbol, alleleId,
                // 8 total allele references -- a COUNT, not a citation
                "31", "JAX:003582", "RRID:MGI:3044689", markerSymbol, markerId, "TIGM:IST11443F3|RBRC10271" );
    }

    private List<MgiDiseaseMouseModelReport.Entry> parse() throws Exception {
        return new ArrayList<>( MgiDiseaseMouseModelReport.parse(
                new ByteArrayInputStream( REPORT.getBytes( StandardCharsets.UTF_8 ) ) ) );
    }

    /**
     * One row per allele-pair/strain-background combination, not per relation — the same shape as the
     * genotype report's phenotype rows and the same reason to collapse it: counting rows would make an
     * allele studied on many backgrounds look better attested than one studied on a single background.
     */
    @Test
    void severalAllelePairRowsCollapseIntoOneStatement() throws Exception {
        List<MgiDiseaseMouseModelReport.Entry> entries = parse();

        assertThat( entries ).extracting( MgiDiseaseMouseModelReport.Entry::getAlleleSymbol,
                        MgiDiseaseMouseModelReport.Entry::getDoid )
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple( "Cp<tm1Hrs>", "DOID:0050711" ),
                        org.assertj.core.api.Assertions.tuple( "Fgfr3<tm1Dor>", "DOID:4480" ),
                        org.assertj.core.api.Assertions.tuple( "Apoe<tm1Unc>", "DOID:3393" ),
                        org.assertj.core.api.Assertions.tuple( "Apoe<tm1Unc>", "DOID:3393" ) );
        assertThat( entries.get( 0 ).getRows() )
                .as( "two allele-pair rows, one statement" )
                .isEqualTo( 2 );
        assertThat( entries.get( 0 ).getAlleleId() ).isEqualTo( "MGI:1857981" );
        assertThat( entries.get( 0 ).getGeneId() ).isEqualTo( "MGI:88476" );
    }

    /**
     * 🛑 The {@code NOT} column is this report's whole refutation channel — 338 rows, 236 distinct
     * pairs on the 2026-08-30 file. Read past it and every one of them is stored asserting exactly
     * what MGI's curators recorded as false.
     */
    @Test
    void theNotColumnIsRead() throws Exception {
        List<MgiDiseaseMouseModelReport.Entry> entries = parse();

        assertThat( entries )
                .extracting( MgiDiseaseMouseModelReport.Entry::getAlleleSymbol,
                        MgiDiseaseMouseModelReport.Entry::isNotModel )
                .contains( org.assertj.core.api.Assertions.tuple( "Fgfr3<tm1Dor>", true ),
                        org.assertj.core.api.Assertions.tuple( "Cp<tm1Hrs>", false ) );
    }

    /**
     * 🛑 A pair MGI states both ways stays two statements. It really does state both — 60 pairs on the
     * 2026-08-30 file, at different allele pairs or strain backgrounds. Keying the dedup on the pair
     * alone would keep whichever the file printed first and silently discard the other, which is a
     * precedence decision made by row order rather than by rule.
     */
    @Test
    void aPairStatedBothWaysStaysTwoStatements() throws Exception {
        List<MgiDiseaseMouseModelReport.Entry> apoe = new ArrayList<>();
        parse().stream().filter( e -> e.getAlleleSymbol().equals( "Apoe<tm1Unc>" ) ).forEach( apoe::add );

        assertThat( apoe ).hasSize( 2 );
        assertThat( apoe ).extracting( MgiDiseaseMouseModelReport.Entry::isNotModel )
                .containsExactly( false, true );
    }

    /**
     * 🛑 Nothing here is cited, and column 8 is not a way round that: it is a count of the papers about
     * the ALLELE, which says nothing about who, if anyone, reported that it models this disease.
     * Storing it as evidence would put a number where a PMID belongs and promote every one of these
     * statements to {@code TAS}.
     */
    @Test
    void noStatementIsCitedBecauseTheReportHasNoPubMedColumn() throws Exception {
        assertThat( parse() ).allSatisfy( e -> {
            assertThat( e.getCitations() ).isEmpty();
            assertThat( e.getEvidence() ).isNull();
        } );
    }

    /**
     * The report lists every disease with a one-to-one mouse ortholog whether or not a model exists —
     * 7,006 of 15,471 rows. A row with no subject is not a relation.
     */
    @Test
    void aDiseaseWithNoModelYieldsNoStatement() throws Exception {
        assertThat( parse() ).extracting( MgiDiseaseMouseModelReport.Entry::getDoid )
                .doesNotContain( "DOID:0050573" );
    }

    /** Header comments and lines that are not this report's shape are skipped, not fatal. */
    @Test
    void unusableLinesAreSkippedRatherThanFatal() throws Exception {
        assertThat( parse() ).hasSize( 4 );
    }
}
