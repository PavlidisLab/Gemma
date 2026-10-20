package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import ubic.gemma.core.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.search.OntologySearchResult;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MeshDiseaseSynonymOntologyServiceTest {

    private static final String T2D = "http://purl.obolibrary.org/obo/MONDO_0005148";
    private static final String LARYNGEAL_NEOPLASM = "http://purl.obolibrary.org/obo/MONDO_0021071";

    /** A few rows in the shipped table's exact shape, provenance comments and all. */
    private static final String TSV = String.join( "\n",
            "# MeSH entry terms as MONDO disease synonyms -- PRECOMPUTED, do not hand-edit.",
            "# script: scripts/build_mesh_disease_synonyms.py",
            "# built: 2026-08-15T01:58:18+00:00",
            "# mondo: releases/2026-08-04",
            "mondo_uri\tmondo_label\tsynonym\tmesh_id",
            T2D + "\ttype 2 diabetes mellitus\tNon-Insulin-Dependent Diabetes Mellitus\tMESH:D003924",
            T2D + "\ttype 2 diabetes mellitus\tMaturity-Onset Diabetes Mellitus\tMESH:D003924",
            LARYNGEAL_NEOPLASM + "\tlaryngeal neoplasm\tLaryngeal Neoplasms\tMESH:D007822",
            "" );

    private MeshDiseaseSynonymOntologyService load() {
        MeshDiseaseSynonymOntologyService s = new MeshDiseaseSynonymOntologyService();
        s.initialize( new ByteArrayInputStream( TSV.getBytes( StandardCharsets.UTF_8 ) ), true );
        return s;
    }

    @Test
    void groupsRowsByUriAndReadsProvenance() throws Exception {
        MeshDiseaseSynonymOntologyService s = load();
        assertTrue( s.isOntologyLoaded() );
        // the MONDO release the strings were joined against has to reach /admin/ontologies: the table
        // is only ever as current as that release
        assertNotNull( s.getVersion() );
        assertTrue( s.getVersion().startsWith( "releases/2026-08-04" ), s.getVersion() );
        // both MeSH strings for T2D must land on the one MONDO term, not two terms
        assertEquals( 1, search( s, "Non-Insulin-Dependent Diabetes Mellitus" ).size() );
        assertEquals( 1, search( s, "Maturity-Onset Diabetes Mellitus" ).size() );
        s.close();
    }

    @Test
    void resolvesAMeshStringToItsMondoUriAndLabel() throws Exception {
        MeshDiseaseSynonymOntologyService s = load();
        Collection<OntologySearchResult<OntologyTerm>> hits = search( s, "Non-Insulin-Dependent Diabetes Mellitus" );
        OntologyTerm t = hits.iterator().next().getResult();
        assertEquals( T2D, t.getUri() );
        // the label is MONDO's, not MeSH's -- the client must see the term it actually resolved to
        assertEquals( "type 2 diabetes mellitus", t.getLabel() );
        s.close();
    }

    /**
     * The service contributes search strings only. {@code OntologyServiceImpl.getTerm} takes the
     * first non-null answer across every service and {@code getTerms} unions them, so answering for
     * a MONDO URI here would let a bare label stand in for, or duplicate, the real MONDO term.
     */
    @Test
    void neverAnswersForAMondoUriOutsideSearch() throws Exception {
        MeshDiseaseSynonymOntologyService s = load();
        assertNull( s.getTerm( T2D ) );
        assertNull( s.getResource( T2D ) );
        assertTrue( s.getAllURIs().isEmpty() );
        // ... while search still resolves it
        assertFalse( search( s, "Non-Insulin-Dependent Diabetes Mellitus" ).isEmpty() );
        s.close();
    }

    /**
     * Guards the shipped table itself, since that is where the conservatism decisions live.
     * <p>
     * Every row must carry a MONDO URI — this source mints none of its own — and no row may carry a
     * string from a narrower MeSH concept. "Laryngeal Cancer" is the worked example: MeSH marks it
     * NRW of the "Laryngeal Neoplasms" heading that MONDO maps to MONDO:0021071, so indexing it
     * would ground a cancer to its benign-or-malignant parent. If a rebuild ever starts emitting
     * non-preferred concepts, this fails.
     * <p>
     * Note this is deliberately NOT a search assertion: querying "Laryngeal Cancer" does return
     * MONDO:0021071 by matching the token "laryngeal" in "Laryngeal Neoplasms", which is ordinary
     * partial matching and not this table's doing.
     */
    @Test
    void shippedTableCarriesOnlyMondoUrisAndNoNarrowerConceptStrings() throws Exception {
        List<String[]> rows = readShippedTable();
        assertFalse( rows.isEmpty(), "the precomputed table is missing from the classpath" );
        for ( String[] f : rows ) {
            assertTrue( f[0].startsWith( MeshDiseaseSynonymOntologyService.URI_PREFIX ),
                    "non-MONDO URI in the table: " + f[0] );
        }
        assertTrue( rows.stream().noneMatch( f -> "laryngeal cancer".equalsIgnoreCase( f[2] ) ),
                "\"Laryngeal Cancer\" is a narrower MeSH concept's term and must not be in the table" );
    }

    private List<String[]> readShippedTable() throws Exception {
        try ( InputStream is = MeshDiseaseSynonymOntologyService.class
                .getResourceAsStream( "/ubic/gemma/core/ontology/mesh-disease-synonyms.tsv" ) ) {
            if ( is == null ) {
                return List.of();
            }
            try ( BufferedReader r = new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ) ) {
                return r.lines()
                        .filter( l -> !l.startsWith( "#" ) && !l.isEmpty() && !l.startsWith( "mondo_uri" ) )
                        .map( l -> l.split( "\t", -1 ) )
                        .filter( f -> f.length >= 3 )
                        .collect( Collectors.toList() );
            }
        }
    }

    @Test
    void isSupplementarySoItCannotOutrankMondoItself() {
        assertTrue( new MeshDiseaseSynonymOntologyService().isSupplementary() );
    }

    private Collection<OntologySearchResult<OntologyTerm>> search( MeshDiseaseSynonymOntologyService s, String q )
            throws Exception {
        return s.findTerm( q, 10, false );
    }
}
