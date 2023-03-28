package org.example;

import org.example.pojo.API;
import org.example.utils.ExcelUtil;
import org.example.utils.YAMLReaderUtil;

import java.io.IOException;

public class App
{
    public static void main( String[] args ) throws IOException {
        API api = YAMLReaderUtil.read("src/main/resources/input/api.yaml");
        ExcelUtil.saveToExcel(api, "src/main/resources/output/api.xlsx");
    }
}
