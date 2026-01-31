package ubic.gemma.persistence.service.analysis.expression.diff;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.util.test.BaseIntegrationTest;
import ubic.gemma.core.util.test.TestAuthenticationUtils;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysisResult;
import ubic.gemma.model.analysis.expression.diff.ExpressionAnalysisResultSet;
import ubic.gemma.model.analysis.expression.diff.PvalueDistribution;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneProduct;
import ubic.gemma.model.genome.sequenceAnalysis.AnnotationAssociation;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.sequenceAnalysis.AnnotationAssociationService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

public class DifferentialExpressionResultServiceTest extends BaseIntegrationTest {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private TaxonService taxonService;

    @Autowired
    private GeneService geneService;

    @Autowired
    private BioSequenceService bioSequenceService;

    @Autowired
    private AnnotationAssociationService annotationAssociationService;

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private DifferentialExpressionResultService differentialExpressionResultService;

    @Autowired
    private TestAuthenticationUtils testAuthenticationUtils;

    /**
     * This test mainly exercise the ACL annotation for filtering DEA results.
     */
    @Test
    public void testFindByGeneAndExperimentAnalyzed() {
        Taxon taxon = Taxon.Factory.newInstance( "hooman" );
        taxon = taxonService.create( taxon );

        Gene gene = Gene.Factory.newInstance();
        gene.setTaxon( taxon );
        GeneProduct gp = GeneProduct.Factory.newInstance();
        gp.setGene( gene );
        gene.getProducts().add( gp );
        gene = geneService.create( gene );

        BioSequence bioSequence = BioSequence.Factory.newInstance();
        bioSequence.setTaxon( taxon );
        bioSequence = bioSequenceService.create( bioSequence );

        AnnotationAssociation bs2gp = AnnotationAssociation.Factory.newInstance();
        bs2gp.setGeneProduct( gp );
        bs2gp.setBioSequence( bioSequence );
        bs2gp = annotationAssociationService.create( bs2gp );

        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setPrimaryTaxon( taxon );
        CompositeSequence probe = CompositeSequence.Factory.newInstance( "probe1", ad );
        probe.setBiologicalCharacteristic( bioSequence );
        ad.getCompositeSequences().add( probe );
        ad = arrayDesignService.create( ad );

        ExpressionExperiment ee = ExpressionExperiment.Factory.newInstance();
        ee.setTaxon( taxon );
        ee = expressionExperimentService.create( ee );

        DifferentialExpressionAnalysis dea = DifferentialExpressionAnalysis.Factory.newInstance();
        dea.setExperimentAnalyzed( ee );

        ExpressionAnalysisResultSet resultSet = ExpressionAnalysisResultSet.Factory.newInstance();
        resultSet.setAnalysis( dea );
        resultSet.setPvalueDistribution( PvalueDistribution.Factory.newInstance( new double[0] ) );
        dea.getResultSets().add( resultSet );

        DifferentialExpressionAnalysisResult result = DifferentialExpressionAnalysisResult.Factory.newInstance();
        result.setProbe( probe );
        result.setResultSet( resultSet );
        result.setCorrectedPvalue( 0.0001 );
        resultSet.getResults().add( result );

        dea = differentialExpressionAnalysisService.create( dea );

        assertThat( differentialExpressionResultService.findByGeneAndExperimentAnalyzed( gene, false, true, Collections.singleton( ee ), false, 0.05, -1 ) )
                .hasSize( 1 );

        testAuthenticationUtils.runAsAnonymous();
        assertThat( differentialExpressionResultService.findByGeneAndExperimentAnalyzed( gene, false, true, Collections.singleton( ee ), false, 0.05, -1 ) )
                .isEmpty();
    }
}