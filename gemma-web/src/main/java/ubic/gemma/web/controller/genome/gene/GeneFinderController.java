/*
 * The Gemma project
 * 
 * Copyright (c) 2006 University of British Columbia
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
package ubic.gemma.web.controller.genome.gene;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.designElement.CompositeSequenceService;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.model.genome.gene.GeneProductService;
import ubic.gemma.model.genome.gene.GeneService;

/** 
 * @author joseph
 * @version $Id$
 * @spring.bean id="geneFinderController"  
 * @spring.property name="formView" value="geneFinder"
 * @spring.property name="successView" value="geneFinder"
 * @spring.property name="geneService" ref="geneService"
 * @spring.property name="geneProductService" ref="geneProductService" 
 * @spring.property name="compositeSequenceService" ref="compositeSequenceService"  
 */
public class GeneFinderController extends SimpleFormController {
    private GeneService geneService;
    private GeneProductService geneProductService;
    private CompositeSequenceService compositeSequenceService;
    
    /**
     * @return the compositeSequenceService
     */
    public CompositeSequenceService getCompositeSequenceService() {
        return compositeSequenceService;
    }

    /**
     * @param compositeSequenceService the compositeSequenceService to set
     */
    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    /**
     * @return the geneProductService
     */
    public GeneProductService getGeneProductService() {
        return geneProductService;
    }

    /**
     * @param geneProductService the geneProductService to set
     */
    public void setGeneProductService( GeneProductService geneProductService ) {
        this.geneProductService = geneProductService;
    }

    /**
     * @return Returns the bibliographicReferenceService.
     */
    public GeneService getGeneService() {
        return geneService;
    }

    /**
     * @param geneService The geneService to set.
     */
    public void setGeneService( GeneService geneService ) {
        this.geneService = geneService;
    }

    @Override
    @SuppressWarnings({ "unused", "unchecked" })
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        String searchString = request.getParameter( "searchString" );

        // search by inexact symbol
        //Collection tmp = new ArrayList<Gene>();
        Set<Gene> geneSet =  new HashSet<Gene>();
        geneSet.addAll( geneService.findByOfficialSymbolInexact( searchString ) );
        geneSet.addAll( geneService.getByGeneAlias( searchString ) );  
        geneSet.addAll( geneProductService.getGenesByName( searchString ) );  
        geneSet.addAll( geneProductService.getGenesByNcbiId( searchString ) );  
        
        List<Gene> geneList = new ArrayList<Gene>(geneSet);
        Comparator<Gene> comparator = new GeneComparator();
        Collections.sort( geneList, comparator );
//        Collection<Gene> genesOfficialSymbol = geneService.findByOfficialSymbolInexact( searchString );
//        Collection<Gene> genesAlias = geneService.getByGeneAlias( searchString );
//        Set<Gene> genesGeneProductSet = new HashSet<Gene>();
//        genesGeneProductSet.addAll( geneProductService.getGenesByName( searchString ) );
//        genesGeneProductSet.addAll( geneProductService.getGenesByNcbiId( searchString ) );        
        ModelAndView mav = new ModelAndView("geneFinderList");
        mav.addObject( "genes", geneList );
        mav.addObject( "searchParameter", searchString );
        
        return mav;

    }

    /**
     * This is needed or you will have to specify a commandClass in the DispatcherServlet's context
     * 
     * @param request
     * @return Object
     * @throws Exception
     */
    @Override
    protected Object formBackingObject( HttpServletRequest request ) throws Exception {
        return request;
    }
    
    class GeneComparator implements Comparator<Gene> {

        public int compare( Gene arg0, Gene arg1 ) {
            Gene obj0 = arg0;
            Gene obj1 = arg1;

            return obj0.getName().compareTo( obj1.getName() );
        }
    }
}
