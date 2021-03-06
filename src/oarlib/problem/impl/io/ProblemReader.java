/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013-2015 Oliver Lum
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
 */
package oarlib.problem.impl.io;

import com.sun.jdi.event.WatchpointEvent;
import gnu.trove.TIntObjectHashMap;
import oarlib.core.Graph;
import oarlib.exceptions.FormatMismatchException;
import oarlib.exceptions.UnsupportedFormatException;
import oarlib.graph.impl.*;
import oarlib.graph.util.Pair;
import oarlib.link.impl.Arc;
import oarlib.link.impl.Edge;
import oarlib.link.impl.WindyEdge;
import oarlib.link.impl.ZigZagLink;
import oarlib.vertex.impl.DirectedVertex;
import oarlib.vertex.impl.MixedVertex;
import oarlib.vertex.impl.UndirectedVertex;
import oarlib.vertex.impl.WindyVertex;
import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

/**
 * Reader to accept various file formats, and store them as a graph object.
 *
 * @author Oliver
 */
public class ProblemReader {

    private static final Logger LOGGER = Logger.getLogger(ProblemReader.class);

    private ProblemFormat.Name mFormat;

    public ProblemReader(ProblemFormat.Name format) {
        mFormat = format;
    }

    public ProblemFormat.Name getFormat() {
        return mFormat;
    }

    public void setFormat(ProblemFormat.Name newFormat) {
        mFormat = newFormat;
    }

    public Graph<?, ?> readGraph(String fileName) throws UnsupportedFormatException, FormatMismatchException {
        switch (mFormat) {
            case Simple:
                return readSimpleGraph(fileName);
            case Corberan:
                return readCorberanGraph(fileName);
            case Yaoyuenyong:
                return readYaoyuenyongGraph(fileName);
            case Campos:
                return readCamposGraph(fileName);
            case METIS:
                return readMETISGraph(fileName);
            case OARLib:
                return readOARLibGraph(fileName);
            case Zhang_Matrix_WRPP:
                return readRuiWRPPGraph(fileName);
            case MeanderingPostman:
                return readMeanderingPostmanGraph(fileName);
            default:
                break;
        }
        LOGGER.error("While the format seems to have been added to the Format.Name type list,"
                + " there doesn't seem to be an appropriate read method assigned to it.  Support is planned in the future," +
                "but not currently available");
        throw new UnsupportedFormatException();
    }

    private Graph<?, ?> readRuiWRPPGraph(String fileName) throws FormatMismatchException {
        try {

            List<String> allLines = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
            int count = allLines.size();
            int n = count / 3;

            String line, line2;
            String[] temp;
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));

            WindyGraph ans = new WindyGraph(n);

            //keys are endpoint ids, with the first value always < second
            HashMap<Pair<Integer>, WindyEdge> links = new HashMap<Pair<Integer>, WindyEdge>();

