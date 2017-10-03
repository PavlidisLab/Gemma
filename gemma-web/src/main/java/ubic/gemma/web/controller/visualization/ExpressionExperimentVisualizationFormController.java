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
package ubic.gemma.web.controller.visualization;

import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.ServletRequestDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import ubic.gemma.core.analysis.preprocess.ExpressionDataMatrixBuilder;
import ubic.gemma.core.analysis.service.CompositeSequenceGeneMapperService;
import ubic.gemma.core.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.core.datastructure.matrix.ExpressionDataMatrixRowElement;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.bioAssayData.DesignElementDataVector;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.genome.Gene;
import ubic.gemma.persistence.service.expression.bioAssayData.DesignElementDataVectorService;
import ubic.gemma.persistence.service.expression.designElement.CompositeSequenceService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.web.controller.BaseFormController;
import ubic.gemma.web.propertyeditor.QuantitationTypePropertyEditor;
import ubic.gemma.web.util.ConfigurationCookie;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;

/**
 * A SimpleFormController providing search functionality of genes or design elements (probe sets). The
 * success view returns either a visual representation of the result set or a downloadable data file.
 * viewSampling sets whether or not just some randomly selected vectors will be shown, and species sets
 * the type of species to search. keywords restrict the search.
 * maskMissing masks the missing values.
 *
 * @author keshav
 */
@SuppressWarnings("unused") // Used in front end
@Deprecated
public class ExpressionExperimentVisualizationFormController extends BaseFormController {

    public static final String SEARCH_BY_PROBE = "probe set id";
    public static final String SEARCH_BY_GENE = "gene symbol";
    private static final String SEARCH_CRITERIA = "searchCriteria";
    private static final String COOKIE_NAME = "expressionExperimentVisualizationCookie";
    private static final int MAX_ELEMENTS_TO_VISUALIZE = 200;
    private ExpressionExperimentService expressionExperimentService = null;
    private CompositeSequenceService compositeSequenceService = null;
    private DesignElementDataVectorService designElementDataVectorService;
    private CompositeSequenceGeneMapperService compositeSequenceGeneMapperService = null;

    public ExpressionExperimentVisualizationFormController() {
        /*
         * if true, reuses the same command object across the edit-submit-process (get-post-process).
         */
        setSessionForm( true );
    }

    @Override
    public ModelAndView onSubmit( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        ExpressionExperimentVisualizationCommand eevc = ( ( ExpressionExperimentVisualizationCommand ) command );

        /* store user choices from command object in a cookie. */
        Cookie cookie = new ExpressionExperimentVisualizationCookie( eevc );
        response.addCookie( cookie );

        Long id = eevc.getExpressionExperimentId();

        ExpressionExperiment expressionExperiment = this.expressionExperimentService.load( id );
        expressionExperiment = expressionExperimentService.thawLite( expressionExperiment );

        if ( expressionExperiment == null ) {
            return processErrors( request, response, command, errors,
                    "No expression experiment with id " + id + " found" );
        }
        //
        // for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
        // bioAssayService.thaw( ba );
        // }

        QuantitationType quantitationType = eevc.getQuantitationType();
        if ( quantitationType == null ) {
            return processErrors( request, response, command, errors, "Quantitation type must be provided" );
        }

        Collection<DesignElementDataVector> dataVectors = getVectors( command, errors, eevc, expressionExperiment,
                quantitationType );

        if ( errors.hasErrors() ) {
            return processErrors( request, response, command, errors, null );
        }

        designElementDataVectorService.thaw( dataVectors );

        ExpressionDataMatrixBuilder matrixBuilder = new ExpressionDataMatrixBuilder( dataVectors );
        ExpressionDataDoubleMatrix expressionDataMatrix;

        if ( eevc.isMaskMissing() ) {
            expressionDataMatrix = matrixBuilder.getProcessedData();
        } else {
            expressionDataMatrix = matrixBuilder.getPreferredData();
        }

        /*
         * deals with the case where probes don't match for the given quantitation type, or we lack a preferred data
         * type, or other calamaties.
         */
        if ( expressionDataMatrix == null || expressionDataMatrix.rows() == 0 ) {
            String message =
                    "None of the probe sets match the given quantitation type " + quantitationType.getType().getValue();

            return processErrors( request, response, command, errors, message );
        }

        Map<CompositeSequence, Collection<Gene>> genes = getGenes( expressionDataMatrix ); // this will slow things
        // down.

        /* return the model and view */
        ModelAndView mav = new ModelAndView( getSuccessView() );
        mav.addObject( "expressionDataMatrix", expressionDataMatrix );
        mav.addObject( "genes", genes );
        mav.addObject( "expressionExperiment", expressionExperiment );
        mav.addObject( "quantitationType", eevc.getQuantitationType() );
        mav.addObject( SEARCH_CRITERIA, eevc.getSearchCriteria() );
        mav.addObject( "searchString", eevc.getSearchString() );
        mav.addObject( "viewSampling", eevc.isViewSampling() );
        mav.addObject( "maskMissing", eevc.isMaskMissing() );
        return mav;
    }

