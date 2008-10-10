Ext.namespace('Gemma');
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

/**
 * This is not a visual component but we want to use it with the componentmanager.
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

		this.editable = Ext.get("hasAdmin").getValue() == 'true';

		/*
		 * Load the EE information via an ajax call.
		 */
		ExpressionExperimentController.loadExpressionExperimentDetails(id, this.build.createDelegate(this));

	},

	save : function() {
		var snField = Ext.getCmp('shortname');
		var dField = Ext.getCmp('description')
		var nField = Ext.getCmp('name');
		var shortName = snField.getValue();
		var description = dField.getValue();
		var name = nField.getValue();

		var entity = {
			id : this.eeId
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
					this.manager.handleWait(data, 'updated', false);
				}.createDelegate(this))
	},

	savePubMed : function() {
		var pubmedId = Ext.getCmp('pubmed-id-field').getValue();
		ExpressionExperimentController.updatePubMed(this.eeId, pubmedId, {
					callback : function(data) {
						this.manager.handleWait(data, 'pubmedUpdated', false);
					}.createDelegate(this)
				});

	},

	removePubMed : function() {
		ExpressionExperimentController.removePrimaryPublication(this.eeId, {
					callback : function(data) {
						this.manager.handleWait(data, 'pubmedRemove', false)
					}.createDelegate(this)
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
			pubmedUrl = pubmedUrl + '<a href="#" onClick="Ext.getCmp(\'ee-details-panel\').removePubMed()">'
					+ '<img src="/Gemma/images/icons/cross.png"  ext:qtip="Remove publication"  /></a>&nbsp;';
		}

		var pubmedRegion = {
			id : 'pubmed-region',
			xtype : 'panel',
			baseCls : 'x-plain-panel',
			html : pubmedUrl,
			width : 380
		}
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
						emptyText : 'Enter pubmed id',
						width : 100,
						id : 'pubmed-id-field',
						enableKeyEvents : true,
						listeners : {
							'keyup' : {
								fn : function(e) {
									if (Ext.getCmp('pubmed-id-field').isDirty()
											&& Ext.getCmp('pubmed-id-field').isValid()) {
										// show save button
										Ext.getCmp('update-pubmed-region').show();
									} else {
										Ext.getCmp('update-pubmed-region').hide();
									}
								},
								scope : this
							}
						}
					}, {
						baseCls : 'x-plain-panel',
						id : 'update-pubmed-region',
						html : '<a href="#" onClick="Ext.getCmp(\'ee-details-panel\').savePubMed('
								+ e.id
								+ ',[\'shortname\',\'name\',\'description\'])" ><img src="/Gemma/images/icons/database_save.png" title="Click to save changes" alt="Click to save changes"/></a>',
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
			result = result + '<a target="_blank" href="/Gemma/arrays/showArrayDesign.html?id=' + ad.id + '">'
					+ ad.shortName + '</a> - ' + ad.name;
			if (i < arrayDesigns.length - 1) {
				result = result + "<br/>";
			}
		}
		return result;
	},

	build : function(e) {
		// console.log(e);
		adminLinks = '<a href="#" onClick="Ext.getCmp(\'eemanager\').updateEEReport('
				+ e.id
				+ ')"><img src="/Gemma/images/icons/arrow_refresh_small.png" ext:qtip="Refresh statistics"  title="refresh"/></a>'
				+ '&nbsp;<a href="/Gemma/expressionExperiment/editExpressionExperiment.html?id='
				+ e.id
				+ '"  target="_blank"><img src="/Gemma/images/icons/wrench.png" ext:qtip="Go to editor page for this experiment" title="edit"/></a>&nbsp;'
				+ '<a href="#" onClick="return Ext.getCmp(\'eemanager\').deleteExperiment('
				+ e.id
				+ ')"><img src="/Gemma/images/icons/cross.png" ext:qtip="Delete the experiment from the system" title="delete" /></a>&nbsp;';

		var pubmedRegion = {};

		if (e.pubmedId) {
			// display the citation, with link out and delete button.
			pubmedRegion = this.getPubMedHtml(e);
		} else {
			// offer to create a citation link.
			pubmedRegion = this.getPubMedForm(e);
		}

		/*
		 * TODO use this.
		 */
		var externalLink = '';
		if (e.externalUri && e.accession) {
			externalLink = "<a href=\"" + e.externalUri + "\">" + e.accession + "</a>"
		}

		DesignMatrix.init({
					id : e.id
				});

		/*
		 * This is needed to make the annotator initialize properly.
		 */
		new Gemma.MGEDCombo({});

		var taggerurl = '<a href="#" onClick="return Ext.getCmp(\'eemanager\').tagger(' + e.id
				+ ')"><img src="/Gemma/images/icons/pencil.png" alt="add tags" title="add tags"/></a>';

		manager = new Gemma.EEManager({
					editable : this.editable,
					id : "eemanager"
				});
		this.manager = manager;

		tagView = new Gemma.AnnotationDataView({
					readParams : [{
								id : e.id,
								classDelegatingFor : "ExpressionExperimentImpl"
							}]
				});

		manager.on('tagsUpdated', function() {
					tagView.store.reload();
				});

		manager.on('reportUpdated', function(data) {
					ob = data[0];
					// console.log(ob);
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

		manager.on('pubmedUpdated', function(e) {
					var html = this.getPubMedHtml(e);
					Ext.getCmp('pubmed-region-wrap').remove(Ext.getCmp('pubmed-region'));
					Ext.DomHelper.append('pubmed-region-wrap', html);
				}.createDelegate(this));

		manager.on('pubmedRemove', function(success) {
					if (success) {
						var r = Ext.getCmp('pubmed-region-wrap');
						r.remove(Ext.getCmp('pubmed-region'));
						var form = this.getPubMedForm(this.eeId);
						r.add(form);
						r.doLayout();
					}
				}.createDelegate(this));

		manager.on('updated', function(data) {
					Ext.getCmp('update-button-region').getEl().hide();

					/*
					 * Really we won't need to do this -- as the state on the database is supposed to match this one.
					 */

					var k = Ext.getCmp('shortname');
					k.setValue(data.shortName);

					k = Ext.getCmp('name');
					k.setValue(data.name);

					k = Ext.getCmp('description');
					k.setValue(data.description);

				}.createDelegate(this));

		var basics = new Ext.Panel({
			autoHeight : true,
			layout : 'table',

			layoutConfig : {
				columns : 2
			},

			renderTo : 'basics',
			collapsible : true,
			bodyBorder : false,
			frame : false,
			baseCls : 'x-plain-panel',
			bodyStyle : 'padding:10px',
			defaults : {
				bodyStyle : 'vertical-align:top',
				baseCls : 'x-plain-panel',
				fieldClass : 'x-bare-field',

				allowBlank : false
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
									disabledClass : 'disabled-plain',
									fieldClass : 'x-bare-field',
									disabled : !this.editable,
									listeners : {
										'keyup' : {
											fn : function(e) {
												if (Ext.getCmp('shortname').isDirty()
														&& Ext.getCmp('shortname').isValid()) {
													// show save button
													Ext.getCmp('update-button-region').show();
												} else {
													Ext.getCmp('update-button-region').hide();
												}
											},
											scope : this
										}
									},
									width : 100,
									value : e.shortName
								}, {
									baseCls : 'x-plain-panel',
									id : 'update-button-region',
									html : '<a href="#" onClick="Ext.getCmp(\'ee-details-panel\').save('
											+ e.id
											+ ',[\'shortname\',\'name\',\'description\'])" ><img src="/Gemma/images/icons/database_save.png" title="Click to save changes" alt="Click to save changes"/></a>',
									hidden : true
								}]
					}, {
						html : 'Name:'
					}, {
						xtype : 'textfield',
						value : e.name,
						id : 'name',
						disabled : !this.editable,
						disabledClass : 'disabled-plain',
						enableKeyEvents : true,
						listeners : {
							'keyup' : {
								fn : function(e) {
									if (Ext.getCmp('name').isDirty() && Ext.getCmp('name').isValid()) {
										// show save button
										Ext.getCmp('update-button-region').show();
									} else {
										Ext.getCmp('update-button-region').hide();
									}
								}
							}
						},
						id : 'name',
						width : 500,
						fieldLabel : 'Name'
					}, {
						html : "Taxon:"
					}, {
						html : e.taxon
					}, {
						html : 'Description:'
					}, {
						xtype : 'textarea',
						id : 'description',
						grow : true,
						growMax : 300,
						disabled : !this.editable,
						disabledClass : 'disabled-plain',
						growMin : 40,
						emptyText : 'No description provided',
						enableKeyEvents : true,
						listeners : {
							'keyup' : {
								fn : function(e) {
									if (Ext.getCmp('description').isDirty() && Ext.getCmp('description').isValid()) {
										// show save button
										Ext.getCmp('update-button-region').show();
									} else {
										Ext.getCmp('update-button-region').hide();
									}
								}
							}
						},
						width : 500,
						// height : 100,
						value : e.description
					}, {
						html : 'Created:'
					}, {
						html : Ext.util.Format.date(e.dateCreated)
					}, {
						html : 'Samples:'
					}, {
						html : e.bioAssayCount,
						width : 60
					}, {
						html : 'Profiles:'
					}, {
						id : 'processedExpressionVectorCount-region',
						html : e.processedExpressionVectorCount,
						width : 60
					}, {
						html : 'Array designs:'
					}, {
						id : 'arrayDesign-region',
						html : this.renderArrayDesigns(e.arrayDesigns),
						width : 480
					}, {
						html : 'Coexp. Links:'
					}, {
						id : 'coexpressionLinkCount-region',
						html : e.coexpressionLinkCount,
						width : 60
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
						html : this.editable ? 'Admin' : ''
					}, {
						html : this.editable ? adminLinks : ''
					}

			/*
			 * data download, array designs used , accession, authors
			 */
			]
		});

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
								// Ext.get('eedetails').fadeIn({
								// duration : 0.9
								// });

							}, 250);
					});

		});
