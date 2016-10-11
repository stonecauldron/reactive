package reactive;

import logist.agent.Agent;
import logist.behavior.ReactiveBehavior;
import logist.plan.Action;
import logist.plan.Action.Move;
import logist.plan.Action.Pickup;
import logist.simulation.Vehicle;
import logist.task.Task;
import logist.task.TaskDistribution;
import logist.topology.Topology;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class ReactiveAgent implements ReactiveBehavior {

	private Random random;
    private double discount;
    private int numActions;
    private Agent myAgent;
    private Topology topology;
    private TaskDistribution td;

    // best action to take associated with each state
    private Map<State, ActionValue> V;

	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

		this.random = new Random();
        this.discount = discount;
        this.numActions = 0;
        this.myAgent = agent;
        this.topology = topology;
        this.V = new HashMap<>();
        this.td = td;

        reinforcementLearning();
    }

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {
		Action action;

        State currentState = new State(vehicle.getCurrentCity()
                , availableTask == null ? null : availableTask.deliveryCity
                , vehicle.getCurrentCity().neighbors());

        ReactiveAction bestAction = V.get(currentState).getAction();

        switch (bestAction.getType()) {
            case MOVE:
                action = new Move(bestAction.getDestination());
                break;
            case PICKUP:
                action = new Pickup(availableTask);
                break;
            default:
                action = null;
        }

		if (numActions >= 1) {
			System.out.println("The total profit after "+numActions+" actions is "+myAgent.getTotalProfit()+" (average profit: "+(myAgent.getTotalProfit() / (double)numActions)+")");
		}
		numActions++;
		
		return action;
	}

    private void reinforcementLearning() {
        boolean goodEnough = false;

        // get all possible states
        Set<State> states = State.generateAllStates(topology.cities());

        // initialize V
        for (State s : states) {
            V.put(s, new ActionValue(null, 0.0));
        }

        RewardTable rewardTable = new RewardTable(td);
        ProbabilityTransitionTable p = new ProbabilityTransitionTable(topology.cities(), td);

        // create Q table and initialize it
        Map<State, Map<ReactiveAction, Double>> Q = new HashMap<>();
        for (State s : states) {
            // initialize inner hashmap
            Q.put(s, new HashMap<>());
            for (ReactiveAction a : ReactiveAction.generateActionsForState(topology.cities(), s)) {
                Q.get(s).put(a, 0.0);
            }
        }

        while (!goodEnough) {
            Double maxValueDifference = 0.0;
            for (State s : Q.keySet()) {
                for (ReactiveAction a : Q.get(s).keySet()) {
                    double qValue = rewardTable.getReward(s, a);

                    // go through all possible state transitions
                    for (State nextState : State.generateAllNextStates(s, a, topology.cities())) {
                        qValue += discount * p.getProbability(s, a, nextState) * V.get(nextState).getValue();
                    }

                    Q.get(s).put(a, qValue);
                }
                double bestValue = -Double.MAX_VALUE;
                ReactiveAction bestAction = null;
                for (ReactiveAction a : Q.get(s).keySet()) {
                    double currentValue = Q.get(s).get(a);
                    if (currentValue >= bestValue) {
                        bestValue = currentValue;
                        bestAction = a;
                    }
                }
                Double valueDifference = Math.abs(V.get(s).getValue() - bestValue);
                maxValueDifference = Math.max(valueDifference, maxValueDifference);

                V.put(s, new ActionValue(bestAction, bestValue));
            }
            // stop when difference between successive iterations is less than a given value
            if (maxValueDifference < 0.1) {
                goodEnough = true;
            }
        }
    }
}
