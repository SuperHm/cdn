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

	/**
     * 你需要完成的入口
     * <功能详细描述>
     * @param graphContent 用例信息文件
     * @return [参数说明] 输出结果信息
     * @see [类、类#方法、类#成员]
     */
    public static String[] deployServer(String[] graphContent){
    	StringBuffer outStrs = new StringBuffer();
        Graph graph = null;
        graph = readProblemLine(graphContent);
    	readEdges(graphContent, graph);
    	readClients(graphContent, graph);
    	
    	List<Integer> serverNodes = graph.selectServerNodes(1);
    	int clientsNum = graph.clientNodesNum;
    	List<ThreeTuple<Integer, Integer, Integer>> clds = graph.getCLDs();
    	//将消费节点按照带宽需求排序
    	Collections.sort(clds, new Comparator<ThreeTuple<Integer, Integer, Integer>>() {
			@Override
			public int compare(ThreeTuple<Integer, Integer, Integer> o1, ThreeTuple<Integer, Integer, Integer> o2) {
				return o2.third - o1.third;
			}
		});
    	boolean[] isSatisfied = new boolean[clientsNum];
    	int loop = 100;
    	while(loop-- !=0){
    		List<ThreeTuple<String, Integer, Integer>> pcfs = new ArrayList<>();
    		Map<ThreeTuple<String, Integer, Integer>, Integer> incresements = new HashMap<>();
    		for(int i=0; i<clientsNum; i++){
        		int totalFlow = 0;
        		int demandFlow = clds.get(i).third;
        		int needFlow = 0;
        		int realFlow = 0;
        		ThreeTuple<String, Integer, Integer> pcf = null; //pcf 表示 path cost flow， 即最短路径和对应的单位带宽租用费用和最大能通过的带宽
        		while(true){
    	    		pcf = graph.getOptPCF(serverNodes, clds.get(i).second);
    	    		if(pcf.second == Graph.MAX_VALUE){
    	    			break;
    	    		}
    	    		needFlow = demandFlow - totalFlow;
        			realFlow = pcf.third >  needFlow ? needFlow : pcf.third;
    	    		graph.updateBandWidth(pcf.first, realFlow, UpdateBandwidthOperator.MINUS);
    	    		System.out.println(pcf.first + " " + clds.get(i).first + " " + realFlow);
    	    		pcfs.add(pcf);
    	    		incresements.put(pcf, realFlow);
    	    		totalFlow += realFlow;
    	    		System.out.println("need flow: "+ (demandFlow - totalFlow));
    	    		if(totalFlow == demandFlow){
    	    			isSatisfied[clds.get(i).first] = true;
    	    			break;
    	    		}
        		}
        		if(isSatisfied[clds.get(i).first]){
        			System.out.println(clds.get(i).first + " satisfied!");
        		}else{
        			System.out.println(clds.get(i).first + " unsatisfied!");
        			for(ThreeTuple<String, Integer, Integer> pcfTmp: pcfs)
        				graph.updateBandWidth(pcfTmp.first, incresements.get(pcfTmp), UpdateBandwidthOperator.PLUS);
        			break;
        		}
    		}
    		
    	}
        return new String[]{"17","\r\n","0 8 0 20"};
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
