package ubic.gemma.persistence.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import ubic.gemma.persistence.service.common.description.BibliographicReferenceService;
import ubic.gemma.persistence.service.genome.gene.GeneService;
import ubic.gemma.persistence.service.genome.gene.GeneSetService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.common.description.BibliographicReference;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.blacklist.BlacklistedEntity;
import ubic.gemma.model.blacklist.BlacklistedValueObject;
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
import ubic.gemma.model.genome.sequenceAnalysis.BioSequenceValueObject;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.blacklist.BlacklistedEntityService;
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
            GeneSetService geneSetService,
            BlacklistedEntityService blacklistedEntityService ) {
        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter( new ServiceBasedValueObjectConverter<>( blacklistedEntityService, BlacklistedEntity.class, BlacklistedValueObject.class ) );
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
