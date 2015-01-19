package cn.rfidcn.appevent.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;

public class ConfigReader {
	
	static final Logger logger = Logger.getLogger(ConfigReader.class);
	static Properties p;
	
	static void load(){
		InputStream in = ClassLoader.getSystemResourceAsStream("config.properties");
		p = new Properties();      
		try {
			p.load(in);
		} catch (IOException e) {
			logger.error("IOException", e);
		}
	}
	
	public static String getProperty(String key){
		if(p==null) load();
		return p.getProperty(key);
	}

}
