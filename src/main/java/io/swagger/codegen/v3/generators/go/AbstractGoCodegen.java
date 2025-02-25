package io.swagger.codegen.v3.generators.go;

import io.swagger.codegen.v3.CliOption;
import io.swagger.codegen.v3.CodegenConstants;
import io.swagger.codegen.v3.CodegenContent;
import io.swagger.codegen.v3.CodegenModel;
import io.swagger.codegen.v3.CodegenOperation;
import io.swagger.codegen.v3.CodegenParameter;
import io.swagger.codegen.v3.CodegenProperty;
import io.swagger.codegen.v3.ISchemaHandler;
import io.swagger.codegen.v3.generators.DefaultCodegenConfig;
import io.swagger.codegen.v3.generators.SchemaHandler;
import io.swagger.codegen.v3.generators.util.OpenAPIUtil;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import static io.swagger.codegen.v3.generators.handlebars.ExtensionHelper.getBooleanValue;

public abstract class AbstractGoCodegen extends DefaultCodegenConfig {

    protected static Logger LOGGER = LoggerFactory.getLogger(AbstractGoCodegen.class);

    protected boolean withXml = false;

    protected String packageName = "swagger";

    public AbstractGoCodegen() {
        super();

        hideGenerationTimestamp = Boolean.FALSE;

        defaultIncludes = new HashSet<>(
            Arrays.asList(
                "map",
                "array")
            );

        languageSpecificPrimitives = new HashSet<>(
            Arrays.asList(
                "string",
                "bool",
                "uint",
                "uint32",
                "uint64",
                "int",
                "int32",
                "int64",
                "float32",
                "float64",
                "complex64",
                "complex128",
                "rune",
                "byte")
            );

        instantiationTypes.clear();
        /*instantiationTypes.put("array", "GoArray");
        instantiationTypes.put("map", "GoMap");*/

        typeMapping.clear();
        typeMapping.put("integer", "int32");
        typeMapping.put("long", "int64");
        typeMapping.put("number", "float32");
        typeMapping.put("float", "float32");
        typeMapping.put("double", "float64");
        typeMapping.put("BigDecimal", "float64");
        typeMapping.put("boolean", "bool");
        typeMapping.put("string", "string");
        typeMapping.put("UUID", "string");
        typeMapping.put("date", "string");
        typeMapping.put("DateTime", "time.Time");
        typeMapping.put("password", "string");
        typeMapping.put("File", "*os.File");
        typeMapping.put("file", "*os.File");
        // map binary to string as a workaround
        // the correct solution is to use []byte
        typeMapping.put("binary", "*os.File");
        typeMapping.put("ByteArray", "string");
        typeMapping.put("object", "interface{}");
        typeMapping.put("UUID", "string");

        importMapping = new HashMap<>();

        cliOptions.clear();
        cliOptions.add(new CliOption(CodegenConstants.PACKAGE_NAME, "Go package name (convention: lowercase).")
                .defaultValue("swagger"));

        cliOptions.add(new CliOption(CodegenConstants.HIDE_GENERATION_TIMESTAMP, CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC)
                .defaultValue(Boolean.TRUE.toString()));
    }

    /**
     * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
     * those terms here.  This logic is only called if a variable matches the reserved words
     *
     * @return the escaped term
     */
    @Override
    public String escapeReservedWord(String name) {
        // Can't start with an underscore, as our fields need to start with an
        // UppercaseLetter so that Go treats them as public/visible.

        // Options?
        // - MyName
        // - AName
        // - TheName
        // - XName
        // - X_Name
        // ... or maybe a suffix?
        // - Name_ ... think this will work.
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return camelize(name) + '_';
    }

    @Override
    public String toVarName(String name) {
        // replace - with _ e.g. created-at => created_at
        name = sanitizeName(name.replaceAll("-", "_"));

        // if it's all uppper case, do nothing
        if (name.matches("^[A-Z_]*$"))
            return name;

        // camelize (lower first character) the variable name
        // pet_id => PetId
        name = camelize(name);

        // for reserved word or word starting with number, append _
        if (isReservedWord(name))
            name = escapeReservedWord(name);

        // for reserved word or word starting with number, append _
        if (name.matches("^\\d.*"))
            name = "Var" + name;

        return name;
    }

