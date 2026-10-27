package com.tanadelgigante.civilservant;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortManager {
	private static final Logger logger = LoggerFactory.getLogger(PortManager.class);
	private static final PortManager INSTANCE = new PortManager();
	private final Set<Integer> allocated = new HashSet<>();
	private final AtomicInteger nextPort = new AtomicInteger(49152);

	private PortManager() {
	}

	public static PortManager getInstance() {
		return INSTANCE;
	}

	public synchronized int allocateForService(ServiceConfig.BackendBlock backend, String serviceName) {
		if (backend.port() != null) {
			if (isValidPort(backend.port()) && isFree(backend.port())) {
				allocated.add(backend.port());
				return backend.port();
			}
			if ("fixed".equalsIgnoreCase(backend.portMode())) {
				logger.warn("Service {}: porta dichiarata {} non valida o occupata; fallback a auto", serviceName,
						backend.port());
			}
		}
		int port;
		do {
			port = nextPort.getAndIncrement();
			if (port > 65535) {
				nextPort.set(49152);
				port = nextPort.getAndIncrement();
			}
		} while (!isValidPort(port) || !isFree(port) || allocated.contains(port));
		allocated.add(port);
		return port;
	}

	private boolean isValidPort(int port) {
		return port >= 1024 && port <= 65535;
	}

	private boolean isFree(int port) {
		try (ServerSocket ss = new ServerSocket(port)) {
			ss.setReuseAddress(true);
			return true;
		} catch (IOException e) {
			return false;
		}
	}
}
