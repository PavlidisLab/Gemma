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

Gemma.ExpressionExperimentDetails = Ext.extend(Ext.Panel, {

initComponent: function(){
	

		Gemma.ExpressionExperimentDetails.superclass.initComponent.call(this);

		// if no permissions hasWritePermission is no set.
		if ((Ext.get("hasWritePermission")) && Ext.get("hasWritePermission").getValue() == 'true') {
			this.editable = true;
		}
		
		/*
		 * Load the EE information via an ajax call.
		 */
		this.build(this.experimentDetails);
},

	getPubMedHtml : function(e) {
		var pubmedUrl = e.primaryCitation
				+ '&nbsp; <a target="_blank" ext:qtip="Go to PubMed (in new window)"'
				+ ' href="http://www.ncbi.nlm.nih.gov/entrez/query.fcgi?db=pubmed&cmd=Retrieve&dopt=AbstractPlus&list_uids='
				+ e.pubmedId
				+ '&query_hl=2&itool=pubmed_docsum"><img src="/Gemma/images/pubmed.gif" ealt="PubMed" /></a>&nbsp;&nbsp';

		if (this.editable) {
			// Add the 'delete' button.
			pubmedUrl = pubmedUrl
					+ '<span style="cursor:pointer" onClick="Ext.getCmp(\'ee-details-panel\').removePubMed()">'
					+ '<img src="/Gemma/images/icons/cross.png"  ext:qtip="Remove publication"  /></a>&nbsp;';
		}

		var pubmedRegion = {
			id : 'pubmed-region',
			xtype : 'panel',
			baseCls : 'x-plain-panel',
			html : pubmedUrl,
			width : 380
		};
		return pubmedRegion;
	},

	getPubMedForm : function(e) {
		var pubmedRegion = new Ext.Panel({
			baseCls : 'x-plain-panel',
			disabledClass : 'disabled-plain',
			//id : 'pubmed-region',
			width : 150,
			layout : 'table',
			layoutConfig : {
				columns : 2
			},
			defaults : {
				disabled : !this.editable,
				disabledClass : 'disabled-plain',
				fieldClass : 'x-bare-field'
			},
			items : [{
						xtype : 'numberfield',
						allowDecimals : false,
						minLength : 7,
						maxLength : 9,
						allowNegative : false,
						emptyText : this.isAdmin || this.isUser ? 'Enter pubmed id' : 'Not Available',
						width : 100,
						id : 'pubmed-id-field',
						enableKeyEvents : true,
						listeners : {
							'keyup' : {
								fn : function(e) {
									if (Ext.getCmp('pubmed-id-field').isDirty()
											&& Ext.getCmp('pubmed-id-field').isValid()) {
										// show save
										// button
										this.saveButton.show();
										Ext.getCmp('update-pubmed-region').show(true);
									} else {
										Ext.getCmp('update-pubmed-region').hide(true);
									}
								},
								scope : this
							}
						}
					}, {
						baseCls : 'x-plain-panel',
						id : 'update-pubmed-region',
						html : '<span style="cursor:pointer" onClick="Ext.getCmp(\'ee-details-panel\').savePubMed('
								+ e.id
								+ ',[\'shortname\',\'name\',\'description\'])" ><img src="/Gemma/images/icons/database_save.png" title="Click to save changes" alt="Click to save changes"/></span>',
						hidden : true
					}

			]
		});
		return pubmedRegion;
	},

	renderArrayDesigns : function(arrayDesigns) {
		var result = '';
		for (var i = 0; i < arrayDesigns.length; i++) {
			var ad = arrayDesigns[i];
			result = result + '<a href="/Gemma/arrays/showArrayDesign.html?id=' + ad.id + '">' + ad.shortName
					+ '</a> - ' + ad.name;
			if (i < arrayDesigns.length - 1) {
				result = result + "<br/>";
			}
		}
		return result;
	},
	renderCoExpressionLinkCount : function(ee) {

		if (ee.coexpressionLinkCount === null) {
			return "Unavailable"; // analysis not run.
		}

		var downloadCoExpressionDataLink = String
				.format(
						"<span style='cursor:pointer'  ext:qtip='Download all coexpression  data in a tab delimited format'  onClick='fetchCoExpressionData({0})' > &nbsp; <img src='/Gemma/images/download.gif'/> &nbsp; </span>",
						ee.id);
		var count;

		return ee.coexpressionLinkCount + "&nbsp;" + downloadCoExpressionDataLink;

	},

	/**
	 * 
	 */
	visualizeDiffExpressionHandler : function(eeid, diffResultId, factorDetails) {

		var params = {};
		this.visDiffWindow = new Gemma.VisualizationWithThumbsWindow({
					thumbnails : false,
					readMethod : DEDVController.getDEDVForDiffExVisualizationByThreshold,
					title : "Top diff. ex. probes for " + factorDetails,
					showLegend : false,
					downloadLink : String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&rs={1}&thresh={2}&diffex=1",
							eeid, diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD)
				});
		this.visDiffWindow.show({
					params : [eeid, diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD]
				});

	},

	/**
	 * 
	 */
	visualizePcaHandler : function(eeid, component, count) {

		var params = {};
		this.vispcaWindow = new Gemma.VisualizationWithThumbsWindow({
					thumbnails : false,
					readMethod : DEDVController.getDEDVForPcaVisualization,
					title : "Top loaded probes for PC" + component,
					showLegend : false,
					downloadLink : String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&component={1}&thresh={2}&pca=1",
							eeid, component, count)
				});
		this.vispcaWindow.show({
					params : [eeid, component, count]
				});

	},

	/**
	 * 
	 */
	renderSourceDatabaseEntry : function(ee) {
		var result = '';

		var logo = '';
		if (ee.externalDatabase == 'GEO') {
			var acc = ee.accession;
			acc = acc.replace(/\.[1-9]$/, ''); // in case of multi-species.
			logo = '/Gemma/images/logo/geoTiny.png';
			result = '<a target="_blank" href="http://www.ncbi.nlm.nih.gov/geo/query/acc.cgi?acc=' + acc
					+ '"><img src="' + logo + '"/></a>';

		} else if (ee.externalDatabase == 'ArrayExpress') {
			logo = '/Gemma/images/logo/arrayExpressTiny.png';
			result = '<a target="_blank" href="http://www.ebi.ac.uk/microarray-as/aer/result?queryFor=Experiment&eAccession='
					+ ee.accession + '"><img src="' + logo + '"/></a>';
		} else {
			result = "Direct upload";
		}

		return result;

	},

	/**
	 * Link for samples details page.
	 * 
	 * @param {}
	 *            ee
	 * @return {}
	 */
	renderSamples : function(ee) {
		var result = ee.bioAssayCount;
		if (this.editable) {
			result = result
					+ '&nbsp;&nbsp<a href="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id='
					+ ee.id
					+ '"><img ext:qtip="View the details of the samples" src="/Gemma/images/icons/magnifier.png"/></a>';
		}
		return '' + result; // hack for possible problem with extjs 3.1 - bare
		// number not displayed, coerce to string.
	},

	renderStatus : function(ee) {
		var result = '';
		if (ee.validatedFlag) {
			result = result + '<img src="/Gemma/images/icons/emoticon_smile.png" alt="validated" title="validated"/>';
		}

		if (ee.troubleFlag) {
			result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" title="trouble"/>';
		}

		if (ee.hasMultiplePreferredQuantitationTypes) {
			result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" '
					+ 'title="This experiment has multiple \'preferred\' quantitation types. '
					+ 'This isn\'t necessarily a problem but is suspicious."/>';
		}

		if (ee.hasMultipleTechnologyTypes) {
			result = result + '<img src="/Gemma/images/icons/stop.png" alt="trouble" '
					+ 'title="This experiment seems to mix array designs with different technology types."/>';
		}

		if (this.editable) {
			result = result
					+ Gemma.SecurityManager.getSecurityLink(
							'ubic.gemma.model.expression.experiment.ExpressionExperimentImpl', ee.id, ee.isPublic,
							ee.isShared, this.editable);
		}

		return result || "No flags";

	},

	linkAnalysisRenderer : function(ee) {
		var id = ee.id;
		var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doLinks('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="link analysis" title="link analysis"/></span>';
		if (ee.dateLinkAnalysis) {
			var type = ee.linkAnalysisEventType;
			var color = "#000";
			var suggestRun = true;
			var qtip = 'ext:qtip="OK"';
			if (type == 'FailedLinkAnalysisEventImpl') {
				color = 'red';
				qtip = 'ext:qtip="Failed"';
			} else if (type == 'TooSmallDatasetLinkAnalysisEventImpl') {
				color = '#CCC';
				qtip = 'ext:qtip="Too small"';
				suggestRun = false;
			}

			return '<span style="color:' + color + ';" ' + qtip + '>'
					+ Ext.util.Format.date(ee.dateLinkAnalysis, 'y/M/d') + '&nbsp;' + (suggestRun ? runurl : '');
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
		}

	},

	missingValueAnalysisRenderer : function(ee) {
		var id = ee.id;
		var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doMissingValues('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="missing value computation" title="missing value computation"/></span>';

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

				return '<span style="color:' + color + ';" ' + qtip + '>'
						+ Ext.util.Format.date(ee.dateMissingValueAnalysis, 'y/M/d') + '&nbsp;'
						+ (suggestRun ? runurl : '');
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
			}

		} else {
			return '<span ext:qtip="Only relevant for two-channel microarray studies with intensity data available." style="color:#CCF;">NA</span>';
		}
	},

	processedVectorCreateRenderer : function(ee) {
		var id = ee.id;
		var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doProcessedVectors('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="processed vector computation" title="processed vector computation"/></span>';

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

			return '<span style="color:' + color + ';" ' + qtip + '>'
					+ Ext.util.Format.date(ee.dateProcessedDataVectorComputation, 'y/M/d') + '&nbsp;'
					+ (suggestRun ? runurl : '');
		} else {
			return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
		}
	},

	differentialAnalysisRenderer : function(ee) {
		var id = ee.id;
		var runurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').doDifferential('
				+ id
				+ ')"><img src="/Gemma/images/icons/control_play_blue.png" alt="differential expression analysis" title="differential expression analysis"/></span>';

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

				return '<span style="color:' + color + ';" ' + qtip + '>'
						+ Ext.util.Format.date(ee.dateDifferentialAnalysis, 'y/M/d') + '&nbsp;'
						+ (suggestRun ? runurl : '');
			} else {
				return '<span style="color:#3A3;">Needed</span>&nbsp;' + runurl;
			}
		} else {
			return '<span style="color:#CCF;">NA</span>';
		}
	},
	renderProcessedExpressionVectorCount : function(e) {
		return e.processedExpressionVectorCount ? e.processedExpressionVectorCount : ' [count not available] ';
	},

	build : function(experimentDetails) {

		var e = experimentDetails;


		adminLinks = '<span style="cursor:pointer" onClick="Ext.getCmp(\'eemanager\').updateEEReport('
				+ e.id
				+ ',\'admin-links\''
				+ ')"><img src="/Gemma/images/icons/arrow_refresh_small.png" ext:qtip="Refresh statistics"  title="refresh"/></span>'
				+ '&nbsp;<a href="/Gemma/expressionExperiment/editExpressionExperiment.html?id='
				+ e.id
				+ '"  ><img src="/Gemma/images/icons/wrench.png" ext:qtip="Go to editor page for this experiment" title="edit"/></span>&nbsp;';

		if (this.isAdmin) {
			adminLinks = adminLinks
					+ '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').deleteExperiment('
					+ e.id
					+ ')"><img src="/Gemma/images/icons/cross.png" ext:qtip="Delete the experiment from the system" title="delete" /></span>&nbsp;';
		}

		var pubmedRegion = {};

		if (e.pubmedId) {
			// display the citation, with link out and delete
			// button.
			pubmedRegion = this.getPubMedHtml(e);
		} else {
			// offer to create a citation link.
			pubmedRegion = this.getPubMedForm(e);
		}

		/*
		 * This is needed to make the annotator initialize properly.
		 */
		new Gemma.MGEDCombo({});

		var taggerurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').tagger(' + e.id + ','
				+ e.taxonId + ',' + this.editable + ',' + (e.validatedAnnotations !== null)
				+ ')"><img src="/Gemma/images/icons/magnifier.png" alt="view tags" title="view tags"/></span>';

		tagView = new Gemma.AnnotationDataView({
					readParams : [{
								id : e.id,
								classDelegatingFor : "ExpressionExperimentImpl"
							}]
				});
				
		var descriptionPanel = new Ext.Panel({
			//fieldLabel: 'Description',
			height : 120,
			border:true,
			autoScroll:true,
			html: e.description,
			style:'padding-bottom:15px;padding-top:5px'
		});


		var basics = new Ext.Panel({
			layout: 'form',
			collapsible: false,
			bodyBorder: false,
			frame: false,
			baseCls: 'x-plain-panel',
			bodyStyle: 'padding:10px',
			defaults: {
				bodyStyle: 'vertical-align:top;font-size:12px;color:black',
				baseCls: 'x-plain-panel',
				fieldClass: 'x-bare-field'
			},
			items: [{html: '<h4><b>'+e.shortName+'</h4><h5> '+e.name+'</h5>'}, {
				fieldLabel: "Taxon",
				html: e.taxon
			}, {
				fieldLabel: 'Tags&nbsp;' + taggerurl,
				items: [tagView]
			}, {
				fieldLabel: 'Samples',
				html: this.renderSamples(e),
				width: 60
			}, {
				fieldLabel: 'Profiles',
				//id: 'processedExpressionVectorCount-region',
				html: '<div id="downloads"> ' +
				this.renderProcessedExpressionVectorCount(e) +
				'&nbsp;&nbsp;' +
				'<i>Downloads:</i> &nbsp;&nbsp; <span class="link"  ext:qtip="Download the tab delimited data" onClick="fetchData(true,' +
				e.id +
				', \'text\', null, null)">Filtered</span> &nbsp;&nbsp;' +
				'<span class="link" ext:qtip="Download the tab delimited data" onClick="fetchData(false,' +
				e.id +
				', \'text\', null, null)">Unfiltered</span> &nbsp;&nbsp;' +
				'<a class="helpLink" href="?" onclick="showHelpTip(event, \'Tab-delimited data file for this experiment. The filtered version corresponds to what is used in most Gemma analyses, removing some probes. Unfiltered includes all probes\'); return false"> <img src="/Gemma/images/help.png" /> </a>' +
				'</div>',
				width: 400
			}, {
				fieldLabel: 'Array designs',
				//id: 'arrayDesign-region',
				html: this.renderArrayDesigns(e.arrayDesigns),
				width: 480
			},{
				fieldLabel: 'Coexpr. Links',
				//id: 'coexpressionLinkCount-region',
				html: this.renderCoExpressionLinkCount(e),
				width: 80
			}, {
				fieldLabel: 'Differential Expr. Analyses',
				//id: 'DiffExpressedProbes-region',
				items: new Gemma.DifferentialExpressionAnalysesSummaryTree(e)
			}, {
				fieldLabel: 'Status',
				html: this.renderStatus(e)
			}, descriptionPanel, {
				fieldLabel: 'Publication',
				xtype: 'panel',
				//id: 'pubmed-region-wrap',
				layout: 'fit',
				bodyBorder: false,
				baseCls: 'x-plain-panel',
				disabled: false,
				items: [pubmedRegion]
			}, 
			{
				fieldLabel: 'Created',
				html: Ext.util.Format.date(e.dateCreated) + ' from ' + this.renderSourceDatabaseEntry(e)
			}, {
				html: 'The last time an array design associated with this experiment was updated: '+e.lastArrayDesignUpdateDate,
				hidden: !e.lastArrayDesignUpdateDate
			}]
		});
	
		this.add(basics);
		
		this.doLayout();
		this.fireEvent("ready");
	
	}

});

