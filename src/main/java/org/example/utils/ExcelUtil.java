package org.example.utils;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.pojo.API;
import org.example.pojo.Path;
import org.example.pojo.Schema;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.example.utils.YAMLReaderUtil.getSchemaByRef;

public class ExcelUtil {

    public static void saveToExcel(API api, String s) throws IOException {
        XSSFWorkbook workbook = new XSSFWorkbook();

        for (Path path : api.getPaths()) {
            AtomicInteger rownum = new AtomicInteger(0);
            XSSFSheet sheet = workbook.createSheet(path.getUri().replace("/", ""));

            path.getMethods().forEach(method -> {
                fillRow(sheet, rownum.getAndIncrement(), 0, 1, "HTTP-method: " + method.getName().toUpperCase());
                fillRow(sheet, rownum.getAndIncrement(), 0, 1, "Parameters: " + method.getParameters().stream().map(Path.Parameter::getName).collect(Collectors.joining()));
                fillRow(sheet, rownum.getAndIncrement(), 0, 1, "Description: " + (method.getDescription() != null ? method.getDescription() : method.getSummary()));

                fillRow(sheet, rownum.getAndIncrement(), 0, 1);
                fillRow(sheet, rownum.getAndIncrement(), 0, 1, "REQUEST");
                if (method.getRequestBody() != null) {
                    int maxNestingLevel = getNestingLevel(api, method.getRequestBody().getContent().getSchema(), 1, 1);
                    fillRow(sheet, rownum.getAndIncrement(), 0, maxNestingLevel,"Name", "Type", "Max length", "Default value", "Enum", "Description");
                    fillRowBySchema(api, sheet, rownum, new AtomicInteger(0), maxNestingLevel, method.getRequestBody().getContent().getSchema(), false);
                } else {
                    fillRow(sheet, rownum.getAndIncrement(), 0, 1, "EMPTY");
                }

                fillRow(sheet, rownum.getAndIncrement(), 0, 1);
                fillRow(sheet, rownum.getAndIncrement(), 0, 1);
                fillRow(sheet, rownum.getAndIncrement(), 0, 1, "RESPONSES");
                if (method.getResponses() != null && !method.getResponses().isEmpty()) {
                    method.getResponses().forEach(response -> {
                        fillRow(sheet, rownum.getAndIncrement(), 0, 1, "Code: " + response.getCode());
                        fillRow(sheet, rownum.getAndIncrement(), 0, 1, "Description: " + response.getDescription());
                        int nestingLevel = getNestingLevel(api, response.getContent().getSchema(), 1, 1);
                        fillRow(sheet, rownum.getAndIncrement(), 0, nestingLevel,"Name", "Type", "Max length", "Default value", "Enum", "Description");
                        fillRowBySchema(api, sheet, rownum, new AtomicInteger(0), nestingLevel, response.getContent().getSchema(), true);
                        fillRow(sheet, rownum.getAndIncrement(), 0, 1);
                    });
                }
            });
            for (int i = 0; i < 50; i++) {
                sheet.autoSizeColumn(i);
            }

        }
        FileOutputStream stream = new FileOutputStream(s);
        workbook.write(stream);
        workbook.close();
    }

//    private static int getNestingLevel(API api, Schema schema, int initNestingLevel, int maxNestingLevel) {
//        if (schema == null) return initNestingLevel;
//
//        int nestingLevel = 0;
//        if (Objects.nonNull(schema.getProperties())) {
//            nestingLevel = getNestingLevel(api, schema.getProperties(), initNestingLevel, maxNestingLevel);
//        } else if (Objects.nonNull(schema.getAllOf())) {
//            nestingLevel = getNestingLevel(api, schema.getAllOf(), initNestingLevel, maxNestingLevel);
//        } else if (Objects.nonNull(schema.getOneOf())) {
//            nestingLevel = getNestingLevel(api, schema.getOneOf(), initNestingLevel, maxNestingLevel);
//        }
//
//        return Math.max(nestingLevel, maxNestingLevel);
//    }

    private static int getNestingLevel(API api, List<Schema> nested, int initNestingLevel, int maxNestingLevel) {
        int nestingLevel = initNestingLevel;
        for (Schema schema : nested) {

            if (schema.getRef() != null) {
                nestingLevel = getNestingLevel(api, api.getSchemasMap().get(schema.getRef()), ++nestingLevel, maxNestingLevel);
            } else if (Objects.nonNull(schema.getProperties())) {
                nestingLevel = getNestingLevel(api, schema.getProperties(), ++nestingLevel, maxNestingLevel);
            } else if (Objects.nonNull(schema.getItems())) {
                nestingLevel = getNestingLevel(api, schema.getItems(), ++nestingLevel, maxNestingLevel);
            } else if (!schema.getAllOf().isEmpty()) {
                nestingLevel = getNestingLevel(api, schema.getAllOf(), ++nestingLevel, maxNestingLevel);
            } else if (!schema.getOneOf().isEmpty()) {
                nestingLevel = getNestingLevel(api, schema.getOneOf(), ++nestingLevel, maxNestingLevel);
            }
            if (nestingLevel > maxNestingLevel) {
                maxNestingLevel = nestingLevel;
            }
            nestingLevel = initNestingLevel;
        }
        return Math.max(nestingLevel, maxNestingLevel);
    }

