package com.mvs.runbenchmark;

import com.mvs.app.verification.ExportExcel;
import com.mvs.app.verification.FileVerification;
import com.mvs.app.verification.FunctionVerification;
import com.mvs.app.verification.report.VerificationReport;
import jxl.write.WriteException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RunPThread {

    public static void main(String[] args) throws WriteException, IOException {
        ExportExcel exportExcel = new ExportExcel("mvs Report.xls");

        File file = new File("./src/main/resources/benchmark/pthread/test.c");
        FileVerification fv = new FileVerification();
        List<VerificationReport> reportList = fv.verifyDirectory(file, FunctionVerification.UNFOLD_MODE);
        exportExcel.writeExcel(reportList);
    }
}
