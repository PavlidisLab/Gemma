/*
 * The gemma project
 * 
 * Copyright (c) 2013 University of British Columbia
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
package ubic.gemma.persistence.util;

/**
 * author: anton
 * date: 27/03/13
 */
public class TaskCancelledException extends RuntimeException {
    /**
     * 
     */
    private static final long serialVersionUID = 7343146551545342910L;

    public TaskCancelledException() {
        super();
    }

    public TaskCancelledException(String message) {
        super(message);
    }
}