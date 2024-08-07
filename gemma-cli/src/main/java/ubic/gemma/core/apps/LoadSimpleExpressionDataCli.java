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

package ubic.gemma.core.apps;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.apps.GemmaCLI.CommandGroup;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetaData;
import ubic.gemma.core.util.AbstractAuthenticatedCLI;
import ubic.gemma.core.util.AbstractCLI;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;
import ubic.gemma.model.expression.arrayDesign.TechnologyType;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.genome.Taxon;
import ubic.gemma.persistence.service.expression.arrayDesign.ArrayDesignService;
import ubic.gemma.persistence.service.expression.experiment.ExpressionExperimentService;
import ubic.gemma.persistence.service.genome.taxon.TaxonService;

import java.io.*;
import java.util.Collection;
import java.util.HashSet;

/**
 * Command Line tools for loading the expression experiment in flat files
 *
 * @author xiangwan
 */
public class LoadSimpleExpressionDataCli extends AbstractAuthenticatedCLI {
    private final static String SPLIT_CHAR = "\t";
    private final static int NAME_I = 0;
    private final static int SHORT_NAME_I = LoadSimpleExpressionDataCli.NAME_I + 1;
    private final static int DESCRIPTION_I = LoadSimpleExpressionDataCli.SHORT_NAME_I + 1;
    private final static int AD_SHORT_NAME_I = LoadSimpleExpressionDataCli.DESCRIPTION_I + 1; // The short name of the arrayDesign
    private final static int DATA_FILE_I = LoadSimpleExpressionDataCli.AD_SHORT_NAME_I + 1;
    private final static int SPECIES_I = LoadSimpleExpressionDataCli.DATA_FILE_I + 1;
    private final static int Q_NAME_I = LoadSimpleExpressionDataCli.SPECIES_I + 1;
    private final static int Q_DESCRIPTION_I = LoadSimpleExpressionDataCli.Q_NAME_I + 1;
    private final static int Q_TYPE_I = LoadSimpleExpressionDataCli.Q_DESCRIPTION_I + 1;
    private final static int Q_SCALE_I = LoadSimpleExpressionDataCli.Q_TYPE_I + 1;
    private final static int PUBMED_I = LoadSimpleExpressionDataCli.Q_SCALE_I + 1;
    private final static int SOURCE_I = LoadSimpleExpressionDataCli.PUBMED_I + 1;
    private final static int ARRAY_DESIGN_NAME_I = LoadSimpleExpressionDataCli.SOURCE_I + 1;
    private final static int TECHNOLOGY_TYPE_I = LoadSimpleExpressionDataCli.ARRAY_DESIGN_NAME_I + 1;
    private final static int TOTAL_FIELDS = LoadSimpleExpressionDataCli.TECHNOLOGY_TYPE_I + 1;

    @Autowired
    private ExpressionExperimentService eeService;
    @Autowired
    private ArrayDesignService adService;
    @Autowired
    private SimpleExpressionDataLoaderService eeLoaderService;
    @Autowired
    private TaxonService taxonService;

    private String dirName = "./";
    private String fileName = null;

    @Override
    public CommandGroup getCommandGroup() {
        return CommandGroup.EXPERIMENT;
    }

    @Override
    public String getShortDesc() {
        return "Load an experiment from a tab-delimited file instead of GEO";
    }

    @Override
    protected void processOptions( CommandLine commandLine ) {
        if ( commandLine.hasOption( 'f' ) ) {
            fileName = commandLine.getOptionValue( 'f' );
        }

        if ( commandLine.hasOption( 'd' ) ) {
            dirName = commandLine.getOptionValue( 'd' );
        }
    }

    @Override
    public String getCommandName() {
        return "addTSVData";
    }

    @Override
    protected void buildOptions( Options options ) {
        Option fileOption = Option.builder( "f" ).required().hasArg().argName( "File Name" )
                .desc( "the list of experiments in flat file" ).longOpt( "file" ).build();
        options.addOption( fileOption );

        Option dirOption = Option.builder( "d" ).hasArg().argName( "File Folder" )
                .desc( "The folder for containing the experiment files" ).longOpt( "dir" ).build();
        options.addOption( dirOption );
        addBatchOption( options );
    }

    @Override
    protected void doWork() throws Exception {
        if ( this.fileName != null ) {
            AbstractCLI.log.info( "Loading experiments from " + this.fileName );
            InputStream is = new FileInputStream( new File( this.dirName, this.fileName ) );
            try ( BufferedReader br = new BufferedReader( new InputStreamReader( is ) ) ) {
                String conf;
                while ( ( conf = br.readLine() ) != null ) {

                    if ( StringUtils.isBlank( conf ) ) {
                        continue;
                    }

                    /* Comments in the list file */
                    if ( conf.startsWith( "#" ) )
                        continue;

                    String expName = conf.split( LoadSimpleExpressionDataCli.SPLIT_CHAR )[0];

                    try {
                        this.loadExperiment( conf );
                        addSuccessObject( expName );
                    } catch ( Exception e ) {
                        addErrorObject( expName, e );
                    }
                }
            }
        }
    }

    private void checkForArrayDesignName( String[] fields ) {
        if ( StringUtils.isBlank( fields[LoadSimpleExpressionDataCli.ARRAY_DESIGN_NAME_I] ) ) {
            throw new IllegalArgumentException( "Array design must be given if array design is new." );
        }
    }

