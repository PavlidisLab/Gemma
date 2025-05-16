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
package ubic.gemma.core.loader.expression.simple.model;

import lombok.Data;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

/**
 * Represents the basic data to enter about an expression experiment when starting from a delimited file of data
 *
 * @author pavlidis
 */
@Data
public class SimpleExpressionExperimentMetadata implements Serializable {

    private String shortName;

    private String name;

    @Nullable
    private String description;

    /**
     * Platforms associated to the experiments.
     * <p>
     * Once declared here, they may be referred to in {@link #samples}.
     */
    private Collection<SimplePlatformMetadata> arrayDesigns = new HashSet<>();

    private SimpleTaxonMetadata taxon;

    @Nullable
    private SimpleDatabaseEntry accession;

    /**
     * PubMed identifier.
     */
    @Nullable
    private String pubMedId;

    @Nullable
    private String source;

    // experimental design metadata
    private String experimentalDesignDescription = "No information available";
    private String experimentalDesignName = "Unknown";

    /**
     * Required if data is provided.
     */
    @Nullable
    private SimpleQuantitationTypeMetadata quantitationType;

    /**
     * If true, biological characteristics imaging the probes will be created.
     */
    private boolean probeIdsAreImageClones;

    /**
     * Samples to be associated with this experiment.
     * <p>
     * If left unset, the assays will be inferred from the data file.
     */
    private Collection<SimpleSampleMetadata> samples = new HashSet<>();
}
