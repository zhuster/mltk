package mltk.predictor.gam.tool;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import mltk.cmdline.Argument;
import mltk.cmdline.CmdLineParser;
import mltk.core.Instance;
import mltk.core.Instances;
import mltk.core.io.InstancesReader;
import mltk.predictor.Regressor;
import mltk.predictor.gam.GAM;
import mltk.predictor.io.PredictorReader;
import mltk.util.StatUtils;
import mltk.util.Element;

/**
 * Class for GAM diagnostics.
 * 
 * @author Yin Lou
 * 
 */
public class Diagnostics {
	
	/**
	 * Enumeration of methods for calculating term importance.
	 * 
	 * @author Yin Lou
	 * 
	 */
	public enum Mode {

		/**
		 * L1.
		 */
		L1("L1"),
		/**
		 * PDF terminal.
		 */
		L2("L2");

		String mode;

		Mode(String mode) {
			this.mode = mode;
		}

		public String toString() {
			return mode;
		}

		/**
		 * Parses a mode from a string.
		 * 
		 * @param mode the mode.
		 * @return a parsed terminal.
		 */
		public static Mode getEnum(String mode) {
			for (Mode re : Mode.values()) {
				if (re.mode.compareTo(mode) == 0) {
					return re;
				}
			}
			throw new IllegalArgumentException("Invalid mode: " + mode);
		}

	}

	/**
	 * Computes the weights for each term in a GAM.
	 * 
	 * @param gam the GAM model.
	 * @param instances the training set.
	 * @return the list of weights for each term in a GAM.
	 */
	public static List<Element<int[]>> diagnose(GAM gam, Instances instances) {
		return diagnose(gam, instances, Mode.L2);
	}
	
	/**
	 * Computes the weights for each term in a GAM.
	 * 
	 * @param gam the GAM model.
	 * @param instances the training set.
	 * @return the list of weights for each term in a GAM.
	 */
	public static List<Element<int[]>> diagnose(GAM gam, Instances instances, Mode mode) {
		List<Element<int[]>> list = new ArrayList<>();
		Map<int[], List<Regressor>> map = new HashMap<>();
		List<int[]> terms = gam.getTerms();
		List<Regressor> regressors = gam.getRegressors();
		for (int i = 0; i < terms.size(); i++) {
			int[] term = terms.get(i);
			if (!map.containsKey(term)) {
				map.put(term, new ArrayList<Regressor>());
			}
			Regressor regressor = regressors.get(i);
			map.get(term).add(regressor);
		}

		double[] predictions = new double[instances.size()];
		for (int[] term : map.keySet()) {
			List<Regressor> regressorList = map.get(term);
			for (int i = 0; i < instances.size(); i++) {
				predictions[i] = 0;
				Instance instance = instances.get(i);
				for (Regressor regressor : regressorList) {
					predictions[i] += regressor.regress(instance);
				}
			}
			double weight = 0;
			if (mode == Mode.L2) {
				weight = StatUtils.variance(predictions);
			} else {
				double mean = StatUtils.mean(predictions);
				weight = StatUtils.mad(predictions, mean);
			}
			
			list.add(new Element<int[]>(term, weight));
		}

		return list;
	}

	static class Options {

		@Argument(name = "-r", description = "attribute file path")
		String attPath = null;

		@Argument(name = "-d", description = "dataset path", required = true)
		String datasetPath = null;
		
		@Argument(name = "-m", description = "mode (L1 or L2, default: L2)")
		String mode = null;

		@Argument(name = "-i", description = "input model path", required = true)
		String inputModelPath = null;

		@Argument(name = "-o", description = "output path", required = true)
		String outputPath = null;

	}

	/**
	 * Generates term importance for GAMs.
	 * 
	 * <pre>
	 * Usage: mltk.predictor.gam.tool.Diagnostics
	 * -d	dataset path
	 * -i	input model path
	 * -o	output path
	 * [-r]	attribute file path
	 * [-m]	mode (L1 or L2, default: L2)
	 * </pre>
	 * 
	 * @param args the command line arguments.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		Options opts = new Options();
		CmdLineParser parser = new CmdLineParser(Diagnostics.class, opts);
		try {
			parser.parse(args);
		} catch (IllegalArgumentException e) {
			parser.printUsage();
			System.exit(1);
		}
		Instances dataset = InstancesReader.read(opts.attPath, opts.datasetPath);
		GAM gam = PredictorReader.read(opts.inputModelPath, GAM.class);

		List<Element<int[]>> list = Diagnostics.diagnose(gam, dataset, Mode.getEnum(opts.mode));
		Collections.sort(list);
		Collections.reverse(list);

		PrintWriter out = new PrintWriter(opts.outputPath);
		for (Element<int[]> element : list) {
			int[] term = element.element;
			double weight = element.weight;
			out.println(Arrays.toString(term) + ": " + weight);
		}
		out.flush();
		out.close();
	}

}
