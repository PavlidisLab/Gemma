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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import ubic.gemma.annotation.geommtx.ExpressionExperimentAnnotator;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;

/**
 * Controller for methods involving annotation of experiments (and potentially other things)
 * <p>
 * TODO: put passthru methods for OntologyService, move ajax-exposed methods from there to here.
 * 
 * @author paul
 * @version $Id$
 */
@Controller
public class AnnotationController {

    @Autowired
    private ExpressionExperimentAnnotator expressionExperimentAnnotator;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    /**
     * AJAX
     * 
     * @param eeId
     * @return
     */
    public Collection<Characteristic> annotate( Long eeId ) {

        if ( eeId == null ) {
            throw new IllegalArgumentException( "Id cannot be null" );
        }

        ExpressionExperiment ee = expressionExperimentService.load( eeId );

        if ( ee == null ) {
            throw new IllegalArgumentException( "No experiment with id=" + eeId + " could be loaded" );
        }

        /*
         * TODO: put this in a background job.
         */
        return expressionExperimentAnnotator.annotate( ee, false );
    }

}
