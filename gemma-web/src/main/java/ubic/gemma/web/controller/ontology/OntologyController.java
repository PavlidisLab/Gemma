/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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

/*
 * The Gemma project
 * 
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.web.controller.ontology;

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.ontology.Ontology;
import ubic.gemma.ontology.OntologyTerm;
import ubic.gemma.ontology.OntologyTools;
import ubic.gemma.web.controller.BaseFormController;

/**
 * @author klc
 * @version Id: OntologyController.java
 * @spring.bean id="ontologyController"
 * @spring.property name = "commandName" value="ontology"
 * @spring.property name = "formView" value="ontology.edit"
 * @spring.property name = "successView" value="ontology"
 */

public class OntologyController extends BaseFormController {

    private static Log log = LogFactory.getLog( OntologyController.class.getName() );
    private Collection<Ontology> ontos;

    public OntologyController() {
        // /* if true, reuses the same command object across the edit-submit-process (get-post-process). */
        // setSessionForm( true );
        //    
        // //ontos = OntologyService.listAvailableOntologies();
        //        
        // try{
        // GZIPInputStream is = new GZIPInputStream(
        // this.getClass().getResourceAsStream("/data/loader/ontology/MGEDOntology.owl.gz" ) );
        // OntologyTools.initOntology( is, "http://mged.sourceforge.net/ontologies/MGEDOntology.owl",
        // OntModelSpec.OWL_MEM_RDFS_INF );
        // }
        // catch(Exception e){
        // throw new RuntimeException(e);
        // }
    }

    protected ModelAndView showForm( HttpServletRequest request, HttpServletResponse response, BindException errors )
            throws Exception {

        ModelAndView mnv = new ModelAndView( getSuccessView() );
        // mnv.addObject( "ontologies", ontos );

        return mnv;
    }

    /*
     * @param request @param response @param command @param errors @return ModelAndView @throws Exception
     */
    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        log.debug( "entering onSubmit" );

        String searchString = request.getParameter( "searchString" );

        ModelAndView mnv = new ModelAndView( getSuccessView() );

        OntologyTerm term = OntologyTools.getOntologyTerm( searchString );
        if ( term == null ) {
            this.saveMessage( request, "No ontology term with uri: " + searchString );
            return mnv;
        }
        mnv.addObject( "parents", term.getParents( true ) );
        mnv.addObject( "children", term.getChildren( true ) );
        mnv.addObject( "restrictions", term.getRestrictions() );
        mnv.addObject( "individuals", term.getIndividuals( true ) );

        return mnv;
    }

    // @Override
    // protected Map referenceData( HttpServletRequest request ) throws Exception {
    // Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();
    // //mapping.put( "technologyTypes", new ArrayList<String>( TechnologyType.literals() ) );
    // return mapping;
    // }

    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        return new String();
    }

}
