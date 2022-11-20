package ubic.gemma.persistence.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.persistence.util.Settings;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author poirigui
 */
public class TableMaintenanceUtilIntegrationTest extends BaseSpringContextTest {

    private static final Path GENE2CS_PATH = Paths.get( Settings.getString( "gemma.appdata.home" ), "DbReports", "gene2cs.info" );
    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Before
    @After
    public void removeGene2CsStatusFileAndDirectory() {
        File f = GENE2CS_PATH.toFile();
        if ( f.exists() ) {
            assertThat( f.delete() ).isTrue();
            // also remove the parent folder
            assertThat( f.getParentFile().delete() ).isTrue();
        }
    }

    @Test
    public void testWhenUserIsAgent() {
        this.runAsAgent();
        tableMaintenanceUtil.updateGene2CsEntries();
        assertThat( GENE2CS_PATH ).exists();
    }

    @Test(expected = AccessDeniedException.class)
    public void testWhenUserIsAnonymous() {
        this.runAsAnonymous();
        tableMaintenanceUtil.updateGene2CsEntries();
    }
}
