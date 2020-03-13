package test;

import java.nio.file.Paths;

import ru.windcorp.mineragenesis.rb.config.*;

public class Configuration2Test {

	public static void main(String[] args) throws Exception {
		ConfigReader reader = new ConfigReader(Paths.get("C:\\Users\\javaponyportable\\Documents\\tmp\\configtest.cfg"));
		ConfigLoader config = new ConfigLoader(reader,
				new Verb<Double>("add", Double.class) {
			
					@Override
					protected Double runImpl(Arguments args) throws ConfigurationException {
						return args.get(null, Double.class) + args.get(null, Double.class);
					}
					
				},
				
				new Verb<Double>("div", Double.class) {
					
					@Override
					protected Double runImpl(Arguments args) throws ConfigurationException {
						return args.get("divident", Double.class) / args.get("divisor", Double.class);
					}
					
				},
				
				new Verb<Void>("print", Void.TYPE) {
					
					@Override
					protected Void runImpl(Arguments args) throws ConfigurationException {
						System.out.println(args.get(null, Object.class));
						return null;
					}
					
				}
		);
		
		config.load();
	}

}
