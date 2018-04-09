/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2007 University of British Columbia
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
package ubic.gemma.persistence.service.expression.biomaterial;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ubic.gemma.model.expression.biomaterial.Compound;
import ubic.gemma.persistence.service.AbstractService;

/**
 * <p>
 * Spring Service base class for <code>CompoundService</code>, provides access to all services and entities referenced
 * by this service.
 * </p>
 *
 * @see CompoundService
 */
@Service
public class CompoundServiceImpl extends AbstractService<Compound> implements CompoundService {

    @Autowired
    public CompoundServiceImpl( CompoundDao compoundDao ) {
        super( compoundDao );
    }

}