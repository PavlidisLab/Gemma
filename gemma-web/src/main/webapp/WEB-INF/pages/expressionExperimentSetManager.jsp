<%@ include file="/common/taglibs.jsp"%>

<head>
<title>Dataset Group Manager</title>

<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />
<jwr:script src='/scripts/ajax/entities/DatasetGroupEditor.js' />

<security:authorize ifAnyGranted="GROUP_ADMIN,GROUP_USER">
	<script type="text/javascript">
	Ext.namespace('Gemma');
	Ext.onReady(function() {
		Ext.QuickTips.init();

		var eeSetChooserPanel = new Gemma.DatasetGroupComboPanel( {
			renderTo : 'EESetManager'
		});
		eeSetChooserPanel.show();

	});
</script>
</security:authorize>

</head>
<body>
	<security:authorize access="!hasAnyRole('GROUP_USER','GROUP_ADMIN')">
		<script type="text/javascript">
			Gemma.AjaxLogin.showLoginWindowFn(true);
		</script>
	<p>Sorry, you must be logged in to use this tool.</p>
</security:authorize>

<security:authorize access="hasAnyRole('GROUP_USER','GROUP_ADMIN')">
	<div id='messages' style='width: 600px; height: 1.6em; margin: 0.2em; padding-bottom: 0.4em;'></div>

	<p>Use this tool to create and edit groups of datasets. You can modify a built-in set by making a copy ("clone")
	and editing the copy.</p>


</security:authorize>


<div id='EESetManager'></div>
</body>