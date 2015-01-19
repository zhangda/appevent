package cn.rfidcn.appevent.bolt;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.log4j.Logger;

import storm.trident.operation.BaseFunction;
import storm.trident.operation.TridentCollector;
import storm.trident.tuple.TridentTuple;
import backtype.storm.tuple.Values;
import cn.rfidcn.appevent.model.ManagementEvent;

import com.alibaba.fastjson.JSON;

public class AppeventObjSplitFunction extends BaseFunction{

	static final Logger logger = Logger.getLogger(AppeventObjSplitFunction.class);
	
	@Override
	public void execute(TridentTuple tuple, TridentCollector collector) {

	List<Object> appJsonList = (List)tuple.get(0);
		for(Object obj : appJsonList){
			ManagementEvent event = new ManagementEvent();
			try {
				BeanUtils.copyProperties(event,obj);
			} catch (IllegalAccessException | InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String json = JSON.toJSONString(obj);
			collector.emit(new Values(json));
		}	
	}

}
