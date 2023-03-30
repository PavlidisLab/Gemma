/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.model.expression.experiment;

import gemma.gsec.SecurityService;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.preprocess.svd.SVDService;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.core.search.SearchService;
import ubic.gemma.core.util.test.BaseSpringContextTest;
import ubic.gemma.model.common.auditAndSecurity.User;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.common.quantitationtype.QuantitationTypeImpl;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.persistence.service.analysis.expression.coexpression.CoexpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.pca.PrincipalComponentAnalysisService;
import ubic.gemma.persistence.service.analysis.expression.sampleCoexpression.SampleCoexpressionAnalysisService;
import ubic.gemma.persistence.service.common.auditAndSecurity.AuditEventService;
import ubic.gemma.persistence.service.common.quantitationtype.QuantitationTypeService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.*;
import ubic.gemma.persistence.util.TestComponent;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

/**
 * @author daq2101
 * @author paul
 * @author poirigui
 */
@ContextConfiguration
public class ExpressionExperimentServiceImplTest extends AbstractJUnit4SpringContextTests {

    @Configuration
    @TestComponent
    static class ExpressionExperimentServiceImplTestContextConfiguration {

        @Bean
        public ExpressionExperimentDao expressionExperimentDao() {
            return mock( ExpressionExperimentDao.class );
        }

        @Bean
        public ExpressionExperimentService expressionExperimentService( ExpressionExperimentDao expressionExperimentDao ) {
            return new ExpressionExperimentServiceImpl( expressionExperimentDao );
        }

        @Bean
        public AuditEventService auditEventService() {
            return mock( AuditEventService.class );
        }

        @Bean
        public BioAssayDimensionService bioAssayDimensionService() {
            return mock( BioAssayDimensionService.class );
        }

        @Bean
        public DifferentialExpressionAnalysisService differentialExpressionAnalysisService() {
            return mock( DifferentialExpressionAnalysisService.class );
        }

        @Bean
        public ExpressionExperimentSetService expressionExperimentSetService() {
            return mock( ExpressionExperimentSetService.class );
        }

        @Bean
        public ExpressionExperimentSubSetService expressionExperimentSubSetService() {
            return mock( ExpressionExperimentSubSetService.class );
        }

        @Bean
        public ExperimentalFactorService experimentalFactorService() {
            return mock( ExperimentalFactorService.class );
        }

        @Bean
        public FactorValueService factorValueService() {
            return mock( FactorValueService.class );
        }

        @Bean
        public OntologyService ontologyService() {
            return mock( OntologyService.class );
        }

        @Bean
        public PrincipalComponentAnalysisService principalComponentAnalysisService() {
            return mock( PrincipalComponentAnalysisService.class );
        }

        @Bean
        public QuantitationTypeService quantitationTypeService() {
            return mock( QuantitationTypeService.class );
        }

        @Bean
        public SearchService searchService() {
            return mock( SearchService.class );
        }

        @Bean
        public SecurityService securityService() {
            return mock( SecurityService.class );
        }

        @Bean
        public SVDService svdService() {
            return mock( SVDService.class );
        }

        @Bean
        public CoexpressionAnalysisService coexpressionAnalysisService() {
            return mock( CoexpressionAnalysisService.class );
        }

        @Bean
        public SampleCoexpressionAnalysisService sampleCoexpressionAnalysisService() {
            return mock( SampleCoexpressionAnalysisService.class );
        }

        @Bean
        public BlacklistedEntityService blacklistedEntityService() {
            return mock( BlacklistedEntityService.class );
        }

        @Bean
        public AccessDecisionManager accessDecisionManager() {
            return mock( AccessDecisionManager.class );
        }
    }

    @Autowired
    private ExpressionExperimentService svc;

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @Autowired
    private QuantitationTypeService quantitationTypeService;

    @Autowired
    private ExpressionExperimentDao eeDao;

    @Before
    public void setupMocks() {
        when( eeDao.getElementClass() ).thenAnswer( a -> ExpressionExperiment.class );
    }

