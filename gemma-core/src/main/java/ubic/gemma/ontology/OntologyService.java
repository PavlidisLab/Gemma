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
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.ExternalDatabase;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

import com.hp.hpl.jena.rdf.model.ModelMaker;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

/**
 * Has a static method for finding out which ontologies are loaded into the system and a general purpose find method
 * that delegates to the many ontology services
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="ontologyService"
 * @spring.property name="birnLexOntologyService" ref ="birnLexOntologyService"
 * @spring.property name="fmaOntologyService" ref ="fmaOntologyService"
 * @spring.property name="oboDiseaseOntologyService" ref ="oboDiseaseOntologyService"
 * @spring.property name="bioMaterialService" ref ="bioMaterialService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 */
public class OntologyService {

    private static Log log = LogFactory.getLog( OntologyService.class.getName() );

    private BirnLexOntologyService birnLexOntologyService;
    private OBODiseaseOntologyService oboDiseaseOntologyService;
    private FMAOntologyService fmaOntologyService;
    private BioMaterialService bioMaterialService;
    private ExpressionExperimentService eeService;

    /**
     * List the ontologies that are available locally.
     * 
     * @return
     */
    public static Collection<ubic.gemma.ontology.Ontology> listAvailableOntologies() {

        Collection<ubic.gemma.ontology.Ontology> ontologies = new HashSet<ubic.gemma.ontology.Ontology>();
        ModelMaker maker = OntologyLoader.getRDBMaker();
        ExtendedIterator iterator = maker.listModels();
        while ( iterator.hasNext() ) {
            String name = ( String ) iterator.next();
            ExternalDatabase database = OntologyLoader.ontologyAsExternalDatabase( name );
            ubic.gemma.ontology.Ontology o = new ubic.gemma.ontology.Ontology( database );
            ontologies.add( o );
        }
        return ontologies;

    }

    /**
     * Given a search string will look through the birnlex, obo Disease Ontology and FMA Ontology for terms that match
     * the search strin this a lucene backed search
     * 
     * @param search
     * @return
     */
    public Collection<OntologyTerm> findTerm( String search ) {

        Collection<OntologyTerm> terms = new HashSet<OntologyTerm>();
        Collection<OntologyTerm> results;

        results = birnLexOntologyService.findTerm( search );
        if ( results != null ) terms.addAll( results );

        results = oboDiseaseOntologyService.findTerm( search );
        if ( results != null ) terms.addAll( results );

        results = fmaOntologyService.findTerm( search );
        if ( results != null ) terms.addAll( results );

        return terms;
    }

    /**
     * Will persist the give vocab characteristic to each biomaterial id supplied in the list
     * 
     * @param vc
     * @param bioMaterialIdList
     */
    public void saveBioMaterialStatement( VocabCharacteristic vc, Collection<Long> bioMaterialIdList ) {

        log.debug( "Vocab Characteristic: " + vc.getDescription() );
        log.debug( "Biomaterial ID List: " + bioMaterialIdList );

        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( ( Characteristic ) vc );
        Collection<BioMaterial> biomaterials = bioMaterialService.loadMultiple( bioMaterialIdList );

        for ( BioMaterial bioM : biomaterials ) {
            bioM.setCharacteristics( chars );
            bioMaterialService.update( bioM );

        }

    }

    /**
     * Will persist the give vocab characteristic to each expression experiment id supplied in the list
     * 
     * @param vc
     * @param eeIdList
     */
    public void saveExpressionExperimentStatment( VocabCharacteristic vc, Collection<Long> eeIdList ) {

        log.info( "Vocab Characteristic: " + vc.getDescription() );
        log.info( "Expression Experiment ID List: " + eeIdList );

        Set<Characteristic> chars = new HashSet<Characteristic>();
        chars.add( ( Characteristic ) vc );
        Collection<ExpressionExperiment> ees = eeService.loadMultiple( eeIdList );

        for ( ExpressionExperiment ee : ees ) {
            ee.setCharacteristics( chars );
            eeService.update( ee );

        }
    }

    /**
     * @param birnLexOntologyService the birnLexOntologyService to set
     */
    public void setBirnLexOntologyService( BirnLexOntologyService birnLexOntologyService ) {
        this.birnLexOntologyService = birnLexOntologyService;
    }

    /**
     * @param fmaOntologyService the fmaOntologyService to set
     */
    public void setFmaOntologyService( FMAOntologyService fmaOntologyService ) {
        this.fmaOntologyService = fmaOntologyService;
    }

    /**
     * @param oboDiseaseOntologyService the oboDiseaseOntologyService to set
     */
    public void setOboDiseaseOntologyService( OBODiseaseOntologyService oboDiseaseOntologyService ) {
        this.oboDiseaseOntologyService = oboDiseaseOntologyService;
    }

    /**
     * @param bioMaterialService the bioMaterialService to set
     */
    public void setBioMaterialService( BioMaterialService bioMaterialService ) {
        this.bioMaterialService = bioMaterialService;
    }

    /**
     * @param expressionExperimentService
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.eeService = expressionExperimentService;
    }

}
