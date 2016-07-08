/**
 *
 */
package fr.lelouet.choco.limitpower.model.parser.yumbo;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lelouet.choco.limitpower.model.Objective;
import fr.lelouet.choco.limitpower.model.PowerMode;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * decorate data extracted from yumboparser
 *
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 */
public class YumboDecoration {

	private static final Logger logger = LoggerFactory.getLogger(YumboDecoration.class);

	public YumboDecoration() {
		this(YumboParser.loadRawData());
	}

	final double[][] ram, cpu;
	final int nbVMs;

	public YumboDecoration(double[][][] data) {
		this(data[0], data[1]);
	}

	public YumboDecoration(double[][] cpu, double[][] ram) {
		assert cpu.length == ram.length : "different size for CPU(" + cpu.length + ") and ram(" + ram.length
				+ ") intervals";
		this.cpu = cpu;
		this.ram = ram;
		nbVMs = cpu[0].length;
	}

	public int serverRAM = 10000;
	public int serverPWR = 10000;

	public int nbIntervals = 1;

	public Objective objective = Objective.PROFIT;

	/**
	 * load actual web data.
	 *
	 * @param itv
	 *          the interval to load the web trace from
	 * @param ret
	 *          the problem to add the data in, or null to create a new one
	 * @param ram
	 *          ram specification to fill. if ret is null, will be added in it. NPE if null.
	 * @return ret, or a new problem if ret null ; null if any issue
	 */
	public SchedulingProblem loadWebs(int itv, SchedulingProblem ret, TObjectIntHashMap<String> ram) {
		if (ret == null) {
			ret = new SchedulingProblem();
			ret.nbIntervals = nbIntervals;
			ret.objective = objective;
			ret.setResource("ram", ram::get);

		}
		for (int vmi = 0; vmi < nbVMs; vmi++) {
			String name = "vm_" + vmi;
			int power = (int) (serverPWR * cpu[itv][vmi]);
			if (power > serverPWR) {
				YumboDecoration.logger.warn("power excess : " + name + " consumes " + power + ", max is " + serverPWR);
				power = serverPWR;
			}
			int ramuse = (int) (serverRAM * this.ram[itv][vmi]);
			if (ramuse > serverRAM) {
				YumboDecoration.logger.warn("ram excess : " + name + " consumes " + ramuse + ", max is " + serverRAM);
				ramuse = serverRAM;
			}
			ret.addWeb(name, power, 0);
			ram.put(name, ramuse);
		}
		return ret;
	}

	public double[] profitMults = null;

	/**
	 * inject linear profit to web apps. the web apps must have only one webmode.
	 *
	 * @param pb
	 *          the problem
	 * @param names
	 *          the names of the web apps to consider, should be a substream of pb.webNames().
	 */
	public void injectLinearProfits(SchedulingProblem pb, Stream<String> names) {
		if (profitMults == null || profitMults.length == 0 || profitMults.length == 1 && profitMults[0] == 1.0) {
			return;
		}
		names.forEach(appname -> {
			List<PowerMode> l = pb.getWebPowerModes(appname);
			PowerMode pm = l.get(0);
			pm.profit = pm.power;
			for (double mult : profitMults) {
				pb.addWeb(appname, (int) (pm.power * mult), (int) (pm.profit * mult));
			}
		});
	}

	public boolean bAddHPC = false;

	/**
	 * for each web app, add an HPC task of same memory, and number of intervalles is the minimum number of intervalle to
	 * execute the total CPU of the app at 50% server load. eg if task load is 10%for each intervalle, we have 24
	 * intervalles so app duration is ceil(10*24/50)=5.
	 *
	 * @param pb
	 */
	public void addHPC(SchedulingProblem pb, TObjectIntHashMap<String> ram) {
		IntStream.range(0, cpu[0].length).forEach(vmi -> {
			String name = "hpc_" + vmi;
			int hpcRam = (int) (IntStream.range(0, cpu.length).mapToDouble(itv -> this.ram[itv][vmi]).max().getAsDouble()
					* serverRAM);
			ram.put(name, hpcRam);
			int nbIntervalles = (int) Math.ceil(IntStream.range(0, cpu.length).mapToDouble(itv -> cpu[itv][vmi]).sum() * 2);
			pb.addHPC(name, 0, nbIntervalles, serverPWR / 2, serverPWR * nbIntervalles / 2, -1);
		});
	}

	/** the load of the servers wth regard to CPU/ram and web apps */
	public int webLoad = 50;
	public boolean bAddServers = true;

	/**
	 * generates servers.
	 *
	 * @return a new model or null if an issue is encoutered during parsing/creation/whatever.
	 */
	public SchedulingProblem addServers(SchedulingProblem ret, TObjectIntHashMap<String> ramm) {
		double ramNeededServers = IntStream.range(0, cpu[0].length).mapToDouble(vmi -> {
			return IntStream.range(0, cpu.length).mapToDouble(itv -> ram[itv][vmi]).max().getAsDouble();
		}).sum();
		System.err.println("ram requested servers : " + ramNeededServers);
		double cpuNeededServers = IntStream.range(0, cpu[0].length).mapToDouble(vmi -> {
			return IntStream.range(0, cpu.length).mapToDouble(itv -> cpu[itv][vmi]).max().getAsDouble();
		}).sum();

		System.err.println("cpu requested servers : " + cpuNeededServers);
		int nbServers = (int) Math.ceil(Math.max(ramNeededServers, cpuNeededServers) * 100 / webLoad);
		YumboDecoration.logger.info("requesting " + nbServers + " servers");

		for (int i = 0; i < nbServers; i++) {
			ret.server("s_" + i).maxPower = serverPWR;
			ramm.put("s_" + i, serverRAM);
		}
		return ret;
	}

	public SchedulingProblem load(int itv) {
		TObjectIntHashMap<String> ramm = new TObjectIntHashMap<>();
		SchedulingProblem ret = loadWebs(itv, null, ramm);
		injectLinearProfits(ret, ret.webNames());
		if (bAddHPC) {
			addHPC(ret, ramm);
		}
		if (bAddServers) {
			addServers(ret, ramm);
		}
		return ret;
	}

}
