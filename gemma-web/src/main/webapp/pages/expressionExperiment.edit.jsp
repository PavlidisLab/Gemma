<%@ include file="/common/taglibs.jsp" %>

<jsp:useBean id="expressionExperiment" scope="request"
        class="ubic.gemma.web.controller.expression.experiment.ExpressionExperimentEditValueObject" />

<head>
<title>Edit ${expressionExperiment.shortName}</title>
<Gemma:script src='/scripts/app/bioassay.draganddrop.js'/>
</head>

<spring:bind path="expressionExperiment.*">
    <c:if test="${not empty status.errorMessages}">
        <div class="error">
            <c:forEach var="error" items="${status.errorMessages}">
                <i class="red fa fa-warning fa-lg fa-fw"></i>
                <c:out value="${error}" escapeXml="false" />
                <br />
            </c:forEach>
        </div>
    </c:if>
</spring:bind>

<title><fmt:message key="expressionExperiment.title" /> ${expressionExperiment.shortName}</title>

<div class="padded">
    <form method="post" action="<c:url value="/expressionExperiment/editExpressionExperiment.html"/>">
        <h2>
            Editing: <a
                href="<c:url value="/expressionExperiment/showExpressionExperiment.html?id=${expressionExperiment.id}" />">${expressionExperiment.shortName}</a>
        </h2>
        <h3>Quantitation Types</h3>
        <c:choose>
            <c:when test='<%=expressionExperiment.getQuantitationTypes().isEmpty()%>'>
                No quantitation types! Data may be corrupted (likely data import error)
            </c:when>
            <c:otherwise>
                <table class="detail">
                    <tr>
                        <th>Name</th>
                        <th>Desc</th>
                        <th>Pref?</th>
                        <th>Recom?</th>
                        <th>Batch?</th>
                        <th>Ratio?</th>
                        <th>Bkg?</th>
                        <th>BkgSub?</th>
                        <th>Norm?</th>
                        <th>Type</th>
                        <th>Spec.Type</th>
                        <th>Scale</th>
                        <th>Rep.</th>
                    </tr>

                    <c:forEach var="index" begin="0" end="<%=expressionExperiment.getQuantitationTypes().size() - 1%>"
                            step="1">
                        <spring:nestedPath path="expressionExperiment.quantitationTypes[${index}]">
                            <tr>
                                <td><spring:bind path="name">
                                    <input type="text" size="20" name="<c:out value="${status.expression}"/>"
                                            value="<c:out value="${status.value}"/>" />
                                </spring:bind></td>
                                <td><spring:bind path="description">
                                    <input type="text" size="35" name="<c:out value="${status.expression}"/>"
                                            value="<c:out value="${status.value}"/>" />
                                </spring:bind></td>
                                <td><spring:bind path="isPreferred">
                                    <input id="preferredCheckbox" type="checkbox" name="${status.expression}"
                                            <c:if test="${status.value == true}">checked="checked"</c:if> />
                                    <input type="hidden" name="_<c:out value="${status.expression}"/>">
                                </spring:bind></td>
                                <td><spring:bind path="isRecomputedFromRawData">
                                    <input id="preferredCheckbox" type="checkbox" name="${status.expression}"
                                            <c:if test="${status.value == true}">checked="checked"</c:if> />
                                    <input type="hidden" name="_<c:out value="${status.expression}"/>">
                                </spring:bind></td>
                                <td><spring:bind path="isBatchCorrected">
                                    <input id="preferredCheckbox" type="checkbox" name="${status.expression}"
                                            <c:if test="${status.value == true}">checked="checked"</c:if> />
                                    <input type="hidden" name="_<c:out value="${status.expression}"/>">
                                </spring:bind></td>
                                <td><spring:bind path="isRatio">
                                    <input id="ratioCheckbox" type="checkbox" name="${status.expression}"
                                            <c:if test="${status.value == true}">checked="checked"</c:if> />
                                    <input type="hidden" name="_<c:out value="${status.expression}"/>">
                                </spring:bind></td>
                                <td><spring:bind path="isBackground">
                                    <input id="backgroundCheckbox" type="checkbox" name="${status.expression}"
                                            <c:if test="${status.value == true}">checked="checked"</c:if> />
                                    <input type="hidden" name="_<c:out value="${status.expression}"/>">
                                </spring:bind></td>
                                <td><spring:bind path="isBackgroundSubtracted">
                                    <input id="bkgsubCheckbox" type="checkbox" name="${status.expression}"
                                            <c:if test="${status.value == true}">checked="checked"</c:if> />
                                    <input type="hidden" name="_<c:out value="${status.expression}"/>">
                                </spring:bind></td>
                                <td><spring:bind path="isNormalized">
                                    <input id="normCheckbox" type="checkbox" name="${status.expression}"
                                            <c:if test="${status.value == true}">checked="checked"</c:if> />
                                    <input type="hidden" name="_<c:out value="${status.expression}"/>">
                                </spring:bind></td>
                                <td><spring:bind path="generalType">
                                    <select name="${status.expression}">
                                        <c:forEach items="${generalQuantitationTypes}" var="type">
                                            <option value="${type}"
                                                    <c:if test="${status.value == type}">selected</c:if>>${type}</option>
                                        </c:forEach>
                                    </select>
                                    <span class="fieldError">${status.errorMessage}</span>
                                </spring:bind></td>
                                <td><spring:bind path="type">
                                    <select name="${status.expression}">
                                        <c:forEach items="${standardQuantitationTypes}" var="type">
                                            <option value="${type}"
                                                    <c:if test="${status.value == type}">selected</c:if>>${type}</option>
                                        </c:forEach>
                                    </select>
                                    <span class="fieldError">${status.errorMessage}</span>
                                </spring:bind></td>
                                <td><spring:bind path="scale">
                                    <select name="${status.expression}">
                                        <c:forEach items="${scaleTypes}" var="type">
                                            <option value="${type}"
                                                    <c:if test="${status.value == type}">selected</c:if>>${type}</option>
                                        </c:forEach>
                                    </select>
                                    <span class="fieldError">${status.errorMessage}</span>
                                </spring:bind></td>
                                <td><spring:bind path="representation">
                                    <select name="${status.expression}">
                                        <c:forEach items="${representations}" var="type">
                                            <option value="${type}"
                                                    <c:if test="${status.value == type}">selected</c:if>>${type}</option>
                                        </c:forEach>
                                    </select>
                                    <span class="fieldError">${status.errorMessage}</span>
                                </spring:bind></td>
                            </tr>
                        </spring:nestedPath>
                    </c:forEach>

                </table>
            </c:otherwise>
        </c:choose>

        <hr class="normal">

        <h3>Biomaterials and Assays</h3>

        <div class="v-padded">
            <a
                    href="<c:url value="/expressionExperiment/showBioAssaysFromExpressionExperiment.html?id=${expressionExperiment.id}"/>">
                Click for details and QC</a>
        </div>

        <div class="v-padded">
            <input type="button" onClick="Ext.getCmp('eemanager').unmatchBioAssays(${expressionExperiment.id})"
                    value="Unmatch all bioassays" />
        </div>


        <Gemma:assayView bioAssays="${expressionExperiment.bioAssays}" edit="true"/>

        <div class="v-padded">
            <input type="submit" class="button" name="save" value="<fmt:message key="button.save"/>" />
            <input type="submit" class="button" name="cancel" value="<fmt:message key="button.cancel"/>" />
        </div>

    </form>

</div>