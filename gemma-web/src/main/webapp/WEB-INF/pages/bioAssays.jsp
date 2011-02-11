<%@ include file="/common/taglibs.jsp"%>
<head>
	<title><fmt:message key="bioAssays.title" />
	</title>

	<jwr:script src='/scripts/ajax/ext/data/DwrProxy.js' />

	<script type="text/javascript">
Ext.BLANK_IMAGE_URL = '/Gemma/images/default/s.gif';

Ext.onReady(function() {

	Ext.QuickTips.init();
	Ext.state.Manager.setProvider(new Ext.state.CookieProvider());

	var manager = new Gemma.EEManager( {
		editable : true,
		id : "eemanager"
	});

	manager.on('done', function() {
		window.location.reload(true);
	});

});
</script>

</head>
<h2>
	<fmt:message key="bioAssays.title" />
	for
	<a
		href='<c:url value="showExpressionExperiment.html" />?id=${expressionExperiment.id }'>${expressionExperiment.shortName}</a>
</h2>
<p>
	View the
	<a
		href='<c:url value="/experimentalDesign/showExperimentalDesign.html?eeid=${expressionExperiment.id }" />'>Experimental
		design</a>
</p>

<Gemma:expressionQC ee="${expressionExperiment.id}" />

<display:table name="bioAssays" class="list"
	requestURI="/Gemma/expressionExperiment/showBioAssaysFromExpressionExperiment.html"
	id="bioAssayList" sort="list" pagesize="50"
	decorator="ubic.gemma.web.taglib.displaytag.expression.bioAssay.BioAssayWrapper">

	<display:column property="nameLink" sortable="true"
		titleKey="bioAssay.name" maxWords="20" />
	<display:column property="description" sortable="true"
		titleKey="bioAssay.description" maxWords="20" />
	<display:column property="processingDate" sortable="true"
		title="Batch date" format="{0,date,dd-MM-yyyy}" />

	<security:authorize access="hasRole('GROUP_ADMIN')">
		<%-- FIXME should be if they can edit this EE, not just admin. --%>
		<display:column property="delete" sortable="false" title="QC" />
	</security:authorize>

	<display:setProperty name="basic.empty.showtable" value="true" />
</display:table>
