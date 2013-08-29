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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.gemma.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.EvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.ExperimentalEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.GenericEvidenceValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.LiteratureEvidenceValueObject;

/**
 * Class used to load evidence into Neurocarta The file used to import the evidence must have at least those columns:
 * (GeneSymbol, GeneId, EvidenceCode, Comments, IsNegative, Phenotypes) The order of the column is not important,
 * EvidenceImporterAbstractCLI contain the naming rules of those colunms
 * 
 * @author nicolas
 * @version $Id$
 */
public class EvidenceImporterCLI extends EvidenceImporterAbstractCLI {

    // initArgument is only call when no argument is given on the command line, (it make it faster to run it in eclipse)
    private static String[] initArguments() {

        String[] args = new String[12];
        // user
        args[0] = "-u";
        args[1] = "administrator";
        // password
        args[2] = "-p";
        args[3] = "administrator";
        // the path of the file
        args[4] = "-f";
        args[5] = "/home/nicolas/workspace/Gemma/gemma-core/src/main/resources/neurocarta/evidenceImporter/FilesToImport/finalResults.tsv";
        // create the evidence in the database
        args[6] = "-c";
        args[7] = "true";
        // environment we dont have all genes on a test database, if we are using the production let it know should find
        // true == production database
        // false == testDatabase, put gene not found to NCBI 1
        args[8] = "-e";
        args[9] = "true";
        // is the geneNCBI missing ???
        // then it will use the taxon and official symbol to find the gene NBCI, let it know the taxon
        // possible values are : "human","mouse","rat" and "" ( if we have the NCBI gene id )
        args[10] = "-n";
        args[11] = "";

        return args;
    }

    public static void main( String[] args ) {

        EvidenceImporterCLI evidenceImporterCLI = new EvidenceImporterCLI();

        try {
            Exception ex = null;

            String[] argsToTake = null;

            if ( args.length == 0 ) {
                argsToTake = initArguments();
            } else {
                argsToTake = args;
            }

            ex = evidenceImporterCLI.doWork( argsToTake );

            if ( ex != null ) {
                ex.printStackTrace();
            }
        } catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = processCommandLine( "EvidenceImporterCLI", args );
        if ( err != null ) return err;

        try {
            createWriteFolder();

            FileWriter fstream = new FileWriter( WRITE_FOLDER + File.separator + "EvidenceImporter.log" );
            this.logFileWriter = new BufferedWriter( fstream );

            log.info( "File: " + this.inputFile );
            log.info( "Create in Database: " + this.createInDatabase );
            if ( this.prodDatabase ) {
                log.info( "Connection read or write to Production Database" );
            } else {
                log.info( "Using a test database" );
            }

            this.br = new BufferedReader( new FileReader( this.inputFile ) );

            String typeOfEvidence = findTypeOfEvidence();

            // take the file received and create the real objects from it
            Collection<EvidenceValueObject> evidenceValueObjects = file2Objects( typeOfEvidence );

            // make sure all pubmed exists
            if ( !this.errorMessage.isEmpty() ) {

                this.writeAllExceptions();

                this.logFileWriter.close();
                throw new Exception( "check logs" );
            }

            if ( !this.warningMessage.isEmpty() ) {
                log.info( this.warningMessage );
            }

            if ( this.createInDatabase ) {
                int i = 1;

                // for each evidence creates it in Neurocarta
                for ( EvidenceValueObject e : evidenceValueObjects ) {
                    try {
                        // create the evidence in neurocarta
                        this.phenotypeAssociationService.makeEvidence( e );
                    } catch ( EntityNotFoundException ex ) {

                        this.writeError( "went into the exception" );

                        // if a pubmed id was not found dont stop all processes and write to logs
                        if ( ex.getMessage().indexOf( "pubmed id" ) != -1 ) {
                            this.writeError( ex.getMessage() );
                        } else {
                            throw ex;
                        }
                    }

                    log.info( "created evidence " + i );
                    i++;
                }
            }

            this.logFileWriter.close();

            log.info( "Import of evidence is finish" );
            // when we import a file in production we keep a copy of the imported file and keep track of when the file
            // was imported in a log file
            if ( this.prodDatabase && this.createInDatabase ) {
                createImportLog( evidenceValueObjects.iterator().next() );
            }

        } catch ( Exception e ) {
            return e;
        }
        System.exit( -1 );
        return null;
    }

