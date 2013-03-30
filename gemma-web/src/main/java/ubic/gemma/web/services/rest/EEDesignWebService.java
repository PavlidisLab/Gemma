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
package ubic.gemma.web.services.rest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.bioAssay.BioAssayDao;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;

import com.sun.jersey.api.NotFoundException;

/**
 * Simple web service to return sample annotations for curated dataset.
 * 
 * @author anton
 */
@Service
@Path("/eedesign")
public class EEDesignWebService {

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private BioAssayDao bioAssayDao;

    @GET
    @Path("/findByAccession/{gsmId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, String>> getAnnotationsByGSM( @PathParam("gsmId") String gsmId ) {

        Collection<BioAssay> foundBioAssays = this.bioAssayDao.findByAccession( gsmId );

        if ( foundBioAssays.isEmpty() ) throw new NotFoundException( "Sample not found." );

        Collection<Characteristic> characteristics = new HashSet<Characteristic>();

        return prepareEEAnnotationsUnstructured( foundBioAssays, characteristics );
    }

    @GET
    @Path("/findByAccession/includeConstantFactors/{gsmId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, String>> getAnnotationsByGSMIncludeTagsUnstructured( @PathParam("gsmId") String gsmId ) {

        Collection<BioAssay> foundBioAssays = this.bioAssayDao.findByAccession( gsmId );

        if ( foundBioAssays.isEmpty() ) throw new NotFoundException( "Sample not found." );

        Collection<Characteristic> characteristics = new HashSet<Characteristic>();
        if ( foundBioAssays.size() == 1 ) {
            ExpressionExperiment ee = this.expressionExperimentService
                    .findByBioAssay( foundBioAssays.iterator().next() );
            if ( ee != null ) {
                characteristics.addAll( ee.getCharacteristics() );
            }
        }

        return prepareEEAnnotationsUnstructured( foundBioAssays, characteristics );
    }

    @GET
    @Path("/findByAccession/includeConstantFactorsStructured/{gsmId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Map<String, String>>> getAnnotationsByGSMIncludeTagsStructured(
            @PathParam("gsmId") String gsmId ) {

        Collection<BioAssay> foundBioAssays = this.bioAssayDao.findByAccession( gsmId );

        if ( foundBioAssays.isEmpty() ) throw new NotFoundException( "Sample not found." );

        Collection<Characteristic> characteristics = new HashSet<Characteristic>();
        if ( foundBioAssays.size() == 1 ) {
            ExpressionExperiment ee = this.expressionExperimentService
                    .findByBioAssay( foundBioAssays.iterator().next() );
            if ( ee != null ) {
                characteristics.addAll( ee.getCharacteristics() );
            }
        }

        return prepareEEAnnotationsStructured( foundBioAssays, characteristics );
    }

    @GET
    @Path("/findByShortName/{shortName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, String>> getAnnotations( @PathParam("shortName") String shortName ) {

        ExpressionExperiment experiment = this.expressionExperimentService.findByShortName( shortName );
        if ( experiment == null ) throw new NotFoundException( "Dataset not found." );
        Collection<BioAssay> bioAssays = experiment.getBioAssays();
        Collection<Characteristic> chars = new ArrayList<Characteristic>();

        return prepareEEAnnotationsUnstructured( bioAssays, chars );
    }

    @GET
    @Path("/findByShortName/includeConstantFactors/{shortName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, String>> getAnnotationsIncludeTagsUnstructured(
            @PathParam("shortName") String shortName ) {

        ExpressionExperiment experiment = this.expressionExperimentService.findByShortName( shortName );
        if ( experiment == null ) throw new NotFoundException( "Dataset not found." );
        Collection<BioAssay> bioAssays = experiment.getBioAssays();
        Collection<Characteristic> chars = experiment.getCharacteristics();

        return prepareEEAnnotationsUnstructured( bioAssays, chars );
    }

    @GET
    @Path("/findByShortName/includeConstantFactorsStructured/{shortName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Map<String, String>>> getAnnotationsIncludeTagsStructured(
            @PathParam("shortName") String shortName ) {

        ExpressionExperiment experiment = this.expressionExperimentService.findByShortName( shortName );
        if ( experiment == null ) throw new NotFoundException( "Dataset not found." );
        Collection<BioAssay> bioAssays = experiment.getBioAssays();
        Collection<Characteristic> chars = experiment.getCharacteristics();

        return prepareEEAnnotationsStructured( bioAssays, chars );
    }