    @Override
    public String toParamName(String name) {
        // params should be lowerCamelCase. E.g. "person Person", instead of
        // "Person Person".
        //
        // REVISIT: Actually, for idiomatic go, the param name should
        // really should just be a letter, e.g. "p Person"), but we'll get
        // around to that some other time... Maybe.
        return camelize(toVarName(name), true);
    }

    @Override
    public String toModelName(String name) {
        // camelize the model name
        // phone_number => PhoneNumber
        return camelize(toModel(name));
    }

    @Override
    public String toModelFilename(String name) {
        return toModel("model_" + name);
    }

    public String toModel(String name) {
        if (!StringUtils.isEmpty(modelNamePrefix) &&
            !name.toLowerCase().startsWith(modelNamePrefix.toLowerCase())) {
            name = modelNamePrefix + "_" + name;
        }

        if (!StringUtils.isEmpty(modelNameSuffix) &&
            !name.toLowerCase().endsWith(modelNameSuffix.toLowerCase())) {
            name = name + "_" + modelNameSuffix;
        }

        name = sanitizeName(name);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(name)) {
            LOGGER.warn(name + " (reserved word) cannot be used as model name. Renamed to " + ("model_" + name));
            name = "model_" + name; // e.g. return => ModelReturn (after camelize)
        }

        // model name starts with number
        if (name.matches("^\\d.*")) {
            LOGGER.warn(name + " (model name starts with number) cannot be used as model name. Renamed to "
                    + ("model_" + name));
            name = "model_" + name; // e.g. 200Response => Model200Response (after camelize)
        }

