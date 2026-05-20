package ubic.gemma.core.ontology.providers;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import ubic.gemma.core.ontology.basecode.model.OntologyTerm;
import ubic.gemma.core.util.test.NetworkAvailable;
import ubic.gemma.core.util.test.NetworkAvailableExtension;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(NetworkAvailableExtension.class)
public class GemmaOntologyServiceTest {

    @Test
    @NetworkAvailable(url = "https://raw.githubusercontent.com/PavlidisLab/TGEMO/master/TGEMO.OWL")
    public void test() {
        GemmaOntologyService gemmaOntology = new GemmaOntologyService();
        gemmaOntology.setSearchEnabled( false );
        gemmaOntology.setProcessImports( false ); // FIXME: remove this once https://github.com/PavlidisLab/TGEMO/pull/20 is merged
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.LanguageLevel.FULL, gemmaOntology.getLanguageLevel() );
        assertEquals( ubic.gemma.core.ontology.basecode.providers.OntologyService.InferenceMode.TRANSITIVE, gemmaOntology.getInferenceMode() );
        gemmaOntology.initialize( true, false );
        OntologyTerm overexpression = gemmaOntology.getTerm( "http://gemma.msl.ubc.ca/ont/TGEMO_00004" );
        assertNotNull( overexpression );
        assertThat( gemmaOntology.getParents( Collections.singleton( overexpression ), false, false ) )
                .extracting( OntologyTerm::getUri )
                .contains( "http://www.ebi.ac.uk/efo/EFO_0000510" );
    }
}
