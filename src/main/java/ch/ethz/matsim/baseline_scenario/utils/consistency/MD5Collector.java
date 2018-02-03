package ch.ethz.matsim.baseline_scenario.utils.consistency;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class MD5Collector {
	final private File basePath;
	final private List<String> files = new LinkedList<>();

	public MD5Collector(File basePath) {
		this.basePath = basePath;
	}

	public void add(String... file) {
		this.files.addAll(Arrays.asList(file));
	}

	public void write(File outputPath) throws IOException {
		List<File> paths = files.stream().map(f -> new File(basePath, f)).collect(Collectors.toList());

		for (File path : paths) {
			if (!path.exists()) {
				throw new IllegalStateException("Path does not exist: " + path.getPath());
			}
		}

		List<String> absolutePaths = paths.stream().map(f -> f.getAbsolutePath()).collect(Collectors.toList());
		InputStream inputStream = Runtime.getRuntime().exec("md5sum " + String.join(" ", absolutePaths))
				.getInputStream();

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputPath)));

		List<String> output = new LinkedList<>();

		String line = null;
		while ((line = reader.readLine()) != null) {
			writer.write(line.replace(basePath.getAbsolutePath() + "/", "") + "\n");
			writer.flush();
		}

		writer.close();
	}
}
