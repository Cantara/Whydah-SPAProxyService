package net.whydah.service.httpproxy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProxySpecification implements Serializable, Cloneable {

    private static final Map<String, String> templateByFileReference = new ConcurrentHashMap<>(200);

    private String command_url;
    private String command_contenttype = "application/json";
    private String command_http_authstring;
    private boolean command_http_post = false;
    private boolean command_use_hystrix = true;
    private int command_timeout_milliseconds = 2000;
    private String command_template = "";
    private Map<String, String> command_replacement_map;
    private Map<String, String> command_response_map = new LinkedHashMap<>();
    @JsonIgnore private transient TemplateUtil templateUtil;


    private boolean isTemplate = true;

    private static final Logger log = LoggerFactory.getLogger(ProxySpecification.class);

    public ProxySpecification(@JsonProperty("command_url") String command_url,
                              @JsonProperty("command_contenttype") String command_contenttype,
                              @JsonProperty("command_http_authstring") String command_http_authstring,
                              @JsonProperty("command_http_post") String command_http_post,
                              @JsonProperty("command_use_hystrix") String command_use_hystrix,
                              @JsonProperty("command_timeout_milliseconds") String command_timeout_milliseconds,
                              @JsonProperty("command_template") String command_template,
                              @JsonProperty("command_replacement_map") Map<String, String> command_replacement_map,
                              @JsonProperty("command_response_map") Map<String, String> command_response_map) {
        this.command_url = command_url;
        this.command_http_post = Boolean.parseBoolean(command_http_post);
        this.command_use_hystrix = Boolean.parseBoolean(command_use_hystrix);
        this.command_http_authstring = command_http_authstring;
        this.command_timeout_milliseconds = Integer.parseInt(command_timeout_milliseconds);
        this.command_contenttype = command_contenttype;
        this.command_template = command_template;
        this.command_replacement_map = command_replacement_map;

    }

    public ProxySpecification() {
    }

    public ProxySpecification clone() throws CloneNotSupportedException {
        ProxySpecification cloneSpecification = (ProxySpecification) super.clone();
        cloneSpecification.isTemplate = false;
        return cloneSpecification;

    }

    public String getCommand_url() {
        if (!isTemplate && command_url != null && command_url.contains("#")) {
            setCommand_url(getTemplateUtil().updateTemplateWithValuesFromMap(command_url));
        }
        return command_url;
    }

    public void setCommand_url(String command_url) {
        this.command_url = command_url;
    }

    public String getCommand_contenttype() {
        return command_contenttype;
    }

    public void setCommand_contenttype(String command_contenttype) {
        this.command_contenttype = command_contenttype;
    }

    public String getCommand_template() {
        return command_template;
    }

    public void setCommand_template(String command_template) {
        this.command_template = command_template;
    }

    public Map<String, String> getCommand_replacement_map() {
        if (command_replacement_map == null) {
            command_replacement_map = new HashMap<>();
        }

        return command_replacement_map;
    }

    public void setCommand_replacement_map(Map<String, String> command_replacement_map) {
        addMapToCommand_replacement_map(command_replacement_map);
    }

    public void addMapToCommand_replacement_map(Map<String, String> map_to_add) {
        //map_to_add.forEach(this.command_replacement_map::putIfAbsent);
        this.command_replacement_map.putAll(map_to_add);
        //this.command_replacement_map.putAll(Maps.difference(command_replacement_map, map_to_add).entriesOnlyOnLeft());
    }

    public void addEntryToCommand_replacement_map(String key, String value) {
        //map_to_add.forEach(this.command_replacement_map::putIfAbsent);
        this.command_replacement_map.put(key, value);
        //this.command_replacement_map.putAll(Maps.difference(command_replacement_map, map_to_add).entriesOnlyOnLeft());
    }


    public boolean isCommand_http_post() {
        return command_http_post;
    }

    public void setCommand_http_post(boolean command_http_post) {
        this.command_http_post = command_http_post;
    }

    public boolean isCommand_use_hystrix() {
        return command_use_hystrix;
    }

    public void setCommand_use_hystrix(boolean command_use_hystrix) {
        this.command_use_hystrix = command_use_hystrix;
    }

    public int getCommand_timeout_milliseconds() {
        return command_timeout_milliseconds;
    }

    public void setCommand_timeout_milliseconds(int command_timeout_milliseconds) {
        this.command_timeout_milliseconds = command_timeout_milliseconds;
    }

    public String getCommand_http_authstring() {
        if (isTemplate) {
            return command_http_authstring;
        }

        if (command_http_authstring == null || command_http_authstring.length() < 1) {
            return null;
        }

        if (command_http_authstring.startsWith("X-AUTH")) {
            return command_http_authstring;
        }
        if (command_http_authstring != null && !command_http_authstring.contains("Bearer") && command_http_authstring.split("/").length == 2) {
            String[] upfields = command_http_authstring.split("/");
            String name = upfields[0];
            String password = upfields[1];

            String authString = name + ":" + password;
            log.info("auth string: " + authString);
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
//            String authStringEnc = new String(authEncBytes);
            String authStringEnc = "Basic " + new String(authEncBytes);
            return authStringEnc;

        }
        if (command_http_authstring != null && !command_http_authstring.contains("Bearer") && command_http_authstring.split(":").length == 2) {
            String[] upfields = command_http_authstring.split(":");
            String name = upfields[0];
            String password = upfields[1];

            String authString = name + ":" + password;
            log.info("auth string: " + authString);
            byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
            String authStringEnc = "Basic " + new String(authEncBytes);
            return authStringEnc;

        }
        if (command_http_authstring.contains("Bearer")) {
            String test = "\\[";
            command_http_authstring = command_http_authstring.replaceAll("]", "").replaceAll("\"", "").replaceAll(test, "").replaceAll("  ", " ");
        }

        return command_http_authstring;
    }

    public void setCommand_http_authstring(String command_http_authstring) {
        this.command_http_authstring = command_http_authstring;
    }


    public Map<String, String> getCommand_response_map() {
        if (command_response_map == null) {
            command_response_map = new HashMap<>();
        }
        return command_response_map;
    }

    public void setCommand_response_map(Map<String, String> command_response_map) {
        this.command_response_map = command_response_map;
    }

    public void resolveVariables(Map<String, String> globalMap, Map<String, String> inheritedVariables, Map<String, String> resolvedResultVariables) {
        if (isTemplate) {
            return;
        }
        if (globalMap != null) {
            addMapToCommand_replacement_map(globalMap);
        }
        if (inheritedVariables != null) {
            addMapToCommand_replacement_map(inheritedVariables);
        }
        if (resolvedResultVariables != null) {
            addMapToCommand_replacement_map(resolvedResultVariables);
        }
        Map<String, String> command_replacement_map = getCommand_replacement_map();
        log.trace("resolveVariables - Active variables: {}", command_replacement_map);
        setCommand_url(getTemplateUtil().updateTemplateWithValuesFromMap(getCommand_url()));
        log.trace("resolveVariables -Updated commandURL: {}", getCommand_url());
        setCommand_http_authstring(getTemplateUtil().updateTemplateWithValuesFromMap(getCommand_http_authstring()));
        log.trace("resolveVariables - Updated command_http_authstring: {}", getCommand_http_authstring());
        setCommand_template(getTemplateUtil().updateTemplateWithValuesFromMap(getCommand_template()));
        log.trace("resolveVariables - Updated command_template: {}", getCommand_template());

    }

    @Override
    public String toString() {
        return "TestSpecification{" +
                "command_url='" + command_url + '\'' +
                ", command_http_post=" + command_http_post +
                '}';
    }

    public String toLongString() {
        return "TestSpecification{" +
                "command_url='" + command_url + '\'' +
                ", command_contenttype='" + command_contenttype + '\'' +
                ", command_http_authstring='" + command_http_authstring + '\'' +
                ", command_http_post=" + command_http_post +
                ", command_timeout_milliseconds=" + command_timeout_milliseconds +
                ", command_template='" + command_template + '\'' +
                ", command_replacement_map=" + command_replacement_map +
                ", command_response_map=" + command_response_map +
                '}';
    }

    public TemplateUtil getTemplateUtil() {
        if (templateUtil == null) {
            templateUtil = new TemplateUtil(command_replacement_map);
        }
        return templateUtil;
    }
}