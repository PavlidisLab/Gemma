package ubic.gemma.core.ontology;

import org.junit.Test;
import ubic.basecode.ontology.model.AnnotationProperty;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.simple.OntologyTermSimple;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OntologyExternalLinksTest {

    @Test
    public void test() throws IOException {
        OntologyExternalLinks ontologyExternalLinks = new OntologyExternalLinks( false );
        assertEquals( "https://gemma.msl.ubc.ca/ont/TGEMO_0000001", ontologyExternalLinks.getExternalLink( new OntologyTermSimple( "http://gemma.msl.ubc.ca/ont/TGEMO_0000001", "" ) ) );
        assertEquals( "https://gemma.msl.ubc.ca/ont/TGFVO/1", ontologyExternalLinks.getExternalLink( new OntologyTermSimple( "http://gemma.msl.ubc.ca/ont/TGFVO/1", "" ) ) );
        assertEquals( "https://gemma.msl.ubc.ca/ont/TGFVO/1/2", ontologyExternalLinks.getExternalLink( new OntologyTermSimple( "http://gemma.msl.ubc.ca/ont/TGFVO/1/2", "" ) ) );
        assertEquals( "https://www.ncbi.nlm.nih.gov/gene/123", ontologyExternalLinks.getExternalLink( new OntologyTermSimple( "http://purl.org/commons/record/ncbi_gene/123", "" ) ) );
        assertEquals( "https://www.ncbi.nlm.nih.gov/gene/123", ontologyExternalLinks.getExternalLink( new OntologyTermSimple( "http://purl.org/commons/record/ncbi_gene/123", "" ) ) );
        OntologyTerm goTerm = mock();
        AnnotationProperty ap = mock();
        when( ap.getProperty() ).thenReturn( "http://www.geneontology.org/formats/oboInOwl#id" );
        when( ap.getContents() ).thenReturn( "GO:0000028" );
        when( goTerm.getUri() ).thenReturn( "http://purl.obolibrary.org/obo/GO_0000028" );
        when( goTerm.getAnnotation( "http://www.geneontology.org/formats/oboInOwl#id" ) ).thenReturn( ap );
        assertEquals( "https://amigo.geneontology.org/amigo/term/GO%3A0000028", ontologyExternalLinks.getExternalLink( goTerm ) );
    }
}