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

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.OntologyService;
import ubic.basecode.ontology.search.OntologySearchException;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.association.phenotype.EntityNotFoundException;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.description.CharacteristicValueObject;
import ubic.gemma.model.common.description.CitationValueObject;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.phenotype.valueObject.*;
import ubic.gemma.persistence.util.EntityUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Class used to load evidence into Phenocarta. The file used to import the evidence must have at least those columns:
 * (GeneSymbol, GeneId, EvidenceCode, Comments, IsNegative, Phenotypes) The order of the column is not important,
 * EvidenceImporterAbstractCLI contain the naming rules of those colunms
 *
 * @author nicolas
 */
@SuppressWarnings({"unused", "WeakerAccess"}) // Possible external use
public class EvidenceImporterCLI extends EvidenceImporterAbstractCLI {

    @Override
    public int executeCommand( String[] args ) {
        String[] argsToTake;

        if ( args.length == 0 ) {
            argsToTake = EvidenceImporterCLI.initArguments();
        } else {
            argsToTake = args;
        }
        return super.executeCommand( argsToTake );
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
    protected void doWork() throws Exception {
        this.createWriteFolder();

        FileWriter fstream = new FileWriter(
                EvidenceImporterAbstractCLI.WRITE_FOLDER + File.separator + "EvidenceImporter.log" );
        this.logFileWriter = new BufferedWriter( fstream );

        AbstractCLI.log.info( "File: " + this.inputFile );
        AbstractCLI.log.info( "Create in Database: " + this.createInDatabase );

        this.br = new BufferedReader( new FileReader( this.inputFile ) );

        String typeOfEvidence = this.findTypeOfEvidence();

        // take the file received and create the real objects from it
        Collection<EvidenceValueObject<?>> evidenceValueObjects = this.file2Objects( typeOfEvidence );

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

            // for each evidence creates it in Phenocarta
            for ( EvidenceValueObject<?> e : evidenceValueObjects ) {
                try {
                    // create the evidence in neurocarta
                    this.phenotypeAssociationManagerService.makeEvidence( e );
                } catch ( EntityNotFoundException ex ) {

                    this.writeError( "went into the exception" );

                    // if a pubmed id was not found dont stop all processes and write to logs
                    if ( ex.getMessage().contains( "pubmed id" ) ) {
                        this.writeError( ex.getMessage() );
                    } else {
                        throw ex;
                    }
                }
                AbstractCLI.log.info( "created evidence " + i++ );
            }
        } else {
            for ( EvidenceValueObject<?> e : evidenceValueObjects ) {
                System.out.println( e );
            }
        }

        this.logFileWriter.close();

        AbstractCLI.log.info( "Import of evidence is finish" );
        // when we import a file in production we keep a copy of the imported file and keep track of when the file
        // was imported in a log file

        // createImportLog( evidenceValueObjects.iterator().next() );
    }

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.PHENOTYPES;
    }

    @Override
    public String getShortDesc() {
        return "Import gene-phenotype information from any of the supported sources (via files created with the appropriate source-specific CLI)";
    }

    /**
     * convert for LiteratureEvidenceValueObject
     */
    private EvidenceValueObject<?> convert2LiteratureOrGenereicVO( String[] tokens ) throws IOException, OntologySearchException {
        EvidenceValueObject<?> evidence;

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
            throws IOException, OntologySearchException {

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

        //        Set<String> developmentStage = this
        //                .trimArray( tokens[this.mapColumns.get( DEVELOPMENTAL_STAGE )].split( ";" ) );
        Set<String> bioSource = this.trimArray( tokens[this.mapColumns.get( BIOSOURCE )].split( ";" ) );
        Set<String> organismPart = this.trimArray( tokens[this.mapColumns.get( ORGANISM_PART )].split( ";" ) );
        Set<String> experimentDesign = this.trimArray( tokens[this.mapColumns.get( EXPERIMENT_DESIGN )].split( ";" ) );
        Set<String> treatment = this.trimArray( tokens[this.mapColumns.get( TREATMENT )].split( ";" ) );
        Set<String> experimentOBI = this.trimArray( tokens[this.mapColumns.get( EXPERIMENT )].split( ";" ) );

        Set<CharacteristicValueObject> experimentTags = new HashSet<>();

        //        experimentTags.addAll( this.experimentTags2Ontology( developmentStage, this.DEVELOPMENTAL_STAGE,
        //                this.DEVELOPMENTAL_STAGE_ONTOLOGY, this.nifstdOntologyService ) );

        experimentTags
                .addAll( this.experimentTags2Ontology( bioSource, this.BIOSOURCE, this.BIOSOURCE_ONTOLOGY, null ) );
        experimentTags.addAll(
                this.experimentTags2Ontology( organismPart, this.ORGANISM_PART, this.ORGANISM_PART_ONTOLOGY,
                        this.uberonOntologyService ) );
        experimentTags.addAll(
                this.experimentTags2Ontology( experimentDesign, this.EXPERIMENT_DESIGN, this.EXPERIMENT_DESIGN_ONTOLOGY,
                        this.obiService ) );
        experimentTags
                .addAll( this.experimentTags2Ontology( treatment, this.TREATMENT, this.TREATMENT_ONTOLOGY, null ) );
        experimentTags.addAll( this.experimentTags2Ontology( experimentOBI, this.EXPERIMENT, this.EXPERIMENT_ONTOLOGY,
                this.obiService ) );

        evidence.setExperimentCharacteristics( experimentTags );

        return evidence;
    }

    /**
     * once we imported some evidence in Phenocarta, we want to copy a copy of what was imported and when, those files
     * are committed in Gemma, so its possible to see over time all that was imported
     */
    @SuppressWarnings("unused")
    private void createImportLog( EvidenceValueObject<?> evidenceValueObject ) {

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

    private Set<CharacteristicValueObject> experimentTags2Ontology( Set<String> values, String category,
                                                                    String categoryUri, OntologyService ontologyUsed ) throws OntologySearchException {

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
    private Collection<EvidenceValueObject<?>> file2Objects( String evidenceType ) throws Exception {

        Collection<EvidenceValueObject<?>> evidenceValueObjects = new ArrayList<>();
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

    private String getTodayDate() {
        DateFormat dateFormat = new SimpleDateFormat( "yyyy-MM-dd_HH:mm", Locale.ENGLISH );
        Calendar cal = Calendar.getInstance();
        return dateFormat.format( cal.getTime() );
    }

    // value or valueUri given changed to valueUri (even if valueUri is given in file we need to check)
    private String phenotype2Ontology( String phenotypeToSearch ) throws OntologySearchException {

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
    private void populateCommonFields( EvidenceValueObject<?> evidence, String[] tokens ) throws IOException, OntologySearchException {

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
    private void setScoreDependingOnExternalSource( String externalDatabaseName, EvidenceValueObject<?> evidence,
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
    private SortedSet<CharacteristicValueObject> toValuesUri( Set<String> phenotypes ) throws OntologySearchException {

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
