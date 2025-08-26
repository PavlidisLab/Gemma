<%@ include file="/common/taglibs.jsp" %>
<%--@elvariable id="bioAssaySet" type="ubic.gemma.model.expression.experiment.BioAssaySet"--%>
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