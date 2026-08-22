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
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package ubic.gemma.apps;

import org.springframework.beans.factory.annotation.Autowired;
import ubic.gemma.core.analysis.report.ArrayDesignReportService;
import ubic.gemma.model.expression.arrayDesign.ArrayDesign;

import java.util.Collection;

/**
 * Regenerate the on-disk platform reports — the per-platform element / sequence / alignment / gene
 * counts that {@code ArrayDesignReportService} serializes under
 * {@code ${gemma.appdata.home}/ArrayDesignReports}.
 * <p>
 * These counts are expensive (counting distinct genes for one large platform measures ~1.7&nbsp;s
 * against production) and change only when a platform's sequences or gene mappings are recomputed,
 * so they are computed ahead of time rather than per request. {@code GET /platforms} serves
 * {@code numberOfGenes} / {@code numberOfMappedElements} from these files.
 * <p>
 * Until now nothing could write them on demand. The pipeline CLIs regenerate a single platform's
 * report as a side effect of remapping it, and {@code SchedulerConfig.arrayDesignReportTrigger}
 * regenerates all of them monthly — but that trigger is a Quartz bean gated on the
 * {@code scheduler} profile, and production nodes run {@code production} without it, so on a
 * production deployment the reports are never written at all. This command fills that gap.
 * <p>
 * With no platform arguments it processes every platform, which is the usual way to run it.
 *
 * @author paul
 */
public class ArrayDesignReportCli extends ArrayDesignSequenceManipulatingCli {

    @Autowired
    private ArrayDesignReportService arrayDesignReportService;

    @Override
    public String getCommandName() {
        return "updatePlatformReports";
    }

    @Override
    public String getShortDesc() {
        return "Regenerate the cached per-platform element/sequence/alignment/gene counts used by the platform pages";
    }

    @Override
    protected void processArrayDesigns( Collection<ArrayDesign> arrayDesigns ) {
        int done = 0;
        for ( ArrayDesign ad : arrayDesigns ) {
            try {
                arrayDesignReportService.generateArrayDesignReport( ad.getId() );
                done++;
                addSuccessObject( ad );
            } catch ( Exception e ) {
                // One unreadable platform must not abandon the rest of the run; the failures are
                // reported in the summary at the end.
                addErrorObject( ad, e );
            }
        }
        log.info( String.format( "Regenerated %d of %d platform report(s).", done, arrayDesigns.size() ) );
        // The grand-total summary is a separate file and is what the "all platforms" figures read.
        arrayDesignReportService.generateAllArrayDesignReport();
    }
}
