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
package ubic.gemma.loader.expression.simple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.datastructure.matrix.ExpressionDataWriterUtils;
import ubic.gemma.expression.experiment.service.ExperimentalDesignService;
import ubic.gemma.expression.experiment.service.ExpressionExperimentService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.VocabCharacteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.biomaterial.BioMaterialService;
import ubic.gemma.model.expression.experiment.ExperimentalDesign;
import ubic.gemma.model.expression.experiment.ExperimentalFactor;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;
import ubic.gemma.model.expression.experiment.FactorType;
import ubic.gemma.model.expression.experiment.FactorValue;
import ubic.gemma.model.expression.experiment.FactorValueService;
import ubic.gemma.ontology.OntologyService;
import ubic.gemma.ontology.providers.MgedOntologyService;

/**
 * See interface for docs.
 * 
 * @author Paul
 * @version $Id$
 */
@Service
public class ExperimentalDesignImporterImpl implements ExperimentalDesignImporter {

    private static final int NUMBER_OF_EXTRA_COLUMNS_ALLOWED = 2;

    public static final String EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR = "#$";

    private static Log log = LogFactory.getLog( ExperimentalDesignImporterImpl.class.getName() );

    @Autowired
    private BioMaterialService bioMaterialService;

    @Autowired
    private ExperimentalDesignService experimentalDesignService;

    @Autowired
    private OntologyService ontologyService;

    @Autowired
    FactorValueService factorValueServiceService = null;

    @Autowired
    ExpressionExperimentService expressionExperimentService;

    private MgedOntologyService mgedOntologyService;

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.simple.ExperimentalDesignImporter#importDesign(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.io.InputStream)
     */
    @Override
    public void importDesign( ExpressionExperiment experiment, InputStream is ) throws IOException {
        this.importDesign( experiment, is, false );
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * ubic.gemma.loader.expression.simple.ExperimentalDesignImporter#importDesign(ubic.gemma.model.expression.experiment
     * .ExpressionExperiment, java.io.InputStream, boolean)
     */
    @Override
    public void importDesign( ExpressionExperiment experiment, InputStream is, boolean dryRun ) throws IOException {
        this.mgedOntologyService = this.ontologyService.getMgedOntologyService();

        log.debug( "Parsing input file" );
        boolean readHeader = false;

        BufferedReader r = new BufferedReader( new InputStreamReader( is ) );
        String line = null;
        if ( mgedOntologyService == null ) {
            throw new IllegalStateException( "Please set the MGED OntologyService, thanks." );
        }

        ExperimentalDesign experimentalDesign = experiment.getExperimentalDesign();

        if ( !experimentalDesign.getExperimentalFactors().isEmpty() ) {
            log.warn( "Experimental design already has factors, import will add new ones" );
        }

        experimentalDesign.setDescription( "Parsed from file." );

        List<String> experimentalFactorLines = new ArrayList<String>();
        String sampleHeaderLine = "";
        List<String> factorValueLines = new ArrayList<String>();

        while ( ( line = r.readLine() ) != null ) {
            if ( line.startsWith( EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR ) ) {
                experimentalFactorLines.add( line );
            } else if ( line.startsWith( "#" ) || StringUtils.isBlank( line ) ) {
                continue;
            } else if ( !readHeader ) {
                sampleHeaderLine = line;
                readHeader = true;
            } else {
                factorValueLines.add( line );
            }
        }
        String[] headerFields = StringUtils.splitPreserveAllTokens( sampleHeaderLine, "\t" );

        Collection<BioMaterial> experimentBioMaterials = this.bioMaterialService.findByExperiment( experiment );

        validateFileComponents( experimentalFactorLines, sampleHeaderLine, factorValueLines );
        validateExperimentalFactorFileContent( experimentalFactorLines, sampleHeaderLine );
        validateFactorFileContent( experimentalFactorLines.size(), factorValueLines );
        validateBioMaterialFileContent( experiment, experimentBioMaterials, factorValueLines );

        // build up the composite: create experimental factor then add the experimental value
        addExperimentalFactorsToExperimentalDesign( experimentalDesign, experimentalFactorLines, headerFields,
                factorValueLines );

        experimentalDesignService.update( experimentalDesign );

        // a bit tricky as there is an assumption that the first biomaterial in the bioassay set is the relevent one;
        // safer to use biomaterial collection returned; cannot guarantee order of objects in collection.
        Collection<BioMaterial> bioMaterialsWithFactorValues = addFactorValuesToBioMaterialsInExpressionExperiment(
                experiment, experimentBioMaterials, experimentalDesign, factorValueLines, headerFields );

        for ( BioMaterial bioMaterial : bioMaterialsWithFactorValues ) {
            this.bioMaterialService.update( bioMaterial );

            // just a debugging sanity check.
            BioMaterial bbm = this.bioMaterialService.load( bioMaterial.getId() );
            if ( log.isDebugEnabled() )
                log.debug( bbm + ": " + bbm.getFactorValues().size() + " factor values: "
                        + StringUtils.join( bbm.getFactorValues(), " ; " ) );
        }

    }

