/**
 *
 */
package fr.lelouet.choco.limitpower.model.parser.yumbo;

import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lelouet.choco.limitpower.model.PowerMode;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;
import fr.lelouet.choco.limitpower.model.SchedulingProblem.Objective;
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

	public int nbServers = 150;
	public boolean bAddServers = true;

	/**
	 * generates a model according to traces. Since the traces contain VM ram and CPU load, we multiply those load by
	 * 10000 to have a base value. In order to host those VMs, we also create servers.
	 *
	 * @param filenumber
	 *          the number of the file to load traces from
	 * @return a new model or null if an issue is encoutered during parsing/creation/whatever.
	 */
	public SchedulingProblem addServers(int itv, SchedulingProblem ret) {
		TObjectIntHashMap<String> ram = new TObjectIntHashMap<>();
		for (int i = 0; i < nbServers; i++) {
			ret.server("s_" + i).maxPower = serverPWR;
			ram.put("s_" + i, serverRAM);
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

	public SchedulingProblem load(int itv) {
		SchedulingProblem ret = loadWebs(itv, null, new TObjectIntHashMap<>());
		if (bAddServers) {
			addServers(itv, ret);
		}
		injectLinearProfits(ret, ret.webNames());
		return ret;
	}

}
