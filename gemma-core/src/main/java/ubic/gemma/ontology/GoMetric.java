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

package ubic.gemma.ontology;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.dataStructure.matrix.SparseDoubleMatrix;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.ontology.providers.GeneOntologyService;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl;
import ubic.gemma.ontology.providers.GeneOntologyServiceImpl.GOAspect;

/**
 * @author meeta
 * @version $Id$
 */
@Component
public class GoMetric {

    public enum Metric {
        jiang, lin, resnik, simple, percent, kappa, cosine
    }

    @Autowired
    private Gene2GOAssociationService gene2GOAssociationService;

    @Autowired
    private GeneOntologyService geneOntologyService;

    private boolean partOf = true;
    private String process = "http://purl.org/obo/owl/GO#GO_0008150";
    private String function = "http://purl.org/obo/owl/GO#GO_0003674";

    private String component = "http://purl.org/obo/owl/GO#GO_0005575";

    public static final String BASE_GO_URI = "http://purl.org/obo/owl/GO#";

    private static org.apache.commons.logging.Log log = LogFactory.getLog( GoMetric.class.getName() );

    /**
     * @param ontoM
     * @param ontoC
     * @param GOProbMap
     * @return the lowest probability value of the shared term among both collections of parent terms
     */
    public Double checkParents( OntologyTerm ontoM, OntologyTerm ontoC, Map<String, Double> GOProbMap ) {

        Collection<OntologyTerm> parentM = geneOntologyService.getAllParents( ontoM, partOf );
        parentM.add( ontoM );
        Collection<OntologyTerm> parentC = geneOntologyService.getAllParents( ontoC, partOf );
        parentC.add( ontoC );

        double pmin = 1;

        for ( OntologyTerm termM : parentM ) {
            if ( isRoot( termM ) ) continue;

            for ( OntologyTerm termC : parentC ) {
                if ( isRoot( termC ) ) continue;

                if ( ( termM.getUri().equalsIgnoreCase( termC.getUri() ) )
                        && ( GOProbMap.get( termM.getUri() ) != null ) ) {

                    double value = GOProbMap.get( termM.getUri() );
                    if ( value < pmin ) {
                        pmin = value;
                        break;
                    }
                }
            }
        }
        return pmin;
    }

    /**
     * @param gene1
     * @param gene2
     * @param metric
     * @return
     */
    public Double computeMatrixSimilarity( Gene gene1, Gene gene2, DoubleMatrix<Long, String> gene2TermMatrix,
            Metric metric ) {

        if ( !geneOntologyService.isReady() ) log.error( "Method called before geneOntologyService is ready!!!" );
        Double score = null;

        double[] g1 = gene2TermMatrix.getRowByName( gene1.getId() );
        double[] g2 = gene2TermMatrix.getRowByName( gene2.getId() );

        if ( metric.equals( GoMetric.Metric.cosine ) ) {
            score = computeCosineSimilarity( g1, g2 );
        }

        if ( metric.equals( GoMetric.Metric.kappa ) ) {
            score = computeKappaSimilarity( gene2TermMatrix, g1, g2 );
        }

        return score;
    }

    /**
     * @param queryGene
     * @param targetGene
     * @param GOProbMap
     * @param metric
     * @return the MAX overlap score between two genes
     * @throws Exception
     */
    public Double computeMaxSimilarity( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap, Metric metric ) {

        Collection<OntologyTerm> masterGO = getOntologyTerms( queryGene );
        if ( ( masterGO == null ) || masterGO.isEmpty() ) return 0.0;

        Collection<OntologyTerm> coExpGO = getOntologyTerms( targetGene );
        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return 0.0;

        double checkScore = 0.0;

        for ( OntologyTerm ontoM : masterGO ) {
            if ( isRoot( ontoM ) ) continue;
            if ( !GOProbMap.containsKey( ontoM.getUri() ) ) continue;
            double probM = GOProbMap.get( ontoM.getUri() );

            for ( OntologyTerm ontoC : coExpGO ) {
                if ( isRoot( ontoC ) ) continue;
                if ( !GOProbMap.containsKey( ontoC.getUri() ) ) continue;
                Double probC = GOProbMap.get( ontoC.getUri() );
                Double pmin = 1.0;
                Double score = 0.0;

                if ( ontoM.getUri().equalsIgnoreCase( ontoC.getUri() ) )
                    pmin = GOProbMap.get( ontoM.getUri() );
                else
                    pmin = checkParents( ontoM, ontoC, GOProbMap );

                if ( pmin < 1 ) {
                    score = getMetric( metric, pmin, probM, probC );
                    if ( score > checkScore ) checkScore = score;
                }

            }
        }
        log.info( "score for " + queryGene + " and " + targetGene + " is " + checkScore );
        return checkScore;
    }

