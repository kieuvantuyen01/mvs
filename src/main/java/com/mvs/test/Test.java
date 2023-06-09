package com.mvs.test;

import com.mvs.app.verification.FunctionVerification;
import com.mvs.app.verification.report.VerificationReport;
import com.mvs.cfg.build.ASTFactory;
import com.mvs.cfg.build.mvsCFG;
import com.mvs.cfg.node.CFGNode;
import com.mvs.graph.GraphGenerator;
import com.mvs.visualize.AddMoreInformation;
import com.mvs.visualize.PathExecutionVisualize;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.parse.Parser;
import jxl.write.WriteException;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @author va
 *
 */
public class Test {
	private static String getBenchmark(String props) throws IOException {
		String path = System.getProperty("user.dir") + "/src/main/java/com/mvs/test/benchmark.properties";
		FileInputStream is = new FileInputStream(new File(path));
		Properties benchmarkProps = new Properties();
		benchmarkProps.load(is);
		return benchmarkProps.getProperty(props);
	}

	private static void printTree(IASTNode node, int index) {
		IASTNode[] children = node.getChildren();

		for (int i = 0; i < index; i++) {
			System.out.print(" ");
		}

		System.out.println("-" + node.getClass().getSimpleName() + " -> " + node.getRawSignature());
		for (IASTNode iastNode : children)
			printTree(iastNode, index + 2);
	}

	public static void  main(String[] args) throws IOException, WriteException {
////		FileVerification fileVerification = new FileVerification();
//		fileVerification.verify(new File("./src/main/resources/benchmark/example/example_7.c"), FunctionVerification.UNFOLD_MODE);
		ASTFactory ast = new ASTFactory("./src/main/resources/benchmark/example/sum.c");
		IASTFunctionDefinition main_func = ast.getFunction(0);
		System.out.println(main_func.toString());
		mvsCFG cfg = new mvsCFG(ast.getFunction(0), ast);
		cfg.unfold(2);
		cfg.index();
//		String pre_condition = "a = 5";
		String pre_condition = "";
		String post_condition = "return = 0";
		int nLoops = 2;
		int mode = FunctionVerification.UNFOLD_MODE;
		File smtFile = File.createTempFile("temp", ".txt");
		System.out.println(smtFile.getAbsolutePath());
		VerificationReport vr = FunctionVerification.verify(ast, main_func, pre_condition, post_condition, nLoops, mode, smtFile);
		PathExecutionVisualize pathExecutionVisualize = new PathExecutionVisualize(cfg, vr);
		List<CFGNode> nodes = pathExecutionVisualize.findPathToFail();

		pathExecutionVisualize.findPathsToFail(pre_condition, post_condition);
		List<CFGNode> firstPath = pathExecutionVisualize.getListPaths().get(1);
		VerificationReport firstVerReport = pathExecutionVisualize.getListvr().get(1);
		System.out.println(pathExecutionVisualize.getListPaths().size());

//		AddMoreInformation addMoreInformation = new AddMoreInformation(cfg, vr);
		AddMoreInformation addMoreInformation = new AddMoreInformation(cfg, firstVerReport);

		Map<String, String> listParameters = addMoreInformation.parseParameters();

        GraphGenerator graphGenerator = new GraphGenerator(cfg, listParameters);
        graphGenerator.printGraph(false);
		if(vr.getStatus() == VerificationReport.ALWAYS_TRUE){
//			graphGenerator.fillColor(nodes, false, true);
			graphGenerator.fillColor(firstPath, false, true);
		} else {
//			graphGenerator.fillColor(nodes, true, true);
			graphGenerator.fillColor(firstPath, true, true);
		}
		try {
			File file = new File("./graph.dot");
			InputStream dot = new FileInputStream(file);
			MutableGraph g = new Parser().read(dot);
			Graphviz.fromGraph(g).width(500).render(Format.PNG).toFile(new File("./a1.png"));
		} catch(Exception e){
            System.out.println(e.toString());
		}
	}
}
