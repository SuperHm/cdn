package com.cacheserverdeploy.deploy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cacheserverdeploy.deploy.Graph;
import com.cacheserverdeploy.deploy.Graph.UpdateBandwidthOperator;

public class Deploy{
	private static int lineNum = 0;//网络图读取过程中标记行数
	final  static long TIME_LIMIT = 90*1000;
	/**
     * 你需要完成的入口
     * <功能详细描述>
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */

	public static String[] deployServer(String[] graphContent){
    	long startTime = System.currentTimeMillis();
    	List<String> outStrs = new ArrayList<>();
    	List<String> outStrsTmp = null;
    	int totalCostTmp = 0;
    	int totalCost = Integer.MAX_VALUE;
        Graph graph = null;
        graph = readProblemLine(graphContent);
    	readEdges(graphContent, graph);
    	readClients(graphContent, graph);
    	graph.initFrequency();
    	int serverNodesNum = 1;//初始服务器个数设置为1，可根据问题规模动态调整
    	int clientsNum = graph.clientNodesNum;
    	int satisfiedClientNum = 0;
    	List<ThreeTuple<String, Integer, Integer>> pcfs = new ArrayList<>();
		Map<ThreeTuple<String, Integer, Integer>, Integer> incresements = new HashMap<>();
    	List<ThreeTuple<Integer, Integer, Integer>> clds = graph.getCLDs();//cld 表示 client linkedNode demand，及消费节点client 相连的服务器节点 带宽需求
    	boolean isSolved = false;
    	int satServerNodeNum = 0;
    	while(true){
	    	//将消费节点按照带宽需求排序
	    	Collections.sort(clds, new Comparator<ThreeTuple<Integer, Integer, Integer>>() {
				@Override
				public int compare(ThreeTuple<Integer, Integer, Integer> o1, ThreeTuple<Integer, Integer, Integer> o2) {
					return o2.third - o1.third;
				}
			});

	    	int nobetter = 0;
	    	int unsat = 0;
	    	while(true){
		    	List<Integer> serverNodes = graph.selectServerNodes(serverNodesNum);
	    		//重新计算新的服务器部署方案，将所有存储信息恢复默认值
		    	totalCostTmp = 0;
	    	  	boolean[] isSatisfied = new boolean[clientsNum];
	    	  	pcfs.clear();
	    	  	outStrsTmp = new ArrayList<>();
	    		satisfiedClientNum = 0;
	    		incresements.clear();
	    		//加上服务器的部署成本
	    		totalCostTmp += serverNodes.size() * graph.serverCost;
	    		
	    		for(int i=0; i<clientsNum; i++){
	        		int totalFlow = 0; //已获得的带宽
	           		int demandFlow = clds.get(i).third;//当前消费节点的需求带宽
	        		int needFlow = 0;//仍需要的带宽
	        		int realFlow = 0;//路径上真实传输的带宽
	        		ThreeTuple<String, Integer, Integer> pcf = null; //pcf 表示 path cost flow， 即最短路径和对应的单位带宽租用费用和最大能通过的带宽
	        		while(true){
	        			//返回当前所有服务器到消费节点 clds.get(i).second 最优的一条路径
	    	    		pcf = graph.getOptPCF(serverNodes, clds.get(i).second);
	    	    		if(pcf.second == Graph.MAX_VALUE){
	    	    			break;
	    	    		}
	    	    		needFlow = demandFlow - totalFlow;
	        			realFlow = pcf.third >  needFlow ? needFlow : pcf.third;
	    	    		graph.updateBandWidth(pcf.first, realFlow, UpdateBandwidthOperator.MINUS);
	    	    		totalCostTmp += pcf.second * realFlow;
//	    	    		System.out.println(pcf.first +" "+clds.get(i).first+ " " + realFlow);
	    	    		outStrsTmp.add("\r\n"+pcf.first +" "+clds.get(i).first+ " " + realFlow);
	    	    		pcfs.add(pcf);
	    	    		incresements.put(pcf, realFlow);
	    	    		totalFlow += realFlow;
	    	    		if(totalFlow == demandFlow){
	    	    			isSatisfied[clds.get(i).first] = true;
	    	    			break;
	    	    		}
	        		}
	        		if(isSatisfied[clds.get(i).first]){
	        			satisfiedClientNum++;
	        		}else{
	        			//当前服务器分配方案不能满足，根据结果跟新每个服务器节点的代价信息
	        			for(int serverNode: serverNodes){
	        				ThreeTuple<String, Integer, Integer> pcfClientToServer = graph.getShortPath(clds.get(i).second, serverNode);
	        				graph.updateFrequency(pcfClientToServer.first);
	        			}
	        			//恢复每条边上占用的带宽
	        			for(ThreeTuple<String, Integer, Integer> pcfTmp: pcfs)
	        				graph.updateBandWidth(pcfTmp.first, incresements.get(pcfTmp), UpdateBandwidthOperator.PLUS);
	        			break;
	        		}
	    		}
	    		if(satisfiedClientNum == clientsNum){
	    			isSolved = true;
	    			for(int node: serverNodes)
	    				
//	    			System.out.println(serverNodes);
//	    			System.out.println("sat");
	    			System.out.println(totalCostTmp);
	    			if(totalCostTmp < totalCost){
	    				satServerNodeNum = serverNodesNum;
	    				totalCost = totalCostTmp;
	    				outStrs = new ArrayList<>(outStrsTmp);
	    			}else{
	    				nobetter++;
		    			if(nobetter > serverNodesNum * 5000)
		    				break;
	    			}
	    		}else{
//	    			System.out.println(serverNodes);
//	    			System.out.println("unsat");
	    			unsat++;
	    			if(unsat > serverNodesNum * 1000)
	    				break;
	    		}
	    	}
	    	serverNodesNum++;
	    	System.out.println(serverNodesNum);
	    	if((satServerNodeNum!=0 && ((serverNodesNum-satServerNodeNum)>(double)clientsNum/5.0))
	    			|| System.currentTimeMillis() - startTime > TIME_LIMIT)
	    		break;
    	}
    	
    	if(isSolved){
    		StringBuffer sb = new StringBuffer();
    		for(String s: outStrs)
    			sb.append(s);
    		return new String[]{outStrs.size()+"", sb.toString()};
    	}else{
    		return new String[]{"NA"};
    	}
    }
    

    
    /**
     * 读取网络节点数量、消费节点数量、服务器部署成本、网络链路数量。基于此信息初始化 Graph
     * 
     * @param graphContent
     * @return 返回初始化之后的Graph对象
     */
    public static Graph readProblemLine(String[] graphContent){
    	String problemLine = graphContent[lineNum++];
    	String[] problemLineStrs = problemLine.split(" ");
    	lineNum++; //跳过空行
    	String costLine = graphContent[lineNum++];
    	return new Graph(Integer.parseInt(problemLineStrs[0]), // 网络节点数量
    			Integer.parseInt(problemLineStrs[2]), //消费节点数量
    			Integer.parseInt(costLine), //服务器部署成本
    			Integer.parseInt(problemLineStrs[1])); //网络链路数量
    }
    
    /**
     * 逐行读取链路信息
     * 
     * @param graphContent
     * @param graph
     */
    public static void readEdges(String[] graphContent, Graph graph){
    	String line = null;
    	lineNum++;//跳过空行
    	while(! (line = graphContent[lineNum++]).equals("")){
    		String[] lineStrs = line.split(" ");
    		graph.addEdge(Integer.parseInt(lineStrs[0]),//起始节点
    				Integer.parseInt(lineStrs[1]),//终止节点
    				Integer.parseInt(lineStrs[2]), //链路总带宽
    				Integer.parseInt(lineStrs[3]));//单位带宽租用费用    		
    	}
    }
    
    /**
     * 逐行读取消费节点信息
     * 
     * @param graphContent
     * @param graph
     */
    public static void readClients(String[] graphContent, Graph graph){
    	String line = null;
    	while(lineNum < graphContent.length){
    		line = graphContent[lineNum++];
    		String[] lineStrs = line.split(" ");
    		graph.addClient(Integer.parseInt(lineStrs[0]),//消费节点
    				Integer.parseInt(lineStrs[1]), //相邻的网络节点
    				Integer.parseInt(lineStrs[2]));//带宽需求
    	}
    }
    
  
   
}
