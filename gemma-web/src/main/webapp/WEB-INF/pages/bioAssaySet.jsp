<%@ include file="/WEB-INF/common/taglibs.jsp" %>
<jsp:useBean id="bioAssaySet" scope="request" type="ubic.gemma.model.expression.experiment.BioAssaySet" />
<title>Assays for ${bioAssaySet.name}</title>

<h2>Assays for ${bioAssaySet.name}</h2>

<table>
    <c:forEach items="${bioAssaySet.bioAssays}" var="bioAssay">
        <tr>
            <td>
                <a href="${pageContext.request.contextPath}/bioAssay/showBioAssay.html?id=${bioAssay.id}">${bioAssay.name}
            </td>
        </tr>
    </c:forEach>
</table>