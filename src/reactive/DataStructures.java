package reactive;

import logist.task.TaskDistribution;
import logist.topology.Topology;
import logist.topology.Topology.City;

import java.util.*;


enum ActionType {
    MOVE,
    PICKUP
}




class ActionEdge {

    private State fromState;
    private TaskDistribution taskDistribution;
    private ActionType actionType;
    private City destination;


    public ActionEdge(State state, ActionType action, City destination, TaskDistribution distribution) {
        this.actionType = action;
        this.destination = destination;
        this.fromState = state;
        this.taskDistribution = distribution;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public City getDestination(){
        return destination;
    }

    public Double getImmediateReward(){

        switch (getActionType()) {
            case MOVE:
                return -fromState.getCity().distanceTo(getDestination());

            case PICKUP:
                double reward = taskDistribution.reward(fromState.getCity(), getDestination());
                return reward / fromState.getCity().distanceTo(getDestination());

            default:
                return 0.0;
        }

    }

    @Override
    public boolean equals(Object that) {

        return that instanceof ActionEdge
                && ((ActionEdge)that).destination == this.destination;

    }

    @Override
    public int hashCode() {
        return actionType.hashCode() + 31*destination.hashCode();
    }

    @Override
    public String toString(){
        if(this.getActionType() == ActionType.MOVE){
            return "empty vehicle : "+this.fromState.getCity()
                    + " -> " + this.getDestination()
                    + "// immediate reward : " + this.getImmediateReward();
        }
        else {
            return "loaded vehicle : " + this.fromState.getCity()
                    + " -> " +this.getDestination()
                    + "// immediate reward : "+ this.getImmediateReward();
        }
    }


}



class State {


    private City city, taskDestination;
    private TaskDistribution taskDistrib;
    private List<ActionEdge> actions = null;

    private ActionEdge bestAction = null;
    private Double reward = 0.0;


    public State(City c, City td, TaskDistribution taskDistrib) {
        this.city = c;
        this.taskDestination = td;
        this.taskDistrib = taskDistrib;
    }



    public City getCity() {
        return city;
    }


    public boolean hasTask() {
        return taskDestination != null;
    }


    public City getTaskDestination() {
        return taskDestination;
    }

    public List<ActionEdge> getAvailableAction(){

        if(this.actions == null){
            actions = new ArrayList<>();
            if(this.hasTask()){
                actions.add(new ActionEdge(this,ActionType.PICKUP,this.getTaskDestination(),taskDistrib));
            }
            for(City dest : this.getCity().neighbors()){
                if(dest != this.getCity()){
                    actions.add(new ActionEdge(this,ActionType.MOVE, dest,taskDistrib));
                }
            }
        }

        return this.actions;
    }


    public Double probability(){
        return taskDistrib.probability(this.city,this.taskDestination);
    }


    public ActionEdge getBestAction(){
        return bestAction;
    }

    public double getExpectedReward(){
        return reward;
    }


    public double updateExpectedReward(Graph graph, Double discount){

        ActionEdge bestAction = null;
        double bestEv = -Double.MAX_VALUE;

        for(ActionEdge a : getAvailableAction()){

            // immediate reward from the curr action
            double currEv = a.getImmediateReward() + discount*graph.expectedRewardIn(a.getDestination());

            if(currEv > bestEv){
                bestAction = a;
                bestEv = currEv;
            }

        }

        double diff = Math.abs(this.reward - bestEv);
        this.bestAction = bestAction;
        this.reward = bestEv;
        return diff;

    }

    @Override
    public boolean equals(Object that) {

        return that instanceof State
                && ((State)that).city == this.city
                && ((State)that).taskDestination == this.taskDestination;

    }


    @Override
    public int hashCode() {
        return city.hashCode() + 31*(taskDestination != null ? taskDestination.hashCode() : 0);
    }

    @Override
    public String toString(){
        return city + (this.getTaskDestination() != null ? " has a delivered package to " + this.getTaskDestination() :
                " has no package to deliver") + " // expected gain : " + this.getExpectedReward();

    }
}





class Graph {


    private Map<City, Set<State>> states = new HashMap<>();
    private Topology topology;
    private TaskDistribution taskDistribution;
    private Double maxDiff = Double.MAX_VALUE;


    public Graph(TaskDistribution taskDistribution, Topology topology){

        this.topology = topology;
        this.taskDistribution = taskDistribution;

        for (City city : topology.cities()) {

            states.put(city, new HashSet<>());
            // no task on this current city
            states.get(city).add(new State(city, null, taskDistribution));

            for (City destinationTask : topology.cities()) {
                if (city != destinationTask) {
                    states.get(city).add(new State(city, destinationTask, taskDistribution));
                }
            }
        }

    }


    public State getState(City c, City taskDestination){
        for(State s : getStateIt(c)){
            if(s.getTaskDestination() == taskDestination){
                return s;
            }
        }
        return null;
    }


    public Iterable<State> getStateIt(City city){
        return new Iterable<State>() {
            @Override
            public Iterator<State> iterator() {
                return states.get(city).iterator();
            }
        };
    }



    public Iterable<State> getStateIt(){

        return new Iterable(){
            @Override
            public Iterator iterator() {
                return new Iterator() {
                        Iterator<City> cityIt = topology.iterator();
                        Iterator<State> stateItForCurrCity = new ArrayList().iterator();

                        @Override
                        public boolean hasNext() {
                        while(!stateItForCurrCity.hasNext() && cityIt.hasNext()){
                            stateItForCurrCity = getStateIt(cityIt.next()).iterator();
                        }
                        return stateItForCurrCity.hasNext();
                    }

                        @Override
                        public State next() {
                        return stateItForCurrCity.next();
                    }
                };
            }
        };

    }


    public double getDiff(){
        return maxDiff;
    }


    public double update(double discount){
        double maxDiff = 0;
        for(State s : getStateIt()){
            maxDiff = Math.max(maxDiff,s.updateExpectedReward(this,discount));
        }
        this.maxDiff = maxDiff;
        return this.maxDiff;
    }


    public double expectedRewardIn(City c){
        double expectedReward = 0.0;
        for(State s : this.getStateIt(c)){
            expectedReward += s.probability()*s.getExpectedReward();
        }
        return expectedReward;
    }

}