    /**
     * Tailored to handle computing overlap between two gene lists which may contain duplicate genes of the same name
     * but different IDs. If gene lists do not contain duplicates (size = 1) the result will be the same as that of
     * computing simple overlap.
     * 
     * @param sameGenes1
     * @param sameGenes2
     * @param geneGoMap
     * @return number of overlapping terms between merged sets of GO terms for duplicate gene lists
     */
    public Double computeMergedOverlap( List<Gene> sameGenes1, List<Gene> sameGenes2,
            Map<Long, Collection<String>> geneGoMap ) {
        return computeMergedOverlap( sameGenes1, sameGenes2, geneGoMap, null );
    }

    /**
     * Tailored to handle computing overlap between two gene lists which may contain duplicate genes of the same name
     * but different IDs. If gene lists do not contain duplicates (size = 1) the result will be the same as that of
     * computing simple overlap.
     * 
     * @param sameGenes1
     * @param sameGenes2
     * @param geneGoMap
     * @param goAspect if non-null, limit overlap to only terms in the given aspect.
     * @return number of overlapping terms between merged sets of GO terms for duplicate gene lists
     */
    public Double computeMergedOverlap( List<Gene> sameGenes1, List<Gene> sameGenes2,
            Map<Long, Collection<String>> geneGoMap, GOAspect goAspect ) {
        HashSet<String> mergedGoTerms1 = new HashSet<String>();
        HashSet<String> mergedGoTerms2 = new HashSet<String>();

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

        if ( mergedGoTerms1.isEmpty() || mergedGoTerms2.isEmpty() ) return 0.0;

        double score = 0.0;

        for ( String goTerm1 : mergedGoTerms1 ) {

            /*
             * aspect filtering.
             */
            if ( goAspect != null ) {
                if ( geneOntologyService.getTermAspect( goTerm1 ).equals( goAspect ) ) {
                    continue;
                }
            }

            if ( goTerm1.equalsIgnoreCase( process ) || goTerm1.equalsIgnoreCase( function )
                    || goTerm1.equalsIgnoreCase( component ) ) continue;
            for ( String goTerm2 : mergedGoTerms2 ) {

                if ( goAspect != null ) {
                    if ( geneOntologyService.getTermAspect( goTerm2 ).equals( goAspect ) ) {
                        continue;
                    }
                }

                if ( goTerm2.equalsIgnoreCase( process ) || goTerm2.equalsIgnoreCase( function )
                        || goTerm2.equalsIgnoreCase( component ) ) continue;

                if ( goTerm1.equalsIgnoreCase( goTerm2 ) ) score++;
            }
        }

        return score;
    }

