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
package ubic.gemma.core.datastructure.matrix.io;

import lombok.Setter;
import org.springframework.util.Assert;
import ubic.basecode.util.StringUtil;
import ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporterImpl;
import ubic.gemma.core.util.BuildInfo;
import ubic.gemma.core.util.TsvUtils;
import ubic.gemma.model.expression.bioAssay.BioAssay;
import ubic.gemma.model.expression.biomaterial.BioMaterial;
import ubic.gemma.model.expression.experiment.*;
import ubic.gemma.persistence.util.EntityUrlBuilder;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static ubic.gemma.core.datastructure.matrix.io.ExpressionDataWriterUtils.*;
import static ubic.gemma.core.util.TsvUtils.format;

/**
 * Output compatible with {@link ExperimentalDesignImporterImpl}.
 *
 * @author keshav
 * @see ExperimentalDesignImporterImpl
 */
public class ExperimentalDesignWriter {

    private static final String EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR = "#$";

    private final EntityUrlBuilder entityUrlBuilder;
    private final BuildInfo buildInfo;
    private final boolean autoFlush;

    /**
     * Write assays over multiple rows.
     * <p>
     * This simplifies merging the experimental design with the expression data as the "Bioassay" column will match
     * exactly the columns of the matrix written by {@link MatrixWriter}.
     * <p>
     * If separating identifiers with {@link #separateSampleFromAssaysIdentifiers}, {@link MatrixWriter#setOnlyIncludeBioAssayIdentifiers(boolean)}
     * can be used to match the "Assay" column instead.
     */
    @Setter
    private boolean useMultipleRowsForAssays = false;
    /**
     * If true, write the sample identifier and the assay(s) identifiers in separate columns.
     * <p>
     * The column will be named "Sample" and "Assays" respectively. If {@link #useMultipleRowsForAssays} is true, the
     * assay column will be named "Assay".
     * <p>
     * The default column name for mangled sample and assay identifiers is "Bioassay".
     */
    @Setter
    private boolean separateSampleFromAssaysIdentifiers = false;
    /**
     * If true, use the IDs of the {@link BioAssay}s (and {@link BioMaterial}s.
     */
    @Setter
    private boolean useBioAssayIds = false;
    /**
     * If true, use column names as they appear in the database.
     */
    @Setter
    private boolean useRawColumnNames = false;

    public ExperimentalDesignWriter( EntityUrlBuilder entityUrlBuilder, BuildInfo buildInfo, boolean autoFlush ) {
        this.entityUrlBuilder = entityUrlBuilder;
        this.buildInfo = buildInfo;
        this.autoFlush = autoFlush;
    }

    /**
     * Write the experimental design of the given {@link ExpressionExperiment} to the given {@link Writer}.
     * @see #write(ExpressionExperiment, Collection, boolean, Writer)
     */
    public void write( ExpressionExperiment ee, Writer writer ) throws IOException {
        write( ee, ee.getBioAssays(), true, writer );
    }

    /**
     * Write the experimental design of the given {@link ExpressionExperiment} to the given {@link Writer} for a given
     * collection of assays.
     * @param bioAssays       assays to write, the order is defined by the order of their corresponding biomaterials
     *                        as per {@link BioMaterial#COMPARATOR}.
     * @param writeBaseHeader whether to write the base header (experiment URL, build info, etc.), see
     *                        {@link ExpressionDataWriterUtils#appendBaseHeader(ExpressionExperiment, String, String, BuildInfo, Date, Writer)}
     *                        for details
     */
    public void write( ExpressionExperiment ee, Collection<BioAssay> bioAssays, boolean writeBaseHeader, Writer writer ) throws IOException {
        Assert.isTrue( ee.getExperimentalDesign() != null && !ee.getExperimentalDesign().getExperimentalFactors().isEmpty(),
                ee + " does not have an experimental design." );

        ExperimentalDesign ed = ee.getExperimentalDesign();

        /*
         * See BaseExpressionDataMatrix.setUpColumnElements() for how this is constructed for the DataMatrix, and for
         * some notes about complications.
         */
        SortedMap<BioMaterial, Set<BioAssay>> bioMaterials = new TreeMap<>( BioMaterial.COMPARATOR );
        for ( BioAssay bioAssay : bioAssays ) {
            BioMaterial bm = bioAssay.getSampleUsed();
            bioMaterials.computeIfAbsent( bm, k -> new HashSet<>() ).add( bioAssay );
        }

        List<ExperimentalFactor> orderedFactors = ed.getExperimentalFactors()
                .stream()
                .sorted( ExperimentalFactor.COMPARATOR )
                .collect( Collectors.toList() );

        this.writeHeader( ee, orderedFactors, writeBaseHeader, writer );

        Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap = ExperimentalDesignUtils.getFactorValueMap( ed, bioMaterials.keySet() );

        for ( Map.Entry<BioMaterial, Set<BioAssay>> e : bioMaterials.entrySet() ) {
            if ( useMultipleRowsForAssays ) {
                for ( BioAssay assay : e.getValue() ) {
                    writeRow( e.getKey(), Collections.singleton( assay ), orderedFactors, factorValueMap, writer );
                }
            } else {
                writeRow( e.getKey(), e.getValue(), orderedFactors, factorValueMap, writer );
            }
        }
    }