    /**
     * Don't introduce structure to separate experimental factors from experiment tags, instead add a prefix to tag
     * categories
     * 
     * @param bioAssays
     * @return
     */
    private Map<String, Map<String, String>> prepareEEAnnotationsUnstructured( Collection<BioAssay> bioAssays,
            Collection<Characteristic> characs ) {
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();

        if ( bioAssays.isEmpty() ) throw new NotFoundException( "BioAssays not found" );
        for ( BioAssay bioAssay : bioAssays ) {

            String accession = bioAssay.getAccession().getAccession();

            Map<String, String> annotations = new HashMap<String, String>();

            BioMaterial bioMaterial = bioAssay.getSampleUsed();

            for ( FactorValue factorValue : bioMaterial.getFactorValues() ) {
                if ( factorValue.getExperimentalFactor().getName().equals( "batch" ) ) {
                    // skip batch
                } else {
                    annotations
                            .put( factorValue.getExperimentalFactor().getName(), getFactorValueString( factorValue ) );
                }

            }

            for ( Characteristic charac : characs ) {

                String[] tagStringArr = getTagString( charac );
                if ( !tagStringArr[0].isEmpty() && !tagStringArr[1].isEmpty() ) {
                    annotations.put( "constant_" + tagStringArr[0], tagStringArr[1] );
                }
            }
            result.put( accession, annotations );
        }

        return result;
    }

    private Map<String, Map<String, Map<String, String>>> prepareEEAnnotationsStructured(
            Collection<BioAssay> bioAssays, Collection<Characteristic> characs ) {
        Map<String, Map<String, Map<String, String>>> result = new HashMap<String, Map<String, Map<String, String>>>();

        if ( bioAssays.isEmpty() ) throw new NotFoundException( "BioAssays not found" );
        for ( BioAssay bioAssay : bioAssays ) {

            String accession = bioAssay.getAccession().getAccession();

            Map<String, Map<String, String>> annotationsCategories = new HashMap<String, Map<String, String>>();
            Map<String, String> annotations = new HashMap<String, String>();

            BioMaterial bioMaterial = bioAssay.getSampleUsed();

            for ( FactorValue factorValue : bioMaterial.getFactorValues() ) {
                if ( factorValue.getExperimentalFactor().getName().equals( "batch" ) ) {
                    // skip batch
                } else {
                    annotations
                            .put( factorValue.getExperimentalFactor().getName(), getFactorValueString( factorValue ) );
                }

            }

            Map<String, String> tagAnnotations = new HashMap<String, String>();

            for ( Characteristic charac : characs ) {

                String[] tagStringArr = getTagString( charac );
                if ( !tagStringArr[0].isEmpty() && !tagStringArr[1].isEmpty() ) {
                    tagAnnotations.put( tagStringArr[0], tagStringArr[1] );
                }
            }

            annotationsCategories.put( "ExperimentFactors", annotations );
            annotationsCategories.put( "ExperimentTags", tagAnnotations );
            result.put( accession, annotationsCategories );
        }

        return result;
    }

    @GET
    @Path("/getAllDatasetNames")
    @Produces(MediaType.APPLICATION_JSON)
    public List<String> getAllDatasetNames() {

        List<String> result = new LinkedList<String>();

        Collection<ExpressionExperiment> experiments = this.expressionExperimentService.loadAll();
        if ( experiments == null ) throw new NotFoundException( "No datasets were found." );

        for ( ExpressionExperiment experiment : experiments ) {
            result.add( experiment.getShortName() );

        }

        return result;
    }

    private String getFactorValueString( FactorValue fv ) {
        if ( fv == null ) return "null";

        if ( fv.getCharacteristics() != null && fv.getCharacteristics().size() > 0 ) {
            String fvString = "";
            for ( Characteristic c : fv.getCharacteristics() ) {
                fvString += c.getValue() + " ";
            }
            return fvString;
        } else if ( fv.getMeasurement() != null ) {
            return fv.getMeasurement().getValue();
        } else if ( fv.getValue() != null && !fv.getValue().isEmpty() ) {
            return fv.getValue();
        } else
            return "absent ";
    }

    private String[] getTagString( Characteristic characteristic ) {

        String[] arr = { "", "" };
        if ( characteristic == null ) return arr;
        if ( ( characteristic.getCategory() == null || characteristic.getCategory().isEmpty() )
                && ( characteristic.getValue() == null || characteristic.getValue().isEmpty() ) ) {
            return arr;
        } else if ( characteristic.getCategory() == null || characteristic.getCategory().isEmpty() ) {
            arr[0] = characteristic.getValue();
            arr[1] = characteristic.getValue();
        } else if ( characteristic.getValue() == null || characteristic.getValue().isEmpty() ) {
            arr[0] = characteristic.getCategory();
            arr[1] = "no value";
        } else {
            arr[0] = characteristic.getCategory();
            arr[1] = characteristic.getValue();
        }
        return arr;
    }

}