    /**
     * This method reads the file line e.g. $Run time : Category=EnvironmentalHistory Type=categorical and creates
     * experimental factors from it and adds them to the experimental design.
     * 
     * @param experimentalDesign Experimental design for this expression experiment
     * @param experimentalFactorFileLines List of strings representing lines from input file containing experimental
     *        factors
     * @param headerFields Sample header line split on tab.
     * @param factorValueLines Lines containing biomaterial names and their factor values
     */
    private void addExperimentalFactorsToExperimentalDesign( ExperimentalDesign experimentalDesign,
            List<String> experimentalFactorFileLines, String[] headerFields, List<String> factorValueLines ) {

        int maxWait = 0;
        while ( !mgedOntologyService.isOntologyLoaded() ) {
            try {
                Thread.sleep( 1000 );
                if ( maxWait++ > 100 ) {
                    throw new RuntimeException( "MGED is not loaded and gave up waiting" );
                }
            } catch ( InterruptedException e ) {
                e.printStackTrace();
            }
        }

        log.info( "Addding experimental factors to experimental design: " + experimentalDesign.getId() );

        Collection<OntologyTerm> terms = mgedOntologyService.getMgedTermsByKey( "factor" );
        if ( experimentalDesign.getExperimentalFactors() == null ) {
            experimentalDesign.setExperimentalFactors( new HashSet<ExperimentalFactor>() );
        }

        Map<String, Set<String>> mapFactorSampleValues = getMapFactorSampleValues( headerFields, factorValueLines );

        for ( String experimentalFactorFileLine : experimentalFactorFileLines ) {

            // $Run time : Category=EnvironmentalHistory Type=categorical
            String[] experimentalFactorfields = experimentalFactorFileLine.split( ":" );

            String factorValue = ( StringUtils.strip( experimentalFactorfields[0].replaceFirst(
                    Pattern.quote( EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR ) + "\\s*", "" ) ) ).trim();
            String categoryAndType = StringUtils.strip( experimentalFactorfields[1] );
            String[] categoryAndTypeFields = StringUtils.split( categoryAndType );

            // e.g. Category=EnvironmentalHistory
            String category = categoryAndTypeFields[0];
            // e.g. EnvironmentalHistory
            String categoryValue = StringUtils.split( category, "=" )[1];

            ExperimentalFactor experimentalFactorFromFile = ExperimentalFactor.Factory.newInstance();
            experimentalFactorFromFile.setExperimentalDesign( experimentalDesign );
            VocabCharacteristic vc = mgedLookup( categoryValue, terms );

            // e.g. Category=EnvironmentalHistory
            String categoryTypeValue = categoryAndTypeFields[1];
            String factorType = StringUtils.split( categoryTypeValue, "=" )[1];

            // vc.setCategory( categoryType );

            experimentalFactorFromFile.setCategory( vc );
            experimentalFactorFromFile.setName( factorValue );
            experimentalFactorFromFile.setDescription( factorValue );
            experimentalFactorFromFile.setType( factorType.equalsIgnoreCase( "CATEGORICAL" ) ? FactorType.CATEGORICAL
                    : FactorType.CONTINUOUS );

            addFactorValuesToExperimentalFactor( experimentalFactorFromFile, mapFactorSampleValues, factorType );

            if ( !checkForDuplicateExperimentalFactorOnExperimentalDesign( experimentalDesign,
                    experimentalFactorFromFile ) ) {
                // assert experimentalFactorFromFile.getId() != null;
                experimentalDesign.getExperimentalFactors().add( experimentalFactorFromFile );
                // here is was the update
                log.debug( "Added experimental factor value " + experimentalFactorFromFile + " to experimental design "
                        + experimentalDesign );

            }
        }

    }

