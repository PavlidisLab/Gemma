package ubic.gemma.rest;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the category→namespace exclusion table, specifically the {@code *} wildcard and
 * the preference-list opt-out that lets one category escape it.
 *
 * <p>The wildcard exists because GO is the one namespace that is the wrong KIND of answer under
 * essentially every category — a GO term is a process, function or component, so under any category
 * naming a thing an experiment is done to or with it cannot be right. Denying it per-category would
 * mean listing every category by hand and failing open the day one is added.</p>
 *
 * <p>Cases and the 1-of-1,018 gold measurement come from
 * {@code handoffs/CAB_TO_GEMBRO_2026_08_14_GATE_GO_ON_CATEGORY_AND_DECLARE_THE_TABLE_ONCE.md}.</p>
 */
class AnnotationsCategoryExclusionTest {

    /** The tables as shipped in default.properties. */
    private static final String PREFIXES =
            "treatment:CHEBI_,EFO_;organismPart:UBERON_,EMAPA_,EFO_;biologicalProcess:GO_";
    private static final String EXCLUDED = "genotype:MONDO_;treatment:MGI:;*:GO_";

    private static AnnotationsWebService service( String prefixes, String excluded ) throws Exception {
        AnnotationsWebService s = new AnnotationsWebService();
        set( s, "categoryPrefixesRaw", prefixes );
        set( s, "categoryExcludedPrefixesRaw", excluded );
        return s;
    }

    private static void set( Object target, String field, Object value ) throws Exception {
        Field f = AnnotationsWebService.class.getDeclaredField( field );
        f.setAccessible( true );
        f.set( target, value );
    }

    @Test
    void wildcardDeniesGoForAnOrdinaryCategory() throws Exception {
        assertThat( service( PREFIXES, EXCLUDED ).resolveCategoryExcludedPrefixes( "treatment" ) )
                .contains( "GO_" );
    }

    @Test
    void wildcardIsUnionedWithTheCategorySpecificDenial() throws Exception {
        // treatment must keep its own MGI: rule; the wildcard adds to it rather than replacing it.
        assertThat( service( PREFIXES, EXCLUDED ).resolveCategoryExcludedPrefixes( "treatment" ) )
                .containsExactlyInAnyOrder( "MGI:", "GO_" );
    }

    @Test
    void preferringTheNamespaceOptsTheCategoryOutOfTheWildcard() throws Exception {
        // biological process is the one category where a GO term is the right kind of answer —
        // 1 of 1,018 gold tags. It escapes by naming GO_ in the preference table.
        assertThat( service( PREFIXES, EXCLUDED ).resolveCategoryExcludedPrefixes( "biological process" ) )
                .doesNotContain( "GO_" );
    }

    @Test
    void optOutMatchesHoweverTheCategoryIsSpelled() throws Exception {
        AnnotationsWebService s = service( PREFIXES, EXCLUDED );
        for ( String spelling : List.of( "biological process", "biologicalProcess", "Biological Process" ) ) {
            assertThat( s.resolveCategoryExcludedPrefixes( spelling ) )
                    .as( "spelling %s", spelling )
                    .doesNotContain( "GO_" );
        }
    }

    @Test
    void aCategoryWithNoRulesStillInheritsTheWildcard() throws Exception {
        // The point of the wildcard: a category nobody has configured is still protected, where a
        // hand-listed denial would have failed open.
        assertThat( service( PREFIXES, EXCLUDED ).resolveCategoryExcludedPrefixes( "diet" ) )
                .containsExactly( "GO_" );
    }

    @Test
    void withNoWildcardConfiguredNothingChanges() throws Exception {
        AnnotationsWebService s = service( PREFIXES, "genotype:MONDO_;treatment:MGI:" );
        assertThat( s.resolveCategoryExcludedPrefixes( "treatment" ) ).containsExactly( "MGI:" );
        assertThat( s.resolveCategoryExcludedPrefixes( "diet" ) ).isEmpty();
    }

    @Test
    void blankCategoryExcludesNothing() throws Exception {
        AnnotationsWebService s = service( PREFIXES, EXCLUDED );
        assertThat( s.resolveCategoryExcludedPrefixes( null ) ).isEmpty();
        assertThat( s.resolveCategoryExcludedPrefixes( "  " ) ).isEmpty();
    }

