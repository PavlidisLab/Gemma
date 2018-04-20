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
package ubic.gemma.core.loader.association.phenotype;

import org.apache.commons.lang3.exception.ExceptionUtils;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.AbstractOntologyService;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class used to load evidence into Neurocarta The file used to import the evidence must have at least those columns:
 * (GeneSymbol, GeneId, EvidenceCode, Comments, IsNegative, Phenotypes) The order of the column is not important,
 * EvidenceImporterAbstractCLI contain the naming rules of those colunms
 *
 * @author nicolas
 */
@SuppressWarnings({ "unused", "WeakerAccess" }) // Possible external use
public class EvidenceImporterCLI extends EvidenceImporterAbstractCLI {

    public static void main( String[] args ) {

        EvidenceImporterCLI evidenceImporterCLI = new EvidenceImporterCLI();

        try {
            Exception ex;

            String[] argsToTake;

            if ( args.length == 0 ) {
                argsToTake = EvidenceImporterCLI.initArguments();
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

    // initArgument is only call when no argument is given on the command line, (it make it faster to run it in eclipse)
    private static String[] initArguments() {

        String[] args = new String[8];
        // user
        args[0] = "-u";
        args[1] = "userhere";
        // password
        args[2] = "-p";
        args[3] = "";
        // the path of the file
        args[4] = "-f";
        args[5] = "pathhere";
        // create the evidence in the database, can be set to false for testing
        args[6] = "-c";
        args[7] = "true";

        return args;
    }

    @Override
    public String getCommandName() {
        return "evidenceImport";
    }

    @Override
    protected Exception doWork( String[] args ) {

        Exception err = this.processCommandLine( args );
        if ( err != null )
            return err;

        try {
            this.createWriteFolder();

            FileWriter fstream = new FileWriter(
                    EvidenceImporterAbstractCLI.WRITE_FOLDER + File.separator + "EvidenceImporter.log" );
            this.logFileWriter = new BufferedWriter( fstream );

            AbstractCLI.log.info( "File: " + this.inputFile );
            AbstractCLI.log.info( "Create in Database: " + this.createInDatabase );

            this.br = new BufferedReader( new FileReader( this.inputFile ) );

            String typeOfEvidence = this.findTypeOfEvidence();

            // take the file received and create the real objects from it
            Collection<EvidenceValueObject> evidenceValueObjects = this.file2Objects( typeOfEvidence );

            // make sure all pubmed exists
            if ( !this.errorMessage.isEmpty() ) {
                System.out.println( errorMessage );
                this.writeAllExceptions();

                this.logFileWriter.close();
                throw new Exception( "check logs" );
            }

            if ( !this.warningMessage.isEmpty() ) {
                AbstractCLI.log.info( this.warningMessage );
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
                        if ( ex.getMessage().contains( "pubmed id" ) ) {
                            this.writeError( ex.getMessage() );
                        } else {
                            throw ex;
                        }
                    } catch ( RuntimeException ex1 ) {
                        AbstractCLI.log.error( ExceptionUtils.getStackTrace( ex1 ) );
                        System.out.println( ex1.getMessage() );
                        this.writeError( "RuntimeException trying to make evidence again 1: " + e.getGeneNCBI() );
                        System.out.println( e.toString() );

                        try {
                            // something is wrong with the pubmed api, wait it out
                            AbstractCLI.log.info( "tring again 1: waiting 10 sec" );
                            Thread.sleep( 10000 );
                            // create the evidence in neurocarta
                            this.phenotypeAssociationService.makeEvidence( e );
                        } catch ( RuntimeException ex2 ) {
                            AbstractCLI.log.error( ExceptionUtils.getStackTrace( ex2 ) );
                            this.writeError( "RuntimeException trying to make evidence again 2: " + e.getGeneNCBI() );
                            // something is wrong with the pubmed api, wait it out more 5000 seconds, this often run at
                            // night so make it wait doesn't matter
                            AbstractCLI.log.info( "tring again 2: waiting 100 sec" );
                            Thread.sleep( 100000 );
                            try {
                                this.phenotypeAssociationService.makeEvidence( e );
                            } catch ( RuntimeException ex3 ) {
                                AbstractCLI.log.error( ExceptionUtils.getStackTrace( ex3 ) );
                                this.writeError(
                                        "RuntimeException trying to make evidence again 3: " + e.getGeneNCBI() );
                                // something is wrong with the pubmed api, wait it out more 5000 seconds, this often run
                                // at night so make it wait doesn't matter
                                AbstractCLI.log.info( "tring again 3: waiting 1000 sec" );
                                Thread.sleep( 1000000 );
                                try {
                                    this.phenotypeAssociationService.makeEvidence( e );
                                } catch ( RuntimeException ex4 ) {
                                    AbstractCLI.log.error( ExceptionUtils.getStackTrace( ex4 ) );
                                    this.writeError(
                                            "RuntimeException trying to make evidence again 4: " + e.getGeneNCBI() );
                                    // something is wrong with the pubmed api, wait it out more 4 hours, this often
                                    // run at night so make it wait doesn't matter
                                    AbstractCLI.log.info( "tring again 4: waiting 15000 sec" );
                                    Thread.sleep( 15000000 );
                                    this.phenotypeAssociationService.makeEvidence( e );
                                }
                            }
                        }
                    }
                    AbstractCLI.log.info( "created evidence " + i++ );
                }
            }

            this.logFileWriter.close();

            AbstractCLI.log.info( "Import of evidence is finish" );
            // when we import a file in production we keep a copy of the imported file and keep track of when the file
            // was imported in a log file

            // createImportLog( evidenceValueObjects.iterator().next() );

        } catch ( Exception e ) {
            return e;
        }
        System.exit( -1 );
        return null;
    }

    // special case to change symbol, used when nothing was found with symbol
    private Integer checkForSymbolChange( String officialSymbol, String evidenceTaxon ) throws IOException {

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

        } else if ( evidenceTaxon.equalsIgnoreCase( "rat" ) ) {
            newOfficialSymbol = SymbolChangeAndLoggingAbstract.populateRatSymbol( officialSymbol );
        }

        if ( newOfficialSymbol != null ) {
            return this.findGeneId( newOfficialSymbol, evidenceTaxon );
        }

        return null;
    }

