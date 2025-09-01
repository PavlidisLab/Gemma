<%@ include file="/common/taglibs.jsp" %>

<title>${fn:escapeXml(term.label)}</title>

<div class="padded" style="padding-bottom: 0.5em;">
    <h1>The Gemma Ontology</h1>
    <p><a href="${pageContext.request.contextPath}/ont/TGEMO">All Terms</a></p>
    <h2 style="padding-bottom: 0.5em">${fn:escapeXml(term.label)}</h2>
    <c:forEach var="annotation" items="${term.annotations}">
        <c:if test="${annotation.property == 'hasDefinition' || annotation.property == 'definition'}">
            <p>${fn:escapeXml(annotation.contents)}</p>
        </c:if>
    </c:forEach>
    <p><b>IRI:</b> ${term.uri}</p>
    <Gemma:ontologyHierarchy term="${term}" cssClass="mb-3" />
    <p style="margin-bottom: 0"> Other ontologies used by Gemma:</p>
    <ul class="list-styled">
        <li><a href="${pageContext.request.contextPath}/ont/TGFVO">The Gemma Factor Value Ontology</a></li>
    </ul>
</div>
