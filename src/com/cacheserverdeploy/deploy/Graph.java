package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;






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
	List<CLD> clds;	//cld 表示 client linkedNode demand，及消费节点client 相连的服务器节点 带宽需求
	
	boolean[] isServer; 
	
	int[] maxOffer;
	int[] out;
	int[] nodeFlow;
	int[] nodeCost;
	boolean[] forbid;
    Set<Integer> satNodes;




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
		this.forbid = new boolean[nodesNum];
		this.isServer = new boolean[nodesNum];
		this.satNodes = new HashSet<>();
		for(int i=0; i<nodesNum; i++){
			nodes.add(i);
			bandWidths[i][i] = MAX_VALUE;
		}
		
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
		this.clds.add(new CLD(node, linkedNode, demand));
		
	}

	
	public List<Integer> getServers(){
		List<Integer> serverList = new ArrayList<>();
		for(int i=0; i<nodesNum; i++){
		if(this.isServer[i])
			serverList.add(i);
		}
		return serverList;
	}
	
	public void plusNodeFlow(PCF pcf){
		String[] nodeStrs = pcf.path.split(" ");
		int cost = 0;
		int node = Integer.parseInt(nodeStrs[0]);
		nodeFlow[node] += pcf.flow;
		int i=0;
		int nextNode=0;
		for(i=1; i<nodeStrs.length-1; i++){
			nextNode = Integer.parseInt(nodeStrs[i]);
			nodeFlow[nextNode] += pcf.flow;
			cost += unitCosts[node][nextNode] * pcf.flow;
			nodeCost[nextNode] += cost;	
			node = nextNode;
		}
		int lastNode = Integer.parseInt(nodeStrs[i]);
		cost += unitCosts[nextNode][lastNode] * pcf.flow;
		nodeCost[lastNode] += cost;
	}
	
	
	public void minusNodeFlow(PCF pcf){
		String[] nodeStrs = pcf.path.split(" ");
		int cost = 0;
		int node = Integer.parseInt(nodeStrs[0]);
		nodeFlow[node] -= pcf.flow;
		int i=0;
		int nextNode=0;
		for(i=1; i<nodeStrs.length-1; i++){
			nextNode = Integer.parseInt(nodeStrs[i]);
			nodeFlow[nextNode] -= pcf.flow;
			cost += unitCosts[node][nextNode] * pcf.flow;
			nodeCost[nextNode] -= cost;	
			node = nextNode;
		}
		int lastNode = Integer.parseInt(nodeStrs[i]);
		cost += unitCosts[nextNode][lastNode] * pcf.flow;
		nodeCost[lastNode] -= cost;
	}
	
	
	

	/**
	 * 将消费节点按照带宽需求排序
	 */
	public void sortClients(){
    	Collections.sort(this.clds, new Comparator<CLD>() {
			@Override
			public int compare(CLD o1, CLD o2) {
				return o1.demand - o2.demand;
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
		int src=0, des=0;
		
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


	
    public 	Map<Integer, List<PCF>>  getBestServers(){
      Map<Integer, List<TwoTuple<PCF, Integer>>> rends = new HashMap<>();
       Map<Integer, List<PCF>> clientPaths = new HashMap<>();
       nodeCost = new int[nodesNum];
       nodeFlow = new int[nodesNum];
       List<CLD> unsatClds = new ArrayList<>(clds);
     
       while(unsatClds.size()!=0){
    		CLD cld = unsatClds.get(0);
    		List<PCF> optiPaths = new ArrayList<>();
    		PCF optPcf = null;
    		int need = cld.demand;
    		int linkedNode = cld.linked;
    		int client = cld.client;
    		int cost = 0;
    		while(true){
    			optPcf = getOptPath(linkedNode);
    			if(optPcf==null)
    				break;
    			int real = Math.min(optPcf.flow, need);
    			optiPaths.add(new PCF(optPcf.path, optPcf.cost, real));
    			updateBandWidth(optPcf.path, real, UpdateOperator.MINUS);
    			need-=real;
    			cost += real * optPcf.cost;
    			if(need <= 0 || cost > serverCost)
    				break;
    		}
    		//租用流量
    		if(need <= 0 && cost + nodeCost[linkedNode]< serverCost && !forbid[linkedNode]){
    			//告知其租用流量的用户，不再提供流量
    			List<TwoTuple<PCF, Integer>> pcfClients = rends.get(linkedNode);
    			if(pcfClients != null){
    				forbid[linkedNode] = true;
    				System.out.println(linkedNode);
	    			for(TwoTuple<PCF, Integer> pcfClient: pcfClients){
	    				String[] nodeStrs = pcfClient.fir.path.split(" ");
	    				int link = Integer.parseInt(nodeStrs[nodeStrs.length-1]);
	    				clientPaths.get(pcfClient.sec).remove(pcfClient.fir);
	    				unsatClds.add(new CLD(pcfClient.sec, link, pcfClient.fir.flow));
	    				updateBandWidth(pcfClient.fir.path, pcfClient.fir.flow, UpdateOperator.PLUS);
	    				minusNodeFlow(pcfClient.fir);
	    			}
    			}
    			isServer[linkedNode] = false;
    			
    			//注册租用信息
    			List<TwoTuple<PCF, Integer>> list = null;
    			for(PCF optiPath: optiPaths){
    				plusNodeFlow(optiPath);
    				String[] nodeStrs = optiPath.path.split(" ");
    				int src = Integer.parseInt(nodeStrs[0]);
    				if(!rends.keySet().contains(src)){
    					List<TwoTuple<PCF, Integer>> newList = new ArrayList<>();
    					rends.put(src, newList);
    				}
    				list = rends.get(src);
    				list.add(new TwoTuple<>(optiPath, client));
    				rends.put(src, list);
    			}
    			
    		}else{
    		//设立服务器
    			for(PCF optiPath: optiPaths){
    				updateBandWidth(optiPath.path, optiPath.flow, UpdateOperator.PLUS);
    			}
    			int flow = 0;
    			List<PCF> list = clientPaths.get(client);
    			if(list != null){
    				for(PCF pcf: list)
        				flow+=pcf.flow;
    				optiPaths.addAll(list);
    			}  		
    			optiPaths.clear();
    			optiPaths.add(new PCF(linkedNode+"", 0, cld.demand+flow));
    			isServer[linkedNode] = true;
    		}
    		List<PCF> list = clientPaths.get(client);
    		if(list == null){
    			clientPaths.put(client, optiPaths);
    		}else {
				optiPaths.addAll(list);
				clientPaths.put(client, optiPaths);
			}
    		unsatClds.remove(cld);
    	}
    	return clientPaths;
	}
    
    public boolean update(){
    	boolean changed = false;
    	for(int node: nodes){
    		if(nodeCost[node] > serverCost){
    			if(!isServer[node]){
    				System.out.println(node);
    				changed = true;
    				forbid[node] = true;
    			}
	    		isServer[node] = true;
	    		System.out.println(node + " set server!");
    		}
    	}
    	return changed;
    }
    
    
    public String printServers(){
    	StringBuilder sb = new StringBuilder();
    	for(int node:nodes){
    		if(isServer[node])
    			sb.append(node+" ");
    	}
    	return sb.toString();
    }
    
    
    
    public PCF getOptPath(int src){
    	List<Integer> servers = getServers();
    	if(servers.contains(src))
    		servers.remove(new Integer(src));
    	List<PCF> paths = new ArrayList<>();
    	for(int node: servers){
    		PCF path =  getPath(node, src);
    		if(path != null)
    			paths.add(path);
    	}
		if(paths.size()==0)
			return null;
		Collections.sort(paths, new Comparator<PCF>() {
			@Override
			public int compare(PCF o1, PCF o2) {
				return o1.cost - o2.cost;
			}
		});
		return paths.get(0);
	}
	
	private PCF getPath(int src, int des){
    	int[] costs = new int[nodes.size()];
    	int[] flows = new int[nodes.size()];
    	String[] shortPaths = new String[nodes.size()];
    	//dijkstra方法计算结果是 src 到所有顶点的最短距离
    	dijkstra(src, shortPaths, costs, flows);
    	if(costs[des] != Graph.MAX_VALUE)
    		return new PCF(shortPaths[des], costs[des], flows[des]);
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





class PCF{
	final String path;
	final int cost;
	final int flow;
	public PCF(String path, int cost, int flow){
		this.path = path;
		this.cost = cost;
		this.flow = flow;
	}
	@Override
	public String toString(){
		return path + " " +cost + " " + flow;
	}
}

class CLD{
	final int client;
	final int linked;
	final int demand;
	public CLD(int client, int linked, int demand){
		this.client = client;
		this.linked = linked;
		this.demand = demand;
	}
	@Override
	public String toString(){
		return client + " " +linked + " " + demand;
	}
}

class TwoTuple<A, B>{
	final A fir;
	final B sec;
	public TwoTuple(A fir, B sec){
		this.fir = fir;
		this.sec = sec;
	}
	
	@Override
	public String toString(){
		return fir + " " +sec ;
	}
}

