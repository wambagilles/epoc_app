
package fr.lelouet.choco.limitpower;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.trace.IOutputFactory;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.SetVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import fr.lelouet.choco.limitpower.model.HPC;
import fr.lelouet.choco.limitpower.model.PowerMode;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * solve an app scheduling problem with benefit maximisation.
 * <ul>
 * <li>the time is divided into a finite number of slots ; interval 0 is from slot 0 to slot 1, etc. we have
 * {@link #nbIntervals} interval so the times go from 0 to {@link #nbIntervals} included (this is last slot's end time)
 * </li>
 * <li>web apps start on 0 and end on {@link #nbIntervals}; have several modes, each consume an amount of power and give
 * a profit</li>
 * <li>hpc app have a given amount of subtask to schedule sequentially on the intervals,each of duration 1, each
 * consumming the same amount of power ; The benefit is given if the hpc task is scheduled entirely before its deadline
 * expires, meaning its last subtask.end <= deadline</li>
 * <li>reduction amounts specify reduction in the total power capacity</li>
 * </ul>
 *
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2015
 */
public class AppScheduler extends Model {

	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AppScheduler.class);

	protected SchedulingModel model = new SchedulingModel();

	public SchedulingModel getModel() {
		return model;
	}

	protected boolean debug = false;

	public AppScheduler withDebug(boolean debug) {
		this.debug = debug;
		return this;
	}

	protected IntVar fixed(int i) {
		return intVar(i);
	}

	protected IntVar enumerated(String name, int min, int max) {
		return intVar(name, min, max, false);
	}

	protected IntVar bounded(String name, int min, int max) {
		return intVar(name, min, max, true);
	}

	/**
	 * all tasks for cumulative : web, hpc, and specific power-limit-oriented tasks<br />
	 * task at position i has power at position i in {@link #allPowers}
	 */
	protected List<Task> allTasks = new ArrayList<>();

	protected List<IntVar> allPowers = new ArrayList<>();

	/**
	 * @param t
	 *          a task
	 * @param p
	 *          the power variable of the task
	 */
	protected void addTask(Task t, IntVar p) {
		allTasks.add(t);
		allPowers.add(p);
	}

	// all the non-null profits of the tasks we schedule

	protected List<IntVar> allProfits = new ArrayList<>();

	protected IntVar totalProfit;

	//
	// web tasks
	//

	protected class WebSubClass extends Task {

		String name;

		int[] profits;
		int[] powers;

		IntVar mode, power, profit;

		WebSubClass(String name, int start, int[] profits, int[] powers) {
			super(fixed(start), fixed(1), fixed(start + 1));
			assert profits.length == powers.length;
			this.name = name;
			this.profits = profits;
			this.powers = powers;
			mode = enumerated(name + "_mode", 0, powers.length - 1);
			power = enumerated(name + "_power", AppScheduler.minIntArray(powers), AppScheduler.maxIntArray(powers));
			post(element(power, powers, mode));
			profit = enumerated(name + "_cost", AppScheduler.minIntArray(profits), AppScheduler.maxIntArray(profits));
			post(element(profit, profits, mode));
		}

		@Override
		public String toString() {
			return "webTask(start=" + getStart() + ")";
		}
	}

	/**
	 * for each web application, its modes
	 */
	protected Map<String, List<WebSubClass>> webModes = new HashMap<>();

	/** make one task corresponding to the web apps */
	protected void makeWebTasks() {
		for (Entry<String, List<PowerMode>> e : model.webs.entrySet()) {
			String name = e.getKey();
			int[] appprofits = model.webProfits(name);
			int[] apppower = model.webPowers(name);
			List<WebSubClass> l = new ArrayList<>();
			webModes.put(name, l);
			for (int i = 0; i < model.nbIntervals; i++) {
				WebSubClass t = new WebSubClass(name + "_" + i, i, appprofits, apppower);
				l.add(t);
				addTask(t, t.power);
				allProfits.add(t.profit);
			}
		}
	}

	//
	// HPC tasks
	//

	/**
	 * an HPC task is divided in subtasks. each subtask has a length of one and must be started after the previous task.
	 * The benefit of the first subtasks is 0, the benefit of the last subtasks is its benefit if it is scheduled before
	 * its deadline
	 *
	 * @author Guillaume Le Louët
	 */
	protected class HPCSubTask extends Task {

		HPC master;

		BoolVar onSchedule;

		public BoolVar getOnSchedule() {
			return onSchedule;
		}

		/** the height of the task */
		IntVar power;

		public IntVar getPower() {
			return power;
		}

		public HPCSubTask(HPC master, IntVar start, IntVar end, IntVar power, BoolVar onSchedule) {
			super(start, fixed(1), end);
			this.master = master;
			this.power = power;
			this.onSchedule = onSchedule;
		}
	}

	protected HashMap<String, List<HPCSubTask>> hpcTasks = new HashMap<>();

	protected HashMap<String, BoolVar[]> hpcExecuteds = new HashMap<>();

	/**
	 * create a variable related to an existing interval.<br />
	 * This variable can take a value from 0 to the number of intervals +1 {@link SchedulingModel#nbIntervals}
	 */
	protected IntVar makeTimeSlotVar(String name) {
		return bounded(name, 0, model.nbIntervals + 1);
	}

	/** for each hpc task, the set of intervals it runs at */
	protected HashMap<String, SetVar> hpcIntervals = new HashMap<>();

	protected void makeHPCTasks() {
		int[] allIntervals = new int[model.nbIntervals + 1];
		for (int i = 0; i < allIntervals.length; i++) {
			allIntervals[i] = i;
		}
		for (Entry<String, HPC> e : model.hpcs.entrySet()) {
			HPC h = e.getValue();
			String name = e.getKey();
			ArrayList<HPCSubTask> subtasksList = new ArrayList<>();
			hpcTasks.put(name, subtasksList);
			HPCSubTask last = null;
			IntVar[] hpcstarts = new IntVar[h.duration];
			for (int i = 0; i < h.duration; i++) {
				String subTaskName = name + "_" + i;
				IntVar start = makeTimeSlotVar(subTaskName + "_start");
				IntVar end = makeTimeSlotVar(subTaskName + "_end");
				BoolVar onSchedule = boolVar(subTaskName + "_onschedule");
				arithm(end, "<=", h.deadline > 0 ? Math.min(h.deadline, model.nbIntervals) : model.nbIntervals)
				.reifyWith(onSchedule);
				IntVar power = enumerated(subTaskName + "_power", 0, h.power);
				post(element(power, new int[] { 0, h.power }, onSchedule));
				if (last != null) {
					arithm(last.getStart(), "<=", start).post();
					Constraint orderedCstr = arithm(last.getEnd(), "<=", start);
					BoolVar isOrdered = boolVar(subTaskName + "_ordered");
					orderedCstr.reifyWith(isOrdered);
					arithm(onSchedule, "<=", isOrdered).post();
				}
				HPCSubTask t = new HPCSubTask(h, start, end, power, onSchedule);
				subtasksList.add(t);
				last = t;
				addTask(t, power);
				hpcstarts[i] = start;
			}
			IntVar profit = enumerated(name + "_profit", 0, h.profit);
			allProfits.add(profit);
			post(element(profit, new int[] { 0, h.profit }, last.onSchedule));

			SetVar usedItvs = setVar(e.getKey() + "_itv", new int[] {}, allIntervals);
			union(hpcstarts, usedItvs).post();
			hpcIntervals.put(e.getKey(), usedItvs);
			BoolVar[] executions = hpcExecuteds.get(name);
			for (int itv = 0; itv < appPositions.length; itv++) {
				member(intVar(itv), usedItvs).reifyWith(executions[itv]);
			}
		}
	}

	/////
	// name<->idx for apps
	////

	protected TObjectIntMap<String> appName2Index = new TObjectIntHashMap<>(10, 0.5f, -1);
	protected String[] index2AppName = null;

	////
	// name<->idx for servers
	////

	protected TObjectIntMap<String> servName2Index = new TObjectIntHashMap<>();
	protected String[] index2ServName = null;

	/**
	 * fill data in {@link #servName2Index}, {@link #index2ServName}, {@link #appName2Index}, {@link #index2AppName}
	 */
	protected void affectIndexes() {
		List<String> appnames = Stream.concat(model.webs.keySet().stream(), model.hpcs.keySet().stream())
				.collect(Collectors.toList());
		index2AppName = appnames.toArray(new String[] {});
		for (int i = 0; i < index2AppName.length; i++) {
			appName2Index.put(index2AppName[i], i);
		}
		index2ServName = model.serversByName.keySet().toArray(new String[] {});
		for (int i = 0; i < index2ServName.length; i++) {
			servName2Index.put(index2ServName[i], i);
		}
	}

	/**
	 * positions[i][j] is the position at interval i of the application j . if it is -1 (for an hpc app) then the
	 * application is not run at interval i.
	 */
	protected IntVar[][] appPositions;

	/**
	 * make the variables related to the position of the applications. The variables are not constrained yet.<br />
	 * If web apps don't need any specific constraint, the hpc apps must have NO host (value -1) when no subtask is
	 * executed on given interval.
	 */
	protected void makeAppPositions() {
		appPositions = new IntVar[model.nbIntervals][index2AppName.length];
		for (int appIdx = 0; appIdx < index2AppName.length; appIdx++) {
			String appName = index2AppName[appIdx];
			boolean isWeb = model.webs.containsKey(appName);
			BoolVar[] executions = null;
			if (!isWeb) {
				executions = new BoolVar[model.nbIntervals];
				hpcExecuteds.put(appName, executions);
			}
			for (int itv = 0; itv < appPositions.length; itv++) {
				IntVar position = intVar("appPos_" + itv + "_" + appIdx, isWeb ? 0 : -1, model.nbServers() - 1);
				appPositions[itv][appIdx] = position;
				if (!isWeb) {
					BoolVar executed = boolVar("appExec_" + itv + "_" + appIdx);
					executions[itv] = executed;
					arithm(position, ">=", 0).reifyWith(executed);
				}
			}
		}
	}

	/**
	 * ismigrateds[i][j] is true if app j is moved at interval i. first interval always returns false. a web app is
	 * migrated if hoster changed, a hpc app is migrated if hoster was not -1 and hoster changed.
	 */
	protected BoolVar[][] isMigrateds = null;

	protected void makeIsMigrateds() {
		isMigrateds = new BoolVar[model.nbIntervals][index2AppName.length];
		for (int appIdx = 0; appIdx < index2AppName.length; appIdx++) {
			isMigrateds[0][appIdx] = boolVar(false);
			String appName = index2AppName[appIdx];
			boolean isWeb = model.webs.containsKey(appName);
			for (int itv = 1; itv < model.nbIntervals; itv++) {
				isMigrateds[itv][appIdx] = boolVar("appMig_" + itv + "_" + appIdx);
				if (isWeb) {
					arithm(appPositions[itv][appIdx], "!=", appPositions[itv - 1][appIdx]).reifyWith(isMigrateds[itv][appIdx]);
				} else {
					BoolVar moved = boolVar("appMoved_" + itv + "_" + appIdx);
					arithm(appPositions[itv][appIdx], "!=", appPositions[itv - 1][appIdx]).reifyWith(moved);
					and(hpcExecuteds.get(appName)[itv - 1], moved, hpcExecuteds.get(appName)[itv])
					.reifyWith(isMigrateds[itv][appIdx]);
				}
			}
		}
	}

	// migrationCosts[i] is thecost of vm migration at interval i
	protected IntVar[] migrationCosts = null;

	protected void makeMigrationCosts() {
		migrationCosts = new IntVar[model.nbIntervals];
		int[] coefs = new int[index2AppName.length];
		for (int i = 0; i < coefs.length; i++) {
			coefs[i] = 1;
		}
		for (int i = 0; i < migrationCosts.length; i++) {
			migrationCosts[i] = intVar("migrationCost_" + i, 0, index2AppName.length * 1);
			post(scalar(isMigrateds[i], coefs, "=", migrationCosts[i]));
		}
	}

	//
	// reduction tasks to reduce effective power at a given time
	//

	protected Task reductionTask(int index) {
		return new Task(fixed(index), fixed(1), fixed(index + 1));
	}

	/**
	 * add fake tasks that reduce the total power available on each interval
	 */
	protected void makeReductionTasks() {
		if (model.getMaxPower() == 0) {
			AppScheduler.logger.warn("you have not set the max power.");
		}
		for (int i = 0; i < model.nbIntervals; i++) {
			int limit = model.getPower(i);
			addTask(reductionTask(i),
					limit == model.maxPower ? migrationCosts[i] : intOffsetView(migrationCosts[i], model.maxPower - limit));
		}
	}

	/**
	 * powers[i][j] is the power used at interval i by application j
	 */
	protected IntVar[][] appPowers;

	/**
	 * servPowers[i][j] is at interval i power of server j
	 */
	protected IntVar[][] servPowers;

	/**
	 * stores the power of actual web applications, hpc tasks in an array ; also add the servers-related powers.<br />
	 * to be called after {@link #makeHPCTasks()} and {@link #makeWebTasks()}. <br />
	 * the web powers are retrieved from the corresponding {@link WebSubClass} ; <br />
	 * the hpc powers are created, and constrained to 0 if no {@link HPCSubTask} is scheduled on the interval or the hpc
	 * power if one is scheduled.
	 */
	protected void makeAppPowers() {
		appPowers = new IntVar[model.nbIntervals][];
		for (int i = 0; i < appPowers.length; i++) {
			appPowers[i] = new IntVar[index2AppName.length];
		}
		// powers of each web tasks, retrieved from the power of the cumulative
		// tasks
		for (Entry<String, List<WebSubClass>> e : webModes.entrySet()) {
			int widx = appName2Index.get(e.getKey());
			IntVar[] wpowers = e.getValue().stream().map(websubtask -> websubtask.power).collect(Collectors.toList())
					.toArray(new IntVar[] {});
			for (int itv = 0; itv < appPowers.length; itv++) {
				appPowers[itv][widx] = wpowers[itv];
			}
		}
		// power of each hpc task, 0 if the task is not on schedule.
		for (Entry<String, List<HPCSubTask>> e : hpcTasks.entrySet()) {
			int hidx = appName2Index.get(e.getKey());
			int power = model.getHPC(e.getKey()).power;
			BoolVar[] executions = hpcExecuteds.get(e.getKey());
			for (int itv = 0; itv < appPowers.length; itv++) {
				appPowers[itv][hidx] = intScaleView(executions[itv], power);
			}
		}
		// now make all the [interval][server] power variables.
		servPowers = new IntVar[model.nbIntervals][index2ServName.length];
		for (int servIdx = 0; servIdx < index2ServName.length; servIdx++) {
			int maxpower = model.serversByName.get(index2ServName[servIdx]).maxPower;
			for (int itv = 0; itv < model.nbIntervals; itv++) {
				IntVar[] powerOnServers = new IntVar[index2AppName.length];
				for (int appIdx = 0; appIdx < powerOnServers.length; appIdx++) {
					IntVar power = intVar("appPwrOn_" + itv + "_" + appIdx + "_" + servIdx, 0, maxpower);
					powerOnServers[appIdx] = power;
					BoolVar execution = boolVar("appExecOn_" + itv + "_" + appIdx + "_" + servIdx);
					arithm(appPositions[itv][appIdx], "=", servIdx).reifyWith(execution);
					times(execution, appPowers[itv][appIdx], power).post();
				}
				IntVar servPower = intVar("servpower_" + itv + "_" + servIdx, 0, maxpower);
				servPowers[itv][servIdx]=servPower;
				sum(powerOnServers, "=", servPower).post();
			}
		}
	}

	//
	// the cumulative constraint on total power.
	//

	/** add the profits and the powers of the tasks */
	protected void makeCumulative() {
		totalProfit = bounded("totalProfit", 0, model.getMaxProfit());
		post(sum(allProfits.toArray(new IntVar[] {}), "=", totalProfit));
		Task[] tasks = allTasks.toArray(new Task[] {});
		IntVar[] heights = allPowers.toArray(new IntVar[] {});
		post(cumulative(tasks, heights, fixed(model.maxPower)));
	}

	//
	// pack the resources
	//

	protected void makePackings() {
		model.resources().forEach(e -> {
			String name = e.getKey();
			ToIntFunction<String> res = e.getValue();
			int[] serversCapas = new int[index2ServName.length + 1];
			serversCapas[0] = Integer.MAX_VALUE - 1;
			for (int i = 1; i < serversCapas.length; i++) {
				serversCapas[i] = res.applyAsInt(index2ServName[i - 1]);
			}
			int[] appuse = new int[index2AppName.length];
			for (int i = 0; i < index2AppName.length; i++) {
				appuse[i] = res.applyAsInt(index2AppName[i]);
			}
			for (int itv = 0; itv < model.nbIntervals; itv++) {
				IntVar[] serversLoads = new IntVar[serversCapas.length];
				for (int servIdx = 0; servIdx < serversLoads.length; servIdx++) {
					serversLoads[servIdx] = intVar(name + "_serverload_" + itv + "_" + (servIdx - 1), 0, serversCapas[servIdx]);
				}
				binPacking(appPositions[itv], appuse, serversLoads, -1).post();
			}
		});
	}

	//
	// define objective
	//

	protected IntVar makeSubtaskSum(String name, Function<HPCSubTask, IntVar> getter, IntVar initial) {
		IntVar ret = bounded(name, 0, IntVar.MAX_INT_BOUND);
		// stream of the hpcsubtasks variables
		Stream<IntVar> variables = hpcTasks.values().stream().flatMap(List::stream).map(getter);
		List<IntVar> vars = Stream.concat(variables, Stream.of(initial)).collect(Collectors.toList());
		post(sum(vars.toArray(new IntVar[] {}), "=", ret));
		return ret;
	}

	protected IntVar makeObjective() {
		switch (model.objective) {
		case PROFIT:
			return totalProfit;
		case PROFIT_POWER:
			// obj = profit*maxpower*duration + sum(hpcsubtask.power)
			return makeSubtaskSum("profit_power", HPCSubTask::getPower,
					intScaleView(totalProfit, model.maxPower * model.nbIntervals));
		case PROFIT_ONSCHEDULE:
			// obj = profit*#subtasks + sum(hpcsubtask.onSchedule)
			return makeSubtaskSum("profit_onschedule", HPCSubTask::getOnSchedule,
					intScaleView(totalProfit, model.hpcs.values().stream().mapToInt(h -> h.duration).sum()));
		default:
			throw new UnsupportedOperationException("case not supported here : " + model.objective);
		}
	}

	//
	// extract data to a result
	//

	protected SchedulingResult extractResult(Solution s) {
		SchedulingResult ret = new SchedulingResult();
		for (Entry<String, List<WebSubClass>> e : webModes.entrySet()) {
			String name = e.getKey();
			ArrayList<PowerMode> list = new ArrayList<>();
			List<PowerMode> modes = model.getWebPowerModes(name);
			for (WebSubClass w : e.getValue()) {
				list.add(modes.get(s.getIntVal(w.mode)));
			}
			ret.webModes.put(e.getKey(), list);
		}
		for (Entry<String, List<HPCSubTask>> e : hpcTasks.entrySet()) {
			List<Integer> l = new ArrayList<>();
			ret.hpcStarts.put(e.getKey(), l);
			for (HPCSubTask h : e.getValue()) {
				if (s.getIntVal(h.onSchedule) == 1) {
					l.add(s.getIntVal(h.getStart()));
				}
			}
		}
		ret.profit = s.getIntVal(totalProfit);
		for (int appIdx = 0; appIdx < index2AppName.length; appIdx++) {
			String appName = index2AppName[appIdx];
			List<String> positionsl = new ArrayList<>();
			for (int itv = 0; itv < appPositions.length; itv++) {
				IntVar posVar = appPositions[itv][appIdx];
				int positionIdx = s.getIntVal(posVar);
				positionsl.add(positionIdx == -1 ? null : index2ServName[positionIdx]);
			}
			ret.appHosters.put(appName, positionsl);
		}
		return ret;

	}

	protected void clearCache() {
		allPowers.clear();
		allProfits.clear();
		allTasks.clear();
		appName2Index.clear();
		appPowers = null;
		hpcExecuteds.clear();
		hpcIntervals.clear();
		hpcTasks.clear();
		index2AppName = null;
		index2ServName = null;
		isMigrateds = null;
		migrationCosts = null;
		appPositions = null;
		servName2Index.clear();
		servPowers = null;
		webModes.clear();
	}

	//
	// Solve the problem
	//

	public SchedulingResult solve(SchedulingModel m) {
		clearCache();
		model = m;
		// first we create indexes for the servers and the applications
		affectIndexes();
		// then we create position variables for each interval and each app
		makeAppPositions();
		// we make migration cost at each interval : a VM is migrated if was running
		// at previous interval and present interval is different from previous
		makeIsMigrateds();
		makeMigrationCosts();

		makeWebTasks();
		makeHPCTasks();
		makeAppPowers();
		makeReductionTasks();
		makeCumulative();
		makePackings();

		if (debug) {
			getSolver().showDecisions(new IOutputFactory.DefaultDecisionMessage(getSolver()) {

				@Override
				public String print() {
					Variable[] vars = getSolver().getStrategy().getVariables();
					StringBuilder s = new StringBuilder(32);
					for (int i = 0; i < vars.length; i++) {
						s.append(vars[i]).append(' ');
					}
					return s.toString();
				}
			});
			getSolver().showContradiction();
			getSolver().showSolutions();
		}

		setObjective(ResolutionPolicy.MAXIMIZE, makeObjective());
		Solution s = new Solution(this);
		while (solve()) {
			s.record();
		}
		return getSolver().isFeasible() == ESat.TRUE ? extractResult(s) : null;
	}

	public static int maxIntArray(int... vals) {
		if (vals == null || vals.length == 0) {
			throw new UnsupportedOperationException();
		}
		int ret = vals[0];
		for (int i : vals) {
			ret = Math.max(ret, i);
		}
		return ret;
	}

	public static int minIntArray(int... vals) {
		if (vals == null || vals.length == 0) {
			throw new UnsupportedOperationException();
		}
		int ret = vals[0];
		for (int i : vals) {
			ret = Math.min(ret, i);
		}
		return ret;
	}

	public static SchedulingResult solv(SchedulingModel m) {
		return new AppScheduler().solve(m);
	}

	public static SchedulingResult debugSolv(SchedulingModel m) {
		return new AppScheduler().withDebug(true).solve(m);
	}

}
