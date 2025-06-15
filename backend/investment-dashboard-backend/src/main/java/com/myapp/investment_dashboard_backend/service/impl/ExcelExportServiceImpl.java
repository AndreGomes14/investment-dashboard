package com.myapp.investment_dashboard_backend.service.impl;

import com.myapp.investment_dashboard_backend.model.Investment;
import com.myapp.investment_dashboard_backend.service.ExcelExportService;
import com.myapp.investment_dashboard_backend.utils.DataCellStyles;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class ExcelExportServiceImpl implements ExcelExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportServiceImpl.class);
    private static final String[] HEADERS = {"Ticker", "Type", "Currency", "Units", "Purchase Price", "Current Value", "Status", "Purchase Date", "Last Price Update"};

    @Override
    public byte[] createInvestmentExcel(List<Investment> investments) throws IOException {
        if (investments == null || investments.isEmpty()) {
            logger.warn("No investments provided for Excel export.");
            return createEmptyWorkbookBytes();
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Investments");

            createHeaderRow(workbook, sheet);
            createDataRows(workbook, sheet, investments);

            // --- Auto-size columns ---
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            logger.info("Successfully generated Excel file for {} investments.", investments.size());
            return out.toByteArray();

        } catch (IOException e) {
            logger.error("Error generating Excel file for investments: {}", e.getMessage(), e);
            throw new IOException("Error generating Excel file for investments", e);
        }
    }

    private byte[] createEmptyWorkbookBytes() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            logger.error("Error generating empty Excel file: {}", e.getMessage(), e);
            throw new IOException("Error generating empty Excel file", e); // Re-throw with context
        }
    }

    private void createHeaderRow(Workbook workbook, Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        CellStyle headerCellStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerCellStyle);
        }
    }

    private void createDataRows(Workbook workbook, Sheet sheet, List<Investment> investments) {
        int rowIdx = 1;
        DataCellStyles cellStyles = new DataCellStyles(workbook); // Re-instantiate DataCellStyles

        for (Investment investment : investments) {
            Row row = sheet.createRow(rowIdx++);
            populateInvestmentRow(investment, row, cellStyles); // Pass cellStyles
        }
    }

    private void populateInvestmentRow(Investment investment, Row row, DataCellStyles cellStyles) {
        createStringCell(row, 0, investment.getTicker());
        createStringCell(row, 1, investment.getType());
        createStringCell(row, 2, investment.getCurrency());

        createBigDecimalCell(row, 3, investment.getAmount(), cellStyles.numberCellStyle);
        createBigDecimalCell(row, 4, investment.getPurchasePrice(), cellStyles.currencyCellStyle);
        createBigDecimalCellWithFallback(row, 5, investment.getCurrentValue(), cellStyles.currencyCellStyle);

        String statusString = (investment.getStatus() != null) ? investment.getStatus() : "N/A";
        createStringCell(row, 6, statusString);

        createLocalDateTimeCellWithFallback(row, 7, investment.getCreatedAt(), cellStyles.dateCellStyle);
        createLocalDateTimeCellWithFallback(row, 8, investment.getLastUpdateDate(), cellStyles.dateCellStyle);
    }

    private void createStringCell(Row row, int columnIndex, String text) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(Objects.requireNonNullElse(text, ""));
    }

    private void createBigDecimalCell(Row row, int columnIndex, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void createBigDecimalCellWithFallback(Row row, int columnIndex, BigDecimal value, CellStyle valueStyle) {
        Cell cell = row.createCell(columnIndex);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(valueStyle);
        } else {
            cell.setCellValue("N/A");
        }
    }

    private void createLocalDateTimeCellWithFallback(Row row, int columnIndex, LocalDateTime dateTime, CellStyle valueStyle) {
        Cell cell = row.createCell(columnIndex);
        if (dateTime != null) {
            cell.setCellValue(dateTime); // POI handles LocalDateTime
            cell.setCellStyle(valueStyle);
        } else {
            cell.setCellValue("N/A");
        }
    }
}