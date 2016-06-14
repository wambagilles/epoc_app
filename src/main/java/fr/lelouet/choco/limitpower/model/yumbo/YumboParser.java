package fr.lelouet.choco.limitpower.model.yumbo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.SchedulingModel;
import fr.lelouet.choco.limitpower.SchedulingResult;
import gnu.trove.map.hash.TObjectIntHashMap;

/**
 * parse the trace and generates a problem from it.
 *
 * @author Guillaume Le Louët
 *
 */
public class YumboParser {

	/**
	 * generates a model according to traces. Since the traces contain VM ram and
	 * CPU load, we multiply those load by 10000 to have a base value. In order to
	 * host those VMs, we also create servers.
	 *
	 * @param filenumber
	 *          the number of the file to load traces from
	 * @param nbServers
	 *          the number of servers to host the vms
	 * @param pwrLoad
	 *          total power load of the center
	 * @param ramLoad
	 *          total ram load of the center
	 * @return a new model or null if an issue is encoutered during
	 *         parsing/creation/whatever.
	 */
	public static SchedulingModel load(int filenumber, int nbServers, int pwrLoad, int ramLoad) {
		String location = "./resources/traces/" + filenumber + ".csv";
		File f = new File(location);
		if (!f.exists()) {
			return null;
		}
		SchedulingModel ret = new SchedulingModel();
		ret.nbIntervals = 1;
		TObjectIntHashMap<String> ram = new TObjectIntHashMap<>();
		int totalram = 0, totalpower = 0;
		ret.setResource("ram", ram::get);
		try (BufferedReader br = new BufferedReader(new FileReader(f))) {
			String line;
			while ((line = br.readLine()) != null) {
				String[] words = line.split(" ");
				if (words.length == 3) {
					String name = "vm_" + words[0];
					int power = (int) (10000 * Double.parseDouble(words[1]));
					totalpower += power;
					int ramuse = (int) (10000 * Double.parseDouble(words[2]));
					totalram += ramuse;
					ret.addWeb(name, power, 0);
					ram.put(name, ramuse);
				}
			}
		} catch (IOException e) {
			throw new UnsupportedOperationException("wtf ?", e);
		}
		ret.setPower(totalpower);
		int serverRam = totalram * 100 / ramLoad;
		int serverPwr = totalpower * 100 / pwrLoad;
		for (int si = 0; si < nbServers; si++) {
			String name = "s_" + si;
			ret.server(name).maxPower = serverPwr;
			ram.put(name, serverRam);
		}
		return ret;
	}

	public static void main(String[] args) {
		SchedulingModel model = load(2, 5, 10, 20);
		SchedulingResult res = new AppScheduler().solve(model);
		System.err.println("" + res);
	}

}
