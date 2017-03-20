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

import com.sun.org.apache.bcel.internal.generic.NEW;

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
	enum UpdateBandwidthOperator{
	    	MINUS, PLUS;
	}
	final static int MAX_VALUE = 100000;

	private int[] serverNodes;//服务节点
	private int[][] bandWidths;
	private int[][] unitCosts;;
	List<ThreeTuple<Integer, Integer, Integer>> clds;//client linkedNode demand表示消费节点集合
	final int serverCost;
	final int linkNum;
	final int clientNodesNum;
	final int serverNodesNum;
	private List<Map.Entry<Integer, Integer>> frequency;

	
	public Graph(int serverNodesNum, int clientNodesNum, int serverCost, int linkNum){
		this.serverNodes = new int[serverNodesNum];
		this.clds = new ArrayList<>(clientNodesNum);
		this.bandWidths = new int[serverNodesNum][serverNodesNum];
		this.unitCosts = new int[serverNodesNum][serverNodesNum];
		this.serverCost = serverCost;
		this.linkNum = linkNum;
		this.clientNodesNum = clientNodesNum;
		this.serverNodesNum = serverNodesNum;
		this.frequency = new ArrayList<>();
		for(int i=0; i<this.serverNodesNum; i++){
			serverNodes[i] = i;
			bandWidths[i][i] = MAX_VALUE;
		}
		
	}
	
	
	public void addEdge(int src, int des, int bandWidth, int unitCost){
		this.bandWidths[src][des] = bandWidth;
		this.bandWidths[des][src] = bandWidth;
		this.unitCosts[src][des] = unitCost;
		this.unitCosts[des][src] = unitCost;
		
	}
	
	public void addClient(int node, int linkedNode, int demand){
		clds.add(new ThreeTuple<>(node, linkedNode, demand));
	}
	
	public int[] getServerNodes() {
		return serverNodes;
	}

	public List<ThreeTuple<Integer, Integer, Integer>> getCLDs() {
		return clds;
	}

	public int[][] getUnitCosts() {
		return unitCosts;
	}


	public int[][] getBandWidths(){
		return this.bandWidths;
	}

	
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
    /**
     * 按一定策略选择初始服务器节点
     * 
     * @param serverNum 服务器节点个数
     * @return 选择的服务器节点
     */
    public List<Integer> selectServerNodes(int initServerNum){
    	List<Integer> selectedServerNodes = new ArrayList<>();
    	this.frequency = new ArrayList<>(calculateFrequency().entrySet());
    
    	Collections.sort(this.frequency, new Comparator<Map.Entry<Integer, Integer>>() {
			@Override
			public int compare(Entry<Integer, Integer> o1, Entry<Integer, Integer> o2) {
				// TODO Auto-generated method stub
				return o2.getValue()-o1.getValue();
			}
		});
    	for(int i=0; i<initServerNum; i++){
    		selectedServerNodes.add(this.frequency.get(i).getKey());
    	}
    	return selectedServerNodes;
    }
    
    public List<Map.Entry<Integer, Integer>> getFrequency() {
		return frequency;
	}
    
	/**
     * 统计出现在最短路径中的节点的频次
     */
    private Map<Integer, Integer> calculateFrequency(){
    	Map<Integer, Integer> fre = new HashMap<>();
//    	List<ThreeTuple<String, Integer, Integer>> pathCostFlows = new ArrayList<>();
    	//初始化设置为0
    	for(int serverNode: serverNodes)
    		fre.put(serverNode, 0);
    	for(int i=0; i<clientNodesNum; i++){
    		for(int j = 0; j<clientNodesNum; j++){
    			ThreeTuple<String, Integer, Integer>  pathCostFlow = getShortPath(clds.get(i).second, clds.get(j).second);
    			String[] nodesStr = pathCostFlow.first.split(" ");
//    			System.out.println(pathCostFlow.first);
    			for(String nodeStr:nodesStr){
    				int node = Integer.parseInt(nodeStr);
    				fre.put(node, fre.get(node) + 1);
    			}
//    			updateBandWidth(pathCostFlow.first, pathCostFlow.third, UpdateBandwidthOperator.MINUS);
//    			pathCostFlows.add(pathCostFlow);
    		}
//    		for(ThreeTuple<String, Integer, Integer> pcf: pathCostFlows)
//    			updateBandWidth(pcf.first, pcf.third, UpdateBandwidthOperator.PLUS);
//    		pathCostFlows.clear();
    	}
    	return fre;
    }

    /**
     * 根据当前服务器节点，选择一条最优路径
     * @param serverNodes
     * @param linkedNode
     * @return
     */
    public ThreeTuple<String, Integer, Integer> getOptPCF(List<Integer> serverNodes, int linkedNode){
    	List<ThreeTuple<String, Integer, Integer>> pcfs = new ArrayList<>();
		ThreeTuple<String, Integer, Integer> pcf = null;
		for(int serverIdx=0; serverIdx<serverNodes.size(); serverIdx++){
			pcf = getShortPath(serverNodes.get(serverIdx), linkedNode);
			pcfs.add(pcf);
//			System.out.println(pcf);
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
    public ThreeTuple<String, Integer, Integer> getShortPath(int src, int des){
    	if(src == des){
    		return new ThreeTuple<String, Integer, Integer>(src+"", 0, MAX_VALUE);
    	}
    	int[] disTmp = new int[serverNodesNum];
    	int[] flowTmp = new int[serverNodesNum];
    	String[] shortPathTmp = new String[serverNodesNum];
    	dijkstra(src, shortPathTmp, disTmp, flowTmp);
    	return new ThreeTuple<String, Integer, Integer>(shortPathTmp[des], disTmp[des], flowTmp[des]);
    }
    
    private void dijkstra(int src, String[] shortPaths, int[] dises, int[] flows){
    	int nodesNum = this.serverNodesNum;
    	int[][] edgesWeight = new int[nodesNum][nodesNum];
    	int[][] maxFlow = new int[nodesNum][nodesNum];
    	//初始化图中单位租用费用信息和最大流量信息。不存在的链路单位租用费用设置为最大值，最大流量设置为0
    	for(int i=0; i<nodesNum; i++){
    		for(int j=0; j<nodesNum; j++){
        		maxFlow[i][j] = bandWidths[i][j];
    			if(unitCosts[i][j] == 0 || bandWidths[i][j] == 0){//没有对应的边
    				edgesWeight[i][j] = MAX_VALUE;
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
    	flows[src] = MAX_VALUE;
    	
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
}
class  ThreeTuple<A, B extends Comparable<? super B>, C extends Comparable<? super C>>{
	final A first;
	final B second;
	final C third;
	public ThreeTuple(A first, B second, C third){
		this.first = first;
		this.second = second;
		this.third = third;
	}
	
	@Override
	public String toString(){
		return first + " " +second + " " + third;
	}
}
