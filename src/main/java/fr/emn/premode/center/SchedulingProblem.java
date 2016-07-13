
package fr.emn.premode.center;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import fr.emn.premode.objectives.Objective;
import fr.emn.premode.objectives.Profit;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2015
 */
public class SchedulingProblem {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SchedulingProblem.class);

	public Objective objective = Profit.INSTANCE;

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

// energy cost of migrative a VM

	protected ToIntFunction<String> migrateCost = null;

	/**
	 * set the name-to-cost function to apply to applications. An application migrated adds up that uch energy cost.
	 *
	 * @param cost
	 *          the function to set. Should return 0 for null or by default. if set to null, all app cost are 0.
	 */
	public void setMigrateCost(ToIntFunction<String> cost) {
		migrateCost = cost;
	}

	/**
	 * get the ost of migrating an app
	 *
	 * @param appName
	 *          the name of the app
	 * @return the value associated to appname in the {@link #migrateCost}, or 0 if name is null, migrateCost is null, or
	 *         the name is not associated in the migrateCost.
	 */
	public int migrateCost(String appName) {
		return appName == null || migrateCost == null ? 0 : migrateCost.applyAsInt(appName);
	}

// web apps

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
	public SchedulingProblem addWeb(String name, int power, int profit) {
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

	public void removeApp(String name) {
		webs.remove(name);
		hpcs.remove(name);
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

	/**
	 * @param name
	 * @return unmodifiable lit of power modes associated to given web app name.
	 */
	public List<PowerMode> getWebPowerModes(String name) {
		List<PowerMode> l = webs.get(name);
		return l == null ? Collections.emptyList() : Collections.unmodifiableList(webs.get(name));
	}

	/**
	 * reference to a web app inside the model
	 *
	 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2015
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

		public void remove() {
			removeApp(appName);
		}
	}

	public NamedWeb nameWeb(String name) {
		return new NamedWeb(name);
	}

	public Stream<String> webNames() {
		return webs.keySet().stream();
	}

	public int nbWebs() {
		return webs.size();
	}

// HPC apps

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

	public void addHPC(String name, int start, int duration, int powerUse, int profit, int deadline) {
		addHPC(name, new HPC(start, duration, powerUse, profit, deadline));
	}

	public Stream<String> hpcNames() {
		return hpcs.keySet().stream();
	}

	public int nbHPCs() {
		return hpcs.size();
	}

// all apps

	public Stream<String> appNames() {
		return Stream.concat(hpcNames(), webNames());
	}

	public int nbApps() {
		return hpcs.size() + webs.size();
	}

//
// tools
//
	/**
	 * @return the sum of the maximum profit of all applications
	 */
	public int getMaxProfit() {
		return hpcs.values().stream().mapToInt(h -> h.profit).sum() + nbIntervals
				* webs.values().stream().mapToInt(l -> l.stream().mapToInt(pm -> pm.profit).max().getAsInt()).sum();
	}

	@Override
	public String toString() {
		return "Model(intervals=" + nbIntervals + " obj=" + objective + ")" + hpcs + webs + powerlimits;
	}

/////////////////////////
// servers
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
	 * add a server with a given name, or return the one already present if exists.
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

	public Stream<String> servNames() {
		return serversByName.keySet().stream();
	}

///////////////////
// resources
///////////////////

	protected HashMap<String, ToIntFunction<String>> resources = new HashMap<>();

	public void setResource(String name, ToIntFunction<String> mapping) {
		if (mapping == null) {
			resources.remove(name);
		} else {
			resources.put(name, mapping);
		}
	}

	public Stream<Entry<String, ToIntFunction<String>>> resources() {
		return resources.entrySet().stream();
	}

	public ToIntFunction<String> getResource(String name) {
		return resources.get(name);
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

	public double getLoad(String resName) {
		ToIntFunction<String> res = resources.get(resName);
		if (res == null) {
			return 0;
		}
		return appNames().mapToDouble(res::applyAsInt).sum()
				/ serversByName.keySet().stream().mapToDouble(res::applyAsInt).sum();
	}

	public double getMinPwrLoad() {
		return (webs.values().stream().mapToDouble(l -> l.stream().mapToDouble(p -> p.power).min().getAsDouble()).sum()
				+ hpcs.values().stream().mapToDouble(h -> h.power).sum())
				/ serversByName.values().stream().mapToDouble(s -> s.maxPower).sum();
	}

	public double getMaxPwrLoad() {
		return (webs.values().stream().mapToDouble(l -> l.stream().mapToDouble(p -> p.power).max().getAsDouble()).sum()
				+ hpcs.values().stream().mapToDouble(h -> h.power).sum())
				/ serversByName.values().stream().mapToDouble(s -> s.maxPower).sum();
	}

	public IntStream streamMaxPwr() {
		return IntStream.concat(webs.values().stream().mapToInt(l -> l.stream().mapToInt(p -> p.power).max().getAsInt()),
				hpcs.values().stream().mapToInt(h -> h.power));
	}
}
