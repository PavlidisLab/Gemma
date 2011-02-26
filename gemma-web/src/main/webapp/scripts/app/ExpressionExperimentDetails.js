Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * This is not a visual component but we want to use it with the
 * componentmanager.
 * 
 * @class Gemma.EEPanel
 * @extends Ext.Component
 */
Gemma.EEPanel = Ext.extend(Ext.Component, {

	constructor : function(id) {

		this.eeId = id;

		this.id = 'ee-details-panel';

		Gemma.EEPanel.superclass.constructor.call(this);

		this.addEvents({
					"ready" : true
				});
		// if no permissions hasWritePermission is no set.

		if ((Ext.get("hasWritePermission")) && Ext.get("hasWritePermission").getValue() == 'true') {
			this.editable = 'true';
		}

		/*
		 * Load the EE information via an ajax call.
		 */
		ExpressionExperimentController.loadExpressionExperimentDetails(id, this.build.createDelegate(this));

	},

	save : function() {
		var snField = Ext.getCmp('shortname');
		var dField = Ext.getCmp('description');
		var nField = Ext.getCmp('name');
		var shortName = snField.getValue();
		var description = dField.getValue();
		var name = nField.getValue();

		var entity = {
			entityId : this.eeId
		};

		if (shortName != snField.originalValue) {
			entity.shortName = shortName;
		}

		if (description != dField.originalValue) {
			entity.description = description;
		}

		if (name != nField.originalValue) {
			entity.name = name;
		}

		ExpressionExperimentController.updateBasics(entity, function(data) {

					Ext.getCmp('update-button-region').hide();

					var k = Ext.getCmp('shortname');
					k.setValue(data.shortName);

					k = Ext.getCmp('name');
					k.setValue(data.name);

					k = Ext.getCmp('description');
					k.setValue(data.description);

				}.createDelegate(this));
	},

	savePubMed : function() {
		var pubmedId = Ext.getCmp('pubmed-id-field').getValue();
		ExpressionExperimentController.updatePubMed(this.eeId, pubmedId, {
					callback : function(data) {
						var k = new Gemma.WaitHandler();
						k.on('done', function(e) {
									// var html = this.getPubMedHtml(e);
									// Ext.getCmp('pubmed-region-wrap').remove(Ext.getCmp('pubmed-region'));
									// Ext.DomHelper.append('pubmed-region-wrap',
									// html);
									window.location.reload();
								}, this)
						k.handleWait(data, false);
					}.createDelegate(this)
				});

	},

	removePubMed : function() {
		Ext.Msg.show({
					title : 'Really delete?',
					msg : 'Are you sure you want to delete the reference? This cannot be undone.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							ExpressionExperimentController.removePrimaryPublication(this.eeId, {
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.on('done', function(success) {

														if (success) {
															var r = Ext.getCmp('pubmed-region-wrap');
															r.remove(Ext.getCmp('pubmed-region'));
															var form = this.getPubMedForm(this.eeId);
															r.add(form);
															r.doLayout();
														}

													}, this);
											k.handleWait(data, false);
										}.createDelegate(this)
									})
						}
					},
					scope : this
				});
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
			id : 'pubmed-region',
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

		if (ee.coexpressionLinkCount == null) {
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

		var params = {}
		this.visDiffWindow = new Gemma.VisualizationWithThumbsWindow({
					thumbnails : false,
					readMethod : DEDVController.getDEDVForDiffExVisualizationByThreshold,
					title : "Top diff. ex. probes for " + factorDetails,
					downloadLink : String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&rs={1}&thresh={2}", eeid,
							diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD)
				});
		this.visDiffWindow.show({
					params : [eeid, diffResultId, Gemma.DIFFEXVIS_QVALUE_THRESHOLD]
				});

	},

	/**
	 * 
	 */
	visualizePcaHandler : function(eeid, component, count) {

		var params = {}
		this.vispcaWindow = new Gemma.VisualizationWithThumbsWindow({
					thumbnails : false,
					readMethod : DEDVController.getDEDVForPcaVisualization,
					title : "Top loaded probes for PC" + component,
					downloadLink : String.format("/Gemma/dedv/downloadDEDV.html?ee={0}&rs={1}&thresh={2}", eeid,
							component, count)
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

	build : function(e) {

		var manager = new Gemma.EEManager({
					editable : this.editable,
					id : "eemanager"
				});
		this.manager = manager;

		//
		// /*
		// * Create a store with one record.
		// */
		// var store = new Ext.data.Store({
		// proxy : new Ext.data.MemoryProxy([e]),
		// reader : new Ext.data.ListRangeReader({},
		// this.manager.record)
		// });
		//
		// this.rec = store.getAt(0);

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
		 * Show the experimental design
		 */
		DesignMatrix.init({
					id : e.id
				});

		var vizPanel = new Gemma.EEDetailsVisualizationWidget({
					renderTo : 'visualization',
					taxon : {
						commonName : e.parentTaxon,
						id : e.parentTaxonId
					}
				});

		/*
		 * This is needed to make the annotator initialize properly.
		 */
		new Gemma.MGEDCombo({});

		var taggerurl = '<span style="cursor:pointer" onClick="return Ext.getCmp(\'eemanager\').tagger(' + e.id + ','
				+ e.taxonId + ',' + this.editable + ',' + (e.validatedAnnotations !== null)
				+ ')"><img src="/Gemma/images/icons/pencil.png" alt="view tags" title="view tags"/></span>';

		tagView = new Gemma.AnnotationDataView({
					readParams : [{
								id : e.id,
								classDelegatingFor : "ExpressionExperimentImpl"
							}]
				});

		manager.on('tagsUpdated', function() {
					tagView.store.reload();
				});

		manager.on('done', function() {
					/*
					 * After a process that requires refreshing the page.
					 */
					window.location.reload();
				});

		manager.on('reportUpdated', function(data) {
					ob = data[0];
					var k = Ext.get('coexpressionLinkCount-region');
					Ext.DomHelper.overwrite(k, {
								html : ob.coexpressionLinkCount
							});
					k.highlight();
					k = Ext.get('processedExpressionVectorCount-region');
					Ext.DomHelper.overwrite(k, {
								html : ob.processedExpressionVectorCount
							});
					k.highlight();
				});

		// manager.on('pubmedUpdated', function(e) {
		// var html = this.getPubMedHtml(e);
		// Ext.getCmp('pubmed-region-wrap').remove(Ext.getCmp('pubmed-region'));
		// Ext.DomHelper.append('pubmed-region-wrap', html);
		// }.createDelegate(this));
		//
		// manager.on('pubmedRemove', function(success) {
		// if (success) {
		// var r = Ext.getCmp('pubmed-region-wrap');
		// r.remove(Ext.getCmp('pubmed-region'));
		// var form = this.getPubMedForm(this.eeId);
		// r.add(form);
		// r.doLayout();
		// }
		// }.createDelegate(this));

		// manager.on('reportUpdated', function() {
		// // window.location.reload(true);
		// });

		manager.on('differential', function() {
					window.location.reload(true);
				});
		// manager.on('processedVector', function() {
		// window.location.reload(true);
		// });
		// manager.on('link', function() {
		// window.location.reload(true);
		// });
		//
		// manager.on('missingValue', function() {
		// window.location.reload(true);
		// });

		var descriptionArea = new Ext.form.TextArea({
					id : 'description',
					allowBlank : true,
					grow : true,
					growMax : 300,
					readOnly : !this.editable,
					disabledClass : 'disabled-plain',
					growMin : 40,
					emptyText : 'No description provided',
					enableKeyEvents : true,
					listeners : {
						'keyup' : {
							fn : function(e) {
								if (Ext.getCmp('description').isDirty() && Ext.getCmp('description').isValid()) {
									// show save button
									Ext.getCmp('update-button-region').show(true);
								} else {
									Ext.getCmp('update-button-region').hide(true);
								}
							}
						}
					},
					width : 500,
					// height : 100,
					value : e.description
				});

		var nameArea = new Ext.form.TextArea({
					id : 'name',
					fieldLabel : 'Name',
					allowBlank : false,
					grow : true,
					growMax : 300,
					readOnly : !this.editable,
					disabledClass : 'disabled-plain',
					growMin : 40,
					emptyText : 'No description provided',
					enableKeyEvents : true,
					listeners : {
						'keyup' : {
							fn : function(e) {
								if (Ext.getCmp('name').isDirty() && Ext.getCmp('name').isValid()) {
									// show save button
									Ext.getCmp('update-button-region').show(true);
								} else {
									Ext.getCmp('update-button-region').hide(true);
								}
							}
						}
					},
					width : 500,
					value : e.name
				});

		var basics = new Ext.Panel({
			autoHeight : true,
			layout : 'table',

			layoutConfig : {
				columns : 2
			},

			renderTo : 'basics',
			collapsible : false,
			bodyBorder : false,
			frame : false,
			baseCls : 'x-plain-panel',
			bodyStyle : 'padding:10px',
			defaults : {
				bodyStyle : 'vertical-align:top;font-size:12px;color:black',
				baseCls : 'x-plain-panel',
				fieldClass : 'x-bare-field'

			},
			items : [{
						html : 'Short name:'
					}, {
						xtype : 'panel',
						layout : 'table',
						baseCls : 'x-plain-panel',
						layoutConfig : {
							columns : 2
						},
						items : [{
									xtype : 'textfield',
									id : 'shortname',
									enableKeyEvents : true,
									allowBlank : false,
									disabledClass : 'disabled-plain',
									fieldClass : 'x-bare-field',
									readOnly : !this.editable,
									listeners : {
										'keyup' : {
											fn : function(e) {
												if (Ext.getCmp('shortname').isDirty()
														&& Ext.getCmp('shortname').isValid()) {
													// show
													// save
													// button
													Ext.getCmp('update-button-region').show(true);
												} else {
													Ext.getCmp('update-button-region').hide(true);
												}
											},
											scope : this
										}
									},
									width : 250,
									value : e.shortName
								}, {
									baseCls : 'x-plain-panel',
									id : 'update-button-region',
									html : '<span style="cursor:pointer" onClick="Ext.getCmp(\'ee-details-panel\').save('
											+ e.id
											+ ',[\'shortname\',\'name\',\'description\'])" ><img src="/Gemma/images/icons/database_save.png" ext:qtip="Click to save changes" alt="Click to save changes"/></span>',
									hidden : true
								}]
					}, {
						html : 'Name:'
					}, nameArea, {
						html : "Taxon:"
					}, {
						html : e.taxon
					}, {
						html : 'Description:'
					}, descriptionArea, {
						html : 'Created:'
					}, {
						html : Ext.util.Format.date(e.dateCreated)
					}, {
						html : 'Source:'
					}, {
						html : this.renderSourceDatabaseEntry(e)
					}, {
						html : 'Samples:'
					}, {
						html : this.renderSamples(e),
						width : 60
					}, {
						html : 'Profiles:'
					}, {
						id : 'processedExpressionVectorCount-region',
						html : '<div id="downloads"> '
								+ this.renderProcessedExpressionVectorCount(e)
								+ '&nbsp;&nbsp;'
								+ '<i>Downloads:</i> &nbsp;&nbsp; <span class="link"  ext:qtip="Download the tab delimited data" onClick="fetchData(true,'
								+ e.id
								+ ', \'text\', null, null)">Filtered</span> &nbsp;&nbsp;'
								+ '<span class="link" ext:qtip="Download the tab delimited data" onClick="fetchData(false,'
								+ e.id
								+ ', \'text\', null, null)">Unfiltered</span> &nbsp;&nbsp;'
								+ '<a class="helpLink" href="?" onclick="showHelpTip(event, \'Tab-delimited data file for this experiment. The filtered version corresponds to what is used in most Gemma analyses, removing some probes. Unfiltered includes all probes\'); return false"> <img src="/Gemma/images/help.png" /> </a>'
								+ '</div>',
						width : 400
					}, {
						html : 'Array designs:'
					}, {
						id : 'arrayDesign-region',
						html : this.renderArrayDesigns(e.arrayDesigns),
						width : 480
					}, {
						html : 'Coexpr. Links:'
					}, {
						id : 'coexpressionLinkCount-region',
						html : this.renderCoExpressionLinkCount(e),
						width : 80
					}, {
						html : 'Differential Expr.<br>Analyses'
					}, {
						id : 'DiffExpressedProbes-region',
						items : new Gemma.DifferentialExpressionAnalysesSummaryTree(e)
					}, {
						html : 'Publication:'
					}, {
						xtype : 'panel',
						id : 'pubmed-region-wrap',
						layout : 'fit',
						bodyBorder : false,
						baseCls : 'x-plain-panel',
						disabled : false,
						items : [pubmedRegion]
					}, {
						html : 'Tags&nbsp;' + taggerurl
					}, tagView, {
						html : 'Status'
					}, {
						html : this.renderStatus(e)
					}, {
						html : this.editable ? 'Admin' : ''
					}, {
						id : 'admin-links',
						html : this.editable ? adminLinks : ''
					}

			/*
			 * authors
			 */
			]
		});

		if (this.editable) {
			Ext.DomHelper.append('admin-links', {
						tag : 'ul',
						cls : 'plainlist',
						children : [{
									tag : 'li',
									html : 'Missing values: ' + this.missingValueAnalysisRenderer(e)
								}, {
									tag : 'li',
									html : 'Proc. vec:  ' + this.processedVectorCreateRenderer(e)
								}, {
									tag : 'li',
									html : 'Diff ex:  ' + this.differentialAnalysisRenderer(e)
								}, {
									tag : 'li',
									html : 'Link an.:  ' + this.linkAnalysisRenderer(e)
								}]
					});
		}

		if (Ext.get('history')) {
			var history = new Gemma.AuditTrailGrid({
						renderTo : 'history',
						bodyBorder : false,
						collapsible : true,
						auditable : {
							id : e.id,
							classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
						}
					});
		}

		this.fireEvent("ready");

	}

});

Ext.onReady(function() {
			Ext.QuickTips.init();

			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			var eePanel = new Gemma.EEPanel(Ext.get("eeId").getValue());

			eePanel.on("ready", function(panel) {
				setTimeout(function() {
							Ext.get('loading').remove();
							Ext.get('loading-mask').fadeOut({
										remove : true
									});
						}, 250);
					// new
					// Gemma.DifferentialExpressionAnalysesSummaryTree(Ext.get("eeId").getValue());
				});

		});

/**
 * Used to make the correlation heatmap clickable. See ExperimentQCTag.java
 * 
 * @param {Object}
 *            bigImageUrl
 */
var popupImage = function(url) {
	url = url + "&nocache=" + Math.floor(Math.random() * 1000);
	var b = new Ext.Window({
				modal : true,
				stateful : false,
				resizable : true,
				autoHeight : false,
				width : 200,
				x : 50,
				padding : 10,
				html : '<img src=\"' + url + '"\" />'
			});
	b.show();
}
