package ubic.gemma.core.analysis.singleCell.aggregate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.security.acl.domain.AclObjectIdentity;
import ubic.gemma.core.security.acl.domain.AclService;
import ubic.gemma.core.util.test.BaseIntegrationTest5;
import ubic.gemma.core.util.test.PersistentDummyObjectHelper;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;

import org.springframework.lang.Nullable;
import org.springframework.security.acls.model.Acl;
import org.springframework.security.acls.model.NotFoundException;

import static org.assertj.core.api.Assertions.assertThat;

public class SingleCellExpressionExperimentCreateSubSetsAndAggregateServiceTest extends BaseIntegrationTest5 {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private SingleCellExpressionExperimentCreateSubSetsAndAggregateService splitAndAggregateService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private PersistentDummyObjectHelper testHelper;

    @Autowired
    private AclService aclService;

    private ExpressionExperiment ee;

    @BeforeEach
    public void setUp() {
        ee = testHelper.getTestPersistentSingleCellExpressionExperiment();
    }

    @AfterEach
    public void cleanUp() {
        // FIXME: experiment with single-cell data cannot be deleted due to some constraint violation
        // if ( ee != null ) {
        //     expressionExperimentService.remove( ee );
        // }
    }

    @Test
    public void testRedoAggregate() {
        assertThat( ee.getQuantitationTypes() )
                .hasSize( 1 );
        assertThat( ee.getSingleCellExpressionDataVectors() )
                .hasSize( 100 );

        SingleCellExperimentSubSetsCreationConfig singleCellExperimentSubSetsCreationConfig = SingleCellExperimentSubSetsCreationConfig.builder().build();
        SingleCellAggregationConfig config = SingleCellAggregationConfig.builder().makePreferred( true ).build();
        QuantitationType qt = splitAndAggregateService.createSubSetsAndAggregateByCellType( ee, singleCellExperimentSubSetsCreationConfig, config );

        ee = expressionExperimentService.thawLite( ee );
        assertThat( ee.getQuantitationTypes() ).contains( qt );

        BioAssayDimension dim = expressionExperimentService.getBioAssayDimension( ee, qt, RawExpressionDataVector.class );
        assertThat( dim ).isNotNull();
        QuantitationType newQt = splitAndAggregateService.redoAggregateByCellType( ee, dim, qt, config );
        BioAssayDimension newBad = expressionExperimentService.getBioAssayDimension( ee, newQt, RawExpressionDataVector.class );
        assertThat( newBad ).isEqualTo( dim );
        assertThat( quantitationTypeService.load( qt.getId() ) ).isNull();
        assertThat( newQt.getIsPreferred() ).isTrue();

        ee = expressionExperimentService.thawLite( ee );
        assertThat( ee.getQuantitationTypes() ).contains( newQt );
    }

    /**
     * Aggregation must leave every assay and sample it creates inheriting from the experiment.
     * <p>
     * These do not get an ACL parent the way a cascaded child does. ExpressionExperimentSubSet's
     * bioAssays is a @ManyToMany with no cascade — there is a FIXME on
     * {@code SingleCellExpressionExperimentSubSetServiceImpl#createBioAssayForCellPopulation}
     * saying a subset does not properly own its assays — so each is inserted on its own by
     * {@code bioAssayService.create}. AclEventListener stashes transient children only for a
     * cascading association, and nothing sets securityOwner here, which leaves
     * discoverParentViaBackRef walking sampleUsed and sourceBioMaterial. That only resolves if
     * those referenced rows already have correct ACLs.
     * <p>
     * On production they did not: an ACL repair on 2026-08-30 found 631,709 parentless BioAssay
     * identities and as many BioMaterial ones, overwhelmingly aggregated single-cell output, plus
     * 37,524 assays with no identity at all. This pins the behaviour so the population cannot
     * silently rebuild.
     * <p>
     * Three paths can supply the parent — {@code locateParentAcl}, the parent stash in
     * {@code stashChildren}, and {@code discoverParentViaBackRef} — and they are REDUNDANT here:
     * disabling any one leaves this test green, and it only goes red with all three disabled
     * ("aggregated assay N has no parent ACL identity"). So it asserts the outcome rather than any
     * one mechanism, which is what makes it survive a change to any of them.
     */
    @Test
    public void testAggregatedAssaysInheritFromTheirExperiment() {
        SingleCellExperimentSubSetsCreationConfig subSetsConfig = SingleCellExperimentSubSetsCreationConfig.builder().build();
        SingleCellAggregationConfig config = SingleCellAggregationConfig.builder().makePreferred( true ).build();
        QuantitationType qt = splitAndAggregateService.createSubSetsAndAggregateByCellType( ee, subSetsConfig, config );

        BioAssayDimension dim = expressionExperimentService.getBioAssayDimension( ee, qt, RawExpressionDataVector.class );
        assertThat( dim ).isNotNull();
        assertThat( dim.getBioAssays() ).isNotEmpty();

        for ( BioAssay ba : dim.getBioAssays() ) {
            assertThat( parentOf( BioAssay.class, ba.getId() ) )
                    .as( "aggregated assay %s has no parent ACL identity", ba.getId() )
                    .isNotNull();
            BioMaterial bm = ba.getSampleUsed();
            assertThat( bm ).isNotNull();
            assertThat( parentOf( BioMaterial.class, bm.getId() ) )
                    .as( "aggregated sample %s has no parent ACL identity", bm.getId() )
                    .isNotNull();
        }
    }

    /**
     * @return the parent of the entity's ACL identity, or null when it has none — including when
     * the identity itself is missing, which is the other way this has failed.
     */
    @Nullable
    private Acl parentOf( Class<? extends ubic.gemma.core.security.model.Securable> clazz, Long id ) {
        try {
            return aclService.readAclById( new AclObjectIdentity( clazz, id ) ).getParentAcl();
        } catch ( NotFoundException e ) {
            return null;
        }
    }
}