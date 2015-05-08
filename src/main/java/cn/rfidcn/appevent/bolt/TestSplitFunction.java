package cn.rfidcn.appevent.bolt;

import java.util.Date;

import com.alibaba.fastjson.JSON;

import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;
import backtype.storm.tuple.Values;
import cn.rfidcn.appevent.model.AppSysLogEvent;

public class TestSplitFunction  extends BaseFunction {

	@Override
	public void execute(TridentTuple tuple, TridentCollector collector) {
		//lv,geoip,oip
		String s = tuple.getString(0).trim();
		AppSysLogEvent event = new AppSysLogEvent();
		String[] ss = s.split(";");
		event.setLv(Short.parseShort(ss[0]));
		event.setTs(new Date());
		event.setGeoip(ss[1]);
		event.setOip(ss[2]);
		String json = JSON.toJSONString(event);
		collector.emit(new Values(json));
	}
	

}
