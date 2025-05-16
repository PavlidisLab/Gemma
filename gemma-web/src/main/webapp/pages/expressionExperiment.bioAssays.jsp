<%@ include file="/common/taglibs.jsp" %>
<jsp:useBean id="expressionExperiment" scope="request"
        type="ubic.gemma.model.expression.experiment.ExpressionExperiment" />
<head>
<title>
    <fmt:message key="bioAssays.title" /> from ${expressionExperiment.shortName}
    - ${fn:escapeXml(expressionExperiment.name)}
    <c:if test="${platform != null}">
        in ${platform.shortName} - ${fn:escapeXml(platform.name)}
    </c:if>
</title>
<meta name="description" content="${fn:escapeXml(expressionExperiment.description)}" />
<meta name="keywords" content="${fn:escapeXml(keywords)}" />
</head>

<div class="padded">
    <h2>
        <fmt:message key="bioAssays.title" />
        from
        <Gemma:entityLink entity="${expressionExperiment}">
            ${fn:escapeXml(expressionExperiment.shortName)}
        </Gemma:entityLink>
        - ${fn:escapeXml(expressionExperiment.name)}
        <c:if test="${platform != null}">
            in <Gemma:entityLink entity="${platform}">${fn:escapeXml(platform.shortName)}</Gemma:entityLink>
            - ${fn:escapeXml(platform.name)}
        </c:if>
    </h2>
    <c:if test="${platform != null}">
        <p>
            <a href="${pageContext.request.contextPath}/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=${expressionExperiment.id}">
                Show all assays
            </a>
        </p>
    </c:if>
    <p>
        <a href="${pageContext.request.contextPath}/experimentalDesign/showExperimentalDesign.html?eeid=${expressionExperiment.id}">
            View the experimental design
        </a>
    </p>
    <c:choose>
        <c:when test="${platform != null}">
            <table>
                <tr>
                    <th>Name</th>
                    <th>Description</th>
                </tr>
                <c:forEach items="${bioAssays}" var="bioAssay">
                    <tr>
                        <td>
                            <Gemma:entityLink entity="${bioAssay}">
                                ${fn:escapeXml(bioAssay.name)}
                            </Gemma:entityLink>
                        </td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty bioAssay.description}">
                                    ${fn:escapeXml(bioAssay.description)}
                                </c:when>
                                <c:otherwise>
                                    <i>No description available</i>
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                </c:forEach>
            </table>
        </c:when>
        <c:otherwise>
            <div class="mb-3">
                <Gemma:expressionQC expressionExperiment="${expressionExperiment}"
                        eeManagerId="eemanager"
                        hasCorrMat="${hasCorrMat}"
                        hasPCA="${hasPCA}"
                        hasMeanVariance="${hasMeanVariance}"
                        hasSingleCellData="${hasSingleCellData}"
                        singleCellSparsityHeatmap="${singleCellSparsityHeatmap}"
                        numFactors="${numFactors}"
                        numOutliersRemoved="${numOutliersRemoved}"
                        numPossibleOutliers="${numPossibleOutliers}"
                        class="mb-3" />
            </div>
            <security:authorize access="hasAuthority('GROUP_ADMIN') || hasPermission(expressionExperiment, 'WRITE')">
                <div id="bioAssayTable" class="v-padded mb-3"></div>
                <input id="eeId" type="hidden" value="${expressionExperiment.id}" />
                <script type="text/javascript">
                Ext.BLANK_IMAGE_URL = '${pageContext.request.contextPath}/images/default/s.gif';

                Ext.onReady( function() {

                   Ext.QuickTips.init();
                   Ext.state.Manager.setProvider( new Ext.state.CookieProvider() );

                   var manager = new Gemma.EEManager( {editable : true, id : "eemanager"} );

                   manager.on( 'done', function() {
                      window.location.reload( true );
                   } );

                   new Gemma.BioAssayGrid( {
                      editable : true,
                      renderTo : 'bioAssayTable',
                      eeId : Ext.get( "eeId" ).getValue()
                   } );

                } );
                </script>
            </security:authorize>
        </c:otherwise>
    </c:choose>
</div>