    private void configureArrayDesigns( String[] fields, SimpleExpressionExperimentMetaData metaData ) {
        int i;
        TechnologyType techType = TechnologyType.valueOf( fields[LoadSimpleExpressionDataCli.TECHNOLOGY_TYPE_I] );
        Collection<ArrayDesign> ads = new HashSet<>();
        if ( StringUtils.isBlank( fields[LoadSimpleExpressionDataCli.AD_SHORT_NAME_I] ) ) {
            // that's okay, so long as we get an array design name
            ArrayDesign ad = this.getNewArrayDesignFromName( fields );
            ad.setTechnologyType( techType );
            ad.setPrimaryTaxon( metaData.getTaxon() );
            ads.add( ad );
        } else if ( fields[LoadSimpleExpressionDataCli.AD_SHORT_NAME_I].trim().equals( "IMAGE" ) ) {
            ArrayDesign ad = this.getNewArrayDesignFromName( fields );
            ad.setTechnologyType( techType );
            ad.setPrimaryTaxon( metaData.getTaxon() );
            ads.add( ad );
            metaData.setProbeIdsAreImageClones( true );
        } else if ( StringUtils.isNotBlank( fields[LoadSimpleExpressionDataCli.ARRAY_DESIGN_NAME_I] ) ) {
            // allow for the case where there is an additional new array design to be added.
            ArrayDesign ad = this.getNewArrayDesignFromName( fields );
            ad.setTechnologyType( techType );
            ad.setPrimaryTaxon( metaData.getTaxon() );
            ads.add( ad );
        } else {
            String allADs[] = fields[LoadSimpleExpressionDataCli.AD_SHORT_NAME_I].split( "\\+" );

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

    private void configureQuantitationType( String[] fields, SimpleExpressionExperimentMetaData metaData ) {
        metaData.setQuantitationTypeName( fields[LoadSimpleExpressionDataCli.Q_NAME_I] );
        metaData.setQuantitationTypeDescription( fields[LoadSimpleExpressionDataCli.Q_DESCRIPTION_I] );
        metaData.setGeneralType( GeneralType.QUANTITATIVE );

        StandardQuantitationType sQType = StandardQuantitationType
                .valueOf( fields[LoadSimpleExpressionDataCli.Q_TYPE_I] );
        metaData.setType( sQType );

        ScaleType sType = ScaleType.valueOf( fields[LoadSimpleExpressionDataCli.Q_SCALE_I] );
        metaData.setScale( sType );
    }

    private void configureTaxon( String[] fields, SimpleExpressionExperimentMetaData metaData ) {
        Taxon existing = taxonService.findByCommonName( fields[LoadSimpleExpressionDataCli.SPECIES_I] );
        if ( existing == null ) {
            throw new IllegalArgumentException(
                    "There is no taxon with scientific name " + fields[LoadSimpleExpressionDataCli.SPECIES_I]
                            + " in the system; please add it first before loading data." );
        }
        metaData.setTaxon( existing );
    }

    private ArrayDesign getNewArrayDesignFromName( String[] fields ) {
        this.checkForArrayDesignName( fields );
        ArrayDesign ad = ArrayDesign.Factory.newInstance();
        ad.setName( fields[LoadSimpleExpressionDataCli.ARRAY_DESIGN_NAME_I] );
        ad.setShortName( ad.getName() );
        return ad;
    }

    private void loadExperiment( String configurationLine ) throws Exception {
        int i;
        String fields[] = configurationLine.split( LoadSimpleExpressionDataCli.SPLIT_CHAR );
        if ( fields.length != LoadSimpleExpressionDataCli.TOTAL_FIELDS ) {
            throw new IllegalArgumentException( "Field Missing Got[" + fields.length + "]: " + configurationLine );
        }
        for ( i = 0; i < fields.length; i++ )
            fields[i] = StringUtils.strip( fields[i] );

        SimpleExpressionExperimentMetaData metaData = new SimpleExpressionExperimentMetaData();

        String shortName = fields[LoadSimpleExpressionDataCli.SHORT_NAME_I];

        ExpressionExperiment existing = eeService.findByShortName( shortName );

        if ( existing != null ) {
            throw new IllegalArgumentException( "There is already an experiment with short name " + shortName
                    + "; please choose something unique." );
        }

        metaData.setName( fields[LoadSimpleExpressionDataCli.NAME_I] );

        metaData.setShortName( shortName );
        metaData.setDescription( fields[LoadSimpleExpressionDataCli.DESCRIPTION_I] );

        this.configureTaxon( fields, metaData );

        this.configureArrayDesigns( fields, metaData );

        try ( InputStream data = new FileInputStream(
                new File( this.dirName, fields[LoadSimpleExpressionDataCli.DATA_FILE_I] ) ) ) {

            metaData.setSourceUrl( fields[LoadSimpleExpressionDataCli.SOURCE_I] );

            String pubMedId = fields[LoadSimpleExpressionDataCli.PUBMED_I];
            if ( StringUtils.isNotBlank( pubMedId ) ) {
                metaData.setPubMedId( Integer.parseInt( pubMedId ) );
            }

            this.configureQuantitationType( fields, metaData );

            ExpressionExperiment ee = eeLoaderService.create( metaData, data );
            this.eeService.thawLite( ee );
        }
    }

}
