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

package ubic.gemma.apps;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;

import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.genome.taxon.service.TaxonService;
import ubic.gemma.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.util.AbstractSpringAwareCLI;

/**
 * Command Line tools for loading the expression experiment in flat files
 * 
 * @author xiangwan
 * @version $Id$
 */
public class LoadSimpleExpressionDataCli extends AbstractSpringAwareCLI {

    private String fileName = null;
    private String dirName = "./";
    private SimpleExpressionDataLoaderService eeLoaderService = null;
    private ArrayDesignService adService = null;
    private TaxonService taxonService = null;
    ExpressionExperimentService eeService;
    final static String SPLITCHAR = "\t{1}";
    final static int NAMEI = 0;
    final static int SHORTNAMEI = NAMEI + 1;
    final static int DESCRIPTIONI = SHORTNAMEI + 1;
    final static int AD_SHORT_NAME_I = DESCRIPTIONI + 1; // The short name of the arrayDesign
    final static int DATAFILEI = AD_SHORT_NAME_I + 1;
    final static int SPECIESI = DATAFILEI + 1;
    final static int QNAMEI = SPECIESI + 1;
    final static int QDESCRIPTIONI = QNAMEI + 1;
    final static int QTYPEI = QDESCRIPTIONI + 1;
    final static int QSCALEI = QTYPEI + 1;
    final static int PUBMEDI = QSCALEI + 1;
    final static int SOURCEI = PUBMEDI + 1;
    final static int ARRAYDESIGNNAMEI = SOURCEI + 1;
    final static int TECHNOLOGYTYPEI = ARRAYDESIGNNAMEI + 1;
    // final static int IMAGECLONEI = QSCALEI + 1;
    final static int TOTALFIELDS = TECHNOLOGYTYPEI + 1;

    /**
     * @param args
     */
    public static void main( String[] args ) {
        LoadSimpleExpressionDataCli p = new LoadSimpleExpressionDataCli();
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            Exception ex = p.doWork( args );
            if ( ex != null ) {
                ex.printStackTrace();
            }
            watch.stop();
            log.info( watch.getTime() );
        } catch ( Exception e ) {
            log.fatal( e, e );
            throw new RuntimeException( e );
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#buildOptions()
     */
    @SuppressWarnings("static-access")
    @Override
    protected void buildOptions() {
        Option fileOption = OptionBuilder.isRequired().hasArg().withArgName( "File Name" ).withDescription(
                "the list of experiments in flat file" ).withLongOpt( "file" ).create( 'f' );
        addOption( fileOption );

        Option dirOption = OptionBuilder.hasArg().withArgName( "File Folder" ).withDescription(
                "The folder for containing the experiment files" ).withLongOpt( "dir" ).create( 'd' );
        addOption( dirOption );

    }

    /*
     * (non-Javadoc)
     * 
     * @see ubic.gemma.util.AbstractCLI#doWork(java.lang.String[])
     */
    @Override
    protected Exception doWork( String[] args ) {
        Exception err = processCommandLine( "Expression Data loader", args );
        if ( err != null ) {
            return err;
        }
        try {
            this.eeLoaderService = ( SimpleExpressionDataLoaderService ) this
                    .getBean( "simpleExpressionDataLoaderService" );
            this.eeService = ( ExpressionExperimentService ) this.getBean( "expressionExperimentService" );
            this.adService = ( ArrayDesignService ) this.getBean( "arrayDesignService" );
            this.taxonService = ( TaxonService ) this.getBean( "taxonService" );
            if ( this.fileName != null ) {
                log.info( "Loading experiments from " + this.fileName );
                InputStream is = new FileInputStream( new File( this.dirName, this.fileName ) );
                BufferedReader br = new BufferedReader( new InputStreamReader( is ) );
                String conf = null;
                while ( ( conf = br.readLine() ) != null ) {

                    if ( StringUtils.isBlank( conf ) ) {
                        continue;
                    }

                    /* Comments in the list file */
                    if ( conf.startsWith( "#" ) ) continue;

                    String expName = conf.split( SPLITCHAR )[0];

                    try {
                        this.loadExperiment( conf );
                        log.info( "Successfully Loaded " + expName );
                        successObjects.add( expName );
                    } catch ( Exception e ) {
                        errorObjects.add( expName + ": " + e.getMessage() );
                        log.error( "Failure loading " + expName, e );
                    }
                }
                summarizeProcessing();
            }
        } catch ( IOException e ) {
            return e;
        }
        return null;
    }

    @Override
    protected void processOptions() {
        super.processOptions();
        if ( hasOption( 'f' ) ) {
            fileName = getOptionValue( 'f' );
        }

        if ( hasOption( 'd' ) ) {
            dirName = getOptionValue( 'd' );
        }
    }

    /**
     * @param fields
     */
    private void checkForArrayDesignName( String[] fields ) {
        if ( StringUtils.isBlank( fields[ARRAYDESIGNNAMEI] ) ) {
            throw new IllegalArgumentException( "Array design must be given if array design is new." );
        }
    }

    /**
     * @param fields
     * @param metaData
     */
    private void configureArrayDesigns( String[] fields, SimpleExpressionExperimentMetaData metaData ) {
        int i;
        TechnologyType techType = TechnologyType.fromString( fields[TECHNOLOGYTYPEI] );
        Collection<ArrayDesign> ads = new HashSet<ArrayDesign>();
        if ( StringUtils.isBlank( fields[AD_SHORT_NAME_I] ) ) {
            // that's okay, so long as we get an array design name
            ArrayDesign ad = getNewArrayDesignFromName( fields );
            ad.setTechnologyType( techType );
            ad.setPrimaryTaxon( metaData.getTaxon() );
            ads.add( ad );
        } else if ( fields[AD_SHORT_NAME_I].trim().equals( "IMAGE" ) ) {
            ArrayDesign ad = getNewArrayDesignFromName( fields );
            ad.setTechnologyType( techType );
            ad.setPrimaryTaxon( metaData.getTaxon() );
            ads.add( ad );
            metaData.setProbeIdsAreImageClones( true );
        } else {
            String allADs[] = fields[AD_SHORT_NAME_I].split( "\\+" );

            // allow for the case where there is an additional new array design to be added.
            if ( StringUtils.isNotBlank( fields[ARRAYDESIGNNAMEI] ) ) {
                ArrayDesign ad = getNewArrayDesignFromName( fields );
                ad.setTechnologyType( techType );
                ads.add( ad );
            }

            for ( i = 0; i < allADs.length; i++ ) {
                ArrayDesign ad = adService.findByShortName( allADs[i] );

                if ( ad == null ) {
                    Collection<ArrayDesign> existingAds = adService.findByAlternateName( allADs[i] );
                    if ( existingAds.size() == 1 ) {
                        ad = existingAds.iterator().next();
                    } else if ( existingAds.size() > 1 ) {
                        throw new IllegalStateException( "Array Design " + allADs[i]
                                + " is ambiguous, it is an alternate name of more than one array design" );
                    }
                }

                if ( ad == null ) {
                    throw new IllegalStateException( "Array Design " + allADs[i]
                            + " is not loaded into the system yet; load it and try again." );
                }
                ads.add( ad );
            }
        }
        metaData.setArrayDesigns( ads );

    }

    /**
     * @param fields
     * @param metaData
     */
    private void configureQuantitationType( String[] fields, SimpleExpressionExperimentMetaData metaData ) {
        metaData.setQuantitationTypeName( fields[QNAMEI] );
        metaData.setQuantitationTypeDescription( fields[QDESCRIPTIONI] );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );

        StandardQuantitationType sQType = StandardQuantitationType.fromString( fields[QTYPEI] );
        metaData.setType( sQType );

        ScaleType sType = ScaleType.fromString( fields[QSCALEI] );
        metaData.setScale( sType );
    }

