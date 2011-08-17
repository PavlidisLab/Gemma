package ubic.gemma.association.phenotype.fileUpload;

import java.util.ArrayList;
import java.util.Collection;

import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;

/** This represents one line from the tsv file in a simple object */
public class EvidenceLineInfo {

    // used the mgedOntologyService to get those values (defined structure)
    public final static String DEVELOPMENTAL_STAGE = "DevelopmentalStage";
    public final static String BIOSOURCE = "BioSource";
    public final static String ORGANISM_PART = "OrganismPart";
    public final static String EXPERIMENT_DESIGN = "ExperimentDesign";
    public final static String TREATMENT = "Treatment";
    public final static String EXPERIMENT = "Experiment";
    public final static String PHENOTYPE = "Phenotype";

    public final static String DEVELOPMENTAL_STAGE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#DevelopmentalStage";
    public final static String BIOSOURCE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#BioSource";
    public final static String ORGANISM_PART_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#OrganismPart";
    public final static String EXPERIMENT_DESIGN_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#ExperimentDesign";
    public final static String TREATMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Treatment";
    public final static String EXPERIMENT_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Experiment";
    public final static String PHENOTYPE_ONTOLOGY = "http://mged.sourceforge.net/ontologies/MGEDOntology.owl#Phenotype";

    private String geneID = "";
    private String primaryReferencePubmed = null;
    private String reviewReferencePubmed = null;
    private String evidenceCode = "";
    private String comment = "";
    private String associationType = null;

    // All characteristics taken from file
    private String[] developmentStage = null;
    private String[] bioSource = null;
    private String[] organismPart = null;
    private String[] experimentDesign = null;
    private String[] treatment = null;
    private String[] experimentOBI = null;
    private String[] phenotype = null;

    // What will populate the Evidences
    private Collection<CharacteristicValueObject> experimentCharacteristics = new ArrayList<CharacteristicValueObject>();
    private Collection<CharacteristicValueObject> phenotypes = new ArrayList<CharacteristicValueObject>();

    public EvidenceLineInfo( String line ) {

        String[] tokens = line.split( "\t" );
        geneID = tokens[0].trim();
        primaryReferencePubmed = tokens[1].trim();
        reviewReferencePubmed = tokens[2].trim();
        evidenceCode = tokens[3].trim();
        comment = tokens[4].trim();
        associationType = tokens[5].trim();

        developmentStage = trimArray( tokens[6].split( ";" ) );
        bioSource = trimArray( tokens[7].split( ";" ) );
        organismPart = trimArray( tokens[8].split( ";" ) );
        experimentDesign = trimArray( tokens[9].split( ";" ) );
        treatment = trimArray( tokens[10].split( ";" ) );
        experimentOBI = trimArray( tokens[11].split( ";" ) );
        phenotype = trimArray( tokens[12].split( ";" ) );
    }

    private String[] trimArray( String[] array ) {

        String[] trimmedArray = new String[array.length];

        for ( int i = 0; i < trimmedArray.length; i++ ) {
            trimmedArray[i] = array[i].trim();
        }

        return trimmedArray;
    }

    public String getGeneID() {
        return geneID;
    }

    public void setGeneID( String geneID ) {
        this.geneID = geneID;
    }

    public String getPrimaryReferencePubmed() {
        return primaryReferencePubmed;
    }

    public void setPrimaryReferencePubmed( String primaryReferencePubmed ) {
        this.primaryReferencePubmed = primaryReferencePubmed;
    }

    public String getReviewReferencePubmed() {
        return reviewReferencePubmed;
    }

    public void setReviewReferencePubmed( String reviewReferencePubmed ) {
        this.reviewReferencePubmed = reviewReferencePubmed;
    }

    public String getEvidenceCode() {
        return evidenceCode;
    }

    public void setEvidenceCode( String evidenceCode ) {
        this.evidenceCode = evidenceCode;
    }

    public String getComment() {
        return comment;
    }

    public void setComment( String comment ) {
        this.comment = comment;
    }

    public String[] getPhenotype() {
        return phenotype;
    }

    public void setPhenotype( String[] phenotype ) {
        this.phenotype = phenotype;
    }

    public String getAssociationType() {
        return associationType;
    }

    public void setAssociationType( String associationType ) {
        this.associationType = associationType;
    }

    public String[] getDevelopmentStage() {
        return developmentStage;
    }

    public void setDevelopmentStage( String[] developmentStage ) {
        this.developmentStage = developmentStage;
    }

    public String[] getBioSource() {
        return bioSource;
    }

    public void setBioSource( String[] bioSource ) {
        this.bioSource = bioSource;
    }

    public String[] getOrganismPart() {
        return organismPart;
    }

    public void setOrganismPart( String[] organismPart ) {
        this.organismPart = organismPart;
    }

    public String[] getExperimentDesign() {
        return experimentDesign;
    }

    public void setExperimentDesign( String[] experimentDesign ) {
        this.experimentDesign = experimentDesign;
    }

    public String[] getTreatment() {
        return treatment;
    }

    public void setTreatment( String[] treatment ) {
        this.treatment = treatment;
    }

    public String[] getExperimentOBI() {
        return experimentOBI;
    }

    public void setExperimentOBI( String[] experimentOBI ) {
        this.experimentOBI = experimentOBI;
    }

    public Collection<CharacteristicValueObject> getExperimentCharacteristics() {
        return experimentCharacteristics;
    }

    public void setExperimentCharacteristics( Collection<CharacteristicValueObject> experimentCharacteristics ) {
        this.experimentCharacteristics = experimentCharacteristics;
    }

    public void addExperimentCharacteristic( CharacteristicValueObject experimentCharacteristic ) {
        experimentCharacteristics.add( experimentCharacteristic );
    }

    public Collection<CharacteristicValueObject> getPhenotypes() {
        return phenotypes;
    }

    public void setPhenotypes( Collection<CharacteristicValueObject> phenotypes ) {
        this.phenotypes = phenotypes;
    }

    public void addPhenotype( CharacteristicValueObject phenotype ) {
        phenotypes.add( phenotype );
    }

}
