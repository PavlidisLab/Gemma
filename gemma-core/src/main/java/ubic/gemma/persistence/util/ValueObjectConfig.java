package ubic.gemma.persistence.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import ubic.gemma.core.annotation.reference.BibliographicReferenceService;
import ubic.gemma.core.genome.gene.service.GeneService;
import ubic.gemma.core.genome.gene.service.GeneSetService;
import ubic.gemma.model.IdentifiableValueObject;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignValueObject;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.designElement.CompositeSequenceValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentSetValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperimentValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.biosequence.BioSequence;
import ubic.gemma.model.genome.gene.GeneSet;
import ubic.gemma.model.genome.gene.GeneSetValueObject;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.PhenotypeValueObject;
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.association.phenotype.service.PhenotypeAssociationService;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentSetService;
import ubic.gemma.persistence.service.genome.biosequence.BioSequenceService;

@Configuration
public class ValueObjectConfig {

    @Bean
    public ConversionService valueObjectConversionService(
            ArrayDesignService arrayDesignService,
            BibliographicReferenceService bibliographicReferenceService,
            BioSequenceService bioSequenceService,
            CompositeSequenceService compositeSequenceService,
            ExpressionExperimentService expressionExperimentService,
            ExpressionExperimentSetService experimentSetService,
            GeneService geneService,
            GeneSetService geneSetService ) {
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter( new GenericValueObjectConverter<>( o -> o, CharacteristicValueObject.class, CharacteristicValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( arrayDesignService, ArrayDesign.class, ArrayDesignValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( bibliographicReferenceService, BibliographicReference.class, BibliographicReferenceValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( bioSequenceService, BioSequence.class, BioSequenceValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( compositeSequenceService, CompositeSequence.class, CompositeSequenceValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( expressionExperimentService, ExpressionExperiment.class, ExpressionExperimentValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( experimentSetService, ExpressionExperimentSet.class, ExpressionExperimentSetValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( geneService, Gene.class, GeneValueObject.class ) );
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( geneSetService, GeneSet.class, GeneSetValueObject.class ) );
        return conversionService;
    }
}
