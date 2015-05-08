package cn.rfidcn.appevent.bolt;

import static org.elasticsearch.shield.authc.support.UsernamePasswordToken.basicAuthHeaderValue;

import java.util.Calendar;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;

import org.apache.log4j.Logger;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.deletebyquery.DeleteByQueryResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.shield.authc.support.SecuredString;

import storm.trident.operation.Aggregator;
import storm.trident.operation.TridentCollector;
import storm.trident.operation.TridentOperationContext;
import storm.trident.topology.TransactionAttempt;
import storm.trident.tuple.TridentTuple;
import cn.rfidcn.appevent.utils.ConfReader;

public class AppeventJsonAggregator implements  Aggregator{
	static final Logger logger = Logger.getLogger(AppeventJsonAggregator.class);
	
	private Queue<String> appeventQueue;
	Client esClient;
	String token;
	ConfReader confReader ;

	@Override
	public void prepare(Map conf, TridentOperationContext context) {
		confReader = ConfReader.getConfReader();
		appeventQueue = new ConcurrentLinkedQueue<String>();
//		esClient = new TransportClient().addTransportAddress(new InetSocketTransportAddress(confReader.getProperty("elasticsearchIp"), 
//				Integer.parseInt(confReader.getProperty("elasticsearchPort"))));
		esClient = new TransportClient(ImmutableSettings.builder()
			    .put("cluster.name", "elasticsearch")
			    .put("shield.user", confReader.getProperty("elasticsearchUsername")+":"+confReader.getProperty("elasticsearchPassword")).build())
		.addTransportAddress(new InetSocketTransportAddress(confReader.getProperty("elasticsearchIp"), 9300));	
		token = basicAuthHeaderValue(confReader.getProperty("elasticsearchUsername"), new SecuredString(confReader.getProperty("elasticsearchPassword").toCharArray()));
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
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, -7);
		try {
			//System.out.println("clear old log: "+esClient.prepareCount(confReader.getProperty("elasticsearchIndex")).setQuery(QueryBuilders.rangeQuery("ts").lt(calendar.getTime().getTime())).execute().get().getCount());
			esClient.prepareDeleteByQuery(confReader.getProperty("elasticsearchIndex")).setQuery(QueryBuilders.rangeQuery("ts").lt(calendar.getTime().getTime()))
				.execute().get();
		} catch (InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(appeventQueue.size()==0){
			return;
		}else{
			logger.info("persist to elastic search, # of records: " + appeventQueue.size());
			BulkRequestBuilder bb = esClient.prepareBulk().putHeader("Authorization", token);
			for(String app: appeventQueue) {
				bb.add(esClient.prepareIndex(confReader.getProperty("elasticsearchIndex"), confReader.getProperty("elasticsearchType")).setSource(app));
			}
		
			bb.execute(new ActionListener<BulkResponse>(){
				@Override
				public void onResponse(BulkResponse response) {	
					if(response.hasFailures()){
						System.out.println(response.buildFailureMessage());
					}
				}

				@Override
				public void onFailure(Throwable e) {
					logger.error("insert into elasticsearch error",e);
				}	
			});
			appeventQueue.clear();
		}
	}
	
	
}
