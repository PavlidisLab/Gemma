/*
 * The Gemma project
 *
 * Copyright (c) 2008 University of British Columbia
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
package ubic.gemma.web.controller.common;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.ejb.criteria.expression.function.AggregationFunction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ubic.gemma.core.job.executor.webapp.TaskRunningService;
import ubic.gemma.core.tasks.maintenance.CharacteristicUpdateCommand;
import ubic.gemma.core.util.AnchorTagUtil;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * NOTE: Logging messages from this service are important for tracking changes to annotations.
 *
 * @author luke
 * @author paul
 * @see ubic.gemma.web.controller.expression.experiment.AnnotationController for related methods.
 */
@Controller
public class CharacteristicBrowserController {

    private static final Log log = LogFactory.getLog( CharacteristicBrowserController.class.getName() );

    private static final int MAX_RESULTS = 2000;

    @Autowired
    private TaskRunningService taskRunningService;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private ExpressionExperimentService expressionExperimentService;

    @Autowired
    private FactorValueService factorValueService;

    @Autowired
    private CharacteristicService characteristicService;

    public JsonReaderResponse<AnnotationValueObject> browse( ListBatchCommand batch ) {
        Integer count = characteristicService.countAll();

        List<AnnotationValueObject> results = new ArrayList<>();

        Collection<Characteristic> records;
        if ( StringUtils.isNotBlank( batch.getSort() ) ) {

            String o = batch.getSort();

            String orderBy;
            switch ( o ) {
                case "className":
                    orderBy = "category";
                    break;
                case "termName":
                    orderBy = "value";
                    break;
                case "evidenceCode":
                    orderBy = "evidenceCode";
                    break;
                default:
                    throw new IllegalArgumentException( "Unknown sort field: " + o );
            }

            boolean descending = batch.getDir() != null && batch.getDir().equalsIgnoreCase( "DESC" );
            records = characteristicService.browse( batch.getStart(), batch.getLimit(), orderBy, descending );

        } else {
            records = characteristicService.browse( batch.getStart(), batch.getLimit() );
        }

        Map<Characteristic, Object> charToParent = characteristicService.getParents( records );

        for ( Object o : records ) {
            Characteristic c = ( Characteristic ) o;
            Object parent = charToParent.get( c );

            AnnotationValueObject avo = new AnnotationValueObject();
            avo.setId( c.getId() );
            avo.setClassName( c.getCategory() );
            avo.setTermName( c.getValue() );

            if ( c.getEvidenceCode() != null )
                avo.setEvidenceCode( c.getEvidenceCode().toString() );

            populateClassValues( c, avo );

            if ( parent != null ) {
                populateParentInformation( avo, parent );
            }
            results.add( avo );
        }

        return new JsonReaderResponse<>( results, count );
    }

    private void populateClassValues( Characteristic c, AnnotationValueObject avo ) {
        avo.setClassUri( c.getCategoryUri() );
        avo.setTermUri( c.getValueUri() );
        avo.setObjectClass( Characteristic.class.getSimpleName() );
    }

    public Integer count() {
        return characteristicService.countAll();
    }

    public Collection<AnnotationValueObject> findCharacteristics( String valuePrefix ) {
        return findCharacteristicsCustom( valuePrefix, true, true, true, true, true, true, false );
    }

    /**
     * @param searchFVs        Search factor values that lack characteristics -- that is, search the factorValue.value.
     * @param searchCategories Should the Category be searched, not just the Value?
     */
    public Collection<AnnotationValueObject> findCharacteristicsCustom( String valuePrefix, boolean searchNos,
            boolean searchEEs, boolean searchBMs, boolean searchFVs, boolean searchPAs, boolean searchFVVs,
            boolean searchCategories ) {

        List<AnnotationValueObject> results = new ArrayList<>();
        if ( StringUtils.isBlank( valuePrefix ) ) {
            return results;
        }
        Collection<Characteristic> chars = characteristicService.findByValue( valuePrefix );

        if ( searchCategories ) {
            chars.addAll( characteristicService.findByCategory( valuePrefix ) );
        }

        Map<Characteristic, Object> charToParent = characteristicService.getParents( chars );
        for ( Object o : chars ) {
            Characteristic c = ( Characteristic ) o;
            Object parent = charToParent.get( c );

            if ( ( searchEEs && parent instanceof ExpressionExperiment ) || ( searchBMs
                    && parent instanceof BioMaterial )
                    || ( searchFVs && ( parent instanceof FactorValue
                    || parent instanceof ExperimentalFactor ) )
                    || ( searchNos && parent == null ) || ( searchPAs
                    && parent instanceof PhenotypeAssociation ) ) {

                AnnotationValueObject avo = new AnnotationValueObject();
                avo.setId( c.getId() );
                avo.setClassName( c.getCategory() );
                avo.setTermName( c.getValue() );

                if ( c.getEvidenceCode() != null )
                    avo.setEvidenceCode( c.getEvidenceCode().toString() );

                populateClassValues( c, avo );

                if ( parent != null ) {
                    populateParentInformation( avo, parent );
                }

                results.add( avo );

                if ( results.size() >= MAX_RESULTS ) {
                    break;
                }
            }
        }

        if ( results.size() < MAX_RESULTS && searchFVVs ) { // non-characteristics.
            Collection<FactorValue> factorValues = factorValueService.findByValue( valuePrefix );
            for ( FactorValue factorValue : factorValues ) {
                if ( factorValue.getCharacteristics().size() > 0 )
                    continue;
                if ( StringUtils.isBlank( factorValue.getValue() ) )
                    continue;

                AnnotationValueObject avo = new AnnotationValueObject();

                avo.setId( factorValue.getId() );
                avo.setTermName( factorValue.getValue() );
                avo.setObjectClass( FactorValue.class.getSimpleName() );

                populateParentInformation( avo, factorValue );

                results.add( avo );
            }

        }

        log.info( "Characteristic search for: '" + valuePrefix + "*': " + results.size() + " results, returning up to "
                + MAX_RESULTS );
        return results.subList( 0, Math.min( results.size(), MAX_RESULTS ) );
    }

