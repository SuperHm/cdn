package com.cacheserverdeploy.deploy;

import java.util.Random;

/**
 * 
 * @ClassName: Graph
 *
 * @Description: 
 *
 * @author: ccding
 * @date: 2017年3月16日 下午2:52:48
 *
 */
public class Graph {
	
	final static int MAX_WEIGHT = 100000;

	private int[] serverNodes;//服务节点
	private int[] clientNodes;//消费节点
	private int[][] bandWidths;
	private int[][] unitCosts;
	private int[] demands;
	private int[] linkedNodes;
	final int serverCost;
	final int linkNum;
	final int clientNodesNum;
	final int serverNodesNum;

	
	public Graph(int serverNodesNum, int clientNodesNum, int serverCost, int linkNum){
		this.serverNodes = new int[serverNodesNum];
		this.clientNodes = new int[clientNodesNum];
		this.bandWidths = new int[serverNodesNum][serverNodesNum];
		this.unitCosts = new int[serverNodesNum][serverNodesNum];
		this.demands = new int[clientNodesNum];
		this.linkedNodes = new int[clientNodesNum];
		
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientNodesNum = clientNodesNum;
		this.serverNodesNum = serverNodesNum;
		
		for(int i=0; i<this.serverNodesNum; i++)
			serverNodes[i] = i;
		for(int i=0; i<this.clientNodesNum; i++)
			clientNodes[i] = i;
	}
	
	
	public void addEdge(int src, int des, int bandWidth, int unitCost){
		this.bandWidths[src][des] = bandWidth;
		this.bandWidths[des][src] = bandWidth;
		this.unitCosts[src][des] = unitCost;
		this.unitCosts[des][src] = unitCost;
		
	}
	
	public void addClient(int node, int linkedNode, int demand){
		this.linkedNodes[node] = linkedNode;
		this.demands[node] = demand;
	}
	
	public int[] getServerNodes() {
		return serverNodes;
	}


	public int[] getClientNodes() {
		return clientNodes;
	}


	public int[][] getUnitCosts() {
		return unitCosts;
	}


	public int[] getDemands() {
		return demands;
	}


	public int[] getLinkedNodes() {
		return linkedNodes;
	}


	public int[][] getBandWidths(){
		return this.bandWidths;
	}
	
    /**
     * 按一定策略选择初始服务器节点
     * 
     * @param serverNum 服务器节点个数
     * @return 选择的服务器节点
     */
    public int[] selectServerNodes(int initServerNum){
    	int[] selectedServerNodes = new int[initServerNum];
    	boolean[] isSelected = new boolean[serverNodesNum];
    	Random random = new Random();
    	int selectedCount = 0;
    	while(selectedCount != initServerNum){
    		int tmp = random.nextInt(serverNodesNum);
    		if(!isSelected[tmp])
    			selectedServerNodes[selectedCount++] = tmp;
    	}
    	return selectedServerNodes;
    }

    /**
     * 使用 dijkstra 算法寻找图中节点 src 到节点 des 的最小租用费用路径
     * 
     * @param src 开始节点
     * @param des 终止节点集合
     * @param shortPath 路径信息
     * @param dis 最小单位租用费用
     */
    public void dijkstra(int src, int[] des, String[] shortPath, int[] dis, int[] flow){
    	int nodesNum = this.serverNodesNum;
    	int[][] edgesWeight = new int[nodesNum][nodesNum];
    	int[][] maxFlow = new int[nodesNum][nodesNum];
    	
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
    			maxFlow[i][j] = bandWidths[i][j];
    			if(unitCosts[i][j] == 0){//没有对应的边
    				edgesWeight[i][j] = MAX_WEIGHT;
    			}else{
    				edgesWeight[i][j]  = unitCosts[i][j];
    			}
    		}
    	}
    	
    	boolean isVisited[] = new boolean[nodesNum];//标记节点最短距离是否求出
    	int[] disTmp = new int[nodesNum];
    	String[] shortPathTmp = new String[nodesNum];
    	int[] flowTmp = new int[nodesNum];
    	
    	//初始化节点 src 到其他节点的距离为无穷大
    	for(int i=0; i<disTmp.length; i++){
    		disTmp[i] = Integer.MAX_VALUE;
    		shortPathTmp[i] = src+"->"+i;
    	}
    	
    	isVisited[src] = true;
    	disTmp[src] = 0;
    	flowTmp[src] = 0;
    	
    	for(int count=1; count<nodesNum; count++){
    		int minDis = Integer.MAX_VALUE;
    		int nextNode = -1;
    		int tmp = 0;
    		for(int i=0; i<nodesNum; i++){
				if(! isVisited[i] && edgesWeight[src][i] < minDis){
					minDis = edgesWeight[src][i];
					nextNode = i;
					tmp = maxFlow[src][i];
				}
    		}
    		disTmp[nextNode] = minDis;
    		flowTmp[nextNode] = tmp;
    		isVisited[nextNode] = true;
    		//松弛操作
    		for(int i=0; i<nodesNum; i++){
				if(!isVisited[i] && edgesWeight[src][nextNode]+edgesWeight[nextNode][i]< edgesWeight[src][i]){
					edgesWeight[src][i] = edgesWeight[src][nextNode] + edgesWeight[nextNode][i];
					maxFlow[src][i] = Math.min(maxFlow[src][nextNode], maxFlow[nextNode][i]);
					shortPathTmp[i] = shortPathTmp[nextNode] +"->"+i;
				}
    		}
    	}
    	for(int i=0; i<des.length; i++){
    		dis[i] = disTmp[des[i]];
    		shortPath[i] = shortPathTmp[des[i]];
    		flow[i] = flowTmp[des[i]];
    	}
    }
}
