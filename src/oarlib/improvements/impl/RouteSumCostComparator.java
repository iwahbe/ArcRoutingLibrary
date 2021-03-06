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
package oarlib.improvements.impl;

import oarlib.core.Route;

import java.util.Collection;
import java.util.Comparator;

/**
 * Simple way of comparing two sets of routes for local search.  Note that this probably isn't
 * the most computationally efficient way, since it calculates the metric on each object instead
 * of just calculating the difference.  Still, it's the easiest way of doing business, and will
 * only be a performance bottleneck if a metric is very compute intensive.
 *
 * Created by oliverlum on 1/22/16.
 */
public class RouteSumCostComparator implements Comparator<Collection<Route>> {
    @Override
    public int compare(Collection<Route> o1, Collection<Route> o2) {
        int total1 = 0;
        int total2 = 0;
        for(Route r: o1)
            total1 += r.getCost();
        for(Route r: o2)
            total2 += r.getCost();

        if(total1 > total2)
            return 1;
        else if (total1 < total2)
            return -1;
        else
            return 0;
    }
}
