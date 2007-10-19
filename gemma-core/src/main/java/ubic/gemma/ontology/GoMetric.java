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

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;

import org.jfree.util.Log;

import ubic.gemma.model.association.Gene2GOAssociationService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneService;

/**
 * @author meeta
 * @spring.bean id="goMetric"
 * @spring.property name="gene2GOAssociationService" ref="gene2GOAssociationService"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="geneOntologyService" ref="geneOntologyService"
 */

public class GoMetric {

    private Gene2GOAssociationService gene2GOAssociationService;
    private GeneService geneService;
    private GeneOntologyService geneOntologyService;
    private boolean partOf = true;

    public enum Metric {
        jiang, lin, resnik, simple
    };

    /**
     * @param gene2GOAssociationService the gene2GOAssociationService to set
     */
    public void setGene2GOAssociationService( Gene2GOAssociationService gene2GOAssociationService ) {
        this.gene2GOAssociationService = gene2GOAssociationService;
    }

    /**
     * @param geneService the geneService to set
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    /**
     * @param geneOntologyService the geneOntologyService to set
     */
    public void setGeneOntologyService( GeneOntologyService geneOntologyService ) {
        this.geneOntologyService = geneOntologyService;
    }

    /**
     * @param Gene2GOMap a map of genes and their GO term associations (uris)
     * @return a map of each GO term and its occurrrence across the list of genes
     */
    public Integer getTermOccurrence( Map<Long, Collection<String>> Gene2GOMap, String term ) {

        int value = 0;
        String termId = GeneOntologyService.asRegularGoId( GeneOntologyService.getTermForURI( term ) );

        for ( Long gene : Gene2GOMap.keySet() ) {

            Collection<String> GO = Gene2GOMap.get( gene );

            for ( String uri : GO ) {

                String ontId = GeneOntologyService.asRegularGoId( GeneOntologyService.getTermForURI( uri ) );

                if ( ontId.equalsIgnoreCase( "GO:0008150" ) || ontId.equalsIgnoreCase( "GO:0003674" )
                        || ontId.equalsIgnoreCase( "GO:0005575" ) ) continue;

                if ( ontId.equalsIgnoreCase( termId ) ) {
                    value++;
                    break;
                }
            }
        }
        return value;
    }

    /**
     * @param termCountMap each GO term uri mapped to the number of its occurrence in the corpus
     * @param the uri of the query GO term
     * @return the number of times the query GO term occurrs in addition to the number of times its children occur in
     *         the corpus
     */
    public Integer getChildrenOccurrence( Map<String, Integer> termCountMap, String term ) {

        int termCount = termCountMap.get( term );
        OntologyTerm ont = GeneOntologyService.getTermForURI( term );

        Collection<OntologyTerm> children = geneOntologyService.getAllChildren( ont, partOf );

        if ( children.isEmpty() || children == null ) {

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
     * @param queryGene
     * @param targetGene
     * @param GOProbMap
     * @param metric
     * @return the overlap score between two genes
     * @throws Exception
     */
    public Double computeSimilarityOverlap( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap,
            Metric metric ) {

        Collection<VocabCharacteristic> masterVoc = gene2GOAssociationService.findByGene( queryGene );
        HashSet<OntologyTerm> masterGO = getOntologyTerms( masterVoc );
        if ( ( masterGO == null ) || masterGO.isEmpty() ) return null;

        Collection<VocabCharacteristic> coExpVoc = gene2GOAssociationService.findByGene( targetGene );
        HashSet<OntologyTerm> coExpGO = getOntologyTerms( coExpVoc );
        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return null;

        if ( metric.equals( GoMetric.Metric.simple ) ) {

            try {
                Collection<OntologyTerm> overlap = geneOntologyService.calculateGoTermOverlap( queryGene, targetGene );
                double avgScore = ( double ) overlap.size();
                return avgScore;
            } catch ( Exception e ) {
                Log.info( "Could not calculate simple overlap!" );
            }

        }

        double total = 0;
        int count = 0;

        for ( OntologyTerm ontoM : masterGO ) {
            if ( !GOProbMap.containsKey( ontoM.getUri() ) ) {
                Log.info( "Go probe map doesn't contain " + ontoM );
                continue;
            }
            double probM = GOProbMap.get( ontoM.getUri() );

            for ( OntologyTerm ontoC : coExpGO ) {
                if ( !GOProbMap.containsKey( ontoC.getUri() ) ) {
                    Log.info( "Go probe map doesn't contain " + ontoC );
                    continue;
                }
                Double probC = GOProbMap.get( ontoC.getUri() );
                Double pmin = 1.0;
                Double score = 0.0;

                if ( ontoM == ontoC ) {
                    pmin = GOProbMap.get( ontoM.getUri() );
                } else
                    pmin = checkParents( ontoM, ontoC, GOProbMap );

                if ( pmin < 1 ) {
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
                    }
                    Log.info( "score for " + ontoM + " and " + ontoC + " is " + score );
                    total += score;
                    count++;
                }
            }
        }
        if ( total > 0 ) {
            double avgScore = total / count;
            return avgScore;
        } else
            return null;
    }

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
            String id = GeneOntologyService.asRegularGoId( termM );
            if ( ( id.equalsIgnoreCase( "GO:0008150" ) ) || ( id.equalsIgnoreCase( "GO:0003674" ) )
                    || ( id.equalsIgnoreCase( "GO:0005575" ) ) ) continue;

            for ( OntologyTerm termC : parentC ) {
                String id2 = GeneOntologyService.asRegularGoId( termM );
                if ( ( id2.equalsIgnoreCase( "GO:0008150" ) ) || ( id2.equalsIgnoreCase( "GO:0003674" ) )
                        || ( id2.equalsIgnoreCase( "GO:0005575" ) ) ) continue;

                if ( ( termM.getUri().equalsIgnoreCase( termC.getUri() ) )
                        && ( GOProbMap.get( termM.getUri() ) != null ) ) {

                    double value = GOProbMap.get( termM.getUri() );
                    if ( value < pmin ) {
                        pmin = value;
                    }
                }
            }
        }
        return pmin;
    }

    /**
     * @param vocab characteristics
     * @return GO terms
     */
    private HashSet<OntologyTerm> getOntologyTerms( Collection<VocabCharacteristic> voc ) {

        HashSet<OntologyTerm> ontTerms = new HashSet();

        for ( VocabCharacteristic characteristic : voc ) {
            OntologyTerm term = GeneOntologyService.getTermForId( characteristic.getValue() );
            if ( ( term != null ) ) ontTerms.add( term );
        }
        return ontTerms;
    }

    /**
     * @return Lin semantic similarity measure between two terms
     */
    private Double calcLin( Double pmin, Double probM, Double probC ) {

        double scoreLin = ( 2 * ( StrictMath.log10( pmin ) ) )
                / ( ( StrictMath.log10( probM ) ) + ( StrictMath.log10( probC ) ) );

        return scoreLin;
    }

    /**
     * @return Jiang semantic similarity measure between two terms
     */
    private Double calcJiang( Double pmin, Double probM, Double probC ) {

        double scoreJiang = 1 / ( ( StrictMath.log10( probM ) ) + ( StrictMath.log10( probC ) ) - 2
                * ( StrictMath.log10( pmin ) ) + 1 );

        return scoreJiang;
    }

    /**
     * @return Resnik semantic similarity measure between two terms
     */
    private Double calcResnik( Double pmin ) {

        double scoreResnik = -1 * ( StrictMath.log10( pmin ) );

        return scoreResnik;
    }
}
