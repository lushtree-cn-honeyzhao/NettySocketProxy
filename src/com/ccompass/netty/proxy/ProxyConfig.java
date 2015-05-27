package com.ccompass.netty.proxy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ProxyConfig {
	public String proxyIP;
	public int proxyPort;
	public String mainIP;
	public int mainPort;
	public long checktimes;
	public int branchNumbers;
	public List<String> branchList = new ArrayList<String>();

	public static ProxyConfig config = new ProxyConfig();

	public static void loadConfig() throws IOException {
		int errorNum = 0;
		Properties configProp = new Properties();
		/*FileInputStream fis = new FileInputStream("proxyConfig.properties");
		configProp.load(fis);
		fis.close();*/
		String aaaa=ProxyConfig.class.getProtectionDomain().getCodeSource().getLocation().getFile();		
		String b=aaaa.substring(0, aaaa.indexOf("!")).substring(aaaa.indexOf("/")+1);
		String path=b.substring(0, b.lastIndexOf("/")+1)+"proxyConfig.properties";
		FileInputStream fis = new FileInputStream(path);
		configProp.load(fis);
		fis.close();

		String strValue = (String) configProp.get("proxy.id");
		if (strValue != null && !"".equals(strValue)) {
			config.proxyIP = strValue;
		} else {
			errorNum++;
		}
		strValue = (String) configProp.get("proxy.port");
		if (strValue != null && !"".equals(strValue)) {
			config.proxyPort = Integer.parseInt(strValue);
		} else {
			errorNum++;
		}

		strValue = (String) configProp.get("main.socket.address");
		if (strValue != null && !"".equals(strValue)) {
			int n = strValue.indexOf(':');
			config.mainIP = strValue.substring(0, n);
			config.mainPort = Integer.parseInt(strValue.substring(n + 1));
		} else {
			errorNum++;
		}

		strValue = (String) configProp.get("branch.socket.address");
		if (strValue != null && !"".equals(strValue)) {
			String[] branchList = strValue.split(",");
			for (String item : branchList) {
				config.branchList.add(item);
			}
		} else {
			errorNum++;
		}

		strValue = (String) configProp.get("branch.check.times");
		if (strValue != null && !"".equals(strValue)) {
			config.checktimes = Long.parseLong(strValue);
		} else {
			errorNum++;
		}
		
		strValue = (String) configProp.get("branch.numbers");
		if (strValue != null && !"".equals(strValue)) {
			config.branchNumbers = Integer.parseInt(strValue);
		} else {
			errorNum++;
		}
	}
}
