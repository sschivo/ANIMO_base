package inat.analyser.uppaal;

import inat.analyser.LevelResult;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Map.Entry;

/**
 * A very simple data container for the concentration/time data.
 * 
 * @author Brend Wanders
 * 
 */
public class SimpleLevelResult implements LevelResult, Serializable {
	private static final long serialVersionUID = 5440819034905472745L;
	Map<String, SortedMap<Double, Double>> levels;

	/**
	 * @param levels the levels to enter
	 */
	public SimpleLevelResult(Map<String, SortedMap<Double, Double>> levels) {
		this.levels = levels;
	}

	@Override
	public double getConcentration(String id, double time) {
		assert this.levels.containsKey(id) : "Can not retrieve level for unknown identifier.";

		SortedMap<Double, Double> data = this.levels.get(id);

		// determine level at requested moment in time:
		// it is either the level set at the requested moment, or the one set
		// before that
		//assert !data.headMap(time + 1).isEmpty() : "Can not retrieve data from any moment before the start of time.";
		//int exactTime = data.headMap(time + 1).lastKey();
		double exactTime = -1;
		for (Double k : data.keySet()) {
			if (k > time) break;
			exactTime = k;
		}

		// use exact time to get value
		return data.get(exactTime);
	}
	
	@Override
	public Double getConcentrationIfAvailable(String id, double time) {
		assert this.levels.containsKey(id) : "Can not retrieve level for unknown identifier.";

		SortedMap<Double, Double> data = this.levels.get(id);
		
		return data.get(time);
	}
	

	/**
	 * Linear interpolation between the two nearest if does not find the requested time 
	 */
	@Override
	public double getInterpolatedConcentration(String id, double time) {
		Double val = this.getConcentrationIfAvailable(id, time);
		if (val == null) {
			SortedMap<Double, Double> data = this.levels.get(id);
			double lowerTime = -1, higherTime = -1;
			for (Double k : data.keySet()) {
				if (k > time) {
					higherTime = k;
					break;
				}
				lowerTime = k;
			}
			double lowerVal = data.get(lowerTime),
				   higherVal = data.get(higherTime);
			return lowerVal + (higherVal - lowerVal) * (time - lowerTime) / (higherTime - lowerTime);
		} else {
			return val;
		}
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		
		b.append("Result["+this.getReactantIds()+"] ");

		for (Entry<String, SortedMap<Double, Double>> r : this.levels.entrySet()) {
			b.append(r.getKey() + ": " + r.getValue() + "\n");
		}

		return b.toString();
	}

	@Override
	public List<Double> getTimeIndices() {
		SortedSet<Double> accumulator = new TreeSet<Double>();

		for (SortedMap<Double, Double> e : this.levels.values()) {
			accumulator.addAll(e.keySet());
		}

		return new ArrayList<Double>(accumulator);
	}

	@Override
	public Set<String> getReactantIds() {
		return Collections.unmodifiableSet(this.levels.keySet());
	}

	@Override
	public boolean isEmpty() {
		return levels.isEmpty();
	}
	
	@Override
	public LevelResult filter(Vector<String> acceptedNames) {
		Map<String, SortedMap<Double, Double>> lev = new HashMap<String, SortedMap<Double, Double>>();
		for (String s : levels.keySet()) {
			if (!acceptedNames.contains(s)) continue;
			SortedMap<Double, Double> m = levels.get(s);
			lev.put(s, m);
		}
		SimpleLevelResult res = new SimpleLevelResult(lev);
		return res;
	}

}
