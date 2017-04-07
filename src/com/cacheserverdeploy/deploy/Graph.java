package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sun.org.glassfish.external.statistics.annotations.Reset;


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
	final static int MAX_VALUE = 100000;//图中整数最大值（由官网给出）
	final int serverCost;
	final int linkNum;
	final int clientsNum;
	final int nodesNum;
	
	final List<Integer> nodes;//图中所有非消费节点
	final int[][] bandWidths; //节点间的带宽
	final int[][] bandWidthsBak; //带宽数据备份
	final int[][] unitCosts; //单位单款租用费用
	Map<Integer, Integer> linkClient; //与消费节点直连点与其对应的消费节点
	Map<Integer, Integer> linkDemand; //与消费节点直连点与其对应的需求
	Map<Integer, List<Integer>> neighbors;
	int[] nodeFlow; //流经节点的流量和
	int[] nodeCost; //每个节点流经流量花费的代价
	int totalDemand;//所有消费节点的需求和
	boolean[] isServer; //是否为服务器节点
	boolean[] forbid;




	public Graph(int nodesNum, int clientsNum, int serverCost, int linkNum){
		this.nodes = new ArrayList<>(nodesNum);
		this.bandWidths = new int[nodesNum][nodesNum];
		this.bandWidthsBak = new int[nodesNum][nodesNum];
		this.unitCosts = new int[nodesNum][nodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientsNum = clientsNum;
		this.nodesNum = nodesNum;
		this.linkClient = new HashMap<>();	
		this.linkDemand = new LinkedHashMap<>();
		this.neighbors = new HashMap<>();
		this.nodeFlow = new int[nodesNum];
		this.nodeCost = new int[nodesNum];
		this.forbid = new boolean[nodesNum];
		this.isServer = new boolean[nodesNum];
		for(int i=0; i<nodesNum; i++){
			this.nodes.add(i);
			this.bandWidths[i][i] = 0;
			this.bandWidthsBak[i][i] = 0;
			this.unitCosts[i][i] = MAX_VALUE;
			this.neighbors.put(i, new LinkedList<>());
		}
		
	}
	
	/**
	 * 添加边，存储相应信息
	 * 
	 * @param src 起点
	 * @param des 终点
	 * @param bandWidth 带宽大小
	 * @param unitCost 单位带宽代价
	 */
	public void addEdge(int src, int des, int bandWidth, int unitCost){
		this.bandWidths[src][des] = bandWidth;
		this.bandWidths[des][src] = bandWidth;
		this.bandWidthsBak[src][des] = bandWidth;
		this.bandWidthsBak[des][src] = bandWidth;
		this.unitCosts[src][des] = unitCost;
		this.unitCosts[des][src] = unitCost;
		this.neighbors.get(src).add(des);
		this.neighbors.get(des).add(src);
	}
	
	/**
	 * 添加消费节点，存储对应的信息
	 * 
	 * @param node 消费节点
	 * @param linkedNode 直连节点
	 * @param demand 需求
	 */
	public void addClient(int node, int linkedNode, int demand){
		this.isServer[linkedNode] = true;
		linkClient.put(linkedNode, node);
		linkDemand.put(linkedNode, demand);		
		totalDemand += demand;
	}
	
	/**
	 * 将无边的两点之间unitcost设置为最大
	 */
	public void setUnitcostsOfZeroBnadWidth(){
		for(int i=0; i<nodesNum; i++){
			for(int j=0; j<nodesNum; j++){
				if(bandWidths[i][j] == 0){
					unitCosts[i][j] = MAX_VALUE;
				}
			}
		}
	}
	
	/**
	 * 恢复图中带宽及其它信息
	 */
	public void reset(){
		nodeCost = new int[nodesNum];
		nodeFlow = new int[nodesNum];
		forbid = new boolean[nodesNum];
		for(int i=0; i<nodesNum; i++)
			for(int j=0; j<nodesNum; j++){
				bandWidths[i][j] = bandWidthsBak[i][j];
			}
	}
	
	

	/**
	 * 获取服务器节点
	 * 
	 * @return 
	 */
	public List<Integer> getServers(){
		List<Integer> serverList = new LinkedList<>();
		for(int i=0; i<nodesNum; i++){
		if(isServer[i])
			serverList.add(i);
		}
		return serverList;
	}
	
	/**
	 * 根据路径增加路径上节点的流量和花费的代价
	 * 
	 * @param pcf
	 */
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
			cost += unitCosts[node][nextNode] * pcf.flow;
			nodeFlow[nextNode] += pcf.flow;
			nodeCost[nextNode] += cost;	
			node = nextNode;
		}
		int lastNode = pcf.path.getFirst();
		cost += unitCosts[nextNode][lastNode] * pcf.flow;
		nodeCost[lastNode] += cost;
	}
	
	/**
	 * 根据路径减少路径上节点的流量和花费的代价
	 * 
	 * @param pcf
	 */
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
	 * 根据 path 和 对应的 increment 增加边上剩余带宽
	 * 
	 * @param path 路径
	 * @param increment 增量
	 * @param operator 增加或者减少
	 */
	public void plusBandWidth(LinkedList<Integer> path, int increment){
		int src=0, des=0;
		for(int i=path.size()-1; i>0; i--){
			src = path.get(i);
			des = path.get(i-1);
			bandWidths[src][des] = bandWidths[src][des] + increment;
		}
	}
	
	/**
	 * 根据 path 和 对应的 increment 减少边上剩余带宽
	 * 
	 * @param path 路径
	 * @param increment 增量
	 * @param operator 增加或者减少
	 */
	public void minusBandWidth(LinkedList<Integer> path, int increment){
		int src=0, des=0;
		for(int i=path.size()-1; i>0; i--){
			src = path.get(i);
			des = path.get(i-1);
			bandWidths[src][des] = bandWidths[src][des] - increment;
		}
	}
	
	/**
	 * 将消费节点按照带宽需求排序
	 */
	public void sortClients(){
		List<Map.Entry<Integer, Integer>> entryList = new LinkedList<>(linkDemand.entrySet());
    	Collections.sort(entryList, new Comparator<Map.Entry<Integer, Integer>>() {
			@Override
			public int compare(Map.Entry<Integer, Integer> o1, Map.Entry<Integer, Integer> o2) {
				return o1.getValue() - o2.getValue();
			}
		});
    	LinkedHashMap<Integer, Integer> tmp = new LinkedHashMap<>();
    	for(Map.Entry<Integer, Integer> entry: entryList)
    		tmp.put(entry.getKey(), entry.getValue());
    	linkDemand = tmp;
	}
	
	/**
	 * 将消费节点的顺序按照需求随机打乱
	 */
	public void randomClients(){
		List<Map.Entry<Integer, Integer>> entryList = new LinkedList<>(linkDemand.entrySet());
		Collections.shuffle(entryList);
		LinkedHashMap<Integer, Integer> tmp = new LinkedHashMap<>();
    	for(Map.Entry<Integer, Integer> entry: entryList)
    		tmp.put(entry.getKey(), entry.getValue());
    	linkDemand = tmp;
	}

	/**
	 * 根据参数设置服务器节点
	 * 
	 * @param servers
	 */
	public void setServers(List<Integer> servers){
		this.isServer = new boolean[this.nodesNum];
		for(int server:servers)
			this.isServer[server] = true;
	}
	
	public String print(List<PCF> paths, List<Integer> servers){
		printServers();
		StringBuffer sb = new StringBuffer();
		int cost = servers.size() * serverCost;
		for(PCF pcf: paths){
    		int client = linkClient.get(pcf.path.getLast());
    		sb.append("\n");
    		System.out.print("\n");
    		for(int i=0; i<pcf.path.size(); i++){
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
     * 计算路径path单位带宽总费用
     * 
     * @param path
     * @return
     */
    private int calculatePathCost(LinkedList<Integer> path){
    	int cost = 0;
    	for(int i=path.size()-1; i>0; i--){
    		int src = path.get(i);
    		int des = path.get(i-1);
    		cost+=unitCosts[src][des];
    	}
    	return cost;
    }
	
    /**
     * 计算路径path中最大流量
     * 
     * @param path
     * @return
     */
    private int calculatePathFlow(LinkedList<Integer> path){
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
    
	/**
	 * 根据各节点流量获取最优路径
	 * 
	 * @param flows
	 * @return
	 */
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
			pcfs.add(new PCF(path, calculatePathCost(path), miniFlow));
			maxflow -= miniFlow;
			if(maxflow == 0)
				break;
		}
		return pcfs;
	}
    
    /**
     * 打印服务器节点
     */
    public void printServers(){
    	StringBuilder sb = new StringBuilder();
    	for(int node:nodes){
    		if(isServer[node])
    			sb.append(node+" ");
    	}
    	System.out.println(sb.toString());;
    }
    
	/**
	 * 模拟博弈过程，每个消费节点作为带宽需求者
	 */
    public void simulateGame(){
    	List<PCF> pcfs = new LinkedList<>();
    	nodeCost = new int[nodesNum];
    	nodeFlow = new int[nodesNum];
       	List<TwoTuple<Integer, Integer>> unsats = new LinkedList<>();//未满足流量需求的直连节点 及其对应的流量
       //将所有直连节点和对应的流量需求加入 unsats
       	for(Map.Entry<Integer, Integer> entry: linkDemand.entrySet())
       		unsats.add(new TwoTuple<>(entry.getKey(), entry.getValue()));
       	while(unsats.size()!=0){
       		TwoTuple<Integer, Integer> curr = unsats.get(0);
       		int linkedNode = curr.fir;
    		int demand = curr.sec;
    		List<PCF> optiPaths = new LinkedList<>();
    		PCF optPcf = null;
    		int cost = 0;
    		//linkedNode 连接的消费节点若购买流量，计算需要支付的最小费用
    		while(true){
    			optPcf = getOptPath(linkedNode);
    			if(optPcf==null)
    				break;
    			int real = Math.min(optPcf.flow, demand);
    			optiPaths.add(new PCF(optPcf.path, optPcf.cost, real));
    			minusBandWidth(optPcf.path, real);
    			demand-=real;
    			cost += real * optPcf.cost;
    			if(demand <= 0)
    				break;
    		}
    		
    		//租用流量
    		if(demand <= 0 && cost + nodeCost[linkedNode]< serverCost && !forbid[linkedNode]){
    			//告知其租用流量的用户，不再提供流量
    			List<PCF> rmPcfs = new LinkedList<>();
    			for(PCF pcf: pcfs){
    				if(pcf.path.getLast() == linkedNode){
    					plusBandWidth(pcf.path, pcf.flow);
    					minusNodeFlow(pcf);
    					int link = pcf.path.getFirst();
    					unsats.add(new TwoTuple<>(link, pcf.flow));
    					rmPcfs.add(pcf);
    				}
    			}
    			pcfs.removeAll(rmPcfs);
    			isServer[linkedNode] = false;
    			
    		}
    		//设立服务器
    		else{
    			//恢复购买流量路径的占用的带宽
    			for(PCF optiPath: optiPaths){
    				plusBandWidth(optiPath.path, optiPath.flow);
    			}
    			optiPaths.clear();
    			
    			List<PCF> uselessPcfs = new LinkedList<>();
    			List<PCF> modifiedPcfs = new LinkedList<>();
    			for(PCF pcf: pcfs){
    				//linkedNode 在某条路径中间，则需要更新前半部分的费用和流量信息
    				if(pcf.path.contains(linkedNode)
    					&& pcf.path.getFirst()!=linkedNode
    					&& pcf.path.getLast()!=linkedNode)
    				{
    					modifiedPcfs.add(pcf);
    				}
    				//linkedNode 购买流量的所有路径需要删除并恢复占用的带宽
    				if(pcf.path.getFirst()==linkedNode){
    					plusBandWidth(pcf.path, pcf.flow);
    					minusNodeFlow(pcf);	
    					uselessPcfs.add(pcf);
    				}
    			}
    			pcfs.removeAll(uselessPcfs);
    			
    		
    			for(PCF pcf:modifiedPcfs){
    				pcfs.remove(pcf);
    				int idx = pcf.path.indexOf(linkedNode);
    				LinkedList<Integer> cutPath = new LinkedList<>(pcf.path.subList(idx, pcf.path.size()));
    				plusBandWidth(cutPath, pcf.flow);
    				minusNodeFlow(new PCF(cutPath, pcf.cost, pcf.flow));
    				LinkedList<Integer> newPath = new LinkedList<>(pcf.path.subList(0, idx+1));
    				pcfs.add(new PCF(newPath, calculatePathCost(newPath), pcf.flow));
    			}
    	
    			List<TwoTuple<Integer, Integer>> alreadySat = new LinkedList<>();
    			for(TwoTuple<Integer, Integer> tmp: unsats){
    				if( tmp.fir == linkedNode){
    					alreadySat.add(tmp);
    				}
    			}
    			unsats.removeAll(alreadySat);
    			
    			LinkedList<Integer> path = new LinkedList<>();
    			path.add(linkedNode);
    			optiPaths.add(new PCF(path, 0, linkDemand.get(linkedNode)));

    			isServer[linkedNode] = true;
    			forbid[linkedNode] = true;
    		}
    		for(PCF pcf: optiPaths)
    			plusNodeFlow(pcf);
    		pcfs.addAll(optiPaths);
    		unsats.remove(curr);
    	}
	}
    
    
    /**
     * 若某个节点的成本超过服务器构造成本，则将自身设置为服务器节点
     */
    public void updateServers(){
    	for(int node: nodes){
    		if(nodeFlow[node] == 0 && isServer[node])
    			isServer[node] = false;
    		if(nodeCost[node] > serverCost && !isServer[node]){
				forbid[node] = true;
	    		isServer[node] = true;
    		}
    	}
    }
   
    /**
     * 求节点 src 到其他服务器的最优路径
     * 
     * @param src
     * @return
     */
    public PCF getOptPath(int src){
    	List<Integer> servers = getServers();
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
		
		//加入一定随机因素，改变纯贪婪策略
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
		return new PCF(optPath, minCost, calculatePathFlow(optPath));
	}
    
    private int[][] initWeights(){
    	int[][] weights = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
    			if(this.bandWidths[i][j] == 0){//没有对应的边
    				weights[j][i] = MAX_VALUE;
    			}else{
    				weights[j][i]  =this.unitCosts[i][j];
    			}
    		}
    	}
    	return weights;
    }
	//TODO 需要删除
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

	
 

    
	/**
	 * dijkstra 算法实现
	 * 
	 * @param src
	 * @param weights
	 * @param shortPaths
	 * @param unitCosts
	 */
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

	public TwoTuple<int[][], Integer> MCMF(){
		int N = this.nodesNum+2; //原始图中增加超级源点和汇点
		int[][] flows = new int[N][N];
		int cost[][] = new int[N][N];
		int[][] cap = new int[N][N];
		int[][] inverseCap = new int[N][N];
		int totalCost = 0;
		initMCMF(cost, cap, N);
		int[] pre = new int[N];
		int totalFlow = 0;
		while(SPFA(pre, cap, inverseCap, cost)){
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
			totalFlow += flow;
		}
		System.out.println("maxFlow:"+totalFlow);
		for(int i=0; i<nodesNum; i++){
			for(int j=0; j<nodesNum; j++){
				totalCost += cost[i][j] * flows[i][j];
			}
		}
		return new TwoTuple<>(flows, totalCost);
		
	}
	private void initMCMF(int[][] cost, int[][] cap, int nodesNum){
		for(int i=0; i<nodesNum-2; i++){
			for(int j=0; j<nodesNum-2; j++){
				cost[i][j] = unitCosts[i][j];
				cap[i][j] =  bandWidthsBak[i][j];
			}
		}
		for(int server: this.getServers()){
			cap[nodesNum-2][server] = Graph.MAX_VALUE;
			cost[nodesNum-2][server] = 0;
			cap[server][nodesNum-2] = 0;
			cost[server][nodesNum-2] = Graph.MAX_VALUE;
		}
		 
		for(Entry<Integer, Integer> entry : this.linkDemand.entrySet()){
			cap[nodesNum-1][entry.getKey()] = 0;
			cost[nodesNum-1][entry.getKey()] = Graph.MAX_VALUE;
			cap[entry.getKey()][nodesNum-1] = entry.getValue();
			cost[entry.getKey()][nodesNum-1] = 0;
		}
	}
	public boolean SPFA(int[] pre, int[][] cap, int[][] inverseCap, int[][] cost){
		boolean[] vis = new boolean[nodesNum];
		int[] dis = new int[nodesNum];
		LinkedList<Integer> list = new LinkedList<>();
		int[] cnt = new int[nodesNum];
		for(int i=0; i<nodesNum; i++){
			dis[i] = Graph.MAX_VALUE;
			vis[i] = false;
			pre[i] = -1;
		}
		dis[nodesNum-2] = 0;
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
					if(dis[i] > dis[u] + tmp){
						dis[i] = dis[u] + tmp;
						pre[i] = u;
					}
					if(!vis[i]){
						if(++cnt[i] == nodesNum)
							return false;
						if(!list.isEmpty() && dis[i] < dis[list.peek()])
							list.addFirst(i);
						else
							list.addLast(i);
						vis[i] = true;
					}
				}
				
			}
		}
		return dis[nodesNum-1] < Graph.MAX_VALUE;
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

