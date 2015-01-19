package cn.rfidcn.appevent.bolt;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;

import storm.trident.operation.Aggregator;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.topology.TransactionAttempt;
import storm.trident.tuple.TridentTuple;
import backtype.storm.topology.FailedException;
import cn.rfidcn.appevent.utils.ConfigReader;

public class AppeventJsonAggregator implements  Aggregator{
	static final Logger logger = Logger.getLogger(AppeventJsonAggregator.class);
	
	private Queue<String> appeventQueue = new ConcurrentLinkedQueue<String>();
	Client esClient;

	@Override
	public void prepare(Map conf, TridentOperationContext context) {
		esClient = new TransportClient().addTransportAddress(new InetSocketTransportAddress(ConfigReader.getProperty("elasticsearchIp"), 
				Integer.parseInt(ConfigReader.getProperty("elasticsearchPort"))));
	}

	@Override
	public void cleanup() {
	}

	@Override
	public Object init(Object batchId, TridentCollector collector) {
		return ((TransactionAttempt) batchId).getTransactionId();
	}

	@Override
	public void aggregate(Object val, TridentTuple tuple, TridentCollector collector) {
		String app = (String) tuple.get(0);
		//app.setTxId((long) val);
		appeventQueue.add(app);
	}

	@Override
	public void complete(Object val, TridentCollector collector) {
		if(appeventQueue.size()==0) 
			return;
		logger.info("persist to elastic search, # of records: " + appeventQueue.size());
		BulkRequestBuilder bb = esClient.prepareBulk();
		for(String app: appeventQueue) {
			bb.add(esClient.prepareIndex(ConfigReader.getProperty("elasticsearchIndex"), ConfigReader.getProperty("elasticsearchType")).setSource(app));
		}
		bb.execute(new ActionListener(){
			@Override
			public void onResponse(Object response) {	
			}

			@Override
			public void onFailure(Throwable e) {
				logger.error("insert into elasticsearch error",e);
				//throw new FailedException();
			}	
		});
		appeventQueue.clear();
	}
}