    /**
     * Add the factor values to the biomaterial
     * 
     * @param experiment
     * @param experimentBioMaterials Current expression experiment's biomaterials.
     * @param experimentalDesign experimental design
     * @param factorValueLines Lines from file containing factor values and biomaterial ids
     * @param headerFields
     * @return Collection of biomaterials associated with this experiment, this is returned as the biomaterial is in a
     *         bioassay (first one retrieved)
     */
    private Collection<BioMaterial> addFactorValuesToBioMaterialsInExpressionExperiment(
            ExpressionExperiment experiment, Collection<BioMaterial> experimentBioMaterials,
            ExperimentalDesign experimentalDesign, List<String> factorValueLines, String[] headerFields ) {
        log.debug( "Adding factors values to biomaterials: " + experimentalDesign.getId() );
        Collection<ExperimentalFactor> experimentalFactorsInExperiment = experimentalDesign.getExperimentalFactors();
        Collection<BioMaterial> biomaterialsWithFactorValuesInExperiment = new HashSet<BioMaterial>();

        Collection<BioMaterial> seenBioMaterials = new HashSet<BioMaterial>();

        Map<ExperimentalFactor, Collection<BioMaterial>> factorsAssociatedWithBioMaterials = new HashMap<ExperimentalFactor, Collection<BioMaterial>>();

        for ( String factorValueLine : factorValueLines ) {
            String[] factorValueFields = StringUtils.splitPreserveAllTokens( factorValueLine, "\t" );

            String externalId = null;
            boolean hasExternalId = headerFields[1].toUpperCase().equals( "EXTERNALID" );
            if ( hasExternalId ) {
                externalId = factorValueFields[1];
            }
            BioMaterial currentBioMaterial = getBioMaterialFromExpressionExperiment( experiment,
                    experimentBioMaterials, factorValueFields[0], externalId );

            if ( currentBioMaterial == null ) {
                throw new IllegalStateException( "No biomaterial for " + factorValueFields[0] );
            }

            if ( seenBioMaterials.contains( currentBioMaterial ) ) {
                throw new IllegalArgumentException( "A biomaterial occurred more than once in the file: "
                        + currentBioMaterial );
            }

            seenBioMaterials.add( currentBioMaterial );

            int start = 1;
            if ( hasExternalId ) {
                start = 2;
            }

            for ( int i = start; i < factorValueFields.length; i++ ) {
                ExperimentalFactor currentExperimentalFactor = null;
                String currentExperimentalFactorName = StringUtils.strip( headerFields[i] );

                FactorValue currentFactorValue = null;
                String currentFactorValueValue = StringUtils.strip( factorValueFields[i] );

                if ( StringUtils.isBlank( currentFactorValueValue ) ) {
                    // Missing value. Note that catching 'NA' etc. is hard, because they could be valid strings.
                    continue;
                }

                for ( ExperimentalFactor experimentalFactor : experimentalFactorsInExperiment ) {
                    if ( experimentalFactor.getName().equals( currentExperimentalFactorName ) ) {
                        currentExperimentalFactor = experimentalFactor;
                    }
                }

                if ( currentExperimentalFactor == null )
                    throw new IllegalStateException( "No factor matches column " + currentExperimentalFactorName );

                Collection<FactorValue> factorValuesInCurrentExperimentalFactor = currentExperimentalFactor
                        .getFactorValues();

                for ( FactorValue factorValue : factorValuesInCurrentExperimentalFactor ) {
                    String fvvalue = factorValue.getValue();
                    assert StringUtils.isNotBlank( fvvalue );
                    if ( fvvalue.trim().equalsIgnoreCase( currentFactorValueValue.trim() ) ) {
                        currentFactorValue = factorValue;
                    }
                }

                if ( currentFactorValue == null ) {
                    log.error( "Current factor value not found " + currentExperimentalFactor + currentFactorValueValue );
                } else {
                    if ( !checkForDuplicateFactorOnBioMaterial( currentBioMaterial, currentFactorValue ) ) {
                        currentBioMaterial.getFactorValues().add( currentFactorValue );
                    } else {
                        // already got warned.
                    }
                }
                log.debug( "Added factor value " + currentFactorValue + " to biomaterial " + currentBioMaterial );
                biomaterialsWithFactorValuesInExperiment.add( currentBioMaterial );

                if ( !factorsAssociatedWithBioMaterials.containsKey( currentExperimentalFactor ) ) {
                    factorsAssociatedWithBioMaterials.put( currentExperimentalFactor, new HashSet<BioMaterial>() );
                }
                factorsAssociatedWithBioMaterials.get( currentExperimentalFactor ).add( currentBioMaterial );

            }

        }

        /*
         * Check if every biomaterial got used. Worth a warning, at least.
         */
        for ( ExperimentalFactor ef : factorsAssociatedWithBioMaterials.keySet() ) {
            if ( !factorsAssociatedWithBioMaterials.get( ef ).containsAll( experimentBioMaterials ) ) {
                log.warn( "File did not contain values for all factor - biomaterial combinations: Missing at least one for "
                        + ef
                        + " [populated "
                        + factorsAssociatedWithBioMaterials.get( ef ).size()
                        + "/"
                        + experimentBioMaterials.size() + " ]" );
            }
        }

        return biomaterialsWithFactorValuesInExperiment;
    }