    /**
     * Write an (R-friendly) header
     */
    private void writeHeader( ExpressionExperiment expressionExperiment, List<ExperimentalFactor> factors,
            boolean writeBaseHeader, Writer buf ) throws IOException {

        if ( writeBaseHeader ) {
            String experimentUrl = entityUrlBuilder.fromHostUrl().entity( expressionExperiment ).web().toUriString();
            appendBaseHeader( expressionExperiment, "Expression design", experimentUrl, buildInfo, new Date(), buf );
            if ( autoFlush ) {
                buf.flush();
            }
        }

        String[] factorColumnNames;
        if ( useRawColumnNames ) {
            factorColumnNames = factors.stream()
                    .map( ExperimentalFactor::getName )
                    .toArray( String[]::new );
            factorColumnNames = StringUtil.makeUnique( factorColumnNames );
        } else {
            factorColumnNames = constructExperimentalFactorNames( factors );
        }

        for ( int i = 0; i < factors.size(); i++ ) {
            ExperimentalFactor ef = factors.get( i );
            buf.append( ExperimentalDesignWriter.EXPERIMENTAL_FACTOR_DESCRIPTION_LINE_INDICATOR );
            buf.append( factorColumnNames[i] ).append( " :" );
            if ( ef.getCategory() != null ) {
                buf.append( " Category=" ).append( ef.getCategory().getValue().replaceAll( "\\s", "_" ) );
            }
            buf.append( " Type=" );

            if ( ef.getType().equals( FactorType.CATEGORICAL ) ) {
                buf.append( "Categorical" );
            } else {
                buf.append( "Continuous" );
            }

            buf.append( "\n" );
            if ( autoFlush ) {
                buf.flush();
            }
        }

        if ( useMultipleRowsForAssays ) {
            if ( separateSampleFromAssaysIdentifiers ) {
                buf.append( "Sample\tAssay" );
            } else {
                buf.append( "Bioassay" );
            }
        } else {
            if ( separateSampleFromAssaysIdentifiers ) {
                buf.append( "Sample\tAssays" );
            } else {
                buf.append( "Bioassay" );
            }
        }

        buf.append( "\tExternalID" );

        for ( String columnName : factorColumnNames ) {
            buf.append( "\t" ).append( format( columnName ) );
        }

        buf.append( "\n" );
        if ( autoFlush ) {
            buf.flush();
        }
    }

    private void writeRow( BioMaterial bioMaterial, Set<BioAssay> assays, List<ExperimentalFactor> orderedFactors, Map<ExperimentalFactor, Map<BioMaterial, FactorValue>> factorValueMap, Writer writer ) throws IOException {
        if ( separateSampleFromAssaysIdentifiers ) {
            writer.append( format( constructSampleName( bioMaterial, useBioAssayIds, useRawColumnNames ) ) );
            writer.append( "\t" ).append( format( constructAssaysName( assays, useBioAssayIds, useRawColumnNames, TsvUtils.SUB_DELIMITER ) ) );
        } else {
            writer.append( format( constructSampleName( bioMaterial, assays, useBioAssayIds, useRawColumnNames, '.' ) ) );
        }

        /* column 1 */
        writer.append( "\t" ).append( formatSampleExternalId( bioMaterial, assays ) );

        /* columns 2 ... n where n+1 is the number of factors */
        for ( ExperimentalFactor ef : orderedFactors ) {
            writer.append( "\t" );
            FactorValue value = factorValueMap.get( ef ).get( bioMaterial );
            if ( value != null ) {
                writer.append( format( FactorValueUtils.getValues( value ) ) );
            } else {
                writer.append( TsvUtils.NA );
            }
        }
        writer.append( "\n" );
        if ( autoFlush ) {
            writer.flush();
        }
    }


    /**
     * @param bioAssays   BAs
     * @param bioMaterial BM
     * @return string representing the external identifier of the biomaterial. This will usually be a GEO or
     * ArrayExpress accession, or {@code null} if no such identifier is available.
     */
    @Nullable
    private String formatSampleExternalId( BioMaterial bioMaterial, Collection<BioAssay> bioAssays ) {
        if ( bioMaterial.getExternalAccession() != null ) {
            return format( bioMaterial.getExternalAccession().getAccession() );
        } else if ( !bioAssays.isEmpty() ) {
            // use the external IDs of the associated bioassays
            SortedSet<String> ids = new TreeSet<>();
            for ( BioAssay ba : bioAssays ) {
                if ( ba.getAccession() != null ) {
                    ids.add( ba.getAccession().getAccession() );
                }
            }
            return format( ids );
        } else {
            return TsvUtils.NA;
        }
    }
}
