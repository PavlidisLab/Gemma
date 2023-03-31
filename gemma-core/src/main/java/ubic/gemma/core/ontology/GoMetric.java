package ubic.gemma.core.ontology;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface GoMetric {
    Double computeMatrixSimilarity( Gene gene1, Gene gene2, DoubleMatrix<Long, String> gene2TermMatrix,
            Metric metric );

    Double computeMaxSimilarity( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap,
            Metric metric );

    Double computeMergedOverlap( List<Gene> sameGenes1, List<Gene> sameGenes2,
            Map<Long, Collection<String>> geneGoMap );

    Double computeSimilarity( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap, Metric metric );

    Double computeSimpleOverlap( Gene g, Gene coexpG, Map<Long, Collection<String>> geneGoMap );

    DoubleMatrix<Long, String> createVectorMatrix( Map<Long, Collection<String>> gene2go, boolean weight );

    Integer getChildrenOccurrence( Map<String, Integer> termCountMap, String term );

    public enum Metric {
        jiang, lin, resnik, simple, percent, kappa, cosine
    }
}
