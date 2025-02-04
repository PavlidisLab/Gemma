package ubic.gemma.persistence.service.expression.bioAssayData;

import org.hibernate.SessionFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContextTestExecutionListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import ubic.gemma.core.context.TestComponent;
import ubic.gemma.core.util.test.BaseDatabaseTest;
import ubic.gemma.model.common.quantitationtype.*;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.DoubleVectorValueObject;
import ubic.gemma.model.expression.bioAssayData.ProcessedExpressionDataVector;
import ubic.gemma.model.expression.bioAssayData.SlicedDoubleVectorValueObject;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSubSet;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDao;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentDaoImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static ubic.gemma.persistence.service.expression.bioAssayData.RandomBulkDataUtils.randomBulkVectors;

@ContextConfiguration
@TestExecutionListeners({ WithSecurityContextTestExecutionListener.class })
public class CachedProcessedExpressionDataVectorServiceTest extends BaseDatabaseTest {

    @Configuration
    @TestComponent
    static class CC extends BaseDatabaseTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao( SessionFactory sessionFactory ) {
            return new ExpressionExperimentDaoImpl( sessionFactory );
        }

        @Bean
        public ProcessedExpressionDataVectorDao processedExpressionDataVectorDao( SessionFactory sessionFactory ) {
            return new ProcessedExpressionDataVectorDaoImpl( sessionFactory );
        }

        @Bean
        public ProcessedDataVectorCache processedDataVectorCache() {
            return new ProcessedDataVectorCacheImpl( new ConcurrentMapCacheManager() );
        }

        @Bean
        public ProcessedDataVectorByGeneCache processedDataVectorByGeneCache() {
            return new ProcessedDataVectorByGeneCacheImpl( new ConcurrentMapCacheManager() );
        }

        @Bean
        public CachedProcessedExpressionDataVectorService cachedProcessedExpressionDataVectorService( ProcessedExpressionDataVectorDao dao, SessionFactory sessionFactory ) {
            return new CachedProcessedExpressionDataVectorServiceImpl( dao, sessionFactory );
        }

        @Bean
        public BioAssayDimensionService bioAssayDimensionService( BioAssayDimensionDao bioAssayDimensionDao ) {
            return new BioAssayDimensionServiceImpl( bioAssayDimensionDao );
        }

