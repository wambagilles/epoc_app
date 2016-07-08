/**
 *
 */
package fr.lelouet.choco.limitpower.model;

import java.util.stream.Stream;

import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.search.strategy.strategy.StrategiesSequencer;

import fr.lelouet.choco.limitpower.AppScheduler;

/**
 * @author Guillaume Le Louët [guillaume.lelouet@gmail.com] 2016
 *
 */
@FunctionalInterface
public interface Heuristic {

	public static class Chain implements Heuristic {

		private Heuristic[] elements;

		public Chain(Heuristic... elements) {
			this.elements = elements;
		}

		@Override
		public AbstractStrategy<?> makeStrat(AppScheduler sc) {
			return new StrategiesSequencer(Stream.of(elements).map(h -> h.makeStrat(sc)).toArray(AbstractStrategy[]::new));
		}

	}

	public AbstractStrategy<?> makeStrat(AppScheduler sc);

	/**
	 * chain this heuristic with another, or some other, ones. Perform checks to ensure there is no nested chain.
	 * 
	 * @param followers
	 *          the heuristics to chain after this one ; can be null, in this case will return this.
	 * @return a new heuristic chaining this to the followers if followers are not null, this if no follower.
	 */
	public default Heuristic chain(Heuristic... followers) {
		if (followers == null || followers.length == 0) {
			return this;
		}
		return new Chain(Stream.concat(Stream.of(this), Stream.of(followers))
				.flatMap(h -> h instanceof Chain ? Stream.of(((Chain) h).elements) : Stream.of(h)).toArray(Heuristic[]::new));

	}

}
