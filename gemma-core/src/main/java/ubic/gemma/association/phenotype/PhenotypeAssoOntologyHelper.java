package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.basecode.ontology.providers.DiseaseOntologyService;
import ubic.basecode.ontology.providers.HumanPhenotypeOntologyService;
import ubic.basecode.ontology.providers.MammalianPhenotypeOntologyService;
import ubic.gemma.association.phenotype.PhenotypeExceptions.EntityNotFoundException;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;

public class PhenotypeAssoOntologyHelper {

    private OntologyService ontologyService = null;
    private DiseaseOntologyService diseaseOntologyService = null;
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;

    /** used to set the Ontology terms */
    public PhenotypeAssoOntologyHelper( OntologyService ontologyService ) throws Exception {
        this.ontologyService = ontologyService;
        this.diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();
    }

    /** search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO */
    public Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) {

        HashMap<String, OntologyTerm> uniqueValueTerm = new HashMap<String, OntologyTerm>();

        Collection<OntologyTerm> ontologyTermDisease = this.diseaseOntologyService.findTerm( searchQuery );

        for ( OntologyTerm ontologyTerm : ontologyTermDisease ) {
            uniqueValueTerm.put( ontologyTerm.getLabel().toLowerCase(), ontologyTerm );
        }

        Collection<OntologyTerm> ontologyTermMammalianPhenotype = this.mammalianPhenotypeOntologyService
                .findTerm( searchQuery );

        for ( OntologyTerm ontologyTerm : ontologyTermMammalianPhenotype ) {
            if ( uniqueValueTerm.get( ontologyTerm.getLabel().toLowerCase() ) == null ) {
                uniqueValueTerm.put( ontologyTerm.getLabel(), ontologyTerm );
            }
        }

        Collection<OntologyTerm> ontologyTermHumanPhenotype = this.humanPhenotypeOntologyService.findTerm( searchQuery );

        for ( OntologyTerm ontologyTerm : ontologyTermHumanPhenotype ) {
            if ( uniqueValueTerm.get( ontologyTerm.getLabel().toLowerCase() ) == null ) {
                uniqueValueTerm.put( ontologyTerm.getLabel(), ontologyTerm );
            }
        }

        return ontology2CharacteristicValueObject( uniqueValueTerm.values() );
    }

    /** search the disease, hp and mp ontology for OntologyTerm */
    public Collection<OntologyTerm> findValueUriInOntology( String searchQuery ) {

        Collection<OntologyTerm> ontologyFound = new TreeSet<OntologyTerm>();

        // search disease ontology
        ontologyFound.addAll( this.diseaseOntologyService.findTerm( searchQuery ) );

        // search mp ontology
        ontologyFound.addAll( this.mammalianPhenotypeOntologyService.findTerm( searchQuery ) );

        // search hp ontology
        ontologyFound.addAll( this.humanPhenotypeOntologyService.findTerm( searchQuery ) );

        return ontologyFound;
    }

    /** Giving some Ontology terms return all valueUri of Ontology Terms + children */
    public Set<String> findAllChildrenAndParent( Collection<OntologyTerm> ontologyTerms ) {

        Set<String> phenotypesFoundAndChildren = new HashSet<String>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {
            // add the parent term found
            phenotypesFoundAndChildren.add( ontologyTerm.getUri() );

            // add all children of the term
            for ( OntologyTerm ontologyTermChildren : ontologyTerm.getChildren( false ) ) {
                phenotypesFoundAndChildren.add( ontologyTermChildren.getUri() );
            }
        }
        return phenotypesFoundAndChildren;
    }

    /** For a valueUri return the OntologyTerm found */
    public OntologyTerm findOntologyTermByUri( String valueUri ) {

        OntologyTerm ontologyTerm = this.diseaseOntologyService.getTerm( valueUri );

        if ( ontologyTerm == null ) {
            ontologyTerm = this.mammalianPhenotypeOntologyService.getTerm( valueUri );
        }
        if ( ontologyTerm == null ) {
            ontologyTerm = this.humanPhenotypeOntologyService.getTerm( valueUri );
        }
        if ( ontologyTerm == null ) {
            throw new EntityNotFoundException( "Could not locate ontology term with uri: " + valueUri );
        }
        return ontologyTerm;
    }

    /** For a valueUri return the Characteristic (represents a phenotype) */
    public Characteristic valueUri2Characteristic( String valueUri ) {

        OntologyTerm o = findOntologyTermByUri( valueUri );

        VocabCharacteristic myPhenotype = VocabCharacteristic.Factory.newInstance();

        myPhenotype.setValueUri( o.getUri() );
        myPhenotype.setValue( o.getLabel() );
        myPhenotype.setCategory( PhenotypeAssociationConstants.PHENOTYPE );
        myPhenotype.setCategoryUri( PhenotypeAssociationConstants.PHENOTYPE_CATEGORY_URI );

        return myPhenotype;
    }

    /** Ontology term to CharacteristicValueObject */
    private Set<CharacteristicValueObject> ontology2CharacteristicValueObject( Collection<OntologyTerm> ontologyTerms ) {

        Set<CharacteristicValueObject> characteristicsVO = new HashSet<CharacteristicValueObject>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {
            CharacteristicValueObject phenotype = new CharacteristicValueObject( ontologyTerm.getLabel().toLowerCase(),
                    ontologyTerm.getUri() );
            characteristicsVO.add( phenotype );
        }
        return characteristicsVO;
    }

    /** CharacteristicValueObject to Characteristic with no valueUri given */
    public VocabCharacteristic characteristicValueObject2Characteristic(
            CharacteristicValueObject characteristicValueObject ) {

        VocabCharacteristic characteristic = VocabCharacteristic.Factory.newInstance();
        characteristic.setCategory( characteristicValueObject.getCategory() );
        characteristic.setCategoryUri( characteristicValueObject.getCategoryUri() );
        characteristic.setValue( characteristicValueObject.getValue() );

        if ( characteristic.getValueUri() != null && !characteristic.getValueUri().equals( "" ) ) {
            characteristic.setValueUri( characteristicValueObject.getValueUri() );
        } else {

            // format the query for lucene to look for ontology terms with an exact match for the value
            String value = "\"" + StringUtils.join( characteristicValueObject.getValue().trim().split( " " ), " AND " )
                    + "\"";

            Collection<OntologyTerm> ontologyTerms = this.ontologyService.findTerms( value );

            for ( OntologyTerm ontologyTerm : ontologyTerms ) {
                if ( ontologyTerm.getLabel().equalsIgnoreCase( characteristicValueObject.getValue() ) ) {
                    characteristic.setValueUri( ontologyTerm.getUri() );
                    break;
                }
            }
        }
        return characteristic;
    }
}
