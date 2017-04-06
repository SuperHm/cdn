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

import sun.print.resources.serviceui_sv;
import sun.security.action.GetBooleanAction;

import java.util.Map.Entry;
import java.util.Queue;







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
	Map<Integer, Integer> linkDemand;
	int totalDemand;
	
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
		this.linkDemand = new HashMap<>();
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
			bandWidthsBak[i][i] = MAX_VALUE;
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
		linkDemand.put(linkedNode, demand);		
		totalDemand += demand;
	}
	
	public void recover(){
		nodeCost = new int[nodesNum];
		nodeFlow = new int[nodesNum];
		forbid = new boolean[nodesNum];
		for(int i=0; i<nodesNum; i++)
			for(int j=0; j<nodesNum; j++)
				bandWidths[i][j] = bandWidthsBak[i][j];
	}

	
	public List<Integer> getServers(){
		List<Integer> serverList = new ArrayList<>();
		for(int i=0; i<nodesNum; i++){
		if(isServer[i])
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
	
	public void randomClients(){
		Collections.shuffle(clds);
	}
	
	public int getCost(List<PCF> paths){
		int cost = getServers().size() * serverCost;
		for(PCF pcf: paths){		
    		cost += pcf.cost * pcf.flow;
    	}
		return cost;
	}
	
	public void recoverServers(List<Integer> servers){
		isServer = new boolean[nodesNum];
		for(int server:servers)
			isServer[server] = true;
	}
	
	public String print(List<PCF> paths, List<Integer> servers){
		printServers();
		StringBuffer sb = new StringBuffer();
		int cost = servers.size() * serverCost;
		for(PCF pcf: paths){
    		int client = linkClient.get(pcf.path.getFirst());
    		sb.append("\n");
    		System.out.print("\n");
    		for(int i=pcf.path.size()-1; i>=0; i--){
    			sb.append(pcf.path.get(i)+" ");
    			System.out.print(pcf.path.get(i)+" ");
    		}
    		sb.append(client+" "+pcf.flow);
    		System.out.print(client+" "+pcf.flow);
    		cost += pcf.cost * pcf.flow;
    	}
    	System.out.println("\ncost:"+cost);
    	return sb.toString();
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
	
	


	
    public 	List<PCF> getBestServers(){
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
//    		System.out.println("cld :"+cld.client +" "+ cld.demand +" "+ cld.linked);
    		//租用流量
    		if(need <= 0 && cost + nodeCost[linkedNode]< serverCost && !forbid[linkedNode]){
    			//告知其租用流量的用户，不再提供流量
//    			System.out.println("租用流量");
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
//    			System.out.println("设立服务器");
    			for(PCF optiPath: optiPaths){
    				updateBandWidth(optiPath.path, optiPath.flow, UpdateOperator.PLUS);
    			}
    			optiPaths.clear();
    			
    			int flow = 0;
    			List<PCF> uselessPcfs = new LinkedList<>();
    			List<PCF> modifiedPcfs = new LinkedList<>();
    			for(PCF pcf: paths){
    				if(pcf.path.contains(linkedNode)&&pcf.path.getFirst()!=linkedNode&&pcf.path.getLast()!=linkedNode){
    					modifiedPcfs.add(pcf);
    				}
    				if(pcf.path.getFirst()==linkedNode){
    					updateBandWidth(pcf.path, pcf.flow, UpdateOperator.PLUS);
    					minusNodeFlow(pcf);
    					flow+=pcf.flow;	
    					uselessPcfs.add(pcf);
    				}
    			}
    			paths.removeAll(uselessPcfs);
    			for(PCF pcf:modifiedPcfs){
    				paths.remove(pcf);
    				int idx = pcf.path.indexOf(linkedNode);
    				LinkedList<Integer> cutPath = new LinkedList<>(pcf.path.subList(idx, pcf.path.size()));
    				updateBandWidth(cutPath, pcf.flow, UpdateOperator.PLUS);
    				minusNodeFlow(new PCF(cutPath, pcf.cost, pcf.flow));
    				LinkedList<Integer> newPath = new LinkedList<>(pcf.path.subList(0, idx+1));
    				paths.add(new PCF(newPath, getMiniCost(newPath), pcf.flow));
    			}
    	
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
    			forbid[linkedNode] = true;
    		}
    		for(PCF pcf: optiPaths)
    			plusNodeFlow(pcf);
    		paths.addAll(optiPaths);
    		unsatClds.remove(cld);
    	}
       	return paths;
	}
    
    public void update(){
    	for(int node: nodes){
    		if(nodeFlow[node] == 0 && isServer[node])
    			isServer[node] = false;
    		if(nodeCost[node] > serverCost && !isServer[node]){
				forbid[node] = true;
	    		isServer[node] = true;
    		}
    	}
    }
    
    
    public void printServers(){
    	StringBuilder sb = new StringBuilder();
    	for(int node:nodes){
    		if(isServer[node])
    			sb.append(node+" ");
    	}
    	System.out.println(sb.toString());;
    }
    
    
    public PCF getOptPath(int src){
    	List<Integer> servers = getServers();
//    	System.out.println(servers);
    	if(servers.contains(src))
    		servers.remove(new Integer(src));
    	 Map<Integer, LinkedList<Integer>> shortPaths = new HashMap<>();
		 int[] costs = new int[nodesNum];
		 int[][] weights = initWeights();
		 dijkstra(src, weights, shortPaths, costs);
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
		double r = 0.2;
		if(nodesNum > 200)
			r = 0.15;
		if(nodesNum > 400)
			r = 0.1;
		if(Math.random() < r){
			candidate = servers.get((int)(Math.random() * servers.size()));
			
			minCost = costs[candidate];
		}
		LinkedList<Integer> optPath = shortPaths.get(candidate);
		return new PCF(optPath, minCost, getMiniFlow(optPath));
	}
    
    
	public List<PCF> getOptPaths(List<Integer> servers){
		 List<PCF> optPaths = new LinkedList<>();
		 Map<Integer, LinkedList<Integer>> shortPaths = new HashMap<>();
		 int[] costs = new int[nodesNum+2];
		 Map<Integer, Integer> linkDemandCopy = new HashMap<>(linkDemand);
		 while(true){
			 int[][] weights = initWeights(servers);
			 for(Entry<Integer, Integer> entry: linkDemandCopy.entrySet()){
				 if(entry.getValue()==0){
					 weights[nodesNum][entry.getKey()] = MAX_VALUE;
				 }else{
					 weights[nodesNum][entry.getKey()] = 0;
				 }
			 }
			 dijkstra(nodesNum, weights, shortPaths, costs);
			 int cost = costs[nodesNum+1];
			 if(cost==MAX_VALUE)
				 break;
			 LinkedList<Integer> path = shortPaths.get(nodesNum+1);
			 path.removeFirst();
			 path.removeLast();
			 int link = path.getFirst();
			 int miniFlow = Math.min(getMiniFlow(path),linkDemandCopy.get(link));
			 linkDemandCopy.put(link, linkDemandCopy.get(link)-miniFlow);
			 updateBandWidth(path, miniFlow, UpdateOperator.MINUS);
			 optPaths.add(new PCF(path, cost, miniFlow));
			 
		 }
		return optPaths;
	}
	
	
	public int[][] getFlows(){
		int[][] flow = new int[nodesNum][nodesNum];
		for(int i=0; i<nodesNum; i++){
			for(int j=0; j<nodesNum; j++){
				flow[i][j] = bandWidthsBak[i][j] - bandWidths[i][j];
			}
		}
		
		for(int i=0; i<nodesNum; i++){
			for(int j=i; j<nodesNum; j++){
				if(flow[i][j] > flow[j][i]){
					flow[i][j] = flow[i][j] - flow[j][i];
					flow[j][i] = 0;
				}else{
					flow[j][i] = flow[j][i] - flow[i][j];
					flow[i][j] = 0;
				}
			}
		}
		return flow;
	}
	
	
	public LinkedList<PCF> getPaths(int[][] flows){
		int nodesNum = flows.length;
		int des = flows.length-1;
		int maxflow = totalDemand;
		LinkedList<PCF> pcfs = new LinkedList<>(); 
		while(true){
			int src = flows.length-2;
			LinkedList<Integer> path = new LinkedList<>();
			path.add(src);
			int miniFlow = MAX_VALUE;
			while(src != des){
				for(int node=0; node<nodesNum; node++){
					if(flows[src][node] > 0){
						path.add(node);
						if(flows[src][node]<miniFlow)
							miniFlow = flows[src][node];
						src = node;
						break;
					}
				}
			}
			for(int i=0; i<path.size()-1; i++){
				flows[path.get(i)][path.get(i+1)] -= miniFlow;
			}
			path.remove(0);
			path.remove(path.size()-1);
			pcfs.add(new PCF(path, getMiniCost(path), miniFlow));
			maxflow -= miniFlow;
			if(maxflow == 0)
				break;
		}
		return pcfs;
	}
	
    private int getMiniFlow(LinkedList<Integer> path){
    	if(path.size()==1)
    		return linkDemand.get(path.getFirst());
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
    
    private int getMiniCost(LinkedList<Integer> path){
    	int cost = 0;
    	for(int i=path.size()-1; i>0; i--){
    		int src = path.get(i);
    		int des = path.get(i-1);
    		cost+=unitCosts[src][des];
    	}
    	return cost;
    }
    
    
    private int[][] initWeights(){
    	int[][] weights = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
    			if(this.unitCosts[i][j] == 0 || this.bandWidths[i][j] == 0){//没有对应的边
    				weights[j][i] = MAX_VALUE;
    			}else{
    				weights[j][i]  =this.unitCosts[i][j];
    			}
    		}
    	}
    	return weights;
    }
    
    private int[][] initWeights(List<Integer> servers){
    	int[][] weights = new int[nodesNum+2][nodesNum+2];
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
    			if(this.bandWidths[i][j] == 0){//没有对应的边
    				weights[j][i] = MAX_VALUE;
    			}else{
    				weights[j][i]  =this.unitCosts[i][j];
    			}
    		}
    	}
    
    	for(int i=0; i<nodesNum+2; i++){
    		weights[i][nodesNum] = MAX_VALUE;
    		weights[nodesNum][i] = MAX_VALUE;
    		weights[i][nodesNum+1] = MAX_VALUE;
    		weights[nodesNum+1][i] = MAX_VALUE;
    	}
    	for(int server:servers){
    		weights[server][nodesNum+1] = 0;
    	}

    	return weights;
    	
    }
    

    
    
	private void dijkstra(int src, int[][] weights, Map<Integer, LinkedList<Integer>> shortPaths, int[] unitCosts){
    	int nodesNum = weights.length;
    	boolean isVisited[] = new boolean[nodesNum];//标记节点最短距离是否求出
    	//初始化节点 src 到其他节点的距离为无穷大
    	for(int i=0; i<unitCosts.length; i++){
    		unitCosts[i] = Integer.MAX_VALUE;
    		LinkedList<Integer> list = new LinkedList<>();
    		list.add(src);
    		list.add(i);
    		shortPaths.put(i, list);
    	}
    	LinkedList<Integer> nodes = new LinkedList<>();
    	for(int i=0; i< nodesNum; i++)
    		nodes.add(i);
    	
    	isVisited[src] = true;
    	nodes.remove(src);
    	unitCosts[src] = 0;
    	
    	while(nodes.size()!=0){
    		int minDis = Integer.MAX_VALUE;
    		int nextNode = -1;
    		for(int node:nodes){
				if(weights[src][node] < minDis ){
					minDis = weights[src][node];
					nextNode = node;
				}
    		}
    		unitCosts[nextNode] = minDis;
    		nodes.remove(new Integer(nextNode));
    		//松弛操作
    		for(int node:nodes){
				if( weights[src][nextNode]+weights[nextNode][node]< weights[src][node]){
					weights[src][node] = weights[src][nextNode] + weights[nextNode][node];
					LinkedList<Integer> list = shortPaths.get(node);
					list.clear();
					list.addAll(shortPaths.get(nextNode));
					list.add(node);
				}
    		}
    	}
    }
}


