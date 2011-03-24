/**
 * 
 * @class Gemma.CoexpressionSearchFormLite
 * @extends Ext.FormPanel
 */
Gemma.CoexpressionSearchFormLite = Ext.extend(Ext.FormPanel, {

			autoHeight : true,
			frame : true,
			stateful : true,
			stateEvents : ["beforesearch"],

			// share state with complex
			// form...
			stateId : "Gemma.CoexpressionSearch",
			labelAlign : "top",

			width : 200,

			initComponent : function() {

				Gemma.CoexpressionSearchFormLite.superclass.initComponent.call(this);

				this.stringencyField = new Ext.form.Hidden({
							value : 3
						});

				this.geneCombo = new Gemma.GeneCombo({
							hiddenName : 'g',
							id : 'gene-combo',
							fieldLabel : 'Select a query gene',
							width : 175
						});

				this.geneCombo.on("focus", this.clearMessages, this);

				this.store = new Gemma.DatasetGroupStore();

				this.eeSetCombo = new Gemma.DatasetGroupCombo({
							width : 175,
							fieldLabel : 'Select search scope',
							store : this.store
						});

				this.eeSetCombo.on("select", function(combo, eeSet) {
							this.clearMessages();
							this.selectedEESet = eeSet;
							if (eeSet && eeSet.store.getSelected().get("taxonId")) {
								var taxon = {
									id : eeSet.store.getSelected().get("taxonId"),
									name : eeSet.store.getSelected().get("taxonName")
								};
								this.taxonChanged(taxon);
							}
						}, this);

				var submitButton = new Ext.Button({
							text : "Find coexpressed genes",
							handler : function() {
								var msg = this.validateSearch(this.geneCombo.getValue());
								if (msg.length === 0) {

									var eeSetId = this.selectedEESet.get("id");
									var eeIds = "";

									if (!eeSetId || eeSetId < 0) {
										eeIds = this.selectedEESet.get("expressionExperimentIds").join(",");
									}

									if (typeof pageTracker != 'undefined') {
										pageTracker._trackPageview("/Gemma/coexpressionSearch.doLiteSearch");
									}
									document.location.href = String.format(
											"/Gemma/searchCoexpression.html?g={0}&a={1}&s={2}&ees={3}&t={4}",
											this.geneCombo.getValue(), eeSetId, this.stringencyField.getValue(), eeIds,
											this.geneCombo.getTaxon().id);
								} else {
									this.handleError(msg);
								}
							}.createDelegate(this)
						});

				this.add(this.stringencyField);
				this.add(this.eeSetCombo);
				this.add(this.geneCombo);
				this.addButton(submitButton);
			},

			applyState : function(state, config) {
				if (state) {
					this.csc = state;
				}
			},

			getState : function() {
				var obj = this.getCoexpressionSearchCommand();
				// Do not save the taxon or the eeSet picker, they maintain state separately.
				return {
					geneIds : obj.geneIds,
					stringency : obj.stringency
				};
			},

			render : function(container, position) {
				Gemma.CoexpressionSearchFormLite.superclass.render.apply(this, arguments);

				// initialize from state
				if (this.csc) {
					this.initializeFromCoexpressionSearchCommand(this.csc);
				}
			},

			getCoexpressionSearchCommand : function() {
				var csc = {
					geneIds : [this.geneCombo.getValue()],
					analysisId : this.eeSetChooserPanel.getSelected().get("id").getValue(),
					stringency : this.stringencyField.getValue()
				};
				return csc;
			},

			initializeFromCoexpressionSearchCommand : function(csc) {
				if (csc.eeSetId > -1) {
					// this.eeSetChooserPanel.getSelected().get("id").setState(csc.cannedAnalysisId);
				}
				if (csc.stringency) {
					this.stringencyField.setValue(csc.stringency);
				}
			},

			validateSearch : function(gene) {
				if (!gene || gene.length === 0) {
					return "Please select a valid query gene";
				}
				if (!this.selectedEESet) {
					return "Please select a query scope";
				}
				return "";
			},

			handleError : function(msg, e) {
				Ext.DomHelper.overwrite("coexpression-messages", {
							tag : 'img',
							src : '/Gemma/images/icons/warning.png'
						});
				Ext.DomHelper.append("coexpression-messages", {
							tag : 'span',
							html : "&nbsp;&nbsp;" + msg
						});
			},

			clearMessages : function() {
				if (Ext.DomQuery.select("coexpression-messages").length > 0) {
					Ext.DomHelper.overwrite("coexpression-messages", {
								tag : 'h3',
								html : "Coexpression query"
							});
				}
			},

			taxonChanged : function(taxon) {
				this.geneCombo.setTaxon(taxon);
			}

		});