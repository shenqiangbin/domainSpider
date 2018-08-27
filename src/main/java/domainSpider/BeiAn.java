package domainSpider;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysql.jdbc.StringUtils;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

class Task extends TimerTask {

	private Logger log = LoggerFactory.getLogger(Task.class);
	
	
	@Override
	public void run() {
		System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		log.info(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
		
		getBeiAnInfo();
	}

	public void getBeiAnInfo() {
		String content = getPage("http://www.beian.gov.cn/portal/index.do");
		// 解析这句：var taken_for_user = 'd4b22312-936b-4d5a-869d-1e8d89770053';
		if (content == "")
			return;

		// print(content);
		String token = getToken(content);
		content = null;
		// print(token);

		String newUri = "http://www.beian.gov.cn/portal/recordShow?token=" + token;
		print(newUri);
		String newContent = getBeiAn(newUri);

		// print(newContent);
		resolveBeiAnContent(newContent);
		newContent = null;
	}

	public String cookieStr = "";

	public String getPage(String uri) {
		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(uri);

		try {
			CloseableHttpResponse response = client.execute(httpget);
			HttpEntity entity = response.getEntity();

			StringBuilder builder = new StringBuilder();
			Header[] headers = response.getHeaders("Set-Cookie");
			for (Header h : headers) {
				// print(h.getValue());
				String cookieVal = getCookie(h.getValue());
				// print(cookieVal);
				builder.append(cookieVal + ";");
				cookieVal = null;
			}
			cookieStr = builder.toString();

			String content = EntityUtils.toString(entity);
			EntityUtils.consume(entity);
			client.close();

			return content;

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}

	public String getToken(String content) {

		/*
		 * 参考： https://blog.csdn.net/yb642518034/article/details/61198976 (?=pattern)
		 * 正向先行断言 (?<=pattern) 正向后行断言
		 */
		String pattern = "(?<=var taken_for_user = ')(.*)(?=')";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);

		if (m.find()) {
			return m.group();
			// int groupCount = m.groupCount();
			// for(int i=0; i<groupCount; i++) {
			// String g = m.group(i);
			// print(g);
			// }
		}

		return "";
	}

	public String getCookie(String headerVal) {
		/* headerVal示例： JSESSIONID=8C22DB5AE5D032BBFD685B13152614BA; Path=/; HttpOnly */
		/*
		 * 、+限定符都是贪婪的，因为它们会尽可能多的匹配文字，只有在它们的后面加上一个?就可以实现非贪婪或最小匹配。
		 */
		String pattern = "(.*?)(?=;)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(headerVal);

		if (m.find()) {
			// return m.group();
			int groupCount = m.groupCount();
			for (int i = 0; i < groupCount; i++) {
				String g = m.group(i);
				// print(g);
				return g;
			}
		}
		return "";
	}

	public String getBeiAn(String uri) {

		/*
		 * Host: www.beian.gov.cn Connection: keep-alive Upgrade-Insecure-Requests: 1
		 * Referer: http://www.beian.gov.cn/portal/index.do Accept-Encoding: gzip,
		 * deflate Accept-Language: zh-CN,zh;q=0.9 Cookie:
		 * BIGipServerPOOL-WebAGPT=202576044.37151.0000;
		 * JSESSIONID=EF31D3439F233D772C60D858BA5B59E4
		 */

		CloseableHttpClient client = HttpClients.createDefault();
		HttpGet httpget = new HttpGet(uri);
		httpget.setHeader("Host", "www.beian.gov.cn");
		httpget.setHeader("Referer", "http://www.beian.gov.cn/portal/index.do");
		httpget.setHeader("Cookie", cookieStr);

		try {
			CloseableHttpResponse response = client.execute(httpget);
			HttpEntity entity = response.getEntity();

			String content = EntityUtils.toString(entity);
			EntityUtils.consume(entity);
			client.close();

			return content;

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return "";
	}

	// 解析备案内容html
	public void resolveBeiAnContent(String content) {
		// content = "<table><tr><td></td></tr></table>";
		// print(content);
		// print("----------------------------");
		String pattern = "<tr[^>]*>([\\s\\S]*?)</tr>"; // 获取tr中间的内容
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);

		while (m.find()) {
			int groupCount = m.groupCount();
			for (int i = 0; i < groupCount; i++) {
				String g = m.group(i);
				if (g.contains("备案网站名称"))
					break;
				// print(g);
				// print("=============");
				// return g;
				String sql = resolveTrContent(g);
				try {
					executeSql(sql);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public String resolveTrContent(String content) {
		String pattern = "(<td[^>]*>)([\\s\\S]*?)(?=</td>)"; // 获取td中间的内容
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);
		print("=============");

		StringBuilder builder = new StringBuilder();
		builder.append("insert sites(name,registerUrl,siteUrl,number,completeTime,addTime) values (");

		while (m.find()) {
			int groupCount = m.groupCount();
			for (int i = 0; i < groupCount; i++) {
				String g = m.group(i);
				g = g.substring(16);
				//print(g);
				if (StringUtils.isNullOrEmpty(g))
					continue;

				if (g.contains("<a")) { // 如果包含a标签
					String aContent = resolveAContent(g);
					//print(aContent);
					builder.append("'").append(aContent).append("',");
					String hrefContent = resolveHrefContent(g);
					//print(hrefContent);
					builder.append("'").append(hrefContent).append("',");
				} else {
					builder.append("'").append(g).append("',");
				}
			}
		}
		builder.append("'").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("'");
		builder.append(")");

		return builder.toString().replace(",)", ")");

	}

	public String resolveAContent(String content) {
		String pattern = "(?<=>)(.*?)(?=<)";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);

		if (m.find()) {
			return m.group();
		}
		return "";
	}

	public String resolveHrefContent(String content) {
		String pattern = "(?<=\")(.*?)(?=\")";
		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(content);
		if (m.find())
			return m.group();

		return "";
	}

	public void executeSql(String sql) throws SQLException {

		HikariDataSource dataSource = null;
		Connection connection = null;
		try {
			/* HikariDataSource 是需要关闭的 */
			dataSource = getDataSource();
			connection = dataSource.getConnection();

			Statement statement = connection.createStatement();

			print("sql:" + sql);
			log.info("sql:" + sql);
			statement.execute(sql);			

		} catch (Exception e) {
			
			if(e.getMessage().contains("siteUrl_UNIQUE") && e.getMessage().contains("Duplicate entry")) {
				print("已有数据");
				log.info("已有数据");
			}				
			else
			{
				e.printStackTrace();
				log.error(e.getMessage() + e.getStackTrace());
			}
			
		}finally {
			if (connection != null && !connection.isClosed())
				connection.close();
			if (dataSource != null && !dataSource.isClosed())
				dataSource.close();
		}
		
	}

	public void print(Object obj) {
		System.out.println(obj);
	}

	private HikariDataSource getDataSource() throws SQLException {

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

public class BeiAn {

	public static void main(String[] args) {
		Timer timer = new Timer();
		TimerTask task = new Task();
		timer.schedule(task, new Date(), 1000 * 60); // 每1秒执行一次
	}

}
