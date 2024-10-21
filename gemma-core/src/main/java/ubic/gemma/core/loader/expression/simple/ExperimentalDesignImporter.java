/*
 * The Gemma project
 *
 * Copyright (c) 2009 University of British Columbia
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

import org.springframework.security.access.annotation.Secured;
import ubic.gemma.core.datastructure.matrix.io.ExperimentalDesignWriter;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.InputStream;

/**
 * <p>
 * Parse a description of ExperimentalFactors from a file, and associate it with a given ExpressionExperiment. The
 * format is specified by {@link ExperimentalDesignWriter}
 * </p>
 * <p>
 * Example of format, where 'Category' is an MGED term and 'Type' is either "Categorical' or 'Continuous' with no extra
 * white space around the '='s. The ID column MUST match the names on the BioAssays the design will be attached to. Main
 * section is tab-delimited. Column headings in the main table must match the identifiers given in the header.
 * </p>
 * <pre>
 *    #$ Age : Category=Age Type=Continuous
 *    #$ Profile : Category=DiseaseState Type=Categorical
 *    #$ PMI (h) : Category=EnvironmentalHistory Type=Continuous
 *    #$ Lifetime Alcohol : Category=EnvironmentalHistory Type=Categorical
 *    #ID  Age     Profile     PMI (h)     Lifetime Alcohol
 *    f-aa     50  Bipolar     48  Moderate present
 *    f-ab     50  Bipolar     60  Heavy in present
 *    f-ac     55  Schizophrenia   26  Little or none
 *    f-ad     35  Bipolar     28  Unknown
 *    f-af     60  Bipolar     70  Little or none
 * </pre>
 * Note: Files downloaded from Gemma may have an "ExternalID" column after the first column. This is allowed in the
 * import format.
 *
 * @author Paul
 * @see ExperimentalDesignWriter
 */
public interface ExperimentalDesignImporter {

    /**
     * This is the main builder director method of the application: It processes the input file containing information
     * about the experimental design for a given expression experiment. There are 3 main steps in the workflow:
     * <p>
     * Step1 - validate the the 3 components of the file which should contain: The first component is the experimental
     * factor lines marked with a $# there are as many lines as experimental factors - (experimentalFactorLines). Then
     * one line containing header information indicating the order of the experimental factors in the file -
     * sampleHeaderLine Finally factor values, the first column of which is the sample or biomaterial ids and thereafter
     * factor values.
     * </p>
     * <p>
     * Step 2 the file components are mapped to objects to populate the experimental design, before addition of objects
     * to the composite existing values are checked for. The expression experiment composite: ExpressionExperiments have
     * an experimental design which have experimental factors. Experimental factors have factor values. BioMaterials
     * have factor values. Bioassays have biomaterials, bioassays are in an expression experiment which completes the
     * circle.
     * </p>
     * <p>
     * Step 3 on successful validation and object creation the experimental design is persisted following a strict
     * order, expression factors first then biomaterial details.
     * </p>
     *
     * @param experiment the one to add the experimental design
     * @param is         File to process
     * @throws IOException when IO problems occur.
     * @see ubic.gemma.core.loader.expression.simple.ExperimentalDesignImporter
     * #importDesign(ubic.gemma.model.expression.experiment .ExpressionExperiment, java.io.InputStream, boolean)
     */
    @Secured({ "GROUP_USER", "ACL_SECURABLE_EDIT" })
    void importDesign( ExpressionExperiment experiment, InputStream is ) throws IOException;

}