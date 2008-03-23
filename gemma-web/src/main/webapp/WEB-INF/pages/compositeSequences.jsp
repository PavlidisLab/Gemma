<%@ include file="/common/taglibs.jsp"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">

<head>
	<script
		src="<c:url value='/scripts/ext/adapter/prototype/ext-prototype-adapter.js'/>"
		type="text/javascript"></script>
	<script src="<c:url value='/scripts/ext/ext-all.js'/>"
		type="text/javascript"></script>

	<script type="text/javascript"
		src="<c:url value='/scripts/ext/data/DwrProxy.js'/>"></script>

	<script type="text/javascript"
		src="<c:url value='/scripts/ext/data/ListRangeReader.js'/>"></script>

	<script type='text/javascript'
		src='/Gemma/dwr/interface/CompositeSequenceController.js'></script>

	<script type='text/javascript' src='/Gemma/dwr/engine.js'></script>
	<script type='text/javascript' src='/Gemma/dwr/util.js'></script>

	<script type="text/javascript"
		src="<c:url value='/scripts/ajax/blatres.grid.js'/>"
		type="text/javascript"></script>
		
    <script type="text/javascript"
		src="<c:url value='/scripts/ajax/probe.grid.js'/>"
		type="text/javascript"></script>

	<script type="text/javascript"
		src="<c:url value="/scripts/scrolltable.js"/>"></script>
	<link rel="stylesheet" type="text/css"
		href="<c:url value='/styles/scrolltable.css'/>" />

</head>

<title><fmt:message key="compositeSequences.title" /></title>

<h2>
	<fmt:message key="compositeSequences.title" />
</h2>

<div id="blatres-grid" class="x-grid-mso" style="border: 1px solid #c3daf9; overflow: hidden; width:430px "></div>
		<input type="hidden" name="cs" id="cs" value="${compositeSequence.id}" />

<%-- fixme --%>
<div id="probe-grid" class="x-grid-mso"
						style="border: 1px solid #c3daf9; overflow: hidden; width:430px "></div>
					<input type="hidden" name="cs" id="cs"
						value="${compositeSequenceIdList}" />

<display:table name="compositeSequences" class="list" requestURI=""
	id="compositeSequenceList" pagesize="100">
	<display:column property="name" sortable="true"
		titleKey="compositeSequence.name" maxWords="20"
		href="/Gemma/compositeSequence/showCompositeSequence.html"
		paramProperty="id" />
	<display:column property="arrayDesign.shortName" sortable="true"
		title="Array Design" maxWords="20"
		href="/Gemma/arrays/showArrayDesign.html" paramId="id"
		paramProperty="arrayDesign.id" />
	<display:column property="biologicalCharacteristic.name"
		sortable="true" title="Biosequence" maxWords="20"
		href="/Gemma/genome/bioSequence/showBioSequence.html" paramId="id"
		paramProperty="biologicalCharacteristic.id" />
	<display:column property="description" sortable="true"
		titleKey="compositeSequence.description" maxWords="100" />
	<display:setProperty name="basic.empty.showtable" value="true" />
</display:table>
