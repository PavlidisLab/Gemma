/*
 * The Gemma project
 *
 * Copyright (c) 2007 University of British Columbia
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

package ubic.gemma.core.ontology;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.SparseDoubleMatrix;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.ontology.providers.GeneOntologyService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.association.Gene2GOAssociationService;

import java.util.*;

import static ubic.gemma.core.ontology.providers.GeneOntologyUtils.asRegularGoId;

/**
 * @author meeta
 */
@SuppressWarnings("unused") // Possible external use
@Component
public class GoMetricImpl implements GoMetric {

    private static final String BASE_GO_URI = "http://purl.org/obo/owl/GO#";
    private static final Log log = LogFactory.getLog( GoMetricImpl.class.getName() );
    private static final String process = "http://purl.org/obo/owl/GO#GO_0008150";
    private static final String function = "http://purl.org/obo/owl/GO#GO_0003674";
    private static final String component = "http://purl.org/obo/owl/GO#GO_0005575";

    private boolean partOf = true;

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;
    @Autowired
    private GeneOntologyService geneOntologyService;

    @Override
    public Double computeMatrixSimilarity( Gene gene1, Gene gene2, DoubleMatrix<Long, String> gene2TermMatrix,
            Metric metric ) {

        if ( !geneOntologyService.isOntologyLoaded() )
            GoMetricImpl.log.error( "Method called before geneOntologyService is ready!!!" );
        Double score = null;

        double[] g1 = gene2TermMatrix.getRowByName( gene1.getId() );
        double[] g2 = gene2TermMatrix.getRowByName( gene2.getId() );

        if ( metric.equals( GoMetricImpl.Metric.cosine ) ) {
            score = this.computeCosineSimilarity( g1, g2 );
        }

        if ( metric.equals( GoMetricImpl.Metric.kappa ) ) {
            score = this.computeKappaSimilarity( gene2TermMatrix, g1, g2 );
        }

        return score;
    }

    /**
     * @param  metric     metric
     * @param  GOProbMap  go prob map
     * @param  queryGene  query gene
     * @param  targetGene target gene
     * @return the MAX overlap score between two genes
     */
    @Override
    public Double computeMaxSimilarity( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap,
            Metric metric ) {

        Collection<OntologyTerm> masterGO = this.getOntologyTerms( queryGene );
        if ( ( masterGO == null ) || masterGO.isEmpty() )
            return 0.0;

        Collection<OntologyTerm> coExpGO = this.getOntologyTerms( targetGene );
        if ( ( coExpGO == null ) || coExpGO.isEmpty() )
            return 0.0;

        double checkScore = 0.0;

        for ( OntologyTerm ontoM : masterGO ) {
            if ( this.isRoot( ontoM ) )
                continue;
            if ( !GOProbMap.containsKey( ontoM.getUri() ) )
                continue;
            double probM = GOProbMap.get( ontoM.getUri() );

            for ( OntologyTerm ontoC : coExpGO ) {
                if ( this.isRoot( ontoC ) )
                    continue;
                if ( !GOProbMap.containsKey( ontoC.getUri() ) )
                    continue;
                Double probC = GOProbMap.get( ontoC.getUri() );
                Double pMin;
                Double score;

                if ( StringUtils.equalsIgnoreCase( ontoM.getUri(), ontoC.getUri() ) )
                    pMin = GOProbMap.get( ontoM.getUri() );
                else
                    pMin = this.checkParents( ontoM, ontoC, GOProbMap );

                if ( pMin < 1 ) {
                    score = this.getMetric( metric, pMin, probM, probC );
                    if ( score > checkScore )
                        checkScore = score;
                }

            }
        }
        GoMetricImpl.log.info( "score for " + queryGene + " and " + targetGene + " is " + checkScore );
        return checkScore;
    }

