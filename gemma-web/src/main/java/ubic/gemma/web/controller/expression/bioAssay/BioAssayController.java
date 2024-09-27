/*
 * The Gemma project
 *
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.expression.bioAssay;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.analysis.preprocess.OutlierDetails;
import ubic.gemma.core.analysis.preprocess.OutlierDetectionService;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.analysis.expression.BioAssayOutlierProcessingTaskCommand;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayValueObject;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.util.EntityNotFoundException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author keshav
 */
@Controller
@RequestMapping("/bioAssay")
public class BioAssayController {

    private static final String identifierNotFound = "You must provide a valid BioAssay identifier";
    private static final Log log = LogFactory.getLog( BioAssayController.class.getName() );

    @Autowired
    private TaskRunningService taskRunningService;

    @Autowired
    private BioAssayService bioAssayService;

    @Autowired
    private ExpressionExperimentService eeService;

    @Autowired
    private OutlierDetectionService outlierDetectionService;

    public Collection<BioAssayValueObject> getBioAssays( Long eeId ) {
        ExpressionExperiment ee = eeService.loadAndThawLiteOrFail( eeId,
                EntityNotFoundException::new, "Could not load experiment with ID=" + eeId );

        Collection<BioAssayValueObject> result = new HashSet<>();
        Collection<OutlierDetails> outliers = outlierDetectionService.getOutlierDetails( ee );

        if ( outliers != null ) {
            Map<Long, OutlierDetails> outlierMap = outliers.stream()
                    .collect( Collectors.toMap( od -> od.getBioAssay().getId(), od -> od, ( a, b ) -> b ) );
            for ( BioAssay assay : ee.getBioAssays() ) {
                result.add( new BioAssayValueObject( assay, false, outlierMap.containsKey( assay.getId() ) ) );
            }
        }

        BioAssayController.log.debug( "Loaded " + result.size() + " bioassays for experiment ID=" + eeId );
        return result;
    }

    @SuppressWarnings("unused") // Is used in EEManager.js
    public String markOutlier( Collection<Long> ids ) {
        return taskRunningService.submitTaskCommand( new BioAssayOutlierProcessingTaskCommand( ids ) );
    }

    @SuppressWarnings("unused") // Is used in EEManager.js
    public String unmarkOutlier( Collection<Long> ids ) {
        return taskRunningService.submitTaskCommand( new BioAssayOutlierProcessingTaskCommand( ids, true ) );
    }

    @RequestMapping(value = { "/showBioAssay.html", "/" }, method = RequestMethod.GET)
    public ModelAndView show( @RequestParam("id") Long id ) {
        BioAssay bioAssay = bioAssayService.load( id );
        if ( bioAssay == null ) {
            throw new EntityNotFoundException( id + " not found" );
        }
        bioAssay = bioAssayService.thaw( bioAssay );
        return new ModelAndView( "bioAssay.detail" )
                .addObject( "bioAssay", new BioAssayValueObject( bioAssay, false ) );
    }

    @RequestMapping(value = "/showAllBioAssays.html", method = RequestMethod.GET)
    public ModelAndView showAllBioAssays( @RequestParam(value = "id", required = false) String sId ) {
        Collection<BioAssay> bioAssays = new ArrayList<>();
        if ( StringUtils.isBlank( sId ) ) {
            /*
             * Probably not desirable ... there are >380,000 of them
             */
            bioAssays = bioAssayService.loadAll();
        } else {
            String[] idList = StringUtils.split( sId, ',' );
            for ( String anIdList : idList ) {
                Long id = Long.parseLong( anIdList );
                BioAssay bioAssay = bioAssayService.load( id );
                if ( bioAssay == null ) {
                    throw new EntityNotFoundException( id + " not found" );
                }
                bioAssay = bioAssayService.thaw( bioAssay );
                bioAssays.add( bioAssay );
            }
        }
        return new ModelAndView( "bioAssays" ).addObject( "bioAssays", bioAssays );
    }

}
