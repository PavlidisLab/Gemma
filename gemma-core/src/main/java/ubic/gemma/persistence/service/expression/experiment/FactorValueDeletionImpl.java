/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 */
/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
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

package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.analysis.expression.diff.DifferentialExpressionAnalysis;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Handles deletions of a factor values. This process includes: 1. Determining if there are any biomaterials that use
 * the factor value 2. If so, remove any differential expression analysis results that use this factor 3. remove the
 * factor value
 *
 * @author tvrossum
 */
@Service
public class FactorValueDeletionImpl implements FactorValueDeletion {

    @Autowired
    private DifferentialExpressionAnalysisService differentialExpressionAnalysisService;

    @Autowired
    private ExperimentalFactorService experimentalFactorService = null;

    @Autowired
    private FactorValueService factorValueService = null;

    @Override
    @Transactional
    public void deleteFactorValues( Collection<Long> fvIds ) {

        Collection<FactorValue> fvsToDelete = new ArrayList<>();

        for ( Long fvId : fvIds ) {

            FactorValue fv = factorValueService.load( fvId );

            if ( fv == null ) {
                throw new IllegalArgumentException( "No factor value with id=" + fvId + " could be loaded" );
            }

            if ( fv.getExperimentalFactor() == null ) {
                throw new IllegalStateException( "No experimental factor for factor value " + fv.getId() );
            }

            /*
             * Delete any diff ex analyses that use this factor.
             */
            ExperimentalFactor ef = experimentalFactorService.loadOrFail( fv.getExperimentalFactor().getId() );
            Collection<DifferentialExpressionAnalysis> analyses = differentialExpressionAnalysisService
                    .findByFactor( ef );
            // Warning: slow.
            for ( DifferentialExpressionAnalysis a : analyses ) {
                differentialExpressionAnalysisService.remove( a );
            }

            ef.getFactorValues().remove( fv ); // this gets done by the factorValueService as well, but can't hurt.
            fvsToDelete.add( fv );

        }

        for ( FactorValue fv : fvsToDelete ) {
            factorValueService.remove( fv );
        }

    }

}
