Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.namespace('Gemma.DatasetChooser');

Gemma.DatasetChooser.app = function() {

	var btn;
	var dcp;

	return {
		init : function() {

			Ext.QuickTips.init();

			Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

			tc = new Ext.Gemma.TaxonCombo({
				renderTo : 'but'
			});

			dcp = new Ext.Gemma.ExpressionExperimentSetPanel({
				renderTo : 'but'
			});

			tc.on("select", function(combo, record, index) {
				dcp.filterByTaxon(record.data);
			});

			tf = new Ext.form.TextField({
				renderTo : 'but',
				width : 150
			});

			dcp.on("set-chosen", function(e) {
				tf
						.setValue(e.get("expressionExperimentIds").length
								+ " ee ids");
			});

		}
	};
}();