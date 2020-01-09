package bconsensus;

import org.apache.commons.lang3.tuple.Pair;

import repast.simphony.random.RandomHelper;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.grid.Grid;

public class ByzantineProcess extends Process {

	public ByzantineProcess(ContinuousSpace<Object> space, Grid<Object> grid, int id) {
		super(space, grid, id);
	}
	
	@Override
	protected void consensus() {
		if (this.round % 3 == 0) {					// Round 1
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);
				this.label = false;
				
				if (this.getPhase() == 0)	//
					this.proposed_v = RandomHelper.nextIntFromTo(0, 1);
				else	//
					this.proposed_v = 1 - this.value;	//
				
				System.out.println(this.id + " broadcast BYZ value " + this.proposed_v + " with label " + this.label);
				this.broadcast(this.round, this.proposed_v);
			}
		} else if (this.round % 3 == 1) {			// Round 2
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);
				this.proposed_v = 1 - this.value;
				System.out.println(this.id + " broadcast BYZ value " + this.proposed_v + " with label " + this.label + " for round " + this.round);
				this.broadcast(this.round, this.proposed_v);
			}
		} else if (this.round % 3 == 2) {			// Round 3
			if (!this.steps.containsKey(Pair.of(this.id, this.round))) {
				this.steps.put(Pair.of(this.id, this.round), 0);
				
				this.proposed_v = this.value;
				this.label = false;
				System.out.println(this.id + " broadcast BYZ value " + this.proposed_v + " with label " + this.label + " for round " + this.round);
				this.broadcast(this.round, this.proposed_v);
			}
		}
	}
	
}
