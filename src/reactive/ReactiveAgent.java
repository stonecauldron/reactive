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

import java.util.Random;

public class ReactiveAgent implements ReactiveBehavior {

    private int numActions = 0;
    private Graph graph;
    private Agent myAgent;

    private double cumulatedReward = 0.0;


    private Random random;
    private Double pPickup;


	@Override
	public void setup(Topology topology, TaskDistribution td, Agent agent) {

		// Reads the discount factor from the agents.xml file.
		// If the property is not present it defaults to 0.95
		Double discount = agent.readProperty("discount-factor", Double.class,
				0.95);

        this.random = new Random();
        this.myAgent = agent;
        this.graph = new Graph(td,topology);
        this.pPickup = discount;

        while(this.graph.update(discount)>0.1);

    }

	@Override
	public Action act(Vehicle vehicle, Task availableTask) {

        return intelligentAct(vehicle,availableTask);
	}



    private Action intelligentAct(Vehicle vehicle, Task availableTask){
        Action action;

        State currentState = graph.getState(vehicle.getCurrentCity(),
                availableTask == null ? null : availableTask.deliveryCity);

        ActionEdge bestAction = currentState.getBestAction();
        cumulatedReward += bestAction.getImmediateReward();


        switch (bestAction.getActionType()) {
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
            System.out.println(this);
        }
        numActions++;

        return action;
    }


    private Action dummyAct(Vehicle vehicle, Task availableTask) {
        Action action;

        if (availableTask == null || random.nextDouble() > pPickup) {
            Topology.City currentCity = vehicle.getCurrentCity();
            action = new Move(currentCity.randomNeighbor(random));
        } else {
            action = new Pickup(availableTask);
        }

        if (numActions >= 1) {
            System.out.println(this);
        }
        numActions++;

        return action;
    }



    @Override
    public String toString(){
        return "The total profit after "+numActions+" actions is "
                + myAgent.getTotalProfit()+" (average profit: "
                +(myAgent.getTotalProfit() / (double)numActions)+")" +
                "average reward by km : " + cumulatedReward/numActions;
    }


}
