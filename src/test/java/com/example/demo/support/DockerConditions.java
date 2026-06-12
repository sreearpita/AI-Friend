package com.example.demo.support;

import org.testcontainers.DockerClientFactory;

public final class DockerConditions {
    private DockerConditions() {
    }

    public static boolean isDockerAvailable() {
        return DockerClientFactory.instance().isDockerAvailable();
    }
}
