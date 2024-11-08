<%@ include file="/common/taglibs.jsp" %>
<jsp:useBean id="bioAssay" scope="request" class="ubic.gemma.model.expression.bioAssay.BioAssayValueObject" />

<head>
<title><fmt:message key="bioAssay.details" /></title>
<meta name="description" content="${fn:escapeXml(bioAssay.description)}" />
</head>

<div class="padded">
    <h2>
        <fmt:message key="bioAssay.details" />
    </h2>
    <table>
        <tr>
            <td class="label"><strong><fmt:message key="bioAssay.name" /></strong></td>
            <td>${bioAssay.name}</td>
        </tr>
        <tr>
            <td class="label"><fmt:message key="databaseEntry.title" /></td>
            <td><Gemma:databaseEntry databaseEntryValueObject="${bioAssay.accession}" /></td>
        </tr>

        <tr>
            <td class="label"><strong> <fmt:message key="bioAssay.description" />
            </strong></td>
            <td>${fn:escapeXml(bioAssay.description)}</td>
        </tr>

        <tr>
            <td class="label"><strong>Sample</strong></td>
            <td>
                <a href="${pageContext.request.contextPath}/bioMaterial/showBioMaterial.html?id=${bioAssay.sample.id}">${bioAssay.sample.name}</a>
            </td>
        </tr>
        <tr>
            <td class="label"><strong>Platform</strong></td>
            <td>
                <a href="${pageContext.request.contextPath}/arrays/showArrayDesign.html?id=${bioAssay.arrayDesign.id}">${bioAssay.arrayDesign.shortName}</a>&nbsp${bioAssay.arrayDesign.name}
            </td>
        </tr>

        <c:if test="${bioAssay.originalPlatform != null}">
            <tr>
                <td class="label"><strong>Original Platform</strong></td>
                <td>
                    <a href="${pageContext.request.contextPath}/arrays/showArrayDesign.html?id=${bioAssay.originalPlatform.id}">${bioAssay.originalPlatform.shortName}</a>&nbsp;${bioAssay.originalPlatform.name}
                </td>
            </tr>
        </c:if>

    </table>

</div>