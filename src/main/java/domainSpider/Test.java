package domainSpider;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Test {

	public static void main(String[] args) {
		
		List<Person> list = new ArrayList<Person>();
		while(true) {
			System.out.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
			list.add(new Person(1, "tom"));
		}

	}

}

class Person{
	private int id;
	private String name;
	
	public Person(int _id,String _name) {
		this.id = _id;
		this.name = _name;
	}
}
