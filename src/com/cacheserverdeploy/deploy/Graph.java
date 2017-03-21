package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 
 * @ClassName: Graph
 *
 * @Description: 网络图结构
 *
 * @author: ccding
 * @date: 2017年3月16日 下午2:52:48
 *
 */
public class Graph {
	enum UpdateBandwidthOperator{
	    	MINUS, PLUS;
	}
	final static int MAX_VALUE = 100000;

	private int[] nodes;//图中所有非消费节点
	private int[][] bandWidths;
	private int[][] unitCosts;;
	List<ThreeTuple<Integer, Integer, Integer>> clds;//client linkedNode demand表示消费节点集合
	final int serverCost;
	final int linkNum;
	final int clientsNum;
	final int nodesNum;
	private int[] assessedValues; //存储每个节点的评估值
	private boolean[] isServer; //对应节点是否设置为服务器
	private boolean[] isLinkedNode; //是否连接消费节点

	
	public Graph(int nodesNum, int clientsNum, int serverCost, int linkNum){
		this.nodes = new int[nodesNum];
		this.clds = new ArrayList<>(clientsNum);
		this.bandWidths = new int[nodesNum][nodesNum];
		this.unitCosts = new int[nodesNum][nodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientsNum = clientsNum;
		this.nodesNum = nodesNum;
		this.assessedValues = new int[nodesNum];
		this.isServer = new boolean[nodesNum];
		this.isLinkedNode = new boolean[nodesNum];
		for(int i=0; i<nodesNum; i++){
			nodes[i] = i;
			bandWidths[i][i] = MAX_VALUE;
		}
		
	}
	
	
	public void addEdge(int src, int des, int bandWidth, int unitCost){
		this.bandWidths[src][des] = bandWidth;
		this.bandWidths[des][src] = bandWidth;
		this.unitCosts[src][des] = unitCost;
		this.unitCosts[des][src] = unitCost;
		assessedValues[src]+=bandWidth;
		assessedValues[des]+=bandWidth;
		
	}
	
	public void addClient(int node, int linkedNode, int demand){
		isLinkedNode[linkedNode] = true;
		assessedValues[linkedNode] += demand;
		clds.add(new ThreeTuple<>(node, linkedNode, demand));
	}
	
	public int[] getNodes() {
		return nodes;
	}

	public List<ThreeTuple<Integer, Integer, Integer>> getCLDs() {
		return clds;
	}

	public int[][] getUnitCosts() {
		return unitCosts;
	}


	public int[][] getBandWidths(){
		return this.bandWidths;
	}

	/**
	 * 根据 path 和 对应的 increment 更新边上剩余带宽
	 * 
	 * @param path
	 * @param increment
	 * @param operator 
	 */
	public void updateBandWidth(String path, int increment, UpdateBandwidthOperator operator){
		int src, des;
		String[] pathNodesStr = path.split(" ");
		for(int ii=0; ii<pathNodesStr.length-1; ii++){
			src = Integer.parseInt(pathNodesStr[ii]);//起始节点
			des = Integer.parseInt(pathNodesStr[ii+1]); //终止节点
			if(operator == UpdateBandwidthOperator.MINUS){
				bandWidths[src][des] = bandWidths[src][des] - increment;
			}else{
				bandWidths[src][des] = bandWidths[src][des] + increment;
			}
		}
	}
    /**
     * 按一定策略选择初始服务器节点
     * 
     * @param serverNum 服务器节点个数
     * @return 选择的服务器节点
     */
    public List<Integer> selectServerNodes(int initServerNum){
    	List<Integer> selectedServerNodes = new ArrayList<>();
    	List<Map.Entry<Integer, Double>> freList = new ArrayList<>(this.frequency.entrySet());
    	Collections.sort(freList, new Comparator<Map.Entry<Integer, Double>>() {
			@Override
			public int compare(Entry<Integer, Double> o1, Entry<Integer, Double> o2) {
				// TODO Auto-generated method stub
				return o2.getValue()-o1.getValue() >= 0 ? 1 : -1;
			}
		});
    	for(int i=0; i<initServerNum; i++){
    		if(Math.random() < 0.9){
    			selectedServerNodes.add(freList.remove(0).getKey());
    		}else{
    			selectedServerNodes.add(freList.remove((int)(Math.random()*freList.size())).getKey());
    		}
    	}
    	return selectedServerNodes;
    }
    
    public Map<Integer, Double> getFrequency() {
		return frequency;
	}
    
	/**
     * 初始化每个节点的评估值
     * 初始评估值设置方法是 该节点流出流量值之和 + w*该节点出现在消费节点之间最短路径上的频次
     * 其中，节点流出流量之和，在读取图的过程中已经初始化
     */
    public void initFrequency(){
    	//初始化设置为0
    	for(int i=0; i<nodesNum; i++){
        	double totalBandwidth = 0;
    		for(int j=0; j<nodesNum; j++){
    			if(i!=j){
    			totalBandwidth+=bandWidths[i][j];
    			}
    		}
    		
    		this.frequency.put(nodes[i], totalBandwidth);
    	}
    	for(int i=0; i<clientsNum; i++){
    		for(int j = 0; j<clientsNum; j++){
    			ThreeTuple<String, Integer, Integer>  pathCostFlow = getShortPath(clds.get(i).second, clds.get(j).second);
    			String[] nodesStr = pathCostFlow.first.split(" ");
    			for(String nodeStr:nodesStr){
    				int node = Integer.parseInt(nodeStr);
    				this.frequency.put(node, this.frequency.get(node) + (double)clds.get(j).third/100.0);
    			}
    		}
    	}
    }
    
   
    public void updateFrequency(String path){
    	String[] nodesStr = path.split(" ");
    	for(String nodeStr: nodesStr){
    		int node = Integer.parseInt(nodeStr);
    		this.frequency.put(node, this.frequency.get(node)+1);
    	}
    }
    
    public void updateFrequency(int node){
    	this.frequency.put(node, this.frequency.get(node)+20);
    }

    /**
     * 根据当前服务器节点，选择一条最优路径
     * @param serverNodes
     * @param linkedNode
     * @return
     */
    public ThreeTuple<String, Integer, Integer> getOptPCF(List<Integer> serverNodes, int linkedNode){
    	List<ThreeTuple<String, Integer, Integer>> pcfs = new ArrayList<>();
		ThreeTuple<String, Integer, Integer> pcf = null;
		for(int serverIdx=0; serverIdx<serverNodes.size(); serverIdx++){
			pcf = getShortPath(serverNodes.get(serverIdx), linkedNode);
			pcfs.add(pcf);
		}
		Collections.sort(pcfs, new Comparator<ThreeTuple<String, Integer, Integer>>(){
			@Override
			public int compare(ThreeTuple<String, Integer, Integer> pcf1, ThreeTuple<String, Integer, Integer> pcf2){
				return pcf1.second - pcf2.second ;
			}
		});
		return pcfs.get(0);
    }
    
    /**
     * 使用 dijkstra 算法寻找图中节点 src 到节点 des 的最小租用费用路径
     * 
     * @param src 开始节点
     * @param des 终止节点集合
     * @param shortPath 路径信息
     * @param dis 最小单位租用费用
     */
    public ThreeTuple<String, Integer, Integer> getShortPath(int src, int des){
    	if(src == des){
    		return new ThreeTuple<String, Integer, Integer>(src+"", 0, MAX_VALUE);
    	}
    	int[] costs = new int[nodesNum];
    	int[] flows = new int[nodesNum];
    	String[] shortPaths = new String[nodesNum];
    	//dijkstra方法计算结果是 src 到所有顶点的最短距离
    	dijkstra(src, shortPaths, costs, flows);
    	return new ThreeTuple<String, Integer, Integer>(shortPaths[des], costs[des], flows[des]);
    }
    
    private void dijkstra(int src, String[] shortPaths, int[] unitCosts, int[] flows){
    	int nodesNum = this.nodesNum;
    	
    	int[][] costs = new int[nodesNum][nodesNum];
    	int[][] maxFlow = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
        		maxFlow[i][j] = bandWidths[i][j];
    			if(costs[i][j] == 0 || bandWidths[i][j] == 0){//没有对应的边
    				costs[i][j] = MAX_VALUE;
    			}else{
    				costs[i][j]  = this.unitCosts[i][j];
    			}
    		}
    	}
    	
