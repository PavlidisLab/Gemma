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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ubic.gemma.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.job.executor.webapp.TaskRunningService;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.description.CharacteristicService;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.tasks.maintenance.CharacteristicUpdateCommand;
import ubic.gemma.util.AnchorTagUtil;
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
 * @version $Id$
 * @see ubic.gemma.web.controller.expression.experiment.AnnotationController for related methods.
 */
@Controller
public class CharacteristicBrowserController {

    private static final Log log = LogFactory.getLog( CharacteristicBrowserController.class.getName() );

    private static final int MAX_RESULTS = 1000;

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

    /**
     * @param valuePrefix
     * @return
     */
    public Collection<AnnotationValueObject> findCharacteristics( String valuePrefix ) {
        return findCharacteristicsCustom( valuePrefix, true, true, true, true, true, true, false );
    }

    /**
     * @param valuePrefix
     * @param searchNos
     * @param searchEEs
     * @param searchBMs
     * @param searchFVs
     * @param searchFFVs Search factor values that lack characteristics -- that is, search the factorValue.value.
     * @param searchCategories Should the Category be searched, not just the Value?
     * @return
     */
    public Collection<AnnotationValueObject> findCharacteristicsCustom( String valuePrefix, boolean searchNos,
            boolean searchEEs, boolean searchBMs, boolean searchFVs, boolean searchPAs, boolean searchFVVs,
            boolean searchCategories ) {

        List<AnnotationValueObject> results = new ArrayList<AnnotationValueObject>();
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
            if ( ( searchEEs && parent instanceof ExpressionExperiment )
                    || ( searchBMs && parent instanceof BioMaterial )
                    || ( searchFVs && ( parent instanceof FactorValue || parent instanceof ExperimentalFactor ) )
                    || ( searchNos && parent == null ) || ( searchPAs && parent instanceof PhenotypeAssociation ) ) {
                AnnotationValueObject avo = new AnnotationValueObject();
                avo.setId( c.getId() );
                avo.setClassName( c.getCategory() );
                avo.setTermName( c.getValue() );

                if ( c.getEvidenceCode() != null ) avo.setEvidenceCode( c.getEvidenceCode().toString() );

                if ( c instanceof VocabCharacteristic ) {
                    VocabCharacteristic vc = ( VocabCharacteristic ) c;
                    avo.setClassUri( vc.getCategoryUri() );
                    avo.setTermUri( vc.getValueUri() );
                    avo.setObjectClass( VocabCharacteristic.class.getSimpleName() );
                } else {
                    avo.setObjectClass( Characteristic.class.getSimpleName() );
                }

                if ( parent != null ) {
                    populateParentInformation( avo, parent );
                }

                results.add( avo );
            }
        }

