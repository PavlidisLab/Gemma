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
package ubic.gemma.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import ubic.gemma.core.search.SearchResult;
import ubic.gemma.model.annotations.GemmaWebOnly;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.search.SearchSettings;
import ubic.gemma.model.common.search.SearchSettingsValueObject;
import ubic.gemma.web.remote.JsonReaderResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Note: do not use parametrized collections as parameters for ajax methods in this class! Type information is lost
 * during proxy creation so DWR can't figure out what type of collection the method should take. See bug 2756. Use
 * arrays instead.
 *
 * @author paul
 */
@Controller
public interface GeneralSearchController {

    JsonReaderResponse<SearchResult<?>> ajaxSearch( SearchSettingsValueObject settings );

    @RequestMapping(value = "/searcher.html", method = RequestMethod.POST)
    ModelAndView doSearch( HttpServletRequest request, HttpServletResponse response, SearchSettings command,
            BindException errors ) throws Exception;

    ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response,
            SearchSettings command, BindException errors ) throws Exception;

}