    /**
     * @param queryGene
     * @param targetGene
     * @param GOProbMap
     * @param metric
     * @return the overlap score between two genes
     * @throws Exception
     */
    public Double computeSimilarity( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap, Metric metric ) {

        if ( metric.equals( GoMetric.Metric.simple ) ) {
            double score = computeSimpleOverlap( queryGene, targetGene, partOf );
            return score;
        }

        if ( metric.equals( GoMetric.Metric.percent ) ) {
            double score = computePercentOverlap( queryGene, targetGene, partOf );
            return score;
        }

        Collection<OntologyTerm> masterGO = getOntologyTerms( queryGene );
        if ( ( masterGO == null ) || masterGO.isEmpty() ) return 0.0;

        Collection<OntologyTerm> coExpGO = getOntologyTerms( targetGene );
        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return 0.0;

        double total = 0;
        int count = 0;

        for ( OntologyTerm ontoM : masterGO ) {
            if ( isRoot( ontoM ) ) continue;
            if ( !GOProbMap.containsKey( ontoM.getUri() ) ) continue;
            double probM = GOProbMap.get( ontoM.getUri() );

            for ( OntologyTerm ontoC : coExpGO ) {
                if ( isRoot( ontoC ) ) continue;
                if ( !GOProbMap.containsKey( ontoC.getUri() ) ) continue;
                Double probC = GOProbMap.get( ontoC.getUri() );
                Double pmin = 1.0;
                Double score = 0.0;

                if ( ontoM.getUri().equalsIgnoreCase( ontoC.getUri() ) )
                    pmin = GOProbMap.get( ontoM.getUri() );
                else
                    pmin = checkParents( ontoM, ontoC, GOProbMap );

                if ( pmin < 1 ) {
                    score = getMetric( metric, pmin, probM, probC );
                    total += score;
                    count++;
                }
            }
        }
        if ( total > 0 ) {
            double avgScore = total / count;
            log.info( "score for " + queryGene + " and " + targetGene + " is " + avgScore );
            return avgScore;
        }
        log.info( "NO score for " + queryGene + " and " + targetGene );
        return 0.0;

    }

    /**
     * @param g
     * @param coexpG
     * @param geneGoMap
     * @return number of overlapping terms
     */

    public Double computeSimpleOverlap( Gene g, Gene coexpG, Map<Long, Collection<String>> geneGoMap ) {

        Collection<String> masterGO = geneGoMap.get( g.getId() );
        Collection<String> coExpGO = geneGoMap.get( coexpG.getId() );

        double score = 0.0;

        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return 0.0;

        if ( ( masterGO == null ) || masterGO.isEmpty() ) return 0.0;

        for ( String ontologyEntry : masterGO ) {
            if ( ontologyEntry.equalsIgnoreCase( process ) || ontologyEntry.equalsIgnoreCase( function )
                    || ontologyEntry.equalsIgnoreCase( component ) ) continue;
            for ( String ontologyEntryC : coExpGO ) {

                if ( ontologyEntry.equalsIgnoreCase( process ) || ontologyEntry.equalsIgnoreCase( function )
                        || ontologyEntry.equalsIgnoreCase( component ) ) continue;

                if ( ontologyEntry.equalsIgnoreCase( ontologyEntryC ) ) score++;
            }
        }

        return score;
    }

