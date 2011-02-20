/**
 * Common tasks to run on experiments. To use this, construct it with a id
 * defined in the config (e.g., {id : 'eemanager}). Then you can use things like
 * onClick=Ext.getCmp('eemanager').updateEEReport(id).
 * 
 * @class Gemma.EEManager
 * @extends Ext.Component
 */
Gemma.EEManager = Ext.extend(Ext.Component, {

	name : 'eemanager',

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
				name : 'validatedAnnotations'
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
				name : "datePcaAnalysis",
				type : 'date'
			}, {
				name : "dateBatchFetch",
				type : 'date'
			}, {
				name : "autoTagDate",
				type : 'date'
			}, {
				name : "linkAnalysisEventType"
			}, {
				name : "processedDataVectorComputationEventType"
			}, {
				name : "missingValueAnalysisEventType"
			}, {
				name : "differentialAnalysisEventType"
			}, {
				name : "batchFetchEventType"
			}, {
				name : "pcaAnalysisEventType"
			}]),

	/**
	 * 
	 * @param {}
	 *            id
	 * @param {}
	 *            throbberEl optional element to show the throbber. If omitted,
	 *            a popup progressbar is shown.
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

	historyWindow : null,

	showAuditWindow : function(id) {
		if (this.historyWindow != null) {
			this.historyWindow.destroy();
		}
		this.historyWindow = new Ext.Window({
					layout : 'fit',
					title : 'History',
					modal : false,
					items : [new Gemma.AuditTrailGrid({
								title : '',
								collapsible : false,
								auditable : {
									id : id,
									classDelegatingFor : "ubic.gemma.model.expression.experiment.ExpressionExperimentImpl"
								}
							})]
				});
		this.historyWindow.show();
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

	/**
	 * Break the relationships between bioassays and biomaterials, such that
	 * there is only one bioassay per biomaterial.
	 */
	unmatchBioAssays : function(id) {
		Ext.Msg.show({
					title : 'Are you sure?',
					msg : 'Are you sure you to unmatch the bioassays? (This has no effect if there is only one array design)',
					buttons : Ext.Msg.YESNO,
					fn : function(btn, text) {
						if (btn == 'yes') {
							var callParams = [];
							callParams.push(id);
							callParams.push({
										callback : function(data) {
											// var k = new Gemma.WaitHandler();
											// this.relayEvents(k, ['done',
											// 'fail']);
											// k.handleWait(data, false);
											this.fireEvent('done');
										}.createDelegate(this),
										errorHandler : function(message, exception) {
											Ext.Msg.alert("There was an error", message);
											Ext.getBody().unmask();
										}
									});

							ExpressionExperimentController.unmatchAllBioAssays.apply(this, callParams);
						}
					},
					scope : this
				})
	},

	/**
	 * Display the annotation tagger window.
	 * 
	 * @param {}
	 *            id
	 * @param {}
	 *            taxonId
	 * @param {}
	 *            canEdit
	 */
	tagger : function(id, taxonId, canEdit, isValidated) {
		var annotator = new Ext.Panel({
					id : 'annotator-wrap',
					collapsible : false,
					stateful : false,
					bodyBorder : false,
					layout : 'fit',
					items : [new Gemma.AnnotationGrid({
								id : 'annotator-grid',
								entityAnnotsAreValidated : isValidated,
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
									.alert(
											"Help with tagging",
											"Select a 'category' for the term; then enter a term, "
													+ "choosing from existing terms if possible. "
													+ "Click 'create' to save it. You can also edit existing terms;"
													+ " click 'save' to make the change stick, or 'delete' to remove a selected tag.");
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
							/* after deletion, clear bottom details pane */
							Ext.get('dataSetDetailsPanel').first().last().dom.innerHTML = '<span></span>'

						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	markOutlierBioAssay : function(bioAssayId) {
		Ext.Msg.show({
			title : 'Are you sure?',
			msg : 'Are you sure you want to mark this bioAssay as an outlier? This can be undone only by regenerating the "processed data".',
			buttons : Ext.Msg.YESNO,
			fn : function(btn, text) {
				if (btn == 'yes') {
					var callParams = [];
					callParams.push(bioAssayId);
					Ext.getBody().mask();
					callParams.push({
								callback : function(data) {
									var k = new Gemma.WaitHandler();
									k.handleWait(data, true);
									this.relayEvents(k, ['done', 'fail']);
									Ext.getBody().unmask();
								}.createDelegate(this),
								errorHandler : function(error) {
									Ext.Msg.alert("Outlier marking failed", error);
									Ext.getBody().unmask();
								}.createDelegate(this)
							});
					BioAssayController.markOutlier.apply(this, callParams);
				}
			},
			scope : this,
			animEl : 'elId',
			icon : Ext.MessageBox.WARNING
		});
	},

	/**
	 * Compute coexpression for the data set.
	 * 
	 * @param {}
	 *            id
	 */
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

	doPca : function(id, hasPca) {

		var prompt = 'Please confirm.';
		if (hasPca) {
			prompt = prompt
					+ ' Previous PCA results will be deleted. Indicate if you want to re-run the full PCA or just update the factor correlations with the PCs';
		}

		// ask for confirmation.
		var w = new Ext.Window({
					name : 'pca-dialog',
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
					title : 'Principal component anslysis',
					html : prompt,
					buttons : [{
								text : 'Full PCA',
								handler : function(btn, text) {
									var callParams = [];
									callParams.push(id);
									var postprocessOnly = false;
									callParams.push(postprocessOnly);
									Ext.getBody().mask();
									callParams.push({
												callback : function(data) {
													var k = new Gemma.WaitHandler();
													k.handleWait(data, true);
													this.relayEvents(k, ['done', 'fail']);
													Ext.getBody().unmask();
													k.on('done', function(payload) {
																this.fireEvent('pca', payload);
															});
												}.createDelegate(this),
												errorHandler : function(error) {
													Ext.Msg.alert("PCA analysis failed", error);
													Ext.getBody().unmask();
												}.createDelegate(this)
											});

									SvdController.run.apply(this, callParams);
									w.close();
								}
							}, {
								text : 'Factors only',
								hidden : !hasPca,
								handler : function(btn, text) {
									var callParams = [];
									callParams.push(id);
									var postprocessOnly = true;
									callParams.push(postprocessOnly);
									Ext.getBody().mask();
									callParams.push({
												callback : function(data) {
													var k = new Gemma.WaitHandler();
													k.handleWait(data, true);
													this.relayEvents(k, ['done', 'fail']);
													Ext.getBody().unmask();
													k.on('done', function(payload) {
																this.fireEvent('pca', payload);
															});
												}.createDelegate(this),
												errorHandler : function(error) {
													Ext.Msg.alert("PCA analysis failed", error);
													Ext.getBody().unmask();
												}.createDelegate(this)
											});

									SvdController.run.apply(this, callParams);
									w.close();
								}
							}, {
								text : 'Cancel',
								handler : function() {
									w.close();
								}
							}],
					iconCls : Ext.MessageBox.QUESTION
				});

		w.show();
	},

	doBatchInfoFetch : function(id) {

		Ext.Msg.show({
					title : 'Sample batches information fetcher',
					msg : 'Please confirm. Previous results will be deleted, including "batch" factor.',
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
														this.fireEvent('batchinfo', payload);
													});
										}.createDelegate(this),
										errorHandler : function(error) {
											Ext.Msg.alert("Batch info fetch failed", error);
											Ext.getBody().unmask();
										}.createDelegate(this)
									});
							BatchInfoFetchController.run.apply(this, callParams);
						}
					},
					scope : this,
					animEl : 'elId',
					icon : Ext.MessageBox.WARNING
				});
	},

	/**
	 * Compute the missing values. This is only relevant for two-channel arrays.
	 * 
	 * @param {}
	 *            id
	 */
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

	/**
	 * Interactive setup and running of a differential expression analysis.
	 * 
	 * @param {}
	 *            id
	 */
	doDifferential : function(id) {

		var m = this;

		/*
		 * Do an analysis interactively.
		 */
		var customize = function(analysisInfo) {

			var factors = analysisInfo.factors;
			var proposedAnalysis = analysisInfo.type;

			/*
			 * Set up buttons for the subset form.
			 */
			var subsetRadios = [];
			subsetRadios.push(new Ext.form.Radio({
						boxLabel : 'None', // need so they can unset it.
						name : 'diff-ex-analyze-subset', // same name ->
						// grouped.
						id : 'no-factor-subset-radio',
						checked : true,
						listeners : {
							check : validateFactorsChosen.createDelegate(this, [factors])
						}
					}));

			for (var i = 0; i < factors.length; i++) {
				var f = factors[i];
				if (!f.name) {
					continue;
				}

				/*
				 * set up the subsets.
				 */
				subsetRadios.push(new Ext.form.Radio({
							boxLabel : "<b>" + f.name + "</b> (" + f.description + ")",
							name : 'diff-ex-analyze-subset', // same name ->
							// grouped.
							id : f.id + '-factor-subset-radio',
							checked : false
						}));
			}

			/*
			 * DifferentialExpressionAnalysisCustomization - only available if
			 * there is more than one factor. We should refactor this code.
			 */
			var deasw = new Ext.Window({
				name : 'diff-customization-window',
				modal : true,
				stateful : false,
				resizable : false,
				autoHeight : true,
				width : 460,
				plain : true,
				border : false,
				title : "Differential analysis settings",
				padding : 10,
				items : [{
							xtype : 'form',
							bodyBorder : false,
							autoHeight : true,
							items : [{
										xtype : 'fieldset',
										title : "Select factor(s) to use",
										autoHeight : true,
										labelWidth : 375,
										id : 'diff-ex-analysis-customize-factors'
									}, {
										xtype : 'fieldset',
										title : "Optional: Select a subset factor",
										items : [{
													xtype : 'radiogroup',
													columns : 1,
													allowBlank : true,
													autoHeight : true,
													id : 'diff-ex-analysis-subset-factors',
													items : subsetRadios,
													hideLabel : true,
													listeners : {
														change : validateFactorsChosen.createDelegate(this, [factors])
													}
												}]
									},

									{
										xtype : 'fieldset',
										labelWidth : 375,
										autoHeight : true,
										hidden : false,

										/*
										 * we hide this if we have more than 2
										 * factors -- basically where we're not
										 * going to bother supporting
										 * interactions.
										 */
										items : [{
													xtype : 'checkbox',
													id : 'diff-ex-analysis-customize-include-interactions-checkbox',
													fieldLabel : 'Include interactions if possible'
												}]
									}]
						}],

				buttons : [{
					text : "Help",
					id : 'diff-ex-customize-help-button',
					disabled : false,
					scope : this,
					handler : function() {
						Ext.Msg.show({
							title : 'Processed vector analysis',
							msg : 'Choose which factors to include in the model. If you choose only one, the analysis will be a t-test or one-way-anova. If you choose two factors, you might be able to include interactions. If you choose three or more, '
									+ 'interactions will not be estimated.'
									+ 'You can also choose to analyze different parts of the data sets separately, by splitting it up according to the factors listed. The analysis is then done independently on each subset.',
							buttons : Ext.Msg.OK,
							icon : Ext.MessageBox.INFO
						});
					}
				}, {
					text : 'Proceed',
					id : 'diff-ex-customize-proceed-button',
					disabled : false,
					scope : this,
					handler : function(btn, text) {

						var includeInteractions = Ext
								.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').getValue();

						/*
						 * Get the factors the user checked. See checkbox
						 * creation code below.
						 */
						var factorsToUseIds = getFactorsToUseIds(factors);
						var subsetFactor = getSubsetFactorId(factors);

						if (factorsToUseIds.length < 1) {
							Ext.Msg.alert("Invalid selection", "Please pick at least one factor.");
							return;
						}

						/*
						 * This should be disallowed by the interface, but just
						 * in case.
						 */
						if (subsetFactor !== null && factorsToUseIds.indexOf(subsetFactor) >= 0) {
							Ext.Msg.alert("Invalid selection", "You cannot subset on a factor included in the model.");
							return;
						}

						/*
						 * Pass back the factors to be used, and the choice of
						 * whether interactions are to be used.
						 */
						var callParams = [];
						callParams.push(id);
						callParams.push(factorsToUseIds);
						callParams.push(includeInteractions);
						callParams.push(subsetFactor)
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
			 * Create the checkboxes for user choice of factors. We assume there
			 * is more than one.
			 */
			if (factors) {
				for (var i = 0; i < factors.length; i++) {
					var f = factors[i];
					if (!f.name) {
						continue;
					}

					/*
					 * Checkbox for one factor.
					 */

					Ext.getCmp('diff-ex-analysis-customize-factors').add(new Ext.form.Checkbox({
								fieldLabel : "<b>" + f.name + "</b> (" + f.description + ")",
								// labelWidth : 375,
								id : f.id + '-factor-checkbox',
								tooltip : f.name,
								checked : false,
								listeners : {
									check : validateFactorsChosen.createDelegate(this, [factors])
								}
							}));
				}
			}

			deasw.doLayout();
			deasw.show();

		};

		var getFactorsToUseIds = function(factors) {
			var factorsToUseIds = [];
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
			return factorsToUseIds;
		};

		var getSubsetFactorId = function(factors) {
			var subsetFactor = null;
			/*
			 * get values of subset radios
			 */
			for (var i = 0; i < factors.length; i++) {
				var f = factors[i];
				if (!f.name) {
					continue;
				}
				var checked = Ext.getCmp(f.id + '-factor-subset-radio').getValue();
				if (checked) {
					subsetFactor = f.id;
					break;
				}
			}
			return subsetFactor;
		};

		/**
		 * Callback for analysis type determination. This gets the type of
		 * analysis, if it can be determined. If the type is non-null, then just
		 * ask the user for confirmation. If they say no, or the type is null,
		 * show them the DifferentialExpressionAnalysisSetupWindow.
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
							name : 'diffex-dialog',
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
							html : 'Please confirm. The analysis performed will be a ' + analysisType
									+ '. If there is an existing analysis on the same factor(s), it will be deleted.',
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
				 * System couldn't guess the analysis type, so force user to
				 * customize.
				 */
				customize(analysisInfo);
			}
		};

		/*
		 * Make sure checkboxes are logically consistent (warning: this might
		 * not work 100% perfectly, so it's a good idea to validate again later
		 * on the client side)
		 */
		var validateFactorsChosen = function(factors) {
			var factorsToUseIds = getFactorsToUseIds(factors);
			var subsetFactor = getSubsetFactorId(factors);

			if (factorsToUseIds.length != 2) {
				Ext.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').setValue(false);
				Ext.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').disable();
			} else {
				Ext.getCmp('diff-ex-analysis-customize-include-interactions-checkbox').enable();
			}

			/*
			 * The top checkboxes take precendence. We unset the 'subset' if
			 * there is a conflict.
			 */
			if (subsetFactor !== null && factorsToUseIds.indexOf(subsetFactor) >= 0) {
				Ext.getCmp(subsetFactor + '-factor-subset-radio').setValue(false);
				Ext.getCmp('no-factor-subset-radio').setValue(true);
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

	/**
	 * Run the vector processing. Note that this is normally done when the data
	 * are first imported, so this is rarely needed unless something fundamental
	 * changes about the data set.
	 */
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
				'tagsUpdated', 'updated', 'pca', 'batchinfo');

		this.save = function(id, fields) {
			/*
			 * TODO
			 */
		};

	}

});
