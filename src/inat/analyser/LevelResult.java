/**
 * 
 */
package inat.analyser;

import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * The concentrations result contains information about the analysis of the
 * activation levels of each substrate in a model.
 * 
 * @author B. Wanders
 */
public interface LevelResult {
	/**
	 * This method retrieves the level of activation for the given substrate
	 * 
	 * @param id the id of the substrate
	 * @param time the time index to do a look up for
	 * @return the level of concentration
	 */
	public double getConcentration(String id, double time);

	/**
	 * This method retrieves the level of activation for the given reactant, or null if that reactant has not a value for the given instant
	 * 
	 * @param id the id of the reactant
	 * @param time the time index to do a look up for
	 * @return the level of concentration
	 */
	public Double getConcentrationIfAvailable(String id, double time);
	
	/**
	 * If the concentration is available, return its value. Otherwise,
	 * use (linear) interpolation to find a value between the two nearest ones
	 * @param id
	 * @param time
	 * @return
	 */
	public double getInterpolatedConcentration(String id, double time);

	/**
	 * Determines the reactant ID's of substrates of which result are known.
	 * 
	 * @return a set of IDs
	 */
	public Set<String> getReactantIds();

	/**
	 * Returns a list of all time indices at which we have a real data point.
	 * 
	 * @return the list of data point time indices
	 */
	public List<Double> getTimeIndices();
	
	public boolean isEmpty();
	
	/**
	 * Returns a new LevelResult where only the series whose names are in
	 * the given Vector are present. All other series are discarded.
	 * @param acceptedNames
	 * @return
	 */
	public LevelResult filter(Vector<String> acceptedNames);
}
