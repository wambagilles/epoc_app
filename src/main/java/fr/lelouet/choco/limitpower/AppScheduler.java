
package fr.lelouet.choco.limitpower;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.chocosolver.solver.ResolutionPolicy;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.ICF;
import org.chocosolver.solver.constraints.LCF;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Task;
import org.chocosolver.solver.variables.VF;
import org.chocosolver.util.ESat;

import fr.lelouet.choco.limitpower.model.HPC;
import fr.lelouet.choco.limitpower.model.PowerMode;


/**
 * solve an app scheduling problem with benefit maximisation.
 * <ul>
 * <li>the time is divided into a finite number of slots ; interval 0 is from
 * slot 0 to slot 1, etc. we have {@link #nbIntervals} interval so the times go
 * from 0 to {@link #nbIntervals} included</li>
 * <li>web apps start on 0 and end on {@link #nbIntervals}; have several modes,
 * each consume an amount of power and give a profit</li>
 * <li>hpc app have a given amount of subtask to schedule sequentially on the
 * intervals,each of duration 1, each consumming the same amount of power ; The
 * benefit is given if the hpc task is scheduled entirely before its deadline
 * expires, meaning its last subtask.end <= deadline</li>
 * <li>reduction amounts specify reduction in the total power capacity</li>
 * </ul>
 *
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2015
 *
 */
@SuppressWarnings("serial")
public class AppScheduler extends Solver {

	@SuppressWarnings("unused")
	private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(AppScheduler.class);

	protected Model model = new Model();

	public Model getModel() {
		return model;
	}

	protected IntVar fixed(int i) {
		return VF.fixed(i, this);
	}

	protected IntVar enumerated(String name, int min, int max) {
		return VF.enumerated(name, min, max, this);
	}

	protected IntVar bounded(String name, int min, int max) {
		return VF.bounded(name, min, max, this);
	}

	// all the tasks and power we want to schedule. add a task with addTask, not
	// directly with this method.
	protected List<Task> allTasks = new ArrayList<>();

	protected List<IntVar> allPowers = new ArrayList<>();

	/**
	 *
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
			super(fixed(start), fixed(1), fixed(start+1));
			assert profits.length == powers.length;
			this.name = name;
			this.profits = profits;
			this.powers = powers;
			mode = enumerated(name + "_mode", 0, powers.length - 1);
			power = enumerated(name + "_power", minIntArray(powers), maxIntArray(powers));
			post(ICF.element(power, powers, mode));
			profit = enumerated(name + "_cost", minIntArray(profits), maxIntArray(profits));
			post(ICF.element(profit, profits, mode));
		}

		@Override
		public String toString() {
			return "webTask(start=" + getStart() + ")";
		}
	}

	/**
	 * for each web application, its modes
	 */
	Map<String, List<WebSubClass>> webModes = new HashMap<>();

