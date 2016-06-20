package fr.lelouet.choco.limitpower.model.parser.yumbo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import fr.lelouet.choco.limitpower.SchedulingModel;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * parse the trace and generates a problem from it.
 *
 * @author Guillaume Le Louët
 */
public class DataLoader {

	public SchedulingModel loadRaw(String filename, TObjectIntHashMap<String> ram) {
		File f = new File(filename);
		if (!f.exists()) {
			return null;
		}
		SchedulingModel ret = new SchedulingModel();
		ret.nbIntervals = 1;
		ret.setResource("ram", ram::get);
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] words = line.split(" ");
				if (words.length == 3) {
					String name = "vm_" + words[0];
					int power = (int) (10000 * Double.parseDouble(words[1]));
					int ramuse = (int) (10000 * Double.parseDouble(words[2]));
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
	 * generates a model according to traces. Since the traces contain VM ram and
	 * CPU load, we multiply those load by 10000 to have a base value. In order to
	 * host those VMs, we also create servers.
	 *
	 * @param filenumber
	 *          the number of the file to load traces from
	 * @return a new model or null if an issue is encoutered during
	 *         parsing/creation/whatever.
	 */
	public SchedulingModel load(int filenumber) {
		String location = "./resources/traces/" + filenumber + ".csv";
		TObjectIntHashMap<String> ram = new TObjectIntHashMap<>();
		SchedulingModel ret = loadRaw(location, ram);
		for (int i = 0; i < 120; i++) {
			ret.server("s_" + i).maxPower = 10000;
			ram.put("s_" + i, 10000);
		}
		return ret;
	}

	public SchedulingModel[] loadAll() {
		return IntStream.rangeClosed(0, 23).mapToObj(this::load).collect(Collectors.toList())
				.toArray(new SchedulingModel[] {});
	}

	public SchedulingModel[] loadRawAll() {
		return IntStream.rangeClosed(0, 23)
				.mapToObj(i -> loadRaw("./resources/traces/" + i + ".csv", new TObjectIntHashMap<>()))
				.collect(Collectors.toList()).toArray(new SchedulingModel[] {});
	}

	public static void main(String[] args) {
		SchedulingModel[] models = new DataLoader().loadRawAll();
		for (int vi = 0; vi < models[0].nbApps(); vi++) {
			String vname = "vm_" + vi;
			System.err.print(vname);
			int min = Integer.MAX_VALUE;
			int max = 0;
			for (SchedulingModel m : models) {
				int power = m.getWebPowerModes(vname).get(0).power;
				min = Math.min(min, power);
				max = Math.max(max, power);
				System.err.print("\t" + power);
			}
			System.err.println("\tstats\t" + min + "\t" + max + "\t" + 1.0 * max / min);
		}
	}

	public static void main2(String[] args) {
		SchedulingModel[] models = new DataLoader().loadAll();
		System.err.println("itv\t#ram\t#pwr\tavgPWR\t#app\t#<.1%\t#<1%\t#<10%");
		for (int i = 0; i < models.length; i++) {
			SchedulingModel m = models[i];
			long avgpwr = m.streamMaxPwr().sum() / m.appNames().count();
			long nb1pct = m.streamMaxPwr().filter(p -> p < 10000 / 1000).count();
			long nb5pct = m.streamMaxPwr().filter(p -> p < 10000 / 100).count();
			long nb10pct = m.streamMaxPwr().filter(p -> p < 10000 / 10).count();
			System.err.println(" " + i + "\t" + m.getLoad("ram") + "\t" + m.getMinPwrLoad() + "\t" + avgpwr + "\t"
					+ m.appNames().count() + "\t" + nb1pct + "\t" + nb5pct + "\t" + nb10pct);
		}
	}

}
