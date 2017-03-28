package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cacheserverdeploy.deploy.Graph.UpdateBandwidthOperator;


public class League {
	int client; //id
	int demand;
	boolean setServer;
	int serverIdx;
	List<Integer> nodes;
	int[][] flow;
	int[][] cost;
	int[][] totalOffers;
	int[][] totalAcquires;
	int[][] maxOutOffer;
	int[][] miniCost;
	int[] outOffer;
	Map<Integer, List<TwoTuple<Integer, Integer>>> neighbors;
	Map<Integer, Map<Integer, List<ThreeTuple<String, Integer, Integer>>>> acquires;
	Map<Integer, Map<Integer, List<ThreeTuple<String, Integer, Integer>>>> offers;
	
	
	public League(int client, int demand){
		this.client = client;
		this.demand = demand;
		this.nodes = new ArrayList<>();
		this.offers = new HashMap<>();
		this.neighbors = new HashMap<>();
		this.acquires = new HashMap<>();
	}
	
	public void initSubgraph(Graph graph){
		this.flow = new int[nodes.size()][nodes.size()];
		this.cost = new int[nodes.size()][nodes.size()];
		for(int i=0; i<nodes.size(); i++){
			for(int j=0; j<nodes.size(); j++){
				flow[i][j] = graph.bandWidths[nodes.get(i)][nodes.get(j)];
				cost[i][j] = graph.unitCosts[nodes.get(i)][nodes.get(j)];
			}
		}
	}
	
	
	public boolean isSat(){
		return totalOffers[serverIdx][0] >= demand;
	}
	
