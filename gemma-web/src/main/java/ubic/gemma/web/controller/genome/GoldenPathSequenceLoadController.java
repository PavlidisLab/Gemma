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
package ubic.gemma.web.controller.genome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.validation.BindException;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.SimpleFormController;

import ubic.gemma.externalDb.GoldenPathDumper;
import ubic.gemma.loader.genome.goldenpath.GoldenPathBioSequenceLoader;
import ubic.gemma.model.common.description.ExternalDatabaseService;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.model.genome.TaxonService;
import ubic.gemma.model.genome.biosequence.BioSequenceService;
import ubic.gemma.util.progress.ProgressJob;
import ubic.gemma.util.progress.ProgressManager;
import ubic.gemma.web.propertyeditor.TaxonPropertyEditor;

/**
 * Load sequences from Golden Path into Gemma.
 * 
 * @author pavlidis
 * @version $Id$
 * @spring.bean id="goldenPathSequenceLoadController" *
 * @spring.property name="commandName" value="goldenPathSequenceLoadCommand"
 * @spring.property name="commandClass" value="ubic.gemma.web.controller.genome.GoldenPathSequenceLoadCommand"
 * @spring.property name="formView" value="goldenPathSequenceLoad"
 * @spring.property name="successView" value="redirect:/genome/goldenPathSequenceLoad.html"
 * @spring.property name="taxonService" ref="taxonService"
 * @spring.property name="externalDatabaseService" ref="externalDatabaseService"
 * @spring.property name="bioSequenceService" ref="bioSequenceService"
 */
public class GoldenPathSequenceLoadController extends SimpleFormController {

    TaxonService taxonService;
    ExternalDatabaseService externalDatabaseService;
    BioSequenceService bioSequenceService;

    @Override
    @SuppressWarnings("unused")
    public ModelAndView onSubmit( final HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {
        final GoldenPathSequenceLoadCommand params = ( GoldenPathSequenceLoadCommand ) command;

        final SecurityContext context = SecurityContextHolder.getContext();

        Taxon taxon = params.getTaxon();
        GoldenPathDumper dumper = new GoldenPathDumper( taxon );
        GoldenPathBioSequenceLoader gp = new GoldenPathBioSequenceLoader( taxon );
        if ( params.getLimit() > 0 ) {
            gp.setLimit( params.getLimit() );
        }
        gp.setExternalDatabaseService( externalDatabaseService );
        gp.setBioSequenceService( bioSequenceService );

        ProgressJob job = ProgressManager.createProgressJob( null, "pavlidis", "Golden path loading" );
        gp.load( dumper );
        ProgressManager.destroyProgressJob( job );

        return new ModelAndView( this.getSuccessView() );

    }

    /*
     * (non-Javadoc)
     * @see ubic.gemma.web.controller.BaseFormController#initBinder(javax.servlet.http.HttpServletRequest,
     * org.springframework.web.bind.ServletRequestDataBinder)
     */
    @Override
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) throws Exception {
        super.initBinder( request, binder );
        binder.registerCustomEditor( Taxon.class, new TaxonPropertyEditor( this.taxonService ) );
    }

    /*
     * (non-Javadoc)
     * @see
     * org.springframework.web.servlet.mvc.SimpleFormController#referenceData(javax.servlet.http.HttpServletRequest,
     * java.lang.Object, org.springframework.validation.Errors)
     */
    @SuppressWarnings( { "unchecked" })
    @Override
    protected Map referenceData( HttpServletRequest request ) throws Exception {

        Map<String, List<? extends Object>> mapping = new HashMap<String, List<? extends Object>>();

        List<Taxon> taxonNames = new ArrayList<Taxon>();

        for ( Taxon taxon : ( Collection<Taxon> ) taxonService.loadAll() ) {
            taxonNames.add( taxon );
        }

        Collections.sort( taxonNames, new Comparator() {
            public int compare( Object o1, Object o2 ) {
                return ( ( Taxon ) o1 ).getScientificName().compareTo( ( ( Taxon ) o2 ).getScientificName() );
            }
        } );

        mapping.put( "taxa", taxonNames );

        return mapping;

    }

    /**
     * @param taxonService the taxonService to set
     */
    public void setTaxonService( TaxonService taxonService ) {
        this.taxonService = taxonService;
    }

    /**
     * @param bioSequenceService the bioSequenceService to set
     */
    public void setBioSequenceService( BioSequenceService bioSequenceService ) {
        this.bioSequenceService = bioSequenceService;
    }

    /**
     * @param externalDatabaseService the externalDatabaseService to set
     */
    public void setExternalDatabaseService( ExternalDatabaseService externalDatabaseService ) {
        this.externalDatabaseService = externalDatabaseService;
    }

}
