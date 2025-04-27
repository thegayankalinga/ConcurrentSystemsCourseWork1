package gayan.tests;

import org.junit.jupiter.api.BeforeAll;

public class BaseTestConfig {

    @BeforeAll
    public static void setUp() {
        // Global test setup if needed
        System.out.println("Setting up tests for Ticket System");
    }

    // Common test constants
    public static final int DEFAULT_CAPACITY = 100;
    public static final int HIGH_CAPACITY = 1000;
    public static final int DEFAULT_THREAD_COUNT = 10;
    public static final int HIGH_CONCURRENCY_THREAD_COUNT = 100;
    public static final int TEST_TIMEOUT_SECONDS = 30;
}