    /**
     * Tailored to handle computing overlap between two gene lists which may contain duplicate genes of the same name
     * but different IDs. If gene lists do not contain duplicates (size = 1) the result will be the same as that of
     * computing simple overlap.
     *
     * @param  geneGoMap  gene go map
     * @param  sameGenes1 same genes 1
     * @param  sameGenes2 same genes 2
     * @return number of overlapping terms between merged sets of GO terms for duplicate gene lists
     */
    @Override
    public Double computeMergedOverlap( List<Gene> sameGenes1, List<Gene> sameGenes2,
            Map<Long, Collection<String>> geneGoMap ) {

        HashSet<String> mergedGoTerms1 = new HashSet<>();
        HashSet<String> mergedGoTerms2 = new HashSet<>();
        for ( Gene gene1 : sameGenes1 ) {
            if ( geneGoMap.containsKey( gene1.getId() ) ) {
                mergedGoTerms1.addAll( geneGoMap.get( gene1.getId() ) );
            }
        }
        for ( Gene gene2 : sameGenes2 ) {
            if ( geneGoMap.containsKey( gene2.getId() ) ) {
                mergedGoTerms2.addAll( geneGoMap.get( gene2.getId() ) );
            }
        }

        if ( mergedGoTerms1.isEmpty() || mergedGoTerms2.isEmpty() )
            return 0.0;

        return this.computeScore( mergedGoTerms1, mergedGoTerms2 );
    }

    /**
     * @param  metric     metric
     * @param  GOProbMap  go prob map
     * @param  queryGene  query gene
     * @param  targetGene target gene
     * @return the overlap score between two genes
     */
    @Override
    public Double computeSimilarity( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap, Metric metric ) {

        if ( metric.equals( GoMetricImpl.Metric.simple ) ) {
            return this.computeSimpleOverlap( queryGene, targetGene, partOf );
        }

        if ( metric.equals( GoMetricImpl.Metric.percent ) ) {
            return this.computePercentOverlap( queryGene, targetGene, partOf );
        }

        Collection<OntologyTerm> masterGO = this.getOntologyTerms( queryGene );
        if ( ( masterGO == null ) || masterGO.isEmpty() )
            return 0.0;

        Collection<OntologyTerm> coExpGO = this.getOntologyTerms( targetGene );
        if ( ( coExpGO == null ) || coExpGO.isEmpty() )
            return 0.0;

        double total = 0;
        int count = 0;

        for ( OntologyTerm ontoM : masterGO ) {
            if ( this.isRoot( ontoM ) )
                continue;
            if ( !GOProbMap.containsKey( ontoM.getUri() ) )
                continue;
            double probM = GOProbMap.get( ontoM.getUri() );

            for ( OntologyTerm ontoC : coExpGO ) {
                if ( this.isRoot( ontoC ) )
                    continue;
                if ( !GOProbMap.containsKey( ontoC.getUri() ) )
                    continue;
                Double probC = GOProbMap.get( ontoC.getUri() );
                Double pMin;
                Double score;

                if ( StringUtils.equalsIgnoreCase( ontoM.getUri(), ontoC.getUri() ) )
                    pMin = GOProbMap.get( ontoM.getUri() );
                else
                    pMin = this.checkParents( ontoM, ontoC, GOProbMap );

                if ( pMin < 1 ) {
                    score = this.getMetric( metric, pMin, probM, probC );
                    total += score;
                    count++;
                }
            }
        }
        if ( total > 0 ) {
            double avgScore = total / count;
            GoMetricImpl.log.info( "score for " + queryGene + " and " + targetGene + " is " + avgScore );
            return avgScore;
        }
        GoMetricImpl.log.info( "NO score for " + queryGene + " and " + targetGene );
        return 0.0;

    }

    /**
     * @param  g         g
     * @param  coexpG    coexp g
     * @param  geneGoMap gene go map
     * @return number of overlapping terms
     */