        @Bean
        public BioAssayDimensionDao bioAssayDimensionDao( SessionFactory sessionFactory ) {
            return new BioAssayDimensionDaoImpl( sessionFactory );
        }
    }

    @Autowired
    private CachedProcessedExpressionDataVectorService cachedProcessedExpressionDataVectorService;

    @Test
    @WithMockUser
    public void testGetVectors() {
        ExpressionExperiment ee = createExperiment();

        Collection<DoubleVectorValueObject> vecs = cachedProcessedExpressionDataVectorService.getProcessedDataArrays( ee );
        assertThat( vecs )
                .hasSize( 10 )
                .allSatisfy( vec -> {
                    assertThat( vec.getBioAssayDimension().getId() ).isNotNull();
                    assertThat( vec.getBioAssayDimension().getBioAssays() ).hasSize( 8 );
                    assertThat( vec.getData() ).hasSize( 8 );
                } );

        // these results are cached, so they are the exact same as the previous call, but from a shuffled collection
        assertThat( cachedProcessedExpressionDataVectorService.getRandomProcessedDataArrays( ee, 5 ) )
                .hasSize( 5 )
                .allSatisfy( vec -> {
                    assertThat( vecs ).satisfiesOnlyOnce( v -> assertThat( v ).isSameAs( vec ) );
                } );
    }

    @Test
    @WithMockUser
    public void testGetVectorsForSubset() {
        List<ExpressionExperimentSubSet> subsets = new ArrayList<>();
        ExpressionExperiment ee = createExperimentWithSubSets( subsets );

        assertThat( cachedProcessedExpressionDataVectorService.getProcessedDataArrays( subsets.get( 0 ) ) )
                .hasSize( 10 )
                .allSatisfy( vec -> {
                    assertThat( vec ).isInstanceOf( SlicedDoubleVectorValueObject.class );
                    assertThat( vec.getBioAssayDimension().getId() ).isNull();
                    assertThat( vec.getBioAssayDimension().getBioAssays() ).hasSize( 4 );
                    assertThat( vec.getData() ).hasSize( 4 );
                } );

        assertThat( cachedProcessedExpressionDataVectorService.getProcessedDataArrays( subsets.get( 1 ) ) )
                .hasSize( 10 )
                .allSatisfy( vec -> {
                    assertThat( vec ).isInstanceOf( SlicedDoubleVectorValueObject.class );
                    assertThat( vec.getBioAssayDimension().getId() ).isNull();
                    assertThat( vec.getBioAssayDimension().getBioAssays() ).hasSize( 4 );
                    assertThat( vec.getData() ).hasSize( 4 );
                } );
    }

    /**
     * This test exercise the ability of the service to retrieve vector for a subset of sub-bioassays.
     */
    @Test
    @WithMockUser
    public void testGetVectorsForSubBioAssays() {
        List<ExpressionExperimentSubSet> subsets = new ArrayList<>();
        ExpressionExperiment ee = createExperimentWithSubAssaysSubSets( subsets );

        assertThat( cachedProcessedExpressionDataVectorService.getProcessedDataArrays( subsets.get( 0 ) ) )
                .hasSize( 10 )
                .allSatisfy( vec -> {
                    assertThat( vec ).isInstanceOf( SlicedDoubleVectorValueObject.class );
                    assertThat( vec.getBioAssayDimension().getId() ).isNull();
                    assertThat( vec.getBioAssayDimension().getBioAssays() ).hasSize( 4 );
                    assertThat( vec.getData() ).hasSize( 4 );
                } );

        assertThat( cachedProcessedExpressionDataVectorService.getProcessedDataArrays( subsets.get( 1 ) ) )
                .hasSize( 10 )
                .allSatisfy( vec -> {
                    assertThat( vec ).isInstanceOf( SlicedDoubleVectorValueObject.class );
                    assertThat( vec.getBioAssayDimension().getId() ).isNull();
                    assertThat( vec.getBioAssayDimension().getBioAssays() ).hasSize( 4 );
                    assertThat( vec.getData() ).hasSize( 4 );
                } );

        assertThat( cachedProcessedExpressionDataVectorService.getRandomProcessedDataArrays( subsets.get( 0 ), 10 ) )
                .hasSize( 10 );
    }

    private ExpressionExperiment createExperiment() {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < 10; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );
        ExpressionExperiment expressionExperiment = new ExpressionExperiment();
        BioAssay[] bas = new BioAssay[8];
        for ( int i = 0; i < 8; i++ ) {
            bas[i] = BioAssay.Factory.newInstance( "ba" + i, ad, BioMaterial.Factory.newInstance( "bm" + i, taxon ) );
        }
        expressionExperiment.getBioAssays().addAll( Arrays.asList( bas ) );
        expressionExperiment.getBioAssays().forEach( ba -> sessionFactory.getCurrentSession().persist( ba.getSampleUsed() ) );
        sessionFactory.getCurrentSession().persist( expressionExperiment );

        // create vectors for the subsets
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<ProcessedExpressionDataVector> vectors = randomBulkVectors( expressionExperiment, ad, qt, ProcessedExpressionDataVector.class );
        expressionExperiment.getQuantitationTypes().add( qt );
        expressionExperiment.getProcessedExpressionDataVectors()
                .addAll( vectors );
        sessionFactory.getCurrentSession().persist( vectors.iterator().next().getBioAssayDimension() );
        // vectors will be created in cascade
        sessionFactory.getCurrentSession().update( expressionExperiment );

        return expressionExperiment;
    }

    private ExpressionExperiment createExperimentWithSubSets( Collection<ExpressionExperimentSubSet> subsets ) {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < 10; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );
        ExpressionExperiment expressionExperiment = new ExpressionExperiment();
        BioAssay[] bas = new BioAssay[8];
        for ( int i = 0; i < 8; i++ ) {
            bas[i] = BioAssay.Factory.newInstance( "ba" + i, ad, BioMaterial.Factory.newInstance( "bm" + i, taxon ) );
        }
        expressionExperiment.getBioAssays().addAll( Arrays.asList( bas ) );
        expressionExperiment.getBioAssays().forEach( ba -> sessionFactory.getCurrentSession().persist( ba.getSampleUsed() ) );
        sessionFactory.getCurrentSession().persist( expressionExperiment );

        // we're going to create two subsets of BAs with 2 sub-assay per assay
        ExpressionExperimentSubSet[] bioAssaySets = new ExpressionExperimentSubSet[2];
        bioAssaySets[0] = ExpressionExperimentSubSet.Factory.newInstance( "Subset A", expressionExperiment );
        bioAssaySets[1] = ExpressionExperimentSubSet.Factory.newInstance( "Subset B", expressionExperiment );
        for ( int i = 0; i < 8; i++ ) {
            if ( i < 4 ) {
                bioAssaySets[0].getBioAssays().add( bas[i] );
            } else {
                bioAssaySets[1].getBioAssays().add( bas[i] );
            }
        }

        // persist the subsets
        sessionFactory.getCurrentSession().persist( bioAssaySets[0] );
        sessionFactory.getCurrentSession().persist( bioAssaySets[1] );

        // create vectors for the subsets
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<ProcessedExpressionDataVector> vectors = randomBulkVectors( expressionExperiment, Arrays.asList( bioAssaySets ), ad, qt, ProcessedExpressionDataVector.class );
        expressionExperiment.getQuantitationTypes().add( qt );
        expressionExperiment.getProcessedExpressionDataVectors()
                .addAll( vectors );
        sessionFactory.getCurrentSession().persist( vectors.iterator().next().getBioAssayDimension() );
        // vectors will be created in cascade
        sessionFactory.getCurrentSession().update( expressionExperiment );

        subsets.addAll( Arrays.asList( bioAssaySets ) );
        return expressionExperiment;
    }

    private ExpressionExperiment createExperimentWithSubAssaysSubSets( Collection<ExpressionExperimentSubSet> subsets ) {
        Taxon taxon = new Taxon();
        sessionFactory.getCurrentSession().persist( taxon );
        ArrayDesign ad = new ArrayDesign();
        for ( int i = 0; i < 10; i++ ) {
            ad.getCompositeSequences().add( CompositeSequence.Factory.newInstance( "cs" + i, ad ) );
        }
        ad.setPrimaryTaxon( taxon );
        sessionFactory.getCurrentSession().persist( ad );
        ExpressionExperiment expressionExperiment = new ExpressionExperiment();
        BioAssay[] sourceBas = new BioAssay[4];
        for ( int i = 0; i < 4; i++ ) {
            sourceBas[i] = BioAssay.Factory.newInstance( "ba" + i, ad, BioMaterial.Factory.newInstance( "bm" + i, taxon ) );
        }
        expressionExperiment.getBioAssays().addAll( Arrays.asList( sourceBas ) );
        expressionExperiment.getBioAssays().forEach( ba -> sessionFactory.getCurrentSession().persist( ba.getSampleUsed() ) );
        sessionFactory.getCurrentSession().persist( expressionExperiment );

        // we're going to create two subsets of BAs with 2 sub-assay per assay
        BioAssay[] bas = new BioAssay[8];
        ExpressionExperimentSubSet[] bioAssaySets = new ExpressionExperimentSubSet[2];
        bioAssaySets[0] = ExpressionExperimentSubSet.Factory.newInstance( "Subset A", expressionExperiment );
        bioAssaySets[1] = ExpressionExperimentSubSet.Factory.newInstance( "Subset B", expressionExperiment );
        for ( int i = 0; i < 8; i++ ) {
            bas[i] = BioAssay.Factory.newInstance( String.format( "ba%d%d", i % 4, i ), ad, BioMaterial.Factory.newInstance( String.format( "bm%d%d", i % 4, i ), taxon ) );
            bas[i].getSampleUsed().setSourceBioMaterial( sourceBas[i % 4].getSampleUsed() );
            if ( i < 4 ) {
                bioAssaySets[0].getBioAssays().add( bas[i] );
            } else {
                bioAssaySets[1].getBioAssays().add( bas[i] );
            }
        }

        // persist the subsets
        bioAssaySets[0].getBioAssays().forEach( ba -> sessionFactory.getCurrentSession().persist( ba.getSampleUsed() ) );
        bioAssaySets[0].getBioAssays().forEach( ba -> sessionFactory.getCurrentSession().persist( ba ) );
        sessionFactory.getCurrentSession().persist( bioAssaySets[0] );
        bioAssaySets[1].getBioAssays().forEach( ba -> sessionFactory.getCurrentSession().persist( ba.getSampleUsed() ) );
        bioAssaySets[1].getBioAssays().forEach( ba -> sessionFactory.getCurrentSession().persist( ba ) );
        sessionFactory.getCurrentSession().persist( bioAssaySets[1] );

        // create vectors for the subsets
        QuantitationType qt = QuantitationType.Factory.newInstance();
        qt.setGeneralType( GeneralType.QUANTITATIVE );
        qt.setType( StandardQuantitationType.AMOUNT );
        qt.setScale( ScaleType.LOG2 );
        qt.setRepresentation( PrimitiveType.DOUBLE );
        Collection<ProcessedExpressionDataVector> vectors = randomBulkVectors( expressionExperiment, Arrays.asList( bioAssaySets ), ad, qt, ProcessedExpressionDataVector.class );
        expressionExperiment.getQuantitationTypes().add( qt );
        expressionExperiment.getProcessedExpressionDataVectors()
                .addAll( vectors );
        sessionFactory.getCurrentSession().persist( vectors.iterator().next().getBioAssayDimension() );
        // vectors will be created in cascade
        sessionFactory.getCurrentSession().update( expressionExperiment );

        subsets.addAll( Arrays.asList( bioAssaySets ) );
        return expressionExperiment;
    }
}