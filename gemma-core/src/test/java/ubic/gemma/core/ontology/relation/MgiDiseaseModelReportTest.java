package ubic.gemma.core.ontology.relation;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reading MGI's genotype-to-disease report.
 *
 * <p>The fixture is real rows from {@code MGI_Geno_DiseaseDO.rpt}, including the shape that matters
 * most: one genotype-disease statement spread over several rows because MGI recorded several
 * phenotypes for it.</p>
 */
class MgiDiseaseModelReportTest {

    /** Ednra: three phenotype rows, ONE statement. Pax3: a second statement, one row, no PubMed. */
    private static final String REPORT = String.join( "\n",
            "# a comment line the report really does carry",
            row( "Ednra<tm1Ywa>/Ednra<tm1Ywa>", "Ednra<tm1Ywa>", "MGI:1857473", "MP:0002127", "9449664", "MGI:105923", "DOID:12583" ),
            row( "Ednra<tm1Ywa>/Ednra<tm1Ywa>", "Ednra<tm1Ywa>", "MGI:1857473", "MP:0000452", "9449664", "MGI:105923", "DOID:12583" ),
            row( "Ednra<tm1Ywa>/Ednra<tm1Ywa>", "Ednra<tm1Ywa>", "MGI:1857473", "MP:0002108", "9449664", "MGI:105923", "DOID:12583" ),
            row( "Pax3<Sp-2H>/Pax3<Sp-2H>", "Pax3<Sp-2H>", "MGI:1856293", "MP:0003054", "", "MGI:97487", "DOID:0110949" ),
            // a different disease on the same allele is a different statement
            row( "Ednra<tm1Ywa>/Ednra<tm1Ywa>", "Ednra<tm1Ywa>", "MGI:1857473", "MP:0002127", "9449664", "MGI:105923", "DOID:999" ),
            "short\tline\twith\tfew\tcolumns",
            "" ) + "\n";

    private static String row( String composition, String allele, String alleleId, String mp, String pubmed,
            String gene, String doid ) {
        return String.join( "\t", composition, allele, alleleId, "involves: C57BL/6", mp, pubmed, gene, doid,
                "OMIM:192430", "MGI:2166570" );
    }

    private List<MgiDiseaseModelReport.Entry> parse() throws Exception {
        return new ArrayList<>( MgiDiseaseModelReport.parse(
                new ByteArrayInputStream( REPORT.getBytes( StandardCharsets.UTF_8 ) ) ) );
    }

    /**
     * 🛑 The report has one row per PHENOTYPE. Reading rows as relations would make a well-phenotyped
     * genotype look many times better attested than a sparsely-phenotyped one — the real file is
     * 53,950 rows and 4,124 statements, a factor of thirteen.
     */
    @Test
    void phenotypeRowsCollapseIntoOneStatement() throws Exception {
        List<MgiDiseaseModelReport.Entry> entries = parse();

        assertThat( entries ).hasSize( 3 );
        assertThat( entries.get( 0 ).getAlleleSymbol() ).isEqualTo( "Ednra<tm1Ywa>" );
        assertThat( entries.get( 0 ).getDoid() ).isEqualTo( "DOID:12583" );
        assertThat( entries.get( 0 ).getRows() )
                .as( "three phenotypes, one statement" )
                .isEqualTo( 3 );
    }

    /**
     * The same allele against a different disease is a different statement, so the key is the pair and
     * not the allele.
     */
    @Test
    void oneAlleleCanModelMoreThanOneDisease() throws Exception {
        assertThat( parse() )
                .filteredOn( e -> e.getAlleleSymbol().equals( "Ednra<tm1Ywa>" ) )
                .extracting( MgiDiseaseModelReport.Entry::getDoid )
                .containsExactlyInAnyOrder( "DOID:12583", "DOID:999" );
    }

    /**
     * The citation survives the collapse, since it decides whether this is a traceable author
     * statement or an import whose own basis we cannot see. 94% of real pairs carry one.
     */
    @Test
    void theCitationSurvivesAndItsAbsenceIsRecorded() throws Exception {
        List<MgiDiseaseModelReport.Entry> entries = parse();

        assertThat( entries.get( 0 ).getPubMedId() ).isEqualTo( "9449664" );
        assertThat( entries )
                .filteredOn( e -> e.getAlleleSymbol().equals( "Pax3<Sp-2H>" ) )
                .singleElement()
                .satisfies( e -> assertThat( e.getPubMedId() ).isNull() );
    }

    /**
     * A comment, a short line and a blank are all things the report legitimately contains, and none of
     * them may cost the rows around it.
     */
    @Test
    void unusableLinesAreSkippedRatherThanFatal() throws Exception {
        assertThat( parse() ).hasSize( 3 );
    }
}
