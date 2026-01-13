/*
 * The gemma-core project
 *
 * Copyright (c) 2018 University of British Columbia
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

package ubic.gemma.core.analysis.preprocess;

import gemma.gsec.SecurityService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.core.analysis.service.ExpressionExperimentDeleterService;
import ubic.gemma.model.analysis.expression.ExpressionExperimentSet;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 *
 * Split an experiment into multiple experiments. This is needed when a load EE (e.g. from GEO) is better represented as
 * two more distinct experiments. The decision of what to split is based on curation guidelines documented
 * elsewhere.
 *
 * @author paul
 */
@Service
@Transactional(propagation = Propagation.NEVER)
public class SplitExperimentServiceImpl implements SplitExperimentService {

    private static final Log log = LogFactory.getLog( SplitExperimentServiceImpl.class );

    @Autowired
    private PreprocessorService preprocessor;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private ExpressionExperimentDeleterService eeService;

    @Autowired
    private SplitExperimentHelperService splitExperimentHelperService;

    @Override
    public ExpressionExperimentSet split( ExpressionExperiment toSplit, ExperimentalFactor splitOn, boolean postProcess, boolean deleteOriginalExperiment ) {
        SplitExperimentHelperService.ExperimentSplitResult result = splitExperimentHelperService.split( toSplit, splitOn );

        if ( result.isFoundPreferred() && postProcess ) {
            for ( ExpressionExperiment split : result.getExperimentSet().getExperiments() ) {
                // postprocess
                try {
                    preprocessor.process( split );
                } catch ( Exception e ) {
                    log.error( "Failure while postprocessing (will continue): " + split + ": " + e.getMessage() );
                }
            }
        } else {
            log.info( "Postprocessing skipped for experiments in " + result.getExperimentSet() + "." );
        }

        // Clean the source experiment? remove diff and coexpression analyses, PCA, correlation matrices, processed data vectors
        // delete it?
        if ( deleteOriginalExperiment ) {
            eeService.delete( toSplit );
        } else {
            // OR perhaps only
            // Or mark it as troubled?
            securityService.makePrivate( toSplit );
        }

        return result.getExperimentSet();
    }
}