    /**
     * Change the file received into an entity that can save in the database
     * 
     * @throws Exception
     */
    private Collection<EvidenceValueObject> file2Objects( String evidenceType ) throws Exception {

        Collection<EvidenceValueObject> evidenceValueObjects = new ArrayList<EvidenceValueObject>();
        String line = "";
        int i = 1;

        // for each line of the file
        while ( ( line = this.br.readLine() ) != null ) {

            String[] tokens = line.split( "\t" );

            log.info( "Reading evidence: " + i++ );

            try {

                if ( evidenceType.equals( this.LITERATURE_EVIDENCE ) ) {
                    evidenceValueObjects.add( convertFileLine2LiteratureValueObjects( tokens ) );
                } else if ( evidenceType.equals( this.EXPERIMENTAL_EVIDENCE ) ) {
                    evidenceValueObjects.add( convertFileLine2ExperimentalValueObjects( tokens ) );
                } else if ( evidenceType.equals( this.GENERIC_EVIDENCE ) ) {
                    evidenceValueObjects.add( convertFileLine2GenericValueObjects( tokens ) );
                }
            } catch ( EntityNotFoundException e ) {
                writeWarning( e.getMessage() );
            }
        }

        this.br.close();

        return evidenceValueObjects;
    }

    /**
     * File to valueObject conversion, populate the basics
     * 
     * @throws Exception
     */
    private void populateCommonFields( EvidenceValueObject evidence, String[] tokens ) throws IOException {

        boolean isNegativeEvidence = false;

        String geneSymbol = tokens[this.mapColumns.get( "GeneSymbol" )].trim();
        String geneID = tokens[this.mapColumns.get( "GeneId" )].trim();

        String evidenceCode = tokens[this.mapColumns.get( "EvidenceCode" )].trim();

        checkEvidenceCodeExits( evidenceCode );

        String description = tokens[this.mapColumns.get( "Comments" )].trim();

        if ( this.mapColumns.get( "IsNegative" ) != null
                && tokens[this.mapColumns.get( "IsNegative" )].trim().equals( "1" ) ) {
            isNegativeEvidence = true;
        }

        String externalDatabaseName = tokens[this.mapColumns.get( "ExternalDatabase" )].trim();

        String databaseID = tokens[this.mapColumns.get( "DatabaseLink" )].trim();

        Set<String> phenotypeFromArray = trimArray( tokens[this.mapColumns.get( "Phenotypes" )].split( ";" ) );

        int geneNcbiId = 0;

        if ( geneID.equals( "" ) && !this.geneNcbiIdMissingUsingTaxon.equals( "" ) ) {
            geneNcbiId = findGeneId( geneSymbol );
        } else {
            geneNcbiId = verifyGeneIdExist( geneID, geneSymbol );
        }

        SortedSet<CharacteristicValueObject> phenotypes = toValuesUri( phenotypeFromArray );

        evidence.setDescription( description );
        evidence.setEvidenceCode( evidenceCode );
        evidence.setEvidenceSource( makeEvidenceSource( databaseID, externalDatabaseName ) );
        evidence.setGeneNCBI( geneNcbiId );
        evidence.setPhenotypes( phenotypes );
        evidence.setIsNegativeEvidence( isNegativeEvidence );

        if ( this.mapColumns.get( "Score" ) != null && this.mapColumns.get( "ScoreType" ) != null
                && this.mapColumns.get( "Strength" ) != null ) {

            try {

                String score = tokens[this.mapColumns.get( "Score" )].trim();
                String scoreName = tokens[this.mapColumns.get( "ScoreType" )].trim();
                String strength = tokens[this.mapColumns.get( "Strength" )].trim();

                // score
                evidence.getScoreValueObject().setScoreValue( score );
                evidence.getScoreValueObject().setScoreName( scoreName );
                evidence.getScoreValueObject().setStrength( new Double( strength ) );
            } catch ( ArrayIndexOutOfBoundsException e ) {
                // no score set for this evidence, blank space
            }

        } else if ( !externalDatabaseName.equalsIgnoreCase( "" ) ) {
            setScoreDependingOnExternalSource( externalDatabaseName, evidence );
        }
    }

