Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
/**
 *
 * Panel containing the most interesting info about an experiment.
 * Used as one tab of the EE page
 *
 * pass in the ee details obj as experimentDetails
 *
 * @class Gemma.ExpressionExperimentDetails
 * @extends Ext.Panel
 *
 */
Gemma.ExpressionExperimentTools = Ext.extend(Ext.Panel, {
    experimentDetails: null,
    border: false,
    tbar: new Ext.Toolbar,
    
    initComponent: function(){
        Gemma.ExpressionExperimentTools.superclass.initComponent.call(this);
        var manager = new Gemma.EEManager({
            editable: this.editable,
        });
        var refreshButton = new Ext.Button({
            text: 'Refresh',
            icon: '/Gemma/images/icons/arrow_refresh_small.png',
            tootltip: 'Refresh statistics',
            handler: function(){
                manager.updateEEReport(this.experimentDetails.id);
            },
            scope: this
        
        });
        
        this.getTopToolbar().addButton(refreshButton);
        this.add({
            border: false,
            padding: 10,
            html: '<h4>Analyses:<br></h4>' +
            'Missing values: ' +
            this.missingValueAnalysisRenderer(this.experimentDetails) +
            '<br>Proccessed Vector Computation:  ' +
            this.processedVectorCreateRenderer(this.experimentDetails) +
            '<br>Differential Expression Analysis:  ' +
            this.differentialAnalysisRenderer(this.experimentDetails) +
            '<br>Link Analysis:  ' +
            this.linkAnalysisRenderer(this.experimentDetails)
        });
        
    },
    
    linkAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doLinks(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></span>';
        if (ee.dateLinkAnalysis) {
            var type = ee.linkAnalysisEventType;
            var color = "#000";
            var suggestRun = true;
            var qtip = 'ext:qtip="Analysis was OK"';
            if (type == 'FailedLinkAnalysisEventImpl') {
                color = 'red';
                qtip = 'ext:qtip="Analysis failed"';
            }
            else 
                if (type == 'TooSmallDatasetLinkAnalysisEventImpl') {
                    color = '#CCC';
                    qtip = 'ext:qtip="Analysis was too small"';
                    suggestRun = false;
                }
            
            return '<span style="color:' + color + ';" ' + qtip + '>' +
            Ext.util.Format.date(ee.dateLinkAnalysis, 'y/M/d') +
            '&nbsp;' +
            (suggestRun ? runurl : '');
        }
        else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
        
    },
    missingValueAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doMissingValues(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></span>';
        
        /*
         * Offer missing value analysis if it's possible (this might need
         * tweaking).
         */
        if (ee.technologyType != 'ONECOLOR' && ee.hasEitherIntensity) {
        
            if (ee.dateMissingValueAnalysis) {
                var type = ee.missingValueAnalysisEventType;
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedMissingValueAnalysisEventImpl') {
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                
                return '<span style="color:' + color + ';" ' + qtip + '>' +
                Ext.util.Format.date(ee.dateMissingValueAnalysis, 'y/M/d') +
                '&nbsp;' +
                (suggestRun ? runurl : '');
            }
            else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
            
        }
        else {
            return '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>';
        }
    },
    
    processedVectorCreateRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doProcessedVectors(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="processed vector computation" title="processed vector computation"/></span>';
        
        if (ee.dateProcessedDataVectorComputation) {
            var type = ee.processedDataVectorComputationEventType;
            var color = "#000";
            
            var suggestRun = true;
            var qtip = 'ext:qtip="OK"';
            if (type == 'FailedProcessedVectorComputationEventImpl') { // note:
                // no
                // such
                // thing.
                color = 'red';
                qtip = 'ext:qtip="Failed"';
            }
            
            return '<span style="color:' + color + ';" ' + qtip + '>' +
            Ext.util.Format.date(ee.dateProcessedDataVectorComputation, 'y/M/d') +
            '&nbsp;' +
            (suggestRun ? runurl : '');
        }
        else {
            return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
        }
    },
    
    differentialAnalysisRenderer: function(ee){
        var id = ee.id;
        var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doDifferential(' +
        id +
        ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" title="differential expression analysis"/></span>';
        
        if (ee.numPopulatedFactors > 0) {
            if (ee.dateDifferentialAnalysis) {
                var type = ee.differentialAnalysisEventType;
                
                var color = "#000";
                var suggestRun = true;
                var qtip = 'ext:qtip="OK"';
                if (type == 'FailedDifferentialExpressionAnalysisEventImpl') { // note:
                    // no
                    // such
                    // thing.
                    color = 'red';
                    qtip = 'ext:qtip="Failed"';
                }
                
                return '<span style="color:' + color + ';" ' + qtip + '>' +
                Ext.util.Format.date(ee.dateDifferentialAnalysis, 'y/M/d') +
                '&nbsp;' +
                (suggestRun ? runurl : '');
            }
            else {
                return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
            }
        }
        else {
            return '<span style="color:#CCF;">NA</span>';
        }
    },
    renderProcessedExpressionVectorCount: function(e){
        return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
    }
});