	public void updateNeighborsAcquires(Graph graph){
		if(neighbors.size()==0)
			return;
		Map<Integer, List<ThreeTuple<String, Integer, Integer>>> offer = offers.get(serverIdx);
		for(int neighborID: neighbors.keySet()){
			League neighbor = graph.getLeague(neighborID);
			List<ThreeTuple<String, Integer, Integer>> list = null;
			Map<Integer, List<ThreeTuple<String, Integer, Integer>>> acquire = new HashMap<>();
			for(TwoTuple<Integer, Integer> src_des: neighbors.get(neighborID)){
				list = new ArrayList<>();
				int src = src_des.first;
				int des = src_des.second;
				int maxOffer = Math.min(maxOutOffer[serverIdx][nodes.indexOf(src)],
						graph.bandWidths[src][des]);
				ThreeTuple<String, Integer, Integer> pcfToDes = null;
				List<ThreeTuple<String, Integer, Integer>> pcfsToDes = new ArrayList<>(neighbor.offers.get(des).get(nodes.get(0)));
				while(maxOffer!=0){
					pcfToDes = pcfsToDes.get(0);
					
				}
				for(ThreeTuple<String, Integer, Integer> pcf: offer.get(src)){
					if(need < pcf.third){
						list.add(new ThreeTuple<>(pcf.first+" "+des,
								pcf.second+graph.unitCosts[src][des],
								need));
						break;
					}
					list.add(new ThreeTuple<>(pcf.first+" "+des,
							pcf.second+graph.unitCosts[src][des],
							pcf.third));
					need -= pcf.third;
					
				}
				acquire.put(des, list);
				
			}
			neighbor.acquires.put(client, acquire);
			
		}
	}
	
	
	
	
	public TwoTuple<Boolean, Integer> getBestServer(Graph graph){
		if(acquires.size() !=0){
			int totalAc = 0;
			int totalCost = 0;
			for(Map<Integer, List<ThreeTuple<String, Integer, Integer>>> acquire:acquires.values()){
				for(int des: acquire.keySet()){
					for(ThreeTuple<String, Integer, Integer> pcf: acquire.get(des)){
						int alloc = Math.min(pcf.third, maxOutOffer[nodes.indexOf(des)][0]);
						totalAc += alloc;
						totalCost = pcf.second * alloc;
					}
				}
			}
			//不设置服务器
			if(totalAc > demand && totalCost < graph.serverCost){
				//撤销联盟内设置的server
				if(setServer){
					setServer = false;
					serverIdx = 0;
					for(int neighborID: neighbors.keySet()){
						League neighbor = graph.getLeague(neighborID);
						neighbor.acquires.remove(client);
					}
				}
				
				
				int need = demand;
				int alloc = 0;
				for(int neighborID: acquires.keySet()){
					Map<Integer, List<ThreeTuple<String, Integer, Integer>>> acquire = acquires.get(neighborID);
					League neighbor = graph.getLeague(neighborID);
					for(ThreeTuple<String, Integer, Integer> pcf: pcfs){
						alloc = pcf.third > need ? need : pcf.third;
						need -= alloc;
						int srcIdx = neighbor.nodes.indexOf(pcf.first);
						neighbor.outOffer[srcIdx] -= alloc;
						System.out.println(pcf.first+"->"+pcf.second+"->消费节点"+client+"   带宽："+alloc);
						for(int i=0; i<neighbor.nodes.size(); i++){
							neighbor.maxOutOffer[i][srcIdx] -= alloc;
						}
						if(need==0)
							break;
					}
					if(need==0)
						break;
				}
				return new TwoTuple<>(false, 0);
			}
		}
		int maxOffer = 0;
		int minCost = 0;
		int serverIdx = 0;
		for(int i=0; i<nodes.size(); i++){
			int outOffer = 0;
			int cost = 0;
			for(int j=0; j<nodes.size(); j++){
				outOffer += maxOutOffer[i][j];
				cost += miniCost[i][j];
			}
			if(maxOutOffer[i][0] < outOffer && (outOffer > maxOffer || (outOffer == maxOffer && cost < minCost))){
				serverIdx = i;
				maxOffer = outOffer;
				minCost = cost;
			}	
		}
		this.serverIdx = serverIdx;
		System.out.println(nodes.get(serverIdx)+"->消费节点"+client+"   带宽："+demand);
		this.setServer = true;
		updateNeighborsAcquires(graph);
		return new TwoTuple<>(true, nodes.get(serverIdx));
	}
	public void initOffer(Graph graph){
		outOffer = new int[nodes.size()];
		for(int i=0; i<nodes.size(); i++){
			for(int node: graph.getNodes()){
				if(!this.nodes.contains(node) && graph.bandWidths[nodes.get(i)][node] != 0){
					outOffer[i] += graph.bandWidths[nodes.get(i)][node];
					int neighborID = graph.leagueID.get(node);
					List<TwoTuple<Integer, Integer>> list = null;
					if(neighbors.get(neighborID) == null){
						list = new ArrayList<>();
					}else{
						list = neighbors.get(neighborID);
					}
					list.add(new TwoTuple<>(nodes.get(i), node));
					neighbors.put(neighborID, list);
				}
			}
		}
		outOffer[0] += demand;
		totalOffers = new int[nodes.size()][nodes.size()];
		miniCost = new int[nodes.size()][nodes.size()];
		maxOutOffer = new int[nodes.size()][nodes.size()];
		for(int serverIdx=0; serverIdx<nodes.size(); serverIdx++){
			Map<Integer, List<ThreeTuple<String, Integer, Integer>>> offer = new HashMap<>();
			List<ThreeTuple<String, Integer, Integer>> list = null;
			for(int i=0; i<nodes.size(); i++){
				initSubgraph(graph);
				list = new ArrayList<>();
				int totalOffer = 0;
				int totalCost = 0;
				while(true){
					ThreeTuple<String, Integer, Integer> pcf = getShortPath(serverIdx, i);
					if(pcf.second == Graph.MAX_VALUE){
						break;
					}
					totalOffer += pcf.third;
					totalCost += pcf.third * pcf.second;
					list.add(pcf);
					updateFlow(pcf.first, pcf.third, UpdateBandwidthOperator.MINUS);
				}
				totalOffers[serverIdx][i] = totalOffer;
				maxOutOffer[serverIdx][i] = Math.min(totalOffer, outOffer[i]);
				miniCost[serverIdx][i] = totalCost;
				offer.put(nodes.get(i), list);
			}
			list = new ArrayList<>();
			list.add(new ThreeTuple<>(nodes.get(serverIdx)+"", 0, Graph.MAX_VALUE));
			offer.put(nodes.get(serverIdx), list);
			totalOffers[serverIdx][serverIdx] = Graph.MAX_VALUE;
			maxOutOffer[serverIdx][serverIdx] = Math.min(Graph.MAX_VALUE, outOffer[serverIdx]);
			miniCost[serverIdx][serverIdx] = 0;
			offers.put(serverIdx, offer);
			
		}	
	}
	
	
	public void updateFlow(String path, int increment, UpdateBandwidthOperator operator){
		int src, des;
		String[] pathNodesStr = path.split(" ");
		for(int ii=0; ii<pathNodesStr.length-1; ii++){
			src = nodes.indexOf(Integer.parseInt(pathNodesStr[ii]));//起始节点
			des = nodes.indexOf(Integer.parseInt(pathNodesStr[ii+1])); //终止节点
			if(operator == UpdateBandwidthOperator.MINUS){
				flow[src][des] = flow[src][des] - increment;
			}else{
				flow[src][des] = flow[src][des] + increment;
			}
		}
	}

	
	private ThreeTuple<String, Integer, Integer> getShortPath(int src, int des){
    	if(src == des){
    		return new ThreeTuple<String, Integer, Integer>(src+"", Graph.MAX_VALUE, 0);
    	}
    	int[] costs = new int[nodes.size()];
    	int[] flows = new int[nodes.size()];
    	String[] shortPaths = new String[nodes.size()];
    	//dijkstra方法计算结果是 src 到所有顶点的最短距离
    	dijkstra(src, shortPaths, costs, flows);
    	return new ThreeTuple<String, Integer, Integer>(shortPaths[des], costs[des], flows[des]);
	}
	
	
    private void dijkstra(int src, String[] shortPaths, int[] unitCosts, int[] flows){
    	int nodesNum = this.nodes.size();
    	
    	int[][] costs = new int[nodesNum][nodesNum];
    	int[][] maxFlow = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
        		maxFlow[i][j] = flow[i][j];
    			if(this.cost[i][j] == 0 || this.flow[i][j] == 0){//没有对应的边
    				costs[i][j] = Graph.MAX_VALUE;
    			}else{
    				costs[i][j]  = this.cost[i][j];
    			}
    		}
    	}
    	
    	boolean isVisited[] = new boolean[nodesNum];//标记节点最短距离是否求出
    	
    	//初始化节点 src 到其他节点的距离为无穷大
    	for(int i=0; i<unitCosts.length; i++){
    		unitCosts[i] = Integer.MAX_VALUE;
    		shortPaths[i] = nodes.get(src)+" "+nodes.get(i);
    	}
    	
    	isVisited[src] = true;
    	unitCosts[src] = 0;
    	flows[src] = Graph.MAX_VALUE;
    	
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
					shortPaths[i] = shortPaths[nextNode] +" "+nodes.get(i);
				}
    		}
    	}
    }
	
	@Override
	public String toString(){
		StringBuffer sb = new StringBuffer();
		sb.append(client+" ");
		for(int node: nodes){
			sb.append(node+" ");
		}
		return sb.toString();
	}
	
}