    /**
     * Method that adds factor values to a given experimental factor
     * 
     * @param experimentalFactor The experimental factor to add the factor values to
     * @param factorSampleValues A map of factor value names keyed on experimental factor name
     * @param factorType Whether the factor is continuous or categorical
     */
    private void addFactorValuesToExperimentalFactor( ExperimentalFactor experimentalFactor,
            Map<String, Set<String>> factorSampleValues, String factorType ) {
        log.debug( "Addding factors values to experimental factor: " + experimentalFactor.getName() );
        VocabCharacteristic category = ( VocabCharacteristic ) experimentalFactor.getCategory();

        Set<String> values = factorSampleValues.get( experimentalFactor.getName() );
        for ( String value : values ) {

            assert StringUtils.isNotBlank( value );
            FactorValue factorValue = FactorValue.Factory.newInstance();
            factorValue.setValue( value );

            if ( factorType.equalsIgnoreCase( "CATEGORICAL" ) ) {
                log.debug( "Factor is categorical" );
                VocabCharacteristic newVc = VocabCharacteristic.Factory.newInstance();
                String category2 = category.getCategory();
                assert category2 != null;
                newVc.setCategory( category2 );
                newVc.setCategoryUri( category.getCategoryUri() );
                newVc.setValue( value );
                newVc.setEvidenceCode( GOEvidenceCode.IC );
                factorValue.getCharacteristics().add( newVc );
            } else {
                log.debug( "Factor is continous" );
                addMeasurementToFactorValueOfTypeContinous( factorValue );
            }
            // set bidirectional relationship
            experimentalFactor.getFactorValues().add( factorValue );
            factorValue.setExperimentalFactor( experimentalFactor );
            log.debug( "Added factor value " + factorValue + " to experimental factor " + experimentalFactor );
        }

    }

    /**
     * Add a measurement to a factor value which is of type continuous
     * 
     * @param FactorValue representing a continuous factor with an associated measurement
     */
    private void addMeasurementToFactorValueOfTypeContinous( FactorValue factorValue ) {
        Measurement m = Measurement.Factory.newInstance();
        m.setType( MeasurementType.ABSOLUTE );
        m.setValue( factorValue.getValue() );
        try {
            Double.parseDouble( factorValue.getValue() ); // check if it is a number, don't need the value.
            m.setRepresentation( PrimitiveType.DOUBLE );
        } catch ( NumberFormatException e ) {
            m.setRepresentation( PrimitiveType.STRING );
        }

        factorValue.setMeasurement( m );
        log.debug( "Created " + factorValue + " for experimental factor " );

    }

    /**
     * Check that experimental design does not already contain the experimental factor.
     * 
     * @param experimentalDesign Existing experimental design.
     * @param experimentalFactorFromFile The experimental factor in the file
     */
    private boolean checkForDuplicateExperimentalFactorOnExperimentalDesign( ExperimentalDesign experimentalDesign,
            ExperimentalFactor experimentalFactorFromFile ) {

        boolean foundMatch = false;
        for ( ExperimentalFactor existingExperimentalFactors : experimentalDesign.getExperimentalFactors() ) {
            if ( existingExperimentalFactors.getName().equals( experimentalFactorFromFile.getName() ) ) {
                log.info( experimentalFactorFromFile + " matches existing " + existingExperimentalFactors );
                experimentalFactorFromFile = existingExperimentalFactors;
                foundMatch = true;
            }
        }
        return foundMatch;
    }

