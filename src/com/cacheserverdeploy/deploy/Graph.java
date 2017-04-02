package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
	int[][] bandWidthsBak;
	int[][] unitCosts;
	List<CLD> clds;	//cld 表示 client linkedNode demand，及消费节点client 相连的服务器节点 带宽需求
	Map<Integer, Integer> linkClient;
	
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
		this.bandWidthsBak = new int[nodesNum][nodesNum];
		this.unitCosts = new int[nodesNum][nodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientsNum = clientsNum;
		this.nodesNum = nodesNum;
		this.linkClient = new HashMap<>();		
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
		this.bandWidthsBak[src][des] = bandWidth;
		this.bandWidthsBak[des][src] = bandWidth;
		this.unitCosts[src][des] = unitCost;
		this.unitCosts[des][src] = unitCost;
		this.out[src] += bandWidth;
		this.out[des] += bandWidth;
	}
	
	public void addClient(int node, int linkedNode, int demand){
		this.out[linkedNode] += demand;
		this.isServer[linkedNode] = true;
		this.clds.add(new CLD(node, linkedNode, demand));
		linkClient.put(linkedNode, node);
		
	}
	
	public void recoverBandwidths(){
		for(int i=0; i<nodesNum; i++)
			for(int j=0; j<nodesNum; j++)
				bandWidths[i][j] = bandWidthsBak[i][j];
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
		int node = pcf.path.getLast();
		if(pcf.path.size()==1){
			nodeFlow[node] += pcf.flow;
			return;
		}
		int cost = 0;
		nodeFlow[node] += pcf.flow;
		int i=0;
		int nextNode=0;
		for(i=pcf.path.size()-2; i>0; i--){
			nextNode = pcf.path.get(i);
			nodeFlow[nextNode] += pcf.flow;
			cost += unitCosts[node][nextNode] * pcf.flow;
			nodeCost[nextNode] += cost;	
			node = nextNode;
		}
		int lastNode = pcf.path.getFirst();
		cost += unitCosts[nextNode][lastNode] * pcf.flow;
		nodeCost[lastNode] += cost;
	}
	
	
	public void minusNodeFlow(PCF pcf){
		int cost = 0;
		int node = pcf.path.getLast();
		nodeFlow[node] -= pcf.flow;
		int i=0;
		int nextNode=0;
		for(i=pcf.path.size()-2; i>0; i--){
			nextNode = pcf.path.get(i);
			nodeFlow[nextNode] -= pcf.flow;
			cost += unitCosts[node][nextNode] * pcf.flow;
			nodeCost[nextNode] -= cost;	
			node = nextNode;
		}
		int lastNode = pcf.path.getFirst();
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
	public void updateBandWidth(LinkedList<Integer> path, int increment, UpdateOperator operator){
		int src=0, des=0;
		for(int i=path.size()-1; i>0; i--){
			src = path.get(i);
			des = path.get(i-1);
			if(operator == UpdateOperator.MINUS){
				bandWidths[src][des] = bandWidths[src][des] - increment;
			}else{
				bandWidths[src][des] = bandWidths[src][des] + increment;
			}
		}
	}
	
	


	
    public 	List<PCF>  getBestServers(){
    	List<PCF> paths = new LinkedList<>();
    	nodeCost = new int[nodesNum];
    	nodeFlow = new int[nodesNum];
       	List<CLD> unsatClds = new ArrayList<>(clds);
       	while(unsatClds.size()!=0){
       		CLD cld = unsatClds.get(0);
    		List<PCF> optiPaths = new ArrayList<>();
    		PCF optPcf = null;
    		int need = cld.demand;
    		int linkedNode = cld.linked;
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
    			if(need <= 0)
    				break;
    		}
    		//租用流量
    		if(need <= 0 && cost + nodeCost[linkedNode]< serverCost && !forbid[linkedNode]){
    			//告知其租用流量的用户，不再提供流量
        		System.out.println(linkedNode);
    			List<PCF> unsatPcfs = new LinkedList<>();
    			for(PCF pcf: paths){
    				if(pcf.path.getLast() == linkedNode){
    					updateBandWidth(pcf.path, pcf.flow, UpdateOperator.PLUS);
	    				minusNodeFlow(pcf);
    					int link = pcf.path.getFirst();
    					unsatClds.add(new CLD(linkClient.get(link), link, pcf.flow));
    					unsatPcfs.add(pcf);
    				}
    			}
    			paths.removeAll(unsatPcfs);
    			isServer[linkedNode] = false;
    			
    			
    		}else{
    		//设立服务器
    			for(PCF optiPath: optiPaths){
    				updateBandWidth(optiPath.path, optiPath.flow, UpdateOperator.PLUS);
    			}
    			optiPaths.clear();
    			
    			int flow = 0;
    			List<PCF> uselessPcfs = new LinkedList<>();
    			for(PCF pcf: paths){
    				if(pcf.path.getFirst()==linkedNode){
    					updateBandWidth(pcf.path, pcf.flow, UpdateOperator.PLUS);
    					minusNodeFlow(pcf);
    					flow+=pcf.flow;	
    					uselessPcfs.add(pcf);
    				}
    			}
    			paths.removeAll(uselessPcfs);
    	
    			List<CLD> uselessClds = new LinkedList<>();
    			for(int i=1; i<unsatClds.size()-1; i++){
    				CLD uselessCld = unsatClds.get(i);
    				if( uselessCld.linked==linkedNode){
    					flow+=uselessCld.demand;
    					uselessClds.add(uselessCld);
    				}
    			}
    			unsatClds.removeAll(uselessClds);
    			
    			LinkedList<Integer> tmp = new LinkedList<>();
    			tmp.add(linkedNode);
    			optiPaths.add(new PCF(tmp, 0, cld.demand+flow));
    			isServer[linkedNode] = true;
    		}
    		for(PCF pcf: optiPaths)
    			plusNodeFlow(pcf);
    		paths.addAll(optiPaths);
    		unsatClds.remove(cld);
    	}
    	return paths;
	}
    
    public boolean update(){
    	boolean changed = false;
    	for(int node: nodes){
//    		if(Math.random() < (float)nodeCost[node]/(float)serverCost){
    		if(nodeCost[node] > serverCost){
    			if(!isServer[node]){
    				System.out.println(node);
    				changed = true;
    				forbid[node] = true;
    	    		isServer[node] = true;
    			}
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
    	 Map<Integer, LinkedList<Integer>> shortPaths = new HashMap<>();
		 int[] costs = new int[nodesNum];
		 dijkstra(src, shortPaths, costs);
		 int minCost = MAX_VALUE;
		 int candidate = -1;
    	for(int server: servers){
    		if(costs[server] < minCost){
    			candidate = server;
    			minCost = costs[server];
    		}
    	}
		if(minCost==MAX_VALUE)
			return null;
		LinkedList<Integer> optPath = shortPaths.get(candidate);
		return new PCF(optPath, minCost, getMiniFlow(optPath));
	}
    
    private int getMiniFlow(List<Integer> path){
    	int miniFlow = MAX_VALUE;
    	for(int i=path.size()-1; i>0; i--){
    		int src = path.get(i);
    		int des = path.get(i-1);
    		if (bandWidths[src][des] < miniFlow) {
    			miniFlow = bandWidths[src][des];
			}
    	}
    	return miniFlow;
    }
    
    
	private void dijkstra(int src, Map<Integer, LinkedList<Integer>> shortPaths, int[] unitCosts){
    	int nodesNum = this.nodesNum;
    	int[][] costs = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
    			if(this.unitCosts[i][j] == 0 || this.bandWidths[i][j] == 0){//没有对应的边
    				costs[j][i] = MAX_VALUE;
    			}else{
    				costs[j][i]  =this.unitCosts[i][j];
    			}
    		}
    	}
    	
    	boolean isVisited[] = new boolean[nodesNum];//标记节点最短距离是否求出
    	
    	//初始化节点 src 到其他节点的距离为无穷大
    	for(int i=0; i<unitCosts.length; i++){
    		unitCosts[i] = Integer.MAX_VALUE;
    		LinkedList<Integer> list = new LinkedList<>();
    		list.add(src);
    		list.add(i);
    		shortPaths.put(i, list);
    	}
    	
    	isVisited[src] = true;
    	unitCosts[src] = 0;
    	
    	for(int count=1; count<nodesNum; count++){
    		int minDis = Integer.MAX_VALUE;
    		int nextNode = -1;
    		for(int i=0; i<nodesNum; i++){
				if(! isVisited[i] && costs[src][i] < minDis ){
					minDis = costs[src][i];
					nextNode = i;
				}
    		}
    		unitCosts[nextNode] = minDis;
    		isVisited[nextNode] = true;
    		//松弛操作
    		for(int i=0; i<nodesNum; i++){
				if(!isVisited[i] && costs[src][nextNode]+costs[nextNode][i]< costs[src][i]){
					costs[src][i] = costs[src][nextNode] + costs[nextNode][i];
					LinkedList<Integer> list = shortPaths.get(i);
					list.clear();
					list.addAll(shortPaths.get(nextNode));
					list.add(i);
				}
    		}
    	}
    }
}





class PCF{
	final LinkedList<Integer> path;
	final int cost;
	final int flow;
	public PCF(LinkedList<Integer> path, int cost, int flow){
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

