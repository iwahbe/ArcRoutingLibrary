package oarlib.route.impl;

import gnu.trove.TIntArrayList;
import oarlib.graph.impl.ZigZagGraph;
import oarlib.link.impl.ZigZagLink;
import oarlib.vertex.impl.ZigZagVertex;
import org.apache.log4j.Logger;

import java.util.ArrayList;

/**
 * Created by oliverlum on 9/23/15.
 */
public class ZigZagTour extends Tour<ZigZagVertex, ZigZagLink> {

    private static final Logger LOGGER = Logger.getLogger(ZigZagTour.class);

    private ArrayList<Boolean> compactZZList;
    private ZigZagGraph mGraph;
    private double mPenalty;
    private double serviceComponent; // DIFFERENT FROM mServCost; this actually contains the service times
    private TIntArrayList incrementalCost;

    public ZigZagTour(ZigZagGraph g, double latePenalty) {
        super();
        mGraph = g;
        mPenalty = latePenalty;
        compactZZList = new ArrayList<Boolean>();
        serviceComponent = 0;
        incrementalCost = new TIntArrayList();
    }

    public ZigZagTour(Tour<ZigZagVertex, ZigZagLink> t, ZigZagGraph g, double latePenalty) {
        super();
        mGraph = g;
        mPenalty = latePenalty;
        mCustomIDMap = t.getMapping();
        mRoute = t.getRoute();
        traversalDirection = t.getTraversalDirection();
        compactRepresentation = t.getCompactRepresentation();
        compactTD = t.getCompactTraversalDirection();
        servicing = t.getServicingList();
        directionDetermined = t.isDirectionDetermined();
        compactZZList = new ArrayList<Boolean>();
        serviceComponent = 0;
        incrementalCost = new TIntArrayList();
    }

    public ArrayList<Boolean> getCompactZZList() {
        return compactZZList;
    }

    public void setGraph(ZigZagGraph g) {
        mGraph = g;
    }

    public void setPenalty(double newPenalty) {
        mPenalty = newPenalty;
    }

    public void changeZigZagStatus(int compactIndex) {
        //manage the cost differential, and switch it if possible
        if (compactIndex > compactZZList.size() || compactIndex < 0) {
            LOGGER.error("You have provided an invalid index; please note that this should be the index in the compact zig zag list.  Exiting...");
            return;
        }

        Boolean toChange = compactZZList.get(compactIndex);
        ZigZagLink temp = mGraph.getEdge(compactRepresentation.get(compactIndex));
        Boolean forward = compactTD.get(compactIndex);

        if (temp.getStatus() == ZigZagLink.ZigZagStatus.NOT_AVAILABLE) {
            LOGGER.warn("You are attempting to zig zag a link that cannot be zig-zagged.  Exiting...");
            return;
        }

        if (toChange) {
            if (forward) {
                serviceComponent += temp.getServiceCost();
                serviceComponent -= temp.getZigzagCost();
            } else {
                serviceComponent += temp.getReverseCost();
                serviceComponent -= temp.getZigzagCost();
            }
            compactZZList.set(compactIndex, false);
        } else {
            //we have to check feasability for the rest
            double costDiff;
            if (forward)
                costDiff = temp.getZigzagCost() - temp.getServiceCost();
            else
                costDiff = temp.getZigzagCost() - temp.getReverseServiceCost();

            int tempCost;
            for (int i = compactIndex + 1; i < compactZZList.size(); i++) {
                //check the rest to make sure the time window slack is sufficient
                tempCost = incrementalCost.get(i);
                temp = mGraph.getEdge(compactRepresentation.get(i));
                if (tempCost + costDiff > temp.getTimeWindow().getSecond()) {
                    LOGGER.warn("You are attempting to change service from normal to zig-zag," +
                            "but it would cause later service of edge " + temp.getId() + " to be " +
                            "infeasible.  Exiting without changing.");
                    return;
                }
                if (forward) {
                    serviceComponent -= temp.getServiceCost();
                    serviceComponent += temp.getZigzagCost();
                    compactZZList.set(compactIndex, true);
                } else {
                    serviceComponent -= temp.getReverseServiceCost();
                    serviceComponent += temp.getZigzagCost();
                    compactZZList.set(compactIndex, true);
                }
            }
        }

    }

