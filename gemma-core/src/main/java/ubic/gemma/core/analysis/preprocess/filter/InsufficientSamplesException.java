/*
 * The Gemma project
 *
 * Copyright (c) 2007 Columbia University
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
package ubic.gemma.core.analysis.preprocess.filter;

/**
 * Exception indicating that there are insufficient samples (columns) in the dataset to perform a particular filter.
 * @author Paul
 */
public class InsufficientSamplesException extends InsufficientDataException {

    private static final long serialVersionUID = 1L;

    public InsufficientSamplesException( String message ) {
        super( message );
    }
}