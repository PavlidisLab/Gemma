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

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    private final ExperimentalDesignDao experimentalDesignDao;

    @Autowired
    public ExperimentalDesignServiceImpl( ExperimentalDesignDao experimentalDesignDao ) {
        super( experimentalDesignDao );
        this.experimentalDesignDao = experimentalDesignDao;
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentalDesign loadWithExperimentalFactors( Long id ) {
        ExperimentalDesign ed = experimentalDesignDao.load( id );
        if ( ed != null ) {
            ed.getExperimentalFactors().forEach( Hibernate::initialize );
        }
        return ed;
    }

    @Override
    @Transactional(readOnly = true)
    public ExperimentalDesign getRandomExperimentalDesignThatNeedsAttention( ExperimentalDesign excludedDesign ) {
        return experimentalDesignDao.getRandomExperimentalDesignThatNeedsAttention( excludedDesign );
    }
}