    /**
     * This method checks that the biomaterial does not already have a factor.
     * 
     * @param bioMaterial
     * @param factorValue
     * @return
     */
    private boolean checkForDuplicateFactorOnBioMaterial( BioMaterial bioMaterial, FactorValue factorValue ) {
        boolean foundMatch = false;
        // make sure we don't add two values.
        for ( FactorValue existingfv : bioMaterial.getFactorValues() ) {
            if ( factorValue.equals( existingfv )
                    || existingfv.getExperimentalFactor().equals( factorValue.getExperimentalFactor() ) ) {
                log.warn( bioMaterial + " already has a factorvalue for " + factorValue.getExperimentalFactor() + " ["
                        + factorValue + " matched existing: " + existingfv + "]" );
                foundMatch = true;
                break;
            }
        }

        return foundMatch;
    }

    /**
     * This method retrieves a biomaterial from the expression experiment based on a biomaterial name given in the input
     * file. If no biomaterial is found then null is returned, indicating that a biomaterial name was given in the file
     * which does not match those stored for the expression experiment.
     * 
     * @param expressionExperiment The current expression experiment
     * @param biomaterialNameFromFile - A factor value file line whose first column contains biomaterial name
     * @param externalId - the external id stored in the file, which might not be available (so this can be null or
     *        blank)
     * @return The biomaterial in the expression experiment set matching the biosource name given in the first column of
     *         the factor value line.
     */
    private BioMaterial getBioMaterialFromExpressionExperiment( ExpressionExperiment ee,
            Collection<BioMaterial> bioMaterials, String biomaterialNameFromFile, String externalId ) {

        Map<String, BioMaterial> biomaterialsInExpressionExperiment = mapBioMaterialsToNamePossibilities( bioMaterials );

        // format the biomaterial name gemma style
        String bioMaterialNameFormatedWithShortName = SimpleExpressionDataLoaderServiceImpl.makeBioMaterialName( ee,
                biomaterialNameFromFile );

        BioMaterial bioMaterial = biomaterialsInExpressionExperiment.get( biomaterialNameFromFile );
        if ( bioMaterial == null ) {
            // try alternative format...
            bioMaterial = biomaterialsInExpressionExperiment.get( bioMaterialNameFormatedWithShortName );
        }

        if ( bioMaterial == null && StringUtils.isNotBlank( externalId ) ) {
            // FIXME document this better. If there are two or more GSM's grouped together we list them in the file
            // separated by '/'.
            String[] externalIds = StringUtils.split( externalId, "/" );

            for ( String id : externalIds ) {
                bioMaterial = biomaterialsInExpressionExperiment.get( id );
                if ( bioMaterial != null ) break;
            }

        }

        return bioMaterial;
    }

    /**
     * Get a map of experimental values keyed on experimental factor name
     * 
     * @param headerFields
     * @param factorValueLines
     * @return map of experimental factor values keyed on experimental factor
     */
    private Map<String, Set<String>> getMapFactorSampleValues( String[] headerFields, List<String> factorValueLines ) {
        Map<String, Set<String>> factorSampleValues = new HashMap<String, Set<String>>();
        for ( String factorValueLine : factorValueLines ) {
            String[] factorValueFields = StringUtils.splitPreserveAllTokens( factorValueLine, "\t" );

            for ( int i = 1; i < headerFields.length; i++ ) {

                // get the key
                String value = headerFields[i];
                value = StringUtils.strip( value );
                String factorValue = StringUtils.strip( factorValueFields[i] );
                Set<String> listFactorValues = factorSampleValues.get( value );
                if ( listFactorValues == null ) {
                    listFactorValues = new HashSet<String>();
                }
                listFactorValues.add( factorValue );
                factorSampleValues.put( value, listFactorValues );

            }

        }
        return factorSampleValues;

    }