    @Override
    public Double computeSimpleOverlap( Gene g, Gene coexpG, Map<Long, Collection<String>> geneGoMap ) {

        Collection<String> masterGO = geneGoMap.get( g.getId() );
        Collection<String> coExpGO = geneGoMap.get( coexpG.getId() );

        double score = 0.0;

        if ( ( coExpGO == null ) || coExpGO.isEmpty() )
            return 0.0;

        if ( ( masterGO == null ) || masterGO.isEmpty() )
            return 0.0;

        return this.computeScore( masterGO, coExpGO );
    }

    /**
     * @param  gene2go Map
     * @param  weight  weight
     * @return Sparse matrix of genes x GOTerms
     */
    @Override
    public DoubleMatrix<Long, String> createVectorMatrix( Map<Long, Collection<String>> gene2go, boolean weight ) {

        Map<String, Double> GOTermFrequency = new HashMap<>();
        List<String> goTerms = new ArrayList<>( geneOntologyService.getAllURIs() );

        // Remove 'BiologicalProcess' etc.
        goTerms.remove( GoMetricImpl.BASE_GO_URI + "GO_0008150" );
        goTerms.remove( GoMetricImpl.BASE_GO_URI + "GO_0003674" );
        goTerms.remove( GoMetricImpl.BASE_GO_URI + "GO_0005575" );

        List<Long> geneSet = new ArrayList<>( gene2go.keySet() );
        DoubleMatrix<Long, String> gene2term = new SparseDoubleMatrix<>( geneSet.size(), goTerms.size() );

        if ( weight ) {
            GOTermFrequency = this.createWeightMap( this.getTermOccurrence( gene2go ), gene2go.keySet().size() );
        }

        gene2term.setColumnNames( goTerms );
        gene2term.setRowNames( geneSet );

        for ( Long id : gene2term.getRowNames() ) {

            Collection<String> terms = gene2go.get( id );
            for ( String goId : gene2term.getColNames() ) {

                if ( terms.contains( goId ) ) {
                    if ( weight ) {
                        gene2term.setByKeys( id, goId, GOTermFrequency.get( goId ) );
                    } else {
                        gene2term.setByKeys( id, goId, ( double ) 1 );
                    }
                }
            }
        }
        return gene2term;
    }

    /**
     * @param  termCountMap each GO term uri mapped to the number of its occurrence in the corpus
     * @param  term         the uri of the query GO term
     * @return the number of times the query GO term occurs in addition to the number of times its children
     *                      occur in
     *                      the corpus
     */
    @Override
    public Integer getChildrenOccurrence( Map<String, Integer> termCountMap, String term ) {

        int termCount = termCountMap.get( term );
        OntologyTerm ont = geneOntologyService.getTerm( term );

        if ( ont == null ) {
            GoMetricImpl.log.warn( "No GO term found for: " + term + ", will fallback to zero for the number of children occurrences." );
            return 0;
        }

        Collection<OntologyTerm> children = ont.getChildren( false, partOf );

        if ( children.isEmpty() ) {
            return termCount;
        }

        for ( OntologyTerm child : children ) {
            if ( termCountMap.containsKey( child.getUri() ) ) {
                int count = termCountMap.get( child.getUri() );
                termCount += count;
            }
        }
        return termCount;

    }

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    @Override
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param geneOntologyService the geneOntologyService to set
     */
    @Override
    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    protected void logIds( String prefix, Collection<OntologyTerm> terms ) {
        StringBuilder buf = new StringBuilder( prefix );
        buf.append( ": [ " );
        Iterator<OntologyTerm> i = terms.iterator();
        while ( i.hasNext() ) {
            buf.append( i.next().getUri() );
            if ( i.hasNext() )
                buf.append( ", " );
        }
        buf.append( " ]" );
        GoMetricImpl.log.info( buf.toString() );
    }

