package io.swagger.codegen.v3.generators.python;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public class PythonFlaskConnexionCodegen extends DefaultCodegenConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PythonFlaskConnexionCodegen.class);

    public static final String CONTROLLER_PACKAGE = "controllerPackage";
    public static final String DEFAULT_CONTROLLER = "defaultController";
    public static final String SUPPORT_PYTHON2= "supportPython2";

    protected int serverPort = 8080;
    protected String packageName;
    protected String packageVersion;
    protected String controllerPackage;
    protected String defaultController;
    protected Map<Character, String> regexModifiers;

    public PythonFlaskConnexionCodegen() {
        super();
        modelPackage = "models";
        testPackage = "test";

        languageSpecificPrimitives.clear();
        languageSpecificPrimitives.add("int");
        languageSpecificPrimitives.add("float");
        languageSpecificPrimitives.add("List");
        languageSpecificPrimitives.add("Dict");
        languageSpecificPrimitives.add("bool");
        languageSpecificPrimitives.add("str");
        languageSpecificPrimitives.add("datetime");
        languageSpecificPrimitives.add("date");
        languageSpecificPrimitives.add("file");
        languageSpecificPrimitives.add("object");
        languageSpecificPrimitives.add("byte");
        languageSpecificPrimitives.add("bytearray");
        languageSpecificPrimitives.add("binary_type");

        typeMapping.clear();
        typeMapping.put("integer", "int");
        typeMapping.put("float", "float");
        typeMapping.put("number", "float");
        typeMapping.put("BigDecimal", "float");
        typeMapping.put("long", "int");
        typeMapping.put("double", "float");
        typeMapping.put("array", "List");
        typeMapping.put("map", "Dict");
        typeMapping.put("boolean", "bool");
        typeMapping.put("string", "str");
        typeMapping.put("date", "date");
        typeMapping.put("DateTime", "datetime");
        typeMapping.put("object", "object");
        typeMapping.put("file", "file");
        typeMapping.put("UUID", "str");
        typeMapping.put("binary", "str");
        typeMapping.put("byte", "bytearray");
        typeMapping.put("ByteArray", "bytearray");

        // from https://docs.python.org/3/reference/lexical_analysis.html#keywords
        setReservedWordsLowerCase(
                Arrays.asList(
                        // @property
                        "property",
                        // python reserved words
                        "and", "del", "from", "not", "while", "as", "elif", "global", "or", "with",
                        "assert", "else", "if", "pass", "yield", "break", "except", "import",
                        "print", "class", "exec", "in", "raise", "continue", "finally", "is",
                        "return", "def", "for", "lambda", "try", "self", "None", "True", "False", "nonlocal",
                        "float", "int", "str", "date", "datetime", "False", "await", "async"));

        // set the output folder here
        outputFolder = "generated-code/connexion";

        apiTemplateFiles.put("controller.mustache", ".py");
        modelTemplateFiles.put("model.mustache", ".py");
        apiTestTemplateFiles().put("controller_test.mustache", ".py");

        /*
         * Additional Properties.  These values can be passed to the templates and
         * are available in models, apis, and supporting files
         */
        additionalProperties.put("serverPort", serverPort);

        /*
         * Supporting Files.  You can write single files for the generator with the
         * entire object tree available.  If the input file has a suffix of `.mustache
         * it will be processed by the template engine.  Otherwise, it will be copied
         */
        supportingFiles.add(new SupportingFile("README.mustache", "", "README.md"));
        supportingFiles.add(new SupportingFile("setup.mustache", "", "setup.py"));
        supportingFiles.add(new SupportingFile("tox.mustache", "", "tox.ini"));
        supportingFiles.add(new SupportingFile("test-requirements.mustache", "", "test-requirements.txt"));
        supportingFiles.add(new SupportingFile("requirements.mustache", "", "requirements.txt"));
        supportingFiles.add(new SupportingFile("git_push.sh.mustache", "", "git_push.sh"));
        supportingFiles.add(new SupportingFile("gitignore.mustache", "", ".gitignore"));
        supportingFiles.add(new SupportingFile("travis.mustache", "", ".travis.yml"));
        supportingFiles.add(new SupportingFile("Dockerfile.mustache", "", "Dockerfile"));
        supportingFiles.add(new SupportingFile("dockerignore.mustache", "", ".dockerignore"));

        regexModifiers = new HashMap<Character, String>();
        regexModifiers.put('i', "IGNORECASE");
        regexModifiers.put('l', "LOCALE");
        regexModifiers.put('m', "MULTILINE");
        regexModifiers.put('s', "DOTALL");
        regexModifiers.put('u', "UNICODE");
        regexModifiers.put('x', "VERBOSE");

        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_NAME, "python package name (convention: snake_case).")
                .defaultValue("swagger_server"));
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_VERSION, "python package version.")
                .defaultValue("1.0.0"));
        cliOptions.add(new CliOption(CONTROLLER_PACKAGE, "controller package").
                defaultValue("controllers"));
        cliOptions.add(new CliOption(DEFAULT_CONTROLLER, "default controller").
                defaultValue("default_controller"));
        cliOptions.add(new CliOption(SUPPORT_PYTHON2, "support python2").
                defaultValue("false"));
        cliOptions.add(new CliOption("serverPort", "TCP port to listen to in app.run").
                defaultValue("8080"));
    }

    @Override
    public void processOpts() {
        super.processOpts();

        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_NAME)) {
            setPackageName((String) additionalProperties.get(CodegenConstants.PACKAGE_NAME));
        } else {
            setPackageName("swagger_server");
            additionalProperties.put(CodegenConstants.PACKAGE_NAME, this.packageName);
        }
        if (additionalProperties.containsKey(CodegenConstants.PACKAGE_VERSION)) {
            setPackageVersion((String) additionalProperties.get(CodegenConstants.PACKAGE_VERSION));
        } else {
            setPackageVersion("1.0.0");
            additionalProperties.put(CodegenConstants.PACKAGE_VERSION, this.packageVersion);
        }
        if (additionalProperties.containsKey(CONTROLLER_PACKAGE)) {
            this.controllerPackage = additionalProperties.get(CONTROLLER_PACKAGE).toString();
        } else {
            this.controllerPackage = "controllers";
            additionalProperties.put(CONTROLLER_PACKAGE, this.controllerPackage);
        }
        if (additionalProperties.containsKey(DEFAULT_CONTROLLER)) {
            this.defaultController = additionalProperties.get(DEFAULT_CONTROLLER).toString();
        } else {
            this.defaultController = "default_controller";
            additionalProperties.put(DEFAULT_CONTROLLER, this.defaultController);
        }
        if (Boolean.TRUE.equals(additionalProperties.get(SUPPORT_PYTHON2))) {
            additionalProperties.put(SUPPORT_PYTHON2, Boolean.TRUE);
            typeMapping.put("long", "long");
        }
        supportingFiles.add(new SupportingFile("__init__.mustache", packageName, "__init__.py"));
        supportingFiles.add(new SupportingFile("__main__.mustache", packageName, "__main__.py"));
        supportingFiles.add(new SupportingFile("encoder.mustache", packageName, "encoder.py"));
        supportingFiles.add(new SupportingFile("util.mustache", packageName, "util.py"));
        supportingFiles.add(new SupportingFile("type_util.mustache", packageName, "type_util.py"));
        supportingFiles.add(new SupportingFile("__init__.mustache", packageName + File.separatorChar + controllerPackage, "__init__.py"));
        supportingFiles.add(new SupportingFile("__init__model.mustache", packageName + File.separatorChar + modelPackage, "__init__.py"));
        supportingFiles.add(new SupportingFile("base_model_.mustache", packageName + File.separatorChar + modelPackage, "base_model_.py"));
        supportingFiles.add(new SupportingFile("__init__test.mustache", packageName + File.separatorChar + testPackage, "__init__.py"));
        supportingFiles.add(new SupportingFile("swagger.mustache", packageName + File.separatorChar + "swagger", "swagger.yaml"));
        supportingFiles.add(new SupportingFile("authorization_controller.mustache", packageName + File.separatorChar + controllerPackage, "authorization_controller.py"));

        modelPackage = packageName + "." + modelPackage;
        controllerPackage = packageName + "." + controllerPackage;
        testPackage = packageName + "." + testPackage;
    }

    private static String dropDots(String str) {
        return str.replaceAll("\\.", "_");
    }

    @Override
    public String apiPackage() {
        return controllerPackage;
    }


    /**
     * Configures the type of generator.
     *
     * @return the CodegenType for this generator
     * @see io.swagger.codegen.CodegenType
     */
    @Override
    public CodegenType getTag() {
        return CodegenType.SERVER;
    }

    /**
     * Configures a friendly name for the generator.  This will be used by the generator
     * to select the library with the -l flag.
     *
     * @return the friendly name for the generator
     */
    @Override
    public String getName() {
        return "python-flask";
    }

    /**
     * Returns human-friendly help for the generator.  Provide the consumer with help
     * tips, parameters here
     *
     * @return A string value for the help message
     */
    @Override
    public String getHelp() {
        return "Generates a Python server library using the Connexion project. By default, " +
                "it will also generate service classes -- which you can disable with the `-Dnoservice` environment variable.";
    }

    @Override
    public String toApiName(String name) {
        if (name == null || name.length() == 0) {
            return "DefaultController";
        }
        return camelize(name, false) + "Controller";
    }

    @Override
    public String toApiFilename(String name) {
        return underscore(toApiName(name));
    }

    @Override
    public String toApiTestFilename(String name) {
        return "test_" + toApiFilename(name);
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        if(this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name; // add an underscore to the name
    }

    /**
     * Location to write api files.  You can use the apiPackage() as defined when the class is
     * instantiated
     */
    @Override
    public String apiFileFolder() {
        return outputFolder + File.separator + apiPackage().replace('.', File.separatorChar);
    }

    @Override
    public String getTypeDeclaration(Schema schemaProperty) {
        if (schemaProperty instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) schemaProperty;
            Schema inner = ap.getItems();
            return getSchemaType(schemaProperty) + "[" + getTypeDeclaration(inner) + "]";
        } else if (schemaProperty instanceof MapSchema) {
            MapSchema mp = (MapSchema) schemaProperty;
            Object inner = mp.getAdditionalProperties();

            if (inner instanceof Schema) {
                return getSchemaType(schemaProperty) + "[str, " + getTypeDeclaration((Schema) inner) + "]";
            }
        }
        return super.getTypeDeclaration(schemaProperty);
    }

    @Override
    public String getSchemaType(Schema p) {
        String swaggerType = super.getSchemaType(p);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (languageSpecificPrimitives.contains(type)) {
                return type;
            }
        } else {
            type = toModelName(swaggerType);
        }
        return type;
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);
        final Paths paths = openAPI.getPaths();
        addRouterControllerExtensions(paths);
        final Map<String, SecurityScheme> securitySchemes = openAPI.getComponents() != null ? openAPI.getComponents().getSecuritySchemes() : null;
        addSecurityExtensions(securitySchemes);
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> getOperations(Map<String, Object> objs) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        Map<String, Object> apiInfo = (Map<String, Object>) objs.get("apiInfo");
        List<Map<String, Object>> apis = (List<Map<String, Object>>) apiInfo.get("apis");
        for (Map<String, Object> api : apis) {
            result.add((Map<String, Object>) api.get("operations"));
        }
        return result;
    }

    protected void addOperationImports(CodegenOperation codegenOperation, Set<String> operationImports) {
        for (String operationImport : operationImports) {
            if ("object".equalsIgnoreCase(operationImport)) {
                continue;
            }
            if (needToImport(operationImport)) {
                codegenOperation.imports.add(operationImport);
            }
        }
    }

    private static List<Map<String, Object>> sortOperationsByPath(List<CodegenOperation> ops) {
        Multimap<String, CodegenOperation> opsByPath = ArrayListMultimap.create();

        for (CodegenOperation op : ops) {
            opsByPath.put(op.path, op);
        }

        List<Map<String, Object>> opsByPathList = new ArrayList<Map<String, Object>>();
        for (Map.Entry<String, Collection<CodegenOperation>> entry : opsByPath.asMap().entrySet()) {
            Map<String, Object> opsByPathEntry = new HashMap<String, Object>();
            opsByPathList.add(opsByPathEntry);
            opsByPathEntry.put("path", entry.getKey());
            opsByPathEntry.put("operation", entry.getValue());
            List<CodegenOperation> operationsForThisPath = Lists.newArrayList(entry.getValue());
            CodegenOperation codegenOperation = operationsForThisPath.get(operationsForThisPath.size() - 1);
            codegenOperation.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, Boolean.FALSE);

            if (opsByPathList.size() < opsByPath.asMap().size()) {
                opsByPathEntry.put("hasMore", "true");
            }
        }

        return opsByPathList;
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        OpenAPI openAPI = (OpenAPI) objs.get("openAPI");
        if(openAPI != null) {
            try {
                objs.put("openapi-yaml", Yaml.mapper().writeValueAsString(openAPI));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        for (Map<String, Object> operations : getOperations(objs)) {
            @SuppressWarnings("unchecked")
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");

            List<Map<String, Object>> opsByPathList = sortOperationsByPath(ops);
            operations.put("operationsByPath", opsByPathList);
        }
        return super.postProcessSupportingFileData(objs);
    }

    @Override
    public String toVarName(String name) {
        // sanitize name
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // remove dollar sign
        name = name.replaceAll("$", "");

        // if it's all uppper case, convert to lower case
        if (name.matches("^[A-Z_]*$")) {
            name = name.toLowerCase();
        }

        // underscore the variable name
        // petId => pet_id
        name = underscore(name);

        // remove leading underscore
        name = name.replaceAll("^_*", "");

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    @Override
    public String toParamName(String name) {
        // don't do name =removeNonNameElementToCamelCase(name); // this breaks connexion, which does not modify param names before sending them
        if (reservedWords.contains(name)) {
            name = escapeReservedWord(name);
        }
        // Param name is already sanitized in swagger spec processing
        return toVarName(name);
    }

    @Override
    public String toModelFilename(String name) {
        // underscore the model file name
        // PhoneNumber => phone_number
        return underscore(dropDots(toModelName(name)));
    }

    @Override
    public String toModelName(String name) {
        name = sanitizeName(name); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        // remove dollar sign
        name = name.replaceAll("$", "");

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + camelize("model_" + name));
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        if (!StringUtils.isEmpty(modelNamePrefix) &&
            !name.toLowerCase().startsWith(modelNamePrefix.toLowerCase())) {
            name = modelNamePrefix + "_" + name;
        }

        if (!StringUtils.isEmpty(modelNameSuffix) &&
            !name.toLowerCase().endsWith(modelNameSuffix.toLowerCase())) {
            name = name + "_" + modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(name);
    }

    @Override
    public String getDefaultTemplateDir() {
        return "pythonFlaskConnexion";
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty (should not occur as an auto-generated method name will be used)
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method name (operationId) not allowed");
        }

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + underscore(sanitizeName("call_" + operationId)));
            operationId = "call_" + operationId;
        }

        return underscore(sanitizeName(operationId));
    }

    /**
     * Return the default value of the property
     *
     * @param schemaProperty Swagger property object
     * @return string presentation of the default value of the property
     */
    @Override
    public String toDefaultValue(Schema schemaProperty) {
        if (schemaProperty instanceof StringSchema) {
            StringSchema dp = (StringSchema) schemaProperty;
            if (dp.getDefault() != null) {
                return "'" + dp.getDefault() + "'";
            }
        } else if (schemaProperty instanceof BooleanSchema) {
            BooleanSchema dp = (BooleanSchema) schemaProperty;
            if (dp.getDefault() != null) {
                if (dp.getDefault().toString().equalsIgnoreCase("false"))
                    return "False";
                else
                    return "True";
            }
        } else if (schemaProperty instanceof DateSchema) {
            // TODO
        } else if (schemaProperty instanceof DateTimeSchema) {
            // TODO
        } else if (schemaProperty instanceof NumberSchema) {
            NumberSchema dp = (NumberSchema) schemaProperty;
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            }
        } else if (schemaProperty instanceof IntegerSchema) {
            IntegerSchema dp = (IntegerSchema) schemaProperty;
            if (dp.getDefault() != null) {
                return dp.getDefault().toString();
            }
        }

        return null;
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        String example;

        if (p.defaultValue == null) {
            example = p.example;
        } else {
            example = p.defaultValue;
        }

        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("String".equalsIgnoreCase(type) || "str".equalsIgnoreCase(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            example = "'" + escapeText(example) + "'";
        } else if ("Integer".equals(type) || "int".equals(type)) {
            if(p.minimum != null) {
                example = "" + (Integer.valueOf(p.minimum) + 1);
            }
            if(p.maximum != null) {
                example = "" + p.maximum;
            } else if (example == null) {
                example = "56";
            }

        } else if ("Long".equalsIgnoreCase(type)) {
            if(p.minimum != null) {
                example = "" + (Long.valueOf(p.minimum) + 1);
            }
            if(p.maximum != null) {
                example = "" + p.maximum;
            } else if (example == null) {
                example = "789";
            }
        } else if ("Float".equalsIgnoreCase(type) || "Double".equalsIgnoreCase(type)) {
            if(p.minimum != null) {
                example = "" + p.minimum;
            } else if(p.maximum != null) {
                example = "" + p.maximum;
            } else if (example == null) {
                example = "3.4";
            }
        } else if ("BOOLEAN".equalsIgnoreCase(type) || "bool".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "True";
            }
        } else if ("file".equalsIgnoreCase(type)) {
            example = "(BytesIO(b'some file data'), 'file.txt')";
        } else if ("Date".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "2013-10-20";
            }
            example = "'" + escapeText(example) + "'";
        } else if ("DateTime".equalsIgnoreCase(type)) {
            if (example == null) {
                example = "2013-10-20T19:20:30+01:00";
            }
            example = "'" + escapeText(example) + "'";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = type + "()";
        } else {
            LOGGER.warn("Type " + type + " not handled properly in setParameterExampleValue");
        }

        if(p.items != null && p.items.defaultValue != null) {
            example = p.items.defaultValue;
        }
        if (example == null) {
            example = "None";
        } else if (getBooleanValue(p, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME)) {
            if (getBooleanValue(p, CodegenConstants.IS_BODY_PARAM_EXT_NAME)) {
                example = "[" + example + "]";
            }
        } else if (getBooleanValue(p, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME)) {
            example = "{'key': " + example + "}";
        }

        p.example = example;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public void setPackageVersion(String packageVersion) {
        this.packageVersion = packageVersion;
    }


    @Override
    public String escapeQuotationMark(String input) {
        // remove ' to avoid code injection
        return input.replace("'", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        // remove multiline comment
        return input.replace("'''", "'_'_'");
    }

    @Override
    public String toModelImport(String name) {
        String modelImport;
        if (StringUtils.startsWithAny(name,"import", "from")) {
            modelImport = name;
        } else {
            modelImport = "from ";
            if (!"".equals(modelPackage())) {
                modelImport += modelPackage() + ".";
            }
            modelImport += toModelFilename(name)+ " import " + name;
        }
        return modelImport;
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property){
        if (StringUtils.isNotEmpty(property.pattern)) {
            addImport(model, "import re");
        }
        postProcessPattern(property.pattern, property.vendorExtensions);
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // process enum in models
        return postProcessModelsEnum(objs);
    }

    @Override
    public void postProcessParameter(CodegenParameter parameter){
        postProcessPattern(parameter.pattern, parameter.vendorExtensions);
    }

    /*
     * The swagger pattern spec follows the Perl convention and style of modifiers. Python
     * does not support this in as natural a way so it needs to convert it. See
     * https://docs.python.org/2/howto/regex.html#compilation-flags for details.
     */
    public void postProcessPattern(String pattern, Map<String, Object> vendorExtensions){
        if(pattern != null) {
            int i = pattern.lastIndexOf('/');

            //Must follow Perl /pattern/modifiers convention
            if(pattern.charAt(0) != '/' || i < 2) {
                pattern = String.format("/%s/", pattern);;
                i = pattern.lastIndexOf('/');
            }

            String regex = pattern.substring(1, i).replace("'", "\\'");
            List<String> modifiers = new ArrayList<String>();

            for(char c : pattern.substring(i).toCharArray()) {
                if(regexModifiers.containsKey(c)) {
                    String modifier = regexModifiers.get(c);
                    modifiers.add(modifier);
                }
            }

            vendorExtensions.put("x-regex", regex);
            vendorExtensions.put("x-modifiers", modifiers);
        }
    }

    protected void addRouterControllerExtensions(Paths paths) {
        if(paths == null || paths.isEmpty()) {
            return;
        }
        // need vendor extensions for x-openapi-router-controller
        for(String pathname : paths.keySet()) {
            final PathItem path = paths.get(pathname);
            final Map<PathItem.HttpMethod, Operation> operationMap = path.readOperationsMap();

            if(operationMap == null || operationMap.isEmpty()) {
                continue;
            }
            for(PathItem.HttpMethod method : operationMap.keySet()) {
                Operation operation = operationMap.get(method);
                String tag = "default";
                if(operation.getTags() != null && operation.getTags().size() > 0) {
                    tag = operation.getTags().get(0);
                }
                String operationId = operation.getOperationId();
                if(operationId == null) {
                    operationId = getOrGenerateOperationId(operation, pathname, method.toString());
                }

                operation.setOperationId(toOperationId(operationId));
                if (operation.getExtensions() == null || operation.getExtensions().get("x-openapi-router-controller") == null) {
                    operation.addExtension("x-openapi-router-controller", controllerPackage + "." + toApiFilename(tag));
                }
                if (operation.getParameters() == null || operation.getParameters().isEmpty()) {
                    continue;
                }
                for (Parameter param: operation.getParameters()) {
                    // sanitize the param name but don't underscore it since it's used for request mapping
                    String name = param.getName();
                    String paramName = sanitizeName(name);
                    if (!paramName.equals(name)) {
                        LOGGER.warn(name + " cannot be used as parameter name with flask-connexion and was sanitized as " + paramName);
                    }
                    param.setName(paramName);
                }
            }
        }
    }

    protected void addSecurityExtensions(Map<String, SecurityScheme> securitySchemes) {
        if (securitySchemes == null || securitySchemes.isEmpty()) {
            return;
        }
        for (String securityName : securitySchemes.keySet()) {
            final SecurityScheme securityScheme = securitySchemes.get(securityName);
            final String functionName = controllerPackage + ".authorization_controller.check_" + securityName;

            if (SecurityScheme.Type.OAUTH2.equals(securityScheme.getType())) {
                securityScheme.addExtension("x-tokenInfoFunc", functionName);
                securityScheme.addExtension("x-scopeValidateFunc", controllerPackage + ".authorization_controller.validate_scope_" + securityName);
            } else if (SecurityScheme.Type.HTTP.equals(securityScheme.getType())) {
                if ("basic".equals(securityScheme.getScheme())) {
                    securityScheme.addExtension("x-basicInfoFunc", functionName);
                } else if ("bearer".equals(securityScheme.getScheme())) {
                    securityScheme.addExtension("x-bearerInfoFunc", functionName);
                }
            } else if (SecurityScheme.Type.APIKEY.equals(securityScheme.getType())) {
                securityScheme.addExtension("x-apikeyInfoFunc", functionName);
            } else {
                LOGGER.warn("Security type " + securityScheme.getType().toString() + " is not supported.");
            }
        }
    }

    public Map<String, Object> postProcessAllModels(Map<String, Object> processedModels) {
        Map<String, CodegenModel> allModels = new HashMap<>();
        for (Map.Entry<String, Object> entry : processedModels.entrySet()) {
            String modelName = toModelName(entry.getKey());
            Map<String, Object> inner = (Map<String, Object>) entry.getValue();
            List<Map<String, Object>> models = (List<Map<String, Object>>) inner.get("models");
            List<Map<String, String>> imports = (List<Map<String, String>>) inner.get("imports");
            for (Map<String, Object> mo : models) {
                CodegenModel codegenModel = (CodegenModel) mo.get("model");

                for (CodegenProperty codegenProperty : codegenModel.vars) {
                    if (Boolean.parseBoolean(String.valueOf(codegenProperty.vendorExtensions.get("x-is-composed")))) {
                        Map<String, String> item = new HashMap<>();
                        if (codegenProperty.getIsContainer()) {
                            item.put("import", toModelImport(codegenProperty.items.datatype));
                        } else {
                            item.put("import", toModelImport(codegenProperty.datatype));
                        }
                        imports.add(item);
                    }
                }

                allModels.put(modelName, codegenModel);
            }
        }
        postProcessAllCodegenModels(allModels);
        return processedModels;
    }

    @Override
    protected void addImport(CodegenModel codegenModel, String type) {
        if ("object".equalsIgnoreCase(type)) {
            return;
        }
        super.addImport(codegenModel, type);
    }
}
