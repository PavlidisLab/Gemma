/**
 * Common tasks to run on experiments. To use this, construct it with a id defined in the config (e.g., {id :
 * 'eemanager}). Then you can use things like onClick=Ext.getCmp('eemanager').updateEEReport(id).
 * 
 * @class Gemma.EEManager
 * @extends Ext.Component
 */
Gemma.EEManager = Ext.extend(Ext.Component, {

	record : Ext.data.Record.create([{
				name : "id",
				type : "int"
			}, {
				name : "shortName"
			}, {
				name : "name"
			}, {
				name : "arrayDesignCount",
				type : "int"
			}, {
				name : "technologyType"
			}, {
				name : "hasBothIntensities",
				type : 'bool'
			}, {
				name : "hasEitherIntensity",
				type : 'bool'
			}, {
				name : "bioAssayCount",
				type : "int"
			}, {
				name : "processedExpressionVectorCount",
				type : "int"
			}, {
				name : "externalUri"
			}, {
				name : "description"
			}, {
				name : "taxon"
			}, {
				name : "taxonId"
			}, {
				name : "numAnnotations"
			}, {
				name : "numPopulatedFactors"
			}, {
				name : "isPublic",
				type : "boolean"
			}, {
				name : "isShared",
				type : "boolean"
			}, {
				name : "currentUserHasWritePermission"
			}, {
				name : "sourceExperiment"
			}, {
				name : "coexpressionLinkCount"
			}, {
				name : "diffExpressedProbes"
			}, {
				name : "validatedFlag"
			}, {
				name : "troubleFlag",
				type : "object"
			}, {
				name : "missingValueAnalysisEventType"
			}, {
				name : "processedDataVectorComputationEventType"
			}, {
				name : "dateCreated",
				type : 'date'
			}, {
				name : "dateProcessedDataVectorComputation",
				type : 'date'
			}, {
				name : "dateMissingValueAnalysis",
				type : 'date'
			}, {
				name : "dateDifferentialAnalysis",
				type : 'date'
			}, {
				name : "dateLastUpdated",
				type : 'date'
			}, {
				name : "dateLinkAnalysis",
				type : 'date'
			}, {
				name : "linkAnalysisEventType"
			}, {
				name : "processedDataVectorComputationEventType"
			}, {
				name : "missingValueAnalysisEventType"
			}, {
				name : "differentialAnalysisEventType"
			}]),

	/**
	 * 
	 * @param {}
	 *            id
	 * @param {}
	 *            throbberEl optional element to show the throbber. If omitted, a popup progressbar is shown.
	 */
	updateEEReport : function(id, throbberEl) {
		var callParams = [];
		callParams.push(id);
		callParams.push({
					callback : function(data) {
						var k = new Gemma.WaitHandler({
									throbberEl : throbberEl
								});
						this.relayEvents(k, ['done', 'fail']);
						k.handleWait(data, false);
						k.on('done', function(payload) {
									this.fireEvent('reportUpdated', payload);
								});
					}.createDelegate(this),
					errorHandler : function(message, exception) {
						Ext.Msg.alert("There was an error", message);
						Ext.getBody().unmask();
					}
				});

		ExpressionExperimentReportGenerationController.run.apply(this, callParams);
	},

	updateAllEEReports : function() {
		var callParams = [];
		callParams.push({
					callback : function(data) {
						var k = new Gemma.WaitHandler();
						k.handleWait(data, true);
						this.relayEvents(k, ['done', 'fail']);
						k.on('done', function(payload) {
									this.fireEvent('reportUpdated', payload);
								});
					}.createDelegate(this),
					errorHandler : function(message, exception) {
						Ext.Msg.alert("There was an error", message);
						Ext.getBody().unmask();
					}
				});

		ExpressionExperimentReportGenerationController.runAll.apply(this, callParams);
	},

	autoTag : function(id) {
		var callParams = [];
		callParams.push(id);
		callParams.push({
					callback : function(data) {
						var k = new Gemma.WaitHandler();
						this.relayEvents(k, ['done', 'fail']);
						k.handleWait(data, false);
						k.on('done', function(payload) {
									this.fireEvent('tagsUpdated', payload);
								});
					}.createDelegate(this),
					errorHandler : function(message, exception) {
						Ext.Msg.alert("There was an error", message);
						Ext.getBody().unmask();
					}
				});

		AnnotationController.autoTag.apply(this, callParams);

	},

	tagger : function(id, taxonId, canEdit) {
		var annotator = new Ext.Panel({
					id : 'annotator-wrap',
					collapsible : false,
					stateful : false,
					bodyBorder : false,
					layout : 'fit',
					items : [new Gemma.AnnotationGrid({
								id : 'annotator-grid',
								readMethod : ExpressionExperimentController.getAnnotation,
								writeMethod : AnnotationController.createExperimentTag,
								removeMethod : AnnotationController.removeExperimentTag,
								readParams : [{
											id : id
										}],
								editable : canEdit,
								showParent : false,
								mgedTermKey : "experiment",
								taxonId : taxonId,
								entId : id
							})]
				});
		this.change = false;
		Ext.getCmp('annotator-grid').on('refresh', function() {
					this.change = true;
				}.createDelegate(this));

		var w = new Ext.Window({
					modal : false,
					stateful : false,
					title : "Experiment tags",
					layout : 'fit',
					width : 600,
					height : 200,
					items : [annotator],
					buttons : [{
						text : 'Help',
						handler : function() {
							Ext.Msg
									.alert("Help with tagging",
											"Select a 'category' for the term; then enter a term, " +
													"choosing from existing terms if possible. " +
													"Click 'create' to save it. You can also edit existing terms;" +
													" click 'save' to make the change stick, or 'delete' to remove a selected tag.");
						}
					}, {
						text : 'Done',
						handler : function() {

							var r = Ext.getCmp('annotator-grid').getEditedCharacteristics();

							if (r.length > 0) {
								Ext.Msg.confirm("Unsaved changes",
										"There are unsaved changes. Do you want to continue without saving?", function(
												btn, txt) {
											if (btn == 'OK') {
												w.hide();
											}
										});
							} else {
								w.hide();
							}

							if (this.change) {
								/* Update the display of the tags. */
								this.fireEvent('tagsUpdated');
							}
						},
						scope : this
					}]

				});

		w.show();
	},

	deleteExperiment : function(id) {
		Ext.Msg.show({
					title : 'Really delete?',
					msg : 'Are you sure you want to delete the experiment? This cannot be undone.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = [];
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done', 'fail']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('deleted', payload)
													});
										}.createDelegate(this),
										errorHandler : function(error) {
											Ext.Msg.alert("Deletion failed", error);
											Ext.getBody().unmask();
										}.createDelegate(this)
									});
							ExpressionExperimentController.deleteById.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	doLinks : function(id) {
		Ext.Msg.show({
					title : 'Link analysis',
					msg : 'Please confirm. Previous analysis results will be deleted.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = [];
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done', 'fail']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('link', payload);
													});
										}.createDelegate(this),
										errorHandler : function(error) {
											Ext.Msg.alert("Link analysis failed", error);
											Ext.getBody().unmask();
										}.createDelegate(this)
									});
							LinkAnalysisController.run.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	doMissingValues : function(id) {
		Ext.Msg.show({
					title : 'Missing value analysis',
					msg : 'Please confirm. Previous analysis results will be deleted.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = []
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done', 'fail']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('missingValue', payload)
													});
										}.createDelegate(this),
										errorHandler : function(error) {
											Ext.Msg.alert("Missing value analysis failed", error);
											Ext.getBody().unmask();
										}.createDelegate(this)
									});
							TwoChannelMissingValueController.run.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	doDifferential : function(id) {

		var m = this;

		/*
		 * Do an analysis interactively.
		 */
		var customize = function(analysisInfo) {

			var factors = analysisInfo.factors;
			var proposedAnalysis = analysisInfo.type;

			var canDoInteractions = (proposedAnalysis == 'TWIA') || factors.length > 2;

			/*
			 * DifferentialExpressionAnalysisSetupWindow - to be refactored.
			 */
			var deasw = new Ext.Window({
						modal : true,
						stateful : false,
						resizable : false,
						autoHeight : true,
						width : 300,
						plain : true,
						title : "Differential analysis settings",
						items : [{
									xtype : 'form',
									autoHeight : true,
									items : [{
												xtype : 'fieldset',
												title : "Select factor(s) to use",
												autoHeight : true,
												labelWidth : 200,
												id : 'diff-ex-analysis-customize-factors'
											}, {
												xtype : 'fieldset',
												labelWidth : 200,
												autoHeight : true,
												hidden : !canDoInteractions,
												/*
												 * FIXME hide this if we have more than 2 factors -- basically where
												 * we're not going to bother supporting interactions.
												 */
												items : [{
															xtype : 'checkbox',
															id : 'diff-ex-analysis-customize-include-interactions-checkbox',
															fieldLabel : 'Include interactions if possible'
														}]
											}]
								}],
						buttons : [{
							text : 'Proceed',
							id : 'diff-ex-customize-proceed-button',
							disabled : false,
							scope : this,
							handler : function(btn, text) {

								var includeInteractions = Ext
										.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').getValue();

								/*
								 * Get the factors the user checked. See checkbox creation code below.
								 */
								var factorsToUseIds = [];
								if (factors) {
									for (var i = 0; i < factors.length; i++) {
										var f = factors[i];
										if (!f.name) {
											continue;
										}
										var checked = Ext.getCmp(f.id + '-factor-checkbox').getValue();
										if (checked) {
											factorsToUseIds.push(f.id);
										}
									}
								}

								if (factorsToUseIds.length < 1) {
									Ext.Msg.alert("Invalid selection", "Please pick at least one factor.");
									return;
								}

								/*
								 * Pass back the factors to be used, and the choice of whether interactions are to be
								 * used.
								 */
								var callParams = [];
								callParams.push(id);
								callParams.push(factorsToUseIds);
								callParams.push(includeInteractions);
								Ext.getBody().mask();
								callParams.push({
											callback : function(data) {
												var k = new Gemma.WaitHandler();
												k.handleWait(data, true);
												m.relayEvents(k, ['done', 'fail']);
												Ext.getBody().unmask();
												k.on('done', function(payload) {
															m.fireEvent('differential', payload)
														});
											}.createDelegate(m),
											errorHandler : function(error) {
												Ext.Msg.alert("Differential exp. Analysis failed", error);
												Ext.getBody().unmask();
											}.createDelegate(this)
										});

								DifferentialExpressionAnalysisController.runCustom.apply(this, callParams);
								deasw.close();
							}
						}, {
							text : 'Cancel',
							handler : function() {
								deasw.close();
							}
						}]
					});

			deasw.doLayout();

			/*
			 * Create the checkboxes for user choice of factors.
			 */
			if (factors) {
				var onlyOne = factors.length == 1;
				for (var i = 0; i < factors.length; i++) {
					var f = factors[i];
					if (!f.name) {
						continue;
					}
					Ext.getCmp('diff-ex-analysis-customize-factors').add(new Ext.form.Checkbox({
								fieldLabel : f.name,
								labelWidth : 180,
								id : f.id + '-factor-checkbox',
								tooltip : f.name,
								checked : onlyOne
							}));
				}
			}

			/*
			 * TODO: add radiobutton for subset, if there are more than one factor
			 */

			deasw.doLayout();
			deasw.show();

		};

		/*
		 * Callback for analysis type determination. This gets the type of analysis, if it can be determined. If the
		 * type is non-null, then just ask the user for confirmation. If they say no, or the type is null, show them the
		 * DifferentialExpressionAnalysisSetupWindow.
		 */
		var cb = function(analysisInfo) {
			if (analysisInfo.type) {
				var customizable = false;
				var analysisType = '';
				if (analysisInfo.type === 'TWIA') {
					analysisType = 'Two-way ANOVA with interactions';
					customizable = true;
				} else if (analysisInfo.type === 'TWA') {
					analysisType = 'Two-way ANOVA without interactions';
					customizable = true;
				} else if (analysisInfo.type === 'TTEST') {
					analysisType = 'T-test (two-sample)';
				} else if (analysisInfo.type === 'OSTTEST') {
					analysisType = 'T-test (one-sample)';
				} else if (analysisInfo.type === 'OWA') {
					analysisType = 'One-way ANOVA';
				} else {
					analysisType = 'Generic ANOVA/ANCOVA';
					customizable = true;
				}

				// ask for confirmation.
				var w = new Ext.Window({
							autoCreate : true,
							resizable : false,
							constrain : true,
							constrainHeader : true,
							minimizable : false,
							maximizable : false,
							stateful : false,
							modal : true,
							shim : true,
							buttonAlign : "center",
							width : 400,
							height : 130,
							minHeight : 80,
							plain : true,
							footer : true,
							closable : true,
							title : 'Differential expression analysis',
							html : 'Please confirm. The analysis performed will be a ' + analysisType +
									'. If there is an existing analysis on the same factor(s), it will be deleted.',
							buttons : [{
										text : 'Proceed',
										handler : function(btn, text) {
											var callParams = [];
											callParams.push(id);
											Ext.getBody().mask();
											callParams.push({
														callback : function(data) {
															var k = new Gemma.WaitHandler();
															k.handleWait(data, true);
															this.relayEvents(k, ['done', 'fail']);
															Ext.getBody().unmask();
															k.on('done', function(payload) {
																		this.fireEvent('differential', payload);
																	});
														}.createDelegate(this),
														errorHandler : function(error) {
															Ext.Msg.alert("Diff. Analysis failed", error);
															Ext.getBody().unmask();
														}.createDelegate(this)
													});

											DifferentialExpressionAnalysisController.run.apply(this, callParams);
											w.close();
										}
									}, {
										text : 'Cancel',
										handler : function() {
											w.close();
										}
									}, {
										disabled : !customizable,
										hidden : !customizable,
										text : 'Customize',
										handler : function() {
											w.close();
											customize(analysisInfo);
										}
									}],
							iconCls : Ext.MessageBox.QUESTION
						});

				w.show();

			} else {
				/*
				 * System couldn't guess the analysis type, so force user to customize.
				 */
				customize(analysisInfo);
			}
		};

		/*
		 * Get the analysis type.
		 */
		var eh = function(error) {
			Ext.Msg.alert("There was an error", error);
		};
		DifferentialExpressionAnalysisController.determineAnalysisType(id, {
					callback : cb,
					errorhandler : eh
				});

	},

	doProcessedVectors : function(id) {
		Ext.Msg.show({
					title : 'Processed vector analysis',
					msg : 'Please confirm. Any existing processed vectors will be deleted.',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = [];
							callParams.push(id);
							Ext.getBody().mask();
							callParams.push({
										callback : function(data) {
											var k = new Gemma.WaitHandler();
											k.handleWait(data, true);
											this.relayEvents(k, ['done', 'fail']);
											Ext.getBody().unmask();
											k.on('done', function(payload) {
														this.fireEvent('processedVector', payload);
													});
										}.createDelegate(this),
										errorHandler : function(message, exception) {
											Ext.Msg.alert("There was an error", message);
											Ext.getBody().unmask();
										}
									});
							ProcessedExpressionDataVectorCreateController.run.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	initComponent : function() {

		Gemma.EEManager.superclass.initComponent.call(this);

		this.addEvents('done', 'reportUpdated', 'differential', 'missingValue', 'link', 'processedVector', 'deleted',
				'tagsUpdated', 'updated');

		this.save = function(id, fields) {
			/*
			 * TODO
			 */
		};

	}

});
