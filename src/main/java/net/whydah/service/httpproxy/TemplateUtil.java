package net.whydah.service.httpproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * https://github.com/Cantara/HTTPLoadTest-Baseline/blob/master/src/main/java/no/cantara/service/loadtest/util/TemplateUtil.java
 */
public class TemplateUtil {

    private static final Logger log = LoggerFactory.getLogger(TemplateUtil.class);

    interface FizzleFunction {
        String apply(String parameters, String input);
    }

    private static final Map<String, FizzleFunction> fizzleFunctionByKey = new LinkedHashMap<>();

    static {
        // Use only lowercase keys in order to support case-insensitive matching
        fizzleFunctionByKey.put("chars", (parameters, input) -> Fizzler.getRandomCharacters(input.length()));
        fizzleFunctionByKey.put("digits", (parameters, input) -> Fizzler.getRandomDigits(input.length()));
        fizzleFunctionByKey.put("u_chars", (parameters, input) -> Fizzler.getRandomUppercaseCharacter(input.length()));
        fizzleFunctionByKey.put("l_chars", (parameters, input) -> Fizzler.getRandomLowercaseCharacter(input.length()));
        fizzleFunctionByKey.put("hex", (parameters, input) -> Fizzler.getRandomHEXCharacter(input.length()));
        fizzleFunctionByKey.put("option", (parameters, input) -> Fizzler.getRandomSetValue(input));
        fizzleFunctionByKey.put("optionvalue", (parameters, input) -> Fizzler.getRandomSetValueAsString(input));
        fizzleFunctionByKey.put("substring", (parameters, input) -> Fizzler.getSubString(parameters, input));
        fizzleFunctionByKey.put("timestamp", (parameters, input) -> Fizzler.getTimestamp(input));
    }

    static final Pattern variablePattern = Pattern.compile("#\\(?([\\p{Alnum}_]+)\\)?");

    final Map<String, String> templatereplacementMap;
    final Map<String, Expression> expressionByKey;

    public TemplateUtil(Map<String, String> templatereplacementMap) {
        this.templatereplacementMap = templatereplacementMap;

        // prepare build expression-map, no resolution or evaluation done here
        this.expressionByKey = new LinkedHashMap<>();
        if (templatereplacementMap != null) {
            for (Map.Entry<String, String> e : templatereplacementMap.entrySet()) {
                Matcher m = variablePattern.matcher(e.getKey());
                if (m.matches()) {
                    String variableIdentifier = m.group(1).toLowerCase();
                    expressionByKey.put(variableIdentifier, new Expression(e.getValue()));
                } else {
                    log.warn("template-replacement-map contains key not on #variable form: {}", e.getKey());
                }
            }
        }
    }

    public String updateTemplateWithValuesFromMap(String template) {
        if (template == null) {
            return "";
        }

        // resolve expressions in template recursively and lazily
        String result = new Expression(template).resolve();

        return result;
    }

    static final Pattern fizzleFunctionPattern =
            Pattern.compile("#[Ff][Ii][Zz][Zz][Ll][Ee]\\(([^():]+)(?:\\(([^)]*)\\))?:?([^)]*)\\)");

    class Expression {

        final String template;
        String resolvedExpression;

        Expression(String template) {
            this.template = template;
        }

        String resolve() {
            if (resolvedExpression != null) {
                return resolvedExpression;
            }

            resolvedExpression = "$$circular$$reference$$protection$$";

            resolvedExpression = resolveWithFizzlePattern(template);
            resolvedExpression = resolveWithVariablePattern(resolvedExpression);

            return resolvedExpression;
        }

        private String resolveWithFizzlePattern(String template) {
            StringBuilder result = new StringBuilder();
            Matcher replaceableExpressionsInTemplateMatcher = fizzleFunctionPattern.matcher(template);
            int previousEnd = 0;
            while (replaceableExpressionsInTemplateMatcher.find()) {
                result.append(template, previousEnd, replaceableExpressionsInTemplateMatcher.start());
                if (replaceableExpressionsInTemplateMatcher.group(1) != null) {
                    String fizzleFunctionKey = replaceableExpressionsInTemplateMatcher.group(1).toLowerCase();
                    String fizzleFunctionArguments = replaceableExpressionsInTemplateMatcher.group(2);
                    String fizzleInput = replaceableExpressionsInTemplateMatcher.group(3);
                    String resolvedFizzleInput = new Expression(fizzleInput).resolve();
                    FizzleFunction function = fizzleFunctionByKey.get(fizzleFunctionKey);
                    if (function == null) {
                        log.warn("#Fizzle function does not exist: {}", fizzleFunctionKey);
                        result.append(resolvedFizzleInput);
                    } else {
                        String fizzleOutput = function.apply(fizzleFunctionArguments, resolvedFizzleInput);
                        result.append(fizzleOutput);
                    }
                }
                previousEnd = replaceableExpressionsInTemplateMatcher.end();
            }
            result.append(template.substring(previousEnd)); // tail

            return result.toString();
        }

        private String resolveWithVariablePattern(String template) {
            StringBuilder result = new StringBuilder();
            Matcher replaceableExpressionsInTemplateMatcher = variablePattern.matcher(template);
            int previousEnd = 0;
            while (replaceableExpressionsInTemplateMatcher.find()) {
                result.append(template, previousEnd, replaceableExpressionsInTemplateMatcher.start());
                if (replaceableExpressionsInTemplateMatcher.group(1) != null) {
                    String variableIdentifier = replaceableExpressionsInTemplateMatcher.group(1).toLowerCase();
                    Expression expression = expressionByKey.get(variableIdentifier);
                    if (expression == null) {
                        log.warn("Unable to resolve template variable #{}", variableIdentifier);
                        result.append(expression);
                    } else {
                        String resolvedExpression = expression.resolve();
                        result.append(resolvedExpression);
                        log.trace("Replaced #{} with: {}", variableIdentifier, resolvedExpression);
                    }
                }
                previousEnd = replaceableExpressionsInTemplateMatcher.end();
            }
            result.append(template.substring(previousEnd)); // tail

            return result.toString();
        }
    }

    public static String readFile(String fileName) {
        ClassLoader classLoader = TemplateUtil.class.getClassLoader();
        File file = new File(classLoader.getResource(fileName).getFile());
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String readFile(File file) {
        try {
            return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param specification        The {@link ProxySpecification} that will be cloned
     * @param applicationTokenId   will be added to the replacement map for the key #applicationTokenId
     * @param userTokenId          will be added to the replacement map for the key #userTokenId
     * @param logonurl             will be added to the replacement map for the key #logonservice
     * @param securitytokenservice will be added to the replacement map for the key #securitytokenservice
     * @return a clone of specification with the two replacement entries added
     */
    static ProxySpecification getCloneWithReplacements(ProxySpecification specification, String applicationTokenId,
                                                       String userTokenId, String logonurl, String securitytokenservice) throws CloneNotSupportedException {
        ProxySpecification clone = specification.clone();
        clone.addEntryToCommand_replacement_map("#applicationTokenId", applicationTokenId);
        clone.addEntryToCommand_replacement_map("#userTokenId", userTokenId);
        clone.addEntryToCommand_replacement_map("#logonservice", logonurl);
        clone.addEntryToCommand_replacement_map("#securitytokenservice", securitytokenservice);
        return clone;
    }
}
