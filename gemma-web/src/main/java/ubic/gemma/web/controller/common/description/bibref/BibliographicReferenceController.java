/*
 * The Gemma project
 * 
 * Copyright (c) 2012 University of British Columbia
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
package ubic.gemma.web.controller.common.description.bibref;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collection;

/**
 * Note: do not use parameterized collections as parameters for ajax methods. Type information is lost during proxy
 * creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use arrays
 * instead.
 */
@SuppressWarnings("unused") // Used in front end
@RequestMapping("/bibRef")
public interface BibliographicReferenceController {

    @RequestMapping("/bibRefAdd.html")
    ModelAndView add( HttpServletRequest request, HttpServletResponse response );

    JsonReaderResponse<BibliographicReferenceValueObject> browse( ListBatchCommand batch );

    @RequestMapping("/deleteBibRef.html")
    ModelAndView delete( HttpServletRequest request, HttpServletResponse response );

    BibliographicReferenceValueObject load( Long id );

    Collection<BibliographicReferenceValueObject> loadAllForExperiments();

    BibliographicReferenceValueObject loadFromPubmedID( String pubmedID );

    JsonReaderResponse<BibliographicReferenceValueObject> loadMultiple( Collection<Long> ids );

    JsonReaderResponse<BibliographicReferenceValueObject> search( SearchSettingsValueObject settings );

    @RequestMapping("/searchBibRefs.html")
    ModelAndView searchBibRefs( HttpServletRequest request, HttpServletResponse response );

    @RequestMapping("/bibRefView.html")
    ModelAndView show( HttpServletRequest request, HttpServletResponse response );

    @RequestMapping("/showAllEeBibRefs.html")
    ModelAndView showAllForExperiments( HttpServletRequest request, HttpServletResponse response );

    BibliographicReferenceValueObject update( String pubMedId );

}