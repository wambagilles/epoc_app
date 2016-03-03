package fr.lelouet.choco.limitpower;

import java.util.HashMap;
import java.util.List;

import fr.lelouet.choco.limitpower.model.PowerMode;

public class Result {

	public Result() {
	}

	public HashMap<String, List<PowerMode>> webModes = new HashMap<>();

	public HashMap<String, List<Integer>> hpcStarts = new HashMap<>();

	public int profit;

	@Override
	public String toString() {
		return "" + profit + webModes + hpcStarts;
	}

}