    @Override
    public ModelAndView processFormSubmission( HttpServletRequest request, HttpServletResponse response, Object command,
            BindException errors ) throws Exception {

        ExpressionExperimentVisualizationCommand eevc = ( ( ExpressionExperimentVisualizationCommand ) command );
        Long id = eevc.getExpressionExperimentId();

        if ( request.getParameter( "cancel" ) != null ) {
            log.info( "Cancelled" );

            if ( id != null ) {
                return new ModelAndView(
                        new RedirectView( "/Gemma/expressionExperiment/showExpressionExperiment.html?id=" + id ) );
            }

            log.warn( "Cannot find details view due to null id.  Redirecting to overview" );
            return new ModelAndView(
                    new RedirectView( "/Gemma/expressionExperiment/showAllExpressionExperiments.html" ) );

        }

        return super.processFormSubmission( request, response, command, errors );
    }

    /**
     * @param compositeSequenceGeneMapperService The compositeSequenceGeneMapperService to set.
     */
    public void setCompositeSequenceGeneMapperService(
            CompositeSequenceGeneMapperService compositeSequenceGeneMapperService ) {
        this.compositeSequenceGeneMapperService = compositeSequenceGeneMapperService;
    }

    public void setCompositeSequenceService( CompositeSequenceService compositeSequenceService ) {
        this.compositeSequenceService = compositeSequenceService;
    }

    public void setDesignElementDataVectorService( DesignElementDataVectorService designElementDataVectorService ) {
        this.designElementDataVectorService = designElementDataVectorService;
    }

    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    @Override
    protected Object formBackingObject( HttpServletRequest request ) {

        Long id;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Id was not valid Long integer", e );
        }

        ExpressionExperiment ee;
        ExpressionExperimentVisualizationCommand eevc = new ExpressionExperimentVisualizationCommand();

        if ( StringUtils.isNotBlank( id.toString() ) ) {
            ee = expressionExperimentService.load( id );
        } else {
            ee = ExpressionExperiment.Factory.newInstance();
        }

        eevc.setExpressionExperimentId( ee.getId() );
        eevc.setName( ee.getName() );

        if ( StringUtils.isBlank( request.getParameter( SEARCH_CRITERIA ) ) ) {
            eevc = loadCookie( request, eevc );
        }

