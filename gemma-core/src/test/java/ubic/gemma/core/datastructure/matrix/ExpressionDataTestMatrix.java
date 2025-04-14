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

package ubic.gemma.core.datastructure.matrix;

import ubic.basecode.dataStructure.matrix.DoubleMatrix;
import ubic.basecode.io.reader.DoubleMatrixReader;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderService;
import ubic.gemma.core.loader.expression.simple.SimpleExpressionDataLoaderServiceImpl;
import ubic.gemma.core.loader.expression.simple.model.SimpleExpressionExperimentMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimplePlatformMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleQuantitationTypeMetadata;
import ubic.gemma.core.loader.expression.simple.model.SimpleTaxonMetadata;
import ubic.gemma.model.common.quantitationtype.GeneralType;
import ubic.gemma.model.common.quantitationtype.ScaleType;
import ubic.gemma.model.common.quantitationtype.StandardQuantitationType;
import ubic.gemma.model.expression.bioAssayData.BulkExpressionDataVector;
import ubic.gemma.model.expression.experiment.ExpressionExperiment;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashSet;

/**
 * Creates a sample small test matrix, not persistent. Useful for testing algorithms. (This is not a test!)
 *
 * @author paul
 */
public class ExpressionDataTestMatrix extends ExpressionDataDoubleMatrix {

    private static final long serialVersionUID = 1L;

    public ExpressionDataTestMatrix() {
        super();
    }
}
