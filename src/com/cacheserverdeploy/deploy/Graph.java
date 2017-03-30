package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;






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
	enum UpdateOperator{
	    	MINUS, PLUS;
	}
	final static int MAX_VALUE = 100000;
	final int serverCost;
	final int linkNum;
	final int clientsNum;
	final int nodesNum;
	List<Integer> nodes;//图中所有非消费节点
	int[][] bandWidths;
	int[][] unitCosts;
	List<ThreeTuple<Integer, Integer, Integer>> clds;	//cld 表示 client linkedNode demand，及消费节点client 相连的服务器节点 带宽需求
	
	boolean[] isServer; 
	
	int[] maxOffer;
	int[] out;
	int[] nodeFlow;
	int[] nodeCost;
	boolean changed;




	public Graph(int nodesNum, int clientsNum, int serverCost, int linkNum){
		this.nodes = new ArrayList<>(nodesNum);
		this.clds = new ArrayList<>(clientsNum);
		this.bandWidths = new int[nodesNum][nodesNum];
		this.unitCosts = new int[nodesNum][nodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientsNum = clientsNum;
		this.nodesNum = nodesNum;
		
		this.out = new int[nodesNum];
		this.maxOffer = new int[nodesNum];
		this.nodeFlow = new int[nodesNum];
		this.nodeCost = new int[nodesNum];
		
		this.isServer = new boolean[nodesNum];
		
		for(int i=0; i<nodesNum; i++){
			nodes.add(i);
			bandWidths[i][i] = MAX_VALUE;
		}
		
	}
	
	
	
	
	public String printServers() {
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<nodesNum; i++)
			if(isServer[i])
			sb.append(i+" ");
		return sb.toString();
			
	}
	public void addEdge(int src, int des, int bandWidth, int unitCost){
		this.bandWidths[src][des] = bandWidth;
		this.bandWidths[des][src] = bandWidth;
		this.unitCosts[src][des] = unitCost;
		this.unitCosts[des][src] = unitCost;
		this.out[src] += bandWidth;
		this.out[des] += bandWidth;
	}
	
	public void addClient(int node, int linkedNode, int demand){
		this.out[linkedNode] += demand;
		this.isServer[linkedNode] = true;
		this.clds.add(new ThreeTuple<>(node, linkedNode, demand));
		
	}

	
	public List<Integer> getServers(){
		List<Integer> serverList = new ArrayList<>();
		for(int i=0; i<nodesNum; i++){
		if(this.isServer[i])
			serverList.add(i);
		}
		return serverList;
	}
	
	public void plusNodeFlow(ThreeTuple<String, Integer, Integer> pcf){
		String[] nodeStrs = pcf.first.split(" ");
		int cost = 0;
		int node = Integer.parseInt(nodeStrs[0]);
		nodeFlow[node] += pcf.third;
		int i=0;
		int nextNode=0;
		for(i=1; i<nodeStrs.length-1; i++){
			nextNode = Integer.parseInt(nodeStrs[i]);
			nodeFlow[nextNode] += pcf.third;
			cost += unitCosts[node][nextNode] * pcf.third;
			nodeCost[nextNode] += cost;	
//			if(nodeCost[nextNode] > serverCost)
//				isServer[nextNode] = true;
			node = nextNode;
		}
		int lastNode = Integer.parseInt(nodeStrs[i]);
		cost += unitCosts[nextNode][lastNode] * pcf.third;
		nodeCost[lastNode] += cost;
	}
	
	
	

	/**
	 * 将消费节点按照带宽需求排序
	 */
	public void sortClients(){
    	Collections.sort(this.clds, new Comparator<ThreeTuple<Integer, Integer, Integer>>() {
			@Override
			public int compare(ThreeTuple<Integer, Integer, Integer> o1, ThreeTuple<Integer, Integer, Integer> o2) {
				return o1.third - o2.third;
			}
		});
	}
	
	/**
	 * 根据 path 和 对应的 increment 更新边上剩余带宽
	 * 
	 * @param path 路径
	 * @param increment 增量
	 * @param operator 增加或者减少
	 */
	public void updateBandWidth(String path, int increment, UpdateOperator operator){
		int src, des;
		String[] pathNodesStr = path.split(" ");
		for(int ii=0; ii<pathNodesStr.length-1; ii++){
			src = Integer.parseInt(pathNodesStr[ii]);//起始节点
			des = Integer.parseInt(pathNodesStr[ii+1]); //终止节点
			if(operator == UpdateOperator.MINUS){
				bandWidths[src][des] = bandWidths[src][des] - increment;
			}else{
				bandWidths[src][des] = bandWidths[src][des] + increment;
			}
		}
	}

    public 	Map<Integer, List<ThreeTuple<String, Integer, Integer>>>  getBestServers(){
       Map<Integer, List<ThreeTuple<String, Integer, Integer>>> clientPaths = new HashMap<>();
    	for(ThreeTuple<Integer, Integer, Integer> cld: clds){
    		List<ThreeTuple<String, Integer, Integer>> optiPaths = new ArrayList<>();
    		ThreeTuple<String, Integer, Integer> optPcf = null;
    		int need = cld.third;
    		int linkedNode = cld.second;
    		int client = cld.first;
    		int cost = 0;
    		while(true){
    			optPcf = getOptPath(linkedNode);
    			if(optPcf==null)
    				break;
    			int real = Math.min(optPcf.third, need);
    			optiPaths.add(new ThreeTuple<>(optPcf.first, optPcf.second, real));
    			
    			updateBandWidth(optPcf.first, real, UpdateOperator.MINUS);
    			need-=real;
    			cost += real * optPcf.second;
    			if(need <= 0 || cost > serverCost)
    				break;
    		}
    		//租用流量
    		if(need <= 0 && cost < serverCost){
    			if(nodeFlow[linkedNode]>0)
    				changed = true;
    			isServer[linkedNode] = false;
    			for(ThreeTuple<String, Integer, Integer> optiPath: optiPaths){
    				plusNodeFlow(optiPath);
    			}
    		}else{
    		//设立服务器
    			for(ThreeTuple<String, Integer, Integer> optiPath: optiPaths){
    				updateBandWidth(optiPath.first, optiPath.third, UpdateOperator.PLUS);
    			}
    			optiPaths.clear();
    			optiPaths.add(new ThreeTuple<>(linkedNode+"", 0, cld.third));
    		}
    		clientPaths.put(client, optiPaths);
    	}
    	return clientPaths;
	}
    
    public boolean update(){
    	int maxNodeCost = serverCost;
    	int maxNode = -1;
    	for(int node: nodes){
    		if(nodeCost[node] > maxNodeCost){
    			maxNode = node;
    			maxNodeCost = nodeCost[node];
    		}
    	}
    	if(maxNode == -1){
    		return false;
    	}else{
    		isServer[maxNode] = true;
    		System.out.println(maxNode + " set server!");
    		return true;
    	}
    }
    
    
    
    public ThreeTuple<String, Integer, Integer> getOptPath(int src){
    	List<Integer> servers = getServers();
    	if(servers.contains(src))
    		servers.remove(new Integer(src));
    	List<ThreeTuple<String, Integer, Integer>> paths = new ArrayList<>();
    	for(int node: servers){
    		ThreeTuple<String, Integer, Integer> path =  getPath(node, src);
    		if(path != null)
    			paths.add(path);
    	}
		if(paths.size()==0)
			return null;
		Collections.sort(paths, new Comparator<ThreeTuple<String, Integer, Integer>>() {
			@Override
			public int compare(ThreeTuple<String, Integer, Integer> o1, ThreeTuple<String, Integer, Integer> o2) {
				return o1.second - o2.second;
			}
		});
		return paths.get(0);
	}
	
	private ThreeTuple<String, Integer, Integer> getPath(int src, int des){
    	int[] costs = new int[nodes.size()];
    	int[] flows = new int[nodes.size()];
    	String[] shortPaths = new String[nodes.size()];
    	//dijkstra方法计算结果是 src 到所有顶点的最短距离
    	dijkstra(src, shortPaths, costs, flows);
    	if(costs[des] != Graph.MAX_VALUE)
    		return new ThreeTuple<String, Integer, Integer>(shortPaths[des], costs[des], flows[des]);
    	return null;
	}
    
    
    private void dijkstra(int src, String[] shortPaths, int[] unitCosts, int[] flows){
    	int nodesNum = this.nodesNum;
    	int[][] costs = new int[nodesNum][nodesNum];
    	int[][] maxFlow = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
        		maxFlow[i][j] = bandWidths[i][j];
    			if(this.unitCosts[i][j] == 0 || this.bandWidths[i][j] == 0){//没有对应的边
    				costs[i][j] = MAX_VALUE;
    			}else{
    				costs[i][j]  =this.unitCosts[i][j];
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





class TwoTuple<A, B>{
	final A first;
	final B second;
	public TwoTuple(A first, B second){
		this.first = first;
		this.second = second;
	}
	@Override
	public String toString(){
		
		return this.first + " " + this.second;
	}
}
class  ThreeTuple<A,  B, C extends Comparable<? super C>> extends TwoTuple<A,  B>{
	
	final C third;
	public ThreeTuple(A first, B second, C third){
		super(first, second);
		this.third = third;
	}
	
	@Override
	public String toString(){
		return first + " " +second + " " + third;
	}
}
