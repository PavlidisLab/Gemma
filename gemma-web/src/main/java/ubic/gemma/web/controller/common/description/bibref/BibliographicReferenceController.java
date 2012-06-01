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

import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import ubic.gemma.model.common.description.BibliographicReferenceValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;

/**
 * Note: do not use parameterized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 * 
 * @version $Id$
 */
@RequestMapping("/bibRef")
public interface BibliographicReferenceController {

    /**
     * Add or update a record.
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/bibRefAdd.html")
    public abstract ModelAndView add( HttpServletRequest request, HttpServletResponse response );
    
    /**
     * AJAX
     * 
     * @param batch
     * @return
     */
    public abstract JsonReaderResponse<BibliographicReferenceValueObject> browse( ListBatchCommand batch );

    /**
     * AJAX
     * 
     * @param query
     * @return
     */
    public abstract JsonReaderResponse<BibliographicReferenceValueObject> search( String query);

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/deleteBibRef.html")
    public abstract ModelAndView delete( HttpServletRequest request, HttpServletResponse response );

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/bibRefView.html")
    public abstract ModelAndView show( HttpServletRequest request, HttpServletResponse response );

    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/searchBibRefs.html")
    public abstract ModelAndView searchBibRefs( HttpServletRequest request, HttpServletResponse response );
    
    /**
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/showAllEeBibRefs.html")
    public abstract ModelAndView showAllForExperiments( HttpServletRequest request, HttpServletResponse response );


    /**
     * AJAX 
     * @return collection of all bib refs that are for an experiment
     */
    public Collection<BibliographicReferenceValueObject> loadAllForExperiments();
    /**
     * For AJAX calls. Refresh the Gemma entry based on information from PubMed.
     * 
     * @param id
     */
    public abstract void update( Long id );
    
    /**
     * AJAX
     * @param ids
     * @return
     */
    public JsonReaderResponse<BibliographicReferenceValueObject> loadMultiple( Collection<Long> ids );
    
    /**
     * AJAX
     * @param ids
     * @return
     */
    public BibliographicReferenceValueObject load( Long id );

    /**
     * AJAX
     * @param pubmed ID id
     * @return
     */
    public BibliographicReferenceValueObject loadFromPubmedID( String pubmedID );

}