    private double computeScore( Collection<String> mergedGoTerms1, Collection<String> mergedGoTerms2 ) {
        double score = 0.0;
        for ( String goTerm1 : mergedGoTerms1 ) {
            if ( goTerm1.equalsIgnoreCase( GoMetricImpl.process ) || goTerm1.equalsIgnoreCase( GoMetricImpl.function )
                    || goTerm1.equalsIgnoreCase( GoMetricImpl.component ) )
                continue;
            for ( String goTerm2 : mergedGoTerms2 ) {

                if ( goTerm2.equalsIgnoreCase( GoMetricImpl.process ) || goTerm2.equalsIgnoreCase( GoMetricImpl.function )
                        || goTerm2.equalsIgnoreCase( GoMetricImpl.component ) )
                    continue;

                if ( goTerm1.equalsIgnoreCase( goTerm2 ) )
                    score++;
            }
        }
        return score;
    }

    /**
     * @param  GOProbMap go prob map
     * @param  ontoC     onto C
     * @param  ontoM     onto M
     * @return the lowest probability value of the shared term among both collections of parent terms
     */
    private Double checkParents( OntologyTerm ontoM, OntologyTerm ontoC, Map<String, Double> GOProbMap ) {

        Collection<OntologyTerm> parentM = ontoM.getParents( false, partOf );
        parentM.add( ontoM );
        Collection<OntologyTerm> parentC = ontoC.getParents( false, partOf );
        parentC.add( ontoC );

        double pMin = 1;

        for ( OntologyTerm termM : parentM ) {
            if ( this.isRoot( termM ) )
                continue;

            for ( OntologyTerm termC : parentC ) {
                if ( this.isRoot( termC ) )
                    continue;

                if ( StringUtils.equalsIgnoreCase( termM.getUri(), termC.getUri() ) && ( GOProbMap.get( termM.getUri() ) != null ) ) {

                    double value = GOProbMap.get( termM.getUri() );
                    if ( value < pMin ) {
                        pMin = value;
                        break;
                    }
                }
            }
        }
        return pMin;
    }

    /**
     * @param  Gene2GOMap a map of genes and their GO term associations (uris)
     * @return a map of each GO term and its occurrence across the list of genes
     */
    private Map<String, Integer> getTermOccurrence( Map<Long, Collection<String>> Gene2GOMap ) {

        Map<String, Integer> countMap = new HashMap<>();
        for ( Long gene : Gene2GOMap.keySet() ) {

            for ( String uri : Gene2GOMap.get( gene ) ) {

                if ( ( uri.equalsIgnoreCase( GoMetricImpl.BASE_GO_URI + "GO_0008150" ) ) || ( uri
                        .equalsIgnoreCase( GoMetricImpl.BASE_GO_URI + "GO_0003674" ) )
                        || ( uri
                        .equalsIgnoreCase( GoMetricImpl.BASE_GO_URI + "GO_0005575" ) ) )
                    continue;

                if ( countMap.containsKey( uri ) ) {
                    int value = countMap.get( uri );
                    countMap.put( uri, ++value );
                } else {
                    countMap.put( uri, 1 );
                }
            }
        }
        return countMap;
    }

    /**
     * @param  pMin  min p
     * @param  probC prob C
     * @param  probM prob M
     * @return Jiang semantic similarity measure between two terms
     */
    private Double calcJiang( Double pMin, Double probM, Double probC ) {

        return 1 / ( ( -1 * StrictMath.log( probM ) ) + ( -1 * StrictMath.log( probC ) ) - ( -2 * StrictMath.log( pMin ) )
                + 1 );
    }

    /**
     * @param  cMatrix contingency matrix constructed from two gene vectors
     * @return kappa statistic value (Huang et al, 2007)
     */
    private Double calcKappaStat( Double[][] cMatrix ) {

        double a = cMatrix[0][0];
        double b = cMatrix[0][1];
        double c = cMatrix[1][0];
        double d = cMatrix[1][1];
        double total = a + b + c + d;

        double observed = ( a + d ) / total;

        double r1 = a + b;
        double r0 = c + d;
        double c1 = a + c;
        double c0 = b + d;

        double chance = ( c1 * r1 + c0 * r0 ) / Math.pow( total, 2 );

        return ( observed - chance ) / ( 1 - chance );

    }