            //parse travel times
            Pair<Integer> key;
            boolean reverse;
            int value;
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= n; j++) {

                    value = Integer.parseInt(temp[j - 1]);
                    if (value == 0)
                        continue;

                    if (i <= j) {
                        key = new Pair<Integer>(i, j);
                        reverse = false;
                    } else {
                        key = new Pair<Integer>(j, i);
                        reverse = true;
                    }

                    if (!links.containsKey(key)) {
                        //default
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, false));
                    }

                    WindyEdge tempLink = links.get(key);
                    if (reverse)
                        tempLink.setReverseCost(value);
                    else
                        tempLink.setCost(value);
                }

            }

            //parse service times
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= n; j++) {

                    value = Integer.parseInt(temp[j - 1]);

                    if (value == 0)
                        continue;

                    if (i <= j) {
                        key = new Pair<Integer>(i, j);
                        reverse = false;
                    } else {
                        key = new Pair<Integer>(j, i);
                        reverse = true;
                    }

                    if (!links.containsKey(key)) {
                        //default
                        LOGGER.warn("THIS SHOULD NOT HAPPEN: All links should have been created already.");
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, false));
                    }

                    WindyEdge tempLink = links.get(key);
                    if (reverse)
                        tempLink.setReverseServiceCost(1);
                    else
                        tempLink.setServiceCost(1);
                }

            }
            //parse type
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= i; j++) {

                    value = Integer.parseInt(temp[j - 1]);
                    if (value == 0)
                        continue;

                    key = new Pair<Integer>(j, i);

                    if (!links.containsKey(key)) {
                        //default
                        LOGGER.warn("THIS SHOULD NOT HAPPEN: All links should have been created already.");
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, false));
                    }

                    WindyEdge tempLink = links.get(key);

                    if (value == 0)
                        tempLink.setRequired(false);
                    else
                        tempLink.setRequired(true);
                }

            }

            for (WindyEdge zzl : links.values()) {
                ans.addEdge(zzl);
            }

            return ans;
        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }
    }

    private Graph<?, ?> readMeanderingPostmanGraph(String fileName) throws FormatMismatchException {

        try {

            List<String> allLines = Files.readAllLines(Paths.get(fileName), Charset.defaultCharset());
            int count = allLines.size();
            int n = count / 5;

            String line, line2;
            String[] temp;
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));

            ZigZagGraph ans = new ZigZagGraph(n);

            //keys are endpoint ids, with the first value always < second
            HashMap<Pair<Integer>, ZigZagLink> links = new HashMap<Pair<Integer>, ZigZagLink>();

            //parse travel times
            Pair<Integer> key;
            boolean reverse;
            int value;
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= n; j++) {

                    value = Integer.parseInt(temp[j - 1]);
                    if (value == 0)
                        continue;

                    if (i <= j) {
                        key = new Pair<Integer>(i, j);
                        reverse = false;
                    } else {
                        key = new Pair<Integer>(j, i);
                        reverse = true;
                    }

                    if (!links.containsKey(key)) {
                        //default
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0, ZigZagLink.ZigZagStatus.OPTIONAL));
                    }

                    ZigZagLink tempLink = links.get(key);
                    if (reverse)
                        tempLink.setmReverseCost(value);
                    else
                        tempLink.setCost(value);
                }

            }

            //parse service times
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= n; j++) {

                    value = Integer.parseInt(temp[j - 1]);

                    if (value == 0)
                        continue;

                    if (i <= j) {
                        key = new Pair<Integer>(i, j);
                        reverse = false;
                    } else {
                        key = new Pair<Integer>(j, i);
                        reverse = true;
                    }

                    if (!links.containsKey(key)) {
                        //default
                        LOGGER.warn("THIS SHOULD NOT HAPPEN: All links should have been created already.");
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0, ZigZagLink.ZigZagStatus.OPTIONAL));
                    }

                    ZigZagLink tempLink = links.get(key);
                    if (reverse)
                        tempLink.setReverseServiceCost(value);
                    else
                        tempLink.setServiceCost(value);
                }

            }

            //parse zig zag times
            double zigValue;
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= i; j++) {

                    zigValue = Double.parseDouble(temp[j - 1]);
                    if (zigValue == 0)
                        continue;

                    key = new Pair<Integer>(j, i);

                    if (!links.containsKey(key)) {
                        //default
                        LOGGER.warn("THIS SHOULD NOT HAPPEN: All links should have been created already.");
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0, ZigZagLink.ZigZagStatus.OPTIONAL));
                    }

                    ZigZagLink tempLink = links.get(key);

                    tempLink.setZigzagCost(zigValue);
                }

            }

            //parse time windows
            int endTime;
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= i; j++) {

                    endTime = Double.valueOf(temp[j - 1]).intValue();
                    if (endTime == 0)
                        continue;

                    key = new Pair<Integer>(j, i);

                    if (!links.containsKey(key)) {
                        //default
                        LOGGER.warn("THIS SHOULD NOT HAPPEN: All links should have been created already.");
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0, ZigZagLink.ZigZagStatus.OPTIONAL));
                    }

                    ZigZagLink tempLink = links.get(key);

                    tempLink.setTimeWindow(new Pair<Integer>(0, endTime));
                }
            }

            //parse type
            for (int i = 1; i <= n; i++) {
                line = br.readLine();
                temp = line.split(",\\s+|:|\t");
                for (int j = 1; j <= i; j++) {

                    value = Integer.parseInt(temp[j - 1]);
                    if (value == 0)
                        continue;

                    key = new Pair<Integer>(j, i);

                    if (!links.containsKey(key)) {
                        //default
                        LOGGER.warn("THIS SHOULD NOT HAPPEN: All links should have been created already.");
                        links.put(key, ans.constructEdge(key.getFirst(), key.getSecond(), "", Integer.MAX_VALUE, Integer.MAX_VALUE, 0, 0, 0, ZigZagLink.ZigZagStatus.OPTIONAL));
                    }

                    ZigZagLink tempLink = links.get(key);

                    if (value == 1)
                        tempLink.setStatus(ZigZagLink.ZigZagStatus.MANDATORY);
                    else if (value == 2)
                        tempLink.setStatus(ZigZagLink.ZigZagStatus.NOT_AVAILABLE);
                    else if (value == 3)
                        tempLink.setStatus(ZigZagLink.ZigZagStatus.OPTIONAL);
                }

            }

            for (ZigZagLink zzl : links.values()) {
                ans.addEdge(zzl);
            }

            return ans;
        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }
    }

    private Graph<?, ?> readOARLibGraph(String fileName) throws FormatMismatchException {

        try {
            String line, line2;
            String[] temp;
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));

            //figure out the graph type

            line = br.readLine();
            //skip any header
            while (!line.startsWith("Graph Type:"))
                line = br.readLine();

            //TODO: use this info to return a problem
            //br.readLine(); //should have problem type
            //br.readLine(); //should have fleet size
            //br.readLine(); //should have depot type
            line2 = br.readLine(); //should have depot ID

            int depotId = Integer.parseInt(line2.split(":")[1].trim());
            line2 = br.readLine(); //should have n

            int n = Integer.parseInt(line2.split(":")[1].trim());

            line2 = br.readLine(); //should have m

            int m = Integer.parseInt(line2.split(":")[1].trim());

            //graph type
            if (line.contains("UNDIRECTED")) {

                //advance to the correct spot
                while (!line.startsWith("Line Format"))
                    line = br.readLine();

                br.readLine();

                UndirectedGraph ans = new UndirectedGraph(n);
                for (int i = 1; i <= m; i++) {
                    line = br.readLine();
                    temp = line.split(",");
                    ans.addEdge(Integer.parseInt(temp[0].trim()), Integer.parseInt(temp[1].trim()), Integer.parseInt(temp[2].trim()), Boolean.parseBoolean(temp[3].trim()));
                }
                ans.setDepotId(depotId);
                return ans;
            } else if (line.contains("DIRECTED")) {

                //advance to the correct spot
                while (!line.startsWith("Line Format"))
                    line = br.readLine();

                br.readLine();

                DirectedGraph ans = new DirectedGraph(n);
                for (int i = 1; i <= m; i++) {
                    line = br.readLine();
                    temp = line.split(",");
                    ans.addEdge(Integer.parseInt(temp[0].trim()), Integer.parseInt(temp[1].trim()), Integer.parseInt(temp[2].trim()), Boolean.parseBoolean(temp[3].trim()));
                }
                ans.setDepotId(depotId);
                return ans;
            } else if (line.contains("MIXED")) {

                //advance to the correct spot
                while (!line.startsWith("Line Format"))
                    line = br.readLine();

                br.readLine();

                MixedGraph ans = new MixedGraph(n);
                for (int i = 1; i <= m; i++) {
                    line = br.readLine();
                    temp = line.split(",");
                    ans.addEdge(Integer.parseInt(temp[0].trim()), Integer.parseInt(temp[1].trim()), Integer.parseInt(temp[2].trim()), Boolean.parseBoolean(temp[3].trim()), Boolean.parseBoolean(temp[4].trim()));
                }
                ans.setDepotId(depotId);
                return ans;
            } else if (line.contains("WINDY")) {

                //advance to the correct spot
                while (!line.startsWith("Line Format"))
                    line = br.readLine();

                br.readLine();

                WindyGraph ans = new WindyGraph(n);
                for (int i = 1; i <= m; i++) {
                    line = br.readLine();
                    temp = line.split(",");
                    ans.addEdge(Integer.parseInt(temp[0].trim()), Integer.parseInt(temp[1].trim()), Integer.parseInt(temp[2].trim()), Integer.parseInt(temp[3].trim()), Boolean.parseBoolean(temp[4].trim()));
                }
                ans.setDepotId(depotId);

                //advance to the correct spot
                while (!line.startsWith("Line Format"))
                    line = br.readLine();

                br.readLine();

                for (int i = 1; i <= n; i++) {
                    line = br.readLine();
                    temp = line.split(",");
                    ans.getVertex(i).setCoordinates(Double.parseDouble(temp[0].trim()), Double.parseDouble(temp[1].trim()));
                }

                return ans;

            }


            return null;
        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }

    }

    private Graph<?, ?> readMETISGraph(String fileName) throws FormatMismatchException {
        try {
            //ans, so far I only know of undirected graphs for this type
            UndirectedGraph ans = new UndirectedGraph();

            //file reading vars
            String line;
            String[] temp;
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));

            //header info
            int n = 0;
            int numWeightsPerVertex = 1;

            boolean hasEdgeWeights = false;
            boolean hasVertexWeights = false;
            boolean hasVertexSizes = false;

            line = br.readLine();

            //skip any header
            while (line.startsWith("%"))
                line = br.readLine();
            temp = line.split(",\\s+|:");

            if (temp.length == 2) {
                //n, and m
                n = Integer.parseInt(temp[0]);
            }
            if (temp.length > 2) {
                //fmt
                if (temp[2].charAt(2) == '0') {
                    hasEdgeWeights = false;
                    LOGGER.warn("This file does not contain information about edge weights.  The graph will attempt to be read, but this seems strange, as this is an Arc-Routing Library");
                }
                if (temp[2].charAt(1) == '0')
                    hasVertexWeights = false;
                if (temp[2].charAt(0) == '0')
                    hasVertexSizes = false;
            }
            if (temp.length > 3) {
                //nconn
                numWeightsPerVertex = Integer.parseInt(temp[3]);
                if (numWeightsPerVertex > 1)
                    LOGGER.warn("The graph specified in this file has multiple weights per vertex.  Currently, there is no support for multiple vertex weights, so only the first will be used.");
            }
            if (temp.length > 4) {
                br.close();
                throw new FormatMismatchException("This does not appear to be a METIS graph file.  The header is malformed.");
            }

            //vertices
            for (int i = 0; i < n; i++) {
                ans.addVertex(new UndirectedVertex(""));
            }

            TIntObjectHashMap<UndirectedVertex> indexedVertices = ans.getInternalVertexMap();

            //now read the rest
            for (int i = 1; i <= n; i++) {
                if ((line = br.readLine()) == null) {
                    br.close();
                    LOGGER.error("This does not appear to be a valid METIS graph file.  There are not as many lines as vertices specified in the header.");
                    throw new FormatMismatchException();
                }

                temp = line.split(",\\s+|:");
                //if there's a vertex size, read it
                if (hasVertexSizes) {
                    indexedVertices.get(i).setSize(Integer.parseInt(temp[0]));
                }
                if (hasVertexWeights) {
                    if (hasVertexSizes)
                        indexedVertices.get(i).setCost(Integer.parseInt(temp[1]));
                    else
                        indexedVertices.get(i).setCost(Integer.parseInt(temp[0]));
                }
                if (hasEdgeWeights) {
                    int start = 0;
                    if (hasVertexSizes)
                        start++;
                    if (hasVertexWeights)
                        start += numWeightsPerVertex;
                    int end = temp.length;
                    for (int j = start; j < end; j += 2) {
                        //to avoid redundancy in the file
                        if (i < Integer.parseInt(temp[j]))
                            ans.addEdge(i, Integer.parseInt(temp[j]), Integer.parseInt(temp[j + 1]));
                    }
                }
            }

            br.close();
            return ans;

        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }
    }

    private Graph<?, ?> readCamposGraph(String fileName) throws FormatMismatchException {
        try {
            //ans, so far I only know of directed graphs for this type
            DirectedGraph ans = new DirectedGraph();

            //file reading vars
            String line;
            String[] temp;
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));

            //header info
            int n = 0;
            int m = 0;
            line = br.readLine();
            temp = line.split(",\\s+|:");
            boolean isReq = false;

            n = Integer.parseInt(temp[0].trim()); //first line is number of vertices

            line = br.readLine(); //second line is number of vertices in the simplified graph

            line = br.readLine(); // third line is the number of arcs

            temp = line.split(",");
            m = Integer.parseInt(temp[0].trim());

            line = br.readLine(); //fourth line is # of connected components of the simplified graph

            line = br.readLine(); //fifth line is number of vertices belonging to each of those connected components

            //construct the ans graph
            for (int i = 0; i < n; i++) {
                ans.addVertex(new DirectedVertex("orig"));
            }
            for (int i = 0; i < m; i++) {
                line = br.readLine();
                if (line == null) {
                    br.close();
                    LOGGER.error("Not enough lines to match the claimed number of arcs");
                    throw new FormatMismatchException();
                }

                temp = line.split(",");
                if (temp.length < 4) {
                    br.close();
                    LOGGER.error("This line doesn't have the required components.");
                    throw new FormatMismatchException();
                }

                isReq = Integer.parseInt(temp[3].trim()) == 1;
                ans.addEdge(Integer.parseInt(temp[0].trim()), Integer.parseInt(temp[1].trim()), "orig", Integer.parseInt(temp[2].trim()), isReq);
            }
            br.close();
            return ans;
        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }
    }

    private Graph<?, ?> readYaoyuenyongGraph(String fileName) throws FormatMismatchException {
        try {    //file reading vars
            String line;
            String type = "";
            String[] temp;
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));

            //header info
            int n = 0;
            int m = 0;
            line = br.readLine();
            temp = line.split(",");

            //graph type
            type = temp[0];
            n = Integer.parseInt(temp[2]);
            m = Integer.parseInt(temp[3]);

            //split on graph type

            //undirected
            if (type.equals("1")) {
                UndirectedGraph ans = new UndirectedGraph();
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new UndirectedVertex("original"));
                }
                for (int i = 0; i < m; i++) {
                    line = br.readLine();
                    temp = line.split(",");

                    ans.addEdge(Integer.parseInt(temp[0]), Integer.parseInt(temp[1]), "original", Integer.parseInt(temp[2]));
                }

                br.close();
                return ans;
            }
            //directed
            else if (type.equals("2")) {
                DirectedGraph ans = new DirectedGraph();
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new DirectedVertex("original"));
                }
                for (int i = 0; i < m; i++) {
                    line = br.readLine();
                    temp = line.split(",");

                    ans.addEdge(Integer.parseInt(temp[0]), Integer.parseInt(temp[1]), "original", Integer.parseInt(temp[2]));
                }

                br.close();
                return ans;
            }
            //mixed
            else if (type.equals("3")) {
                MixedGraph ans = new MixedGraph();
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new MixedVertex("original"));
                }
                for (int i = 0; i < m; i++) {
                    line = br.readLine();
                    temp = line.split(",");

                    boolean directed = (temp[4].equals("1"));
                    ans.addEdge(Integer.parseInt(temp[0]), Integer.parseInt(temp[1]), "original", Integer.parseInt(temp[2]), directed);
                }

                br.close();
                return ans;
            } else {
                br.close();
                LOGGER.error("Unrecognized Type.");
                throw new FormatMismatchException();
            }

        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }
    }

    private Graph<?, ?> readCorberanGraph(String fileName) throws FormatMismatchException {
        try {
            String line;
            String type = "";
            String[] temp;
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));
            //header info
            int n = 0;
            int m = 0;
            int depotId = 1;
            while ((line = br.readLine()) != null) {
                if (line.contains("NOMBRE")) {
                    temp = line.split("\\s+|:");
                    if (temp[3].startsWith("MA") || temp[3].startsWith("MB"))
                        type = "Mixed";
                    else if (temp[3].startsWith("WA") || temp[3].startsWith("WB"))
                        type = "Windy";
                    else if (temp[3].startsWith("A") || temp[3].startsWith("M") || temp[3].startsWith("m") || temp[3].startsWith("P") || temp[3].startsWith("p") || temp[3].startsWith("Minmax")|| temp[3].startsWith("C"))
                        type = "WindyRural";
                    else {
                        br.close();
                        LOGGER.error("We could not figure out what type of graph this is.");
                        throw new FormatMismatchException();
                    }
                } else if (line.contains("COMENTARIO")) {
                    if(line.contains("depot")) {
                        temp = line.split("\\s+|:");
                        depotId = Integer.parseInt(temp[temp.length - 2]);
                    } else {
                        depotId = 1;
                    }
                } else if (line.contains("VERTICES")) {
                    temp = line.split("\\s+|:");
                    n = Integer.parseInt(temp[temp.length - 1]);
                } else if (line.contains("ARISTAS")) {
                    temp = line.split("\\s+|:");
                    m = Integer.parseInt(temp[temp.length - 1]);
                    break;
                } else if (line.contains("RISTAS_REQ")) {
                    temp = line.split("\\s+|:");
                    m += Integer.parseInt(temp[temp.length - 1]);
                } else if (line.contains("RISTAS_NOREQ")) {
                    temp = line.split("\\s+|:");
                    m += Integer.parseInt(temp[temp.length - 1]);
                    break;
                }
            }

            if (n == 0 || m == 0) {
                br.close();
                LOGGER.error("We could not detect any vertices (edges) in the file.");
                throw new FormatMismatchException();
            }
            //now split off into types
            if (type.equals("Mixed")) {
                MixedGraph ans = new MixedGraph();
                int tailId;
                int headId;
                int cost1;
                int cost2;
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new MixedVertex("original"));
                }

                br.readLine();
                br.readLine();
                int index;
                while ((line = br.readLine()) != null) {
                    if (line.contains("ARISTAS"))
                        break;
                    temp = line.split("\\s+|:|\\)|,|\\(");
                    index = 1;
                    if (temp[index].isEmpty())
                        index++;
                    tailId = Integer.parseInt(temp[index++]);
                    if (temp[index].isEmpty())
                        index++;
                    if (temp[index].isEmpty())
                        index++;
                    headId = Integer.parseInt(temp[index++]);
                    index += 2;
                    cost1 = Integer.parseInt(temp[index++]);
                    cost2 = Integer.parseInt(temp[index]);
                    if (cost1 == 99999999) //backwards arc
                    {
                        ans.addEdge(headId, tailId, cost2, true);
                    } else if (cost2 == 99999999) //forwards arc
                    {
                        ans.addEdge(tailId, headId, cost1, true);
                    } else // edge
                    {
                        ans.addEdge(tailId, headId, cost1, false);
                    }
                }

                //skip some interim matter
                br.readLine();
                br.readLine();
                br.readLine();
                br.readLine();

                //now read coordinates
                int i = 1;
                MixedVertex tempV;
                TIntObjectHashMap<MixedVertex> ansVertices = ans.getInternalVertexMap();
                while ((line = br.readLine()) != null) {
                    if (line.contains("="))
                        break;
                    tempV = ansVertices.get(i);
                    temp = line.split("\\s+|:|\\)|,|\\(");
                    tempV.setCoordinates(Integer.parseInt(temp[2]), Integer.parseInt(temp[3]));
                    i++;
                }
                br.close();
                return ans;
            } else if (type.equals("Windy")) {
                WindyGraph ans = new WindyGraph();
                int tailId;
                int headId;
                int cost1;
                int cost2;
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new WindyVertex("original"));
                }
                br.readLine();
                br.readLine();
                int index;
                while ((line = br.readLine()) != null) {
                    if (line.contains("ARISTAS"))
                        break;
                    temp = line.split("\\s+|:|\\)|,|\\(");
                    index = 1;
                    if (temp[index].isEmpty())
                        index++;
                    tailId = Integer.parseInt(temp[index++]);
                    if (temp[index].isEmpty())
                        index++;
                    if (temp[index].isEmpty())
                        index++;
                    headId = Integer.parseInt(temp[index++]);
                    index += 2;
                    cost1 = Integer.parseInt(temp[index++]);
                    cost2 = Integer.parseInt(temp[index]);

                    ans.addEdge(tailId, headId, "original", cost1, cost2);

                }

                //skip some interim matter
                br.readLine();
                br.readLine();
                br.readLine();
                br.readLine();

                //now read coordinates
                int i = 1;
                WindyVertex tempV;
                TIntObjectHashMap<WindyVertex> ansVertices = ans.getInternalVertexMap();
                while ((line = br.readLine()) != null) {
                    if (line.contains("="))
                        break;
                    tempV = ansVertices.get(i);
                    temp = line.split("\\s+|:|\\)|,|\\(");
                    if (temp.length < 4)
                        LOGGER.debug("STOP HERE");
                    tempV.setCoordinates(Integer.parseInt(temp[2]), Integer.parseInt(temp[3]));
                    i++;
                }

                br.close();
                return ans;
            } else if (type.equals("WindyRural")) {
                WindyGraph ans = new WindyGraph();
                int tailId;
                int headId;
                int cost1;
                int cost2;
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new WindyVertex("original"));
                }
                //	br.readLine();
                //	br.readLine();
                int index;
                while ((line = br.readLine()) != null) {
                    if (line.contains("LISTA_ARISTAS_REQ")) //in-process the required guys
                    {
                        while ((line = br.readLine()) != null) {
                            if (line.contains("LISTA_ARISTAS_NOREQ")) {
                                break;
                            }

                            temp = line.split("\\s+|:|\\)|,|\\(");
                            index = 1;
                            while (temp[index].isEmpty())
                                index++;
                            tailId = Integer.parseInt(temp[index++]);
                            if (temp[index].isEmpty())
                                index++;
                            if (temp[index].isEmpty())
                                index++;
                            headId = Integer.parseInt(temp[index++]);
                            index += 2;
                            cost1 = Integer.parseInt(temp[index++]);
                            cost2 = Integer.parseInt(temp[index]);

                            ans.addEdge(tailId, headId, "original", cost1, cost2, true);
                        }
                        while ((line = br.readLine()) != null) {
                            if (line.contains("COORDENADAS")) {
                                break;
                            }
                            temp = line.split("\\s+|:|\\)|,|\\(");
                            if (temp.length == 1)
                                break;
                            index = 1;
                            while (temp[index].isEmpty())
                                index++;
                            tailId = Integer.parseInt(temp[index++]);
                            if (temp[index].isEmpty())
                                index++;
                            if (temp[index].isEmpty())
                                index++;
                            headId = Integer.parseInt(temp[index++]);
                            index += 2;
                            cost1 = Integer.parseInt(temp[index++]);
                            cost2 = Integer.parseInt(temp[index]);

                            ans.addEdge(tailId, headId, "original", cost1, cost2, false);
                        }
                        break;
                    }

                }

                //skip some interim matter
                //br.readLine();
                //br.readLine();
                //br.readLine();
                //br.readLine();

                //now read coordinates
                int i = 1;
                WindyVertex tempV;
                TIntObjectHashMap<WindyVertex> ansVertices = ans.getInternalVertexMap();
                while ((line = br.readLine()) != null) {
                    tempV = ansVertices.get(i);
                    temp = line.split("\\s+|:|\\)|,|\\(");
                    tempV.setCoordinates(Double.parseDouble(temp[1]), Double.parseDouble(temp[2]));
                    i++;
                }

                br.close();
                ans.setDepotId(depotId);
                return ans;
            } else {
                br.close();
                LOGGER.error("We don't currently support the type of graph right now.");
                throw new FormatMismatchException();
            }
        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }
    }

    private Graph<?, ?> readSimpleGraph(String fileName) throws FormatMismatchException {
        try {
            String type; //first line of DIMACS_Modified
            String header; //second line of DIMACS_Modified
            File graphFile = new File(fileName);
            BufferedReader br = new BufferedReader(new FileReader(graphFile));
            //header info
            type = br.readLine();
            if (type == null) {
                br.close();
                LOGGER.error("There were no readable lines in the file.");
                throw new FormatMismatchException();
            }
            header = br.readLine();
            if (header == null) {
                br.close();
                LOGGER.error("There was only one readable line in the file.");
                throw new FormatMismatchException();
            }
            String[] nm = header.split("\\s+");
            int n = Integer.parseInt(nm[0]);
            int m = Integer.parseInt(nm[1]);

            String line;
            String[] splitLine;

            //branch on types, (more elegant way?)
            if (type.equals("Directed")) {
                DirectedGraph ans = new DirectedGraph();
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new DirectedVertex("Original"));
                }
                TIntObjectHashMap<DirectedVertex> indexedVertices = ans.getInternalVertexMap();
                for (int i = 0; i < m - 2; i++) {
                    line = br.readLine();
                    if (line == null) {
                        br.close();
                        LOGGER.error("There were not enough lines in the file to account for the number "
                                + "of edges claimed in the header.");
                        throw new FormatMismatchException();
                    }
                    splitLine = line.split("\\s+");
                    if (splitLine.length != 3) {
                        br.close();
                        LOGGER.error("One of the edge lines had too many entries in it.");
                        throw new FormatMismatchException();
                    }
                    ans.addEdge(new Arc("Original", new Pair<DirectedVertex>(indexedVertices.get(Integer.parseInt(splitLine[0])), indexedVertices.get(Integer.parseInt(splitLine[1]))), Integer.parseInt(splitLine[2])));
                }
                if ((line = br.readLine()) != null) {
                    LOGGER.debug("Ignoring excess lines in file.  This could just be whitespace, but there are more lines than "
                            + "are claimed in the header");
                }
                br.close();
                return ans;
            } else if (type.equals("Undirected")) {
                UndirectedGraph ans = new UndirectedGraph();
                for (int i = 0; i < n; i++) {
                    ans.addVertex(new UndirectedVertex("Original"));
                }
                TIntObjectHashMap<UndirectedVertex> indexedVertices = ans.getInternalVertexMap();
                for (int i = 0; i < m - 2; i++) {
                    line = br.readLine();
                    if (line == null) {
                        br.close();
                        LOGGER.error("There were not enough lines in the file to account for the number "
                                + "of edges claimed in the header.");
                        throw new FormatMismatchException();
                    }
                    splitLine = line.split("\\s+");
                    if (splitLine.length != 3) {
                        br.close();
                        LOGGER.error("One of the edge lines had too many entries in it.");
                        throw new FormatMismatchException();
                    }
                    ans.addEdge(new Edge("Original", new Pair<UndirectedVertex>(indexedVertices.get(Integer.parseInt(splitLine[0])), indexedVertices.get(Integer.parseInt(splitLine[1]))), Integer.parseInt(splitLine[2])));

                }
                if ((line = br.readLine()) != null) {
                    LOGGER.debug("Ignoring excess lines in file.  This could just be whitespace, but there are more lines than "
                            + "are claimed in the header");
                }
                br.close();
                return ans;
            } else if (type.equals("Mixed")) {
                //TODO
            } else if (type.equals("Windy")) {
                //TODO
            }
            //Something is wrong
            else {
                br.close();
                LOGGER.error("The type specified in the first line of the DIMACS_Modified file was not recognized."
                        + "  It should read either \"Directed\" \"Undirected\" \"Mixed\" or \"Windy\"");
                throw new FormatMismatchException();
            }
            br.close();
            return null;
        }
        catch (Exception e) {
            throw new FormatMismatchException(e.getMessage(), e.getCause());
        }
    }

}