	/** make one task corresponding to the web apps */
	protected void makeWebTasks() {
		for (Entry<String, List<PowerMode>> e : model.webs.entrySet()) {
			String name = e.getKey();
			int[] appprofits = model.webProfits(name);
			int[] apppower = model.webPowers(name);
			List<WebSubClass> l = new ArrayList<>();
			webModes.put(name, l);

			for(int i=0;i<model.nbIntervals;i++) {
				WebSubClass t = new WebSubClass(name+"_"+i, i, appprofits, apppower);
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
	 * an HPC task is divided in subtasks. each subtask has a length of one and
	 * must be started after the previous task. The benefit of the first subtasks
	 * is 0, the benefit of the last subtasks is its benefit if it is scheduled
	 * before its deadline
	 *
	 * @author Guillaume Le Louët
	 *
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

	/**
	 * make a variable related to an existing interval this variable can take a
	 * value from 0 to the number of inter
	 */
	protected IntVar makeTimeSlotVar(String name) {
		return bounded(name, 0, model.nbIntervals + 1);
	}

	protected void makeHPCTasks() {
		hpcTasks.clear();
		for (Entry<String, HPC> e : model.hpcs.entrySet()) {
			HPC h = e.getValue();
			String n = e.getKey();
			ArrayList<HPCSubTask> l = new ArrayList<>();
			hpcTasks.put(n, l);
			HPCSubTask last = null;
			for (int i = 0; i < h.duration; i++) {
				IntVar start = makeTimeSlotVar(name + "_start");
				IntVar end = makeTimeSlotVar(name + "_end");
				BoolVar onSchedule = ICF.arithm(end, "<=", model.nbIntervals).reif();
				IntVar power = enumerated(n + "_" + i + "_power", 0, h.power);
				post(ICF.element(power, new int[] {
				    0, h.power
				}, onSchedule));
				if (last != null) {
					LCF.ifThen(onSchedule, ICF.arithm(last.getEnd(), "<=", start));
				}
				HPCSubTask t = new HPCSubTask(h, start, end, power, onSchedule);
				l.add(t);
				last = t;
				addTask(t, power);
			}
			IntVar benefit = VF.enumerated(n + "_benefit", new int[] {
			    0, h.profit
			}, this);
			allProfits.add(benefit);
			post(ICF.element(benefit, new int[] {
			    0, h.profit
			}, last.onSchedule));
		}
	}

	//
	// reduction tasks to reduce effective power at a given time
	//

	protected Task reductionTask(int index) {
		return new Task(fixed(index), fixed(1), fixed(index + 1));
	}

	/**
	 * add the task to reduce the total power available on given positions
	 */
	public void makeReductionTasks() {
		for (Entry<Integer, Integer> e : model.getLimits()) {
			if (e.getValue() > 0) {
				addTask(reductionTask(e.getKey()), fixed(Math.min(e.getValue(), model.maxPower)));
			}
		}
	}

	//
	// the cumulative constraint
	//

	/** add the profits and the powers of the tasks */
	public void makeCumulative() {
		totalProfit = bounded("totalProfit", 0, model.getMaxProfit());
		post(ICF.sum(allProfits.toArray(new IntVar[] {}), totalProfit));
		Task[] tasks = allTasks.toArray(new Task[] {});
		IntVar[] heights = allPowers.toArray(new IntVar[] {});
		post(ICF.cumulative(tasks, heights, fixed(model.maxPower)));
	}

	//
	// define objective
	//

	public IntVar makeSubtaskSum(String name, Function<HPCSubTask, IntVar> getter, IntVar initial) {
		IntVar ret = bounded(name, 0, VF.MAX_INT_BOUND);
		// stream of the hpcsubtasks variables
		Stream<IntVar> variables = hpcTasks.values().stream().flatMap(List::stream).map(getter);
		List<IntVar> vars = Stream.concat(variables, Stream.of(initial)).collect(Collectors.toList());
		post(ICF.sum(vars.toArray(new IntVar[] {}), ret));
		return ret;
	}

	public IntVar makeObjective() {
		switch (model.objective) {
			case PROFIT:
				return totalProfit;
			case PROFIT_POWER:
				// obj = profit*maxpower*duration + sum(hpcsubtask.power)
				return makeSubtaskSum("profit_power", HPCSubTask::getPower,
				    VF.scale(totalProfit, model.maxPower * model.nbIntervals));
			case PROFIT_ONSCHEDULE:
				// obj = profit*#subtasks + sum(hpcsubtask.onSchedule)
				return makeSubtaskSum("profit_onschedule", HPCSubTask::getOnSchedule,
				    VF.scale(totalProfit, model.hpcs.values().stream().mapToInt(h -> h.duration).sum()));
			default:
				throw new UnsupportedOperationException("case not supported here : " + model.objective);
		}
	}

	//
	// extract data to a result
	//

	public Result extractResult() {
		Result ret = new Result();
		for (Entry<String, List<WebSubClass>> e : webModes.entrySet()) {
			String name = e.getKey();
			ArrayList<PowerMode> list = new ArrayList<>();
			List<PowerMode> modes = model.getWPowerModes(name);
			for (WebSubClass w : webModes.get(name)) {
				list.add(modes.get(w.mode.getValue()));
			}
			ret.webModes.put(e.getKey(), list);
		}
		for (Entry<String, List<HPCSubTask>> e : hpcTasks.entrySet()) {
			List<Integer> l = new ArrayList<>();
			ret.hpcStarts.put(e.getKey(), l);
			for (HPCSubTask h : e.getValue()) {
				if (h.onSchedule.getValue() == 1) {
					l.add(h.getStart().getValue());
				}
			}
		}
		ret.profit = totalProfit.getValue();
		return ret;
	}

	//
	// Solve the problem
	//

	public Result solve(Model m) {
		model = m;
		allPowers.clear();
		allProfits.clear();
		allTasks.clear();
		makeWebTasks();
		makeHPCTasks();
		makeReductionTasks();
		makeCumulative();
		// plugMonitor((org.chocosolver.solver.search.loop.monitors.IMonitorContradiction)
		// cex -> System.err.println(cex));
		findOptimalSolution(ResolutionPolicy.MAXIMIZE, makeObjective());
		return isFeasible() == ESat.TRUE ? extractResult() : null;
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

	public static Result solv(Model m) {
		return new AppScheduler().solve(m);
	}

}
