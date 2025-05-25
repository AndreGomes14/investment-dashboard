package com.myapp.investment_dashboard_backend.utils;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Workbook;

public class DataCellStyles {
    public final CellStyle dateCellStyle;
    public final CellStyle currencyCellStyle;
    public final CellStyle numberCellStyle;

    public DataCellStyles(Workbook workbook) {
        CreationHelper createHelper = workbook.getCreationHelper();

        this.dateCellStyle = workbook.createCellStyle();
        this.dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

        this.currencyCellStyle = workbook.createCellStyle();
        this.currencyCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,#0.00"));

        this.numberCellStyle = workbook.createCellStyle();
        this.numberCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,#0.0000"));
    }
} 