/**
 * 
 * @class Ext.Gemma.CoexpressionSearchFormLite
 * @extends Ext.FormPanel
 */
Ext.Gemma.CoexpressionSearchFormLite = Ext.extend(Ext.FormPanel, {

	autoHeight : true,
	frame : true,
	stateful : true,
	stateEvents : ["beforesearch"],
	stateId : "Ext.Gemma.CoexpressionSearch", // share state with complex
	// form...
	labelAlign : "top",

	width : 230,

	initComponent : function() {

		Ext.Gemma.CoexpressionSearchFormLite.superclass.initComponent
				.call(this);

		this.stringencyField = new Ext.form.Hidden({
			value : 3
		});

		this.geneCombo = new Ext.Gemma.GeneCombo({
			hiddenName : 'g',
			id : 'gene-combo',
			fieldLabel : 'Select a query gene',
			width : 200
		});

		this.geneCombo.on("focus", this.clearMessages, this);

		this.store = new Ext.Gemma.ExpressionExperimentSetStore();

		this.eeSetCombo = new Ext.Gemma.ExpressionExperimentSetCombo({
			width : 175,
			fieldLabel : 'Select search scope',
			store : this.store
		});

		this.eeSetCombo.on("select", function(combo, eeSet) {
			this.clearMessages();
			this.selected = eeSet;
			if (eeSet && eeSet.get("taxon")) {
				this.taxonChanged(eeSet.get("taxon"));
			}
		}, this);

		var submitButton = new Ext.Button({
			text : "Find coexpressed genes",
			handler : function() {
				var msg = this.validateSearch(this.geneCombo.getValue());
				if (msg.length === 0) {
					// FIXME add the eeids if the eesetid is -1.
					var eeSetId = this.selected.get("id");
					var eeIds = this.selected.get("expressionExperimentIds")
							.join(",");
					document.location.href = String
							.format(
									"/Gemma/searchCoexpression.html?g={0}&a={1}&s={2}&ees={3}",
									this.geneCombo.getValue(), eeSetId,
									this.stringencyField.getValue(), eeIds);
				} else {
					this.handleError(msg);
				}
			}.createDelegate(this)
		});

		this.add(this.stringencyField);
		this.add(this.geneCombo);
		this.add(this.eeSetCombo);
		this.addButton(submitButton);
	},

	applyState : function(state, config) {
		if (state) {
			this.csc = state;
		}
	},

	getState : function() {
		return this.getCoexpressionSearchCommand();
	},

	render : function(container, position) {
		Ext.Gemma.CoexpressionSearchFormLite.superclass.render.apply(this,
				arguments);

		// initialize from state
		if (this.csc) {
			this.initializeFromCoexpressionSearchCommand(this.csc);
		}
	},

	getCoexpressionSearchCommand : function() {
		var csc = {
			geneIds : [this.geneCombo.getValue()],
			analysisId : this.eeSetChooserPanel.getSelected().get("id")
					.getValue(),
			stringency : this.stringencyField.getValue()
		};
		return csc;
	},

	initializeFromCoexpressionSearchCommand : function(csc) {
		if (csc.cannedAnalysisId > -1) {
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
		// else if (!analysis) {
		// return "Please select an analysis";
		// } else {
		// return "";
		// }
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