    /**
     * @param  pMin  min p
     * @param  probC prob C
     * @param  probM prob M
     * @return Lin semantic similarity measure between two terms
     */
    private Double calcLin( Double pMin, Double probM, Double probC ) {

        return ( 2 * ( StrictMath.log( pMin ) ) ) / ( ( StrictMath.log( probM ) ) + ( StrictMath.log( probC ) ) );
    }

    /**
     * @param  pMin min p
     * @return Resnik semantic similarity measure between two terms
     */
    private Double calcResnik( Double pMin ) {

        return -1 * ( StrictMath.log( pMin ) );
    }

    /**
     * @param  g1 g1
     * @param  g2 g2
     * @return Similarity score for Cosine Similarity Method (Vector Space Model)
     */
    private Double computeCosineSimilarity( double[] g1, double[] g2 ) {

        Double dotProduct = this.getDotProduct( g1, g2 );
        Double g1Length = this.getVectorLength( g1 );
        Double g2Length = this.getVectorLength( g2 );
        Double score = 0.0;

        if ( g1Length != 0 && g2Length != 0 )
            score = dotProduct / ( g1Length * g2Length );

        return score;
    }

    /**
     * @param  g1 g1
     * @param  g2 g2
     * @return Similarity score using kappa statistics
     */
    private Double computeKappaSimilarity( DoubleMatrix<Long, String> gene2TermMatrix, double[] g1, double[] g2 ) {

        if ( g1.length != g2.length )
            return null;
        Double[][] contingencyMatrix = new Double[][] { { 0.0, 0.0 }, { 0.0, 0.0 } };

        for ( int i = 0; i < g1.length; i++ ) {

            if ( g1[i] == g2[i] ) {
                if ( g1[i] == 0 )
                    contingencyMatrix[1][1]++;
                if ( g1[i] == 1 )
                    contingencyMatrix[0][0]++;
            } else if ( g1[i] != g2[i] ) {
                if ( g1[i] == 0 && g2[i] == 1 )
                    contingencyMatrix[0][1]++;
                if ( g1[i] == 1 && g2[i] == 0 )
                    contingencyMatrix[1][0]++;
            }
        }

        return this.calcKappaStat( contingencyMatrix );
    }

    /**
     * @param  gene1         gene 1
     * @param  gene2         gene 2
     * @param  includePartOf include part of
     * @return percent of overlapping terms wrt to the gene with the lower number of GO terms
     */
    private Double computePercentOverlap( Gene gene1, Gene gene2, boolean includePartOf ) {
        if ( !geneOntologyService.isOntologyLoaded() )
            GoMetricImpl.log.error( "computeSimpleOverlap called before geneOntologyService is ready!!!" );

        Double avgScore = 0.0;
        Collection<OntologyTerm> masterGO = geneOntologyService.getGOTerms( gene1, includePartOf, null );
        Collection<OntologyTerm> coExpGO = geneOntologyService.getGOTerms( gene2, includePartOf, null );

        Collection<OntologyTerm> overlappingTerms = new HashSet<>();
        for ( OntologyTerm o : masterGO ) {
            if ( coExpGO.contains( o ) && !this.isRoot( o ) )
                overlappingTerms.add( o );
        }

        if ( masterGO.size() < coExpGO.size() ) {
            avgScore = ( double ) overlappingTerms.size() / masterGO.size();
        }
        if ( coExpGO.size() < masterGO.size() ) {
            avgScore = ( double ) overlappingTerms.size() / coExpGO.size();
        }
        if ( coExpGO.size() == masterGO.size() ) {
            avgScore = ( double ) overlappingTerms.size() / coExpGO.size();
        }

        return avgScore;
    }

