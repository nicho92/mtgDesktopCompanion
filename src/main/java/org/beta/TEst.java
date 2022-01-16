package org.beta;

import java.io.IOException;

import org.magic.api.beans.audit.JsonQueryInfo;
import org.magic.services.TechnicalServiceManager;
import org.magic.services.network.URLTools;

public class TEst {
	
	
	
	public static void main(String[] args) throws IOException {
		TechnicalServiceManager.inst().restore();
		var lst = TechnicalServiceManager.inst().getJsonInfo();
		
		var demoKey="a8dc51356cb04465a1c44a8a4c773946";
		lst.stream().map(JsonQueryInfo::getIp).distinct().forEach(ip->{
			
			try {
				
				var o = URLTools.extractAsJson("https://api.geoapify.com/v1/ipinfo?&ip="+ip+"&apiKey="+demoKey).getAsJsonObject();
				
				
				System.out.println(o);
			} catch (IOException e) {
				
			}
			
		});
	}
}
