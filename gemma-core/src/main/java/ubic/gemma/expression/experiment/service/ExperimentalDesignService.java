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
package ubic.gemma.expression.experiment.service;

import java.util.Collection;

import org.springframework.security.access.annotation.Secured;

import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

/**
 * @author kelsey
 * @version $Id$
 */
public interface ExperimentalDesignService {

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ExperimentalDesign create( ExperimentalDesign experimentalDesign );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExperimentalDesign find( ExperimentalDesign experimentalDesign );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExperimentalDesign findByName( String name );

    /**
     * 
     */
    @Secured( { "GROUP_USER" })
    public ExperimentalDesign findOrCreate( ExperimentalDesign experimentalDesign );

    /**
     * Gets the expression experiment for the specified experimental design object
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExpressionExperiment getExpressionExperiment( ExperimentalDesign experimentalDesign );

    /**
     * 
     */
    @Secured( { "IS_AUTHENTICATED_ANONYMOUSLY", "AFTER_ACL_READ" })
    public ExperimentalDesign load( Long id );

    /**
     * 
     */
    @Secured( { "GROUP_ADMIN" })
    public Collection<ExperimentalDesign> loadAll();

    /**
     * 
     */
    @Secured( { "GROUP_USER", "ACL_SECURABLE_EDIT" })
    public void update( ExperimentalDesign experimentalDesign );

    public String clearDesignCaches( Long eeId );

    public void clearDesignCaches( ExpressionExperiment ee );

    public void clearDesignCaches();
    

}