    /**
     * convert for GenericEvidenceValueObject
     */
    private GenericEvidenceValueObject convertFileLine2GenericValueObjects( String[] tokens ) throws IOException {

        GenericEvidenceValueObject evidence = new GenericEvidenceValueObject();

        populateCommonFields( evidence, tokens );

        return evidence;
    }

    /**
     * convert for LiteratureEvidenceValueObject
     */
    private LiteratureEvidenceValueObject convertFileLine2LiteratureValueObjects( String[] tokens ) throws IOException {

        LiteratureEvidenceValueObject evidence = new LiteratureEvidenceValueObject();
        populateCommonFields( evidence, tokens );

        String primaryReferencePubmed = tokens[this.mapColumns.get( "PrimaryPubMed" )].trim();

        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( primaryReferencePubmed );
        evidence.setCitationValueObject( citationValueObject );

        return evidence;
    }

    /**
     * convert for ExperimentalEvidenceValueObject
     */
    private ExperimentalEvidenceValueObject convertFileLine2ExperimentalValueObjects( String[] tokens )
            throws IOException {

        ExperimentalEvidenceValueObject evidence = new ExperimentalEvidenceValueObject();
        populateCommonFields( evidence, tokens );

        String primaryReferencePubmed = tokens[this.mapColumns.get( "PrimaryPubMed" )].trim();

        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( primaryReferencePubmed );
        evidence.setPrimaryPublicationCitationValueObject( citationValueObject );

        String reviewReferencePubmed = tokens[this.mapColumns.get( "OtherPubMed" )].trim();

        Set<String> relevantPublicationsPubmed = new HashSet<String>();
        if ( !reviewReferencePubmed.equals( "" ) ) {

            relevantPublicationsPubmed.add( reviewReferencePubmed );
        }

        for ( String relevantPubMedID : relevantPublicationsPubmed ) {
            CitationValueObject relevantPublicationValueObject = new CitationValueObject();
            relevantPublicationValueObject.setPubmedAccession( relevantPubMedID );
            evidence.getRelevantPublicationsCitationValueObjects().add( relevantPublicationValueObject );
        }

        Set<String> developmentStage = trimArray( tokens[this.mapColumns.get( "DevelopmentalStage" )].split( ";" ) );
        Set<String> bioSource = trimArray( tokens[this.mapColumns.get( "BioSource" )].split( ";" ) );
        Set<String> organismPart = trimArray( tokens[this.mapColumns.get( "OrganismPart" )].split( ";" ) );
        Set<String> experimentDesign = trimArray( tokens[this.mapColumns.get( "ExperimentDesign" )].split( ";" ) );
        Set<String> treatment = trimArray( tokens[this.mapColumns.get( "Treatment" )].split( ";" ) );
        Set<String> experimentOBI = trimArray( tokens[this.mapColumns.get( "Experiment" )].split( ";" ) );

        Set<CharacteristicValueObject> experimentTags = new HashSet<CharacteristicValueObject>();

        experimentTags.addAll( experiementTags2Ontology( developmentStage, this.DEVELOPMENTAL_STAGE,
                this.DEVELOPMENTAL_STAGE_ONTOLOGY, this.nifstdOntologyService ) );
        experimentTags.addAll( experiementTags2Ontology( bioSource, this.BIOSOURCE_ONTOLOGY, this.BIOSOURCE_ONTOLOGY,
                null ) );
        experimentTags.addAll( experiementTags2Ontology( organismPart, this.ORGANISM_PART, this.ORGANISM_PART_ONTOLOGY,
                this.fmaOntologyService ) );
        experimentTags.addAll( experiementTags2Ontology( experimentDesign, this.EXPERIMENT_DESIGN,
                this.EXPERIMENT_DESIGN_ONTOLOGY, this.obiService ) );
        experimentTags.addAll( experiementTags2Ontology( treatment, this.TREATMENT, this.TREATMENT_ONTOLOGY, null ) );
        experimentTags.addAll( experiementTags2Ontology( experimentOBI, this.EXPERIMENT, this.EXPERIMENT_ONTOLOGY,
                this.obiService ) );

        evidence.setExperimentCharacteristics( experimentTags );

        return evidence;
    }

