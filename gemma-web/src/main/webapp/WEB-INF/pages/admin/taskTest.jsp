<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Task tests</title>
	<jwr:script src='/scripts/api/ext/data/DwrProxy.js' />
	<script type="text/javascript">
	Ext.onReady( function() {
		var f = new Ext.form.FormPanel( {
			width : 300,
			bodyBorder : false,
			renderTo : 'taskForm',
			items : [ {
				xtype : 'textfield',
				fieldLabel : 'Runtime',
				name : 'runtime',
				allowBlank : false,
				value : 15000
			}, {
				xtype : 'checkbox',
				fieldLabel : 'Force Local',
				name : 'forceLocal',
				value : 'off'
			}, {
				xtype : 'checkbox',
				fieldLabel : 'Throw an exception',
				name : 'fail',
				value : 'off'
			} ],
			buttons : [ {
				text : 'Start',
				handler : function() {
					var v = f.getForm().getValues();
					var callParams = [ v.runtime, v.forceLocal === 'on', v.fail === 'on', false /* persist */];
					callParams.push( {
						callback : function(data) {
							var k = new Gemma.WaitHandler();
							k.handleWait(data, false);
							k.on('done', function(payload) {
								f.enable();
							});
						}.createDelegate(this)

					});

					TestTaskController.run.apply(this, callParams);
				},
				scope : this
			} ]
		});
	});
</script>

</head>

<h2>
	Run a completely useless test job
</h2>

<p>
	Uses the same task as the monitor task.
</p>
 

<div id="taskForm"></div>