    /**
     * convert for LiteratureEvidenceValueObject
     */
    private EvidenceValueObject convert2LiteratureOrGenereicVO( String[] tokens ) throws IOException {
        EvidenceValueObject evidence;

        String primaryReferencePubmeds = tokens[this.mapColumns.get( "PrimaryPubMeds" )].trim();

        if ( primaryReferencePubmeds.equalsIgnoreCase( "" ) ) {
            evidence = new GenericEvidenceValueObject( -1L );
        } else {
            evidence = new LiteratureEvidenceValueObject( -1L );
        }

        this.populateCommonFields( evidence, tokens );

        return evidence;
    }

    /**
     * convert for ExperimentalEvidenceValueObject
     */
    private ExperimentalEvidenceValueObject convertFileLine2ExperimentalValueObjects( String[] tokens )
            throws IOException {

        ExperimentalEvidenceValueObject evidence = new ExperimentalEvidenceValueObject( -1L );
        this.populateCommonFields( evidence, tokens );

        String reviewReferencePubmed = tokens[this.mapColumns.get( "OtherPubMed" )].trim();

        Set<String> relevantPublicationsPubmed = new HashSet<>();
        if ( !reviewReferencePubmed.equals( "" ) ) {

            relevantPublicationsPubmed.add( reviewReferencePubmed );
        }

        for ( String relevantPubMedID : relevantPublicationsPubmed ) {
            CitationValueObject relevantPublicationValueObject = new CitationValueObject();
            relevantPublicationValueObject.setPubmedAccession( relevantPubMedID );
            evidence.getPhenotypeAssPubVO()
                    .add( PhenotypeAssPubValueObject.createRelevantPublication( relevantPubMedID ) );
        }

        Set<String> developmentStage = this
                .trimArray( tokens[this.mapColumns.get( "DevelopmentalStage" )].split( ";" ) );
        Set<String> bioSource = this.trimArray( tokens[this.mapColumns.get( "BioSource" )].split( ";" ) );
        Set<String> organismPart = this.trimArray( tokens[this.mapColumns.get( "OrganismPart" )].split( ";" ) );
        Set<String> experimentDesign = this.trimArray( tokens[this.mapColumns.get( "ExperimentDesign" )].split( ";" ) );
        Set<String> treatment = this.trimArray( tokens[this.mapColumns.get( "Treatment" )].split( ";" ) );
        Set<String> experimentOBI = this.trimArray( tokens[this.mapColumns.get( "Experiment" )].split( ";" ) );

        Set<CharacteristicValueObject> experimentTags = new HashSet<>();

        experimentTags.addAll( this.experiementTags2Ontology( developmentStage, this.DEVELOPMENTAL_STAGE,
                this.DEVELOPMENTAL_STAGE_ONTOLOGY, this.nifstdOntologyService ) );
        experimentTags.addAll(
                this.experiementTags2Ontology( bioSource, this.BIOSOURCE_ONTOLOGY, this.BIOSOURCE_ONTOLOGY, null ) );
        experimentTags.addAll(
                this.experiementTags2Ontology( organismPart, this.ORGANISM_PART, this.ORGANISM_PART_ONTOLOGY,
                        this.fmaOntologyService ) );
        experimentTags.addAll( this.experiementTags2Ontology( experimentDesign, this.EXPERIMENT_DESIGN,
                this.EXPERIMENT_DESIGN_ONTOLOGY, this.obiService ) );
        experimentTags
                .addAll( this.experiementTags2Ontology( treatment, this.TREATMENT, this.TREATMENT_ONTOLOGY, null ) );
        experimentTags.addAll( this.experiementTags2Ontology( experimentOBI, this.EXPERIMENT, this.EXPERIMENT_ONTOLOGY,
                this.obiService ) );

        evidence.setExperimentCharacteristics( experimentTags );

        return evidence;
    }

