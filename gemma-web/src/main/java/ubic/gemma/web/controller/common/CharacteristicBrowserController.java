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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import ubic.gemma.core.job.TaskRunningService;
import ubic.gemma.core.tasks.maintenance.CharacteristicUpdateCommand;
import ubic.gemma.model.association.phenotype.PhenotypeAssociation;
import ubic.gemma.model.common.Identifiable;
import ubic.gemma.model.common.description.AnnotationValueObject;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueUtils;
import ubic.gemma.persistence.service.common.description.CharacteristicService;
import ubic.gemma.persistence.service.expression.experiment.FactorValueService;
import ubic.gemma.web.remote.JsonReaderResponse;
import ubic.gemma.web.remote.ListBatchCommand;
import ubic.gemma.web.util.AnchorTagUtil;

import javax.annotation.Nullable;
import javax.servlet.ServletContext;
import java.util.*;

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
    private FactorValueService factorValueService;

    @Autowired
    private CharacteristicService characteristicService;

    @Autowired
    private ServletContext servletContext;

    public JsonReaderResponse<AnnotationValueObject> browse( ListBatchCommand batch ) {
        long count = characteristicService.countAll();

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

        Map<Characteristic, Identifiable> charToParent = characteristicService.getParents( records, null, -1 );

        for ( Characteristic o : records ) {
            Identifiable parent = charToParent.get( o );

            AnnotationValueObject avo = new AnnotationValueObject( o, Characteristic.class );

            if ( parent != null ) {
                populateParentInformation( avo, parent );
            }
            results.add( avo );
        }

        return new JsonReaderResponse<>( results, ( int ) count );
    }

    public Long count() {
        return characteristicService.countAll();
    }

    public Collection<AnnotationValueObject> findCharacteristics( String valuePrefix ) {
        return findCharacteristicsCustom( valuePrefix, true, true, true, true, true, false, null );
    }

    /**
     * @param searchFVs        Search factor values that lack characteristics -- that is, search the factorValue.value.
     * @param searchCategories Should the Category be searched, not just the Value?
     */
    public Collection<AnnotationValueObject> findCharacteristicsCustom( String queryString, boolean searchNos,
            boolean searchEEs, boolean searchBMs, boolean searchFVs, boolean searchFVVs,
            boolean searchCategories, String categoryConstraint ) {

        boolean searchEfs = true; // fixme, make this optional

        queryString = queryString.trim();

        List<AnnotationValueObject> results = new ArrayList<>();
        if ( StringUtils.isBlank( queryString ) ) {
            return results;
        }

        Collection<Characteristic> chars = new HashSet<>();
        if ( queryString.startsWith( "http://" ) ) {
            chars = characteristicService.findByUri( queryString );
            if ( searchCategories ) {
                chars.addAll( characteristicService.findByCategoryStartingWith( queryString ) );
                chars.addAll( characteristicService.findByCategoryUri( queryString ) );
            }
        } else {
            chars = characteristicService.findByValueStartingWith( queryString );

            if ( searchCategories ) {
                chars.addAll( characteristicService.findByCategoryStartingWith( queryString ) );
                chars.addAll( characteristicService.findByCategoryUri( queryString ) );
            }
        }

        Collection<Class<?>> parentClasses = new HashSet<>();
        if ( searchEEs ) {
            parentClasses.add( ExpressionExperiment.class );
        }
        if ( searchBMs ) {
            parentClasses.add( BioMaterial.class );
        }
        if ( searchFVs ) {
            parentClasses.add( FactorValue.class );
        }

        if ( searchEfs ) {
            parentClasses.add( ExperimentalFactor.class );
        }

        Map<Characteristic, Identifiable> charToParent = characteristicService.getParents( chars, parentClasses, MAX_RESULTS );

        // from here we only use the characteristics which were returned by getParents, which has the MAX_RESULTS limit.
        for ( Characteristic c : charToParent.keySet() ) {

            if ( StringUtils.isNotBlank( categoryConstraint ) && ( c.getCategory() == null || !c.getCategory().equalsIgnoreCase( categoryConstraint ) ) ) {
                continue;
            }

            Identifiable parent = charToParent.get( c );
            if ( parent == null && !searchNos ) {
                continue;
            }
            AnnotationValueObject avo = new AnnotationValueObject( c, Characteristic.class );
            if ( parent != null ) {
                populateParentInformation( avo, parent );
            }


            results.add( avo );
        }

        // This might not do anything
        if ( results.size() < MAX_RESULTS && searchFVVs ) { // non-characteristics.
            Collection<FactorValue> factorValues = factorValueService.findByValue( queryString );
            for ( FactorValue factorValue : factorValues ) {
                if ( !factorValue.getCharacteristics().isEmpty() )
                    continue;

                AnnotationValueObject avo = new AnnotationValueObject( factorValue.getId() );
                avo.setTermName( FactorValueUtils.getSummaryString( factorValue ) );
                avo.setObjectClass( FactorValue.class.getSimpleName() );

                populateParentInformation( avo, factorValue );

                results.add( avo );
            }

        }

        log.info( "Characteristic search for: '" + queryString + "*': " + results.size() + " results, returning up to "
                + MAX_RESULTS );
        return results.subList( 0, Math.min( results.size(), MAX_RESULTS ) );
    }

    @RequestMapping(value = "/characteristicBrowser.html", method = { RequestMethod.GET, RequestMethod.HEAD })
    public String getView() {
        return "characteristics";
    }

    public void removeCharacteristics( Collection<AnnotationValueObject> chars ) {
        CharacteristicUpdateCommand c = new CharacteristicUpdateCommand();
        c.setAnnotationValueObjects( chars );
        c.setRemove( true );
        taskRunningService.submitTaskCommand( c );
    }

    /**
     * Update characteristics associated with entities. This allows for the case of factor values that we are adding
     * characteristics to for the first time, but the most common case is altering existing characteristics.
     */
    public void updateCharacteristics( Collection<AnnotationValueObject> avos ) {
        CharacteristicUpdateCommand c = new CharacteristicUpdateCommand();
        c.setAnnotationValueObjects( avos );
        c.setRemove( false );
        taskRunningService.submitTaskCommand( c );
    }

    /**
     * @param annotatedItem - the object that has the annotation, we want to find who "owns" it.
     */
    private void populateParentInformation( AnnotationValueObject avo, @Nullable Identifiable annotatedItem ) {

        assert avo != null;

        if ( annotatedItem == null ) {
            avo.setParentLink(
                    "[Parent hidden or not available, " + avo.getObjectClass() + " ID=" + avo.getId() + "]" );
        } else if ( annotatedItem instanceof ExpressionExperiment ) {
            ExpressionExperiment ee = ( ExpressionExperiment ) annotatedItem;
            avo.setParentLink( AnchorTagUtil.getExpressionExperimentLink( ee, String.format( "Experiment: %s", ee.getName() ), servletContext ) );
        } else if ( annotatedItem instanceof BioMaterial ) {
            BioMaterial bm = ( BioMaterial ) annotatedItem;
            avo.setParentLink( AnchorTagUtil.getBioMaterialLink( bm, String.format( "Sample: %s", bm.getName() ), servletContext ) );
        } else if ( annotatedItem instanceof FactorValue ) {
            FactorValue fv = ( FactorValue ) annotatedItem;
            avo.setParentDescription( String.format( "FactorValue: %s", FactorValueUtils.getSummaryString( fv ) ) );
            ExperimentalFactor ef = fv.getExperimentalFactor();
            avo.setParentOfParentLink( AnchorTagUtil.getExperimentalDesignLink( ef.getExperimentalDesign(),
                    "Exp Fac: " + ef.getName() + " (" + StringUtils.abbreviate( ef.getDescription(), 50 ) + ")", servletContext ) );
        } else if ( annotatedItem instanceof ExperimentalFactor ) {
            ExperimentalFactor ef = ( ExperimentalFactor ) annotatedItem;
            avo.setParentLink( AnchorTagUtil.getExperimentalDesignLink( ef.getExperimentalDesign(),
                    "Exp Fac: " + ef.getName() + ( StringUtils.isNotBlank( ef.getDescription() ) ? " (" + StringUtils.abbreviate( ef.getDescription(), 50 ) + ")" : "" ), servletContext ) );
        } else if ( annotatedItem instanceof PhenotypeAssociation ) {
            PhenotypeAssociation pa = ( PhenotypeAssociation ) annotatedItem;
            avo.setParentLink( "PhenotypeAssoc: " + pa.getGene().getOfficialSymbol() );
            avo.setParentDescription( pa.getId().toString() );
        } else {
            avo.setParentDescription( String.format( "%s: %d", annotatedItem.getClass().getSimpleName(), annotatedItem.getId() ) );
        }
    }

}
