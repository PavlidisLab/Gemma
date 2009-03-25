/*
 * The Gemma project
 * 
 * Copyright (c) 2007 University of British Columbia
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
package ubic.gemma.loader.expression.arrayExpress;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import ubic.basecode.util.FileTools;
import ubic.gemma.analysis.report.ArrayDesignReportService;
import ubic.gemma.analysis.report.ExpressionExperimentReportService;
import ubic.gemma.loader.expression.mage.MageMLConverter;
import ubic.gemma.loader.expression.mage.MageMLParser;
import ubic.gemma.model.common.description.LocalFile;
import ubic.gemma.model.common.quantitationtype.QuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.designElement.CompositeSequence;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.PersisterHelper;

/**
 * Puts together the workflow to load a data set from ArrayExpress
 * 
 * @spring.bean id="arrayExpressLoadService"
 * @spring.property name="persisterHelper" ref="persisterHelper"
 * @spring.property name="mageMLConverter" ref="mageMLConverter"
 * @spring.property name="arrayDesignService" ref="arrayDesignService"
 * @spring.property name="arrayDesignReportService" ref="arrayDesignReportService"
 * @spring.property name="expressionExperimentReportServcie" ref="expressionExperimentReportService"
 * @spring.property name="expressionExperimentService" ref="expressionExperimentService"
 * @author pavlidis
 * @version $Id$
 */
public class ArrayExpressLoadService {

    private static Log log = LogFactory.getLog( ArrayExpressLoadService.class.getName() );

    private ArrayDesignReportService arrayDesignReportService;

    private ArrayDesignService arrayDesignService;

    private ExpressionExperimentReportService expressionExperimentReportService;

    private ExpressionExperimentService expressionExperimentService;
    private MageMLConverter mageMLConverter;
    private PersisterHelper persisterHelper;

    public ExpressionExperiment load( String accession ) {
        return this.load( accession, null, false );
    }

    /**
     * NOTE this currently will not handle data sets that have multiple array designs.
     * 
     * @param accession e.g. E-AFMX-4
     * @param adAccession accession for the array design, either short name or name.
     * @param allowArrayExpressDesign if true, the array design will be loaded from ArrayExpress. Should only be used if
     *        the array design isn't in Gemma or GEO.
     * @return
     */
    public ExpressionExperiment load( String accession, String adAccession, boolean allowArrayExpressDesign ) {
        DataFileFetcher dfFetcher = new DataFileFetcher();
        ProcessedDataFetcher pdFetcher = new ProcessedDataFetcher();
        ProcessedDataFileParser pdParser = new ProcessedDataFileParser();
        ProcessedDataMerger pdMerger = new ProcessedDataMerger();

        ArrayDesign selectedAd = null;
        if ( adAccession != null ) {
            selectedAd = this.arrayDesignService.findByName( adAccession );

            // what if short name was specified....
            if ( selectedAd == null ) selectedAd = this.arrayDesignService.findByShortName( adAccession );

            if ( selectedAd == null ) {
                log.error( "The array design selected doesn't exist in the system: " + adAccession
                        + ", halting processing" );
                return null;
            }
        } else if ( allowArrayExpressDesign ) {
            log.info( "Attempting to get array design from ArrayExpress" );
        }

        MageMLParser mlp = new MageMLParser();

        log.info( "Fetching processed data" );
        Collection<LocalFile> pdFiles = pdFetcher.fetch( accession );
        Collection<LocalFile> filesToUse = pdFetcher.getProcessedDataFile( pdFiles );
        if ( filesToUse.size() == 0 ) {
            log.error( "There is no processed data for " + accession + ", halting processing" );
            return null;
        }

        log.info( "Downloading MAGE-ML file package" );
        Collection<LocalFile> files = dfFetcher.fetch( accession );
        LocalFile mageMlFile = dfFetcher.getMageMlFile( files );
        if ( mageMlFile == null ) {
            log.error( "There is no MAGE-ML file for " + accession + ", halting processing" );
            return null;
        }

        String mageMLpath = mageMlFile.getLocalURL().getPath();

        log.info( "Parsing MAGE-ML" );
        try {
            mlp.parse( mageMLpath );

            Collection<Object> parseResult = mlp.getResults();

            log.info( "Converting MAGE objects" );
            Collection<Object> result = mageMLConverter.convert( parseResult );

            ExpressionExperiment ee = locateExpressionExperimentInMageResults( result );
            ee.setShortName( accession );

            Collection<BioAssay> bioAssays = ee.getBioAssays();
            assert bioAssays != null && bioAssays.size() > 0;

            log.info( "MAGE conversion: located raw expression experiment: " + ee );

            // If we made it this far, and selectedAd is null we know an AD was never specified so go ahead and
            // use the AD given by mage
            if ( selectedAd == null ) {
                if ( allowArrayExpressDesign ) {
                    log.info( "Filling in array design information" );
                    processArrayDesignInfo( bioAssays );
                } else {
                    throw new IllegalStateException(
                            "You must provide a valid array design from Gemma, or allow loader to get it from ArrayExpress" );
                }
            } else { // the user selected an AD in the system, make sure all the bioAssays point to it.
                log.info( "Using specified Array Design: " + selectedAd.getShortName() );
                processArrayDesignInfo( bioAssays, selectedAd );
            }

            log.info( "Parsing processed data" );
            /*
             * Handle case of multiple files.
             */
            for ( LocalFile file : filesToUse ) {
                InputStream is = FileTools.getInputStreamFromPlainOrCompressedFile( file.getLocalURL().getPath() );
                // results accumulate here.
                pdParser.parse( is );
            }

            log.info( "Merging processed data with expression experiment from MAGE-ML" );
            Collection<QuantitationType> qts = ee.getQuantitationTypes(); // locateQuantitationTypesInMageResults(
            // result );

            if ( qts.size() == 0 ) {
                throw new IllegalStateException( "No quantitation types found" );
            }

            pdMerger.merge( ee, qts, pdParser.getMap(), pdParser.getSamples() );

            ExpressionExperiment persistedEE = ( ExpressionExperiment ) persisterHelper.persist( ee );

            updateReports( persistedEE );

            return persistedEE;

        } catch ( IOException e ) {
            throw new RuntimeException( e );
        }

    }