    @RequestMapping(value = "/characteristicBrowser.html", method = RequestMethod.GET)
    public String getView() {
        return "characteristics";
    }

    public void removeCharacteristics( Collection<AnnotationValueObject> chars ) {
        CharacteristicUpdateCommand c = new CharacteristicUpdateCommand();
        c.setAnnotationValueObjects( chars );
        c.setRemove( true );
        taskRunningService.submitLocalTask( c );
    }

    /**
     * Update characteristics associated with entities. This allows for the case of factor values that we are adding
     * characteristics to for the first time, but the most common case is altering existing characteristics.
     */
    public void updateCharacteristics( Collection<AnnotationValueObject> avos ) {
        CharacteristicUpdateCommand c = new CharacteristicUpdateCommand();
        c.setAnnotationValueObjects( avos );
        c.setRemove( false );
        taskRunningService.submitLocalTask( c );
    }

    /**
     *
     * @param avo
     * @param annotatedItem - the object that has the annotation, we want to find who "owns" it.
     */
    private void populateParentInformation( AnnotationValueObject avo, Object annotatedItem ) {

        assert avo != null;

        if ( annotatedItem == null ) {
            avo.setParentLink(
                    "[Parent hidden or not available, " + avo.getObjectClass() + " ID=" + avo.getId() + "]" );
        } else if ( annotatedItem instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) annotatedItem;
            avo.setParentName( String.format( "Experiment: %s", ee.getName() ) );
            avo.setParentLink( AnchorTagUtil.getExpressionExperimentLink( ee.getId(), avo.getParentName() ) );
        } else if ( annotatedItem instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) annotatedItem;
            avo.setParentName( String.format( "BioMat: %s", bm.getName() ) );
            avo.setParentLink( AnchorTagUtil.getBioMaterialLink( bm.getId(), avo.getParentName() ) );
            ExpressionExperiment ee = expressionExperimentService.findByBioMaterial( bm );

            if ( ee == null ) {
                log.warn( "Expression experiment for " + bm + " was null" );
                return;
            }
            avo.setParentOfParentName( String.format( "%s", ee.getName() ) );
            // avo.setParentOfParentDescription( ee.getDescription() );
            avo.setParentOfParentLink(
                    AnchorTagUtil.getExpressionExperimentLink( ee.getId(), avo.getParentOfParentName() ) );

        } else if ( annotatedItem instanceof FactorValue ) {
            FactorValue fv = ( FactorValue ) annotatedItem;
            avo.setParentDescription( String.format( "FactorValue: %s &laquo; Exp.Factor: %s",
                    ( fv.getValue() == null ? "" : ": " + fv.getValue() ), fv.getExperimentalFactor().getName() ) );
            ExpressionExperiment ee = expressionExperimentService.findByFactorValue( fv );


            if ( ee == null ) {
                log.warn( "Expression experiment for " + fv + " was null" );
                return;
            }
            avo.setParentOfParentName( String.format( "Experimental Design for: %s", ee.getName() ) );
            avo.setParentOfParentLink( AnchorTagUtil
                    .getExperimentalDesignLink( fv.getExperimentalFactor().getExperimentalDesign().getId(),
                            avo.getParentName() )
                    + "&nbsp;&laquo;&nbsp;" + AnchorTagUtil
                    .getExpressionExperimentLink( ee.getId(),
                            String.format( "%s (%s)", StringUtils.abbreviate( ee.getName(), 80 ),
                                    ee.getShortName() ) ) );
        } else if ( annotatedItem instanceof ExperimentalFactor ) {
            ExperimentalFactor ef = ( ExperimentalFactor ) annotatedItem;
            avo.setParentLink( AnchorTagUtil.getExperimentalDesignLink( ef.getExperimentalDesign().getId(),
                    "Exp Fac: " + ef.getName() + " (" + StringUtils.abbreviate( ef.getDescription(), 50 ) + ")" ) );
            ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ef.getExperimentalDesign() );
            if ( ee == null ) {
                log.warn( "Expression experiment for " + ef + " was null" );
                return;
            }
            avo.setParentOfParentName(
                    String.format( "%s (%s)", StringUtils.abbreviate( ee.getName(), 80 ), ee.getShortName() ) );
            avo.setParentOfParentLink(
                    AnchorTagUtil.getExpressionExperimentLink( ee.getId(), avo.getParentOfParentName() ) );
        } else if ( annotatedItem instanceof PhenotypeAssociation ) {
            PhenotypeAssociation pa = ( PhenotypeAssociation ) annotatedItem;
            avo.setParentLink( "PhenotypeAssoc: " + pa.getGene().getOfficialSymbol() );
            avo.setParentDescription( pa.getId().toString() );
        }
    }

}
