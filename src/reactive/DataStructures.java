package reactive;

import logist.task.TaskDistribution;
import logist.topology.Topology.City;

import java.util.List;

enum ActionType {
    MOVE,
    PICKUP
}

class State {
    private City currentCity, taskDestination;
    private List<City> neighbours;

    public State(City c, City td, List<City> n) {
        this.currentCity = c;
        this.taskDestination = td;
        this.neighbours = n;
    }

    City getCurrentCity() {
        return currentCity;
    }

    boolean hasTask() {
        return taskDestination != null;
    }

    City getTaskDestination() {
        return taskDestination;
    }
}

class ReactiveAction {
    private ActionType type;
    private City destination;

    public ReactiveAction(ActionType type, City destination) {
        this.type = type;
        this.destination = destination;
    }

    City getDestination() {
        return destination;
    }

    ActionType getType() {
        return type;
    }
}

class RewardTable {
    private TaskDistribution td;

    public RewardTable(TaskDistribution td) {
        this.td = td;
    }

    public double getReward(State s, ReactiveAction a) {
        City currentCity = s.getCurrentCity();
        City destination = a.getDestination();

        switch (a.getType()) {
            case MOVE:
                return -currentCity.distanceTo(destination);

            case PICKUP:
                double averageReward = td.reward(currentCity, destination);
                return averageReward * (1 / currentCity.distanceTo(destination));

            default:
                return 0.0;
        }
    }
}

class ProbabilityTransitionTable {
    private List<City> cities;
    private TaskDistribution td;

    public ProbabilityTransitionTable(List<City> cities, TaskDistribution td) {
        this.cities = cities;
        this.td = td;
    }

    public double getProbability(State initialState, ReactiveAction a, State endState) {
        City destination = a.getDestination();
        City taskDestination = endState.getTaskDestination();

        if (endState.hasTask()) {
            return td.probability(destination, taskDestination);
        } else {
            return computeProbabilityNoTask(destination);
        }
    }

    /**
     * Compute the probability that there is no task in city c
     */
    private double computeProbabilityNoTask(City c) {
        double acc = 0;
        for (City other : cities) {
            acc += td.probability(c, other);
        }
        return 1 - acc;
    }
}
































