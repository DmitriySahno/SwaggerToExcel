package org.example.utils;

import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.pojo.API;
import org.example.pojo.Path;
import org.example.pojo.Schema;
import org.example.pojo.Schema.Property;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;
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
                    int nestingLevel = getNestingLevel(api, method.getRequestBody().getContent().getSchema(), 1, 1);
                    fillRow(sheet, rownum.getAndIncrement(), 0, nestingLevel,"Name", "Type", "Max length", "Default value", "Enum", "Description");
                    fillRowBySchema(api, sheet, 0, nestingLevel, rownum, method.getRequestBody().getContent().getSchema(), null, null);
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
                        fillRowBySchema(api, sheet, 0, nestingLevel, rownum, response.getContent().getSchema(), null, null);
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

    private static int getNestingLevel(API api, Schema schema, int initNestingLevel, int maxNestingLevel) {
        if (schema == null) return initNestingLevel;
        int nestingLevel = getNestingLevelByProperties(api, schema.getProperties(), initNestingLevel, maxNestingLevel);
        return Math.max(nestingLevel, maxNestingLevel);
    }

    private static int getNestingLevelByProperties(API api, Set<Property> properties, int initNestingLevel, int maxNestingLevel) {
        int nestingLevel = initNestingLevel;
        for (Property property : properties) {
            if (property.getSchema() != null || property.getSchemaRef() != null) {
                nestingLevel = getNestingLevel(api, property.getSchema() != null ? property.getSchema() : api.getSchemasMap().get(property.getSchemaRef()), ++nestingLevel, maxNestingLevel);
            } else if (property.getProperties() != null && !property.getProperties().isEmpty()) {
                nestingLevel = getNestingLevelByProperties(api, property.getProperties(), ++nestingLevel, maxNestingLevel);
            }
            if (nestingLevel > maxNestingLevel) {
                maxNestingLevel = nestingLevel;
            }
            nestingLevel = initNestingLevel;
        }
        return Math.max(nestingLevel, maxNestingLevel);
    }

    private static void fillRowBySchema(API api, XSSFSheet sheet, int startColumn, int nestingLevel, AtomicInteger rownum, Schema schema, String parentName, String parentType) {
        if (schema == null) return;
        AtomicInteger currentNestingLevel = new AtomicInteger(startColumn);
        if (parentName != null) {
            fillRow(sheet, rownum.getAndIncrement(), currentNestingLevel.getAndIncrement(), nestingLevel, parentName,
                    parentType != null && parentType.equals("array") ? "array[" + schema.getType() + "]" : schema.getType(), null, null, schema.getENum() == null ? null : schema.getENum().toString(), schema.getDescription());
        }
        int startRowNum = rownum.get();
        fillRowByProperties(api, sheet, nestingLevel, rownum, schema.getProperties(), currentNestingLevel);
        int endRowNum = rownum.get();
        if (currentNestingLevel.get() > 0) {
            sheet.groupRow(startRowNum, endRowNum);
        }
        currentNestingLevel.decrementAndGet();
    }

    private static void fillRowByProperties(API api, XSSFSheet sheet, int nestingLevel, AtomicInteger rownum, LinkedHashSet<Property> properties, AtomicInteger currentNestingLevel) {
        properties.forEach(p -> {
            if (p.getSchema() == null && p.getSchemaRef() == null) {
                fillRow(sheet, rownum.getAndIncrement(), currentNestingLevel.get(), nestingLevel,
                        p.getName(),
                        p.getType(),
                        p.getMaxLength() == null ? null : p.getMaxLength().toString(),
                        p.getDefaultValue(),
                        p.getENum() == null ? null : p.getENum().toString(),
                        p.getDescription());
            } else {
                fillRowBySchema(api, sheet, currentNestingLevel.get(), nestingLevel, rownum, p.getSchemaRef() == null ? p.getSchema() : getSchemaByRef(api, p.getSchemaRef()), p.getName(), p.getType());
            }
            if (p.getProperties() != null && !p.getProperties().isEmpty()) {
                currentNestingLevel.incrementAndGet();
                fillRowByProperties(api, sheet, nestingLevel, rownum, p.getProperties(), currentNestingLevel);
                currentNestingLevel.decrementAndGet();
            }
        });
    }

    private static void fillRow(XSSFSheet sheet, int rowNum, int startColumn, int nestingLevel, String... args) {
        XSSFRow row = sheet.createRow(rowNum);
        if (args.length == 0) {
            row.createCell(0);
        } else {
            row.createCell(startColumn).setCellValue(args[0]);
            for (int i = nestingLevel; i < (args.length - 1 + nestingLevel); i++) {
                row.createCell(i).setCellValue(args[(i + 1) - nestingLevel]);
            }
        }
        if ((nestingLevel - 1 - startColumn) > 0 && args.length > 1) {
            sheet.addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), startColumn, nestingLevel - 1));
        }
    }

}