    /**
     * Create a map of various strings that we might find in a design importing file to the biomaterials.
     * 
     * @param expressionExperiment
     * @return
     */
    private Map<String, BioMaterial> mapBioMaterialsToNamePossibilities( Collection<BioMaterial> bioMaterials ) {
        Map<String, BioMaterial> biomaterialsInExpressionExperiment = new HashMap<String, BioMaterial>();

        // this rather big loop is recomputed each time we call this method. No big deal, but could be more efficient.
        for ( BioMaterial bm : bioMaterials ) {
            biomaterialsInExpressionExperiment.put( bm.getName(), bm );

            // we allow multiple bioassays per biomaterial - e.g. two platforms run on the sa
            for ( BioAssay ba : bm.getBioAssaysUsedIn() ) {

                /*
                 * Allow matches to the accession (external id) of the bioassay; trying to be flexible! This _could_
                 * cause problems if there are multiple bioassays per biomaterial, thus the check here.
                 */
                if ( ba.getAccession() != null && StringUtils.isNotBlank( ba.getAccession().getAccession() ) ) {
                    String accession = ba.getAccession().getAccession();
                    /*
                     * We get at most one bioassay per biomaterial.
                     */
                    biomaterialsInExpressionExperiment.put( accession, bm );
                }

                /*
                 * Similarly allow match on the bioassay name
                 */
                biomaterialsInExpressionExperiment.put( ba.getName(), bm );
            }

            /*
             * All put in the very-mangled name we use in the 'native' Gemma export format. This includes the ID, so not
             * useful for tests.
             */
            biomaterialsInExpressionExperiment.put(
                    ExpressionDataWriterUtils.constructBioAssayName( bm, bm.getBioAssaysUsedIn() ), bm );

        }
        return biomaterialsInExpressionExperiment;
    }

    /**
     * Does an mged lookup
     * 
     * @param category
     * @return
     */
    private VocabCharacteristic mgedLookup( String category, Collection<OntologyTerm> terms ) {

        OntologyTerm t = null;
        for ( OntologyTerm to : terms ) {
            if ( to.getTerm().equals( category ) ) {
                t = to;
                break;
            }
        }

        if ( t == null ) {
            throw new IllegalArgumentException( "No MGED term matches '" + category + "'" );
        }

        VocabCharacteristic vc = VocabCharacteristic.Factory.newInstance();
        vc.setCategoryUri( t.getUri() );
        vc.setCategory( t.getTerm() );
        vc.setValueUri( t.getUri() );
        vc.setValue( t.getTerm() );
        vc.setEvidenceCode( GOEvidenceCode.IC );
        return vc;
    }

    /**
     * Check that the biomaterial is in the file and in the experiment. It is arguable whether this should be an
     * exception. I think it has to be to make sure that simple errors in the format are caught. But it's inconvenient
     * for cases where a single 'design' file is to be used for multiple microarray studies. Biomaterial ids should
     * match what is stored
     * 
     * @param experiment Current experiment
     * @param factorValueLines Lines containing biomaterial names and their factor values
     */
    private void validateBioMaterialFileContent( ExpressionExperiment experiment, Collection<BioMaterial> bioMaterials,
            List<String> factorValueLines ) throws IllegalArgumentException {

        for ( String factorValueLine : factorValueLines ) {
            String[] vals = StringUtils.splitPreserveAllTokens( factorValueLine, '\t' );
            if ( vals.length < 2 ) {
                throw new IllegalArgumentException( "Expected a file with at least two columns separated by tabs, got "
                        + factorValueLine );
            }
            BioMaterial bioMaterialInFile = getBioMaterialFromExpressionExperiment( experiment, bioMaterials, vals[0],
                    vals[1] );
            if ( bioMaterialInFile == null ) {
                throw new IllegalArgumentException(
                        "The uploaded file has a biomaterial name that does not match the study: "
                                + StringUtils.splitPreserveAllTokens( factorValueLine, "\t" )[0]
                                + " (formatted based on on input: " );
            }
        }
    }

