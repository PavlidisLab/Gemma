/*
 * The Gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package ubic.gemma.loader.association.phenotype;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.TreeSet;

import ubic.gemma.genome.gene.service.GeneService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.util.AbstractCLIContextCLI;

// many hardcoded rules, only tries if cant find !!!
public abstract class SymbolChangeAndLoggingAbstract extends AbstractCLIContextCLI {

    protected GeneService geneService = null;
    // keep a log file of the process and error
    protected TreeSet<String> logMessages = new TreeSet<String>();

    protected synchronized void loadServices() throws Exception {

        this.geneService = this.getBean( GeneService.class );
    }

    // sometimes we dont have the gene nbci, so we use taxon and gene symbol to find the correct gene
    protected Gene findGeneUsingSymbolandTaxon( String officialSymbol, String evidenceTaxon ) throws IOException {

        String officialSym = officialSymbol.replaceAll( "@", "" );

        Collection<Gene> genes = this.geneService.findByOfficialSymbol( officialSym );

        Collection<Gene> genesWithTaxon = new HashSet<Gene>();

        for ( Gene gene : genes ) {

            if ( gene.getTaxon().getCommonName().equalsIgnoreCase( evidenceTaxon ) ) {
                if ( gene.getNcbiGeneId() != null ) {
                    genesWithTaxon.add( gene );
                }
            }
        }

        if ( genesWithTaxon.isEmpty() ) {
            return checkForSymbolChange( officialSym, evidenceTaxon );
        }

        // too many results found, to check why
        else if ( genesWithTaxon.size() >= 2 ) {

            Gene g = treatGemmaMultipleGeneSpeacialCases( officialSym, genesWithTaxon, evidenceTaxon );

            if ( g != null ) {
                return g;
            }

            String error = "Found more than 1 gene using Symbol: " + officialSym + "   and taxon: " + evidenceTaxon;

            for ( Gene geneWithTaxon : genesWithTaxon ) {
                logMessages.add( error + "\tGene NCBI: " + geneWithTaxon.getNcbiGeneId() );
            }
        }

        return genesWithTaxon.iterator().next();
    }

    // when we have more than 1 choice, which one to choose, some hard coded rules so we dont redo them each time
    private Gene treatGemmaMultipleGeneSpeacialCases( String officialSymbol, Collection<Gene> genesFound,
            String evidenceTaxon ) {

        Gene theChosenGene = null;

        // human exceptions
        if ( evidenceTaxon.equalsIgnoreCase( "human" ) ) {

            // HLA-DRB1 => 3123
            if ( officialSymbol.equalsIgnoreCase( "HLA-DRB1" ) ) {
                theChosenGene = findCorrectGene( "3123", genesFound );
            }
            // CCR2 => 729230
            else if ( officialSymbol.equalsIgnoreCase( "CCR2" ) ) {
                theChosenGene = findCorrectGene( "729230", genesFound );
            }
            // NPC1 => 4864
            else if ( officialSymbol.equalsIgnoreCase( "NPC1" ) ) {
                theChosenGene = findCorrectGene( "4864", genesFound );
            }
            // PRG4 => 10216
            else if ( officialSymbol.equalsIgnoreCase( "PRG4" ) ) {
                theChosenGene = findCorrectGene( "10216", genesFound );
            }
            // TTC34 => 100287898
            else if ( officialSymbol.equalsIgnoreCase( "TTC34" ) ) {
                theChosenGene = findCorrectGene( "100287898", genesFound );
            }
            // DNAH12 => 201625
            else if ( officialSymbol.equalsIgnoreCase( "DNAH12" ) ) {
                theChosenGene = findCorrectGene( "201625", genesFound );
            }
            // PSORS1C3 => 100130889
            else if ( officialSymbol.equalsIgnoreCase( "PSORS1C3" ) ) {
                theChosenGene = findCorrectGene( "100130889", genesFound );
            }
            // MICA => 100507436
            else if ( officialSymbol.equalsIgnoreCase( "MICA" ) ) {
                theChosenGene = findCorrectGene( "100507436", genesFound );
            }

            // MICA => 100507436
            else if ( officialSymbol.equalsIgnoreCase( "MICA" ) ) {
                theChosenGene = findCorrectGene( "100507436", genesFound );
            }

            // ADH5P2 => 343296
            else if ( officialSymbol.equalsIgnoreCase( "ADH5P2" ) ) {
                theChosenGene = findCorrectGene( "343296", genesFound );
            }

            // RPL15P3 => 653232
            else if ( officialSymbol.equalsIgnoreCase( "RPL15P3" ) ) {
                theChosenGene = findCorrectGene( "653232", genesFound );
            }

            // DCDC5 => 100506627
            else if ( officialSymbol.equalsIgnoreCase( "DCDC5" ) ) {
                theChosenGene = findCorrectGene( "100506627", genesFound );
            }

        } else if ( evidenceTaxon.equalsIgnoreCase( "rat" ) ) {

            // Itga2b => 685269
            if ( officialSymbol.equalsIgnoreCase( "Itga2b" ) ) {
                theChosenGene = findCorrectGene( "685269", genesFound );
            }
            // Tcf7l2 => 679869
            else if ( officialSymbol.equalsIgnoreCase( "Tcf7l2" ) ) {
                theChosenGene = findCorrectGene( "679869", genesFound );
            }
            // Pkd2 => 498328
            else if ( officialSymbol.equalsIgnoreCase( "Pkd2" ) ) {
                theChosenGene = findCorrectGene( "498328", genesFound );
            }
            // Mthfd2 => 680308
            else if ( officialSymbol.equalsIgnoreCase( "Mthfd2" ) ) {
                theChosenGene = findCorrectGene( "680308", genesFound );
            }
            // Mthfd2 => 680308
            else if ( officialSymbol.equalsIgnoreCase( "Mef2a" ) ) {
                theChosenGene = findCorrectGene( "309957", genesFound );
            }
            // Mmp1 => 432357
            else if ( officialSymbol.equalsIgnoreCase( "Mmp1" ) ) {
                theChosenGene = findCorrectGene( "300339", genesFound );
            }

        } else if ( evidenceTaxon.equalsIgnoreCase( "mouse" ) ) {
            // H2-Ea-ps => 100504404
            if ( officialSymbol.equalsIgnoreCase( "H2-Ea-ps" ) ) {
                theChosenGene = findCorrectGene( "100504404", genesFound );
            }
            // Ccl21b => 100504404
            else if ( officialSymbol.equalsIgnoreCase( "Ccl21b" ) ) {
                theChosenGene = findCorrectGene( "100042493", genesFound );
            }
        }

        return theChosenGene;
    }

    private Gene findCorrectGene( String ncbiId, Collection<Gene> genesFound ) {

        for ( Gene gene : genesFound ) {

            if ( gene.getNcbiGeneId().toString().equalsIgnoreCase( ncbiId ) ) {
                return gene;
            }
        }
        return null;
    }

    // special case to change symbol, used when nothing was found with symbol
    private Gene checkForSymbolChange( String officialSymbol, String evidenceTaxon ) throws IOException {

        String newOfficialSymbol = null;

        if ( evidenceTaxon.equalsIgnoreCase( "human" ) ) {

            if ( officialSymbol.equalsIgnoreCase( "ARVD2" ) ) {
                newOfficialSymbol = "RYR2";
            } else if ( officialSymbol.equalsIgnoreCase( "ARVD1" ) ) {
                newOfficialSymbol = "TGFB3";
            } else if ( officialSymbol.equalsIgnoreCase( "PEO1" ) ) {
                newOfficialSymbol = "C10orf2";
            } else if ( officialSymbol.equalsIgnoreCase( "CTPS1" ) ) {
                newOfficialSymbol = "CTPS";
            } else if ( officialSymbol.equalsIgnoreCase( "CO3" ) ) {
                newOfficialSymbol = "COX3";
            } else if ( officialSymbol.equalsIgnoreCase( "CYB" ) ) {
                newOfficialSymbol = "CYTB";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ATP6" ) ) {
                newOfficialSymbol = "ATP6";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ATP8" ) ) {
                newOfficialSymbol = "ATP8";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-CO3" ) ) {
                newOfficialSymbol = "COX3";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-CYB" ) ) {
                newOfficialSymbol = "CYTB";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ND1" ) ) {
                newOfficialSymbol = "ND1";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ND2" ) ) {
                newOfficialSymbol = "ND2";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ND3" ) ) {
                newOfficialSymbol = "ND3";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ND4" ) ) {
                newOfficialSymbol = "ND4";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ND4L" ) ) {
                newOfficialSymbol = "ND4L";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ND5" ) ) {
                newOfficialSymbol = "ND5";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-ND6" ) ) {
                newOfficialSymbol = "ND6";
            } else if ( officialSymbol.equalsIgnoreCase( "MT-TL1" ) ) {
                newOfficialSymbol = "TRNL1";
            }

            else if ( officialSymbol.equalsIgnoreCase( "EFHA2" ) ) {
                newOfficialSymbol = "MICU3";
            } else if ( officialSymbol.equalsIgnoreCase( "FAM108C1" ) ) {
                newOfficialSymbol = "ABHD17C";
            } else if ( officialSymbol.equalsIgnoreCase( "GLT25D2" ) ) {
                newOfficialSymbol = "COLGALT2";
            } else if ( officialSymbol.equalsIgnoreCase( "LOC729852" ) ) {
                newOfficialSymbol = "LOC730538";
            } else if ( officialSymbol.equalsIgnoreCase( "UTS2D" ) ) {
                newOfficialSymbol = "UTS2B";
            }

            else if ( officialSymbol.equalsIgnoreCase( "LOC730538" ) ) {
                newOfficialSymbol = "RPA3-AS1";
            } else if ( officialSymbol.equalsIgnoreCase( "AGPHD1" ) ) {
                newOfficialSymbol = "HYKK";
            } else if ( officialSymbol.equalsIgnoreCase( "C8orf42" ) ) {
                newOfficialSymbol = "TDRP";
            } else if ( officialSymbol.equalsIgnoreCase( "CCBP2" ) ) {
                newOfficialSymbol = "ACKR4";
            } else if ( officialSymbol.equalsIgnoreCase( "DBC1" ) ) {
                newOfficialSymbol = "BRINP1";
            }

        } else if ( evidenceTaxon.equalsIgnoreCase( "rat" ) ) {

            if ( officialSymbol.equalsIgnoreCase( "Hsd3b2" ) ) {
                newOfficialSymbol = "Hsd3b1";
            } else if ( officialSymbol.equalsIgnoreCase( "Mt-coi" ) || officialSymbol.equalsIgnoreCase( "Mt-co1" ) ) {
                newOfficialSymbol = "COX1";
            } else if ( officialSymbol.equalsIgnoreCase( "Mt-cyb" ) ) {
                newOfficialSymbol = "CYTB";
            } else if ( officialSymbol.equalsIgnoreCase( "Mt-nd1" ) ) {
                newOfficialSymbol = "ND1";
            } else if ( officialSymbol.equalsIgnoreCase( "Mt-co2" ) ) {
                newOfficialSymbol = "COX2";
            } else if ( officialSymbol.equalsIgnoreCase( "Mt-nd5" ) ) {
                newOfficialSymbol = "ND5";
            } else if ( officialSymbol.equalsIgnoreCase( "Mt-nd3" ) ) {
                newOfficialSymbol = "ND3";
            } else if ( officialSymbol.equalsIgnoreCase( "Srebf1_v2" ) ) {
                newOfficialSymbol = "Srebf1";
            } else if ( officialSymbol.equalsIgnoreCase( "Naip6" ) ) {
                newOfficialSymbol = "Naip2";
            } else if ( officialSymbol.equalsIgnoreCase( "Slco1a4" ) ) {
                newOfficialSymbol = "Slco1a2";
            } else if ( officialSymbol.equalsIgnoreCase( "Klk1b3" ) ) {
                newOfficialSymbol = "Klk1";
            }
        }

        if ( newOfficialSymbol != null ) {
            return findGeneUsingSymbolandTaxon( newOfficialSymbol, evidenceTaxon );
        }

        writeError( "Symbol: " + officialSymbol + "   Taxon:" + evidenceTaxon
                + "   returned null, this gene was not found in Gemma" );
        return null;

    }

    protected void writeError( String errorMessage ) {
        log.error( errorMessage );
        // this gives the summary of erros at the end
        logMessages.add( errorMessage );

    }

}
