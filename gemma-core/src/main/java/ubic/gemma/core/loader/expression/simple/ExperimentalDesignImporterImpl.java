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
package ubic.gemma.core.loader.expression.simple;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ubic.basecode.ontology.model.OntologyTerm;
import ubic.gemma.core.datastructure.matrix.ExpressionDataWriterUtils;
import ubic.gemma.core.ontology.OntologyService;
import ubic.gemma.model.association.GOEvidenceCode;
import ubic.gemma.model.common.description.Characteristic;
import ubic.gemma.model.common.measurement.Measurement;
import ubic.gemma.model.common.measurement.MeasurementType;
import ubic.gemma.model.common.quantitationtype.PrimitiveType;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.service.expression.biomaterial.BioMaterialService;
import ubic.gemma.persistence.service.expression.experiment.ExperimentalDesignService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * See interface for docs.
 *
 * @author Paul
 */
@Service
public class ExperimentalDesignImporterImpl implements ExperimentalDesignImporter {

    private static final Log log = LogFactory.getLog( ExperimentalDesignImporterImpl.class.getName() );

    private static final String EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR = "#$";
    private static final int NUMBER_OF_EXTRA_COLUMNS_ALLOWED = 2;

    @Autowired
    private BioMaterialService bioMaterialService;
    @Autowired
    private ExperimentalDesignService experimentalDesignService;
    @Autowired
    private OntologyService ontologyService;

    @Override
    @Transactional
    public void importDesign( ExpressionExperiment experiment, InputStream is ) throws IOException {
        ExperimentalDesignImporterImpl.log.debug( "Parsing input file" );
        boolean readHeader = false;

        BufferedReader r = new BufferedReader( new InputStreamReader( is ) );
        String line;

        ExperimentalDesign experimentalDesign = experiment.getExperimentalDesign();

        if ( !experimentalDesign.getExperimentalFactors().isEmpty() ) {
            ExperimentalDesignImporterImpl.log
                    .warn( "Experimental design already has factors, import will add new ones" );
        }

        experimentalDesign.setDescription( "Parsed from file." );

        List<String> experimentalFactorLines = new ArrayList<>();
        String sampleHeaderLine = "";
        List<String> factorValueLines = new ArrayList<>();

        while ( ( line = r.readLine() ) != null ) {
            if ( line.startsWith( ExperimentalDesignImporterImpl.EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR ) ) {
                experimentalFactorLines.add( line );
            } else if ( line.startsWith( "#" ) || StringUtils.isBlank( line ) ) {
                //noinspection UnnecessaryContinue // Better for readability
                continue;
            } else if ( !readHeader ) {
                sampleHeaderLine = line;
                readHeader = true;
            } else {
                factorValueLines.add( line );
            }
        }
        String[] headerFields = StringUtils.splitPreserveAllTokens( sampleHeaderLine, "\t" );

        Collection<BioMaterial> experimentBioMaterials = experiment.getBioAssays().stream()
                .map( BioAssay::getSampleUsed )
                .collect( Collectors.toSet() );

        this.validateFileComponents( experimentalFactorLines, sampleHeaderLine, factorValueLines );
        this.validateExperimentalFactorFileContent( experimentalFactorLines, sampleHeaderLine );
        this.validateFactorFileContent( experimentalFactorLines.size(), factorValueLines );
        this.validateBioMaterialFileContent( experimentBioMaterials, factorValueLines );

        // build up the composite: create experimental factor then add the experimental value
        this.addExperimentalFactorsToExperimentalDesign( experimentalDesign, experimentalFactorLines, headerFields,
                factorValueLines );

        assert !experimentalDesign.getExperimentalFactors().isEmpty();
        assert !experiment.getExperimentalDesign().getExperimentalFactors().isEmpty();

        experimentalDesignService.update( experimentalDesign );

        Collection<BioMaterial> bioMaterialsWithFactorValues = this
                .addFactorValuesToBioMaterialsInExpressionExperiment( experimentBioMaterials, experimentalDesign,
                        factorValueLines, headerFields );

        for ( BioMaterial bioMaterial : bioMaterialsWithFactorValues ) {
            this.bioMaterialService.update( bioMaterial );
        }

    }

