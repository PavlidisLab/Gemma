<%@ include file="/common/taglibs.jsp" %>

<title>Annotation #${annotationId} of FactorValue #${factorValueId}: ${fn:escapeXml(oi.label)}</title>

<div class="padded">
    <h2>The Gemma Factor Value Ontology</h2>
    <p><a href="${pageContext.request.contextPath}/ont/TGFVO">All Terms</a></p>
    <h3>Annotation #${annotationId} of FactorValue #${factorValueId}: <Gemma:ontologyResource
            resource="${factorValueOi}" /></h3>
    <p><b>IRI:</b> ${oi.uri}</p>
    <ul class="mb-3">
        <c:if test="${ee != null}">
            <li>belongs to <Gemma:entityLink entity="${ee}">${fn:escapeXml(ee.shortName)}</Gemma:entityLink></li>
        </c:if>
        <c:if test="${factorValueOi != null}">
            <li>annotation of <Gemma:ontologyResource resource="${factorValueOi}" /></li>
        </c:if>
        <c:if test="${oi.instanceOf != null}">
            <li><Gemma:ontologyResource resource="${oi.instanceOf}" /></li>
        </c:if>
    </ul>
    <div class="mb-3">
        <p style="margin-bottom: 0">Retrieve this in RDF/XML:</p>
        <pre>curl -H Accept:application/rdf+xml ${fn:escapeXml(hostUrl)}/ont/TGFVO/${factorValueId}/${annotationId}</pre>
    </div>
    <p style="margin-bottom: 0"> Other ontologies used by Gemma:</p>
    <ul>
        <li><a href="${pageContext.request.contextPath}/ont/TGEMO">Temporary Gemma Ontology</a></li>
    </ul>
</div>