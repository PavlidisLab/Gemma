package ubic.gemma.core.loader.expression.singleCell;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.expression.MapBasedDesignElementMapper;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.SingleCellDimension;
import ubic.gemma.model.expression.bioAssayData.SingleCellExpressionDataVector;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.experiment.*;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.mock;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createElementsMappingFromResourceFile;
import static ubic.gemma.core.loader.expression.singleCell.MexTestUtils.createLoaderForResourceDir;

/**
 * Load and persist single-cell data stored in the MEX format.
 */
@ContextConfiguration
public class MexSingleCellDataLoaderPersistenceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class MexSingleCellDataLoaderPersistenceTestContextConfiguration extends BaseDatabaseTestContextConfiguration {
        @Bean
        public SingleCellExpressionExperimentService singleCellExpressionExperimentService() {
            return new SingleCellExpressionExperimentServiceImpl();
        }

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public ExperimentalFactorService experimentalFactorService() {
            return mock();
        }

        @Bean
        public AuditTrailService auditTrailService() {
            return mock();
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock();
        }

        @Bean
        public ExperimentalDesignService experimentalDesignService() {
            return mock();
        }

        @Bean
        public SingleCellSparsityMetrics singleCellSparsityMetrics() {
            return new SingleCellSparsityMetrics();
        }
    }

    @Autowired
    private SingleCellExpressionExperimentService singleCellExpressionExperimentService;

    @Test
    public void test() throws IOException {
        MexSingleCellDataLoader loader = createLoaderForResourceDir( "/data/loader/expression/singleCell/GSE224438" );
        loader.setBioAssayToSampleNameMatcher( ( bms, s ) -> bms.stream().filter( bm -> s.equals( bm.getName() ) ).collect( Collectors.toSet() ) );

        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign platform = new ArrayDesign();
        platform.setPrimaryTaxon( taxon );
        Map<String, CompositeSequence> elementsMapping = createElementsMappingFromResourceFile( "data/loader/expression/singleCell/GSE224438/GSM7022367_1_features.tsv.gz" );
        elementsMapping.values().forEach( cs -> cs.setArrayDesign( platform ) );
        platform.getCompositeSequences().addAll( elementsMapping.values() );
        sessionFactory.getCurrentSession().persist( platform );
        ExpressionExperiment ee = new ExpressionExperiment();

        for ( String sampleName : loader.getSampleNames() ) {
            BioMaterial bm = BioMaterial.Factory.newInstance( sampleName, taxon );
            sessionFactory.getCurrentSession().persist( bm );
            BioAssay ba = BioAssay.Factory.newInstance( sampleName, platform, bm );
            bm.getBioAssaysUsedIn().add( ba );
            ee.getBioAssays().add( ba );
        }

        sessionFactory.getCurrentSession().persist( ee );
        sessionFactory.getCurrentSession().flush();
        SingleCellDimension dimension = loader.getSingleCellDimension( ee.getBioAssays() );
        QuantitationType qt = loader.getQuantitationTypes().iterator().next();
        sessionFactory.getCurrentSession().persist( qt );
        try ( Stream<SingleCellExpressionDataVector> stream = loader.loadVectors( new MapBasedDesignElementMapper( "test", elementsMapping ), dimension, qt ) ) {
            singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, stream.collect( Collectors.toList() ), null );
        }
    }
}
