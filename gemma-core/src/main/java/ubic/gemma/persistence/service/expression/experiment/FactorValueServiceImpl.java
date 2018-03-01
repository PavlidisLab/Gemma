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
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueValueObject;
import ubic.gemma.persistence.service.AbstractVoEnabledService;

import java.util.Collection;

/**
 * <p>
 * Spring Service base class for <code>FactorValueService</code>, provides access
 * to all services and entities referenced by this service.
 * </p>
 *
 * @author pavlidis
 * @author keshav
 * @see FactorValueService
 */
@Service
public class FactorValueServiceImpl extends AbstractVoEnabledService<FactorValue, FactorValueValueObject>
        implements FactorValueService {

    private final FactorValueDao factorValueDao;

    @Autowired
    public FactorValueServiceImpl( FactorValueDao factorValueDao ) {
        super( factorValueDao );
        this.factorValueDao = factorValueDao;
    }

    @Override
    @Transactional(readOnly = true)
    public Collection<FactorValue> findByValue( String valuePrefix ) {
        return this.factorValueDao.findByValue( valuePrefix );
    }

}