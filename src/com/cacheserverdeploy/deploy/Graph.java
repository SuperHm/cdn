package com.cacheserverdeploy.deploy;


import java.util.HashSet;
import java.util.Set;

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
	private Edge[][] edges;
	private  Client[] clients;
	private Set<Integer> serverNodes;//服务节点
	private Set<Integer> clientNodes;//消费节点
	final int serverCost;
	final int linkNum;
	final int clientNodesNum;
	final int serverNodesNum;

	
	public Graph(int serverNodesNum, int clientNodesNum, int serverCost, int linkNum){
		edges = new Edge[serverNodesNum][serverNodesNum];
		clients = new Client[clientNodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientNodesNum = clientNodesNum;
		this.serverNodesNum = serverNodesNum;
		this.serverNodes = new HashSet<>(this.serverNodesNum);
		this.clientNodes = new HashSet<>(this.clientNodesNum);
		for(int i=0; i<this.serverNodesNum; i++)
			serverNodes.add(i);
		for(int i=0; i<this.clientNodesNum; i++)
			clientNodes.add(i);
	}
	
	
	public void addEdge(int src, int des, Edge edge){
		edges[src][des] = edge;
	}
	
	public void addClient(int node, Client client){
		clients[node] = client;
	}
	
	public Edge[][] getEdges(){
		return edges;
	}
	
	public Client[] getClients(){
		return clients;
	}
	
	public Set<Integer> getServerNodes(){
		return serverNodes;
	}

	static class Edge{
		int srcNode;
		int desNode;
		int bandWidth;
		int unitCost;
		int weight;
		public Edge(int srcNode, int desNode, int bandWidth, int unitCost){
			this.srcNode = srcNode;
			this.desNode = desNode;
			this.bandWidth = bandWidth;
			this.unitCost = unitCost;
			this.weight = unitCost;
		}
	}
	
	static class Client{
		int node;
		int neighborNode;
		int demand;
		public Client(int node, int neighborNode, int demand){
			this.node = node;
			this.neighborNode = neighborNode;
			this.demand = demand;
		}
	}
}
