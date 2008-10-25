Ext.namespace("Gemma");

Gemma.NewsDisplay = Ext.extend(Ext.Panel, {

	height : 300,
	width : 350,
	baseCls : 'x-plain-panel',
	bodyStyle : 'margin : 10px',
	initComponent : function() {

		/*
		 * I don't know why, but if you just initialize items outside of initcomponent it fails.
		 */
		Ext.apply(this, {
			items : new Ext.DataView({

						autoHeight : false,
						emptyText : 'No news',
						loadingText : 'Loading news ...',

						store : new Ext.data.Store({
									proxy : new Ext.data.DWRProxy(FeedReader.getLatestNews),
									reader : new Ext.data.ListRangeReader({}, Ext.data.Record.create([{
														name : "title"
													}, {
														name : "date",
														type : "date"
													}, {
														name : "body"
													}, {
														name : "teaser"
													}])),
									autoLoad : true
								}),

						tpl : new Ext.XTemplate(' <tpl for="."><div class="news"><h3>{title}</h3>{body}</div></tpl>')

					})
		});

		Gemma.NewsDisplay.superclass.initComponent.call(this);
	}

});