    /**
     * Validates that the input for experimental factors is correct: Experimental factor file line should be for e.g.
     * #$Run time : Category=EnvironmentalHistory Type=categorical Checks there is a colon, between experimental factor
     * and category and that category is correctly formatted.
     * 
     * @param sampleHeaderLine Lines in file corresponding to order of experimental factors
     * @param experimentalFactorList The lines in the file corresponding to experimental factors.
     * @throws IOException Experimental factor lines were not correctly format.
     */
    private void validateExperimentalFactorFileContent( List<String> experimentalFactorLines, String sampleHeaderLine )
            throws IOException {
        Set<String> experimentalFactorValueNames = new HashSet<String>();
        // validate experimental factor lines
        for ( String line : experimentalFactorLines ) {
            String[] fields = line.split( ":" );
            if ( fields.length != 2 ) {
                throw new IOException( "EF description must have two fields with a single ':' in between (" + line
                        + ")" );
            }
            String factorName = StringUtils.strip( fields[0].replaceFirst(
                    Pattern.quote( EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR ) + "\\s*", "" ) );

            experimentalFactorValueNames.add( factorName );
            String category = StringUtils.strip( fields[1] );

            String[] descriptions = StringUtils.split( category );

            if ( descriptions.length != 2 ) {
                throw new IOException( "EF details should have the format 'Category=CATEGORY Type=TYPE'" );
            }

        }

        validateSampleHeaderFileContent( experimentalFactorValueNames, experimentalFactorLines.size(), sampleHeaderLine );

    }

    /**
     * Validates that factor values given in file for each biomaterial match the number of experimental factor values
     * expected.
     * 
     * @para numberOfExperimentalFactors
     * @param factorValueList Represents lines of file containing factor values for a biomaterial
     */
    private void validateFactorFileContent( Integer numberOfExperimentalFactors, List<String> factorValueList )
            throws IOException {
        for ( String factorValueLine : factorValueList ) {
            String[] fields = StringUtils.splitPreserveAllTokens( factorValueLine, "\t" );
            if ( fields.length > numberOfExperimentalFactors + NUMBER_OF_EXTRA_COLUMNS_ALLOWED ) {
                throw new IOException( "Expected no more than "
                        + ( numberOfExperimentalFactors + NUMBER_OF_EXTRA_COLUMNS_ALLOWED )
                        + " columns based on EF descriptions (plus id column), got " + fields.length );
            }
            if ( fields.length <= numberOfExperimentalFactors ) {
                throw new IOException( "Expected at least " + ( numberOfExperimentalFactors + 1 )
                        + " columns based on EF descriptions (plus id column), got " + fields.length );

            }
        }
    }

    /**
     * Simple file content validation checking that the 3 file components are present in the file
     * 
     * @param experimentalFactorLines Lines identified by EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR (#$) detailing
     *        experimental factor values.
     * @param sampleHeaderLine Header Giving order of experimental factor values in the file
     * @param factorValues The factor values in this file
     * @throws IOException File was not in correct format.
     */
    private void validateFileComponents( List<String> experimentalFactorLines, String sampleHeaderLine,
            List<String> factorValues ) throws IOException {
        if ( experimentalFactorLines.isEmpty() ) {
            throw new IOException( "No experimentalFactorLine definitions found in the design file." );
        }
        if ( StringUtils.isBlank( sampleHeaderLine ) ) {
            throw new IOException( "No Sample header found" );
        }

        if ( factorValues.isEmpty() ) {
            throw new IOException( "No factorValues definitions found in the design file." );
        }

    }

    /**
     * Validates that the sample header is correctly formatted. Checks that the experimental factors defined in the
     * header match those in the experimental factor file lines.
     * 
     * @param experimentalFactorValueNames
     * @param numberOfExperimentalFactors
     * @param sampleHeaderLine
     * @throws IOException Validation fails.
     */
    private void validateSampleHeaderFileContent( Set<String> experimentalFactorValueNames,
            Integer numberOfExperimentalFactors, String sampleHeaderLine ) throws IOException {
        String[] headerFields = StringUtils.splitPreserveAllTokens( sampleHeaderLine, "\t" );

        // we might have the ids, and the external id.
        if ( headerFields.length > numberOfExperimentalFactors + NUMBER_OF_EXTRA_COLUMNS_ALLOWED ) {
            throw new IOException( "Expected " + ( numberOfExperimentalFactors + NUMBER_OF_EXTRA_COLUMNS_ALLOWED )
                    + " columns based on EF descriptions (plus id column), got " + headerFields.length );
        }

        for ( int i = 1; i < headerFields.length; i++ ) {

            String value = headerFields[i];

            value = StringUtils.strip( value );

            if ( value.equals( "ExternalID" ) ) {
                // that's fine.
                continue;
            }

            if ( !experimentalFactorValueNames.contains( value ) ) {
                throw new IOException( "Expected to find an EF matching the column heading '" + value + "'" );
            }

        }

    }

}