    /**
     * @param  gene1         gene 1
     * @param  gene2         gene 2
     * @param  includePartOf include part of
     * @return number of overlapping terms
     */
    private Double computeSimpleOverlap( Gene gene1, Gene gene2, boolean includePartOf ) {
        if ( !geneOntologyService.isOntologyLoaded() )
            GoMetricImpl.log.error( "computeSimpleOverlap called before geneOntologyService is ready!!!" );

        Collection<OntologyTerm> masterGO = geneOntologyService.getGOTerms( gene1, includePartOf, null );
        Collection<OntologyTerm> coExpGO = geneOntologyService.getGOTerms( gene2, includePartOf, null );

        Collection<OntologyTerm> overlappingTerms = new HashSet<>();
        for ( OntologyTerm o : masterGO ) {
            if ( coExpGO.contains( o ) && !this.isRoot( o ) )
                overlappingTerms.add( o );
        }

        return ( double ) overlappingTerms.size();
    }

    /**
     * @param  GOFreq hashMap of GO term to its frequency in the corpus
     * @param  N      number of genes in the corpus
     * @return map
     */
    private Map<String, Double> createWeightMap( Map<String, Integer> GOFreq, Integer N ) {

        Map<String, Double> weightMap = new HashMap<>();
        for ( String id : GOFreq.keySet() ) {
            Double weightedGO = Math.log10( ( double ) N / GOFreq.get( id ) );
            weightMap.put( id, weightedGO );
        }
        return weightMap;
    }

    /**
     * @param  vector1 vector 1
     * @param  vector2 vector 2
     * @return the dot product of two vectors
     */
    private Double getDotProduct( double[] vector1, double[] vector2 ) {

        if ( vector1.length != vector2.length )
            throw new IllegalArgumentException( "Dot product can not be computed for vectors of different lengths." );
        int x = vector1.length;
        Double dotProduct = 0.0;

        for ( int i = 0; i < x; i++ ) {
            double prod = vector1[i] * vector2[i];
            if ( prod > 0 )
                dotProduct += prod;
        }

        return dotProduct;
    }

    /**
     * TODO add unsupported methods.
     *
     * @param  metric metric
     * @param  pMin   min p
     * @param  probC  prob C
     * @param  probM  prob M
     * @return a score given the choice of metric and all parameters
     */
    private Double getMetric( Metric metric, Double pMin, Double probM, Double probC ) {

        double score = 0;
        switch ( metric ) {
            case lin:
                score = this.calcLin( pMin, probM, probC );
                break;
            case jiang:
                score = this.calcJiang( pMin, probM, probC );
                break;
            case resnik:
                score = this.calcResnik( pMin );
                break;
            case cosine:
                throw new UnsupportedOperationException( "cosine not supported yet" );
            case kappa:
                throw new UnsupportedOperationException( "kappa not supported yet" );
            case percent:
                throw new UnsupportedOperationException( "percent not supported yet" );
            case simple:
                break;
            default:
                break;
        }

        return score;
    }

    /**
     * @param  gene gene
     * @return direct GO annotation terms
     */
    private Collection<OntologyTerm> getOntologyTerms( Gene gene ) {

        Collection<Characteristic> termsVoc = gene2GOAssociationService.findByGene( gene );
        HashSet<OntologyTerm> termsGO = new HashSet<>();

        for ( Characteristic characteristic : termsVoc ) {
            OntologyTerm term = geneOntologyService.getTermForId( characteristic.getValue() );
            if ( ( term != null ) )
                termsGO.add( term );
        }
        return termsGO;
    }

    private Double getVectorLength( double[] vector ) {

        Double value = 0.0;
        for ( Double i : vector ) {
            if ( i != 0 ) {
                double squared = Math.pow( i, 2 );
                value += squared;
            }
        }

        return Math.sqrt( value );
    }

    private boolean isRoot( OntologyTerm term ) {
        String id = asRegularGoId( term );
        return "GO:0008150".equalsIgnoreCase( id ) || "GO:0003674".equalsIgnoreCase( id ) || "GO:0005575".equalsIgnoreCase( id );
    }

}