    // Change a set of phenotype to a set of CharacteristicValueObject
    private SortedSet<CharacteristicValueObject> toValuesUri( Set<String> phenotypes ) throws IOException {

        SortedSet<CharacteristicValueObject> characteristicPhenotypes = new TreeSet<CharacteristicValueObject>();

        for ( String phenotype : phenotypes ) {

            String valueUri = phenotype2Ontology( phenotype );

            if ( valueUri != null ) {
                CharacteristicValueObject c = new CharacteristicValueObject( valueUri );
                characteristicPhenotypes.add( c );
            }
        }

        return characteristicPhenotypes;
    }

    // value or valueUri given changed to valueUri (even if valueUri is given in file we need to check)
    private String phenotype2Ontology( String phenotypeToSearch ) throws IOException {

        OntologyTerm ot = null;

        // we got an uri, search by uri
        if ( phenotypeToSearch.indexOf( "http://purl." ) != -1 ) {
            log.info( "Found an URI: " + phenotypeToSearch );
            ot = this.diseaseOntologyService.getTerm( phenotypeToSearch );

            if ( ot == null ) {
                ot = this.humanPhenotypeOntologyService.getTerm( phenotypeToSearch );
            }
            if ( ot == null ) {
                ot = this.mammalianPhenotypeOntologyService.getTerm( phenotypeToSearch );
            }
        }
        // value found
        else {

            // search disease
            Collection<OntologyTerm> ontologyTerms = this.diseaseOntologyService.findTerm( phenotypeToSearch );

            ot = findExactTerm( ontologyTerms, phenotypeToSearch );

            if ( ot == null ) {

                // search hp
                ontologyTerms = this.humanPhenotypeOntologyService.findTerm( phenotypeToSearch );
                ot = findExactTerm( ontologyTerms, phenotypeToSearch );

            }
            if ( ot == null ) {

                // search mammalian
                ontologyTerms = this.mammalianPhenotypeOntologyService.findTerm( phenotypeToSearch );
                ot = findExactTerm( ontologyTerms, phenotypeToSearch );
            }
        }

        // we cannot find the specific phenotype given
        if ( ot == null ) {
            writeError( "phenotype not found in disease, hp and mp Ontology : " + phenotypeToSearch );
            return null;
        }

        // check for obsolete terms
        if ( ot.isTermObsolete() ) {
            writeError( "TERM IS OBSOLETE: " + ot.getLabel() + " " + ot.getUri() );
        }

        return ot.getUri();
    }

    /**
     * check that all gene exists in Gemma
     * 
     * @throws IOException
     * @throws Exception
     */
    private int verifyGeneIdExist( String geneId, String geneName ) throws IOException {

        Gene g = this.geneService.findByNCBIId( new Integer( geneId ) );

        // no gene found but we are on a test database so its fine
        if ( g == null && this.prodDatabase == false ) {
            return 1;
        }
        // we found a gene
        if ( g != null ) {
            if ( !g.getOfficialSymbol().equalsIgnoreCase( geneName ) ) {

                writeWarning( "Different Gene name found: file=" + geneName + "      Gene name in Gemma="
                        + g.getOfficialSymbol() );
            }

            if ( !g.getTaxon().getCommonName().equals( "human" ) && !g.getTaxon().getCommonName().equals( "mouse" )
                    && !g.getTaxon().getCommonName().equals( "rat" ) && !g.getTaxon().getCommonName().equals( "fly" )
                    && !g.getTaxon().getCommonName().equals( "worm" )
                    && !g.getTaxon().getCommonName().equals( "zebrafish" ) ) {

                String speciesFound = g.getTaxon().getCommonName();

                // lets try to map it to a human taxon using its symbol
                g = this.geneService.findByOfficialSymbol( geneName, taxonService.findByCommonName( "human" ) );

                if ( g != null ) {
                    writeWarning( "We found species: " + speciesFound + " on geneId: " + geneId
                            + " and changed to it to the human symbol: " + geneName );
                } else {
                    throw new EntityNotFoundException( "The geneId: " + geneId + " using species: " + speciesFound
                            + " exist but couldnt be map to its human symbol using: " + geneName
                            + ", this evidence wont be imported" );
                }
            }
        } else {
            // lets try to map it to a human taxon using its symbol
            g = this.geneService.findByOfficialSymbol( geneName, taxonService.findByCommonName( "human" ) );

            if ( g != null ) {
                writeWarning( "We didnt found the geneId: " + geneId + " and changed it to the human symbol: "
                        + geneName );
            } else {
                throw new EntityNotFoundException( "The geneId:" + geneId + " symbol: " + geneName
                        + " was not found in Gemma, this evidence wont be imported" );
            }
        }
        return g.getNcbiGeneId();
    }

