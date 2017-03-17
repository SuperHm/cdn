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
	private Set<Integer> nodes;//所有节点
	final int serverCost;
	final int linkNum;
	final int nodesNum;
	final int clientNodesNum;
	final int serverNodesNum;

	
	public Graph(int nodesNum, int clientNodesNum, int serverCost, int linkNum){
		edges = new Edge[nodesNum][nodesNum];
		clients = new Client[nodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.nodesNum = nodesNum;
		this.clientNodesNum = clientNodesNum;
		this.serverNodesNum = nodesNum-clientNodesNum;
		this.serverNodes = new HashSet<>(this.serverNodesNum);
		this.clientNodes = new HashSet<>(this.clientNodesNum);
		for(int i=0; i<this.nodesNum; i++)
			nodes.add(i);
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
	
	public void addClientNode(int node){
		clientNodes.add(node);
	}
	
	public void setServerNodes(){
		serverNodes.addAll(nodes);
		serverNodes.removeAll(clientNodes);
	}
	
	static class Edge{
		int srcNode;
		int desNode;
		int bandWidth;
		int unitCost;
		public Edge(int srcNode, int desNode, int bandWidth, int unitCost){
			this.srcNode = srcNode;
			this.desNode = desNode;
			this.bandWidth = bandWidth;
			this.unitCost = unitCost;
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