        if ( searchFVVs ) { // non-characteristics.
            Collection<FactorValue> factorValues = factorValueService.findByValue( valuePrefix );
            for ( FactorValue factorValue : factorValues ) {
                if ( factorValue.getCharacteristics().size() > 0 ) continue;
                if ( StringUtils.isBlank( factorValue.getValue() ) ) continue;

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

    /**
     * @return
     */
    public Integer count() {
        return characteristicService.count();
    }

    /**
     * @param batch
     * @return
     */
    public JsonReaderResponse<AnnotationValueObject> browse( ListBatchCommand batch ) {
        Integer count = characteristicService.count(); // fixme: maybe don't do this every time. It's actually fast once
        // it's cached.

        List<AnnotationValueObject> results = new ArrayList<AnnotationValueObject>();

        Collection<Characteristic> records;
        if ( StringUtils.isNotBlank( batch.getSort() ) ) {

            String o = batch.getSort();

            String orderBy = "";
            if ( o.equals( "className" ) ) {
                orderBy = "category";
            } else if ( o.equals( "termName" ) ) {
                orderBy = "value";

            } else if ( o.equals( "evidenceCode" ) ) {
                orderBy = "evidenceCode";

            } else {
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

            if ( c.getEvidenceCode() != null ) avo.setEvidenceCode( c.getEvidenceCode().toString() );

            if ( c instanceof VocabCharacteristic ) {
                VocabCharacteristic vc = ( VocabCharacteristic ) c;
                avo.setClassUri( vc.getCategoryUri() );
                avo.setTermUri( vc.getValueUri() );
                avo.setObjectClass( VocabCharacteristic.class.getSimpleName() );
            } else {
                avo.setObjectClass( Characteristic.class.getSimpleName() );
            }

            if ( parent != null ) {
                populateParentInformation( avo, parent );
            }
            results.add( avo );
        }

        JsonReaderResponse<AnnotationValueObject> returnVal = new JsonReaderResponse<AnnotationValueObject>( results,
                count.intValue() );
        return returnVal;
    }

    /**
     * @return
     */
    @RequestMapping(value = "/characteristicBrowser.html", method = RequestMethod.GET)
    public String getView() {
        return "characteristics";
    }

    /**
     * @param chars
     * @return taskId
     */
    public void removeCharacteristics( Collection<AnnotationValueObject> chars ) {
        CharacteristicUpdateCommand c = new CharacteristicUpdateCommand();
        c.setAnnotationValueObjects( chars );
        c.setRemove( true );
        taskRunningService.submitLocalTask( c );
    }

    /**
     * Update characteristics associated with entities. This allows for the case of factor values that we are adding
     * characteristics to for the first time, but the most common case is altering existing characteristics.
     * 
     * @param avos
     */
    public void updateCharacteristics( Collection<AnnotationValueObject> avos ) {
        CharacteristicUpdateCommand c = new CharacteristicUpdateCommand();
        c.setAnnotationValueObjects( avos );
        c.setRemove( false );
        taskRunningService.submitLocalTask( c );
    }

    /**
     * @param avo
     * @param parent
     */
    private void populateParentInformation( AnnotationValueObject avo, Object parent ) {
        if ( parent == null ) {
            avo.setParentLink( "[Parent hidden or not available, " + avo.getObjectClass() + " ID=" + avo.getId() + "]" );
        } else if ( parent instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) parent;
            avo.setParentName( String.format( "Experiment: %s", ee.getName() ) );
            avo.setParentLink( AnchorTagUtil.getExpressionExperimentLink( ee.getId(), avo.getParentName() ) );
        } else if ( parent instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) parent;
            avo.setParentName( String.format( "BioMat: %s", bm.getName() ) );
            avo.setParentLink( AnchorTagUtil.getBioMaterialLink( bm.getId(), avo.getParentName() ) );
            ExpressionExperiment ee = expressionExperimentService.findByBioMaterial( bm );

            if ( ee != null ) {
                avo.setParentOfParentName( String.format( "%s", ee.getName() ) );
                // avo.setParentOfParentDescription( ee.getDescription() );
                avo.setParentOfParentLink( AnchorTagUtil.getExpressionExperimentLink( ee.getId(),
                        avo.getParentOfParentName() ) );
            } else {
                log.warn( "Expression experiment for " + bm + " was null" );
            }
        } else if ( parent instanceof FactorValue ) {
            FactorValue fv = ( FactorValue ) parent;
            avo.setParentDescription( String.format( "FactorValue: %s &laquo; Exp.Factor: %s",
                    ( fv.getValue() == null ? "" : ": " + fv.getValue() ), fv.getExperimentalFactor().getName() ) );
            ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( fv.getExperimentalFactor()
                    .getExperimentalDesign() );
            avo.setParentOfParentName( String.format( "Experimental Design for: %s", ee.getName() ) );
            avo.setParentOfParentLink( AnchorTagUtil.getExperimentalDesignLink( fv.getExperimentalFactor()
                    .getExperimentalDesign().getId(), avo.getParentName() )
                    + "&nbsp;&laquo;&nbsp;"
                    + AnchorTagUtil.getExpressionExperimentLink( ee.getId(),
                            String.format( "%s (%s)", StringUtils.abbreviate( ee.getName(), 80 ), ee.getShortName() ) ) );
        } else if ( parent instanceof ExperimentalFactor ) {
            ExperimentalFactor ef = ( ExperimentalFactor ) parent;
            avo.setParentLink( AnchorTagUtil.getExperimentalDesignLink( ef.getExperimentalDesign().getId(), "Exp Fac: "
                    + ef.getName() + " (" + StringUtils.abbreviate( ef.getDescription(), 50 ) + ")" ) );
            ExpressionExperiment ee = experimentalDesignService.getExpressionExperiment( ef.getExperimentalDesign() );
            avo.setParentOfParentName( String.format( "%s (%s)", StringUtils.abbreviate( ee.getName(), 80 ),
                    ee.getShortName() ) );
            avo.setParentOfParentLink( AnchorTagUtil.getExpressionExperimentLink( ee.getId(),
                    avo.getParentOfParentName() ) );
        } else if ( parent instanceof PhenotypeAssociation ) {
            PhenotypeAssociation pa = ( PhenotypeAssociation ) parent;
            avo.setParentLink( "PhenotypeAssociation" );
            avo.setParentDescription( pa.getId().toString() );

        }
    }

}
