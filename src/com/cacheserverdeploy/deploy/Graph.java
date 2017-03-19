package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import com.sun.corba.se.impl.encoding.OSFCodeSetRegistry.Entry;

import java.util.Map;

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
	
	final static int MAX_WEIGHT = 100000;

	private int[] serverNodes;//服务节点
	private int[] clientNodes;//消费节点
	private int[][] bandWidths;
	private int[][] unitCosts;
	private int[] demands;
	private int[] linkedNodes;
	final int serverCost;
	final int linkNum;
	final int clientNodesNum;
	final int serverNodesNum;
	private Map<Integer, Integer> frequency;

	
	public Graph(int serverNodesNum, int clientNodesNum, int serverCost, int linkNum){
		this.serverNodes = new int[serverNodesNum];
		this.clientNodes = new int[clientNodesNum];
		this.bandWidths = new int[serverNodesNum][serverNodesNum];
		this.unitCosts = new int[serverNodesNum][serverNodesNum];
		this.demands = new int[clientNodesNum];
		this.linkedNodes = new int[clientNodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientNodesNum = clientNodesNum;
		this.serverNodesNum = serverNodesNum;
		this.frequency = new LinkedHashMap<>();
		for(int i=0; i<this.serverNodesNum; i++)
			serverNodes[i] = i;
		for(int i=0; i<this.clientNodesNum; i++)
			clientNodes[i] = i;
	}
	
	
	public void addEdge(int src, int des, int bandWidth, int unitCost){
		this.bandWidths[src][des] = bandWidth;
		this.bandWidths[des][src] = bandWidth;
		this.unitCosts[src][des] = unitCost;
		this.unitCosts[des][src] = unitCost;
		
	}
	
	public void addClient(int node, int linkedNode, int demand){
		this.linkedNodes[node] = linkedNode;
		this.demands[node] = demand;
	}
	
	public int[] getServerNodes() {
		return serverNodes;
	}

	public int[] getClientNodes() {
		return clientNodes;
	}

	public int[][] getUnitCosts() {
		return unitCosts;
	}

	public int[] getDemands() {
		return demands;
	}

	public int[] getLinkedNodes() {
		return linkedNodes;
	}

	public int[][] getBandWidths(){
		return this.bandWidths;
	}
	
	
	public void updateBandwidth(int src, int des, int minusValue){
		this.bandWidths[src][des] = this.bandWidths[src][des] - minusValue;
	}
    /**
     * 按一定策略选择初始服务器节点
     * 
     * @param serverNum 服务器节点个数
     * @return 选择的服务器节点
     */
    public int[] selectServerNodes(int initServerNum){
    	this.setFrequency();
    	int[] selectedServerNodes = new int[initServerNum];
    	List<Map.Entry<Integer, Integer>> entrys = new ArrayList<>(frequency.entrySet());
    	for(int i=0; i<initServerNum; i++){
    		selectedServerNodes[i] = entrys.get(i).getKey();
    	}
    	return selectedServerNodes;
    }
    
    /**
     * 设置 frequency，并使其按照 value 值排序
     */
    public void setFrequency(){
    	Map<Integer, Integer> unSortFrequency = calculateFrequency();
    	this.frequency = sortByValue(unSortFrequency);
    }
    
    public Map<Integer, Integer> getFrequency() {
		return frequency;
	}
    
	/**
     * 统计出现在最短路径中的节点的频次
     */
    private Map<Integer, Integer> calculateFrequency(){
    	//初始化设置为0
    	Map<Integer, Integer> fre = new HashMap<>();
    	for(int serverNode: serverNodes)
    		fre.put(serverNode, 0);
    	int[] unitCosts = new int[linkedNodes.length];
    	int[] flows = new int[linkedNodes.length];
		String[] shortPaths = new String[linkedNodes.length];
    	for(int linkedNode:this.linkedNodes){
    		getShortPath(linkedNode, linkedNodes, shortPaths, unitCosts, flows);
    		for(String shortPath: shortPaths){
    			String[] nodesStr = shortPath.split(" ");
    			for(String nodeStr:nodesStr){
    				int node = Integer.parseInt(nodeStr);
    				fre.put(node, fre.get(node) + 1);
    			}
    		}
    	}
    	return fre;
    }

    
    /**
     * 使用 dijkstra 算法寻找图中节点 src 到节点 des 的最小租用费用路径
     * 
     * @param src 开始节点
     * @param des 终止节点集合
     * @param shortPath 路径信息
     * @param dis 最小单位租用费用
     */
    public boolean getShortPath(int src, int[] des, String[] shortPath, int[] dis, int[] flow){
    	int[] disTmp = new int[serverNodesNum];
    	int[] flowTmp = new int[serverNodesNum];
    	String[] shortPathTmp = new String[serverNodesNum];
    	dijkstra(src, shortPathTmp, disTmp, flowTmp);
    	for(int i=0; i<des.length; i++){
    		dis[i] = disTmp[des[i]];
    		shortPath[i] = shortPathTmp[des[i]];
    		flow[i] = flowTmp[des[i]];
    	}
    	for(int d: dis)
    		if(d == Integer.MAX_VALUE)
    			return false;
    	return true;
    }
    
    private void dijkstra(int src, String[] shortPaths, int[] dises, int[] flows){
    	int nodesNum = this.serverNodesNum;
    	int[][] edgesWeight = new int[nodesNum][nodesNum];
    	int[][] maxFlow = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
    			maxFlow[i][j] = bandWidths[i][j];
    			if(unitCosts[i][j] == 0){//没有对应的边
    				edgesWeight[i][j] = MAX_WEIGHT;
    			}else{
    				edgesWeight[i][j]  = unitCosts[i][j];
    			}
    		}
    	}
    	
    	boolean isVisited[] = new boolean[nodesNum];//标记节点最短距离是否求出
    	
    	//初始化节点 src 到其他节点的距离为无穷大
    	for(int i=0; i<dises.length; i++){
    		dises[i] = Integer.MAX_VALUE;
    		shortPaths[i] = src+" "+i;
    	}
    	
    	isVisited[src] = true;
    	dises[src] = 0;
    	flows[src] = 0;
    	
    	for(int count=1; count<nodesNum; count++){
    		int minDis = Integer.MAX_VALUE;
    		int nextNode = -1;
    		int tmp = 0;
    		for(int i=0; i<nodesNum; i++){
				if(! isVisited[i] && 
						(edgesWeight[src][i] < minDis || (edgesWeight[src][i] == minDis && maxFlow[src][i] > tmp))){
					minDis = edgesWeight[src][i];
					nextNode = i;
					tmp = maxFlow[src][i];
				}
    		}
    		dises[nextNode] = minDis;
    		flows[nextNode] = tmp;
    		isVisited[nextNode] = true;
    		//松弛操作
    		for(int i=0; i<nodesNum; i++){
				if(!isVisited[i] && edgesWeight[src][nextNode]+edgesWeight[nextNode][i]< edgesWeight[src][i]){
					edgesWeight[src][i] = edgesWeight[src][nextNode] + edgesWeight[nextNode][i];
					maxFlow[src][i] = Math.min(maxFlow[src][nextNode], maxFlow[nextNode][i]);
					shortPaths[i] = shortPaths[nextNode] +" "+i;
				}
    		}
    	}
    }
    
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o2.getValue()).compareTo(o1.getValue());
            }
        });

        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
}
