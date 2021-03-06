package mltk.predictor.function;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.util.Arrays;

import mltk.core.Instance;
import mltk.predictor.Regressor;
import mltk.util.ArrayUtils;
import mltk.util.MathUtils;
import mltk.util.VectorUtils;

/**
 * Class for 1D functions.
 * 
 * <p>
 * This class represents a segmented 1D function. Segments are defined in split array. For example, [3, 5, +INF] defines
 * three segments: (-INF, 3], (3, 5], (5, +INF). The last value in the split array is always +INF. The prediction array
 * is the corresponding predictions for segments defined in splits.
 * </p>
 * 
 * @author Yin Lou
 * 
 */
public class Function1D implements Regressor, UnivariateFunction {

	/**
	 * Attribute index.
	 */
	protected int attIndex;

	/**
	 * Last value is always Double.POSITIVE_INFINITY. e.g. [3, 5, +INF] defines three segments: (-INF, 3], (3, 5], (5,
	 * +INF)
	 */
	protected double[] splits;

	/**
	 * Corresponding predictions for segments defined in splits.
	 */
	protected double[] predictions;
	
	/**
	 * Prediction on missing value.
	 */
	protected double predictionOnMV;

	/**
	 * Returns a constant 1D function.
	 * 
	 * @param attIndex the attribute index of this function.
	 * @param prediction the constant.
	 * @return a constant 1D function.
	 */
	public static Function1D getConstantFunction(int attIndex, double prediction) {
		Function1D func = new Function1D(attIndex, new double[] { Double.POSITIVE_INFINITY },
				new double[] { prediction });
		return func;
	}

	/**
	 * Resets this function to 0.
	 */
	public void setZero() {
		splits = new double[] { Double.POSITIVE_INFINITY };
		predictions = new double[] { 0 };
		predictionOnMV = 0.0;
	}

	/**
	 * Returns {@code true} if the function is 0.
	 * 
	 * @return {@code true} if the function is 0.
	 */
	public boolean isZero() {
		return ArrayUtils.isConstant(predictions, 0, predictions.length, 0)
				&& MathUtils.isZero(predictionOnMV);
	}

	/**
	 * Returns {@code true} if the function is constant.
	 * 
	 * @return {@code true} if the function is constant.
	 */
	public boolean isConstant() {
		return ArrayUtils.isConstant(predictions, 1, predictions.length, predictions[0])
				&& MathUtils.isZero(predictionOnMV - predictions[0]);
	}

	/**
	 * Multiplies this function with a constant.
	 * 
	 * @param c the constant.
	 * @return this function.
	 */
	public Function1D multiply(double c) {
		VectorUtils.multiply(predictions, c);
		predictionOnMV *= c;
		return this;
	}

	/**
	 * Divides this function with a constant.
	 * 
	 * @param c the constant.
	 * @return this function.
	 */
	public Function1D divide(double c) {
		VectorUtils.divide(predictions, c);
		predictionOnMV /= c;
		return this;
	}

	/**
	 * Adds this function with a constant.
	 * 
	 * @param c the constant.
	 * @return this function.
	 */
	public Function1D add(double c) {
		VectorUtils.add(predictions, c);
		predictionOnMV += c;
		return this;
	}

	/**
	 * Subtracts this function with a constant.
	 * 
	 * @param c the constant.
	 * @return this function.
	 */
	public Function1D subtract(double c) {
		VectorUtils.subtract(predictions, c);
		predictionOnMV -= c;
		return this;
	}

	/**
	 * Constructor.
	 */
	public Function1D() {

	}
	
	/**
	 * Constructor.
	 * 
	 * @param attIndex the attribute index.
	 * @param splits the splits.
	 * @param predictions the predictions.
	 */
	public Function1D(int attIndex, double[] splits, double[] predictions) {
		this(attIndex, splits, predictions, 0.0);
	}

	/**
	 * Constructor.
	 * 
	 * @param attIndex the attribute index.
	 * @param splits the splits.
	 * @param predictions the predictions.
	 * @param predictionOnMissing prediction on missing value;
	 */
	public Function1D(int attIndex, double[] splits, double[] predictions, double predictionOnMissing) {
		this.attIndex = attIndex;
		this.splits = splits;
		this.predictions = predictions;
		this.predictionOnMV = predictionOnMissing;
	}

