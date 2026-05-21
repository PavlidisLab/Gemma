package ubic.gemma.core.loader.expression.singleCell;

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import ubic.gemma.core.analysis.singleCell.SingleCellSparsityMetrics;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.loader.util.mapper.MapBasedDesignElementMapper;
import ubic.gemma.core.loader.util.mapper.SimpleBioAssayMapper;
import ubic.gemma.core.util.test.BaseDatabaseTest5;
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
public class MexSingleCellDataLoaderPersistenceTest extends BaseDatabaseTest5 {

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

        // EE DAO now field-injects ArrayDesignDao for batched platform loads (round-2 probe #8).
        @Bean
        public ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDao arrayDesignDao( SessionFactory sessionFactory ) {
            return new ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignDaoImpl( sessionFactory );
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
    @Tag("slow")
    public void test() throws IOException {
        MexSingleCellDataLoader loader = createLoaderForResourceDir( "/data/loader/expression/singleCell/GSE224438" );
        loader.setBioAssayToSampleNameMapper( new SimpleBioAssayMapper() );

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
        loader.setDesignElementToGeneMapper( new MapBasedDesignElementMapper( "test", elementsMapping ) );
        try ( Stream<SingleCellExpressionDataVector> stream = loader.loadVectors( elementsMapping.values(), dimension, qt ) ) {
            singleCellExpressionExperimentService.addSingleCellDataVectors( ee, qt, stream.collect( Collectors.toList() ), null, true, false );
        }
    }
}