    @Test
    void wildcardKeySurvivesTheCategoryKeyFold() {
        // categoryKey strips every non-alphanumeric, so "*" would collapse to "" and the wildcard
        // would silently never match. Guarding the parser's carve-out directly.
        assertThat( AnnotationsWebService.categoryKey( "*" ) ).isEmpty();
        assertThat( AnnotationsWebService.parseCategoryPrefixProperty( "*:GO_" ) )
                .containsKey( AnnotationsWebService.WILDCARD_KEY );
    }

    /**
     * The disease category's live label is {@code obsolete_disease} (EFO obsoleted its own term in
     * favour of MONDO's while ~15k annotations still use the old URI). Any table keyed on the label
     * has to fold that away or disease silently escapes every rule.
     */
    @Test
    void obsoleteLabelledDiseaseStillInheritsTheWildcard() throws Exception {
        assertThat( service( PREFIXES, EXCLUDED ).resolveCategoryExcludedPrefixes( "obsolete_disease" ) )
                .contains( "GO_" );
    }

    // ---- the shipped tables -----------------------------------------------------------------

    private static Map<String, List<String>> shipped( String key ) throws Exception {
        Properties p = new Properties();
        try ( InputStream is = AnnotationsWebService.class.getResourceAsStream( "/default.properties" ) ) {
            assertThat( is ).as( "default.properties on the classpath" ).isNotNull();
            p.load( is );
        }
        return AnnotationsWebService.parseCategoryPrefixProperty( p.getProperty( key ) );
    }

    /**
     * Guards the gold-derived preference rows. These categories declared nothing before, and an
     * absent category gets no promotion at all, so a silent drop here is invisible in behaviour
     * until someone measures grounding again.
     */
    @Test
    void shippedPreferencesCoverTheGoldDerivedCategories() throws Exception {
        Map<String, List<String>> prefs = shipped( "annotation.category.prefixes" );
        assertThat( prefs ).containsEntry( "assay", List.of( "OBI_" ) );
        assertThat( prefs ).containsEntry( "biologicalsex", List.of( "PATO_" ) );
        assertThat( prefs ).containsEntry( "strain", List.of( "EFO_", "TGEMO_" ) );
        assertThat( prefs ).containsEntry( "studydesign", List.of( "TGEMO_", "OBI_" ) );
        assertThat( prefs ).containsEntry( "diet", List.of( "EFO_" ) );
        // NBO carries `fear conditioning`, which nothing else loaded has.
        assertThat( prefs.get( "treatment" ) ).contains( "NBO_" );
    }

    /**
     * The genotype row is the one that looks wrong and isn't. Matching is
     * {@code uri.contains(token)} rather than startsWith, so a token need not be an OBO-style
     * prefix — which is equally why {@code treatment:MGI:} matches
     * {@code .../strain/MGI:3028467}. Gold's genotype values are ncbi_gene records 15 of 17, so
     * dropping this token would leave the preference describing only the minority.
     */
    @Test
    void genotypePrefersNonPrefixShapedGeneRecordsFirst() throws Exception {
        List<String> genotype = shipped( "annotation.category.prefixes" ).get( "genotype" );
        assertThat( genotype ).isNotNull();
        assertThat( genotype.get( 0 ) ).isEqualTo( "ncbi_gene/" );
        assertThat( "http://purl.org/commons/record/ncbi_gene/22059" ).contains( genotype.get( 0 ) );
    }

    /** The shipped wildcard denial, and the one category that escapes it. */
    @Test
    void shippedTablesDenyGoEverywhereExceptBiologicalProcess() throws Exception {
        Properties p = new Properties();
        try ( InputStream is = AnnotationsWebService.class.getResourceAsStream( "/default.properties" ) ) {
            p.load( is );
        }
        AnnotationsWebService s = service( p.getProperty( "annotation.category.prefixes" ),
                p.getProperty( "annotation.category.excludedPrefixes" ) );
        assertThat( s.resolveCategoryExcludedPrefixes( "biological process" ) ).doesNotContain( "GO_" );
        for ( String c : List.of( "treatment", "disease", "cell type", "assay", "organism part" ) ) {
            assertThat( s.resolveCategoryExcludedPrefixes( c ) ).as( "category %s", c ).contains( "GO_" );
        }
    }
}