    /**
     * Add a edge to the end of this route.
     *
     * @param l       - the link to be added
     * @param service - true if l is going to be serviced by this route.
     */
    @Override
    public void appendEdge(ZigZagLink l, boolean service) throws IllegalArgumentException {
        appendEdge(l, service, false);
    }

    /**
     * Add a edge to the end of this route.
     *
     * @param l       - the link to be added
     * @param service - true if l is going to be serviced by this route.
     * @param zigzag  - true if we should zigzag l.  Ignored if zig-zag is not available,
     *                if the cost thus far prohibits zig-zagging, or if service is not required.
     */
    public void appendEdge(ZigZagLink l, boolean service, boolean zigzag) throws IllegalArgumentException {
        if (zigzag) {
            if (l.getStatus() == ZigZagLink.ZigZagStatus.NOT_AVAILABLE) {
                LOGGER.warn("You are attempting to zig zag a link that doesn't allow zig zag service. Exiting without appending.");
                return;
            }
            if (getCost() + l.getZigzagCost() > l.getTimeWindow().getSecond()) {
                LOGGER.warn("You are attempting to zig zag a link after its time window has closed.  Exiting without appending.");
                return;
            }
            if (!l.isRequired()) {
                LOGGER.warn("You are attempting to zig zag a link that does not require service. Exiting without appending.");
                return;
            }
        }

        boolean prevDetermined = directionDetermined;

        super.appendEdge(l, service);

        //augment the service costs
        if (directionDetermined) {

            ArrayList<Boolean> td = getCompactTraversalDirection();
            if (!prevDetermined) {

                //catch up service component
                for (int i = 0; i < td.size() - 1; i++) {
                    if (compactZZList.get(i)) {
                        serviceComponent += mRoute.get(i).getZigzagCost();
                    } else if (td.get(i))
                        serviceComponent += mRoute.get(i).getServiceCost();
                    else
                        serviceComponent += mRoute.get(i).getReverseServiceCost();
                }

                int tempId;
                //update incremental costs
                if (traversalDirection.get(0)) {
                    incrementalCost.set(0, mRoute.get(0).getCost() + mRoute.get(0).getServiceCost());
                } else {
                    incrementalCost.set(0, mRoute.get(0).getReverseCost() + mRoute.get(0).getReverseServiceCost());
                }

                for (int i = 1; i < mRoute.size() - 1; i++) {
                    if (traversalDirection.get(i))
                        incrementalCost.set(i, incrementalCost.get(i - 1) + mRoute.get(i).getCost() + mRoute.get(i).getServiceCost());
                    else
                        incrementalCost.set(i, incrementalCost.get(i - 1) + mRoute.get(i).getReverseCost() + mRoute.get(i).getReverseServiceCost());
                }
            }

            if (zigzag) {
                serviceComponent += l.getZigzagCost();
            } else if (service) {
                if (td.get(td.size() - 1))
                    serviceComponent += l.getServiceCost();
                else
                    serviceComponent += l.getReverseServiceCost();
            }
        }

        //add to the zig zag list
        if (service) {
            if (zigzag)
                compactZZList.add(true);
            else
                compactZZList.add(false);
        }


        //keep track of the incremental costs

        //should be meaningless if we haven't figured this out
        if (!directionDetermined)
            incrementalCost.add(-1);
        else
            incrementalCost.add(getCost());
    }

    /**
     * @return - the cost of the route
     */
    @Override
    public int getCost() {
        return (int) (mCost + serviceComponent);
    }
}
