package ubic.gemma.core.ontology;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static ubic.gemma.core.ontology.OntologyUtils.getLabelFromTermUri;

public class OntologyUtilsTest {

    @Test
    public void testGoTerm() {
        assertEquals( "GO:0004016", getLabelFromTermUri( "http://purl.obolibrary.org/obo/GO_0004016" ) );
        assertEquals( "CHEBI:7466", getLabelFromTermUri( "http://purl.obolibrary.org/obo/chebi.owl#CHEBI_7466" ) );
        assertEquals( "BIRNLEX:15001", getLabelFromTermUri( "http://ontology.neuinfo.org/NIF/Function/NIF-Function.owl#birnlex_15001" ) );
        assertEquals( "GO:0004016", getLabelFromTermUri( "http://purl.obolibrary.org/obo//GO_0004016//" ) );
        assertEquals( "http://purl.obolibrary.org//", getLabelFromTermUri( "http://purl.obolibrary.org////" ) );
        assertEquals( "PAT:ID_20327", getLabelFromTermUri( "http://www.orphanet.org/rdfns#pat_id_20327" ) );
        assertEquals( "PAT:ID_20327", getLabelFromTermUri( "http://www.orphanet.org/rdfns#pat_id_20327" ) );
        assertEquals( "63857", getLabelFromTermUri( "http://purl.org/commons/record/ncbi_gene/63857" ) );
    }

}