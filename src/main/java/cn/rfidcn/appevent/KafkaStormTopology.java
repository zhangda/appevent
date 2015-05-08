package cn.rfidcn.appevent;

import org.apache.log4j.Logger;

import storm.kafka.BrokerHosts;
import storm.kafka.ZkHosts;
import storm.kafka.trident.TransactionalTridentKafkaSpout;
import storm.kafka.trident.TridentKafkaConfig;
import storm.trident.TridentTopology;
import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.tuple.Fields;
import cn.rfidcn.appevent.bolt.AppeventJsonAggregator;
import cn.rfidcn.appevent.bolt.AppeventObjSplitFunction;
import cn.rfidcn.appevent.scheme.AvroScheme;
import cn.rfidcn.appevent.utils.ConfReader;

public class KafkaStormTopology {
	
	static final Logger logger = Logger.getLogger(KafkaStormTopology.class);
	
	public static void main(String args[]) {
		
		ConfReader confReader = ConfReader.getConfReader();
		
		BrokerHosts zk = new ZkHosts(confReader.getProperty("zkHosts"));	 
		Config conf = new Config(); 
		conf.put(Config.TOPOLOGY_TRIDENT_BATCH_EMIT_INTERVAL_MILLIS, Integer.parseInt(confReader.getProperty("emitTimeInt")));
		conf.put(Config.TOPOLOGY_WORKERS, Integer.parseInt(confReader.getProperty("num_workers")));
		conf.put(Config.TOPOLOGY_ACKER_EXECUTORS, Integer.parseInt(confReader.getProperty("num_workers")));
		
        TridentTopology topology = new TridentTopology();
        
        TridentKafkaConfig appSpoutConf = new TridentKafkaConfig(zk, confReader.getProperty("appeventTopic"));
        appSpoutConf.fetchSizeBytes = 5 * 1024 * 1024;
        appSpoutConf.bufferSizeBytes = 5 * 1024 * 1024;
        appSpoutConf.scheme = new SchemeAsMultiScheme(new AvroScheme());
//        appSpoutConf.scheme = new SchemeAsMultiScheme(new StringScheme());
        TransactionalTridentKafkaSpout appSpout = new TransactionalTridentKafkaSpout(appSpoutConf);
        topology.newStream("appspout", appSpout).parallelismHint(Integer.parseInt(confReader.getProperty("num_spouts"))).shuffle()
        .each(new Fields("datalist"), new AppeventObjSplitFunction(), new Fields("json")).parallelismHint(Integer.parseInt(confReader.getProperty("num_bolts")))
//         .each(new Fields("str"), new TestSplitFunction(), new Fields("json")).parallelismHint(Integer.parseInt(confReader.getProperty("num_bolts")))
        .partitionAggregate(new Fields("json"), new AppeventJsonAggregator(), new Fields());
          
        try {
			StormSubmitter.submitTopology("appevent", conf, topology.build());
		} catch (AlreadyAliveException e) {
			logger.error("AlreadyAliveException", e);
		} catch (InvalidTopologyException e) {
			logger.error("InvalidTopologyException", e);
		}
        
	}
	

}
