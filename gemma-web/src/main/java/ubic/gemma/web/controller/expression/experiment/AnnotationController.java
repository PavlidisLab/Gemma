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

import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.ontology.OntologyService;

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

    private static Log log = LogFactory.getLog( AnnotationController.class );

    @Autowired
    private ExpressionExperimentAnnotator expressionExperimentAnnotator;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private TaxonService taxonService;

    /**
     * @param eeId
     * @return
     */
    public Collection<Characteristic> autoTag( Long eeId ) {

        if ( eeId == null ) {
            throw new IllegalArgumentException( "Id cannot be null" );
        }

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            throw new IllegalArgumentException( "No experiment with id=" + eeId + " could be loaded" );
        }

        if ( !ExpressionExperimentAnnotator.ready() ) {
            throw new RuntimeException( "The auto-tagger is not available." );
        }

        /*
         * TODO: put this in a background job, because it is slow.
         */
        return expressionExperimentAnnotator.annotate( ee, false );
    }

    /**
     * @param givenQueryString
     * @param categoryUri
     * @param taxonId
     * @return
     */
    public Collection<Characteristic> findTerm( String givenQueryString, String categoryUri, Long taxonId ) {
        Taxon taxon = null;
        if ( taxonId != null ) {
            taxon = taxonService.load( taxonId );
        }
        return ontologyService.findExactTerm( givenQueryString, categoryUri, taxon );
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
     * Ajax
     * 
     * @param vc . If the evidence code is null, it will be filled in with IC. A category and value must be provided.
     * @param id of the expression experiment
     */
    public void createExperimentTag( Characteristic vc, Long id ) {
        ExpressionExperiment ee = expressionExperimentService.load( id );
        ontologyService.saveExpressionExperimentStatement( vc, ee );

    }
}
