package ubic.gemma.association.phenotype;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

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

    private DiseaseOntologyService diseaseOntologyService = null;
    private MammalianPhenotypeOntologyService mammalianPhenotypeOntologyService = null;
    private HumanPhenotypeOntologyService humanPhenotypeOntologyService = null;
    
    
    /** used to set the Ontology terms */
    public PhenotypeAssoOntologyHelper( OntologyService ontologyService ) throws Exception {
        this.diseaseOntologyService = ontologyService.getDiseaseOntologyService();
        this.mammalianPhenotypeOntologyService = ontologyService.getMammalianPhenotypeOntologyService();
        this.humanPhenotypeOntologyService = ontologyService.getHumanPhenotypeOntologyService();
    }

    /** search the disease,hp and mp ontology for a searchQuery and return an ordered set of CharacteristicVO */
    public Set<CharacteristicValueObject> findPhenotypesInOntology( String searchQuery ) {
        Set<CharacteristicValueObject> allPhenotypesFoundInOntology = new TreeSet<CharacteristicValueObject>();

        // search disease ontology
        allPhenotypesFoundInOntology.addAll( ontology2CharacteristicValueObject(
                this.diseaseOntologyService.findTerm( searchQuery ), PhenotypeAssociationConstants.DISEASE ) );

        // search mp ontology
        allPhenotypesFoundInOntology.addAll( ontology2CharacteristicValueObject(
                this.mammalianPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.MAMMALIAN_PHENOTYPE ) );

        // search hp ontology
        allPhenotypesFoundInOntology.addAll( ontology2CharacteristicValueObject(
                this.humanPhenotypeOntologyService.findTerm( searchQuery ),
                PhenotypeAssociationConstants.HUMAN_PHENOTYPE ) );

        return allPhenotypesFoundInOntology;
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
    private Set<CharacteristicValueObject> ontology2CharacteristicValueObject( Collection<OntologyTerm> ontologyTerms,
            String ontologyUsed ) {

        Set<CharacteristicValueObject> characteristicsVO = new HashSet<CharacteristicValueObject>();

        for ( OntologyTerm ontologyTerm : ontologyTerms ) {

            CharacteristicValueObject phenotype = new CharacteristicValueObject( ontologyTerm.getLabel(),
                    ontologyTerm.getUri() );
            phenotype.setOntologyUsed( ontologyUsed );
            characteristicsVO.add( phenotype );

        }
        return characteristicsVO;
    }

}
