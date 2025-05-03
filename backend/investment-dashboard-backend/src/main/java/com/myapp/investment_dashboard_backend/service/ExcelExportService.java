package com.myapp.investment_dashboard_backend.service;

import com.myapp.investment_dashboard_backend.model.Investment;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ExcelExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExcelExportService.class);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public byte[] createInvestmentExcel(List<Investment> investments) throws IOException {
        if (investments == null || investments.isEmpty()) {
            logger.warn("No investments provided for Excel export.");
            // Correctly handle empty workbook byte generation
            try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                workbook.write(out);
                return out.toByteArray();
            } catch (IOException e) {
                logger.error("Error generating empty Excel file: {}", e.getMessage(), e);
                throw e;
            }
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Investments");

            // --- Create    Header Row ---
            String[] headers = {"Ticker", "Type", "Currency", "Units", "Purchase Price", "Current Value", "Status", "Purchase Date", "Last Price Update"};
            Row headerRow = sheet.createRow(0);
            CellStyle headerCellStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerCellStyle);
            }

            // --- Create Data Rows ---
            int rowIdx = 1;
            CellStyle dateCellStyle = workbook.createCellStyle();
            CreationHelper createHelper = workbook.getCreationHelper();
            dateCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));

            CellStyle currencyCellStyle = workbook.createCellStyle();
            // Adjust format based on currency if needed, simple number format for now
            currencyCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,#0.00"));

            CellStyle numberCellStyle = workbook.createCellStyle();
            numberCellStyle.setDataFormat(createHelper.createDataFormat().getFormat("#,#0.0000")); // Example for amount


            for (Investment investment : investments) {
                Row row = sheet.createRow(rowIdx++);

                row.createCell(0).setCellValue(investment.getTicker());
                row.createCell(1).setCellValue(investment.getType());
                row.createCell(2).setCellValue(investment.getCurrency());

                Cell amountCell = row.createCell(3);
                if(investment.getAmount() != null) amountCell.setCellValue(investment.getAmount().doubleValue());
                amountCell.setCellStyle(numberCellStyle);

                Cell purchasePriceCell = row.createCell(4);
                if(investment.getPurchasePrice() != null) purchasePriceCell.setCellValue(investment.getPurchasePrice().doubleValue());
                purchasePriceCell.setCellStyle(currencyCellStyle);

                Cell currentValueCell = row.createCell(5);
                if(investment.getCurrentValue() != null) currentValueCell.setCellValue(investment.getCurrentValue().doubleValue()); else currentValueCell.setCellValue("N/A");
                currentValueCell.setCellStyle(currencyCellStyle);

                String statusString = "N/A";
                if (investment.getStatus() != null) {
                    statusString = investment.getStatus();
                }
                row.createCell(6).setCellValue(statusString);

                Cell purchaseDateCell = row.createCell(7);
                if (investment.getCreatedAt() != null) {
                    purchaseDateCell.setCellValue(investment.getCreatedAt());
                    purchaseDateCell.setCellStyle(dateCellStyle);
                } else {
                    purchaseDateCell.setCellValue("N/A");
                }

                Cell lastUpdateCell = row.createCell(8);
                if (investment.getLastUpdateDate() != null) {
                    lastUpdateCell.setCellValue(investment.getLastUpdateDate());
                    lastUpdateCell.setCellStyle(dateCellStyle);
                } else {
                    lastUpdateCell.setCellValue("N/A");
                }
            }

            // --- Auto-size columns ---
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            logger.info("Successfully generated Excel file for {} investments.", investments.size());
            return out.toByteArray();

        } catch (IOException e) {
            logger.error("Error generating Excel file: {}", e.getMessage(), e);
            throw e; // Re-throw exception to be handled by controller advice
        }
    }
}