    /**
     * This method reads the file line e.g. $Run time : Category=environmental_history Type=categorical and creates
     * experimental factors from it and adds them to the experimental design.
     * NOTE that this doesn't have the ability to add values to existing factors, which might be desirable.
     *
     * @param experimentalDesign          Experimental design for this expression experiment
     * @param experimentalFactorFileLines List of strings representing lines from input file containing experimental
     *                                    factors
     * @param headerFields                Sample header line split on tab.
     * @param factorValueLines            Lines containing biomaterial names and their factor values
     */
    private void addExperimentalFactorsToExperimentalDesign( ExperimentalDesign experimentalDesign,
            List<String> experimentalFactorFileLines, String[] headerFields, List<String> factorValueLines ) {

        Collection<OntologyTerm> terms = ontologyService.getCategoryTerms();
        if ( experimentalDesign.getExperimentalFactors() == null ) {
            experimentalDesign.setExperimentalFactors( new HashSet<ExperimentalFactor>() );
        }

        Map<String, Set<String>> mapFactorSampleValues = this
                .getMapFactorSampleValues( headerFields, factorValueLines );

        for ( String experimentalFactorFileLine : experimentalFactorFileLines ) {

            // $Run time : Category=EnvironmentalHistory Type=categorical
            String[] experimentalFactorfields = experimentalFactorFileLine.split( ":" );

            String factorValue = ( StringUtils.strip( experimentalFactorfields[0].replaceFirst(
                    Pattern.quote( ExperimentalDesignImporterImpl.EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR )
                            + "\\s*",
                    "" ) ) ).trim();
            String categoryAndType = StringUtils.strip( experimentalFactorfields[1] );
            String[] categoryAndTypeFields = StringUtils.split( categoryAndType );

            // e.g. Category=EnvironmentalHistory
            String category = categoryAndTypeFields[0];
            // e.g. EnvironmentalHistory
            String categoryValue = StringUtils.split( category, "=" )[1];

            ExperimentalFactor experimentalFactorFromFile = ExperimentalFactor.Factory.newInstance();
            experimentalFactorFromFile.setExperimentalDesign( experimentalDesign );
            Characteristic vc = this.termForCategoryLookup( categoryValue, terms );

            // e.g. Category=EnvironmentalHistory
            String categoryTypeValue = categoryAndTypeFields[1];
            String factorType = StringUtils.split( categoryTypeValue, "=" )[1];

            // vc.setCategory( categoryType );

            experimentalFactorFromFile.setCategory( vc );
            experimentalFactorFromFile.setName( factorValue );
            experimentalFactorFromFile.setDescription( factorValue );
            experimentalFactorFromFile.setType(
                    factorType.equalsIgnoreCase( "CATEGORICAL" ) ? FactorType.CATEGORICAL : FactorType.CONTINUOUS );

            this.addFactorValuesToExperimentalFactor( experimentalFactorFromFile, mapFactorSampleValues, factorType );

            if ( !this.checkForDuplicateExperimentalFactorOnExperimentalDesign( experimentalDesign,
                    experimentalFactorFromFile ) ) {
                experimentalDesign.getExperimentalFactors().add( experimentalFactorFromFile );
                ExperimentalDesignImporterImpl.log.info( "Added " + experimentalFactorFromFile );
            }
        }

    }

