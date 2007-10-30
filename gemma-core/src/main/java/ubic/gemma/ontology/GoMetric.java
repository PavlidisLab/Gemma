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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.logging.LogFactory;

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

    private static org.apache.commons.logging.Log log = LogFactory.getLog( GoMetric.class.getName() );

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
    public Map<String, Integer> getTermOccurrence( Map<Long, Collection<String>> Gene2GOMap ) {


        Map<String, Integer> countMap = new HashMap<String, Integer>();
        for ( Long gene : Gene2GOMap.keySet() ) {

            for ( String uri : Gene2GOMap.get( gene ) ) {

                if ( isRoot( GeneOntologyService.getTermForURI( uri ) ) ) continue;
                
                if (countMap.containsKey( uri )){
                    int value = countMap.get( uri );
                    countMap.put( uri, ++value );
                }
                else{
                    countMap.put( uri, 1 );
                }
            }
        }
        return countMap;
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
    public Double computeSimilarity( Gene queryGene, Gene targetGene, Map<String, Double> GOProbMap,
            Metric metric ) {

        if ( metric.equals( GoMetric.Metric.simple ) ) {
            double score = computeSimpleOverlap( queryGene, targetGene, partOf );
            return score;
        }

        Collection<VocabCharacteristic> masterVoc = gene2GOAssociationService.findByGene( queryGene );
        Collection<OntologyTerm> masterGO = getOntologyTerms( masterVoc );
        if ( ( masterGO == null ) || masterGO.isEmpty() ) return null;

        Collection<VocabCharacteristic> coExpVoc = gene2GOAssociationService.findByGene( targetGene );
        Collection<OntologyTerm> coExpGO = getOntologyTerms( coExpVoc );
        if ( ( coExpGO == null ) || coExpGO.isEmpty() ) return null;

        double total = 0;
        int count = 0;

        for ( OntologyTerm ontoM : masterGO ) {
            if ( !GOProbMap.containsKey( ontoM.getUri() ) ) {
                log.info( "Go probe map doesn't contain " + ontoM );
                continue;
            }
            double probM = GOProbMap.get( ontoM.getUri() );

            for ( OntologyTerm ontoC : coExpGO ) {
                if ( !GOProbMap.containsKey( ontoC.getUri() ) ) {
                    log.info( "Go probe map doesn't contain " + ontoC );
                    continue;
                }
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
        } else {
            log.info( "NO score for " + queryGene + " and " + targetGene );
            return null;
        }
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
     * @param masterGO terms
     * @param coExpGO terms
     * @return number of overlapping terms
     */
    private Double computeSimpleOverlap( Gene gene1, Gene gene2, boolean includePartOf ) {

        Collection<OntologyTerm> masterGO = geneOntologyService.getGOTerms( gene1, includePartOf );
        Collection<OntologyTerm> coExpGO = geneOntologyService.getGOTerms( gene2, includePartOf );

        masterGO.retainAll( coExpGO );

        Collection<OntologyTerm> noRoots = new HashSet<OntologyTerm>();
        for ( OntologyTerm o : masterGO ) {
            if ( !isRoot( o ) ) noRoots.add( o );
        }

        double avgScore = ( double ) noRoots.size();
        return avgScore;
    }

    /**
     * @param term
     * @return boolean whether it is a root term or not
     */
    private boolean isRoot( OntologyTerm term ) {

        String id = GeneOntologyService.asRegularGoId( term );
        boolean root = false;
        if ( ( id.equalsIgnoreCase( "GO:0008150" ) ) || ( id.equalsIgnoreCase( "GO:0003674" ) )
                || ( id.equalsIgnoreCase( "GO:0005575" ) ) ) root = true;
        return root;
    }

    /**
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
        }

        return score;
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

        double scoreLin = ( 2 * ( StrictMath.log( pmin ) ) )
                / ( ( StrictMath.log( probM ) ) + ( StrictMath.log( probC ) ) );

        return scoreLin;
    }

    /**
     * @return Jiang semantic similarity measure between two terms
     */
    private Double calcJiang( Double pmin, Double probM, Double probC ) {

        double scoreJiang = 1 / ( ( StrictMath.log( probM ) ) + ( StrictMath.log( probC ) ) - 2
                * ( StrictMath.log( pmin ) ) + 1 );

        return scoreJiang;
    }

    /**
     * @return Resnik semantic similarity measure between two terms
     */
    private Double calcResnik( Double pmin ) {

        double scoreResnik = -1 * ( StrictMath.log( pmin ) );

        return scoreResnik;
    }
}
