package ubic.gemma.persistence.service.maintenance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import java.io.File;
import java.nio.file.Path;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author poirigui
 */
@TestExecutionListeners(value = WithSecurityContextTestExecutionListener.class,
        mergeMode = TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS)
public class TableMaintenanceUtilIntegrationTest extends BaseIntegrationTest5 {

    @Autowired
    private TableMaintenanceUtil tableMaintenanceUtil;

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    @Value("${gemma.gene2cs.path}")
    private Path gene2CsPath;

    @BeforeEach
    @AfterEach
    public void removeGene2CsStatusFileAndDirectory() {
        File f = gene2CsPath.toFile();
        if ( f.exists() ) {
            assertThat( f.delete() ).isTrue();
            // also remove the parent folder
            assertThat( f.getParentFile().delete() ).isTrue();
        }
    }

    @Test
    @WithMockUser(authorities = "GROUP_ADMIN")
    public void test() {
        tableMaintenanceUtil.updateGene2CsEntries();
        assertThatThrownBy( () -> tableMaintenanceUtil.updateGene2CsEntries( new Date(), true, true ) )
                .isInstanceOf( IllegalArgumentException.class );
        tableMaintenanceUtil.updateGene2CsEntries( null, true, true );
        ArrayDesign ad = new ArrayDesign();
        ad.setId( 1L );
        tableMaintenanceUtil.updateGene2CsEntries( ad, true );
    }

    @Test
    @WithMockUser(authorities = "GROUP_AGENT")
    public void testWhenUserIsAgent() {
        tableMaintenanceUtil.updateGene2CsEntries();
        assertThat( gene2CsPath ).exists();
    }

    @Test
    public void testWhenUserIsAnonymous() {
        testAuthenticationUtils.runAsAnonymous();
        assertThrows( AccessDeniedException.class, () -> tableMaintenanceUtil.updateGene2CsEntries() );
    }

    @Test
    @WithMockUser(authorities = "GROUP_AGENT")
    public void testUpdateExpressionExperiment2CharacteristicEntries() {
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ExpressionExperiment.class, null, false );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( BioMaterial.class, null, false );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ExperimentalDesign.class, null, false );
        assertThatThrownBy( () -> tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( FactorValue.class, null, false ) )
                .isInstanceOf( IllegalArgumentException.class );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( new Date(), false );
        assertThatThrownBy( () -> tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( new Date(), true ) )
                .isInstanceOf( IllegalArgumentException.class );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ee, null );
        tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( ee, ExperimentalDesign.class );
    }

    @Test
    public void testUpdateEE2CAsUser() {
        testAuthenticationUtils.runAsAnonymous();
        assertThrows( AccessDeniedException.class, () -> tableMaintenanceUtil.updateExpressionExperiment2CharacteristicEntries( null, false ) );
    }

    @Test
    @WithMockUser(authorities = "GROUP_AGENT")
    public void testUpdateExpressionExperiment2ArrayDesignEntries() {
        tableMaintenanceUtil.updateExpressionExperiment2ArrayDesignEntries( null, false );
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        tableMaintenanceUtil.updateExpressionExperiment2ArrayDesignEntries( ee );
    }
}
