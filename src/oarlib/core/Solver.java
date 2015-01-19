package oarlib.core;

import oarlib.exceptions.GraphInfeasibleException;
import oarlib.problem.impl.ProblemAttributes;
import org.apache.log4j.Logger;

import java.util.Collection;

/**
 * Created by oliverlum on 1/18/15.
 */
public abstract class Solver<V extends Vertex, E extends Link<V>, G extends Graph<V, E>> {

    private static final Logger LOGGER = Logger.getLogger(SingleVehicleSolver.class);

    protected Problem<V, E, G> mInstance;

    /**
     * Default constructor; must set problem instance.
     *
     * @param instance - instance for which this is a solver
     */
    protected Solver(Problem<V, E, G> instance) throws IllegalArgumentException {
        //make sure I'm a valid problem instance
        if (!(instance.getProblemAttributes().isCompatibleWith(getProblemAttributes()))) {
            throw new IllegalArgumentException("It appears that this problem does not match the problem type handled by this solver.");
        }
        mInstance = instance;
    }

    /**
     * Attempts to solve the instance assigned to this problem.
     *
     * @return null if problem instance is not assigned, or solver failed.
     */
    public Collection<? extends Route> trySolve() throws GraphInfeasibleException {
        if (!checkGraphRequirements())
            throw new GraphInfeasibleException();
        return solve();
    }

    /**
     * Determines if the graph meets theoretical requirements for this solver to be run on it.
     *
     * @return - true if the graph meets pre-requisites (usually connectivity-related), false oth.
     */
    protected abstract boolean checkGraphRequirements();

    /**
     * @return - the problem instance
     */
    protected abstract Problem<V, E, G> getInstance();

    /**
     * Actually solves the instance, (first checking for feasibility), returning a Collection of routes.
     *
     * @return The set of routes the solver has concluded is best.
     */
    protected abstract Collection<? extends Route> solve();

    public abstract ProblemAttributes getProblemAttributes();

    public abstract String printCurrentSol();

    public abstract String getSolverName();

    public abstract Solver<V, E, G> instantiate(Problem<V, E, G> p);

}