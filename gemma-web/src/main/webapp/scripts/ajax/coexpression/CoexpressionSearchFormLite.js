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

		this.analysisCombo = new Ext.Gemma.AnalysisCombo({
			hiddenName : 'a',
			id : 'analysis-combo',
			fieldLabel : 'Select search scope',
			showCustomOption : false,
			width : 200
		});

		this.analysisCombo.on("analysischanged", function(combo, analysis) {
			this.clearMessages();
			if (analysis && analysis.taxon) {
				this.taxonChanged(analysis.taxon);
			}
		}, this);

		var submitButton = new Ext.Button({
			text : "Find coexpressed genes",
			handler : function() {
				var msg = this.validateSearch(this.geneCombo.getValue(),
						this.analysisCombo.getValue());
				if (msg.length === 0) {
					document.location.href = String.format(
							"/Gemma/searchCoexpression.html?g={0}&a={1}&s={2}",
							this.geneCombo.getValue(), this.analysisCombo
									.getValue(), this.stringencyField
									.getValue());
				} else {
					this.handleError(msg);
				}
			}.createDelegate(this)
		});

		this.add(this.stringencyField);
		this.add(this.geneCombo);
		this.add(this.analysisCombo);
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
			analysisId : this.analysisCombo.getValue(),
			stringency : this.stringencyField.getValue()
		};
		return csc;
	},

	initializeFromCoexpressionSearchCommand : function(csc) {
		if (csc.cannedAnalysisId > -1) {
			this.analysisCombo.setState(csc.cannedAnalysisId);
		}
		if (csc.stringency) {
			this.stringencyField.setValue(csc.stringency);
		}
	},

	validateSearch : function(gene, analysis) {
		if (!gene || gene.length === 0) {
			return "Please select a valid query gene";
		} else if (!analysis) {
			return "Please select an analysis";
		} else {
			return "";
		}
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