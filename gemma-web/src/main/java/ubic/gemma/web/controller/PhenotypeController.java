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
package ubic.gemma.web.controller;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;

/**
 * Controller for phenotype
 * 
 * @author frances
 * @version $Id$
 */
@Controller
public class PhenotypeController extends BaseController {

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

    @RequestMapping(value = "/phenotypes.html", method = RequestMethod.GET)
    public ModelAndView showAllPhenotypes( HttpServletRequest request, HttpServletResponse response ) {
        ModelAndView mav = new ModelAndView( "phenotypes" );

        mav.addObject( "phenotypeValue", request.getParameter( "phenotypeValue" ) );
        mav.addObject( "geneId", request.getParameter( "geneId" ) );

        return mav;
    }

    @RequestMapping("/phenotypeAssociationForm.html")
    public ModelAndView createPhenotypeAssociationForm() {
        return new ModelAndView( "phenotypeAssociationForm" );
    }

    // Frances: This method is not being used and for testing purpose ONLY.
    // @RequestMapping("/phenotype-search.html")
    // public ModelAndView searchPhenotype() {
    // return new ModelAndView("phenotypeSearch");
    // }

    /**
     * Returns all genes that have given phenotypes.
     * 
     * @param phenotypes
     * @return all genes that have given phenotypes
     */
    public JsonReaderResponse<GeneValueObject> findCandidateGenes( String[] phenotypes ) {
        Set<String> myPhenotypes = new HashSet<String>();
        for ( String pheno : phenotypes ) {
            myPhenotypes.add( pheno );
        }

        return new JsonReaderResponse<GeneValueObject>( new ArrayList<GeneValueObject>(
                this.phenotypeAssociationManagerService.findCandidateGenes( myPhenotypes ) ) );
    }

    /**
     * Returns all phenotypes in the system.
     * 
     * @return all phenotypes in the system
     */
    public JsonReaderResponse<CharacteristicValueObject> loadAllPhenotypes() {

        return new JsonReaderResponse<CharacteristicValueObject>( new ArrayList<CharacteristicValueObject>(
                this.phenotypeAssociationManagerService.loadAllPhenotypes() ) );
    }
}
