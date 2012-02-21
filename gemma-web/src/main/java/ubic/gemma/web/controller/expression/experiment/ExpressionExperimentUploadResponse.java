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
package ubic.gemma.web.controller.expression.experiment;

public class ExpressionExperimentUploadResponse {
    
    private Long taskId;
    
    private String errorMessage;
    
    private boolean error;

    public void setTaskId( Long taskId ) {
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setErrorMessage( String errorMessage ) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setError( boolean error ) {
        this.error = error;
    }

    public boolean isError() {
        return error;
    }
    
    
}
