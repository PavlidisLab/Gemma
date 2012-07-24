Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';
/**
 *
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
    tbar: new Ext.Toolbar(),
	defaultType:'box',
    defaults:{
		border:false
	},
	padding:10,
    initComponent: function(){
        Gemma.ExpressionExperimentTools.superclass.initComponent.call(this);
        var manager = new Gemma.EEManager({
            editable: this.editable
        });
        manager.on('reportUpdated', function(){
        	this.fireEvent('reloadNeeded');
        },this);
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
            html: '<h4>Analyses:<br></h4>'
        });
		
		
		var missingValueInfo = this.missingValueAnalysisPanelRenderer(this.experimentDetails, manager);
		this.add(missingValueInfo);
		var processedVectorInfo = this.processedVectorCreatePanelRenderer(this.experimentDetails, manager);
		this.add(processedVectorInfo);
		var differentialAnalysisInfo = this.differentialAnalysisPanelRenderer(this.experimentDetails, manager);
		this.add(differentialAnalysisInfo);
		var linkAnalysisInfo = this.linkAnalysisPanelRenderer(this.experimentDetails, manager);
		this.add(linkAnalysisInfo);
        
    },
    
    linkAnalysisPanelRenderer: function(ee, manager){
		var panel = new Ext.Panel({
			layout: 'hbox',
			defaults: {
				border: false,
				padding:2
			},
			items: [{
				html: 'Link Analysis: '
			}]
		});
        var id = ee.id;
		var runBtn = new Ext.Button({
			icon:'/Gemma/images/icons/control_play_blue.png',
			tooltip:'missing value computation',
			handler: manager.doLinks.createDelegate(this,[id]),
			scope:this,
			cls:'transparent-btn'
		});
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
            panel.add({
				html: '<span style="color:' + color + ';" ' + qtip + '>' +
				Ext.util.Format.date(ee.dateLinkAnalysis, 'y/M/d')
			});
			if(suggestRun){
				panel.add(runBtn);
			}
			return panel;
        }
        else {
			panel.add({html:'<span style="color:#3A3;">Needed</span>&nbsp;'});
			panel.add(runBtn);
            return panel;
        }
        
    },
    missingValueAnalysisPanelRenderer: function(ee, manager){
		var panel = new Ext.Panel({
			layout: 'hbox',
			defaults: {
				border: false,
				padding:2
			},
			items: [{
				html: 'Missing values: '
			}]
		});
        var id = ee.id;
		var runBtn = new Ext.Button({
			icon:'/Gemma/images/icons/control_play_blue.png',
			tooltip:'missing value computation',
			handler: manager.doMissingValues.createDelegate(this,[id]),
			scope:this,
			cls:'transparent-btn'
		});
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
                
                //return '<span style="color:' + color + ';" ' + qtip + '>' +
                //Ext.util.Format.date(ee.dateMissingValueAnalysis, 'y/M/d') +
                //'&nbsp;' +
                //(suggestRun ? runurl : '');
				panel.add({ html: '<span style="color:' + color + ';" ' + qtip + '>' +
					Ext.util.Format.date(ee.dateMissingValueAnalysis, 'y/M/d') +
					'&nbsp;'});
					if(suggestRun){
						panel.add(runBtn);
					}
				return panel;
            }
            else {
				panel.add({
					html: '<span style="color:#3A3;">Needed</span>&nbsp;'
				});
				panel.add(runBtn);
                return panel;
            }
            
        }
        else {
			
				panel.add({
					html: '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>'
				});
                return panel;
        }
    },
    
    processedVectorCreatePanelRenderer: function(ee, manager){
        var panel = new Ext.Panel({
			layout: 'hbox',
			defaults: {
				border: false,
				padding:2
			},
			items: [{
				html: 'Proccessed Vector Computation: '
			}]
		});
        var id = ee.id;
		var runBtn = new Ext.Button({
			icon:'/Gemma/images/icons/control_play_blue.png',
			tooltip:'processed vector computation',
			handler: manager.doProcessedVectors.createDelegate(this,[id]),
			scope:this,
			cls:'transparent-btn'
		});
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
            panel.add({html:'<span style="color:' + color + ';" ' + qtip + '>' +
            Ext.util.Format.date(ee.dateProcessedDataVectorComputation, 'y/M/d') +
            '&nbsp;'});
			if(suggestRun){
				panel.add(runBtn);
			}
            return panel;
        }
        else {
			panel.add({html:'<span style="color:#3A3;">Needed</span>&nbsp;'});
			panel.add(runBtn);
            return panel;
        }
    },
    
    differentialAnalysisPanelRenderer: function(ee, manager){
        var panel = new Ext.Panel({
			layout: 'hbox',
			defaults: {
				border: false,
				padding:2
			},
			items: [{
				html: 'Differential Expression Analysis: '
			}]
		});
        var id = ee.id;
		var runBtn = new Ext.Button({
			icon:'/Gemma/images/icons/control_play_blue.png',
			tooltip:'differential expression analysis',
			handler: manager.doDifferential.createDelegate(this,[id]),
			scope:this,
			cls:'transparent-btn'
		});
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
                panel.add({html:'<span style="color:' + color + ';" ' + qtip + '>' +
                Ext.util.Format.date(ee.dateDifferentialAnalysis, 'y/M/d') +
                '&nbsp;'});
				if(suggestRun){
					panel.add(runBtn);
				}
                return panel;
            }
            else {
				
                panel.add({html:'<span style="color:#3A3;">Needed</span>&nbsp;'});
				panel.add(runBtn);
                return panel;
            }
        }
        else {
			
            panel.add({html:'<span style="color:#CCF;">NA</span>'});
            return panel;
        }
    },
    renderProcessedExpressionVectorCount: function(e){
        return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
    }
});



