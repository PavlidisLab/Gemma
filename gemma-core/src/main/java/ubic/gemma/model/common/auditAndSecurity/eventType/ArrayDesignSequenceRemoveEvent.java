/*
 * The Gemma project.
 *
 * Copyright (c) 2006-2012 University of British Columbia
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
package ubic.gemma.model.common.auditAndSecurity.eventType;

/**
 * <p>
 * Used to indicate that all associations that this array design has with BioSequences have been removed. This is needed
 * for cases where the original import has associated the probes with the wrong sequences. A common case is for GEO data
 * sets where the actual oligonucleotide is not given. Instead the submitter provides GenBank accessions, which are
 * misleading.
 * </p>
 */
public class ArrayDesignSequenceRemoveEvent
        extends ubic.gemma.model.common.auditAndSecurity.eventType.ArrayDesignAnalysisEvent {


}