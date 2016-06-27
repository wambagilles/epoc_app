package fr.lelouet.choco.limitpower.model.parser.yumbo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.lelouet.choco.limitpower.model.PowerMode;
import fr.lelouet.choco.limitpower.model.SchedulingProblem;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * parse the trace and generates a problem from it.
 *
 * @author Guillaume Le Louët
 */
public class DataLoader {

	private static final Logger logger = LoggerFactory.getLogger(DataLoader.class);

	public static final int LASTFILENB = 23;

	/**
	 * return {CPU[vm][itv], RAM[vm][itv]}
	 *
	 * @return
	 */
	public static int[][][] loadRawData() {
		String firstFileName = "./resources/traces/0.csv";
		long nbVM = 0;
		try (Stream<String> lines = Files.lines(Paths.get(firstFileName))) {
			nbVM = lines.count();
		} catch (IOException e) {
			DataLoader.logger.warn("", e);
			return null;
		}
		int[][] ram = new int[(int) nbVM][];
		int[][] cpu = new int[(int) nbVM][];
		int[][][] ret = { ram, cpu };
		for (int i = 0; i < ram.length; i++) {
			ram[i] = new int[DataLoader.LASTFILENB + 1];
			cpu[i] = new int[DataLoader.LASTFILENB + 1];
		}
		for (int itv = 0; itv <= DataLoader.LASTFILENB; itv++) {
			String filename = "./resources/traces/" + itv + ".csv";
			File f = new File(filename);
			if (!f.exists()) {
				throw new UnsupportedOperationException("missing file " + filename);
			}
			try (BufferedReader br = new BufferedReader(new FileReader(f))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] words = line.split(" ");
					if (words.length == 3) {
						int vm_i = Integer.parseInt(words[0]);
						ret[0][vm_i][itv] = (int) (10000 * Double.parseDouble(words[1]));
						ret[1][vm_i][itv] = (int) (10000 * Double.parseDouble(words[2]));
					} else {
						DataLoader.logger.warn("ncorrect number of words in line" + line);
					}
				}
			} catch (IOException e) {
				throw new UnsupportedOperationException("wtf ?", e);
			}
		}
		return ret;
	}

	public int nbServers = 150;
	public int serverRAM = 10000;
	public int serverPWR = 10000;

	/**
	 * only load the real data from the traces : VM CPU and ram
	 *
	 * @param filename
	 *          the name of the file to load traces from
	 * @param ram
	 *          map of ram
	 * @return a new problem containing the VM cpu and the ram . null if any issue.
	 */
	public SchedulingProblem loadRaw(String filename, TObjectIntHashMap<String> ram) {
		File f = new File(filename);
		if (!f.exists()) {
			return null;
		}
		SchedulingProblem ret = new SchedulingProblem();
		ret.nbIntervals = 1;
		ret.setResource("ram", ram::get);
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] words = line.split(" ");
				if (words.length == 3) {
					String name = "vm_" + words[0];
					int power = (int) (serverPWR * Double.parseDouble(words[1]));
					if (power > serverPWR) {
						DataLoader.logger.warn("power excess : " + name + " consumes " + power + ", max is " + serverPWR);
						power = serverPWR;
					}
					int ramuse = (int) (serverRAM * Double.parseDouble(words[2]));
					if (ramuse > serverRAM) {
						DataLoader.logger.warn("ram excess : " + name + " consumes " + ramuse + ", max is " + serverRAM);
						ramuse = serverRAM;
					}
					ret.addWeb(name, power, 0);
					ram.put(name, ramuse);
				}
			}
		} catch (IOException e) {
			throw new UnsupportedOperationException("wtf ?", e);
		}
		return ret;
	}

	/**
	 * generates a model according to traces. Since the traces contain VM ram and CPU load, we multiply those load by
	 * 10000 to have a base value. In order to host those VMs, we also create servers.
	 *
	 * @param filenumber
	 *          the number of the file to load traces from
	 * @return a new model or null if an issue is encoutered during parsing/creation/whatever.
	 */
	public SchedulingProblem load(int filenumber) {
		String location = "./resources/traces/" + filenumber + ".csv";
		TObjectIntHashMap<String> ram = new TObjectIntHashMap<>();
		SchedulingProblem ret = loadRaw(location, ram);
		for (int i = 0; i < nbServers; i++) {
			ret.server("s_" + i).maxPower = serverPWR;
			ram.put("s_" + i, serverRAM);
		}
		return ret;
	}

	public SchedulingProblem[] loadAll() {
		return IntStream.rangeClosed(0, 23).mapToObj(this::load).collect(Collectors.toList())
				.toArray(new SchedulingProblem[] {});
	}

	public SchedulingProblem[] loadRawAll() {
		return IntStream.rangeClosed(0, 23)
				.mapToObj(i -> loadRaw("./resources/traces/" + i + ".csv", new TObjectIntHashMap<>()))
				.collect(Collectors.toList()).toArray(new SchedulingProblem[] {});
	}

	/**
	 * inject linear profit to web apps. the web apps must have only one webmode.
	 * 
	 * @param pb
	 *          the problem
	 * @param names
	 *          the names of the web apps to consider, should be a substream of pb.webNames().
	 * @param mults
	 *          multiplies of profit/power, one for each additionnal web mode.
	 */
	public static void injectLinearProfits(SchedulingProblem pb, Stream<String> names, double... mults) {
		if (mults == null || mults.length == 0 || mults.length == 1 && mults[0] == 1.0) {
			return;
		}
		names.forEach(appname -> {
			List<PowerMode> l = pb.getWebPowerModes(appname);
			PowerMode pm = l.get(0);
			pm.profit = pm.power;
			for (double mult : mults) {
				pb.addWeb(appname, (int) (pm.power * mult), (int) (pm.profit * mult));
			}
		});
	}

	public static void main(String[] args) {
		int[][] cpu = DataLoader.loadRawData()[0];
		int nbHPC = 0;
		for (int vmi = 0; vmi < cpu.length; vmi++) {
			int[] vmcpu = cpu[vmi];
			int last = vmcpu[0];
			double maxfall = Double.MAX_VALUE;
			for (int idx = 1; idx < vmcpu.length; idx++) {
				int val = vmcpu[idx];
				maxfall = Double.min(maxfall, 1.0 * val / last);
// System.err.println("new maxfall" + maxfall + " val" + val + " last" + last);
				last = val;
			}
			if (Double.isNaN(maxfall) || maxfall < 0.1) {
				nbHPC++;
			}
			System.err.println("" + vmi + "\t" + maxfall);
		}
		System.err.println("nb HPC : " + nbHPC);
	}

	public static void main3(String[] args) {
		new DataLoader();
		SchedulingProblem[] models = new DataLoader().loadRawAll();
		for (int vi = 0; vi < models[0].nbApps(); vi++) {
			String vname = "vm_" + vi;
			System.err.print(vname);
			int min = Integer.MAX_VALUE;
			int max = 0;
			for (SchedulingProblem m : models) {
				int power = m.getWebPowerModes(vname).get(0).power;
				min = Math.min(min, power);
				max = Math.max(max, power);
				System.err.print("\t" + power);
			}
			System.err.println("\tstats\t" + min + "\t" + max + "\t" + 1.0 * max / min);
		}
	}

	public static void main2(String[] args) {
		new DataLoader();
		SchedulingProblem[] models = new DataLoader().loadAll();
		System.err.println("itv\t#ram\t#pwr\tavgPWR\t#app\t#<.1%\t#<1%\t#<10%");
		for (int i = 0; i < models.length; i++) {
			SchedulingProblem m = models[i];
			long avgpwr = m.streamMaxPwr().sum() / m.appNames().count();
			long nb1pct = m.streamMaxPwr().filter(p -> p < 10000 / 1000).count();
			long nb5pct = m.streamMaxPwr().filter(p -> p < 10000 / 100).count();
			long nb10pct = m.streamMaxPwr().filter(p -> p < 10000 / 10).count();
			System.err.println(" " + i + "\t" + m.getLoad("ram") + "\t" + m.getMinPwrLoad() + "\t" + avgpwr + "\t"
					+ m.appNames().count() + "\t" + nb1pct + "\t" + nb5pct + "\t" + nb10pct);
		}
	}

}
