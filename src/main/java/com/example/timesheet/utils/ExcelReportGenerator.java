package com.example.timesheet.utils;

import com.example.timesheet.models.DailyTimeSheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelReportGenerator {

    /**
     * Generates an Excel report for the given timesheet entries.
     *
     * @param baseDir     Base directory to store reports
     * @param periodLabel Label representing the period (e.g., "May-2025" or "2025-05-01_to_2025-05-15")
     * @param projectName Project name
     * @param managerName Project manager name
     * @param employeeName Employee name
     * @param entries List of DailyTimeSheet entries
     * @throws IOException if file operations fail
     */
    public static void generateExcel(String baseDir, String periodLabel, String projectName, String managerName,
                                     String employeeName, List<DailyTimeSheet> entries) throws IOException {

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Timesheet");

        int rowIdx = 0;
        sheet.createRow(rowIdx++).createCell(0).setCellValue("Project Name");
        sheet.getRow(rowIdx - 1).createCell(1).setCellValue(projectName);

        sheet.createRow(rowIdx++).createCell(0).setCellValue("Project Manager");
        sheet.getRow(rowIdx - 1).createCell(1).setCellValue(managerName);

        sheet.createRow(rowIdx++).createCell(0).setCellValue("Period");
        sheet.getRow(rowIdx - 1).createCell(1).setCellValue(periodLabel);

        rowIdx++; // empty row

        sheet.createRow(rowIdx++).createCell(0).setCellValue("Name");
        sheet.getRow(rowIdx - 1).createCell(1).setCellValue(employeeName);

        // Table header
        Row header = sheet.createRow(rowIdx++);
        String[] columns = {"S.No", "Date", "Day", "Task Description", "Time Worked (in Hr)"};
        for (int i = 0; i < columns.length; i++) header.createCell(i).setCellValue(columns[i]);

        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE");

        int count = 1;
        for (DailyTimeSheet dts : entries) {
            Row row = sheet.createRow(rowIdx++);
            row.createCell(0).setCellValue(count++);
            LocalDate date = dts.getWorkDate().toLocalDate();
            row.createCell(1).setCellValue(date.toString());
            row.createCell(2).setCellValue(date.format(dayFormatter));
            row.createCell(3).setCellValue(dts.getDescription());
            row.createCell(4).setCellValue(dts.getHoursSpent());
        }

        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Create folders based on period label and project name
        String folderPath = baseDir + "/" + periodLabel + "/" + projectName;
        new File(folderPath).mkdirs();

        String safeEmployeeName = employeeName.replace(" ", "_");
        String fileName = folderPath + "/" + safeEmployeeName + "_" + periodLabel + ".xlsx";

        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            workbook.write(fos);
        }

        workbook.close();
    }
}
