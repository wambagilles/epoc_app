package fr.lelouet.choco.limitpower.model.parser.groovy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import fr.lelouet.choco.limitpower.AppScheduler;
import fr.lelouet.choco.limitpower.Model;
import fr.lelouet.choco.limitpower.Model.NamedWeb;
import fr.lelouet.choco.limitpower.Model.Objective;
import fr.lelouet.choco.limitpower.model.HPC;
import groovy.lang.GroovyShell;

public class GroovyParser {

	protected Model model = new Model();

	GroovyShell shell = new GroovyShell();

	public GroovyParser() {
		shell.getContext().setVariable("web", new WebLink());
		shell.getContext().setVariable("hpc", new HpcLink());
		shell.getContext().setVariable("total", new TotalLink());
		shell.getContext().setVariable("limit", new LimitLink());
	}

	public void parseLine(String line) {
		shell.evaluate(line);
	}

	@SuppressWarnings("resource")
	public Model parseFile(String filename) throws FileNotFoundException {
		new BufferedReader(new FileReader(filename)).lines().forEach(this::parseLine);
		return model;
	}

	public Model getModel() {
		return model;
	}

	protected class WebLink {
		public NamedWeb getAt(String name) {
			return model.nameWeb(name);
		}
	}

	protected class HpcLink {
		public HPC getAt(String name) {
			HPC ret = model.getHPC(name);
			if (ret == null) {
				ret = new HPC(0, 1, 0, 0, 100);
				model.addHPC(name, ret);
			}
			return ret;
		}
	}

	protected class TotalLink {

		public TotalLink power(int power) {
			model.maxPower = power;
			return this;
		}

		public TotalLink intervals(int nb) {
			model.nbIntervals = nb;
			return this;
		}

		public TotalLink objective(String name) {
			model.objective = Objective.valueOf(name);
			return this;
		}

	}

	protected class LimitLink {

		public void putAt(int idx, int power) {
			model.setLimit(idx, power);
		}
	}

	public static void main(String[] args) throws FileNotFoundException {
		for (String file : args) {
			Model m = new GroovyParser().parseFile(file);
			// System.out.println("solving : " + m);
			System.out.println(AppScheduler.solv(m));
		}
	}

}