    /**
     * @param arrayDesignReportService the arrayDesignReportService to set
     */
    public void setArrayDesignReportService( ArrayDesignReportService arrayDesignReportService ) {
        this.arrayDesignReportService = arrayDesignReportService;
    }

    public void setArrayDesignService( ArrayDesignService arrayDesignService ) {
        this.arrayDesignService = arrayDesignService;
    }

    /**
     * @param expressionExperimentReportService the expressionExperimentReportService to set
     */
    public void setExpressionExperimentReportService(
            ExpressionExperimentReportService expressionExperimentReportService ) {
        this.expressionExperimentReportService = expressionExperimentReportService;
    }

    /**
     * @param expressionExperimentService the expressionExperimentService to set
     */
    public void setExpressionExperimentService( ExpressionExperimentService expressionExperimentService ) {
        this.expressionExperimentService = expressionExperimentService;
    }

    public void setMageMLConverter( MageMLConverter mageMLConverter ) {
        this.mageMLConverter = mageMLConverter;
    }

    public void setPersisterHelper( PersisterHelper persisterHelper ) {
        this.persisterHelper = persisterHelper;
    }

    /**
     * Locate the expression experiment in the MAGE results.
     * 
     * @param result
     * @return
     */
    private ExpressionExperiment locateExpressionExperimentInMageResults( Collection<Object> result ) {
        ExpressionExperiment ee = null;
        for ( Object object : result ) {
            if ( object instanceof ExpressionExperiment ) {
                ee = ( ExpressionExperiment ) object;
                break;
            }
        }
        return ee;
    }

    /**
     * @param bioAssays
     */
    private void processArrayDesignInfo( Collection<BioAssay> bioAssays ) {
        ArrayDesignFetcher adFetcher = new ArrayDesignFetcher();
        ArrayDesignParser adParser = new ArrayDesignParser();
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();

        for ( BioAssay assay : bioAssays ) {
            ads.add( assay.getArrayDesignUsed() );
        }
        log.info( "There are " + ads.size() + " array designs for this experiment" );

        // At this point the name is like "A-AFFY-6"
        for ( ArrayDesign design : ads ) {
            String name = design.getName();
            Collection<LocalFile> designFiles = adFetcher.fetch( name );
            LocalFile compositeSequenceFile = null;

            for ( LocalFile file : designFiles ) {
                String localPath = file.getLocalURL().getPath();
                if ( localPath.contains( "compositesequences" ) ) {
                    compositeSequenceFile = file;
                }
            }

            if ( compositeSequenceFile == null ) {
                throw new IllegalStateException( "Could not locate the compositesequence file from ArrayExpress for "
                        + design );
            }

            try {
                LocalFile fileToParse = compositeSequenceFile; // first choice
                adParser.parse( fileToParse.getLocalURL().getPath() );
                Collection<CompositeSequence> results = adParser.getResults();
                design.setCompositeSequences( results );
                for ( CompositeSequence sequence : results ) {
                    sequence.setArrayDesign( design );
                }

                // replace so they are all pointing at the same instance.
                for ( BioAssay assay : bioAssays ) {
                    if ( assay.getArrayDesignUsed().equals( design ) ) {
                        assay.setArrayDesignUsed( design );
                    } else {
                        log.info( "Assay uses " + assay.getArrayDesignUsed() );
                    }
                }

            } catch ( IOException e ) {
                throw new RuntimeException( e );
            }

        }
    }

    /**
     * @param bioAssays
     * @param ad
     */
    private void processArrayDesignInfo( Collection<BioAssay> bioAssays, ArrayDesign ad ) {

        arrayDesignService.thawLite( ad );

        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();

        for ( BioAssay assay : bioAssays ) {
            ads.add( assay.getArrayDesignUsed() );
        }
        log.info( "There are " + ads.size() + " array designs for this experiment" );
        if ( ads.size() != 1 ) {
            throw new IllegalStateException(
                    "Expression experiment uses multiple Array Designs, "
                            + ads.size()
                            + "  Unable to match up multiple array designs with single array design selected. Failed to load Expression experiment. " );
        }

        // Just make sure all the bioAssays are pointing to the correct AD.
        for ( BioAssay assay : bioAssays ) {
            assay.setArrayDesignUsed( ad );
        }

    }

    /**
     * @param entities
     */
    private void updateReports( ExpressionExperiment expressionExperiment ) {
        Collection<ArrayDesign> adsToUpdate = new HashSet<ArrayDesign>();

        this.expressionExperimentReportService.generateSummaryObject( expressionExperiment.getId() );

        this.expressionExperimentService.thawLite( expressionExperiment );

        for ( BioAssay ba : expressionExperiment.getBioAssays() ) {
            adsToUpdate.add( ba.getArrayDesignUsed() );
        }

        for ( ArrayDesign arrayDesign : adsToUpdate ) {
            this.arrayDesignReportService.generateArrayDesignReport( arrayDesign.getId() );
        }

    }
}
