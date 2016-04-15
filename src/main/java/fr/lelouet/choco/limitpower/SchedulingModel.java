
package fr.lelouet.choco.limitpower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import fr.lelouet.choco.limitpower.model.HPC;
import fr.lelouet.choco.limitpower.model.PowerMode;


/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2015
 *
 */
public class SchedulingModel {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SchedulingModel.class);

	/** the objective to solve this problem */
	public static enum Objective {
		/** objective is to maximize profit of the applications */
		PROFIT,
		/** objective is to maximize profit, THEN to maximize the power use */
		PROFIT_POWER,
		/**
		 * objecive is to maximize profit, THEN to maximize number of subtasks on
		 * schedule
		 */
		PROFIT_ONSCHEDULE
	}

	public Objective objective = Objective.PROFIT;

	public int nbIntervals = 6;

	public int maxPower = 100;

	protected LinkedHashMap<String, List<PowerMode>> webs = new LinkedHashMap<>();

	/**
	 * add a power mode to an app name, if it does not have it already
	 *
	 * @param name
	 *          the name of the app
	 * @param power
	 *          the power of the mode
	 * @param profit
	 *          the profit of the mode
	 * @return this
	 */
	public SchedulingModel addWeb(String name, int power, int profit) {
		if (hpcs.containsKey(name)) {
			throw new UnsupportedOperationException(
					"name " + name + " is already used for the HPC apps, can't use it for web app");
		}
		List<PowerMode> l = webs.get(name);
		if (l == null) {
			l = new ArrayList<>();
			webs.put(name, l);
		}
		PowerMode pm = PowerMode.mode(power, profit);
		if (!l.contains(pm)) {
			l.add(pm);
		}
		return this;
	}

	public int[] webPowers(String name) {
		List<PowerMode> l = webs.get(name);
		if (l == null || l.isEmpty()) {
			return new int[] {};
		}
		int[] ret = new int[l.size()];
		for (int i = 0; i < l.size(); i++) {
			ret[i] = l.get(i).power;
		}
		return ret;
	}

	public int[] webProfits(String name) {
		List<PowerMode> l = webs.get(name);
		if (l == null || l.isEmpty()) {
			return new int[] {};
		}
		int[] ret = new int[l.size()];
		for (int i = 0; i < l.size(); i++) {
			ret[i] = l.get(i).profit;
		}
		return ret;
	}

	public List<PowerMode> getWebPowerModes(String name) {
		return Collections.unmodifiableList(webs.get(name));
	}

	/**
	 * reference to a web app inside the model
	 *
	 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2015
	 *
	 */
	public class NamedWeb {

		protected String appName;

		/**
		 *
		 */
		public NamedWeb(String name) {
			appName = name;
		}

		public NamedWeb add(int power, int profit) {
			addWeb(appName, power, profit);
			return this;
		}
	}

	public NamedWeb nameWeb(String name) {
		return new NamedWeb(name);
	}

	protected LinkedHashMap<String, HPC> hpcs = new LinkedHashMap<>();

	public HPC getHPC(String name) {
		return hpcs.get(name);
	}

	public void addHPC(String name, HPC model) {
		if (webs.containsKey(name)) {
			throw new UnsupportedOperationException(
					"name " + name + " is already used for the web apps, can't use it for HPC app");
		}
		hpcs.put(name, model);
	}

	public void addHPC(String name, int start, int duration, int power, int benefit, int deadline) {
		addHPC(name, new HPC(start, duration, power, benefit, deadline));
	}

	protected HashMap<Integer, Integer> powerlimits = new HashMap<>();

	public Iterable<Map.Entry<Integer, Integer>> getLimits() {
		return powerlimits.entrySet();
	}

	public void setLimit(int interval, int power) {
		powerlimits.put(interval, power);
	}

	public int limit(int idx) {
		return powerlimits.containsKey(idx) ? powerlimits.get(idx) : 0;
	}

	//
	// tools
	//
	/**
	 *
	 * @return the sum of the maximum profit of all applications
	 */
	public int getMaxProfit() {
		return hpcs.values().stream().mapToInt(h -> h.profit).sum()
				+ nbIntervals
				* webs.values().stream().mapToInt(l -> l.stream().mapToInt(pm -> pm.profit).max().getAsInt()).sum();
	}

	@Override
	public String toString() {
		return "Model(total power=" + maxPower + " intervals=" + nbIntervals + " obj=" + objective + ")" + hpcs + webs
				+ powerlimits;
	}

	/** a server has a name, and a power(=cpu) capacity. */
	public class Server {

		public final String name;
		int maxPower = Integer.MAX_VALUE;

		public Server(String name) {
			this.name = name;
		}
	};

	protected LinkedHashMap<String, Server> serversByName = new LinkedHashMap<>();

	/**
	 * add a server with a given name, or return the one already present if
	 * exists.
	 */
	public Server addServer(String name) {
		Server ret = serversByName.get(name);
		if (ret == null) {
			ret = new Server(name);
			serversByName.put(name, ret);
		}
		return ret;
	}

	public Server getServer(String name) {
		return serversByName.get(name);
	}

	public int nbServers() {
		return serversByName.size();
	}

	public Stream<Entry<String, Server>> servers() {
		return serversByName.entrySet().stream();
	}

}
