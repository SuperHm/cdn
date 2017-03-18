package com.cacheserverdeploy.deploy;

import java.security.cert.TrustAnchor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;

import com.cacheserverdeploy.deploy.Graph.Edge;

public class Deploy{
	private static int lineNum = 0;
	final static int MAX_WEIGHT = 100000;
	/**
     * 你需要完成的入口
     * <功能详细描述>
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */
    public static String[] deployServer(String[] graphContent){
        Graph graph = null;
        graph = readProblemLine(graphContent);
    	readEdges(graphContent, graph);
    	readClients(graphContent, graph);
    	int[] selectedServerNodes = selectServerNodes(3, graph);
    	
        return new String[]{"17","\r\n","0 8 0 20"};
    }
    
    public static int[] selectServerNodes(int serverNum, Graph graph){
    	int[] selectedServerNodes = new int[serverNum];
    	List<Integer> serverNodes = new ArrayList<>(graph.getServerNodes());
    	for(int i=0; i<serverNum; i++){
    		selectedServerNodes[i] = serverNodes.remove((int)(Math.random()*(serverNodes.size()-1)));
    	}
    	return selectedServerNodes;
    }
    
    /**
     * 读取网络节点数量、消费节点数量、服务器部署成本、网络链路数量。基于此信息初始化 Graph
     * 
     * @param graphContent
     * @return 返回初始化之后的Graph对象
     */
    public static Graph readProblemLine(String[] graphContent){
    	String problemLine = graphContent[lineNum++];
    	String[] problemLineStrs = problemLine.split(" ");
    	lineNum++; //跳过空行
    	String costLine = graphContent[lineNum++];
    	return new Graph(Integer.parseInt(problemLineStrs[0]), // 网络节点数量
    			Integer.parseInt(problemLineStrs[2]), //消费节点数量
    			Integer.parseInt(costLine), //服务器部署成本
    			Integer.parseInt(problemLineStrs[1])); //网络链路数量
    }
    
    /**
     * 逐行读取链路信息
     * 
     * @param graphContent
     * @param graph
     */
    public static void readEdges(String[] graphContent, Graph graph){
    	String line = null;
    	lineNum++;//跳过空行
    	while(! (line = graphContent[lineNum++]).equals("")){
    		String[] lineStrs = line.split(" ");
    		Graph.Edge edge = new Graph.Edge(Integer.parseInt(lineStrs[0]),//起始节点
    				Integer.parseInt(lineStrs[1]),//终止节点
    				Integer.parseInt(lineStrs[2]), //链路总带宽
    				Integer.parseInt(lineStrs[3]));//单位带宽租用费用
    		graph.addEdge(edge.srcNode, edge.desNode, edge);
    		/*双向边*/ 
    		edge = new Graph.Edge(Integer.parseInt(lineStrs[1]),//起始节点
    				Integer.parseInt(lineStrs[0]),//终止节点
    				Integer.parseInt(lineStrs[2]), //链路总带宽
    				Integer.parseInt(lineStrs[3]));//单位带宽租用费用
    		graph.addEdge(edge.srcNode, edge.desNode, edge);
    		
    	}
    }
    
    /**
     * 逐行读取消费节点信息
     * 
     * @param graphContent
     * @param graph
     */
    public static void readClients(String[] graphContent, Graph graph){
    	String line = null;
    	while(lineNum < graphContent.length){
    		line = graphContent[lineNum++];
    		String[] lineStrs = line.split(" ");
    		Graph.Client client = new Graph.Client(Integer.parseInt(lineStrs[0]),//消费节点
    				Integer.parseInt(lineStrs[1]), //相邻的网络节点
    				Integer.parseInt(lineStrs[2]));//带宽需求
    		graph.addClient(client.node, client);

    	}
    }
    
    /**
     * 寻找图 graph 中点 src 到 点 des 最小单位租用费用的路径
     * @param graph
     * @param src
     * @param des
     */
    public static void dijkstra(Graph graph, int src, String[] shortPath, int[] dis){
    	int nodesNum = graph.serverNodesNum;
    	int[][] edgesWeight = new int[nodesNum][nodesNum];
    	Edge edge = null;
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
    			edge = graph.getEdges()[i][j];
    			if(edge != null){
    				edgesWeight[i][j]  = edge.weight;
    			}else{
    				edgesWeight[i][j] = MAX_WEIGHT;
    			}
    		}
    	}
    	boolean isVisited[] = new boolean[nodesNum];//标记节点最短距离是否求出
    	dis = new int[nodesNum];//存储节点 src 到其他节点的最短距离
    	shortPath = new String[nodesNum];
    	//初始化节点 src 到其他节点的距离为无穷大
    	for(int i=0; i<dis.length; i++){
    		dis[i] = Integer.MAX_VALUE;
    		shortPath[i] = src+"->"+i;
    	}
    	
    	isVisited[src] = true;
    	dis[src] = 0;
    	
    	for(int count=1; count<nodesNum; count++){
    		int minDis = Integer.MAX_VALUE;
    		int nextNode = -1;
    		for(int i=0; i<nodesNum; i++){
				if(! isVisited[i] && edgesWeight[src][i] < minDis){
					minDis = edgesWeight[src][i];
					nextNode = i;
				}
    		}
    		dis[nextNode] = minDis;
    		isVisited[nextNode] = true;
    		for(int i=0; i<nodesNum; i++){
				if(!isVisited[i] && edgesWeight[src][nextNode]+edgesWeight[nextNode][i]< edgesWeight[src][i]){
					edgesWeight[src][i] = edgesWeight[src][nextNode] + edgesWeight[nextNode][i];
					shortPath[i] = shortPath[nextNode] +"->"+i;
				}
    		}
    	}
    }
}
