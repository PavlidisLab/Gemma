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
package ubic.gemma.web.controller.util.view;

import java.util.Collection;
import java.util.List;

/**
 * Creates a response that can be consumed by an Ext.data.JsonReader. The client-side Ext.data.JsonReader must have the
 * "root" property set to "records". Note: Ext documentation often uses "rows" for this property, but "records" is more
 * clear. Example Ext.data.JsonReader configuration:
 * <pre>
 * {
 *  root : 'records',
 *  successProperty : 'success', // same as default.
 *  messageProperty : 'message',
 *  totalProperty : 'totalRecords'
 * }
 * </pre>
 * If the parameterized type has two properties "field1" and "field2", then when an instance of this class is read by
 * the client and there are 100 records in total to page through, it will look like:
 * <pre>
 *  {
 *      records : [
 *          {
 *              field1 : 'value',
 *              field2 : 'value',
 *          }, {
 *              field1 : 'value',
 *              field2 : 'value',
 *          }
 *      ],
 *      success : true,
 *      totalRecords : 100,  // Only needed if you are paging.
 *      message : &quot;Yay!&quot; // optional.
 * }
 * </pre>
 * Note that for Gemma, 'success' is rarely all that useful because even though DWR will always (?) return a 200, the
 * error handler gets called.
 *
 * @param <T> Type of Objects that will be converted to Ext.data.Records by the client-side Ext.data.DataReader.
 * @author paul (based on <a href="https://github.com/BigLep/ExtJsWithDwr/">BigLep/ExtJsWithDwr</a>)
 *
 */
public class JsonReaderResponse<T> {

    public String message = "";
    public Collection<T> records;
    public boolean success;
    public long totalRecords = 0;

    /**
     * Creates a successful JsonReaderResponse with the provided objectsToConvertToRecords. The
     * totalRecords is assumed to be the length of objectsToConvertToRecords
     */
    public JsonReaderResponse( List<T> objectsToConvertToRecords ) {
        this.records = objectsToConvertToRecords;
        this.totalRecords = records.size();
        success = true;
    }

    /**
     * Use for remote paging applications. Creates a {@link #success}ful JsonReaderResponse with the provided objectsToConvertToRecords.
     *
     * @param objectsToConvertToRecords objects to convert
     * @param totalRecords              total records
     */
    public JsonReaderResponse( Collection<T> objectsToConvertToRecords, long totalRecords ) {
        this.records = objectsToConvertToRecords;
        this.success = true;
        this.totalRecords = totalRecords;
    }

    /**
     * Creates an un{@link #success}ful JsonReaderResponse with null {@link #records}. This signals the case where the
     * client established a connection with the server, but the server couldn't fulfill it (e.g., user doesn't have
     * proper user credentials).
     *
     * @param message an error message to give to the client.
     */
    public JsonReaderResponse( String message ) {
        this.message = message;
        this.records = null;
        success = false;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage( String message ) {
        this.message = message;
    }

    /**
     * @return the records
     */
    public Collection<T> getRecords() {
        return records;
    }

    /**
     * @param records the records to set
     */
    public void setRecords( List<T> records ) {
        this.records = records;
    }

    /**
     * @return the totalRecords
     */
    public long getTotalRecords() {
        return totalRecords;
    }

    /**
     * @param totalRecords the totalRecords to set
     */
    public void setTotalRecords( long totalRecords ) {
        this.totalRecords = totalRecords;
    }

    /**
     * @return the success
     */
    public boolean isSuccess() {
        return success;
    }

    /**
     * @param success the success to set
     */
    public void setSuccess( boolean success ) {
        this.success = success;
    }
}