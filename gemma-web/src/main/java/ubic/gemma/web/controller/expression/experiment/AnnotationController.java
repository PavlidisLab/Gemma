/*
 * The Gemma project
 * 
 * Copyright (c) 2009 University of British Columbia
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
package ubic.gemma.web.controller.expression.experiment;

import gemma.gsec.util.SecurityUtil;

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.ValidatedAnnotations;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.tasks.analysis.expression.AutoTaggerTaskCommand;

/**
 * Controller for methods involving annotation of experiments (and potentially other things); delegates to
 * OntologyService and the ExpressionExperimentAnnotator
 * 
 * @author paul
 * @version $Id$
 * @see ubic.gemma.web.controller.common.CharacteristicBrowserController for related methods.
 */
@Controller
public class AnnotationController {

    private static final Log log = LogFactory.getLog( AnnotationController.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;
    @Autowired
    private AuditTrailService auditTrailService;
    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private CharacteristicService characteristicService;
    @Autowired
    private ExpressionExperimentService expressionExperimentService;
    @Autowired
    private OntologyService ontologyService;
    @Autowired
    private TaxonService taxonService;

    /**
     * @return
     */
    public Collection<OntologyTerm> getCategoryTerms() {
        return ontologyService.getCategoryTerms();
    }

    /**
     * @param eeId
     * @return taskId
     */
    public String autoTag( Long eeId ) {

        if ( eeId == null ) {
            throw new IllegalArgumentException( "Id cannot be null" );
        }

        return taskRunningService.submitRemoteTask( new AutoTaggerTaskCommand( eeId ) );
    }

    public void createBiomaterialTag( Characteristic vc, Long id ) {
        BioMaterial bm = bioMaterialService.load( id );
        if ( bm == null ) {
            throw new IllegalArgumentException( "No such BioMaterial with id=" + id );
        }
        bioMaterialService.thaw( bm );
        ontologyService.saveBioMaterialStatement( vc, bm );
    }

    /**
     * Ajax
     * 
     * @param vc . If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param id of the expression experiment
     */
    public void createExperimentTag( Characteristic vc, Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        if ( ee == null ) {
            throw new IllegalArgumentException( "No such experiment with id=" + id );
        }
        ee = expressionExperimentService.thawLite( ee );
        ontologyService.saveExpressionExperimentStatement( vc, ee );

    }

    /**
     * AJAX. Find terms for tagging, etc.
     * 
     * @param givenQueryString
     * @param categoryUri Currently not used as it is not always set properly.
     * @param taxonId only used for genes, but generally this restriction is problematic for factorvalues, which is an
     *        important use case.
     * @return
     */
    public Collection<CharacteristicValueObject> findTerm( String givenQueryString, Long taxonId ) {
        if ( StringUtils.isBlank( givenQueryString ) ) {
            return new HashSet<CharacteristicValueObject>();
        }
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        return ontologyService.findTermsInexact( givenQueryString, taxon );
    }

    /**
     * AJAX Note that this completely scraps the indices, and runs asynchronously.
     */
    public void reinitializeOntologyIndices() {
        if ( !SecurityUtil.isRunningAsAdmin() ) {
            log.warn( "Attempt to run ontology re-indexing as non-admin." );
            return;
        }
        ontologyService.reinitializeAllOntologies();
    }

    /**
     * @param vc
     * @param id
     */
    public void removeBiomaterialTag( Characteristic vc, Long id ) {
        BioMaterial bm = bioMaterialService.load( id );
        if ( bm == null ) {
            throw new IllegalArgumentException( "No such BioMaterial with id=" + id );
        }
        bioMaterialService.thaw( bm );
        ontologyService.removeBioMaterialStatement( vc.getId(), bm );
    }

    /**
     * Ajax.
     * 
     * @param characterIds
     * @param eeId
     */
    public void removeExperimentTag( Collection<Long> characterIds, Long eeId ) {

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            return;
        }

        ee = expressionExperimentService.thawLite( ee );

        Collection<Characteristic> current = ee.getCharacteristics();

        Collection<Characteristic> found = new HashSet<Characteristic>();

        for ( Characteristic characteristic : current ) {
            if ( characterIds.contains( characteristic.getId() ) ) found.add( characteristic );

        }

        for ( Characteristic characteristic : found ) {
            log.info( "Removing characteristic  from " + ee + " : " + characteristic );
        }

        current.removeAll( found );
        ee.setCharacteristics( current );
        expressionExperimentService.update( ee );

        for ( Long id : characterIds ) {
            characteristicService.delete( id );
        }

    }

    /**
     * @param eeId
     */
    public void validateTags( Long eeId ) {
        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            return;
        }
        this.auditTrailService.addUpdateEvent( ee, ValidatedAnnotations.class, "", "" );
    }

}