    /**
     * once we imported some evidence in Neurocarta, we want to copy a copy of what was imported and when, those files
     * are committed in Gemma, so its possible to see over time all that was imported
     */
    @SuppressWarnings("unused")
    private void createImportLog( EvidenceValueObject evidenceValueObject ) {

        // default
        String externalDatabaseName = "MANUAL_CURATION";

        // name the file by its external database name
        if ( evidenceValueObject.getEvidenceSource() != null
                && evidenceValueObject.getEvidenceSource().getExternalDatabase() != null ) {
            externalDatabaseName = evidenceValueObject.getEvidenceSource().getExternalDatabase().getName();
        }

        String keepCopyOfImportedFile = externalDatabaseName + "_" + this.getTodayDate() + ".tsv";

        // move the file
        File mvFile = new File( inputFile );
        EntityUtils.renameFile( mvFile,
                new File( EvidenceImporterAbstractCLI.WRITE_FOLDER + File.separator + keepCopyOfImportedFile ) );
    }

    private Set<CharacteristicValueObject> experiementTags2Ontology( Set<String> values, String category,
            String categoryUri, AbstractOntologyService ontologyUsed ) {

        Set<CharacteristicValueObject> experimentTags = new HashSet<>();

        for ( String term : values ) {

            String valueUri = "";

            if ( ontologyUsed != null ) {
                Collection<OntologyTerm> ontologyTerms = ontologyUsed.findTerm( term );
                OntologyTerm ot = this.findExactTerm( ontologyTerms, term );

                if ( ot != null ) {
                    valueUri = ot.getUri();
                }
            }

            CharacteristicValueObject c = new CharacteristicValueObject( -1L, term, category, valueUri, categoryUri );
            experimentTags.add( c );
        }
        return experimentTags;
    }

    /**
     * Change the file received into an entity that can save in the database
     */
    private Collection<EvidenceValueObject> file2Objects( String evidenceType ) throws Exception {

        Collection<EvidenceValueObject> evidenceValueObjects = new ArrayList<>();
        String line;
        int i = 1;

        // for each line of the file
        while ( ( line = this.br.readLine() ) != null ) {

            String[] tokens = line.split( "\t" );

            AbstractCLI.log.info( "Reading evidence: " + i++ );

            try {
                switch ( evidenceType ) {
                    case LITERATURE_EVIDENCE:
                        evidenceValueObjects.add( this.convert2LiteratureOrGenereicVO( tokens ) );
                        break;
                    case EXPERIMENTAL_EVIDENCE:
                        evidenceValueObjects.add( this.convertFileLine2ExperimentalValueObjects( tokens ) );
                        break;
                    default:
                        throw new Exception( "unknown type" );
                }
            } catch ( EntityNotFoundException e ) {
                this.writeWarning( e.getMessage() );
            }
        }

        this.br.close();

        return evidenceValueObjects;
    }