    private Set<CharacteristicValueObject> experiementTags2Ontology( Set<String> values, String category,
            String categoryUri, AbstractOntologyService ontologyUsed ) throws IOException {

        Set<CharacteristicValueObject> experimentTags = new HashSet<CharacteristicValueObject>();

        for ( String term : values ) {

            String valueUri = "";

            if ( ontologyUsed != null ) {
                Collection<OntologyTerm> ontologyTerms = ontologyUsed.findTerm( term );
                OntologyTerm ot = findExactTerm( ontologyTerms, term );

                if ( ot != null ) {
                    valueUri = ot.getUri();
                }
            }

            CharacteristicValueObject c = new CharacteristicValueObject( term, category, valueUri, categoryUri );
            experimentTags.add( c );
        }
        return experimentTags;
    }

    // sometimes we dont have the gene nbci, so we use taxon and gene symbol to find the correct gene
    private Integer findGeneId( String officialSymbol ) throws IOException {

        Collection<Gene> genes = this.geneService.findByOfficialSymbol( officialSymbol );

        Collection<Gene> genesWithTaxon = new HashSet<Gene>();

        for ( Gene gene : genes ) {

            if ( gene.getTaxon().getCommonName().equalsIgnoreCase( this.geneNcbiIdMissingUsingTaxon ) ) {
                if ( gene.getNcbiGeneId() != null ) {
                    genesWithTaxon.add( gene );
                }
            }
        }

        if ( genesWithTaxon.isEmpty() ) {
            if ( this.prodDatabase ) {

                Integer geneNCBi = checkForSymbolChange( officialSymbol );

                if ( geneNCBi != null ) {
                    return geneNCBi;
                }

                writeError( "Gene not found using symbol: " + officialSymbol + "   and taxon: "
                        + this.geneNcbiIdMissingUsingTaxon );

                return -1;
            }
            return 1;
        }

        // too many results found, to check why
        if ( genesWithTaxon.size() >= 2 ) {

            Gene g = treatGemmaMultipleGeneSpeacialCases( officialSymbol, genesWithTaxon );

            if ( g != null ) {
                return g.getNcbiGeneId();
            }

            writeError( "Found more than 1 gene using Symbol: " + officialSymbol + "   and taxon: "
                    + this.geneNcbiIdMissingUsingTaxon );

            for ( Gene geneWithTaxon : genesWithTaxon ) {
                writeError( "Gene NCBI: " + geneWithTaxon.getNcbiId() );
            }
        }

        return genesWithTaxon.iterator().next().getNcbiGeneId();
    }

