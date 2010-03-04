<%@ include file="/common/taglibs.jsp"%>

<head>
	<title>Expression Experiment Set Manager</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
	<jwr:script src='/scripts/ajax/entities/DatasetChooserPanel.js' />

</head>



<h1>
	Expression Experiment Set Manager 
</h1>
<security:authorize access="!hasRole('GROUP_ADMIN')">
Sorry, you must be an administrator to use this tool.

</security:authorize>


	<security:authorize access="hasRole('GROUP_ADMIN')">
	<div id='messages' style='width: 600px; height: 1.6em; margin: 0.2em; padding-bottom: 0.4em;'></div>
	<script type="text/javascript">
	Ext.namespace('Gemma');
	Ext.onReady( function() {
		Ext.QuickTips.init();

		var eeSetChooserPanel = new Gemma.ExpressionExperimentSetPanel( {
			renderTo : 'EESetManager',
			isAdmin : true 
		});

	});
</script>
</security:authorize>


<div id='EESetManager'></div>