        return eevc;

    }

    private Collection<DesignElementDataVector> getVectors( Object command, BindException errors,
            ExpressionExperimentVisualizationCommand eevc, ExpressionExperiment expressionExperiment,
            QuantitationType quantitationType ) {

        Collection<DesignElementDataVector> vectors;

        Collection<CompositeSequence> compositeSequences = null;

        boolean viewSampling = ( ( ExpressionExperimentVisualizationCommand ) command ).isViewSampling();

        Collection<ArrayDesign> arrayDesigns = expressionExperimentService.getArrayDesignsUsed( expressionExperiment );

        /* check size if 'viewSampling' is set. */
        if ( viewSampling ) {
            vectors = expressionExperimentService.getSamplingOfVectors( quantitationType, MAX_ELEMENTS_TO_VISUALIZE );
        } else {
            String searchString = eevc.getSearchString();

            String[] searchIds = StringUtils.split( searchString, "," );
            if ( searchIds.length > MAX_ELEMENTS_TO_VISUALIZE ) {
                String message = "Max elements to search for is " + MAX_ELEMENTS_TO_VISUALIZE;
                log.error( message );
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
                return null;
            }

            List<String> searchIdsAsList = Arrays.asList( searchIds );

            /* handle search by design element */
            if ( eevc.getSearchCriteria().equalsIgnoreCase( SEARCH_BY_PROBE ) ) {

                if ( checkIfPlatformsExistAndErrorIfNot( command, errors, expressionExperiment, arrayDesigns ) )
                    return null;

                compositeSequences = compositeSequenceService
                        .findByNamesInArrayDesigns( searchIdsAsList, arrayDesigns );

            } else if ( eevc.getSearchCriteria().equalsIgnoreCase( SEARCH_BY_GENE ) ) {
                /* search by gene */
                if ( checkIfPlatformsExistAndErrorIfNot( command, errors, expressionExperiment, arrayDesigns ) )
                    return null;

                compositeSequences = getProbesByGeneSymbols( arrayDesigns, searchIdsAsList );
            }

            if ( compositeSequences == null || compositeSequences.size() == 0 ) {
                String message = "Genes/Probes could not be found.";
                log.error( message );
                errors.addError( new ObjectError( command.toString(), null, null, message ) );
                return null;
            }

            vectors = expressionExperimentService.getDesignElementDataVectors( compositeSequences, quantitationType );
        }
        if ( vectors == null || vectors.size() == 0 ) {
            errors.addError( new ObjectError( command.toString(), null, null, "No data could be found." ) );
        }
        return vectors;
    }

    private boolean checkIfPlatformsExistAndErrorIfNot( Object command, BindException errors,
            ExpressionExperiment expressionExperiment, Collection<ArrayDesign> arrayDesigns ) {
        if ( arrayDesigns.size() == 0 ) {
            String message = "No platforms found for " + expressionExperiment;
            log.error( message );
            errors.addError( new ObjectError( command.toString(), null, null, message ) );
            return true;
        }
        return false;
    }

    @Override
    @InitBinder
    protected void initBinder( HttpServletRequest request, ServletRequestDataBinder binder ) {
        super.initBinder( binder );
        binder.registerCustomEditor( QuantitationType.class,
                new QuantitationTypePropertyEditor( getContinuousQuantitationTypes( request ) ) );
    }

    /**
     * Populates drop downs.
     *
     * @param request request
     * @return map
     */
    @Override
    protected Map<String, List<?>> referenceData( HttpServletRequest request ) {

        Map<String, List<?>> searchByMap = new HashMap<>();
        List<String> searchCategories = new ArrayList<>();
        searchCategories.add( SEARCH_BY_GENE );
        searchCategories.add( SEARCH_BY_PROBE );
        searchByMap.put( "searchCategories", searchCategories );

        Collection<QuantitationType> types = getContinuousQuantitationTypes( request );
        List<QuantitationType> listedTypes = new ArrayList<>();
        listedTypes.addAll( types );

        searchByMap.put( "quantitationTypes", listedTypes );

        return searchByMap;
    }

    private Collection<QuantitationType> getContinuousQuantitationTypes( HttpServletRequest request ) {
        Long id;
        try {
            id = Long.parseLong( request.getParameter( "id" ) );
        } catch ( NumberFormatException e ) {
            throw new RuntimeException( "Id was not valid Long integer", e );
        }
        ExpressionExperiment expressionExperiment = this.expressionExperimentService.load( id );
        Collection<QuantitationType> types = expressionExperimentService.getQuantitationTypes( expressionExperiment );
        Iterator<QuantitationType> iter = types.iterator();
        while ( iter.hasNext() ) {
            QuantitationType type = iter.next();
            if ( !type.getGeneralType().equals( GeneralType.QUANTITATIVE ) ) {
                iter.remove();
            }
        }
        return types;
    }

    private Map<CompositeSequence, Collection<Gene>> getGenes( ExpressionDataDoubleMatrix expressionDataMatrix ) {
        Collection<CompositeSequence> css = new HashSet<>();
        for ( ExpressionDataMatrixRowElement el : expressionDataMatrix.getRowElements() ) {
            CompositeSequence cs = el.getDesignElement();
            css.add( cs );
        }
        return compositeSequenceService.getGenes( css );
    }

    private Collection<CompositeSequence> getProbesByGeneSymbols( Collection<ArrayDesign> arrayDesigns,
            List<String> searchIdsAsList ) {
        Collection<CompositeSequence> compositeSequences;
        Map<Gene, Collection<CompositeSequence>> genes2Probes = compositeSequenceGeneMapperService
                .getGene2ProbeMapByOfficialSymbols( searchIdsAsList, arrayDesigns );

        compositeSequences = new HashSet<>();
        for ( Gene g : genes2Probes.keySet() ) {
            compositeSequences.addAll( genes2Probes.get( g ) );
            log.debug( "gene official symbol: " + g.getOfficialSymbol() + " has " + compositeSequences.size()
                    + " composite sequences associated with it." );
        }
        return compositeSequences;
    }

    /**
     * @param request request
     * @param eevc    eevc to add the cookie to
     * @return given eevc with a cookie to store the user preferences.
     */
    private ExpressionExperimentVisualizationCommand loadCookie( HttpServletRequest request,
            ExpressionExperimentVisualizationCommand eevc ) {

        Collection<QuantitationType> quantitationTypes = getContinuousQuantitationTypes( request );

        /*
         * If we don't have any cookies, just return. We probably won't get this situation as we'll always have at least
         * one cookie (the one with the JSESSION ID).
         */
        if ( request == null || request.getCookies() == null ) {
            return null;
        }

        for ( Cookie cook : request.getCookies() ) {
            if ( cook.getName().equals( COOKIE_NAME ) ) {
                try {
                    ConfigurationCookie cookie = new ConfigurationCookie( cook );
                    eevc.setSearchString( cookie.getString( "searchString" ) );
                    eevc.setSearchCriteria( cookie.getString( SEARCH_CRITERIA ) );
                    eevc.setViewSampling( cookie.getBoolean( "viewSampling" ) );
                    eevc.setMaskMissing( cookie.getBoolean( "maskMissing" ) );

                    /* determine which quantitation type was previously selected */
                    String qtName = cookie.getString( "quantitationTypeName" );
                    for ( QuantitationType qt : quantitationTypes ) {
                        if ( StringUtils.equals( qtName, qt.getName() ) ) {
                            eevc.setQuantitationType( qt );
                            return eevc;
                        }
                    }
                } catch ( Exception e ) {
                    log.warn( "Cookie could not be loaded: " + e.getMessage() );
                    // that's okay, we just don't get a cookie.
                }
            }
        }

        /* If we've come this far, we have a cookie but not one that matches COOKIE_NAME. Provide friendly defaults. */
        if ( quantitationTypes.size() > 0 ) {
            QuantitationType qt = quantitationTypes.iterator().next();
            eevc.setQuantitationType( qt );
        } else {
            throw new RuntimeException( "No continuous-valued quantitation types" );
        }

        eevc.setSearchString( "gene symbol 1, gene symbol 2" );

        eevc.setViewSampling( true );
        return eevc;
    }

    /**
     * @author keshav
     */
    static class ExpressionExperimentVisualizationCookie extends ConfigurationCookie {

        public ExpressionExperimentVisualizationCookie( ExpressionExperimentVisualizationCommand command ) {

            super( COOKIE_NAME );

            log.debug( "creating cookie" );

            this.setProperty( "searchString", command.getSearchString() );
            this.setProperty( "viewSampling", command.isViewSampling() );
            this.setProperty( "maskMissing", command.isMaskMissing() );
            this.setProperty( SEARCH_CRITERIA, command.getSearchCriteria() );
            this.setProperty( "quantitationTypeName", command.getQuantitationType().getName() );

            /* set cookie to expire after 2 days. */
            this.setMaxAge( 172800 );
            this.setComment( "User selections for visualization form" );
        }

    }
}

