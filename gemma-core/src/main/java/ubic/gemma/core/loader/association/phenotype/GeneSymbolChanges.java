/*
 * The gemma-core project
 * 
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.loader.association.phenotype;

import java.io.IOException;
import java.util.Collection;

import ubic.gemma.model.genome.Gene;

/**
 * FIXME delete
 * 
 * @author paul
 */
public class GeneSymbolChanges {
//
//    /**
//     * ???
//     * 
//     * @param  ncbiId
//     * @param  genesFound
//     * @return
//     */
//    static Gene findCorrectGene( String ncbiId, Collection<Gene> genesFound ) {
//        for ( Gene gene : genesFound ) {
//            if ( gene.getNcbiGeneId().toString().equalsIgnoreCase( ncbiId ) ) {
//                return gene;
//            }
//        }
//        return null;
//    }

//    /**
//     * ???
//     * 
//     * @param  officialSymbol
//     * @param  genesFound
//     * @return
//     */
//    static Gene populateMouseGene( String officialSymbol, Collection<Gene> genesFound ) {
//        Gene theChosenGene = null;
//        // H2-Ea-ps => 100504404
//        if ( officialSymbol.equalsIgnoreCase( "H2-Ea-ps" ) ) {
//            theChosenGene = findCorrectGene( "100504404", genesFound );
//        }
//        // Ccl21b => 100504404
//        else if ( officialSymbol.equalsIgnoreCase( "Ccl21b" ) ) {
//            theChosenGene = findCorrectGene( "100042493", genesFound );
//        }
//        return theChosenGene;
//    }
//
//    /**
//     * FIXME what is this for?
//     * 
//     * @param  officialSymbol
//     * @return
//     */
//    static Gene populateRatGene( String officialSymbol, Collection<Gene> genesFound ) {
//        Gene theChosenGene = null;
//        // Itga2b => 685269
//        if ( officialSymbol.equalsIgnoreCase( "Itga2b" ) ) {
//            theChosenGene = findCorrectGene( "685269", genesFound );
//        }
//        // Tcf7l2 => 679869
//        else if ( officialSymbol.equalsIgnoreCase( "Tcf7l2" ) ) {
//            theChosenGene = findCorrectGene( "679869", genesFound );
//        }
//        // Pkd2 => 498328
//        else if ( officialSymbol.equalsIgnoreCase( "Pkd2" ) ) {
//            theChosenGene = findCorrectGene( "498328", genesFound );
//        }
//        // Mthfd2 => 680308
//        else if ( officialSymbol.equalsIgnoreCase( "Mthfd2" ) ) {
//            theChosenGene = findCorrectGene( "680308", genesFound );
//        }
//        // Mthfd2 => 680308
//        else if ( officialSymbol.equalsIgnoreCase( "Mef2a" ) ) {
//            theChosenGene = findCorrectGene( "309957", genesFound );
//        }
//        // Mmp1 => 432357
//        else if ( officialSymbol.equalsIgnoreCase( "Mmp1" ) ) {
//            theChosenGene = findCorrectGene( "300339", genesFound );
//        }
//        return theChosenGene;
//    }

//    /**
//     * FIXME what is this for?
//     * 
//     * @param  officialSymbol
//     * @return
//     */
//    static String populateRatSymbol( String officialSymbol ) {
//        String newOfficialSymbol = null;
//        if ( officialSymbol.equalsIgnoreCase( "Hsd3b2" ) ) {
//            newOfficialSymbol = "Hsd3b1";
//        } else if ( officialSymbol.equalsIgnoreCase( "Mt-coi" ) || officialSymbol.equalsIgnoreCase( "Mt-co1" ) ) {
//            newOfficialSymbol = "COX1";
//        } else if ( officialSymbol.equalsIgnoreCase( "Mt-cyb" ) ) {
//            newOfficialSymbol = "CYTB";
//        } else if ( officialSymbol.equalsIgnoreCase( "Mt-nd1" ) ) {
//            newOfficialSymbol = "ND1";
//        } else if ( officialSymbol.equalsIgnoreCase( "Mt-co2" ) ) {
//            newOfficialSymbol = "COX2";
//        } else if ( officialSymbol.equalsIgnoreCase( "Mt-nd5" ) ) {
//            newOfficialSymbol = "ND5";
//        } else if ( officialSymbol.equalsIgnoreCase( "Mt-nd3" ) ) {
//            newOfficialSymbol = "ND3";
//        } else if ( officialSymbol.equalsIgnoreCase( "Srebf1_v2" ) ) {
//            newOfficialSymbol = "Srebf1";
//        } else if ( officialSymbol.equalsIgnoreCase( "Naip6" ) ) {
//            newOfficialSymbol = "Naip2";
//        } else if ( officialSymbol.equalsIgnoreCase( "Slco1a4" ) ) {
//            newOfficialSymbol = "Slco1a2";
//        } else if ( officialSymbol.equalsIgnoreCase( "Klk1b3" ) ) {
//            newOfficialSymbol = "Klk1";
//        }
//        return newOfficialSymbol;
//    }


}
