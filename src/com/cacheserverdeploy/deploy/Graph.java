package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.org.apache.xpath.internal.operations.Bool;




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

	private List<Integer> nodes;//图中所有非消费节点
	int[][] bandWidths;
	int[][] unitCosts;
	List<ThreeTuple<Integer, Integer, Integer>> clds;	//cld 表示 client linkedNode demand，及消费节点client 相连的服务器节点 带宽需求
	final int serverCost;
	final int linkNum;
	final int clientsNum;
	final int nodesNum;
	private boolean isSolve;
	boolean[] isServer; //对应节点是否设置为服务器
	


	/*与节点评估值相关的信息*/
	private int[] assessedValues; //存储每个节点的评估值
	private int[] totalCost; //存储每个节点所有带宽的租用费用和
	int[] totalFlow; //每个节点的能够提供的带宽和
	int totalFlows;
	int totalDemand;
	float maxUnitCostOfDemand;
	int[][] clientToNodesdis;
	List<League> leagues;
	Map<Integer, Integer> leagueID;




	public Graph(int nodesNum, int clientsNum, int serverCost, int linkNum){
		this.nodes = new ArrayList<>(nodesNum);
		this.clds = new ArrayList<>(clientsNum);
		this.bandWidths = new int[nodesNum][nodesNum];
		this.unitCosts = new int[nodesNum][nodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientsNum = clientsNum;
		this.nodesNum = nodesNum;
		
		
		this.assessedValues = new int[nodesNum];
		this.totalCost = new int[nodesNum];
		this.totalFlow = new int[nodesNum];
		this.totalFlows = 0;
		this.isServer = new boolean[nodesNum];
		this.clientToNodesdis = new int[clientsNum][nodesNum];
		leagues = new ArrayList<>(clientsNum);
		leagueID = new HashMap<>();
		for(int i=0; i<nodesNum; i++){
			nodes.add(i);
			bandWidths[i][i] = MAX_VALUE;
			isServer[i] = true;
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
		this.totalCost[src] += bandWidth * unitCost;
		this.totalCost[des] += bandWidth * unitCost;
		this.totalFlow[src] += bandWidth;
		this.totalFlow[des] += bandWidth;
		
	}
	
	public void addClient(int node, int linkedNode, int demand){
		this.totalFlow[linkedNode] += demand;//与消费节点连接的节点能满足对应消费节点的所有带宽
		this.clds.add(new ThreeTuple<>(node, linkedNode, demand));
		this.totalDemand+= demand;
	}
	
	public List<Integer> getNodes() {
		return nodes;
	}
	
	public List<Integer> getServers(){
		List<Integer> serverList = new ArrayList<>();
		for(int i=0; i<nodesNum; i++){
		if(this.isServer[i])
			serverList.add(i);
		}
		return serverList;
	}

	public boolean isSolve() {
		return isSolve;
	}

	
	public void calculateDis(){
		for(int i=0; i<clientsNum; i++){
			this.clientToNodesdis[clds.get(i).first] = getShortDis(clds.get(i).second, nodes);
		}
	}
	
	public void initLeagus(){
		for(League league: leagues){
			league.initOffer(this);
		}
	}
	
	public League getLeague(int id){
		for(League league: leagues)
			if(league.client == id)
				return league;
		return null;
	}
	
	
	public void createLeagues(){
		boolean isVisited[] = new boolean[this.nodes.size()];
		for(ThreeTuple<Integer, Integer, Integer> cld: this.clds){
			League league = new League(cld.first, cld.third);
			league.nodes.add(cld.second);
			leagueID.put( cld.second, league.client);
			isVisited[cld.second] = true;
			this.leagues.add(league);
		}
		int visitedNodeNum = leagues.size();
		int dis = 1;
		while(visitedNodeNum != this.nodesNum){
			for(League league: leagues){
				for(int node: this.nodes){
					if(!isVisited[node]){
						if(this.clientToNodesdis[league.client][node] == dis){
							league.nodes.add(node);
							leagueID.put(node, league.client);
							isVisited[node] = true;
							visitedNodeNum++;
						}
					}
				}
			}
			dis++;
		}
		
	}
	
	


	public void setSolve(boolean isSolve) {
		this.isSolve = isSolve;
	}
	
	public int[] getAssessedValues(){
		return this.assessedValues;
	}

	/**
	 * 将消费节点按照带宽需求排序
	 */
	public void sortClients(){
    	Collections.sort(this.clds, new Comparator<ThreeTuple<Integer, Integer, Integer>>() {
			@Override
			public int compare(ThreeTuple<Integer, Integer, Integer> o1, ThreeTuple<Integer, Integer, Integer> o2) {
				return o2.third - o1.third;
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
	public void updateBandWidth(List<ThreeTuple<ThreeTuple<String, Integer, Integer>, Integer, Integer>> pcfClientAllocates, UpdateBandwidthOperator operator){
		for(ThreeTuple<ThreeTuple<String, Integer, Integer>, Integer, Integer> pcfCA:pcfClientAllocates){
			updateBandWidth(pcfCA.first.first, pcfCA.third, operator);
		}
	}

	public void flipNode(){
		int maxNode = 0;
		int minNode = 0;
		int maxAssVal = 0;
		int minAssVal = Integer.MAX_VALUE;
		for(int i=0; i<nodesNum; i++){
			if(assessedValues[i] > maxAssVal){
				maxAssVal = assessedValues[i];
				maxNode = i;
			}
			if(assessedValues[i] < minAssVal){
				minAssVal = assessedValues[i];
				minNode = i;
			}
		}
		isServer[maxNode] = true;
		isServer[minNode] = false;
	}
	
	
	
	
	
 
    
	/**
     * 初始化每个节点的评估值
     * 初始评估值设置方法是 该节点流出流量值之和 + w*该节点出现在消费节点之间最短路径上的频次
     * 其中，节点流出流量之和，在读取图的过程中已经初始化
     */
    public void initAssessedValues(){
    	for(int i=0; i<this.nodesNum; i++){
    		this.assessedValues[i] = this.totalFlow[i];
    		this.totalFlows+=totalFlow[i];
    	}
    	for(int i=0; i<this.nodesNum; i++){
//    		if(Math.random() > (double)totalFlow[i]/totalFlows)
    			isServer[i] = false;
    	}
    	this.maxUnitCostOfDemand = ((float)serverCost*clientsNum)/(float)totalDemand;
    	
    }
    
    
    /**
     * 计算某个server部署方案的成本
     * 
     * @param servers 服务器部署方案
     * @param pcfClientAllocated 存储已经占用路径,连接的消费节点及分配的带宽
     * @return 返回二元组，如果能够满足所有消费节点，返回 <true, 对应的成本>，如果不能满足所有消费节点，返回 <false, 0>
     */
    public TwoTuple<Boolean, Float> costOfServerNodes(List<Integer> servers,
    		List<ThreeTuple<ThreeTuple<String, Integer, Integer>, Integer, Integer>> pcfClientAllocated){
    	int totalCost = 0;
    	int totalObtained = 0;
      	boolean[] isSatisfied = new boolean[this.clientsNum];
    	int satClientNum = 0;
    	//加上服务器的部署成本
    	totalCost = servers.size() * this.serverCost;
    	
    	for(int i=0; i<this.clientsNum; i++){
    		int obtained = 0; //节点i已获得的带宽
       		int demand = this.clds.get(i).third;//当前消费节点的需求带宽
    		int need = 0;//仍需要的带宽
    		int allocated = 0;//某路径上真实分配的带宽
    		
    		ThreeTuple<String, Integer, Integer> pcf = null; //pcf 表示 path cost flow， 即最短路径和对应的单位带宽租用费用和最大能通过的带宽
    		while(true){
    			//返回当前所有服务器到消费节点 clds.get(i).second 最优的一条路径
        		pcf = getOptPCF(servers, this.clds.get(i).second);
        		//所有服务器都不存在路径到消费该节点
        		if(pcf.second == Graph.MAX_VALUE){
        			break;
        		}
        		need = demand - obtained;
    			allocated = pcf.third >  need ? need : pcf.third;//按需求分配带宽，若路径剩余带宽大于仍需要的带宽，仅需分配仍需要的带宽即可
        		updateBandWidth(pcf.first, allocated, UpdateBandwidthOperator.MINUS); //更新边上剩余带宽
        		totalCost += pcf.second * allocated;
//        		System.out.println(pcf.first +" "+clds.get(i).first+ " " + realFlow);
//        		outStrsTmp.add("\r\n"+pcf.first +" "+clds.get(i).first+ " " + realFlow);
        		pcfClientAllocated.add(new ThreeTuple<>(pcf, this.clds.get(i).first, allocated));
        		obtained += allocated;
        		if(obtained == demand){
        			isSatisfied[clds.get(i).first] = true;
        			break;
        		}
    		}
    		if(isSatisfied[clds.get(i).first]){
    			satClientNum++;
    		}
    		totalObtained+=obtained;
    	}
    	if(satClientNum == this.clientsNum){
    		return new TwoTuple<>(true, totalCost - totalObtained* maxUnitCostOfDemand);
    	}else{
    		return new TwoTuple<>(false, totalCost - totalObtained *maxUnitCostOfDemand);
    	}
    }

    /**
     * 根据当前服务器节点，选择一条最优路径
     * @param serverNodes
     * @param linkedNode
     * @return
     */
    private ThreeTuple<String, Integer, Integer> getOptPCF(List<Integer> serverNodes, int linkedNode){
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
    private ThreeTuple<String, Integer, Integer> getShortPath(int src, int des){
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
    
    private String[] getShortPath(int src, List<Integer> des){
    	int[] costs = new int[nodesNum];
    	int[] flows = new int[nodesNum];
    	String[] shortPaths = new String[nodesNum];
    	//dijkstra方法计算结果是 src 到所有顶点的最短距离
    	dijkstra(src, shortPaths, costs, flows);
    	String[] paths = new String[des.size()];
    	for(int i=0; i<paths.length; i++){
    		paths[i] = shortPaths[des.get(i)];
    	}
    	return paths;
    }
    
    private int[] getShortDis(int src, List<Integer> des){
    	int[] costs = new int[nodesNum];
    	int[] flows = new int[nodesNum];
    	String[] shortPaths = new String[nodesNum];
    	//dijkstra方法计算结果是 src 到所有顶点的最短距离
    	dijkstra(src, shortPaths, costs, flows);
    	int[] dis = new int[des.size()];
    	for(int i=0; i<dis.length; i++){
    		dis[i] = costs[des.get(i)];
    	}
    	return dis;
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


	public int getTotalFlows() {
		return totalFlows;
	}


	public void setTotalFlows(int totalFlows) {
		this.totalFlows = totalFlows;
	}
    
}





class TwoTuple<A, B>{
	A first;
	B second;
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
	
	C third;
	public ThreeTuple(A first, B second, C third){
		super(first, second);
		this.third = third;
	}
	
	@Override
	public String toString(){
		return first + " " +second + " " + third;
	}
}
