package ubic.gemma.core.ontology.providers;

import org.junit.Test;
import ubic.basecode.ontology.jena.TdbOntologyService;

import java.nio.file.Paths;

public class UnifiedOntologyServiceTest {

    @Test
    public void test() throws Exception {
        try ( TdbOntologyService os = new TdbOntologyService( "Gemma Unified Ontology", Paths.get( "/home/guillaume/Projets/Gemma/gemma-data/ontology/tdb" ), null, true, "unified" ) ) {
            os.setProcessImports( false );
            os.initialize( false, false );
            System.out.println( os.findTerm( "test", 10 ) );
        }
    }
}