Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.namespace('Gemma.DatasetChooser');

Gemma.DatasetChooser.app = function() {

	var btn;
	var dcp;

	return {
		init : function() {

			Ext.QuickTips.init();

			// dcp = new Ext.Gemma.DatasetChooserPanel();
			//
			// gp = new Ext.Gemma.GeneChooserPanel({
			// renderTo : 'panel',
			// frame : true,
			// width : 500
			// });
			//
			// btn = new Ext.Button({
			// renderTo : 'but',
			// text : "Show ds picker",
			// handler : function() {
			// dcp.show();
			// }
			// });

			// ach = new Ext.Gemma.AnalysisCombo({
			// renderTo : 'but',
			// showCustomOption : true
			// });

			var searchForm = new Ext.Gemma.CoexpressionSearchFormLite({
				renderTo : "but"
			});

			// txc = new Ext.Gemma.TaxonCombo({
			// renderTo : 'but'
			// });

			// Ext.state.Manager.setProvider(new Ext.state.SessionProvider({
			// state : Ext.appState
			// }));

			// dcp.on("datasets-selected", function(e) {
			// Ext.Msg.alert("Yay", "You got " + e.eeIds.length + " ee ids");
			// });

			// dcp.show();
		}
	};
}();