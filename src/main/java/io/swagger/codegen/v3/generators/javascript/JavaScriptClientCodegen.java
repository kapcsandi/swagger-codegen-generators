package io.swagger.codegen.v3.generators.javascript;

import com.google.common.base.Strings;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.CodegenType;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.SupportingFile;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.DateTimeSchema;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.NumberSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static io.swagger.codegen.v3.CodegenConstants.HAS_ENUMS_EXT_NAME;
import static io.swagger.codegen.v3.CodegenConstants.IS_ENUM_EXT_NAME;
import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public class JavaScriptClientCodegen extends DefaultCodegenConfig {
    @SuppressWarnings("hiding")
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaScriptClientCodegen.class);

    public static final String PROJECT_NAME = "projectName";
    public static final String MODULE_NAME = "moduleName";
    public static final String PROJECT_DESCRIPTION = "projectDescription";
    public static final String PROJECT_VERSION = "projectVersion";
    public static final String USE_PROMISES = "usePromises";
    public static final String USE_INHERITANCE = "useInheritance";
    public static final String EMIT_MODEL_METHODS = "emitModelMethods";
    public static final String EMIT_JS_DOC = "emitJSDoc";
    public static final String USE_ES6 = "useES6";
    public static final String LOAD_TEST_DATA_FROM_FILE = "loadTestDataFromFile";
    public static final String TEST_DATA_FILE = "testDataFile";
    public static final String PRESERVE_LEADING_PARAM_CHAR = "preserveLeadingParamChar";

    final String[][] JAVASCRIPT_ES6_SUPPORTING_FILES = new String[][] {
            new String[] {"package.mustache", "package.json"},
            new String[] {"index.mustache", "index.js"},
            new String[] {"ApiClient.mustache", "ApiClient.js"},
            new String[] {"git_push.sh.mustache", "git_push.sh"},
            new String[] {"README.mustache", "README.md"},
            new String[] {"mocha.opts", "mocha.opts"},
            new String[] {"travis.yml", ".travis.yml"},
            new String[] {"assert-equals.js", "assert-equals.js"},
            new String[] {".babelrc.mustache", ".babelrc"}
    };

    static final String[][] JAVASCRIPT_SUPPORTING_FILES = new String[][] {
            new String[] {"package.mustache", "package.json"},
            new String[] {"index.mustache", "index.js"},
            new String[] {"ApiClient.mustache", "ApiClient.js"},
            new String[] {"git_push.sh.mustache", "git_push.sh"},
            new String[] {"README.mustache", "README.md"},
            new String[] {"mocha.opts", "mocha.opts"},
            new String[] {"travis.yml", ".travis.yml"},
            new String[] {"assert-equals.js", "assert-equals.js"},

    };

    static final Collection<String> INVOKER_PKG_SUPPORTING_FILES = Arrays.asList("ApiClient.mustache", "index.mustache");
    static final Collection<String> TEST_SUPPORTING_FILES = Arrays.asList("assert-equals.js");

    protected String projectName;
    protected String moduleName;
    protected String projectDescription;
    protected String projectVersion;
    protected String licenseName;

    protected String invokerPackage;
    protected String sourceFolder = "src";
    protected String localVariablePrefix = "";
    protected boolean usePromises;
    protected boolean emitModelMethods;
    protected boolean emitJSDoc = true;
    protected String apiDocPath = "docs/";
    protected String modelDocPath = "docs/";
    protected String apiTestPath = "api/";
    protected String modelTestPath = "model/";
    protected boolean useES6 = true;
    private String modelPropertyNaming = "camelCase";

    protected boolean loadTestDataFromFile = false;
    protected File testDataFile = null;
    protected boolean preserveLeadingParamChar = false;

    public JavaScriptClientCodegen() {
        super();
        outputFolder = "generated-code/js";
        modelTemplateFiles.put("model.mustache", ".js");
        modelTestTemplateFiles.put("model_test.mustache", ".js");
        apiTemplateFiles.put("api.mustache", ".js");
        apiTestTemplateFiles.put("api_test.mustache", ".js");
        apiPackage = "api";
        modelPackage = "model";
        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        // default HIDE_GENERATION_TIMESTAMP to true
        hideGenerationTimestamp = Boolean.TRUE;

        // reference: http://www.w3schools.com/js/js_reserved.asp
        setReservedWordsLowerCase(
                Arrays.asList(
                        "abstract", "arguments", "boolean", "break", "byte",
                        "case", "catch", "char", "class", "const",
                        "continue", "debugger", "default", "delete", "do",
                        "double", "else", "enum", "eval", "export",
                        "extends", "false", "final", "finally", "float",
                        "for", "function", "goto", "if", "implements",
                        "import", "in", "instanceof", "int", "interface",
                        "let", "long", "native", "new", "null",
                        "package", "private", "protected", "public", "return",
                        "short", "static", "super", "switch", "synchronized",
                        "this", "throw", "throws", "transient", "true",
                        "try", "typeof", "var", "void", "volatile",
                        "while", "with", "yield",
                        "Array", "Date", "eval", "function", "hasOwnProperty",
                        "Infinity", "isFinite", "isNaN", "isPrototypeOf",
                        "Math", "NaN", "Number", "Object",
                        "prototype", "String", "toString", "undefined", "valueOf")
        );

        languageSpecificPrimitives = new HashSet<String>(
                Arrays.asList("String", "Boolean", "Number", "Array", "Object", "Date", "File", "Blob")
        );
        defaultIncludes = new HashSet<String>(languageSpecificPrimitives);

        instantiationTypes.put("array", "Array");
        instantiationTypes.put("list", "Array");
        instantiationTypes.put("map", "Object");
        typeMapping.clear();
        typeMapping.put("array", "Array");
        typeMapping.put("map", "Object");
        typeMapping.put("List", "Array");
        typeMapping.put("boolean", "Boolean");
        typeMapping.put("string", "String");
        typeMapping.put("int", "Number");
        typeMapping.put("float", "Number");
        typeMapping.put("number", "Number");
        typeMapping.put("BigDecimal", "Number");
        typeMapping.put("DateTime", "Date");
        typeMapping.put("date", "Date");
        typeMapping.put("long", "Number");
        typeMapping.put("short", "Number");
        typeMapping.put("char", "String");
        typeMapping.put("double", "Number");
        typeMapping.put("object", "Object");
        typeMapping.put("integer", "Number");
        // binary not supported in JavaScript client right now, using String as a workaround
        typeMapping.put("ByteArray", "Blob"); // I don't see ByteArray defined in the Swagger docs.
        typeMapping.put("binary", "File");
        typeMapping.put("binary", "Blob");
        typeMapping.put("file", "File");
        typeMapping.put("URI", "String");
        typeMapping.put("UUID", "String");

        importMapping.clear();

        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC).defaultValue("src"));
        cliOptions.add(new CliOption(CodegenConstants.LOCAL_VARIABLE_PREFIX, CodegenConstants.LOCAL_VARIABLE_PREFIX_DESC));
        cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, CodegenConstants.INVOKER_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        cliOptions.add(new CliOption(PROJECT_NAME,
                "name of the project (Default: generated from info.title or \"swagger-js-client\")"));
        cliOptions.add(new CliOption(MODULE_NAME,
                "module name for AMD, Node or globals (Default: generated from <projectName>)"));
        cliOptions.add(new CliOption(PROJECT_DESCRIPTION,
                "description of the project (Default: using info.description or \"Client library of <projectName>\")"));
        cliOptions.add(new CliOption(PROJECT_VERSION,
                "version of the project (Default: using info.version or \"1.0.0\")"));
        cliOptions.add(new CliOption(CodegenConstants.LICENSE_NAME,
                "name of the license the project uses (Default: using info.license.name)"));
        cliOptions.add(new CliOption(USE_PROMISES,
                "use Promises as return values from the client API, instead of superagent callbacks")
                .defaultValue(Boolean.FALSE.toString()));
        cliOptions.add(new CliOption(EMIT_MODEL_METHODS,
                "generate getters and setters for model properties")
                .defaultValue(Boolean.FALSE.toString()));
        cliOptions.add(new CliOption(EMIT_JS_DOC,
                "generate JSDoc comments")
                .defaultValue(Boolean.TRUE.toString()));
        cliOptions.add(new CliOption(USE_INHERITANCE,
                "use JavaScript prototype chains & delegation for inheritance")
                .defaultValue(Boolean.TRUE.toString()));
        cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP, CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC)
                .defaultValue(Boolean.TRUE.toString()));
        cliOptions.add(new CliOption(USE_ES6,
                "use JavaScript ES6 (ECMAScript 6) (beta). Default is ES5.")
                .defaultValue(Boolean.FALSE.toString()));
        cliOptions.add(new CliOption(CodegenConstants.MODEL_PROPERTY_NAMING, CodegenConstants.MODEL_PROPERTY_NAMING_DESC).defaultValue("camelCase"));
        cliOptions.add(CliOption.newBoolean(LOAD_TEST_DATA_FROM_FILE, "Load test data from a JSON file"));
        cliOptions.add(CliOption.newString(TEST_DATA_FILE, "JSON file to contain test data"));
        cliOptions.add(CliOption.newString(PRESERVE_LEADING_PARAM_CHAR, "Preserves leading $ and _ characters in parameter names."));
    }

    @Override
    public CodegenType getTag() {
        return CodegenType.CLIENT;
    }

    @Override
    public String getName() {
        return "javascript";
    }

    @Override
    public String getHelp() {
        return "Generates a Javascript client library.";
    }

    @Override
    public void processOpts() {
        if (additionalProperties.containsKey(USE_ES6)) {
            setUseES6(convertPropertyToBooleanAndWriteBack(USE_ES6));
        }
        super.processOpts();

        if (additionalProperties.containsKey(PROJECT_NAME)) {
            setProjectName(((String) additionalProperties.get(PROJECT_NAME)));
        }
        if (additionalProperties.containsKey(MODULE_NAME)) {
            setModuleName(((String) additionalProperties.get(MODULE_NAME)));
        }
        if (additionalProperties.containsKey(PROJECT_DESCRIPTION)) {
            setProjectDescription(((String) additionalProperties.get(PROJECT_DESCRIPTION)));
        }
        if (additionalProperties.containsKey(PROJECT_VERSION)) {
            setProjectVersion(((String) additionalProperties.get(PROJECT_VERSION)));
        }
        if (additionalProperties.containsKey(CodegenConstants.LICENSE_NAME)) {
            setLicenseName(((String) additionalProperties.get(CodegenConstants.LICENSE_NAME)));
        }
        if (additionalProperties.containsKey(CodegenConstants.LOCAL_VARIABLE_PREFIX)) {
            setLocalVariablePrefix((String) additionalProperties.get(CodegenConstants.LOCAL_VARIABLE_PREFIX));
        }
        if (additionalProperties.containsKey(CodegenConstants.SOURCE_FOLDER)) {
            setSourceFolder((String) additionalProperties.get(CodegenConstants.SOURCE_FOLDER));
        }
        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
        }
        if (additionalProperties.containsKey(USE_PROMISES)) {
            setUsePromises(convertPropertyToBooleanAndWriteBack(USE_PROMISES));
        }
        if (additionalProperties.containsKey(USE_INHERITANCE)) {
            setUseInheritance(convertPropertyToBooleanAndWriteBack(USE_INHERITANCE));
        } else {
            supportsInheritance = true;
            supportsMixins = true;
        }
        if (additionalProperties.containsKey(EMIT_MODEL_METHODS)) {
            setEmitModelMethods(convertPropertyToBooleanAndWriteBack(EMIT_MODEL_METHODS));
        }
        if (additionalProperties.containsKey(EMIT_JS_DOC)) {
            setEmitJSDoc(convertPropertyToBooleanAndWriteBack(EMIT_JS_DOC));
        }
        if (additionalProperties.containsKey(CodegenConstants.MODEL_PROPERTY_NAMING)) {
            setModelPropertyNaming((String) additionalProperties.get(CodegenConstants.MODEL_PROPERTY_NAMING));
        }
        boolean loadTestDataFromFile = convertPropertyToBooleanAndWriteBack(LOAD_TEST_DATA_FROM_FILE);
        this.setLoadTestDataFromFile(loadTestDataFromFile);
        if (loadTestDataFromFile) {
            String testDataFileStr;
            if (additionalProperties.containsKey(TEST_DATA_FILE))
                testDataFileStr = (String) additionalProperties.get(TEST_DATA_FILE);
            else
                testDataFileStr = outputFolder() + "/test/test-data.json";
            testDataFileStr = testDataFileStr.replace('/', File.separatorChar);
            try {
                File testDataFile = new File(testDataFileStr).getCanonicalFile();
                testDataFileStr = testDataFile.getPath();
                additionalProperties.put(TEST_DATA_FILE, testDataFileStr.replaceAll("\\\\", "\\\\\\\\"));
                setTestDataFile(testDataFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to canonicalize file " + testDataFileStr, e);
            }
        }
        boolean preserveLeadingParamChar = convertPropertyToBooleanAndWriteBack(PRESERVE_LEADING_PARAM_CHAR);
        this.setPreserveLeadingParamChar(preserveLeadingParamChar);
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);

        if (openAPI.getInfo() != null) {
            Info info = openAPI.getInfo();
            if (StringUtils.isBlank(projectName) && info.getTitle() != null) {
                // when projectName is not specified, generate it from info.title
                projectName = sanitizeName(dashize(info.getTitle()));
            }
            if (StringUtils.isBlank(projectVersion)) {
                // when projectVersion is not specified, use info.version
                projectVersion = escapeUnsafeCharacters(escapeQuotationMark(info.getVersion()));
            }
            if (projectDescription == null) {
                // when projectDescription is not specified, use info.description
                projectDescription = sanitizeName(info.getDescription());
            }

            // when licenceName is not specified, use info.license
            if (additionalProperties.get(CodegenConstants.LICENSE_NAME) == null && info.getLicense() != null) {
                License license = info.getLicense();
                licenseName = license.getName();
            }
        }

        // default values
        if (StringUtils.isBlank(projectName)) {
            projectName = "swagger-js-client";
        }
        if (StringUtils.isBlank(moduleName)) {
            moduleName = camelize(underscore(projectName));
        }
        if (StringUtils.isBlank(projectVersion)) {
            projectVersion = "1.0.0";
        }
        if (projectDescription == null) {
            projectDescription = "Client library of " + projectName;
        }
        if (StringUtils.isBlank(licenseName)) {
            licenseName = "Unlicense";
        }

        additionalProperties.put(PROJECT_NAME, projectName);
        additionalProperties.put(MODULE_NAME, moduleName);
        additionalProperties.put(PROJECT_DESCRIPTION, escapeText(projectDescription));
        additionalProperties.put(PROJECT_VERSION, projectVersion);
        additionalProperties.put(CodegenConstants.LICENSE_NAME, licenseName);
        additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
        additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        additionalProperties.put(CodegenConstants.LOCAL_VARIABLE_PREFIX, localVariablePrefix);
        additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
        additionalProperties.put(CodegenConstants.SOURCE_FOLDER, sourceFolder);
        additionalProperties.put(USE_PROMISES, usePromises);
        additionalProperties.put(USE_INHERITANCE, supportsInheritance);
        additionalProperties.put(EMIT_MODEL_METHODS, emitModelMethods);
        additionalProperties.put(EMIT_JS_DOC, emitJSDoc);
        additionalProperties.put(USE_ES6, useES6);

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        String[][] supportingTemplateFiles = useES6 ? JAVASCRIPT_ES6_SUPPORTING_FILES : JAVASCRIPT_SUPPORTING_FILES;
        for (String[] supportingTemplateFile : supportingTemplateFiles) {
            String templateFile = supportingTemplateFile[0];
            String folder;
            if (INVOKER_PKG_SUPPORTING_FILES.contains(templateFile))
                // #1150: index.js & ApiClient.js must be generated to invokerPackage, otherwise nothing works!
                folder = createPath(sourceFolder, invokerPackage);
            else if (TEST_SUPPORTING_FILES.contains(templateFile))
                folder = createPath("test");
            else
                folder = "";
            supportingFiles.add(new SupportingFile(templateFile, folder, supportingTemplateFile[1]));
        }
    }

    @Override
    public String escapeReservedWord(String name) {
        if(this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    /**
     * Concatenates an array of path segments into a path string.
     * @param segments The path segments to concatenate. A segment may contain either of the file separator characters '\' or '/'.
     * A segment is ignored if it is <code>null</code>, empty or &quot;.&quot;.
     * @return A path string using the correct platform-specific file separator character.
     */
    private String createPath(String... segments) {
        StringBuilder buf = new StringBuilder();
        for (String segment : segments) {
            if (!StringUtils.isEmpty(segment) && !segment.equals(".")) {
                if (buf.length() != 0)
                    buf.append(File.separatorChar);
                buf.append(segment);
            }
        }
        for (int i = 0; i < buf.length(); i++) {
            char c = buf.charAt(i);
            if ((c == '/' || c == '\\') && c != File.separatorChar)
                buf.setCharAt(i, File.separatorChar);
        }
        return buf.toString();
    }

    @Override
    public String apiTestFileFolder() {
        return (outputFolder + "/test/" + apiTestPath).replace('/', File.separatorChar);
    }

    @Override
    public String modelTestFileFolder() {
        return (outputFolder + "/test/" + modelTestPath).replace('/', File.separatorChar);
    }

    @Override
    public String apiFileFolder() {
        return createPath(outputFolder, sourceFolder, invokerPackage, apiPackage());
    }

    @Override
    public String modelFileFolder() {
        return createPath(outputFolder, sourceFolder, invokerPackage, modelPackage());
    }

    public String getInvokerPackage() {
        return invokerPackage;
    }

    public void setInvokerPackage(String invokerPackage) {
        this.invokerPackage = invokerPackage;
    }

    public void setSourceFolder(String sourceFolder) {
        this.sourceFolder = sourceFolder;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public void setLocalVariablePrefix(String localVariablePrefix) {
        this.localVariablePrefix = localVariablePrefix;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public void setProjectDescription(String projectDescription) {
        this.projectDescription = projectDescription;
    }

    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    public void setLicenseName(String licenseName) {
        this.licenseName = licenseName;
    }

    public void setUsePromises(boolean usePromises) {
        this.usePromises = usePromises;
    }

    public void setUseES6(boolean useES6) {
        this.useES6 = useES6;
        if (useES6) {
            embeddedTemplateDir = templateDir = "Javascript/es6";
            LOGGER.info("Using JS ES6 templates");
        } else {
            embeddedTemplateDir = templateDir = "Javascript";
            LOGGER.info("Using JS ES5 templates");
        }
    }

    public void setUseInheritance(boolean useInheritance) {
        this.supportsInheritance = useInheritance;
        this.supportsMixins = useInheritance;
    }

    public void setEmitModelMethods(boolean emitModelMethods) {
        this.emitModelMethods = emitModelMethods;
    }

    public void setEmitJSDoc(boolean emitJSDoc) {
        this.emitJSDoc = emitJSDoc;
    }

    @Override
    public String apiDocFileFolder() {
        return createPath(outputFolder, apiDocPath);
    }

    @Override
    public String modelDocFileFolder() {
        return createPath(outputFolder, modelDocPath);
    }

    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + ".spec";
    }

    @Override
    public String toModelTestFilename(String name) {
        return toModelName(name) + ".spec";
    }

    public String getModelPropertyNaming() {
        return this.modelPropertyNaming;
    }

    private String getNameUsingModelPropertyNaming(String name) {
        switch (CodegenConstants.MODEL_PROPERTY_NAMING_TYPE.valueOf(getModelPropertyNaming())) {
            case original:    return name;
            case camelCase:   return camelize(name, true);
            case PascalCase:  return camelize(name);
            case snake_case:  return underscore(name);
            default:          throw new IllegalArgumentException("Invalid model property naming '" +
                    name + "'. Must be 'original', 'camelCase', " +
                    "'PascalCase' or 'snake_case'");
        }
    }

    @Override
    public String toVarName(String name) {
        char first = name.charAt(0);

        // sanitize name
        name = sanitizeName(name);  // FIXME parameter should not be assigned. Also declare it as "final"

        if("_".equals(name)) {
          name = "_u";
        }

        // if it's all upper case, do nothing
        if (name.matches("^[A-Z_]*$")) {
            return name;
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        name = getNameUsingModelPropertyNaming(name);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        // $ and _ characters are legal in JavaScript identifiers, so no need to strip them.
        if (preserveLeadingParamChar && (first == '$' || first == '_'))
            name = first + name;

        return name;
    }

    @Override
    public String toParamName(String name) {
        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(String name) {
        if (name == null) {
            return "Object";
        }
        name = sanitizeName(name);  // FIXME parameter should not be assigned. Also declare it as "final"

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
        name = camelize(name);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            String modelName = "Model" + name;
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            String modelName = "Model" + name; // e.g. 200Response => Model200Response (after camelize)
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to " + modelName);
            return modelName;
        }

        return name;
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String toModelImport(String name) {
        return name;
    }

    @Override
    public String toApiImport(String name) {
        return toApiName(name);
    }

    @Override
    public String getTypeDeclaration(Schema schema) {
        if (schema instanceof ArraySchema) {
            ArraySchema ap = (ArraySchema) schema;
            Schema inner = ap.getItems();
            return "[" + getTypeDeclaration(inner) + "]";
        } else if (schema instanceof MapSchema && hasSchemaProperties(schema)) {
            MapSchema mapSchema = (MapSchema) schema;
            Schema inner = (Schema) mapSchema.getAdditionalProperties();
            return "{String: " + getTypeDeclaration(inner) + "}";
        } else if (schema instanceof MapSchema && hasTrueAdditionalProperties(schema)) {
            Schema inner = new ObjectSchema();
            return "{String: " + getTypeDeclaration(inner) + "}";
        }
        return super.getTypeDeclaration(schema);
    }

    @Override
    public String toDefaultValue(Schema propertySchema) {
        if (propertySchema instanceof StringSchema) {
            StringSchema stringSchema = (StringSchema) propertySchema;
            if (stringSchema.getDefault() != null) {
                return "'" + stringSchema.getDefault() + "'";
            }
        } else if (propertySchema instanceof BooleanSchema) {
            BooleanSchema booleanSchema = (BooleanSchema) propertySchema;
            if (booleanSchema.getDefault() != null) {
                return booleanSchema.getDefault().toString();
            }
        } else if (propertySchema instanceof DateSchema) {
            // TODO
        } else if (propertySchema instanceof DateTimeSchema) {
            // TODO
        } else if (propertySchema instanceof NumberSchema) {
            NumberSchema numberSchema = (NumberSchema) propertySchema;
            if (numberSchema.getDefault() != null) {
                return numberSchema.getDefault().toString();
            }
        } else if (propertySchema instanceof IntegerSchema) {
            IntegerSchema integerSchema = (IntegerSchema) propertySchema;
            if (integerSchema.getDefault() != null) {
                return integerSchema.getDefault().toString();
            }
        }

        return null;
    }

    public void setModelPropertyNaming(String naming) {
        if ("original".equals(naming) || "camelCase".equals(naming) ||
                "PascalCase".equals(naming) || "snake_case".equals(naming)) {
            this.modelPropertyNaming = naming;
        } else {
            throw new IllegalArgumentException("Invalid model property naming '" +
                    naming + "'. Must be 'original', 'camelCase', " +
                    "'PascalCase' or 'snake_case'");
        }
    }

    @Override
    public String toDefaultValueWithParam(String name, Schema schema) {
        String typeDeclaration = getTypeDeclaration(schema);
        String type = normalizeType(typeDeclaration);
        if (!StringUtils.isEmpty(schema.get$ref()) && !isPrimitiveType(typeDeclaration.toLowerCase())) {
            return " = " + type + ".constructFromObject(data['" + name + "']);";
        }
        return " = ApiClient.convertToType(data['" + name + "'], " + type + ");";
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

        if ("String".equals(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Integer".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Number".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
        } else if ("Boolean".equals(type)) {
            if (example == null) {
                example = "true";
            }
        } else if ("Blob".equals(type)) {
            if (example == null || example.equals("B")) {
                example = "QmFzZTY0IGV4YW1wbGU=";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("File".equals(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Date".equals(type)) {
            if (example == null) {
                example = "2013-10-20T19:20:30+01:00";
            }
            example = "new Date(\"" + escapeText(example) + "\")";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + moduleName + "." + type + "()";
        }

        if (example == null) {
            example = "null";
        } else if (getBooleanValue(p, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME)) {
            example = "[" + example + "]";
        } else if (getBooleanValue(p, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME)) {
            example = "{key: " + example + "}";
        }

        p.example = example;
    }

    /**
     * Normalize type by wrapping primitive types with single quotes.
     *
     * @param type Primitive type
     * @return Normalized type
     */
    public String normalizeType(String type) {
      return type.replaceAll("\\b(Boolean|Integer|Number|String|Date|Blob)\\b", "'$1'");
    }

    @Override
    public String getSchemaType(Schema schema) {
        String swaggerType = super.getSchemaType(schema);
        String type = null;
        if (typeMapping.containsKey(swaggerType)) {
            type = typeMapping.get(swaggerType);
            if (!needToImport(type)) {
                return type;
            }
        } else {
            type = swaggerType;
        }
        if (null == type) {
            LOGGER.error("No Type defined for Property " + schema);
        }
        return toModelName(type);
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method/operation name (operationId) not allowed");
        }

        operationId = camelize(sanitizeName(operationId), true);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, true);
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to " + newOperationId);
            return newOperationId;
        }

        return operationId;
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, Map<String, Schema> schemas, OpenAPI openAPI) {
      CodegenOperation op = super.fromOperation(path, httpMethod, operation, schemas, openAPI);
      if (op.returnType != null) {
        op.returnType = normalizeType(op.returnType);
      }

      //path is an unescaped variable in the mustache template api.mustache line 82 '<&path>'
      op.path = sanitizePath(op.path);

      // Set vendor-extension to be used in template:
      //     x-codegen-hasMoreRequired
      //     x-codegen-hasMoreOptional
      //     x-codegen-hasRequiredParams
      CodegenParameter lastRequired = null;
      CodegenParameter lastOptional = null;
      for (CodegenParameter p : op.allParams) {
          if (p.required) {
              lastRequired = p;
          } else {
              lastOptional = p;
          }
      }
      for (CodegenParameter p : op.allParams) {
          if (p == lastRequired) {
              p.vendorExtensions.put("x-codegen-hasMoreRequired", false);
          } else if (p == lastOptional) {
              p.vendorExtensions.put("x-codegen-hasMoreOptional", false);
          } else {
              p.vendorExtensions.put("x-codegen-hasMoreRequired", true);
              p.vendorExtensions.put("x-codegen-hasMoreOptional", true);
          }
      }
      op.vendorExtensions.put("x-codegen-hasRequiredParams", lastRequired != null);

      return op;
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allSchemas) {
        CodegenModel codegenModel = super.fromModel(name, schema, allSchemas);

        boolean hasEnums = getBooleanValue(codegenModel, HAS_ENUMS_EXT_NAME);
        if (allSchemas != null && codegenModel != null && codegenModel.parent != null && hasEnums) {
            final Schema parentModel = allSchemas.get(codegenModel.parentSchema);
            final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel, allSchemas);
            codegenModel = JavaScriptClientCodegen.reconcileInlineEnums(codegenModel, parentCodegenModel);
        }
        if (schema instanceof ArraySchema) {
            final Schema items = ((ArraySchema) schema).getItems();
            if (items != null) {
                codegenModel.vendorExtensions.put("x-isArray", true);
                codegenModel.vendorExtensions.put("x-itemType", getSchemaType(items));
            }

        } else if (schema instanceof MapSchema && hasSchemaProperties(schema)) {
            if (schema.getAdditionalProperties() != null) {
                codegenModel.vendorExtensions.put("x-isMap", true);
                codegenModel.vendorExtensions.put("x-itemType", getSchemaType((Schema) schema.getAdditionalProperties()));
            } else {
                String type = schema.getType();
                if (isPrimitiveType(type)){
                    codegenModel.vendorExtensions.put("x-isPrimitive", true);
                }
            }
        }

        return codegenModel;
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        super.addAdditionPropertiesToCodeGenModel(codegenModel, schema);
        if (schema.getAdditionalProperties() != null && schema.getAdditionalProperties() instanceof Schema) {
            codegenModel.additionalPropertiesType = getSchemaType((Schema) schema.getAdditionalProperties());
        }
    }

    private String sanitizePath(String p) {
        //prefer replace a ', instead of a fuLL URL encode for readability
        return p.replaceAll("'", "%27");
    }

    private String trimBrackets(String s) {
        if (s != null) {
            int beginIdx = s.charAt(0) == '[' ? 1 : 0;
            int endIdx = s.length();
            if (s.charAt(endIdx - 1) == ']')
                endIdx--;
            return s.substring(beginIdx, endIdx);
        }
        return null;
    }

    private String getModelledType(String dataType) {
        return "module:" + (StringUtils.isEmpty(invokerPackage) ? "" : (invokerPackage + "/"))
            + (StringUtils.isEmpty(modelPackage) ? "" : (modelPackage + "/")) + dataType;
    }

    @Override
    public String getDefaultTemplateDir() {
        if (useES6) {
            return "javascript/es6";
        }
        return "javascript";
    }

    private String getJSDocType(CodegenModel cm, CodegenProperty cp) {
        if (getBooleanValue(cp, CodegenConstants.IS_CONTAINER_EXT_NAME)) {
            if (cp.containerType.equals("array"))
                return "Array.<" + getJSDocType(cm, cp.items) + ">";
            else if (cp.containerType.equals("map"))
                return "Object.<String, " + getJSDocType(cm, cp.items) + ">";
        }
        String dataType = trimBrackets(cp.datatypeWithEnum);

        boolean isEnum = getBooleanValue(cp, IS_ENUM_EXT_NAME);
        if (isEnum) {
            dataType = cm.classname + '.' + dataType;
        }
        if (isModelledType(cp))
            dataType = getModelledType(dataType);
        return dataType;
    }

    private boolean isModelledType(CodegenProperty cp) {
        // N.B. enums count as modelled types, file is not modelled (SuperAgent uses some 3rd party library).
        boolean isEnum = getBooleanValue(cp, IS_ENUM_EXT_NAME);
        return isEnum || !languageSpecificPrimitives.contains(cp.baseType == null ? cp.datatype : cp.baseType);
    }

    private String getJSDocType(CodegenParameter cp) {
        String dataType = trimBrackets(cp.dataType);
        if (isModelledType(cp))
            dataType = getModelledType(dataType);
        if (getBooleanValue(cp, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME)) {
            return "Array.<" + dataType + ">";
        } else if (getBooleanValue(cp, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME)) {
            return "Object.<String, " + dataType + ">";
        }
        return dataType;
    }

    private boolean isModelledType(CodegenParameter cp) {
        // N.B. enums count as modelled types, file is not modelled (SuperAgent uses some 3rd party library).
        boolean isEnum = getBooleanValue(cp, IS_ENUM_EXT_NAME);
        return isEnum || !languageSpecificPrimitives.contains(cp.baseType == null ? cp.dataType : cp.baseType);
    }

    private String getJSDocType(CodegenOperation co) {
        String returnType = trimBrackets(co.returnType);
        if (returnType != null) {
            if (isModelledType(co))
                returnType = getModelledType(returnType);
            if (getBooleanValue(co, CodegenConstants.IS_LIST_CONTAINER_EXT_NAME)) {
                return "Array.<" + returnType + ">";
            } else if (getBooleanValue(co, CodegenConstants.IS_MAP_CONTAINER_EXT_NAME)) {
                return "Object.<String, " + returnType + ">";
            }
        }
        return returnType;
    }

    private boolean isModelledType(CodegenOperation co) {
        // This seems to be the only way to tell whether an operation return type is modelled.
        return !Boolean.TRUE.equals(co.returnTypeIsPrimitive);
    }

    private boolean isPrimitiveType(String type) {
        final String[] primitives = {"number", "integer", "string", "boolean", "null"};
        return Arrays.asList(primitives).contains(type);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        // Generate and store argument list string of each operation into
        // vendor-extension: x-codegen-arg-list.
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if (operations != null) {
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (CodegenOperation operation : ops) {
                List<String> argList = new ArrayList<String>();
                boolean hasOptionalParams = false;
                for (CodegenParameter p : operation.allParams) {
                    if (p.required) {
                        argList.add(p.paramName);
                    } else {
                      hasOptionalParams = true;
                    }
                }
                if (hasOptionalParams) {
                    argList.add("opts");
                }
                operation.vendorExtensions.put("x-codegen-arg-list", StringUtils.join(argList, ", "));

                // Store JSDoc type specification into vendor-extension: x-jsdoc-type.
                for (CodegenParameter cp : operation.allParams) {
                    String jsdocType = getJSDocType(cp);
                    cp.vendorExtensions.put("x-jsdoc-type", jsdocType);
                }
                String jsdocType = getJSDocType(operation);
                operation.vendorExtensions.put("x-jsdoc-type", jsdocType);
            }
        }
        return objs;
    }

    @Override
    public Map<String, Object> postProcessOperationsWithModels(Map<String, Object> objs, List<Object> allModels) {
        objs = super.postProcessOperationsWithModels(objs, allModels);

        // Map the models so we can look them up by name.
        Map<String, CodegenModel> cgModels = new HashMap<>();
        for (Object model : allModels) {
            @SuppressWarnings("unchecked")
            CodegenModel cgModel = ((Map<String, CodegenModel>) model).get("model");
            /** todo: let's handle this with handlebar helpers
            cgModel.vendorExtensions.put("x-indent", indent);
            */
            cgModels.put(cgModel.classname, cgModel);
        }

        // Provide access to all parameter models.
        @SuppressWarnings("unchecked")
        Map<String, Object> operations = (Map<String, Object>) objs.get("operations");
        if (operations != null) {
            @SuppressWarnings("unchecked")
            List<CodegenOperation> ops = (List<CodegenOperation>) operations.get("operation");
            for (CodegenOperation op : ops) {
                CodegenModel cgModel = cgModels.get(op.returnBaseType);
                if (cgModel != null)
                    op.vendorExtensions.put("x-codegen-model", cgModel);
                op.vendorExtensions.put("x-expect-js-type", getExpectJsItemType(op.returnBaseType));
                op.vendorExtensions.put("x-is-container", op.getIsListContainer() || op.getIsMapContainer());
                /** todo: let's handle this with handlebar helpers
                op.vendorExtensions.put("x-indent", indent);
                */
                postProcessParameters(op.allParams, cgModels);
                postProcessParameters(op.requiredParams, cgModels);
            }
        }

        // Provide access to all property models.
        for (CodegenModel cgModel : cgModels.values()) {
            detectRecursiveModel(cgModel.allVars, cgModel.classname, cgModels);
            postProcessProperties(cgModel.vars, cgModels);
            if (cgModel.allVars != cgModel.vars) {
                postProcessProperties(cgModel.allVars, cgModels);
            }
        }

        return objs;
    }

    private void postProcessParameters(Collection<CodegenParameter> cgParams, Map<String, CodegenModel> cgModels) {
        for (CodegenParameter cgParam : cgParams) {
            if (cgParam.items != null)
                cgParam.items.vendorExtensions.put("x-is-param-item", true);
            if (!cgParam.getIsPrimitiveType() && !cgParam.vendorExtensions.containsKey("x-codegen-model")) {
                CodegenModel paramType = cgModels.get(cgParam.baseType);
                if (paramType != null)
                    cgParam.vendorExtensions.put("x-codegen-model", paramType);
            }
            if (cgParam.getIsEnum()) {
                cgParam.vendorExtensions.put("x-first-enum-value",
                        ((List<Map<String, String>>) cgParam.allowableValues.get("enumVars")).get(0).get("value"));
            }
            /** todo: let's handle this with handlebar helpers
            cgParam.vendorExtensions.put("x-cache-current-param", cacheCurrentParam);
            cgParam.vendorExtensions.put("x-indent", indent);
            */
        }
    }

    private void postProcessProperties(Collection<CodegenProperty> cgProps, Map<String, CodegenModel> cgModels) {
        for (CodegenProperty cgProp : cgProps) {
            if (!cgProp.getIsPrimitiveType() && !cgProp.vendorExtensions.containsKey("x-codegen-model")) {
                CodegenModel propType = cgModels.get(cgProp.complexType);
                if (propType != null)
                    cgProp.vendorExtensions.put("x-codegen-model", propType);
            }
            if (cgProp.getIsEnum() && cgProp.allowableValues != null && !cgProp.allowableValues.isEmpty()) {
                cgProp.vendorExtensions.put("x-first-enum-value", cgProp.allowableValues.values().stream().findFirst().get());
            }
            cgProp.vendorExtensions.put("x-expect-js-type", getExpectJsItemType(cgProp.baseType));
            /** todo: let's handle this with handlebar helpers
            cgProp.vendorExtensions.put("x-is-param-required", isParamRequired);
            cgProp.vendorExtensions.put("x-is-param-list-container", isParamListContainer);
            cgProp.vendorExtensions.put("x-is-param-map-container", isParamMapContainer);
            cgProp.vendorExtensions.put("x-cache-current-context", cacheCurrentContext);
            cgProp.vendorExtensions.put("x-stash-cached-contexts", stashCachedContexts);
            cgProp.vendorExtensions.put("x-execute-for-cached-contexts", executeForCachedContexts);
            cgProp.vendorExtensions.put("x-execute-for-cached-parent-context", executeForCachedParentContext);
             */
            //cgProp.vendorExtensions.put("x-indent", indent);
            boolean isContainer = cgProp.getIsListContainer() || cgProp.getIsMapContainer();
            if (isContainer) {
                cgProp.vendorExtensions.put("x-is-container", true);
                cgProp.items.vendorExtensions.put("x-is-prop-item", true);
                postProcessProperties(Collections.singleton(cgProp.items), cgModels);
            }
            // Ensure string examples are quoted.
            if (cgProp.getIsString() && cgProp.example != null // split
                    && !cgProp.example.startsWith("\"") && !cgProp.example.endsWith("\"")) {

                cgProp.example = '"' + cgProp.example + '"';
            }
        }
    }

    private String getExpectJsItemType(String baseType) {
        String type = baseType;
        if (baseType != null) {
            switch (baseType) {
            case "Boolean":
                type = "'boolean'";
                break;
            case "Number":
                type = "'number'";
                break;
            case "String":
                type = "'string'";
                break;
            }
        }
        return type;
    }

    /** todo: let's handle this with handlebar helpers
    // The api-test-param-complex and api-test-property-complex templates need to know whether a parameter is required,
    // but they cannot use normal inheritance to determine this, because CodegenProperty.required in the current context
    // shadows CodegenParameter.required in the ancestor context. Also, they can't use a vendor extension property on
    // the CodegenParameter, as compound tags are not resolved recursively. So the only way is to use Lambdas.

    private CodegenParameter cgParam;

    private Lambda cacheCurrentParam = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            cgParam = (CodegenParameter) frag.context();
            frag.execute(out);
            cgParam = null;
        }
    };

    private Lambda isParamRequired = new InvertibleLambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            if (cgParam != null && cgParam.required)
                frag.execute(out);
        }

        @Override
        public void executeInverse(Fragment frag, Writer out) throws IOException {
            if (cgParam == null || !cgParam.required)
                frag.execute(out);
        }
    };

    private Lambda isParamListContainer = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            if (cgParam != null && cgParam.isListContainer)
                frag.execute(out);
        }
    };

    private Lambda isParamMapContainer = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            if (cgParam.isMapContainer)
                frag.execute(out);
        }
    };

    private Deque<List<Object>> contexts = new ArrayDeque<>();

    {
        contexts.push(new ArrayList<>());
    }

    private Lambda cacheCurrentContext = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            List<Object> list = contexts.peek();
            list.add(frag.context());
            frag.execute(out);
            list.remove(list.size() - 1);
        }
    };

    private Lambda stashCachedContexts = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            contexts.push(new ArrayList<>());
            frag.execute(out);
            contexts.pop();
        }
    };

    private Lambda executeForCachedContexts = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            frag.execute(contexts.peek(), out);
        }
    };

    private Lambda executeForCachedParentContext = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            List<Object> list = contexts.peek();
            int index = list.indexOf(frag.context());
            if (index > 0)
                frag.execute(list.get(index - 1), out);
        }
    };

    private static final String INDENT = "  ";
    private static final String NL = System.getProperty("line.separator");

    private Lambda indent = new Lambda() {
        @Override
        public void execute(Fragment frag, Writer out) throws IOException {
            StringWriter sw = new StringWriter();
            frag.execute(sw);
            BufferedReader br = new BufferedReader(new StringReader(sw.toString()));
            String line;
            while ((line = br.readLine()) != null) {
                out.write(INDENT);
                out.write(line);
                out.write(NL);
            }
        }
    };

     @Override
     public Compiler processCompiler(Compiler compiler) {
     return super.processCompiler(compiler).escapeHTML(false);
     }
     */

    public void setLoadTestDataFromFile(boolean loadTestDataFromFile) {
        this.loadTestDataFromFile = loadTestDataFromFile;
    }

    public void setTestDataFile(File testDataFile) {
        this.testDataFile = testDataFile;
    }

    public void setPreserveLeadingParamChar(boolean preserveLeadingParamChar) {
        this.preserveLeadingParamChar = preserveLeadingParamChar;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        objs = super.postProcessModelsEnum(objs);
        List<Object> models = (List<Object>) objs.get("models");
        for (Object _mo : models) {
            Map<String, Object> mo = (Map<String, Object>) _mo;
            CodegenModel cm = (CodegenModel) mo.get("model");

            // Collect each model's required property names in *document order*.
            // NOTE: can't use 'mandatory' as it is built from ModelImpl.getRequired(), which sorts names
            // alphabetically and in any case the document order of 'required' and 'properties' can differ.
            List<CodegenProperty> required = new ArrayList<>();
            List<CodegenProperty> allRequired = supportsInheritance || supportsMixins ? new ArrayList<CodegenProperty>() : required;
            cm.vendorExtensions.put("x-required", required);
            cm.vendorExtensions.put("x-all-required", allRequired);

            for (CodegenProperty var : cm.vars) {
                // Add JSDoc @type value for this property.
                String jsDocType = getJSDocType(cm, var);
                var.vendorExtensions.put("x-jsdoc-type", jsDocType);

                if (Boolean.TRUE.equals(var.required)) {
                    required.add(var);
                }
            }

            if (supportsInheritance || supportsMixins) {
                for (CodegenProperty var : cm.allVars) {
                    if (Boolean.TRUE.equals(var.required)) {
                        allRequired.add(var);
                    }
                }
            }

            // set vendor-extension: x-codegen-hasMoreRequired
            CodegenProperty lastRequired = null;
            for (CodegenProperty var : cm.vars) {
                if (var.required) {
                    lastRequired = var;
                }
            }
            for (CodegenProperty var : cm.vars) {
                if (var == lastRequired) {
                    var.vendorExtensions.put("x-codegen-hasMoreRequired", false);
                } else if (var.required) {
                    var.vendorExtensions.put("x-codegen-hasMoreRequired", true);
                }
            }
        }
        return objs;
    }

    public void detectRecursiveModel(List<CodegenProperty> allVars, String className, Map<String, CodegenModel> allModels) {
        if (allVars == null || allVars.isEmpty()) {
            return;
        }
        for (CodegenProperty codegenProperty : allVars) {
            if (codegenProperty.getIsPrimitiveType()) {
                continue;
            }
            if (codegenProperty.getIsListContainer() || codegenProperty.getIsMapContainer()) {
                if (className.equalsIgnoreCase(codegenProperty.items.datatype)) {
                    codegenProperty.items.vendorExtensions.put("x-is-recursive-model", Boolean.TRUE);
                    continue;
                }
            }
            if (className.equalsIgnoreCase(codegenProperty.datatype)) {
                codegenProperty.vendorExtensions.put("x-is-recursive-model", Boolean.TRUE);
                continue;
            }
        }
    }

    @Override
    protected void addImport(CodegenModel model, String type) {
        if (type != null && !model.classname.equals(type) && needToImport(type)) {
            model.imports.add(type);
        }
    }

    @Override
    protected boolean needToImport(String type) {
        return !defaultIncludes.contains(type)
            && !languageSpecificPrimitives.contains(type);
    }

    private static CodegenModel reconcileInlineEnums(CodegenModel codegenModel, CodegenModel parentCodegenModel) {
        // This generator uses inline classes to define enums, which breaks when
        // dealing with models that have subTypes. To clean this up, we will analyze
        // the parent and child models, look for enums that match, and remove
        // them from the child models and leave them in the parent.
        // Because the child models extend the parents, the enums will be available via the parent.

        // Only bother with reconciliation if the parent model has enums.
        boolean hasEnums = getBooleanValue(parentCodegenModel, HAS_ENUMS_EXT_NAME);
        if (hasEnums) {

            // Get the properties for the parent and child models
            final List<CodegenProperty> parentModelCodegenProperties = parentCodegenModel.vars;
            List<CodegenProperty> codegenProperties = codegenModel.vars;

            // Iterate over all of the parent model properties
            boolean removedChildEnum = false;
            for (CodegenProperty parentModelCodegenProperty : parentModelCodegenProperties) {
                // Look for enums
                boolean isEnum = getBooleanValue(parentModelCodegenProperty, IS_ENUM_EXT_NAME);
                if (isEnum) {
                    // Now that we have found an enum in the parent class,
                    // and search the child class for the same enum.
                    Iterator<CodegenProperty> iterator = codegenProperties.iterator();
                    while (iterator.hasNext()) {
                        CodegenProperty codegenProperty = iterator.next();
                        if (getBooleanValue(codegenProperty, IS_ENUM_EXT_NAME) && codegenProperty.equals(parentModelCodegenProperty)) {
                            // We found an enum in the child class that is
                            // a duplicate of the one in the parent, so remove it.
                            iterator.remove();
                            removedChildEnum = true;
                        }
                    }
                }
            }

            if(removedChildEnum) {
                // If we removed an entry from this model's vars, we need to ensure hasMore is updated
                int count = 0, numVars = codegenProperties.size();
                for(CodegenProperty codegenProperty : codegenProperties) {
                    count += 1;
                    codegenProperty.getVendorExtensions().put(CodegenConstants.HAS_MORE_EXT_NAME, (count < numVars) ? true : false);
                }
                codegenModel.vars = codegenProperties;
            }
        }

        return codegenModel;
    }

    private static String sanitizePackageName(String packageName) { // FIXME parameter should not be assigned. Also declare it as "final"
        packageName = packageName.trim();
        packageName = packageName.replaceAll("[^a-zA-Z0-9_\\.]", "_");
        if(Strings.isNullOrEmpty(packageName)) {
            return "invalidPackageName";
        }
        return packageName;
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        return sanitizeName(camelize(property.name)) + "Enum";
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (value.length() == 0) {
            return "empty";
        }

        // for symbol, e.g. $, #
        if (getSymbolName(value) != null) {
            return (getSymbolName(value)).toUpperCase();
        }

        return toVarName(value);
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("Integer".equals(datatype) || "Number".equals(datatype) || "Boolean".equals(datatype)) {
            return value;
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }


    @Override
    public String escapeQuotationMark(String input) {
        // remove ', " to avoid code injection
        return input.replace("\"", "").replace("'", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

}
