package cn.rfidcn.appevent;

import org.apache.log4j.Logger;

import storm.kafka.BrokerHosts;
import storm.kafka.ZkHosts;
import storm.kafka.trident.TransactionalTridentKafkaSpout;
import storm.kafka.trident.TridentKafkaConfig;
import storm.trident.TridentTopology;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.generated.AlreadyAliveException;
import backtype.storm.generated.InvalidTopologyException;
import backtype.storm.spout.SchemeAsMultiScheme;
import backtype.storm.tuple.Fields;
import cn.rfidcn.appevent.bolt.AppeventJsonAggregator;
import cn.rfidcn.appevent.bolt.AppeventObjSplitFunction;
import cn.rfidcn.appevent.scheme.AvroScheme;
import cn.rfidcn.appevent.utils.ConfigReader;

public class KafkaStormTopology {
	
	static final Logger logger = Logger.getLogger(KafkaStormTopology.class);
	
	public static void main(String args[]) {
		
		BrokerHosts zk = new ZkHosts(ConfigReader.getProperty("zkHosts"));	 
		Config conf = new Config(); 
		conf.put(Config.TOPOLOGY_TRIDENT_BATCH_EMIT_INTERVAL_MILLIS, Integer.parseInt(ConfigReader.getProperty("emitTimeInt")));
	 
        TridentTopology topology = new TridentTopology();
        
        TridentKafkaConfig appSpoutConf = new TridentKafkaConfig(zk, ConfigReader.getProperty("appeventTopic"));
        appSpoutConf.scheme = new SchemeAsMultiScheme(new AvroScheme());
        TransactionalTridentKafkaSpout appSpout = new TransactionalTridentKafkaSpout(appSpoutConf);
        topology.newStream("appspout", appSpout).shuffle()
        .each(new Fields("datalist"), new AppeventObjSplitFunction(), new Fields("json"))
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
