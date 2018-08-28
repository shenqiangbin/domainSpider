package domainSpider;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BeiAn {

	public static void main(String[] args) {
	
		Logger log = LoggerFactory.getLogger("BeiAn");
		
		StdSchedulerFactory schedulerFactory = new StdSchedulerFactory();
		Scheduler scheduler;
		try {
			
			scheduler = schedulerFactory.getScheduler();
			scheduler.start();
			
		} catch (SchedulerException e) {			
			e.printStackTrace();
			log.error(e.getMessage() + e.getStackTrace());
		}
		

	}

}
