package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.sun.jndi.url.iiopname.iiopnameURLContextFactory;

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
	private  List<Edge> edges;
	private  List<Client> clients;
	final int serverCost;
	final int linkNum;
	final int serverNodesNum;
	final int clientNodesNum;
	
	public Graph(int serverNodesNum, int clientNodesNum, int serverCost, int linkNum){
		edges = new ArrayList<>(linkNum);
		clients = new ArrayList<>(clientNodesNum);
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.serverNodesNum = serverNodesNum;
		this.clientNodesNum = clientNodesNum;
	}
	
	
	public void addEdge(Edge edge){
		edges.add(edge);
	}
	
	public void addClient(Client client){
		clients.add(client);
	}
	
	public List<Edge> getEdges(){
		return edges;
	}
	
	public List<Client> getClients(){
		return clients;
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
