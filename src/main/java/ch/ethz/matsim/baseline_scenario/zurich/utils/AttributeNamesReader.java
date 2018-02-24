package ch.ethz.matsim.baseline_scenario.zurich.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.matsim.core.utils.io.IOUtils;

public class AttributeNamesReader {
	public Collection<String> read(File path) throws IOException {
		Set<String> attributes = new HashSet<>();

		BufferedReader reader = new BufferedReader(new InputStreamReader(IOUtils.getInputStream(path.getPath())));
		Pattern pattern = Pattern.compile("name=\"(.+?)\"");

		String line = null;
		while ((line = reader.readLine()) != null) {
			if (line.contains("<attribute ")) {
				Matcher matcher = pattern.matcher(line);

				while (matcher.find()) {
					attributes.add(matcher.group(1));
				}
			}
		}

		reader.close();

		return attributes;
	}

	static public void main(String[] args) throws IOException {
		System.out.println(String.join(",", new AttributeNamesReader().read(new File(args[0]))));
	}
}