        return underscore(name);
    }

    @Override
    public String toApiFilename(String name) {
        // replace - with _ e.g. created-at => created_at
        name = name.replaceAll("-", "_"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        // e.g. PetApi.go => pet_api.go
        return "api_" + underscore(name);
    }

    /**
     * Overrides postProcessParameter to add a vendor extension "x-exportParamName".
     * This is useful when paramName starts with a lowercase letter, but we need that
     * param to be exportable (starts with an Uppercase letter).
     *
     * @param parameter CodegenParameter object to be processed.
     */
    @Override
    public void postProcessParameter(CodegenParameter parameter) {

        // Give the base class a chance to process
        super.postProcessParameter(parameter);

        char nameFirstChar = parameter.paramName.charAt(0);
        if (Character.isUpperCase(nameFirstChar)) {
            // First char is already uppercase, just use paramName.
            parameter.vendorExtensions.put("x-exportParamName", parameter.paramName);
        } else {
            // It's a lowercase first char, let's convert it to uppercase
            StringBuilder sb = new StringBuilder(parameter.paramName);
            sb.setCharAt(0, Character.toUpperCase(nameFirstChar));
            parameter.vendorExtensions.put("x-exportParamName", sb.toString());
        }
    }

    @Override
    public String getTypeDeclaration(Schema schema) {
        if(schema instanceof ArraySchema) {
            ArraySchema arraySchema = (ArraySchema) schema;
            Schema inner = arraySchema.getItems();
            if (inner instanceof ComposedSchema && schema.getExtensions() != null && schema.getExtensions().containsKey("x-schema-name")) {
                final ComposedSchema composedSchema = (ComposedSchema) inner;
                final String prefix;
                if (composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
                    prefix = SchemaHandler.ALL_OF_PREFFIX;
                } else if (composedSchema.getOneOf() != null && !composedSchema.getOneOf().isEmpty()) {
                    prefix = SchemaHandler.ONE_OF_PREFFIX;
                } else {
                    prefix = SchemaHandler.ANY_OF_PREFFIX;
                }
                return "[]" + toModelName(prefix + schema.getExtensions().remove("x-schema-name")) + SchemaHandler.ARRAY_ITEMS_SUFFIX;
            } else {
                return "[]" + getTypeDeclaration(inner);
            }
        } else if (schema instanceof MapSchema && hasSchemaProperties(schema)) {
            MapSchema mapSchema = (MapSchema) schema;
            Schema inner = (Schema) mapSchema.getAdditionalProperties();

            return getSchemaType(schema) + "[string]" + getTypeDeclaration(inner);
        } else if (schema.get$ref() != null) {
            final String simpleRefName = OpenAPIUtil.getSimpleRef(schema.get$ref());
            final Schema refSchema = OpenAPIUtil.getSchemaFromName(simpleRefName, this.openAPI);
            if (refSchema != null && (refSchema instanceof ArraySchema || refSchema instanceof MapSchema)) {
                refSchema.addExtension("x-schema-name", simpleRefName);
                return getTypeDeclaration(refSchema);
            }
        }
        // Not using the supertype invocation, because we want to UpperCamelize
        // the type.
        String schemaType = getSchemaType(schema);
        if (typeMapping.containsKey(schemaType)) {
            return typeMapping.get(schemaType);
        }
        if(typeMapping.containsValue(schemaType)) {
            return schemaType;
        }
        if(languageSpecificPrimitives.contains(schemaType)) {
            return schemaType;
        }
        return toModelName(schemaType);
    }

    @Override
    public String getSchemaType(Schema schema) {
        String schemaType = super.getSchemaType(schema);

        if (schema.get$ref() != null) {
            final Schema refSchema = OpenAPIUtil.getSchemaFromName(schemaType, this.openAPI);
            if (refSchema != null && !isObjectSchema(refSchema) && refSchema.getEnum() == null) {
                schemaType = super.getSchemaType(refSchema);
            }
        }

        String type;
        if(typeMapping.containsKey(schemaType)) {
            type = typeMapping.get(schemaType);
            if(languageSpecificPrimitives.contains(type)) {
                return type;
            }
        } else {
            type = schemaType;
        }
        return type;
    }

    @Override
    public String toOperationId(String operationId) {
        String sanitizedOperationId = sanitizeName(operationId);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(sanitizedOperationId)) {
            LOGGER.warn(operationId + " (reserved word) cannot be used as method name. Renamed to "
                    + camelize("call_" + operationId));
            sanitizedOperationId = "call_" + sanitizedOperationId;
        }
        return camelize(sanitizedOperationId);
    }

    @Override
    public Map<String, Object> postProcessOperations(Map<String, Object> objs) {
        @SuppressWarnings("unchecked")
        Map<String, Object> objectMap = (Map<String, Object>) objs.get("operations");
        @SuppressWarnings("unchecked")
        List<CodegenOperation> operations = (List<CodegenOperation>) objectMap.get("operation");
        for (CodegenOperation operation : operations) {
            // http method verb conversion (e.g. PUT => Put)
            operation.httpMethod = camelize(operation.httpMethod.toLowerCase());
        }

        // remove model imports to avoid error
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        if (imports == null)
            return objs;

        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(apiPackage()))
                iterator.remove();
        }

        // this will only import "fmt" if there are items in pathParams
        for (CodegenOperation operation : operations) {
            if (operation.pathParams != null && operation.pathParams.size() > 0) {
                imports.add(createMapping("import", "fmt"));
                break; //just need to import once
            }
        }

        boolean addedOptionalImport = false;
        boolean addedTimeImport = false;
        boolean addedOSImport = false;
        for (CodegenOperation operation : operations) {
            for (CodegenContent codegenContent : operation.getContents()) {
                for (CodegenParameter param : codegenContent.getParameters()) {
                    // import "os" if the operation uses files
                    if (!addedOSImport && param.dataType == "*os.File") {
                        imports.add(createMapping("import", "os"));
                        addedOSImport = true;
                    }

                    // import "time" if the operation has a required time parameter.
                    if (param.required) {
                        if (!addedTimeImport && param.dataType == "time.Time") {
                            imports.add(createMapping("import", "time"));
                            addedTimeImport = true;
                        }
                    } else {
                        if (!addedOptionalImport) {
                            imports.add(createMapping("import", "github.com/antihax/optional"));
                            addedOptionalImport = true;
                        }
                        // We need to specially map Time type to the optionals package
                        if (param.dataType == "time.Time") {
                            param.vendorExtensions.put("x-optionalDataType", "Time");
                            continue;
                        }
                        // Map optional type to dataType
                        param.vendorExtensions.put("x-optionalDataType", param.dataType.substring(0, 1).toUpperCase() + param.dataType.substring(1));
                    }
                }
            }
        }

        // recursively add import for mapping one type to multiple imports
        List<Map<String, String>> recursiveImports = (List<Map<String, String>>) objs.get("imports");
        if (recursiveImports == null)
            return objs;

        ListIterator<Map<String, String>> listIterator = imports.listIterator();
        while (listIterator.hasNext()) {
            String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (importMapping.containsKey(_import)) {
                listIterator.add(createMapping("import", importMapping.get(_import)));
            }
        }

        return objs;
    }

    @Override
    public Map<String, Object> postProcessModels(Map<String, Object> objs) {
        // remove model imports to avoid error
        List<Map<String, String>> imports = (List<Map<String, String>>) objs.get("imports");
        final String prefix = modelPackage();
        Iterator<Map<String, String>> iterator = imports.iterator();
        while (iterator.hasNext()) {
            String _import = iterator.next().get("import");
            if (_import.startsWith(prefix))
                iterator.remove();
        }

        boolean addedTimeImport = false;
        boolean addedOSImport = false;
        List<Map<String, Object>> models = (List<Map<String, Object>>) objs.get("models");
        for (Map<String, Object> m : models) {
            Object v = m.get("model");
            if (v instanceof CodegenModel) {
                CodegenModel model = (CodegenModel) v;
                for (CodegenProperty param : model.vars) {
                    if (!addedTimeImport && param.baseType == "time.Time") {
                        imports.add(createMapping("import", "time"));
                        addedTimeImport = true;
                    }
                    if (!addedOSImport && param.baseType == "*os.File") {
                        imports.add(createMapping("import", "os"));
                        addedOSImport = true;
                    }
                }
            }
        }
        // recursively add import for mapping one type to multiple imports
        List<Map<String, String>> recursiveImports = (List<Map<String, String>>) objs.get("imports");
        if (recursiveImports == null)
            return objs;

        ListIterator<Map<String, String>> listIterator = imports.listIterator();
        while (listIterator.hasNext()) {
            String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (importMapping.containsKey(_import)) {
                listIterator.add(createMapping("import", importMapping.get(_import)));
            }
        }

        return postProcessModelsEnum(objs);
    }

    @Override
    public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
        OpenAPI openAPI = (OpenAPI)objs.get("openAPI");
        if(openAPI != null) {
            try {
                objs.put("swagger-yaml", Yaml.mapper().writeValueAsString(openAPI));
            } catch (JsonProcessingException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
        return super.postProcessSupportingFileData(objs);
    }

    @Override
    protected boolean needToImport(String type) {
        return !defaultIncludes.contains(type) && !languageSpecificPrimitives.contains(type);
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    public Map<String, String> createMapping(String key, String value) {
        Map<String, String> customImport = new HashMap<String, String>();
        customImport.put(key, value);

        return customImport;
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            return value;
        } else {
            return escapeText(value);
        }
    }

    @Override
    public String toEnumDefaultValue(String value, String datatype) {
        return datatype + "_" + value;
    }

    @Override
    public String toEnumVarName(String name, String datatype) {
        if (name.length() == 0) {
            return "EMPTY";
        }

        // number
        if ("int".equals(datatype) || "double".equals(datatype) || "float".equals(datatype)) {
            String varName = name;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // for symbol, e.g. $, #
        if (getSymbolName(name) != null) {
            return getSymbolName(name).toUpperCase();
        }

        // string
        String enumName = sanitizeName(underscore(name).toUpperCase());
        enumName = enumName.replaceFirst("^_", "");
        enumName = enumName.replaceFirst("_$", "");

        if (isReservedWord(enumName) || enumName.matches("\\d.*")) { // reserved word or starts with number
            return escapeReservedWord(enumName);
        } else {
            return enumName;
        }
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        String enumName = underscore(toModelName(property.name)).toUpperCase();

        // remove [] for array or map of enum
        enumName = enumName.replace("[]", "");

        if (enumName.matches("\\d.*")) { // starts with number
            return "_" + enumName;
        } else {
            return enumName;
        }
    }

    public void setWithXml(boolean withXml) {
        this.withXml = withXml;
    }

    @Override
    public CodegenModel fromModel(String name, Schema schema, Map<String, Schema> allDefinitions) {
        final CodegenModel codegenModel = super.fromModel(name, schema, allDefinitions);
        if (!getBooleanValue(codegenModel, CodegenConstants.IS_ALIAS_EXT_NAME)) {
            boolean isAlias = schema instanceof ArraySchema
                    || schema instanceof MapSchema
                    || (!isObjectSchema(schema) && schema.getEnum() == null);

            codegenModel.getVendorExtensions().put(CodegenConstants.IS_ALIAS_EXT_NAME, isAlias);
        }



        return codegenModel;
    }

    @Override
    public ISchemaHandler getSchemaHandler() {
        return new GoSchemaHandler(this);
    }

    @Override
    public boolean checkAliasModel() {
        return true;
    }
}
