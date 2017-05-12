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

import org.springframework.security.access.annotation.Secured;

/**
 * @author kelsey
 * @version $Id$
 */
public interface CompoundService {

    /**
     * 
     */
    public ubic.gemma.model.expression.biomaterial.Compound find(
            ubic.gemma.model.expression.biomaterial.Compound compound );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.expression.biomaterial.Compound findOrCreate(
            ubic.gemma.model.expression.biomaterial.Compound compound );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void remove( ubic.gemma.model.expression.biomaterial.Compound compound );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public void update( ubic.gemma.model.expression.biomaterial.Compound compound );

}
