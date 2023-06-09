package com.mvs.runbenchmark;

import com.mvs.app.verification.ExportExcel;
import com.mvs.app.verification.FileVerification;
import com.mvs.app.verification.FunctionVerification;
import com.mvs.app.verification.report.VerificationReport;
import com.mvs.invariant.LoopTemplate;
import jxl.write.WriteException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class RunInvariant {
    public static void  main(String[] args) throws IOException, WriteException {
        String benchmark = "./src/main/resources/benchmark/invgen/template2/loop-acceleration/simple_3_2_true.c";
        ExportExcel exportExcel = new ExportExcel();
        File file = new File(benchmark);
        FileVerification fv = new FileVerification();
        LoopTemplate.generateInvariantDirectory(file);
        List<VerificationReport> reportList = fv.verifyDirectory(file, FunctionVerification.UNFOLD_MODE);
        exportExcel.writeExcel(reportList);
    }
}