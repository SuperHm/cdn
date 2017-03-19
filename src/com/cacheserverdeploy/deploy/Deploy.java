package com.cacheserverdeploy.deploy;

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
    	
    	int[] serverNodes = graph.selectServerNodes(2);
    	int clientsNum = graph.clientNodesNum;
    	int[] linkedNodes = graph.getLinkedNodes();
    	boolean[] isSatisfied = new boolean[clientsNum];
    	for(int clientIndex=0; clientIndex<clientsNum; clientIndex++){
    		int[] flows = new int[serverNodes.length];
    		int[] unitCosts = new int[serverNodes.length];
    		String[] shortPaths = new String[serverNodes.length];
    		int totalFlow = 0;
    		for(int serverIndex=0; serverIndex<serverNodes.length; serverIndex++){
	    		boolean hasShortPath;
	       		int[] flowTmp = new int[1];
	    		int[] unitCostTmp = new int[1];
	    		String[] shortPathTmp = new String[1];
	    		hasShortPath = graph.getShortPath(serverNodes[serverIndex], new int[]{linkedNodes[clientIndex]}, shortPathTmp, unitCostTmp, flowTmp);
	    		if(hasShortPath){
	    			flows[serverIndex] = flowTmp[0];
	    			unitCosts[serverIndex] = unitCostTmp[0];
	    			shortPaths[serverIndex] = shortPathTmp[0];
	    			totalFlow += flows[serverIndex];
	    			//节点 clientIndex 已满足带宽需求
	    			if(totalFlow > graph.getDemands()[clientIndex]){
	    				
	    			}
	    		}
	 			String[] pathNodesStr = shortPathTmp[0].split(" ");
    			//更新网络中边的带宽
    			for(int ii=0; ii<pathNodesStr.length; ii++){
    				for(int jj=ii+1; jj<pathNodesStr.length; jj++){
    					graph.updateBandwidth(Integer.parseInt(pathNodesStr[ii]), //起始节点
    							Integer.parseInt(pathNodesStr[jj]), //终止节点
    							flowTmp[0]);//已占用的带宽
    				}
    			}
	    		System.out.println(000);
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
