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
package ubic.gemma.model.expression.experiment;

import org.springframework.security.access.annotation.Secured;

/**
 * @author kelsey
 * @version $Id$
 */
public interface ExperimentalDesignService {

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.expression.experiment.ExperimentalDesign create(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ubic.gemma.model.expression.experiment.ExperimentalDesign find(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findByName( java.lang.String name );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ubic.gemma.model.expression.experiment.ExperimentalDesign findOrCreate(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * Gets the expression experiment for the specified experimental design object
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ubic.gemma.model.expression.experiment.ExpressionExperiment getExpressionExperiment(
            ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ubic.gemma.model.expression.experiment.ExperimentalDesign load( java.lang.Long id );

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public java.util.Collection<ExperimentalDesign> loadAll();

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ubic.gemma.model.expression.experiment.ExperimentalDesign experimentalDesign );

}
