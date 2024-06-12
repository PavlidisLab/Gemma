/*
 * The Gemma project
 *
 * Copyright (c) 2011 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExperimentalFactorValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;
import ubic.gemma.persistence.service.analysis.expression.diff.DifferentialExpressionAnalysisService;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;

import java.util.Collection;

/**
 * @author pavlidis
 * @see ExperimentalFactorService
 */
@Service
public class ExperimentalFactorServiceImpl
        extends AbstractVoEnabledService<ExperimentalFactor, ExperimentalFactorValueObject>
        implements ExperimentalFactorService {

    private final ExperimentalFactorDao experimentalFactorDao;
    private final DifferentialExpressionAnalysisService differentialExpressionAnalysisService;
    private final BioMaterialService bioMaterialService;

    @Autowired
    public ExperimentalFactorServiceImpl( ExperimentalFactorDao experimentalFactorDao,
            DifferentialExpressionAnalysisService differentialExpressionAnalysisService, BioMaterialService bioMaterialService ) {
        super( experimentalFactorDao );
        this.experimentalFactorDao = experimentalFactorDao;
        this.differentialExpressionAnalysisService = differentialExpressionAnalysisService;
        this.bioMaterialService = bioMaterialService;
    }

    @Override
    @Transactional
    public void remove( ExperimentalFactor experimentalFactor ) {
        experimentalFactor = ensureInSession( experimentalFactor );

        log.info( "Removing factor " + experimentalFactor + "..." );
        // First, check to see if there are any diff results that use this factor.
        int removedAnalysis = differentialExpressionAnalysisService.removeForExperimentalFactor( experimentalFactor );
        if ( removedAnalysis > 0 ) {
            log.info( String.format( "Removed %d analyses associated to factor %s", removedAnalysis, experimentalFactor ) );
        }

        // detach the experimental factor from its experimental design, otherwise it will be re-saved in cascade
        ExperimentalDesign ed = experimentalFactor.getExperimentalDesign();
        ed.getExperimentalFactors().remove( experimentalFactor );

        // remove associations with the experimental factor values in related expression experiments
        Collection<BioMaterial> bioMaterials = bioMaterialService.findByFactor( experimentalFactor );
        for ( BioMaterial bm : bioMaterials ) {
            if ( bm.getFactorValues().removeAll( experimentalFactor.getFactorValues() ) ) {
                log.info( "Removed factor value(s) of " + experimentalFactor + " from " + bm );
            }
        }

        super.remove( experimentalFactor );
    }

    @Override
    @Transactional
    public void remove( Collection<ExperimentalFactor> experimentalFactors ) {
        experimentalFactors = ensureInSession( experimentalFactors );
        // First, check to see if there are any diff results that use this factor.
        int removedAnalysis = differentialExpressionAnalysisService.removeForExperimentalFactors( experimentalFactors );
        if ( removedAnalysis > 0 ) {
            log.info( String.format( "Removed %d analyses associated to %d factors", removedAnalysis, experimentalFactors.size() ) );
        }
        super.remove( experimentalFactors );
    }

    @Override
    public void remove( Long id ) {
        throw new UnsupportedOperationException( "Removing an experimental factor by ID is not supported" );
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentalFactor thaw( ExperimentalFactor ef ) {
        return this.experimentalFactorDao.thaw( ef );
    }

}