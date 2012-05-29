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
package ubic.gemma.loader.expression.geo;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ubic.gemma.analysis.preprocess.ProcessedExpressionDataVectorCreateService;
import ubic.gemma.datastructure.matrix.ExpressionDataDoubleMatrix;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.loader.expression.AffyPowerToolsProbesetSummarize;
import ubic.gemma.loader.expression.geo.fetcher.RawDataFetcher;
import ubic.gemma.loader.expression.geo.service.GeoService;
import ubic.gemma.model.common.auditAndSecurity.AuditTrailService;
import ubic.gemma.model.common.auditAndSecurity.eventType.AuditEventType;
import ubic.gemma.model.common.auditAndSecurity.eventType.ExpressionExperimentPlatformSwitchEvent;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssayData.RawExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;

/**
 * Update the data associated with an experiment. Primary designed for filling in data that we can't or don't want to
 * get from GEO. For loading experiments from flat files, see SimpleExpressionDataLoaderService
 * 
 * @author paul
 * @version $Id$
 */
@Component
public class DataUpdater {

    private static Log log = LogFactory.getLog( DataUpdater.class );

    @Autowired
    private ArrayDesignService arrayDesignService;

    @Autowired
    private ExpressionExperimentService experimentService;

    @Autowired
    private GeoService geoService;

    @Autowired
    private AuditTrailService auditTrailService;

    @Autowired
    private ProcessedExpressionDataVectorCreateService processedExpressionDataVectorCreateService;

    public void addAffyExonArrayData( ExpressionExperiment ee ) {
        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException();
        }
        addAffyExonArrayData( ee, ads.iterator().next() );
    }

    /**
     * @param ee
     * @param ad
     */
    public void addAffyExonArrayData( ExpressionExperiment ee, ArrayDesign ad ) {

        RawDataFetcher f = new RawDataFetcher();
        Collection<LocalFile> files = f.fetch( ee.getAccession().getAccession() );

        if ( files.isEmpty() ) {
            throw new RuntimeException( "Data was apparently not available" );
        }
        ad = arrayDesignService.thaw( ad );
        ee = experimentService.thawLite( ee );

        Taxon primaryTaxon = ad.getPrimaryTaxon();

        ArrayDesign targetPlatform = prepareTargetPlatform( primaryTaxon );

        AffyPowerToolsProbesetSummarize apt = new AffyPowerToolsProbesetSummarize();

        Collection<RawExpressionDataVector> vectors = apt.processExonArrayData( ee, targetPlatform, files );

        if ( vectors.isEmpty() ) {
            throw new IllegalStateException( "No vectors were returned for " + ee );
        }

        experimentService.replaceVectors( ee, targetPlatform, vectors );

        if ( !targetPlatform.equals( ad ) ) {
            AuditEventType eventType = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
            auditTrailService.addUpdateEvent( ee, eventType,
                    "Switched in course of updating vectors using AffyPowerTools (from " + ad.getShortName() + " to "
                            + targetPlatform.getShortName() + ")" );
        }

        audit( ee, "Data vector computation from CEL files using AffyPowerTools for " + targetPlatform );

        processedExpressionDataVectorCreateService.computeProcessedExpressionData( ee );
    }

    /**
     * @param arrayDesign
     */
    private void audit( ExpressionExperiment ee, String note ) {
        // AuditEventType eventType = DataVectorUpdateEvent.Factory.newInstance();

        /*
         * This is temporary until we have a more specific event type.
         */
        AuditEventType eventType = ExpressionExperimentPlatformSwitchEvent.Factory.newInstance();
        auditTrailService.addUpdateEvent( ee, eventType, note );
    }

    /**
     * @param ee
     * @param ad
     * @param data
     */
    public void addData( ExpressionExperiment ee, ArrayDesign ad, ExpressionDataDoubleMatrix data ) {
        throw new UnsupportedOperationException( "not implemented yet" );
    }

    /**
     * @param ee
     * @param data
     */
    public void replaceData( ExpressionExperiment ee, ExpressionDataDoubleMatrix data ) {
        Collection<ArrayDesign> ads = experimentService.getArrayDesignsUsed( ee );
        if ( ads.size() > 1 ) {
            throw new IllegalArgumentException();
        }
        addData( ee, ads.iterator().next(), data );
    }

    private ArrayDesign prepareTargetPlatform( Taxon primaryTaxon ) {
        /*
         * determine the target array design. We use filtered versions of these platforms from GEO.
         */
        String targetPlatformAcc = "";
        if ( primaryTaxon.getCommonName().equals( "mouse" ) ) {
            targetPlatformAcc = "GPL6096";
        } else if ( primaryTaxon.getCommonName().equals( "human" ) ) {
            targetPlatformAcc = "GPL5188";
        } else if ( primaryTaxon.getCommonName().equals( "rat" ) ) {
            targetPlatformAcc = "GPL6543";
        } else {
            throw new IllegalArgumentException( "Exon arrays only supported for mouse, human and rat" );
        }

        ArrayDesign targetPlatform = arrayDesignService.findByShortName( targetPlatformAcc );

        if ( targetPlatform == null ) {
            log.warn( "The target platform " + targetPlatformAcc + " could not be found in the system. Loading it ..." );

            Collection<?> r = geoService.fetchAndLoad( targetPlatformAcc, true, false, false, false );

            if ( r.isEmpty() ) throw new IllegalStateException( "Loading target platform failed." );

            targetPlatform = ( ArrayDesign ) r.iterator().next();

        }

        targetPlatform = arrayDesignService.thaw( targetPlatform );
        return targetPlatform;
    }

}
