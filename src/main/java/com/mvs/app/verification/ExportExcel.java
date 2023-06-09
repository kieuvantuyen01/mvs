package com.mvs.app.verification;

import com.mvs.app.verification.report.VerificationReport;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.write.*;
import jxl.write.biff.RowsExceededException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ExportExcel {
    static WritableWorkbook workbook = null;
    static WritableSheet sheet = null;

    public ExportExcel() throws IOException {
        workbook = Workbook.createWorkbook(new File("mvs Report.xls"));
        sheet = workbook.createSheet("floats-cdfpl", 0);
    }

    //TODO constructor for new export
    public ExportExcel(String fileName) throws IOException {
        if (fileName.equals("")) {
            fileName = "mvs Export.xls";
        }
        workbook = Workbook.createWorkbook(new File(fileName));
        sheet = workbook.createSheet("floats-cdfpl", 0);

    }

    private static double roundAvoid(double value, int places) {
        double scale = Math.pow(10, places);
        return Math.round(value * scale) / scale;
    }

    /**
     * new export excel for each row in excel file
     *
     * @param reportList
     * @throws IOException
     * @throws RowsExceededException
     * @throws WriteException
     */
    public void writeExcel(List<VerificationReport> reportList)
            throws IOException, RowsExceededException, WriteException {

        try {
            int numberOfRow = 4;

            //TODO: title of excel export
            sheet.addCell(new Label(5, 0, "Ket qua thuc nghiem")); // add a String to cell A1
            sheet.addCell(new Label(0, 3, "ID"));
            sheet.addCell(new Label(1, 3, "function"));
            sheet.addCell(new Label(2, 3, "pre-condition"));
            sheet.addCell(new Label(3, 3, "post-condition"));
            sheet.addCell(new Label(4, 3, "status"));
            sheet.addCell(new Label(5, 3, "cputime (s)"));
            sheet.addCell(new Label(6, 3, "memUsage (MB)"));
            sheet.addCell(new Label(7, 3, "totalTime (s)"));
            //TODO: add element to excel export
            for (VerificationReport row : reportList) {

                String function = null, preCondition = null, postCondition = null;
                String status = null, memoryUsage = null, counterEx = null;
                double cpuTime = 0;
                double generateConstaintTime = 0;
                double totalTime = 0;
                WritableCellFormat cellFormat = new WritableCellFormat();
                cellFormat.setAlignment(Alignment.LEFT);
                cellFormat.setWrap(true);

                function = row.getFunctionName();
                preCondition = row.getPreCondition();
                postCondition = row.getPostCondition();
                status = row.getStatus();
                cpuTime = row.getSolverTime() / 1000.0;
                generateConstaintTime = row.getGenerateConstraintTime() / 1000.0;
                totalTime = roundAvoid(cpuTime + generateConstaintTime, 3);
                memoryUsage = "unknown";
                counterEx = row.getCounterEx();

                sheet.addCell(new Label(0, numberOfRow, String.valueOf(numberOfRow++)));
                sheet.addCell(new Label(1, numberOfRow, function));
                sheet.addCell(new Label(2, numberOfRow, preCondition));
                sheet.addCell(new Label(3, numberOfRow, postCondition));
                sheet.addCell(new Label(4, numberOfRow, status));
                sheet.addCell(new Label(5, numberOfRow, String.valueOf(cpuTime)));
                sheet.addCell(new Label(6, numberOfRow, memoryUsage));
                sheet.addCell(new Label(8, numberOfRow, counterEx, cellFormat));
                sheet.addCell(new Label(7, numberOfRow, String.valueOf(totalTime)));

                numberOfRow++;
            }

            workbook.write();
            workbook.close();

            //System.out.println("Export successfully");
        } catch (Exception e) {
            System.err.println("Error while exporting excel");
            e.printStackTrace();
        }
    }
}