	/**
	 * Adds this function whit another function.
	 * 
	 * @param func the other function.
	 * @return this function.
	 */
	public Function1D add(Function1D func) {
		if (attIndex != func.attIndex) {
			throw new IllegalArgumentException("Cannot add functions on different terms");
		}
		int[] insertionPoints = new int[func.splits.length - 1];
		int newElements = 0;
		for (int i = 0; i < insertionPoints.length; i++) {
			insertionPoints[i] = Arrays.binarySearch(splits, func.splits[i]);
			if (insertionPoints[i] < 0) {
				newElements++;
			}
		}
		if (newElements > 0) {
			double[] newSplits = new double[splits.length + newElements];
			System.arraycopy(splits, 0, newSplits, 0, splits.length);
			int k = splits.length;
			for (int i = 0; i < insertionPoints.length; i++) {
				if (insertionPoints[i] < 0) {
					newSplits[k++] = func.splits[i];
				}
			}
			Arrays.sort(newSplits);

			double[] newPredictions = new double[newSplits.length];
			for (int i = 0; i < newSplits.length; i++) {
				newPredictions[i] = this.evaluate(newSplits[i]) + func.evaluate(newSplits[i]);
			}
			splits = newSplits;
			predictions = newPredictions;
		} else {
			for (int i = 0; i < splits.length; i++) {
				predictions[i] += func.evaluate(splits[i]);
			}
		}
		this.predictionOnMV += func.predictionOnMV;
		return this;
	}

	/**
	 * Returns the attribute index.
	 * 
	 * @return the attribute index.
	 */
	public int getAttributeIndex() {
		return attIndex;
	}

	/**
	 * Sets the attribute index.
	 * 
	 * @param attIndex the new attribute index.
	 */
	public void setAttributeIndex(int attIndex) {
		this.attIndex = attIndex;
	}

	/**
	 * Returns the internal split array.
	 * 
	 * @return the internal split array.
	 */
	public double[] getSplits() {
		return splits;
	}

	/**
	 * Sets the split array.
	 * 
	 * @param splits the new split array.
	 */
	public void setSplits(double[] splits) {
		this.splits = splits;
	}

	/**
	 * Returns the internal prediction array.
	 * 
	 * @return the internal prediction array.
	 */
	public double[] getPredictions() {
		return predictions;
	}

	/**
	 * Sets the prediction array.
	 * 
	 * @param predictions the new prediction array.
	 */
	public void setPredictions(double[] predictions) {
		this.predictions = predictions;
	}
	
	/**
	 * Returns the prediction on missing value.
	 * 
	 * @return the prediction on missing value.
	 */
	public double getPredictionOnMV() {
		return predictionOnMV;
	}
	
	/**
	 * Sets the prediction on missing value.
	 * 
	 * @param predictionOnMV the prediction on missing value.
	 */
	public void setPredictionOnMV(double predictionOnMV) {
		this.predictionOnMV = predictionOnMV;
	}

	@Override
	public void read(BufferedReader in) throws Exception {
		String line = in.readLine();
		String[] data = line.split(": ");
		attIndex = Integer.parseInt(data[1]);

		line = in.readLine();
		data = line.split(": ");
		predictionOnMV = Double.parseDouble(data[1]);
		
		in.readLine();
		line = in.readLine();
		splits = ArrayUtils.parseDoubleArray(line);

		in.readLine();
		line = in.readLine();
		predictions = ArrayUtils.parseDoubleArray(line);
	}

	@Override
	public void write(PrintWriter out) throws Exception {
		out.printf("[Predictor: %s]\n", this.getClass().getCanonicalName());
		out.println("AttIndex: " + attIndex);
		out.println("PredictinOnMV: " + predictionOnMV);
		out.println("Splits: " + splits.length);
		out.println(Arrays.toString(splits));
		out.println("Predictions: " + predictions.length);
		out.println(Arrays.toString(predictions));
	}

	@Override
	public double regress(Instance instance) {
		return evaluate(instance.getValue(attIndex));
	}

	@Override
	public double evaluate(double x) {
		if (Double.isNaN(x)) {
			return predictionOnMV;
		} else {
			return predictions[getSegmentIndex(x)];
		}
	}

	@Override
	public Function1D copy() {
		double[] splitsCopy = Arrays.copyOf(splits, splits.length);
		double[] predictionsCopy = Arrays.copyOf(predictions, predictions.length);
		return new Function1D(attIndex, splitsCopy, predictionsCopy, predictionOnMV);
	}
	
	/**
	 * Returns the segment index given x.
	 * 
	 * @param x the search key.
	 * @return the segment index given x.
	 */
	protected int getSegmentIndex(double x) {
		// Assume x is not NaN
		return ArrayUtils.findInsertionPoint(splits, x);
	}

}
