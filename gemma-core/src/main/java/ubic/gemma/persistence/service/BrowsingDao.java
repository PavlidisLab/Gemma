/*
 * The Gemma project
 *
 * Copyright (c) 2010 University of British Columbia
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

package ubic.gemma.persistence.service;

import ubic.gemma.model.common.Identifiable;

import java.util.List;

/**
 * Support for paging through the data.
 *
 * @author paul
 */
public interface BrowsingDao<T extends Identifiable> extends BaseDao<T> {

    List<T> browse( int start, int limit );

    List<T> browse( int start, int limit, String orderField, boolean descending );

}