    private static int getNestingLevel(API api, Schema schema, int initNestingLevel, int maxNestingLevel) {
        if (schema == null) return initNestingLevel;
        return getNestingLevel(api, List.of(schema), initNestingLevel, maxNestingLevel);
    }

    /**
     * Method for writing schema into Excel sheet
     * @param api api value
     * @param sheet current sheet
     * @param columnNum column number for starting the row, as usual is current nesting level
     * @param maxNestingLevel maximum value of nesting level
     * @param rowNum row number to write in
     * @param schema schema to write
     */
    private static void fillRowBySchema(API api, XSSFSheet sheet, AtomicInteger rowNum, AtomicInteger columnNum, int maxNestingLevel, Schema schema, boolean skipFill) {
        if (schema == null) {
            return;
        }
        if (schema.getRef() != null) {
            schema = getSchemaByRef(api, schema.getRef());
        }

        if (!skipFill && Objects.nonNull(schema.getName())) {
            fillRow(sheet, rowNum, columnNum, maxNestingLevel, schema);
        }

        parseSchema(api, sheet, rowNum, columnNum, maxNestingLevel, schema, skipFill);
    }

    /**
     * Method allows to skip filling the row
     * @param api
     * @param sheet
     * @param rowNum
     * @param columnNum
     * @param maxNestingLevel
     * @param schema
     * @param skipFill
     */
    private static void parseSchema(API api, XSSFSheet sheet, AtomicInteger rowNum, AtomicInteger columnNum, int maxNestingLevel, Schema schema, boolean skipFill) {
        switch (schema.getType()) {
            case ("object") -> {
                fillRowBySchema(api, sheet, rowNum, columnNum, maxNestingLevel, schema.getProperties(), skipFill);
            }
            case ("array") -> {
                Schema item = schema.getItems().get(0);
                if (Objects.nonNull(item.getRef())) {
                    Schema schemaOfItem = getSchemaByRef(api, item.getRef());
                    parseSchema(api, sheet, rowNum, columnNum, maxNestingLevel, schemaOfItem, false);
                } else {
                    parseSchema(api, sheet, rowNum, columnNum, maxNestingLevel, item, false);
                }
            }
            case ("oneOf") -> {
                fillRowBySchema(api, sheet, rowNum, columnNum, maxNestingLevel, schema.getOneOf(), false);
            }
            case ("allOf") -> {
                fillRowBySchema(api, sheet, rowNum, columnNum, maxNestingLevel, schema.getAllOf(), false);
            }
        }
    }


    private static void fillRowBySchema(API api, XSSFSheet sheet, AtomicInteger rowNum, AtomicInteger columnNum, int maxNestingLevel, List<Schema> schemas, boolean skipFill) {
        if (Objects.isNull(schemas)) return;

        int startRowNum = rowNum.get();
        if (!skipFill) {
            columnNum.incrementAndGet();
        }

        schemas.forEach(s -> {
            fillRowBySchema(api, sheet, rowNum, columnNum, maxNestingLevel, s, false);
        });

        if (columnNum.get() > 0) {
            sheet.groupRow(startRowNum, rowNum.get());
        }
        if (!skipFill) {
            columnNum.decrementAndGet();
        }
    }

    private static boolean fillRow(XSSFSheet sheet, AtomicInteger rowNum, AtomicInteger columnNum, int maxNestingLevel, Schema schema) {
        return fillRow(sheet,
                rowNum.getAndIncrement(),
                columnNum.get(),
                maxNestingLevel,
                schema.getName(),
                schema.getType(),
                schema.getMaxLength() == null ? null : schema.getMaxLength().toString(),
                schema.getENum() == null ? null : schema.getENum().toString(),
                schema.getDefaultValue(),
                schema.getDescription());
    }

    private static boolean fillRow(XSSFSheet sheet, int rowNum, int startColumn, int nestingLevel, String... args) {
        XSSFRow row = sheet.createRow(rowNum);
        if (args.length == 0) {
            row.createCell(0);
        } else {
            row.createCell(startColumn).setCellValue(args[0]);
            for (int i = nestingLevel; i < (args.length - 1 + nestingLevel); i++) {
                row.createCell(i).setCellValue(args[(i + 1) - nestingLevel]);
            }
        }
        if ((nestingLevel - 1 - startColumn) > 0 && args.length > 1) { //merge empty columns
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), startColumn, nestingLevel - 1));
        }
        return true;
    }

}