    @After
    public void resetMocks() {
        reset( bioAssayDimensionService, quantitationTypeService, eeDao );
    }

    @Test
    public void testExpressionExperimentFindAll() {

        svc = new ExpressionExperimentServiceImpl( eeDao );

        User nobody = User.Factory.newInstance();

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setDescription( "From test" );
        ee.setName( "Test experiment" );
        ee.setOwner( nobody );

        ArrayDesign ad1 = ArrayDesign.Factory.newInstance();
        ad1.setShortName( "foo" );
        ArrayDesign ad2 = ArrayDesign.Factory.newInstance();
        ad2.setShortName( "bar" );

        for ( long i = 0; i < 10; i++ ) {
            BioAssay ba = BioAssay.Factory.newInstance();
            ba.setId( i + 1 );
            if ( i % 2 == 0 ) {
                ba.setArrayDesignUsed( ad1 );
            } else {
                ba.setArrayDesignUsed( ad2 );
            }
            ee.getBioAssays().add( ba );
        }

        Collection<ExpressionExperiment> c = new HashSet<>();
        ExpressionExperiment numberTwelve = ExpressionExperiment.Factory.newInstance();
        numberTwelve.setId( 12L );

        c.add( numberTwelve );
        c.add( ExpressionExperiment.Factory.newInstance() );
        c.add( ExpressionExperiment.Factory.newInstance() );

        Collection<ExpressionExperiment> cJustTwelve = new HashSet<>();
        cJustTwelve.add( numberTwelve );

        when( eeDao.loadAll() ).thenReturn( c );
        assertThat( svc.loadAll() ).isEqualTo( c );
        verify( eeDao ).loadAll();
    }

    @Test
    public void testReplaceRawVectors() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( eeDao.load( 1L ) ).thenReturn( ee );
        QuantitationType qt = new QuantitationTypeImpl();
        qt.setIsPreferred( true );
        BioAssayDimension bad = new BioAssayDimension();
        bad.setBioAssays( Collections.singletonList( new BioAssay() ) );
        ArrayDesign ad = new ArrayDesign();
        when( bioAssayDimensionService.findOrCreate( bad ) ).thenReturn( bad );
        when( quantitationTypeService.create( qt ) ).thenReturn( qt );
        Set<RawExpressionDataVector> vectors = createRawVectors( ee, qt, bad, ad );
        svc.replaceRawVectors( ee, vectors );
        verify( eeDao ).update( ee );
        assertThat( ee.getQuantitationTypes() )
                .containsExactly( qt );
        assertThat( ee.getRawExpressionDataVectors() )
                .hasSize( 10 );
    }

    @Test
    public void testReplaceRawVectorsWithoutPreferredQt() {
        ExpressionExperiment ee = new ExpressionExperiment();
        ee.setId( 1L );
        when( eeDao.load( 1L ) ).thenReturn( ee );
        QuantitationType qt = new QuantitationTypeImpl();
        BioAssayDimension bad = new BioAssayDimension();
        ArrayDesign ad = new ArrayDesign();
        Set<RawExpressionDataVector> vectors = createRawVectors( ee, qt, bad, ad );
        assertThatThrownBy( () -> svc.replaceRawVectors( ee, vectors ) )
                .isInstanceOf( IllegalArgumentException.class );
        verifyNoInteractions( eeDao );
    }

    private Set<RawExpressionDataVector> createRawVectors( ExpressionExperiment ee, QuantitationType qt, BioAssayDimension bad, ArrayDesign ad ) {
        Set<RawExpressionDataVector> vectors = new HashSet<>();
        for ( int i = 0; i < 10; i++ ) {
            RawExpressionDataVector v = new RawExpressionDataVector();
            v.setExpressionExperiment( ee );
            v.setQuantitationType( qt );
            v.setBioAssayDimension( bad );
            CompositeSequence cs = new CompositeSequence();
            cs.setName( "cs" + i );
            cs.setArrayDesign( ad );
            v.setDesignElement( cs );
            vectors.add( v );
        }
        return vectors;
    }
}