package ubic.gemma.association.phenotype.fileUpload;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
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

public class EvidenceImporterCLI extends EvidenceImporterAbstractCLI {

    // specify what is the name of the imported file
    private static String fileName = "OMIM.tsv";

    // configuration here can replace if needed to use the command line
    private static String[] initArguments() {

        String[] args = new String[12];
        // user
        args[0] = "-u";
        args[1] = "administrator";
        // password
        args[2] = "-p";
        args[3] = "administrator";
        // file
        args[4] = "-f";
        args[5] = "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/FilesToImport/" + fileName;
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
            // to pass args by the command line dont use the initArguments method
            // Exception ex = p.doWork( args );

            // *** Hardcoded Arguments
            Exception ex = evidenceImporterCLI.doWork( initArguments() );

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

            FileWriter fstream = new FileWriter(
                    "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/FilesToImport/log" );
            this.logger = new BufferedWriter( fstream );

            System.out.println( "File: " + this.inputFile );
            System.out.println( "Create in Database: " + this.createInDatabase );
            if ( this.prodDatabase ) {
                System.out.println( "Connection read or write to Production Database" );
            } else {
                System.out.println( "Using a test database" );
            }

            this.br = new BufferedReader( new FileReader( this.inputFile ) );

            String typeOfEvidence = findTypeOfEvidence();

            // take the file received and create the real objects from it
            Collection<EvidenceValueObject> evidenceValueObjects = file2Objects( typeOfEvidence );

            // make sure all pubmed exists

            if ( !this.errorMessage.isEmpty() ) {

                this.writeAllExceptions();

                this.logger.close();
                throw new Exception( "check logs" );
            }

            if ( !this.warningMessage.isEmpty() ) {
                System.out.println( this.warningMessage );
            }

            if ( this.createInDatabase ) {
                int i = 1;

                for ( EvidenceValueObject e : evidenceValueObjects ) {
                    try {
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

                    System.out.println( "created evidence " + i );
                    i++;
                }
            }

            this.logger.close();

            System.out.println( "Import of evidence is finish" );
            // if created on production write the logs
            if ( this.prodDatabase && this.createInDatabase ) {

                createImportLog();
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

            System.out.println( "Reading evidence: " + i++ );

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

    private void populateCommonFields( EvidenceValueObject evidence, String[] tokens ) throws IOException {

        boolean isNegativeEvidence = false;

        String geneSymbol = tokens[this.mapColumns.get( "GeneSymbol" )].trim();
        String geneID = tokens[this.mapColumns.get( "GeneId" )].trim();

        String evidenceCode = tokens[this.mapColumns.get( "EvidenceCode" )].trim();

        checkEvidenceCodeExits( evidenceCode );

        String description = tokens[this.mapColumns.get( "Comments" )].trim();

        if ( tokens[this.mapColumns.get( "IsNegative" )].trim().equals( "1" ) ) {
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

            String score = tokens[this.mapColumns.get( "Score" )].trim();
            String scoreName = tokens[this.mapColumns.get( "ScoreType" )].trim();
            String strength = tokens[this.mapColumns.get( "Strength" )].trim();

            // score
            evidence.getScoreValueObject().setScoreValue( score );
            evidence.getScoreValueObject().setScoreName( scoreName );
            evidence.getScoreValueObject().setStrength( new Double( strength ) );
        } else if ( !externalDatabaseName.equalsIgnoreCase( "" ) ) {
            setScoreDependingOnExternalSource( externalDatabaseName, evidence );
        }
    }

    private GenericEvidenceValueObject convertFileLine2GenericValueObjects( String[] tokens ) throws IOException {

        GenericEvidenceValueObject evidence = new GenericEvidenceValueObject();

        populateCommonFields( evidence, tokens );

        return evidence;
    }

    private LiteratureEvidenceValueObject convertFileLine2LiteratureValueObjects( String[] tokens ) throws IOException {

        LiteratureEvidenceValueObject evidence = new LiteratureEvidenceValueObject();
        populateCommonFields( evidence, tokens );

        String primaryReferencePubmed = tokens[this.mapColumns.get( "PrimaryPubMed" )].trim();

        CitationValueObject citationValueObject = new CitationValueObject();
        citationValueObject.setPubmedAccession( primaryReferencePubmed );
        evidence.setCitationValueObject( citationValueObject );

        return evidence;
    }

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

    private String phenotype2Ontology( String phenotypeToSearch ) throws IOException {

        OntologyTerm ot = null;

        // we got an uri, search by uri
        if ( phenotypeToSearch.indexOf( "http://purl." ) != -1 ) {
            System.out.println( "Found an URI: " + phenotypeToSearch );
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

                // search mamalian
                ontologyTerms = this.mammalianPhenotypeOntologyService.findTerm( phenotypeToSearch );
                ot = findExactTerm( ontologyTerms, phenotypeToSearch );
            }
        }

        if ( ot == null ) {
            writeError( "phenotype not found in disease, hp and mp Ontology : " + phenotypeToSearch );
            return null;
        }

        if ( ot.isTermObsolete() ) {
            writeError( "TERM IS OBSOLETE: " + ot.getLabel() );
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

        // we dont import those geneID, species not in gemma
        if ( geneId.equalsIgnoreCase( "100195119" ) || geneId.equalsIgnoreCase( "100305177" )
                || geneId.equalsIgnoreCase( "1100195758" ) || geneId.equalsIgnoreCase( "100049380" )
                || geneId.equalsIgnoreCase( "100125427" ) || geneId.equalsIgnoreCase( "100156830" )
                || geneId.equalsIgnoreCase( "100328933" ) || geneId.equalsIgnoreCase( "100400516" )
                || geneId.equalsIgnoreCase( "380482" ) || geneId.equalsIgnoreCase( "396058" )
                || geneId.equalsIgnoreCase( "414399" ) || geneId.equalsIgnoreCase( "445693" )
                || geneId.equalsIgnoreCase( "480491" ) || geneId.equalsIgnoreCase( "595061" )
                || geneId.equalsIgnoreCase( "697193" ) || geneId.equalsIgnoreCase( "724036" )
                || geneId.equalsIgnoreCase( "443231" )

        ) {
            throw new EntityNotFoundException( "this gene Id is an exception and the line wont be imported: " + geneId );
        }

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

                writeWarning( "Strange species Found !!! : " + geneId + "    " + geneName + "    "
                        + g.getTaxon().getCommonName() );
            }

            return g.getNcbiGeneId();
        }

        // this should never happen if not there is a problem
        writeError( "Gene not found in Gemma: " + geneId + "   " + geneName );

        return -1;
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

    // when we have more than 1 choice, which one to choose, some hard coded rules
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

    // used when nothing was found with symbol, some special case to change symbol
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

    // hard coded rules to set scores depending on the type of the database
    private void setScoreDependingOnExternalSource( String externalDatabaseName, EvidenceValueObject evidence ) {

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
        // set in a specific way using an other file
        else if ( externalDatabaseName.equalsIgnoreCase( "SFARI" ) ) {
            return;
        } else if ( externalDatabaseName.equalsIgnoreCase( "CTD" ) ) {
            evidence.getScoreValueObject().setStrength( new Double( 0.2 ) );
        }

        // no score set ?
        else if ( evidence.getScoreValueObject().getStrength() == null ) {
            writeError( "no score found for a evidence using Symbol: " + evidence.getGeneOfficialSymbol()
                    + "   and taxon: " + this.geneNcbiIdMissingUsingTaxon );
        }
    }

    private void createImportLog() throws IOException {

        Calendar c = Calendar.getInstance();
        Date d1 = c.getTime();
        PrintWriter out = new PrintWriter( new BufferedWriter( new FileWriter(
                "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/fileImported/ImportTraceLog",
                true ) ) );
        out.println( "File: " + d1 + "_" + EvidenceImporterCLI.fileName );
        out.close();

        // move the file
        File mvFile = new File( inputFile );
        mvFile.renameTo( new File(
                "./gemma-core/src/main/java/ubic/gemma/association/phenotype/fileUpload/fileImported/" + d1 + "_"
                        + EvidenceImporterCLI.fileName ) );

        System.out.println( inputFile + "" );
    }

}
