/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2016 Oliver Lum
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package oarlib.core;

import oarlib.exceptions.GraphInfeasibleException;
import oarlib.problem.impl.ProblemAttributes;
import org.apache.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;

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

    /**
     * Get the properties of the problem, (i.e. for the Windy Rural Postman, it will return a ProblemAttributes
     * object with the properties Windy, Rural, Single Depot, Single Vehicle, etc.
     *
     * @return - a Problem Attributes object describing the problem
     */
    public abstract ProblemAttributes getProblemAttributes();

    public abstract String printCurrentSol();

    public abstract String getSolverName();

    public abstract Solver<V, E, G> instantiate(Problem<V, E, G> p);

    /**
     * Get the parameters of the solver.  The map can be used to query for
     * special parameters that are specific to one solver or another, (i.e. if one solver
     * contains weights, learns a cooling schedule, etc. these should be returned in the map
     * exposed by this method)
     *
     * @return - a HashMap where the keys are the names of the solver-specific parameters, and
     * the values are what the solver used to arrive at its answer.
     */
    public abstract HashMap<String, Double> getProblemParameters();

}
