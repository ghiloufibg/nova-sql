package com.novasql;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class DatabaseEngineTest {

    private DatabaseEngine engine;

    @BeforeEach
    void setUp() {
        engine = new DatabaseEngine();
    }

    @Test
    void shouldInitializeEngine() {
        assertThat(engine.isRunning()).isFalse();

        engine.start();

        assertThat(engine.isRunning()).isTrue();
    }

    @Test
    void shouldStopEngine() {
        engine.start();
        assertThat(engine.isRunning()).isTrue();

        engine.stop();

        assertThat(engine.isRunning()).isFalse();
    }

    @Test
    void shouldHandleDoubleStart() {
        engine.start();
        assertThat(engine.isRunning()).isTrue();

        engine.start(); // Should not throw exception

        assertThat(engine.isRunning()).isTrue();
    }
}