package ubic.gemma.apps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.PublicationAssociationRole;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceReadService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.common.description.PublicationAssertion;
import ubic.gemma.persistence.service.common.description.PublicationAssociationService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * findDatasetPubs: GEO's papers are linked by their own ids.
 */
@ExtendWith(MockitoExtension.class)
class UpdatePubMedCliTest {

    @Mock
    private ExpressionExperimentService eeserv;
    @Mock
    private BibliographicReferenceReadService bibliographicReferenceReadService;
    @Mock
    private BibliographicReferenceService bibliographicReferenceService;
    @Mock
    private PublicationAssociationService publicationAssociationService;

    @InjectMocks
    private UpdatePubMedCli cli;

    /**
     * 🛑 The loop over GEO's second and later PMIDs looked up the first one every time: the primary
     * was added again as other-relevant, its PRIMARY assertion was rewritten as OTHER_RELEVANT, and the
     * further papers were never added.
     */
    @Test
    void testFurtherPapersAreLinkedByTheirOwnIds() {
        BibliographicReference primary = ref( 1L, "111" );
        BibliographicReference second = ref( 2L, "222" );
        when( bibliographicReferenceReadService.findByExternalId( "111" ) ).thenReturn( primary );
        when( bibliographicReferenceReadService.findByExternalId( "222" ) ).thenReturn( second );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setShortName( "GSE1" );

        cli.addPublications( ee, "GSE1", Arrays.asList( "111", "222" ) );

        assertThat( ee.getPrimaryPublication() ).isSameAs( primary );
        assertThat( ee.getOtherRelevantPublications() ).containsExactly( second );
        ArgumentCaptor<PublicationAssertion> otherRelevant = ArgumentCaptor.forClass( PublicationAssertion.class );
        verify( publicationAssociationService ).assertAccepted( eq( ee ), otherRelevant.capture(),
                eq( PublicationAssociationRole.OTHER_RELEVANT ) );
        assertThat( otherRelevant.getValue().getPublication() ).isSameAs( second );
        verify( eeserv ).update( ee );
    }

    private static BibliographicReference ref( Long id, String pmid ) {
        BibliographicReference ref = new BibliographicReference();
        ref.setId( id );
        DatabaseEntry acc = new DatabaseEntry();
        acc.setAccession( pmid );
        ref.setPubAccession( acc );
        return ref;
    }
}