class MCMF{
	int nodesNum;
	boolean[] vis;
	int[] d;
	int pre[];
	int[][] cost;
	int[][] cap;
	int[][] inverseCap;
	int[][] flows;
	int totalCost;
	LinkedList<Integer> list;
	
	public MCMF(Graph graph){
		nodesNum = graph.nodesNum+2;
		cost = new int[nodesNum][nodesNum];
		cap = new int[nodesNum][nodesNum];
		inverseCap = new int[nodesNum][nodesNum];
		flows = new int[nodesNum][nodesNum];

		 for(int i=0; i<nodesNum-2; i++){
			 for(int j=0; j<nodesNum-2; j++){
				 if(graph.bandWidths[i][j] > 0){
					 cost[i][j] = graph.unitCosts[i][j];
					 cap[i][j] = graph.bandWidths[i][j];
				 }else{
					 cost[i][j] = Graph.MAX_VALUE;
					 cap[i][j] = 0; 
				 }
			 }
		 }
		 for(int server: graph.getServers()){
			 cap[nodesNum-2][server] = Graph.MAX_VALUE;
			 cost[nodesNum-2][server] = 0;
			 cap[server][nodesNum-2] = 0;
			 cost[server][nodesNum-2] = Graph.MAX_VALUE;
		 }
		 
		 for(Entry<Integer, Integer> entry : graph.linkDemand.entrySet()){
			 cap[nodesNum-1][entry.getKey()] = 0;
			 cost[nodesNum-1][entry.getKey()] = Graph.MAX_VALUE;
			 cap[entry.getKey()][nodesNum-1] = entry.getValue();
			 cost[entry.getKey()][nodesNum-1] = 0;
		 }
		 totalCost = graph.getServers().size() * graph.serverCost;
		 
		 
	}
	public boolean spfa(){
		vis = new boolean[nodesNum];
		d = new int[nodesNum];
		pre = new int[nodesNum];
		list = new LinkedList<>();
		int[] cnt = new int[nodesNum];
		for(int i=0; i<nodesNum; i++){
			d[i] = Graph.MAX_VALUE;
			vis[i] = false;
			pre[i] = -1;
		}
		d[nodesNum-2] = 0;
		list.add(nodesNum-2);
		while(!list.isEmpty()){
			int u = list.removeFirst();
			vis[u] = true;
			cnt[u] ++;
			for(int i=0; i<nodesNum; i++){
				if(cap[u][i] > 0 || inverseCap[u][i] > 0){
					int tmp;
					if(cap[u][i] != 0)
						tmp = inverseCap[u][i] == 0 ? cost[u][i] : -cost[u][i];
					else
						tmp = -cost[u][i];
					if(d[i] > d[u] + tmp){
						d[i] = d[u] + tmp;
						pre[i] = u;
					}
					if(!vis[i]){
						if(++cnt[i] == nodesNum)
							return false;
						if(!list.isEmpty() && d[i] < d[list.peek()])
							list.addFirst(i);
						else
							list.addLast(i);
						vis[i] = true;
					}
				}
				
			}
		}
		return d[nodesNum-1] < Graph.MAX_VALUE;
	}
	
	
	public int costFlow(){
		int flow = Graph.MAX_VALUE;
		for(int i=nodesNum-1; i!=nodesNum-2; i=pre[i]){
			if(inverseCap[pre[i]][i] == 0)
				flow = Math.min(flow, cap[pre[i]][i]);
			else
				flow = Math.min(flow, inverseCap[pre[i]][i]);
		}
		for(int i=nodesNum-1; i!=nodesNum-2; i=pre[i]){
			if(inverseCap[pre[i]][i] == 0){
				cap[pre[i]][i] -= flow;
				flows[pre[i]][i] += flow;
				if(pre[i] != nodesNum-2 && i != nodesNum-1){
					inverseCap[i][pre[i]] += flow;
				}
			}else {
				flows[i][pre[i]]-=flow;
				cap[i][pre[i]] += flow;
				inverseCap[pre[i]][i] -= flow;
			}
			
		}
		return flow;
	}
	
	public int getCost(){
		while(spfa()){
			costFlow();
		}
		for(int i=0; i<nodesNum; i++){
			for(int j=0; j<nodesNum; j++){
				totalCost += cost[i][j] * Math.abs(flows[i][j]);
			}
		}
		return totalCost;	
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