    private Gene findCorrectGene( String ncbiId, Collection<Gene> genesFound ) {

        for ( Gene gene : genesFound ) {

            if ( gene.getNcbiGeneId().toString().equalsIgnoreCase( ncbiId ) ) {
                return gene;
            }
        }
        return null;
    }

    // sometimes we dont have the gene nbci, so we use taxon and gene symbol to find the correct gene
    private Integer findGeneId( String officialSymbol, String evidenceTaxon ) throws IOException {

        Collection<Gene> genes = this.geneService.findByOfficialSymbol( officialSymbol );

        Collection<Gene> genesWithTaxon = new HashSet<>();

        for ( Gene gene : genes ) {

            if ( gene.getTaxon().getCommonName().equalsIgnoreCase( evidenceTaxon ) ) {
                if ( gene.getNcbiGeneId() != null ) {
                    genesWithTaxon.add( gene );
                }
            }
        }

        if ( genesWithTaxon.isEmpty() ) {

            Integer geneNCBi = this.checkForSymbolChange( officialSymbol, evidenceTaxon );

            if ( geneNCBi != null ) {
                return geneNCBi;
            }

            this.writeError( "Gene not found using symbol: " + officialSymbol + "   and taxon: " + evidenceTaxon );

            return -1;

        }

        // too many results found, to check why
        if ( genesWithTaxon.size() >= 2 ) {

            Gene g = this.treatGemmaMultipleGeneSpeacialCases( officialSymbol, genesWithTaxon, evidenceTaxon );

            if ( g != null ) {
                return g.getNcbiGeneId();
            }

            String error = "Found more than 1 gene using Symbol: " + officialSymbol + "   and taxon: " + evidenceTaxon;

            for ( Gene geneWithTaxon : genesWithTaxon ) {
                this.writeError( error + "\tGene NCBI: " + geneWithTaxon.getNcbiGeneId() );
            }
        }

        return genesWithTaxon.iterator().next().getNcbiGeneId();
    }