    	boolean isVisited[] = new boolean[nodesNum];//标记节点最短距离是否求出
    	
    	//初始化节点 src 到其他节点的距离为无穷大
    	for(int i=0; i<unitCosts.length; i++){
    		unitCosts[i] = Integer.MAX_VALUE;
    		shortPaths[i] = src+" "+i;
    	}
    	
    	isVisited[src] = true;
    	unitCosts[src] = 0;
    	flows[src] = MAX_VALUE;
    	
    	for(int count=1; count<nodesNum; count++){
    		int minDis = Integer.MAX_VALUE;
    		int nextNode = -1;
    		int flow = 0;
    		for(int i=0; i<nodesNum; i++){
				if(! isVisited[i] && 
						(costs[src][i] < minDis || (costs[src][i] == minDis && maxFlow[src][i] > flow))){
					minDis = costs[src][i];
					nextNode = i;
					flow = maxFlow[src][i];
				}
    		}
    		unitCosts[nextNode] = minDis;
    		flows[nextNode] = flow;
    		isVisited[nextNode] = true;
    		//松弛操作
    		for(int i=0; i<nodesNum; i++){
				if(!isVisited[i] && costs[src][nextNode]+costs[nextNode][i]< costs[src][i]){
					costs[src][i] = costs[src][nextNode] + costs[nextNode][i];
					maxFlow[src][i] = Math.min(maxFlow[src][nextNode], maxFlow[nextNode][i]);
					shortPaths[i] = shortPaths[nextNode] +" "+i;
				}
    		}
    	}
    }
    
}

class  ThreeTuple<A, B extends Comparable<? super B>, C extends Comparable<? super C>>{
	final A first;
	final B second;
	final C third;
	public ThreeTuple(A first, B second, C third){
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	@Override
	public String toString(){
		return first + " " +second + " " + third;
	}
}
