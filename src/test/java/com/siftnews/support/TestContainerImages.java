package com.siftnews.support;

import org.testcontainers.utility.DockerImageName;

public final class TestContainerImages {

    private TestContainerImages() {}

    public static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
}
