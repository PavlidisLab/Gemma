/*
 * The Gemma project.
 *
 * Copyright (c) 2026 University of British Columbia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 */
package ubic.gemma.core.pipeline;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;

/**
 * Per-job tokens for the Nextflow weblog callback.
 * <p>
 * {@code nextflow run -with-weblog <url>} cannot send an {@code Authorization} header, so the credential
 * has to travel in the URL — and that URL ends up in {@code launch.sh} and {@code .nextflow.log} under
 * the work-dir, which the whole {@code pavlab} group can read. Putting the shared callback secret there
 * would let anyone who reads one work-dir post events for every job. The token is instead
 * {@code HMAC-SHA256(secret, jobId)}: it authorizes events for that one job, and Gemma can check it
 * without storing anything.
 */
public final class PipelineCallbackTokens {

    private static final String ALGORITHM = "HmacSHA256";

    private PipelineCallbackTokens() {
    }

    /**
     * @return the hex token for {@code jobId}
     * @throws IllegalArgumentException if {@code secret} is blank
     */
    public static String forJob( String secret, long jobId ) {
        if ( secret == null || secret.isBlank() ) {
            throw new IllegalArgumentException( "the pipeline callback secret is not configured" );
        }
        try {
            Mac mac = Mac.getInstance( ALGORITHM );
            mac.init( new SecretKeySpec( secret.getBytes( StandardCharsets.UTF_8 ), ALGORITHM ) );
            return HexFormat.of().formatHex( mac.doFinal( Long.toString( jobId ).getBytes( StandardCharsets.UTF_8 ) ) );
        } catch ( GeneralSecurityException e ) {
            // HmacSHA256 is a mandatory JCA algorithm; reaching this means a broken JRE.
            throw new IllegalStateException( e );
        }
    }

    /**
     * Constant-time check of a supplied token against the one {@link #forJob} derives. False when the
     * secret is blank, so an unconfigured server rejects every callback.
     */
    public static boolean matches( String secret, long jobId, String supplied ) {
        if ( secret == null || secret.isBlank() || supplied == null ) {
            return false;
        }
        return MessageDigest.isEqual( forJob( secret, jobId ).getBytes( StandardCharsets.UTF_8 ),
                supplied.getBytes( StandardCharsets.UTF_8 ) );
    }
}