class FactorValueComparator implements Comparator<FactorValue> {

    @Override
    public int compare( FactorValue arg0, FactorValue arg1 ) {
        if ( arg0.getMeasurement() != null && arg1.getMeasurement() != null ) {
            return ( new MeasurementComparator() ).compare( arg0.getMeasurement(), arg1.getMeasurement() );
        } else if ( arg0.getCharacteristics().size() > 0 && arg1.getCharacteristics().size() > 0 ) {
            // FIXME implement real comparison.
            // return CharacteristicUtils.compare( arg0.getCharacteristics().size(), arg1.getCharacteristics().size() );
            return -1;
        } else if ( arg0.getValue() != null && arg1.getValue() != null ) {
            return arg0.getValue().compareTo( arg1.getValue() );
        } else {
            return arg0.getId().compareTo( arg1.getId() ); // fallback.
        }
    }
}

class MeasurementComparator implements Comparator<Measurement> {

    @Override
    public int compare( Measurement o1, Measurement o2 ) {
        PrimitiveType ptype = o1.getRepresentation();
        if ( ptype.equals( PrimitiveType.STRING ) || ptype.equals( PrimitiveType.BOOLEAN ) ) {
            return o1.getValue().compareTo( o2.getValue() );
        } else if ( ptype.equals( PrimitiveType.DOUBLE ) ) {
            Double d1 = Double.parseDouble( o1.getValue() );
            Double d2 = Double.parseDouble( o2.getValue() );
            return d1.compareTo( d2 );
        } else if ( ptype.equals( PrimitiveType.INT ) ) {
            Integer d1 = Integer.parseInt( o1.getValue() );
            Integer d2 = Integer.parseInt( o2.getValue() );
            return d1.compareTo( d2 );
        } else {
            throw new UnsupportedOperationException( "Don't know how to compare " + ptype + "'s" );
        }
    }
}