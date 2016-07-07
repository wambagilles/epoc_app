package fr.lelouet.choco.limitpower.model.parser.yumbo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * parse the traces provided by yumbo
 *
 * @author Guillaume Le Louët
 */
public class YumboParser {

	private static final Logger logger = LoggerFactory.getLogger(YumboParser.class);

	/** yumbo traces go from 0 to 23 included */
	public static final int LASTFILENB = 23;

	/**
	 * trace of 24 1-hour interval. Contains VM index, host cpu load and host ram load. Since we
	 *
	 * @return {CPU[vm][itv], RAM[vm][itv]}
	 */
	public static double[][][] loadRawData() {
		String firstFileName = "./resources/traces/0.csv";
		long nbVM = 0;
		try (Stream<String> lines = Files.lines(Paths.get(firstFileName))) {
			nbVM = lines.count();
		} catch (IOException e) {
			YumboParser.logger.warn("", e);
			return null;
		}
		double[][] ram = new double[(int) nbVM][];
		double[][] cpu = new double[(int) nbVM][];
		for (int i = 0; i < ram.length; i++) {
			ram[i] = new double[YumboParser.LASTFILENB + 1];
			cpu[i] = new double[YumboParser.LASTFILENB + 1];
		}
		for (int itv = 0; itv <= YumboParser.LASTFILENB; itv++) {
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
						ram[vm_i][itv] = Double.parseDouble(words[1]);
						cpu[vm_i][itv] = Double.parseDouble(words[2]);
					} else {
						YumboParser.logger.warn("incorrect number of words in line" + line + " file " + f);
					}
				}
			} catch (IOException e) {
				throw new UnsupportedOperationException("wtf ?", e);
			}
		}
		return new double[][][] { ram, cpu };
	}

}
