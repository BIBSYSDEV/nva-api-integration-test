<#ftl output_format="HTML">
<#-- @ftlvariable name="data" type="io.qameta.allure.attachment.http.HttpResponseAttachment" -->

<#assign sensitiveHeaders = ["authorization", "cookie", "set-cookie", "proxy-authorization"]>

<#function redactSigV4 value>
    <#return value?replace("([?&](?:X-Amz-Signature|X-Amz-Security-Token|X-Amz-Credential)=)[^&\\s\"']+", "$1[REDACTED]", "r")>
</#function>

<div>Status code <#if data.responseCode??>${data.responseCode} <#else>Unknown</#if></div>
<#if data.url??><div>${redactSigV4(data.url)}</div></#if>

<#if data.body??>
<h4>Body</h4>
<div>
    <pre class="preformated-text">
    <#t>${redactSigV4(data.body)}
    </pre>
</div>
</#if>

<#if (data.headers)?has_content>
<h4>Headers</h4>
<div>
    <#list data.headers as name, value>
        <#if sensitiveHeaders?seq_contains(name?lower_case)>
        <div>${name}: [REDACTED]</div>
        <#else>
        <div>${name}: ${redactSigV4(value!"null")}</div>
        </#if>
    </#list>
</div>
</#if>


<#if (data.cookies)?has_content>
<h4>Cookies</h4>
<div>
    <#list data.cookies as name, value>
        <div>${name}: [REDACTED]</div>
    </#list>
</div>
</#if>
