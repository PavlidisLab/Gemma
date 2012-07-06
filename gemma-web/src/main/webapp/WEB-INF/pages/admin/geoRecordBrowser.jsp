<%@ include file="/common/taglibs.jsp"%>

<head>
<title>GEO Record browser</title>

<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/app/geoBrowse.js' />

<script type="text/javascript">
	Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

	Ext.onReady(function() {

		Ext.QuickTips.init();
		Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

		var grid = new Gemma.GeoBrowseGrid({});

		var form = new Ext.form.FormPanel({
			width : 300,
			height : 100,
			bodyStyle : 'padding:5px',
			items : [ {
				id : 'back',
				xtype : 'button',
				text : 'Back',
				handler : function() {
					grid.back(0);
				},
				width : 50

			}, {
				id : 'next',
				xtype : 'button',
				text : 'Next',
				handler : function() {
					grid.proceed(Ext.get('skip').getValue());
				},
				width : 50
			}, {
				id : 'skip',
				xtype : 'textfield',
				name : 'Skip',
				fieldLabel : 'Skip',
				width : 80,
				listeners : {
					specialKey : function(field, eventObj) {
						if (eventObj.getKey() == Ext.EventObject.ENTER) {
							grid.proceed(field.getValue());
						}
					}
				},
			} ]

		});

		new Ext.Panel({
			renderTo : 'geostuff',
			items : [ form, grid ]
		})

	});
</script>

</head>
<body>

	<div id="messages" style="margin: 10px; width: 600px"></div>

	<div id="progress-area" style="padding: 5px;"></div>

	<p>
		Displaying <b> <span id="numRecords" />
		</b> GEO records.
	</p>


	<div id="geostuff" style="padding: 20px; margin: 10px;" />

	<br />
	<p>
		Note: Records are not shown for taxa not in the Gemma system. If you choose to load an experiment, please be careful:
		experiments that have two (or more) array designs should be loaded using the regular load form if you need to suppress
		the sample-matching functions. <strong>Click on a row</strong> to display more information about the dataset, if available
		from GEO, including information about platforms. This information is often not available for a day or two after the
		data sets becomes publicly available.
	</p>
	<div id="taskId" style="display: none;"></div>
</body>