    // when we have more than 1 choice, which one to choose, some hard coded rules so we dont redo them each time
    private Gene treatGemmaMultipleGeneSpeacialCases( String officialSymbol, Collection<Gene> genesFound ) {

        Gene theChosenGene = null;

        // human exceptions
        if ( this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "human" ) ) {

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
        } else if ( this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "rat" ) ) {

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
        } else if ( this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "mouse" ) ) {
            // H2-Ea-ps => 100504404
            if ( officialSymbol.equalsIgnoreCase( "H2-Ea-ps" ) ) {
                theChosenGene = findCorrectGene( "100504404", genesFound );
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
    private Integer checkForSymbolChange( String officialSymbol ) throws IOException {

        String newOfficialSymbol = null;

        if ( this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "human" ) ) {

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
            }
        } else if ( this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "rat" ) ) {

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
            }
        }

        if ( newOfficialSymbol != null ) {
            return findGeneId( newOfficialSymbol );
        }

        return null;

    }

    /**
     * hard coded rules to set scores depending on the type of the database
     */
    private void setScoreDependingOnExternalSource( String externalDatabaseName, EvidenceValueObject evidence ) {
        // OMIM got special character in description to find score
        if ( externalDatabaseName.equalsIgnoreCase( "OMIM" ) ) {

            String description = evidence.getDescription();

            if ( description.indexOf( "{" ) != -1 && description.indexOf( "}" ) != -1 ) {
                evidence.getScoreValueObject().setStrength( new Double( 0.6 ) );
            } else if ( description.indexOf( "[" ) != -1 && description.indexOf( "]" ) != -1 ) {
                evidence.getScoreValueObject().setStrength( new Double( 0.4 ) );
            } else if ( description.indexOf( "{?" ) != -1 && description.indexOf( "}" ) != -1 ) {
                evidence.getScoreValueObject().setStrength( new Double( 0.4 ) );
            } else if ( description.indexOf( "?" ) != -1 ) {
                evidence.getScoreValueObject().setStrength( new Double( 0.2 ) );
            } else {
                evidence.getScoreValueObject().setStrength( new Double( 0.8 ) );
            }
        }

        // RGD we use the taxon and the evidence code
        else if ( externalDatabaseName.equalsIgnoreCase( "RGD" ) ) {

            if ( this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "human" ) ) {

                String evidenceCode = evidence.getEvidenceCode();

                if ( evidenceCode.equalsIgnoreCase( "TAS" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.8 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "IEP" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.4 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "IGI" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.4 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "IED" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.4 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "IAGP" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.4 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "QTM" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.4 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "IPM" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.2 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "IMP" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.2 ) );
                } else if ( evidenceCode.equalsIgnoreCase( "IDA" ) ) {
                    evidence.getScoreValueObject().setStrength( new Double( 0.2 ) );
                }

            } else if ( this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "rat" )
                    || this.geneNcbiIdMissingUsingTaxon.equalsIgnoreCase( "mouse" ) ) {
                evidence.getScoreValueObject().setStrength( new Double( 0.2 ) );
            }
        }
        // for SFARI it is set into an other program
        // TODO move SFARIImporter to ExternalDatabaseImporter
        else if ( externalDatabaseName.equalsIgnoreCase( "SFARI" ) ) {
            return;
        } else if ( externalDatabaseName.equalsIgnoreCase( "CTD" ) ) {
            evidence.getScoreValueObject().setStrength( new Double( 0.2 ) );
        } else if ( externalDatabaseName.equalsIgnoreCase( "MK4MDD" )
                || externalDatabaseName.equalsIgnoreCase( "BDgene" ) ) {
            return;
        }

        // no score set ?
        else if ( evidence.getScoreValueObject().getStrength() == null ) {
            writeError( "no score found for a evidence using Symbol: " + evidence.getGeneOfficialSymbol()
                    + "   and taxon: " + this.geneNcbiIdMissingUsingTaxon );
        }
    }

    /**
     * once we imported some evidence in Neurocarta, we want to copy a copy of what was imported and when, those files
     * are committed in Gemma, so its possible to see over time all that was imported
     */
    private void createImportLog( EvidenceValueObject evidenceValueObject ) {

        // default
        String externalDatabaseName = "MANUAL_CURATION";

        // name the file by its external database name
        if ( evidenceValueObject.getEvidenceSource() != null
                && evidenceValueObject.getEvidenceSource().getExternalDatabase() != null ) {
            externalDatabaseName = evidenceValueObject.getEvidenceSource().getExternalDatabase().getName();
        }

        String keepCopyOfImportedFile = externalDatabaseName + "_" + getTodayDate() + ".tsv";

        // move the file
        File mvFile = new File( inputFile );
        mvFile.renameTo( new File( WRITE_FOLDER + File.separator + keepCopyOfImportedFile ) );
    }

    private String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH:mm" );
        Calendar cal = Calendar.getInstance();
        return dateFormat.format( cal.getTime() );
    }

}