    /**
     * Add the factor values to the biomaterials
     *
     * @param  experimentBioMaterials Current expression experiment's biomaterials.
     * @param  experimentalDesign     experimental design
     * @param  factorValueLines       Lines from file containing factor values and biomaterial ids
     * @param  headerFields           header fields
     * @return Collection of biomaterials associated with this experiment, this is returned as
     *                                the biomaterial is in a
     *                                bioassay (first one retrieved)
     */
    private Collection<BioMaterial> addFactorValuesToBioMaterialsInExpressionExperiment(
            Collection<BioMaterial> experimentBioMaterials, ExperimentalDesign experimentalDesign,
            List<String> factorValueLines, String[] headerFields ) {
        ExperimentalDesignImporterImpl.log
                .debug( "Adding factors values to biomaterials: " + experimentalDesign.getId() );
        Collection<ExperimentalFactor> experimentalFactorsInExperiment = experimentalDesign.getExperimentalFactors();
        Collection<BioMaterial> biomaterialsWithFactorValuesInExperiment = new HashSet<>();

        Collection<BioMaterial> seenBioMaterials = new HashSet<>();

        Map<ExperimentalFactor, Collection<BioMaterial>> factorsAssociatedWithBioMaterials = new HashMap<>();

        for ( String factorValueLine : factorValueLines ) {
            String[] factorValueFields = StringUtils.splitPreserveAllTokens( factorValueLine, "\t" );

            String externalId = null;
            boolean hasExternalId = headerFields[1].toUpperCase().equals( "EXTERNALID" );
            if ( hasExternalId ) {
                externalId = factorValueFields[1];
            }
            BioMaterial currentBioMaterial = this
                    .getBioMaterialFromExpressionExperiment( experimentBioMaterials, factorValueFields[0], externalId );

            if ( currentBioMaterial == null ) {
                // this could just be due to extras.
                throw new IllegalStateException( "No biomaterial for " + factorValueFields[0] + ", " + factorValueFields[1] );
            }

            if ( seenBioMaterials.contains( currentBioMaterial ) ) {
                throw new IllegalArgumentException(
                        "A biomaterial occurred more than once in the file: " + currentBioMaterial );
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
                String currentFVtext = StringUtils.strip( factorValueFields[i] );

                if ( StringUtils.isBlank( currentFVtext ) ) {
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
                    String fvv = factorValue.getValue();
                    if ( StringUtils.isBlank( fvv ) ) {
                        // try characteristics; this would be a mess if there are more than one.
                        if ( factorValue.getCharacteristics().size() == 1 ) {
                            fvv = factorValue.getCharacteristics().iterator().next().getValue();
                            if ( StringUtils.isBlank( fvv ) ) {
                                continue; // we can't match to factor values that lack a value string.
                            }
                        }

                    }

                    if ( fvv.trim().equalsIgnoreCase( currentFVtext ) ) {
                        currentFactorValue = factorValue;
                    }
                }

                /*
                 * If we can't find the factorvalue that matches this, we don't get a value for this biomaterial.
                 */
                if ( currentFactorValue == null ) {
                    ExperimentalDesignImporterImpl.log
                            .error( "No factor value for " + currentExperimentalFactor + " matches the text value="
                                    + currentFVtext );
                } else {
                    if ( !this.checkForDuplicateFactorOnBioMaterial( currentBioMaterial, currentFactorValue ) ) {
                        currentBioMaterial.getFactorValues().add( currentFactorValue );
                    }
                }

                ExperimentalDesignImporterImpl.log
                        .debug( "Added factor value " + currentFactorValue + " to biomaterial " + currentBioMaterial );
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
                ExperimentalDesignImporterImpl.log
                        .warn( "File did not contain values for all factor - biomaterial combinations: Missing at least one for "
                                + ef + " [populated " + factorsAssociatedWithBioMaterials.get( ef ).size() + "/"
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
     * @param factorType         Whether the factor is continuous or categorical
     */
    private void addFactorValuesToExperimentalFactor( ExperimentalFactor experimentalFactor,
            Map<String, Set<String>> factorSampleValues, String factorType ) {
        ExperimentalDesignImporterImpl.log
                .debug( "Addding factors values to experimental factor: " + experimentalFactor.getName() );
        Characteristic category = experimentalFactor.getCategory();

        Set<String> values = factorSampleValues.get( experimentalFactor.getName() );
        for ( String value : values ) {

            FactorValue factorValue = FactorValue.Factory.newInstance();
            factorValue.setValue( value );

            if ( factorType.equalsIgnoreCase( "CATEGORICAL" ) ) {
                ExperimentalDesignImporterImpl.log.debug( "Factor is categorical" );
                Statement newVc = Statement.Factory.newInstance();
                if ( category != null ) {
                    newVc.setCategory( category.getCategory() );
                    newVc.setCategoryUri( category.getCategoryUri() );
                }
                newVc.setValue( value ); // don't have a valueUri at this point
                newVc.setEvidenceCode( GOEvidenceCode.IC );
                factorValue.getCharacteristics().add( newVc );
            } else {
                ExperimentalDesignImporterImpl.log.debug( "Factor is continous" );
                this.addMeasurementToFactorValueOfTypeContinous( factorValue );
            }
            // set bidirectional relationship
            experimentalFactor.getFactorValues().add( factorValue );
            factorValue.setExperimentalFactor( experimentalFactor );
            ExperimentalDesignImporterImpl.log
                    .debug( "Added factor value " + factorValue + " to experimental factor " + experimentalFactor );
        }

    }

    /**
     * Add a measurement to a factor value which is of type continuous
     *
     * @param factorValue representing a continuous factor with an associated measurement
     */
    private void addMeasurementToFactorValueOfTypeContinous( FactorValue factorValue ) {
        Measurement m = Measurement.Factory.newInstance();
        m.setType( MeasurementType.ABSOLUTE );
        m.setValue( factorValue.getValue() );
        try {
            //noinspection ResultOfMethodCallIgnored // check if it is a number, don't need the value.
            Double.parseDouble( factorValue.getValue() );
            m.setRepresentation( PrimitiveType.DOUBLE );
        } catch ( NumberFormatException e ) {
            m.setRepresentation( PrimitiveType.STRING );
        }

        factorValue.setMeasurement( m );
        ExperimentalDesignImporterImpl.log.debug( "Created " + factorValue + " for experimental factor " );

    }

    /**
     * @param  experimentalDesign         Existing experimental design.
     * @param  experimentalFactorFromFile The experimental factor in the file
     * @return Check that experimental design does not already contain the experimental
     *                                    factor.
     */
    private boolean checkForDuplicateExperimentalFactorOnExperimentalDesign( ExperimentalDesign experimentalDesign,
            ExperimentalFactor experimentalFactorFromFile ) {

        boolean foundMatch = false;
        for ( ExperimentalFactor existingExperimentalFactors : experimentalDesign.getExperimentalFactors() ) {
            if ( existingExperimentalFactors.getName().equals( experimentalFactorFromFile.getName() ) ) {
                ExperimentalDesignImporterImpl.log
                        .info( experimentalFactorFromFile + " matches existing " + existingExperimentalFactors );
                experimentalFactorFromFile = existingExperimentalFactors;
                foundMatch = true;
            }
        }
        return foundMatch;
    }

    /**
     * @param  bioMaterial bio material
     * @param  factorValue factor value
     * @return This method checks that the biomaterial does not already have a value for the factor.
     */
    private boolean checkForDuplicateFactorOnBioMaterial( BioMaterial bioMaterial, FactorValue factorValue ) {
        boolean foundMatch = false;
        // make sure we don't add two values.
        for ( FactorValue existingfv : bioMaterial.getAllFactorValues() ) {
            if ( factorValue.equals( existingfv ) || existingfv.getExperimentalFactor()
                    .equals( factorValue.getExperimentalFactor() ) ) {
                ExperimentalDesignImporterImpl.log
                        .warn( bioMaterial + " already has a factorvalue for " + factorValue.getExperimentalFactor()
                                + " [" + factorValue + " matched existing: " + existingfv + "]" );
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
     * @param  biomaterialNameFromFile - A factor value file line whose first column contains biomaterial name
     * @param  externalId              - the external id stored in the file, which might not be available (so this can
     *                                 be null or
     *                                 blank)
     * @return The biomaterial in the expression experiment set matching the biosource name
     *                                 given in the first column of
     *                                 the factor value line.
     */
    private BioMaterial getBioMaterialFromExpressionExperiment( Collection<BioMaterial> bioMaterials,
            String biomaterialNameFromFile, String externalId ) {

        Map<String, BioMaterial> biomaterialsInExpressionExperiment = this
                .mapBioMaterialsToNamePossibilities( bioMaterials );

        // format the biomaterial name gemma style

        BioMaterial bioMaterial = biomaterialsInExpressionExperiment.get( biomaterialNameFromFile );
        if ( bioMaterial == null ) {
            // try alternative format...
            bioMaterial = biomaterialsInExpressionExperiment.get( biomaterialNameFromFile );
        }

        if ( bioMaterial == null && StringUtils.isNotBlank( externalId ) ) {
            // If there are two or more GSM's grouped together we list them in the file separated by '/'.
            String[] externalIds = StringUtils.split( externalId, "/" );

            for ( String id : externalIds ) {
                bioMaterial = biomaterialsInExpressionExperiment.get( id );
                if ( bioMaterial != null )
                    break;
            }

        }

        return bioMaterial;
    }

    /**
     * Get a map of experimental values keyed on experimental factor name
     *
     * @param  headerFields     header fields
     * @param  factorValueLines factor value lines
     * @return map of experimental factor values keyed on experimental factor
     */
    private Map<String, Set<String>> getMapFactorSampleValues( String[] headerFields, List<String> factorValueLines ) {
        Map<String, Set<String>> factorSampleValues = new HashMap<>();
        for ( String factorValueLine : factorValueLines ) {
            String[] factorValueFields = StringUtils.splitPreserveAllTokens( factorValueLine, "\t" );

            for ( int i = 1; i < headerFields.length; i++ ) {

                // get the key
                String value = headerFields[i];
                value = StringUtils.strip( value );
                String factorValue = StringUtils.strip( factorValueFields[i] );
                Set<String> listFactorValues = factorSampleValues.get( value );
                if ( listFactorValues == null ) {
                    listFactorValues = new HashSet<>();
                }
                listFactorValues.add( factorValue );
                factorSampleValues.put( value, listFactorValues );

            }

        }
        return factorSampleValues;

    }

    /**
     * @param  bioMaterials bio materials
     * @return a map of various strings that we might find in a design importing file to the biomaterials.
     */
    private Map<String, BioMaterial> mapBioMaterialsToNamePossibilities( Collection<BioMaterial> bioMaterials ) {
        Map<String, BioMaterial> biomaterialsInExpressionExperiment = new HashMap<>();

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
            biomaterialsInExpressionExperiment
                    .put( ExpressionDataWriterUtils.constructBioAssayName( bm, bm.getBioAssaysUsedIn() ), bm );

        }
        return biomaterialsInExpressionExperiment;
    }

    /**
     * Does a lookup for the Ontology term to match the category.
     *
     * @param  category category
     * @return vocab characteristic
     */
    private Characteristic termForCategoryLookup( String category, Collection<OntologyTerm> terms ) {

        OntologyTerm t = null;
        String lookup = category.replaceAll( "_", " " );
        for ( OntologyTerm to : terms ) {
            if ( category.equalsIgnoreCase( to.getLabel() )
                    || lookup.equalsIgnoreCase( to.getLabel() )
                    // if EFO is loaded, the "sex" term is actually named "biological sex"
                    || ( "biological sex".equalsIgnoreCase( to.getLabel() ) && "sex".equalsIgnoreCase( lookup ) ) ) {
                t = to;
                break;
            }
        }

        if ( t == null ) {
            throw new IllegalArgumentException( "No term matches '" + category + "' - formalized to: " + lookup );
        }

        Characteristic vc = Characteristic.Factory.newInstance();
        vc.setCategoryUri( t.getUri() );
        vc.setCategory( t.getLabel() );
        vc.setValueUri( t.getUri() );
        vc.setValue( t.getLabel() );
        vc.setEvidenceCode( GOEvidenceCode.IC );
        return vc;
    }

    /**
     * Check that the biomaterial is in the file and in the experiment. It is arguable whether this should be an
     * exception. I think it has to be to make sure that simple errors in the format are caught. But it's inconvenient
     * for cases where a single 'design' file is to be used for multiple microarray studies. Biomaterial ids should
     * match what is stored
     *
     * @param factorValueLines Lines containing biomaterial names and their factor values
     */
    private void validateBioMaterialFileContent( Collection<BioMaterial> bioMaterials, List<String> factorValueLines )
            throws IllegalArgumentException {

        for ( String factorValueLine : factorValueLines ) {
            String[] vals = StringUtils.splitPreserveAllTokens( factorValueLine, '\t' );
            if ( vals.length < 2 ) {
                throw new IllegalArgumentException(
                        "Expected a file with at least two columns separated by tabs, got " + factorValueLine );
            }
            BioMaterial bioMaterialInFile = this
                    .getBioMaterialFromExpressionExperiment( bioMaterials, vals[0], vals[1] );
            if ( bioMaterialInFile == null ) {
                // these might just be "extras" but we're being strict
                throw new IllegalArgumentException(
                        "The uploaded file has a biomaterial name/ID that does not match the study: " + vals[0] + ", " + vals[1] );
            }
        }
    }

    /**
     * Validates that the input for experimental factors is correct: Experimental factor file line should be for e.g.
     * #$Run time : Category=EnvironmentalHistory Type=categorical Checks there is a colon, between experimental factor
     * and category and that category is correctly formatted.
     *
     * @param  sampleHeaderLine        Lines in file corresponding to order of experimental factors
     * @param  experimentalFactorLines The lines in the file corresponding to experimental factors.
     * @throws IOException             Experimental factor lines were not correctly format.
     */
    private void validateExperimentalFactorFileContent( List<String> experimentalFactorLines, String sampleHeaderLine )
            throws IOException {
        Set<String> experimentalFactorValueNames = new HashSet<>();
        // validate experimental factor lines
        for ( String line : experimentalFactorLines ) {
            String[] fields = line.split( ":" );
            if ( fields.length != 2 ) {
                throw new IOException(
                        "EF description must have two fields with a single ':' in between (" + line + ")" );
            }
            String factorName = StringUtils.strip( fields[0].replaceFirst(
                    Pattern.quote( ExperimentalDesignImporterImpl.EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR )
                            + "\\s*",
                    "" ) );

            experimentalFactorValueNames.add( factorName );
            String category = StringUtils.strip( fields[1] );

            String[] descriptions = StringUtils.split( category );

            if ( descriptions.length != 2 ) {
                throw new IOException( "EF details should have the format 'Category=CATEGORY Type=TYPE'" );
            }

        }

        this.validateSampleHeaderFileContent( experimentalFactorValueNames, experimentalFactorLines.size(),
                sampleHeaderLine );

    }

    /**
     * Validates that factor values given in file for each biomaterial match the number of experimental factor values
     * expected.
     *
     * @param factorValueList             Represents lines of file containing factor values for a biomaterial
     * @param numberOfExperimentalFactors number of experimental factors
     */
    private void validateFactorFileContent( Integer numberOfExperimentalFactors, List<String> factorValueList )
            throws IOException {
        for ( String factorValueLine : factorValueList ) {
            String[] fields = StringUtils.splitPreserveAllTokens( factorValueLine, "\t" );
            if ( fields.length > numberOfExperimentalFactors + ExperimentalDesignImporterImpl.NUMBER_OF_EXTRA_COLUMNS_ALLOWED ) {
                throw new IOException( "Expected no more than " + ( numberOfExperimentalFactors
                        + ExperimentalDesignImporterImpl.NUMBER_OF_EXTRA_COLUMNS_ALLOWED )
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
     * @param  experimentalFactorLines Lines identified by EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR (#$) detailing
     *                                 experimental factor values.
     * @param  sampleHeaderLine        Header Giving order of experimental factor values in the file
     * @param  factorValues            The factor values in this file
     * @throws IOException             File was not in correct format.
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
     * @param  experimentalFactorValueNames experimental factor value names
     * @param  numberOfExperimentalFactors  number fo EFs
     * @param  sampleHeaderLine             sample header line
     * @throws IOException                  Validation fails.
     */
    private void validateSampleHeaderFileContent( Set<String> experimentalFactorValueNames,
            Integer numberOfExperimentalFactors, String sampleHeaderLine ) throws IOException {
        String[] headerFields = StringUtils.splitPreserveAllTokens( sampleHeaderLine, "\t" );

        // we might have the ids, and the external id.
        if ( headerFields.length > numberOfExperimentalFactors + ExperimentalDesignImporterImpl.NUMBER_OF_EXTRA_COLUMNS_ALLOWED ) {
            throw new IOException( "Expected " + ( numberOfExperimentalFactors
                    + ExperimentalDesignImporterImpl.NUMBER_OF_EXTRA_COLUMNS_ALLOWED )
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
