package ubic.gemma.core.loader.expression.cellxgene;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.auditAndSecurity.eventType.CommentedEvent;
import ubic.gemma.model.common.description.DatabaseEntry;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.ExternalDatabases;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.description.DatabaseEntryService;
import ubic.gemma.persistence.service.common.description.ExternalDatabaseService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Covers {@link CellXGeneDataLoaderService#replaceVersionAccession(ExpressionExperiment, String)} outside any test
 * transaction, as {@code updateCELLxGENEAccessions} calls it.
 */
public class CellXGeneAccessionReplacementTest extends BaseIntegrationTest5 {

    private static final String COLLECTION_URI = "https://cellxgene.cziscience.com/collections/f406a653-c079-4bf9-aab6-85846c27571d";
    private static final String DATASET_ID = "56a4bc14-9407-45fd-8786-f7f6fc87c9dd";
    private static final String VERSION_ID = "ecd5537e-9561-43f2-9bb0-9d0b911084fe";

    @Autowired
    private CellXGeneDataLoaderService cellXGeneDataLoaderService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private ExternalDatabaseService externalDatabaseService;

    @Autowired
    private DatabaseEntryService databaseEntryService;

    @Autowired
    private AuditEventService auditEventService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = testHelper.getTestPersistentBasicExpressionExperiment();
        ExternalDatabase cellxgene = requireNonNull( externalDatabaseService.findByName( ExternalDatabases.CELLXGENE ) );
        // the shape addCELLxGENEData stored before permanent IDs were recorded
        DatabaseEntry versionAccession = DatabaseEntry.Factory.newInstance( VERSION_ID, cellxgene );
        versionAccession.setUri( COLLECTION_URI );
        ee.setAccession( versionAccession );
        expressionExperimentService.update( ee );
    }

    @AfterEach
    public void tearDown() {
        if ( ee != null ) {
            expressionExperimentService.remove( ee );
        }
    }

    @Test
    public void testReplaceVersionAccession() {
        Long previousId = requireNonNull( expressionExperimentService.thawLite( ee ).getAccession() ).getId();

        DatabaseEntry replacement = cellXGeneDataLoaderService.replaceVersionAccession( ee, DATASET_ID );
        assertThat( replacement.getAccession() ).isEqualTo( DATASET_ID );
        assertThat( replacement.getAccessionVersion() ).isEqualTo( VERSION_ID );
        assertThat( replacement.getUri() ).isEqualTo( COLLECTION_URI );

        DatabaseEntry stored = requireNonNull( expressionExperimentService.thawLite( ee ).getAccession() );
        assertThat( stored.getId() ).isNotEqualTo( previousId );
        assertThat( stored.getAccession() ).isEqualTo( DATASET_ID );
        assertThat( stored.getAccessionVersion() ).isEqualTo( VERSION_ID );
        assertThat( stored.getExternalDatabase().getName() ).isEqualTo( ExternalDatabases.CELLXGENE );
        assertThat( databaseEntryService.load( previousId ) ).isNull();

        assertThat( auditEventService.getEvents( ee ) )
                .anySatisfy( e -> {
                    assertThat( e.getEventType() ).isInstanceOf( CommentedEvent.class );
                    assertThat( e.getNote() ).contains( DATASET_ID ).contains( VERSION_ID );
                } );

        // a second run leaves the experiment alone
        assertThatThrownBy( () -> cellXGeneDataLoaderService.replaceVersionAccession( ee, DATASET_ID ) )
                .isInstanceOf( IllegalStateException.class )
                .hasMessageContaining( "already has a versioned CELLxGENE accession" );
    }
}
