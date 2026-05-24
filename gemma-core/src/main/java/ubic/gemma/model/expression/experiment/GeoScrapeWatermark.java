/*
 * The Gemma project
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.model.expression.experiment;

import java.io.Serializable;
import java.util.Date;

/**
 * Append-only record of one GEO scrape run. One row per
 * {@code POST /admin/tasks/geo-scrape} invocation.
 *
 * <p>The pair {@code scanFrom}/{@code scanTo} defines the time window the
 * scrape covered. The latest {@link Status#COMPLETED} row's {@code scanTo}
 * is the high-water mark the next scrape resumes from. {@code status}
 * tracks lifecycle (IN_PROGRESS, COMPLETED, FAILED, CANCELLED) so
 * partial / wedged scrapes can be inspected and resumed.</p>
 *
 * <p>Schema: Flyway {@code mysql/V17__geo_scrape_watermark.sql} +
 * {@code h2/V19__geo_scrape_watermark.sql}. Paul-approved 2026-05-23.</p>
 */
public class GeoScrapeWatermark implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Lifecycle status of a scrape run. String-valued column with the enum
     * constant name stored verbatim (useNamed=true).
     */
    public enum Status {
        IN_PROGRESS,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    private Long id;
    private Date scannedAt;
    private Date scanFrom;
    private Date scanTo;
    private int recordsScanned;
    private int recordsMatched;
    private String criteriaApplied;
    private Status status = Status.IN_PROGRESS;
    private String errorMessage;

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    /** When the scrape started (or was last updated). NOT NULL. */
    public Date getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt( Date scannedAt ) {
        this.scannedAt = scannedAt;
    }

    /** Inclusive lower bound of the scrape window; null if the first scrape ever. */
    public Date getScanFrom() {
        return scanFrom;
    }

    public void setScanFrom( Date scanFrom ) {
        this.scanFrom = scanFrom;
    }

    /** Exclusive upper bound of the scrape window; null while IN_PROGRESS. */
    public Date getScanTo() {
        return scanTo;
    }

    public void setScanTo( Date scanTo ) {
        this.scanTo = scanTo;
    }

    public int getRecordsScanned() {
        return recordsScanned;
    }

    public void setRecordsScanned( int recordsScanned ) {
        this.recordsScanned = recordsScanned;
    }

    public int getRecordsMatched() {
        return recordsMatched;
    }

    public void setRecordsMatched( int recordsMatched ) {
        this.recordsMatched = recordsMatched;
    }

    /**
     * Comma-separated list of matcher names applied (e.g. {@code "brain,scbrain,tfperturb"});
     * null means "all available matchers".
     */
    public String getCriteriaApplied() {
        return criteriaApplied;
    }

    public void setCriteriaApplied( String criteriaApplied ) {
        this.criteriaApplied = criteriaApplied;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus( Status status ) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage( String errorMessage ) {
        this.errorMessage = errorMessage;
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : System.identityHashCode( this );
    }

    @Override
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !( o instanceof GeoScrapeWatermark ) ) return false;
        GeoScrapeWatermark other = ( GeoScrapeWatermark ) o;
        if ( id != null && other.id != null ) return id.equals( other.id );
        return false;
    }
}
