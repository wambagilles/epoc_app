
package fr.lelouet.choco.limitpower;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

import fr.lelouet.choco.limitpower.model.HPC;
import fr.lelouet.choco.limitpower.model.PowerMode;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;


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

	protected TIntIntHashMap powerlimits = new TIntIntHashMap();

	/**
	 * set a different limit for given interval
	 *
	 * @param interval
	 *          the index of the interval(starting 0)
	 * @param power
	 *          the maximum power at this interval. must be &lt; maxpower
	 */
	public void setPower(int interval, int power) {
		if (interval < 0) {
			return;
		}
		if (power < 0) {
			powerlimits.remove(interval);
		} else {
			powerlimits.put(interval, power);
		}
	}

	/**
	 * get the power specified at slot idx
	 * 
	 * @param idx
	 *          the slot to consider
	 * @return the value of power associated to this slot, or -1 if not present
	 */
	public int getPower(int idx) {
		return powerlimits.containsKey(idx) ? powerlimits.get(idx) : -1;
	}

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
		return "Model(intervals=" + nbIntervals + " obj=" + objective + ")" + hpcs + webs
				+ powerlimits;
	}

	/////////////////////////
	// server position
	/////////////////////////

	/** a server has a name, and a power(=cpu) capacity. */
	public class Server {

		public final String name;
		public int maxPower = Integer.MAX_VALUE;

		public Server(String name) {
			this.name = name;
		}
	};

	protected LinkedHashMap<String, Server> serversByName = new LinkedHashMap<>();

	/**
	 * add a server with a given name, or return the one already present if
	 * exists.
	 */
	public Server server(String name) {
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

	///////////////////
	// resources
	///////////////////

	protected HashMap<String, ToIntFunction<String>> resources = new HashMap<>();

	public void setResource(String name, ToIntFunction<String> mapping) {
		resources.put(name, mapping);
	}

	public Stream<Entry<String, ToIntFunction<String>>> resources() {
		return resources.entrySet().stream();
	}

	///////////////////
	// previous position and modes
	///////////////////

	/**
	 * The class previous contains data just before the first interval. It gives the possiblity to find if an app is
	 * migrated and its migration cost.
	 *
	 * @author guillaume
	 */
	public class Previous {

		/**
		 * previous location of the wep/hpc apps. If an app name is not mapped to a Server name, the app is not migrate
		 * wherever it is placed on the first interval. If the app name is mapped to a Server name, this app is migrated on
		 * first interval if its servers has a different name. Obvioulsy, if the server name stands for no server in the
		 * problem, the app is migrated on first interval.
		 */
		public final HashMap<String, String> pos = new HashMap<>();
		/**
		 * previous power mode of the web apps. If not present here, a web app has not migration cost for the first
		 * interval. we don't check the presence of a corresponding webmode.
		 */
		public final TObjectIntHashMap<String> power = new TObjectIntHashMap<>();
	}

	public final Previous previous = new Previous();
}
