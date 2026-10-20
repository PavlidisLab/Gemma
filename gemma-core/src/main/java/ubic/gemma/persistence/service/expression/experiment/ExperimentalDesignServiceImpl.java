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
package ubic.gemma.persistence.service.expression.experiment;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.persistence.service.AbstractService;

/**
 * Spring Service base class for <code>ubic.gemma.model.expression.experiment.ExperimentalDesignService</code>, provides
 * access to all services and entities referenced by this service.
 *
 * @author pavlidis
 * @author keshav
 * @see ExperimentalDesignService
 */
@Service
public class ExperimentalDesignServiceImpl extends AbstractService<ExperimentalDesign>
        implements ExperimentalDesignService {

    @Autowired
    private ExperimentalDesignReadService readService;

    @Autowired
    public ExperimentalDesignServiceImpl( ExperimentalDesignDao experimentalDesignDao ) {
        super( experimentalDesignDao );
    }

    // =====================================================================
    // Read methods -- delegate to ExperimentalDesignReadService.
    // ACL @Secured / @PostAuthorize annotations live on the
    // ExperimentalDesignService interface and apply at the facade proxy
    // boundary.
    // =====================================================================

    @Override
    public ExperimentalDesign loadWithExperimentalFactors( Long id ) {
        return readService.loadWithExperimentalFactors( id );
    }

    @Override
    public ExperimentalDesign getRandomExperimentalDesignThatNeedsAttention( ExperimentalDesign excludedDesign ) {
        return readService.getRandomExperimentalDesignThatNeedsAttention( excludedDesign );
    }
}