    /**
     * @param fields
     * @param metaData
     */
    private void configureTaxon( String[] fields, SimpleExpressionExperimentMetaData metaData ) {
        Taxon taxon = Taxon.Factory.newInstance();
        taxon.setScientificName( fields[SPECIESI] );
        Taxon existing = taxonService.find( taxon );
        if ( existing == null ) {
            throw new IllegalArgumentException( "There is no taxon with scientific name " + fields[SPECIESI]
                    + " in the system; please add it first before loading data." );
        }
        metaData.setTaxon( taxon );
    }

    /**
     * @param fields
     * @return
     */
    private ArrayDesign getNewArrayDesignFromName( String[] fields ) {
        checkForArrayDesignName( fields );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( fields[ARRAYDESIGNNAMEI] );
        ad.setShortName( ad.getName() );
        return ad;
    }

    /**
     * @param configurationLine
     * @return
     * @throws Exception
     */
    private void loadExperiment( String configurationLine ) throws Exception {
        int i = 0;
        String fields[] = configurationLine.split( SPLITCHAR );
        if ( fields.length != TOTALFIELDS ) {
            throw new IllegalArgumentException( "Field Missing Got[" + fields.length + "]: " + configurationLine );
        }
        for ( i = 0; i < fields.length; i++ )
            fields[i] = StringUtils.trim( fields[i] );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        String shortName = fields[SHORTNAMEI];

        ExpressionExperiment existing = eeService.findByShortName( shortName );

        if ( existing != null ) {
            throw new IllegalArgumentException( "There is already an experiment with short name " + shortName
                    + "; please choose something unique." );
        }

        metaData.setName( fields[NAMEI] );

        metaData.setShortName( shortName );
        metaData.setDescription( fields[DESCRIPTIONI] );

        configureArrayDesigns( fields, metaData );

        configureTaxon( fields, metaData );

        InputStream data = new FileInputStream( new File( this.dirName, fields[DATAFILEI] ) );

        metaData.setSourceUrl( fields[SOURCEI] );

        String pubMedId = fields[PUBMEDI];
        if ( StringUtils.isNotBlank( pubMedId ) ) {
            metaData.setPubMedId( Integer.parseInt( pubMedId ) );
        }

        configureQuantitationType( fields, metaData );

        ExpressionExperiment ee = eeLoaderService.create( metaData, data );

        ee = eeService.thawLite( ee );

    }

}