    private String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH:mm" );
        Calendar cal = Calendar.getInstance();
        return dateFormat.format( cal.getTime() );
    }

    // value or valueUri given changed to valueUri (even if valueUri is given in file we need to check)
    private String phenotype2Ontology( String phenotypeToSearch ) {

        OntologyTerm ot;

        // we got an uri, search by uri
        if ( phenotypeToSearch.contains( "http://purl." ) ) {
            AbstractCLI.log.info( "Found an URI: " + phenotypeToSearch );
            ot = this.diseaseOntologyService.getTerm( phenotypeToSearch );

            if ( ot == null ) {
                ot = this.humanPhenotypeOntologyService.getTerm( phenotypeToSearch );
            }
            if ( ot == null ) {
                this.mammalianPhenotypeOntologyService.getTerm( phenotypeToSearch );
            }
        }
        // value found
        else {
            // search disease
            Collection<OntologyTerm> ontologyTerms = this.diseaseOntologyService.findTerm( phenotypeToSearch );

            ot = this.findExactTerm( ontologyTerms, phenotypeToSearch );

            if ( ot == null ) {
                // search hp
                ontologyTerms = this.humanPhenotypeOntologyService.findTerm( phenotypeToSearch );
                ot = this.findExactTerm( ontologyTerms, phenotypeToSearch );

            }
            if ( ot == null ) {
                // search mammalian
                ontologyTerms = this.mammalianPhenotypeOntologyService.findTerm( phenotypeToSearch );
                this.findExactTerm( ontologyTerms, phenotypeToSearch );
            }
        }

        return phenotypeToSearch;
    }

    /**
     * File to valueObject conversion, populate the basics
     */
    private void populateCommonFields( EvidenceValueObject evidence, String[] tokens ) throws IOException {

        boolean isNegativeEvidence = false;

        String primaryReferencePubmeds = tokens[this.mapColumns.get( "PrimaryPubMeds" )].trim();

        if ( !primaryReferencePubmeds.equalsIgnoreCase( "" ) ) {
            String[] tokensPrimary = primaryReferencePubmeds.split( ";" );

            for ( String primary : tokensPrimary ) {
                //noinspection unchecked // There is no reason for this method to have the return Set type erased
                Set<PhenotypeAssPubValueObject> set = evidence.getPhenotypeAssPubVO();
                set.add( PhenotypeAssPubValueObject.createPrimaryPublication( primary.trim() ) );
            }
        }

        String geneSymbol = tokens[this.mapColumns.get( "GeneSymbol" )].trim();
        String geneNcbiId = "";

        if ( this.mapColumns.get( "GeneId" ) != null ) {
            geneNcbiId = tokens[this.mapColumns.get( "GeneId" )].trim();
        }

        String evidenceCode = tokens[this.mapColumns.get( "EvidenceCode" )].trim();

        this.checkEvidenceCodeExits( evidenceCode );

        String description = tokens[this.mapColumns.get( "Comments" )].trim();

        if ( !StringUtil.containsValidCharacter( description ) ) {
            this.writeError( description
                    + " Ivalid character found (if character is ok add it to StringUtil.containsValidCharacter)" );
        }

        if ( this.mapColumns.get( "IsNegative" ) != null && this.mapColumns.get( "IsNegative" ) < tokens.length
                && tokens[this.mapColumns.get( "IsNegative" )].trim().equals( "1" ) ) {
            isNegativeEvidence = true;
        }

        String externalDatabaseName = tokens[this.mapColumns.get( "ExternalDatabase" )].trim();

        String databaseID = tokens[this.mapColumns.get( "DatabaseLink" )].trim();

        String originalPhenotype = tokens[this.mapColumns.get( "OriginalPhenotype" )].trim();
        System.out.println( "original phenotype is: " + originalPhenotype );
        String phenotypeMapping = tokens[this.mapColumns.get( "PhenotypeMapping" )].trim();

        this.verifyMappingType( phenotypeMapping );

        Set<String> phenotypeFromArray = this.trimArray( tokens[this.mapColumns.get( "Phenotypes" )].split( ";" ) );

        Gene g = this.verifyGeneIdExist( geneNcbiId, geneSymbol );

        SortedSet<CharacteristicValueObject> phenotypes = this.toValuesUri( phenotypeFromArray );

        evidence.setDescription( description );
        evidence.setEvidenceCode( evidenceCode );
        evidence.setEvidenceSource( this.makeEvidenceSource( databaseID, externalDatabaseName ) );
        evidence.setGeneNCBI( new Integer( geneNcbiId ) );
        evidence.setPhenotypes( phenotypes );
        evidence.setIsNegativeEvidence( isNegativeEvidence );
        evidence.setOriginalPhenotype( originalPhenotype );
        evidence.setPhenotypeMapping( phenotypeMapping );
        evidence.setRelationship( "gene-disease association" );
        if ( externalDatabaseName.equalsIgnoreCase( "CTD" ) ) {
            if ( description.contains( "marker/mechanism" ) )
                evidence.setRelationship( "biomarker" );
            if ( description.contains( "therapeutic" ) )
                evidence.setRelationship( "therapeutic target" );
        }

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
            this.setScoreDependingOnExternalSource( externalDatabaseName, evidence, g.getTaxon().getCommonName() );
        }
    }

    /**
     * hard coded rules to set scores depending on the type of the database
     */
    @SuppressWarnings("StatementWithEmptyBody") // Better readability
    private void setScoreDependingOnExternalSource( String externalDatabaseName, EvidenceValueObject evidence,
            String evidenceTaxon ) {
        // OMIM got special character in description to find score
        if ( externalDatabaseName.equalsIgnoreCase( "OMIM" ) ) {

            String description = evidence.getDescription();

            if ( description.contains( "{" ) && description.contains( "}" ) ) {
                evidence.getScoreValueObject().setStrength( 0.6 );
            } else if ( description.contains( "[" ) && description.contains( "]" ) ) {
                evidence.getScoreValueObject().setStrength( 0.4 );
            } else if ( description.contains( "{?" ) && description.contains( "}" ) ) {
                evidence.getScoreValueObject().setStrength( 0.4 );
            } else if ( description.contains( "?" ) ) {
                evidence.getScoreValueObject().setStrength( 0.2 );
            } else {
                evidence.getScoreValueObject().setStrength( 0.8 );
            }
        }

        // RGD we use the taxon and the evidence code
        else if ( externalDatabaseName.equalsIgnoreCase( "RGD" ) ) {

            if ( evidenceTaxon.equalsIgnoreCase( "human" ) ) {

                String evidenceCode = evidence.getEvidenceCode();

                if ( evidenceCode.equalsIgnoreCase( "TAS" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.8 );
                } else if ( evidenceCode.equalsIgnoreCase( "IEP" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.4 );
                    evidence.setRelationship( "altered expression association" );
                } else if ( evidenceCode.equalsIgnoreCase( "IGI" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.4 );
                } else if ( evidenceCode.equalsIgnoreCase( "IED" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.4 );
                } else if ( evidenceCode.equalsIgnoreCase( "IAGP" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.4 );
                    evidence.setRelationship( "genetic association" );
                } else if ( evidenceCode.equalsIgnoreCase( "QTM" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.4 );
                } else if ( evidenceCode.equalsIgnoreCase( "IPM" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.2 );
                    evidence.setRelationship( "genetic association" );
                } else if ( evidenceCode.equalsIgnoreCase( "IMP" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.2 );
                    evidence.setRelationship( "mutation association" );
                } else if ( evidenceCode.equalsIgnoreCase( "IDA" ) ) {
                    evidence.getScoreValueObject().setStrength( 0.2 );
                }

            } else if ( evidenceTaxon.equalsIgnoreCase( "rat" ) || evidenceTaxon.equalsIgnoreCase( "mouse" ) ) {
                evidence.getScoreValueObject().setStrength( 0.2 );
            }
        }
        // for SFARI it is set into an other program
        else if ( externalDatabaseName.equalsIgnoreCase( "SFARI" ) ) {
        } else if ( externalDatabaseName.equalsIgnoreCase( "CTD" ) || externalDatabaseName
                .equalsIgnoreCase( "GWAS_Catalog" ) ) {
            evidence.getScoreValueObject().setStrength( 0.2 );
        } else if ( externalDatabaseName.equalsIgnoreCase( "MK4MDD" ) || externalDatabaseName
                .equalsIgnoreCase( "BDgene" ) || externalDatabaseName.equalsIgnoreCase( "DGA" ) ) {
        }

        // no score set ?
        else if ( evidence.getScoreValueObject().getStrength() == null ) {
            this.writeError( "no score found for a evidence using NCBI: " + evidence.getGeneNCBI() + "   and taxon: "
                    + evidenceTaxon );
        }
    }

    // Change a set of phenotype to a set of CharacteristicValueObject
    private SortedSet<CharacteristicValueObject> toValuesUri( Set<String> phenotypes ) {

        SortedSet<CharacteristicValueObject> characteristicPhenotypes = new TreeSet<>();

        for ( String phenotype : phenotypes ) {

            String valueUri = this.phenotype2Ontology( phenotype );

            if ( valueUri != null ) {
                CharacteristicValueObject c = new CharacteristicValueObject( -1L, valueUri );
                characteristicPhenotypes.add( c );
            }
        }

        return characteristicPhenotypes;
    }

    // when we have more than 1 choice, which one to choose, some hard coded rules so we dont redo them each time
    private Gene treatGemmaMultipleGeneSpeacialCases( String officialSymbol, Collection<Gene> genesFound,
            String evidenceTaxon ) {

        Gene theChosenGene = null;

        // human exceptions
        if ( evidenceTaxon.equalsIgnoreCase( "human" ) ) {

            // HLA-DRB1 => 3123
            if ( officialSymbol.equalsIgnoreCase( "HLA-DRB1" ) ) {
                theChosenGene = this.findCorrectGene( "3123", genesFound );
            }
            // CCR2 => 729230
            else if ( officialSymbol.equalsIgnoreCase( "CCR2" ) ) {
                theChosenGene = this.findCorrectGene( "729230", genesFound );
            }
            // NPC1 => 4864
            else if ( officialSymbol.equalsIgnoreCase( "NPC1" ) ) {
                theChosenGene = this.findCorrectGene( "4864", genesFound );
            }
            // PRG4 => 10216
            else if ( officialSymbol.equalsIgnoreCase( "PRG4" ) ) {
                theChosenGene = this.findCorrectGene( "10216", genesFound );
            }
            // TTC34 => 100287898
            else if ( officialSymbol.equalsIgnoreCase( "TTC34" ) ) {
                theChosenGene = this.findCorrectGene( "100287898", genesFound );
            }
            // DNAH12 => 201625
            else if ( officialSymbol.equalsIgnoreCase( "DNAH12" ) ) {
                theChosenGene = this.findCorrectGene( "201625", genesFound );
            }
            // PSORS1C3 => 100130889
            else if ( officialSymbol.equalsIgnoreCase( "PSORS1C3" ) ) {
                theChosenGene = this.findCorrectGene( "100130889", genesFound );
            }
            // MICA => 100507436
            else if ( officialSymbol.equalsIgnoreCase( "MICA" ) ) {
                theChosenGene = this.findCorrectGene( "100507436", genesFound );
            }

            // MICA => 100507436
            else if ( officialSymbol.equalsIgnoreCase( "MICA" ) ) {
                theChosenGene = this.findCorrectGene( "100507436", genesFound );
            }

            // ADH5P2 => 343296
            else if ( officialSymbol.equalsIgnoreCase( "ADH5P2" ) ) {
                theChosenGene = this.findCorrectGene( "343296", genesFound );
            }

            // RPL15P3 => 653232
            else if ( officialSymbol.equalsIgnoreCase( "RPL15P3" ) ) {
                theChosenGene = this.findCorrectGene( "653232", genesFound );
            }

        } else if ( evidenceTaxon.equalsIgnoreCase( "rat" ) ) {
            theChosenGene = SymbolChangeAndLoggingAbstract.populateRatGene( officialSymbol, genesFound );
        } else if ( evidenceTaxon.equalsIgnoreCase( "mouse" ) ) {
            theChosenGene = SymbolChangeAndLoggingAbstract.populateMouseGene( officialSymbol, genesFound );
        }

        return theChosenGene;
    }

    /**
     * check that all gene exists in Gemma
     */
    private Gene verifyGeneIdExist( String geneId, String geneName ) throws IOException {

        System.out.println( "Problem: gene id " + geneId + " gene name: " + geneName );

        Gene g = this.geneService.findByNCBIId( new Integer( geneId ) );

        // we found a gene
        if ( g != null ) {
            if ( !g.getOfficialSymbol().equalsIgnoreCase( geneName ) ) {

                this.writeWarning( "Different Gene name found: file=" + geneName + "      Gene name in Gemma=" + g
                        .getOfficialSymbol() );
            }

            if ( !g.getTaxon().getCommonName().equals( "human" ) && !g.getTaxon().getCommonName().equals( "mouse" )
                    && !g.getTaxon().getCommonName().equals( "rat" ) && !g.getTaxon().getCommonName().equals( "fly" )
                    && !g.getTaxon().getCommonName().equals( "worm" ) && !g.getTaxon().getCommonName()
                    .equals( "zebrafish" ) ) {

                String speciesFound = g.getTaxon().getCommonName();

                // lets try to map it to a human taxon using its symbol
                g = this.geneService.findByOfficialSymbol( geneName, taxonService.findByCommonName( "human" ) );

                if ( g != null ) {
                    this.writeWarning( "We found species: " + speciesFound + " on geneId: " + geneId
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
                this.writeWarning(
                        "We didnt found the geneId: " + geneId + " and changed it to the human symbol: " + geneName );
            } else {
                throw new EntityNotFoundException( "The geneId:" + geneId + " symbol: " + geneName
                        + " was not found in Gemma, this evidence wont be imported" );
            }
        }
        return g;
    }

    private void verifyMappingType( String phenotypeMapping ) {

        if ( !( phenotypeMapping.equalsIgnoreCase( "Cross Reference" ) || phenotypeMapping.equalsIgnoreCase( "Curated" )
                || phenotypeMapping.equalsIgnoreCase( "Inferred Cross Reference" ) || phenotypeMapping
                .equalsIgnoreCase( "Inferred Curated" ) || phenotypeMapping.isEmpty() ) ) {
            this.writeError( "Unsuported phenotypeMapping: " + phenotypeMapping );
        }

    }

}