    /**
     * @param gene2go Map
     * @param boolean weight
     * @return Sparse matrix of genes x GOterms
     */
    public DoubleMatrix<Long, String> createVectorMatrix( Map<Long, Collection<String>> gene2go, boolean weight ) {

        Map<String, Double> GOTermFrequency = new HashMap<String, Double>();
        List<String> goTerms = new ArrayList<String>( geneOntologyService.getAllGOTermIds() );

        // Remove 'BiologicalProcess' etc.
        goTerms.remove( BASE_GO_URI + "GO_0008150" );
        goTerms.remove( BASE_GO_URI + "GO_0003674" );
        goTerms.remove( BASE_GO_URI + "GO_0005575" );

        List<Long> geneSet = new ArrayList<Long>( gene2go.keySet() );
        DoubleMatrix<Long, String> gene2term = new SparseDoubleMatrix<Long, String>( geneSet.size(), goTerms.size() );

        if ( weight ) {
            GOTermFrequency = createWeightMap( getTermOccurrence( gene2go ), gene2go.keySet().size() );
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
     * @param termCountMap each GO term uri mapped to the number of its occurrence in the corpus
     * @param the uri of the query GO term
     * @return the number of times the query GO term occurrs in addition to the number of times its children occur in
     *         the corpus
     */
    public Integer getChildrenOccurrence( Map<String, Integer> termCountMap, String term ) {

        int termCount = termCountMap.get( term );
        OntologyTerm ont = GeneOntologyServiceImpl.getTermForURI( term );

        Collection<OntologyTerm> children = geneOntologyService.getAllChildren( ont, partOf );

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
     * @param Gene2GOMap a map of genes and their GO term associations (uris)
     * @return a map of each GO term and its occurrrence across the list of genes
     */
    public Map<String, Integer> getTermOccurrence( Map<Long, Collection<String>> Gene2GOMap ) {

        Map<String, Integer> countMap = new HashMap<String, Integer>();
        for ( Long gene : Gene2GOMap.keySet() ) {

            for ( String uri : Gene2GOMap.get( gene ) ) {

                if ( ( uri.equalsIgnoreCase( BASE_GO_URI + "GO_0008150" ) )
                        || ( uri.equalsIgnoreCase( BASE_GO_URI + "GO_0003674" ) )
                        || ( uri.equalsIgnoreCase( BASE_GO_URI + "GO_0005575" ) ) ) continue;

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
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param geneOntologyService the geneOntologyService to set
     */
    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    protected void logIds( String prefix, Collection<OntologyTerm> terms ) {
        StringBuffer buf = new StringBuffer( prefix );
        buf.append( ": [ " );
        Iterator<OntologyTerm> i = terms.iterator();
        while ( i.hasNext() ) {
            buf.append( i.next().getUri() );
            if ( i.hasNext() ) buf.append( ", " );
        }
        buf.append( " ]" );
        log.info( buf.toString() );
    }

    /**
     * @return Jiang semantic similarity measure between two terms
     */
    private Double calcJiang( Double pmin, Double probM, Double probC ) {

        double scoreJiang = 1 / ( ( -1 * StrictMath.log( probM ) ) + ( -1 * StrictMath.log( probC ) )
                - ( -2 * StrictMath.log( pmin ) ) + 1 );

        return scoreJiang;
    }

    /**
     * @param cMatrix contingency matrix constructed from two gene vectors
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

        Double kappa = ( observed - chance ) / ( 1 - chance );

        return kappa;

    }

    /**
     * @return Lin semantic similarity measure between two terms
     */
    private Double calcLin( Double pmin, Double probM, Double probC ) {

        double scoreLin = ( 2 * ( StrictMath.log( pmin ) ) )
                / ( ( StrictMath.log( probM ) ) + ( StrictMath.log( probC ) ) );

        return scoreLin;
    }

    /**
     * @return Resnik semantic similarity measure between two terms
     */
    private Double calcResnik( Double pmin ) {

        double scoreResnik = -1 * ( StrictMath.log( pmin ) );

        return scoreResnik;
    }

    /**
     * @param gene2TermMatrix
     * @param gene1
     * @param gene2
     * @return Similarity score for Cosine Similarity Method (Vector Space Model)
     */
    private Double computeCosineSimilarity( double[] g1, double[] g2 ) {

        Double dotProduct = getDotProduct( g1, g2 );
        Double g1Length = getVectorLength( g1 );
        Double g2Length = getVectorLength( g2 );
        Double score = 0.0;

        if ( g1Length != 0 && g2Length != 0 ) score = dotProduct / ( g1Length * g2Length );

        return score;
    }

    /**
     * @param gene2TermMatrix
     * @param gene1
     * @param gene2
     * @return Similarity score using kappa statistics
     */
    private Double computeKappaSimilarity( DoubleMatrix<Long, String> gene2TermMatrix, double[] g1, double[] g2 ) {

        if ( g1.length != g2.length ) return null;
        Double[][] contingencyMatrix = new Double[][] { { 0.0, 0.0 }, { 0.0, 0.0 } };

        for ( int i = 0; i < g1.length; i++ ) {

            if ( g1[i] == g2[i] ) {
                if ( g1[i] == 0 ) contingencyMatrix[1][1]++;
                if ( g1[i] == 1 ) contingencyMatrix[0][0]++;
            } else if ( g1[i] != g2[i] ) {
                if ( g1[i] == 0 && g2[i] == 1 ) contingencyMatrix[0][1]++;
                if ( g1[i] == 1 && g2[i] == 0 ) contingencyMatrix[1][0]++;
            }
        }

        Double score = calcKappaStat( contingencyMatrix );
        return score;
    }

    /**
     * @param masterGO terms
     * @param coExpGO terms
     * @return percent of overlapping terms wrt to the gene with the lower number of GO terms
     */
    private Double computePercentOverlap( Gene gene1, Gene gene2, boolean includePartOf ) {
        if ( !geneOntologyService.isReady() )
            log.error( "computeSimpleOverlap called before geneOntologyService is ready!!!" );

        Double avgScore = 0.0;
        Collection<OntologyTerm> masterGO = geneOntologyService.getGOTerms( gene1, includePartOf, null );
        Collection<OntologyTerm> coExpGO = geneOntologyService.getGOTerms( gene2, includePartOf, null );

        Collection<OntologyTerm> overlappingTerms = new HashSet<OntologyTerm>();
        for ( OntologyTerm o : masterGO ) {
            if ( coExpGO.contains( o ) && !isRoot( o ) ) overlappingTerms.add( o );
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
     * @param masterGO terms
     * @param coExpGO terms
     * @return number of overlapping terms
     */
    private Double computeSimpleOverlap( Gene gene1, Gene gene2, boolean includePartOf ) {
        if ( !geneOntologyService.isReady() )
            log.error( "computeSimpleOverlap called before geneOntologyService is ready!!!" );

        Collection<OntologyTerm> masterGO = geneOntologyService.getGOTerms( gene1, includePartOf, null );
        Collection<OntologyTerm> coExpGO = geneOntologyService.getGOTerms( gene2, includePartOf, null );

        Collection<OntologyTerm> overlappingTerms = new HashSet<OntologyTerm>();
        for ( OntologyTerm o : masterGO ) {
            if ( coExpGO.contains( o ) && !isRoot( o ) ) overlappingTerms.add( o );
        }

        double avgScore = overlappingTerms.size();
        return avgScore;
    }

    /**
     * @param GOFreq hashMap of GO term to its frequency in the corpus
     * @param N number of genes in the corpus
     * @return
     */
    private Map<String, Double> createWeightMap( Map<String, Integer> GOFreq, Integer N ) {

        Map<String, Double> weightMap = new HashMap<String, Double>();
        for ( String id : GOFreq.keySet() ) {
            Double weightedGO = Math.log10( ( double ) N / GOFreq.get( id ) );
            weightMap.put( id, weightedGO );
        }
        return weightMap;
    }

    /**
     * @param vector1
     * @param vector2
     * @return the dot product of two vectors
     */
    private Double getDotProduct( double[] vector1, double[] vector2 ) {

        if ( vector1.length != vector2.length ) return null;
        int x = vector1.length;
        Double dotProduct = 0.0;

        for ( int i = 0; i < x; i++ ) {
            double prod = vector1[i] * vector2[i];
            if ( prod > 0 ) dotProduct += prod;
        }

        return dotProduct;
    }

    /**
     * FIXME add unsupported methods.
     * 
     * @param metric
     * @param pmin
     * @param probM
     * @param probC
     * @return a score given the choice of metric and all parameters
     */
    private Double getMetric( Metric metric, Double pmin, Double probM, Double probC ) {

        double score = 0;
        switch ( metric ) {
            case lin:
                score = calcLin( pmin, probM, probC );
                break;
            case jiang:
                score = calcJiang( pmin, probM, probC );
                break;
            case resnik:
                score = calcResnik( pmin );
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
     * @param gene
     * @return direct GO annotation terms
     */
    private Collection<OntologyTerm> getOntologyTerms( Gene gene ) {

        Collection<VocabCharacteristic> termsVoc = gene2GOAssociationService.findByGene( gene );
        HashSet<OntologyTerm> termsGO = new HashSet<OntologyTerm>();

        for ( VocabCharacteristic characteristic : termsVoc ) {
            OntologyTerm term = GeneOntologyServiceImpl.getTermForId( characteristic.getValue() );
            if ( ( term != null ) ) termsGO.add( term );
        }
        return termsGO;
    }

    /**
     * @param vector
     * @return the length of the vector
     */
    private Double getVectorLength( double[] vector ) {

        Double value = 0.0;
        for ( Double i : vector ) {
            if ( i != 0 ) {
                double squared = Math.pow( i, 2 );
                value += squared;
            }
        }

        Double length = Math.sqrt( value );
        return length;
    }

    /**
     * @param term
     * @return boolean whether it is a root term or not
     */
    private boolean isRoot( OntologyTerm term ) {

        String id = GeneOntologyServiceImpl.asRegularGoId( term );
        boolean root = false;
        if ( ( id.equalsIgnoreCase( "GO:0008150" ) ) || ( id.equalsIgnoreCase( "GO:0003674" ) )
                || ( id.equalsIgnoreCase( "GO:0005575" ) ) ) root = true;
        return root;
    }

}
