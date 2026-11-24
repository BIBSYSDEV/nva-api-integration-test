<#ftl output_format="HTML">
<#-- @ftlvariable name="data" type="io.qameta.allure.attachment.http.HttpRequestAttachment" -->

<#assign sensitiveHeaders = ["authorization", "cookie", "set-cookie", "proxy-authorization"]>

<#function redactSigV4 value>
    <#return value?replace("([?&](?:X-Amz-Signature|X-Amz-Security-Token|X-Amz-Credential)=)[^&\\s\"']+", "$1[REDACTED]", "r")>
</#function>

<#-- A national identity number is eleven digits, and is the only value these tests send as such. -->
<#function redactNin value>
    <#return value?replace("(?<![0-9])[0-9]{11}(?![0-9])", "[REDACTED]", "r")>
</#function>

<#function redactCurl curl>
    <#return redactNin(redactSigV4(curl
        ?replace("-H '[Aa]uthorization:[^']*'", "-H 'Authorization: [REDACTED]'", "r")
        ?replace("-H '[Cc]ookie:[^']*'", "-H 'Cookie: [REDACTED]'", "r")))>
</#function>

<div><#if data.method??>${data.method}<#else>GET</#if> to <#if data.url??>${redactSigV4(data.url)}<#else>Unknown</#if></div>

<#if data.body??>
<h4>Body</h4>
<div>
    <pre class="preformated-text">
    <#t>${redactNin(redactSigV4(data.body))}
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

<#if data.curl??>
<h4>Curl</h4>
<div>
${redactCurl(data.curl)}
</div>
</#if>
