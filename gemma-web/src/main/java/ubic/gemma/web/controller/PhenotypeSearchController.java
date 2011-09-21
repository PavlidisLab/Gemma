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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.association.phenotype.PhenotypeAssociationManagerService;
import ubic.gemma.model.genome.gene.GeneValueObject;
import ubic.gemma.model.genome.gene.phenotype.valueObject.CharacteristicValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;

/**
 * Controller for searching phenotypes
 * 
 * @author frances
 * @version $Id$
 */
@Controller
public class PhenotypeSearchController extends BaseController { 

    @Autowired
    private PhenotypeAssociationManagerService phenotypeAssociationManagerService;

	
    @RequestMapping("/phenotypes.html")
    public ModelAndView showAllPhenotypes() {
        return new ModelAndView("phenotypes");
    }
   
    @RequestMapping("/phenotype-search.html")
    public ModelAndView searchPhenotype() {
        return new ModelAndView("phenotypeSearch");
    }

    public JsonReaderResponse<GeneValueObject> findCandidateGenes(String[] values) {
    	return new JsonReaderResponse<GeneValueObject>(
    			new ArrayList<GeneValueObject>(phenotypeAssociationManagerService.findCandidateGenes(values)));
    }

    public JsonReaderResponse<CharacteristicValueObject> findAllPhenotypes() {
    	return new JsonReaderResponse<CharacteristicValueObject>(
    			new ArrayList<CharacteristicValueObject>(phenotypeAssociationManagerService.findAllPhenotypes()));
    }
}

