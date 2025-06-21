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
import ubic.gemma.model.expression.bioAssayData.BioAssayDimension;
import ubic.gemma.model.expression.experiment.BioAssaySet;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.persistence.service.expression.bioAssay.BioAssayService;
import ubic.gemma.persistence.service.expression.bioAssayData.BioAssayDimensionService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.util.EntityNotFoundException;

import javax.annotation.Nullable;
import java.util.*;
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

    @Autowired
    private BioAssayDimensionService bioAssayDimensionService;

    @RequestMapping(value = { "/showBioAssay.html", "/" }, method = { RequestMethod.GET, RequestMethod.HEAD }, params = {"id"})
    public ModelAndView show( @RequestParam("id") Long id, @RequestParam(value = "dimension", required = false) Long dimensionId ) {
        BioAssay bioAssay = bioAssayService.loadOrFail( id, EntityNotFoundException::new );
        bioAssay = bioAssayService.thaw( bioAssay );
        return new ModelAndView( "bioAssay.detail" )
                .addObject( "bioAssay", bioAssay )
                .addAllObjects( getBioAssayHierarchy( bioAssay, dimensionId ) )
                .addObject( "bioAssaySets", bioAssayService.getBioAssaySets( bioAssay ).stream().sorted( Comparator.comparing( BioAssaySet::getName ) ).collect( Collectors.toList() ) );
    }

    @RequestMapping(value = { "/showBioAssay.html", "/" }, method = { RequestMethod.GET, RequestMethod.HEAD }, params = {"shortName"})
    public ModelAndView showByShortName( @RequestParam("shortName") String shortName, @RequestParam(value = "dimension", required = false) Long dimensionId ) {
        BioAssay bioAssay = bioAssayService.findByShortName( shortName );
        if (bioAssay == null) {
            throw new EntityNotFoundException( "No BioAssay with the short name " + shortName + ".");
        }
        bioAssay = bioAssayService.thaw( bioAssay );
        return new ModelAndView( "bioAssay.detail" )
                .addObject( "bioAssay", bioAssay )
                .addAllObjects( getBioAssayHierarchy( bioAssay, dimensionId ) )
                .addObject( "bioAssaySets", bioAssayService.getBioAssaySets( bioAssay ).stream().sorted( Comparator.comparing( BioAssaySet::getName ) ).collect( Collectors.toList() ) );
    }

    private Map<String, ?> getBioAssayHierarchy( BioAssay bioAssay, @Nullable Long dimensionId ) {
        if ( dimensionId == null ) {
            return Collections.emptyMap();
        }
        List<BioAssay> parents;
        if ( bioAssay.getSampleUsed().getSourceBioMaterial() != null ) {
            parents = bioAssay.getSampleUsed().getSourceBioMaterial().getBioAssaysUsedIn().stream()
                    .sorted( Comparator.comparing( BioAssay::getName ) )
                    .collect( Collectors.toList() );
        } else {
            parents = Collections.emptyList();
        }
        // only show siblings that share a common BAD
        BioAssayDimension dim = bioAssayDimensionService.loadOrFail( dimensionId, EntityNotFoundException::new );
        Set<BioAssay> sharedBas = new HashSet<>( dim.getBioAssays() );
        List<BioAssay> siblings = bioAssayService.findSiblings( bioAssay ).stream()
                .filter( sharedBas::contains )
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .collect( Collectors.toList() );
        List<BioAssay> children = bioAssayService.findSubBioAssays( bioAssay, true ).stream()
                .filter( sharedBas::contains )
                .sorted( Comparator.comparing( BioAssay::getName ) )
                .collect( Collectors.toList() );
        Map<String, Object> result = new HashMap<>();
        result.put( "dimension", dim );
        result.put( "parents", parents );
        result.put( "singleParent", parents.size() == 1 ? parents.iterator().next() : null );
        result.put( "children", children );
        result.put( "siblings", siblings );
        return result;
    }

    @SuppressWarnings("unused") // Is used in EEManager.js
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
}
