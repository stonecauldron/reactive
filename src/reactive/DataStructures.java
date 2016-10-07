package reactive;

import logist.task.TaskDistribution;
import logist.topology.Topology.City;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    static Set<State> generateAllStates(List<City> cities) {
        Set<State> S = new HashSet<>();

        for (City current : cities) {
            // add also null destinations
            S.add(new State(current, null, current.neighbors()));
            for (City destination : cities) {
                if (current != destination) {
                    S.add(new State(current, destination, current.neighbors()));
                }
            }
        }
        return S;
    }

    static Set<State> generateAllNextStates(State current, ReactiveAction action, List<City> cities) {
        Set<State> S = new HashSet<>();
        City destination = action.getDestination();

        // state where there is no task present
        S.add(new State(destination, null, destination.neighbors()));

        // add state where there is a task for all possible cities
        for (City taskDestination : cities) {
            if (taskDestination != destination) {
                S.add(new State(destination, taskDestination, destination.neighbors()));
            }
        }
        return S;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        State state = (State) o;

        if (!currentCity.equals(state.currentCity)) return false;
        if (taskDestination != null ? !taskDestination.equals(state.taskDestination) : state.taskDestination != null)
            return false;
        return neighbours.equals(state.neighbours);
    }

    @Override
    public int hashCode() {
        int result = currentCity.hashCode();
        result = 31 * result + (taskDestination != null ? taskDestination.hashCode() : 0);
        result = 31 * result + neighbours.hashCode();
        return result;
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

class ActionValue {
    private ReactiveAction action;
    private Double value;

    ActionValue(ReactiveAction action, Double value) {
        this.action = action;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ActionValue that = (ActionValue) o;

        if (!action.equals(that.action)) return false;
        return value.equals(that.value);

    }

    @Override
    public int hashCode() {
        int result = action.hashCode();
        result = 31 * result + value.hashCode();
        return result;
    }

    ReactiveAction getAction() {
        return action;
    }

    Double getValue() {
        return value;
    }
}

class ReactiveAction {
    private ActionType type;
    private City destination;

    ReactiveAction(ActionType type, City destination) {
        this.type = type;
        this.destination = destination;
    }

    static Set<ReactiveAction> generateActionsForState(List<City> cities, State s) {
        Set<ReactiveAction> A = new HashSet<>();
        for (ActionType type : ActionType.values()) {
            if (type == ActionType.MOVE) {
                // only generate Moves to neighbours of the current city
                for (City destination : s.getCurrentCity().neighbors()) {
                    A.add(new ReactiveAction(type, destination));
                }
            } else if (type == ActionType.PICKUP) {
                // only generate pickup action if there is a task avalaible
                if (s.getTaskDestination() != null) {
                    A.add(new ReactiveAction(type, s.getTaskDestination()));
                }
            }
        }
        return A;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReactiveAction that = (ReactiveAction) o;

        if (type != that.type) return false;
        return destination.equals(that.destination);

    }

    @Override
    public int hashCode() {
        int result = type.hashCode();
        result = 31 * result + destination.hashCode();
        return result;
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

    RewardTable(TaskDistribution td) {
        this.td = td;
    }

    double getReward(State s, ReactiveAction a) {
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

    ProbabilityTransitionTable(List<City> cities, TaskDistribution td) {
        this.cities = cities;
        this.td = td;
    }

    double getProbability(State initialState, ReactiveAction a, State endState) {
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
