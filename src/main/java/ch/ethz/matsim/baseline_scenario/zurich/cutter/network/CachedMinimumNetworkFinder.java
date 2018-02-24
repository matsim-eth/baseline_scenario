package ch.ethz.matsim.baseline_scenario.zurich.cutter.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;

public class CachedMinimumNetworkFinder implements MinimumNetworkFinder {
	final private static Logger log = Logger.getLogger(CachedMinimumNetworkFinder.class);

	final private File cacheFile;
	final private MinimumNetworkFinder delegate;

	public CachedMinimumNetworkFinder(File cacheFile, MinimumNetworkFinder delegate) {
		this.cacheFile = cacheFile;
		this.delegate = delegate;
	}

	private Set<Id<Link>> load() {
		Set<Id<Link>> linkIds = new HashSet<>();

		try (DataInputStream input = new DataInputStream(new FileInputStream(cacheFile))) {
			while (true) {
				linkIds.add(Id.createLinkId(input.readUTF()));
			}
		} catch (EOFException e) {
			// Ignore
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		return linkIds;
	}

	private void save(Set<Id<Link>> linkIds) {
		try (DataOutputStream output = new DataOutputStream(new FileOutputStream(cacheFile))) {
			for (Id<Link> linkId : linkIds) {
				output.writeUTF(linkId.toString());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Set<Id<Link>> run(Set<Id<Link>> links) {
		Set<Id<Link>> result;

		if (cacheFile.exists()) {
			log.info("Loading minimum network from cache: " + cacheFile.getPath());
			result = load();
		} else {
			result = delegate.run(links);
			log.info("Saved minimum network to cache: " + cacheFile.getPath());
			save(result);
		}

		return result;
	}
}
