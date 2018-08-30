package domainSpider;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DataSource {
    private DataSource() {}
    
    private static class SingletonHolder{
    	
        private static HikariDataSource instance = getDataSource();
        
        private static HikariDataSource getDataSource() {

    		HikariConfig config = new HikariConfig();

    		config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/javablog?useUnicode=true&characterEncoding=utf8&useSSL=false");
    		config.setUsername("root");
    		config.setPassword("123456");
    		config.addDataSourceProperty("cachePrepStmts", "true");
    		config.addDataSourceProperty("prepStmtCacheSize", "250");
    		config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

    		return new HikariDataSource(config);
    	}
    }    
    
    public static HikariDataSource getInstance(){
        return SingletonHolder.instance;
    }
    
    
}

