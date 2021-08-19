package com.buschmais.jqassistant.commandline.test;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies command line listing of available rules.
 */
@ExtendWith(Neo4JTestTemplateInvocationContextProvider.class)
class HelpIT extends AbstractCLIIT {

    @TestTemplate
    void runWithoutTask() throws IOException, InterruptedException {
        verify(new String[0]);
    }

    @TestTemplate
    void helpOption() throws IOException, InterruptedException {
        verify(new String[] { "-help}" });
    }

    private void verify(String[] args) throws IOException, InterruptedException {
        ExecutionResult executionResult = execute(args);
        assertThat(executionResult.getExitCode()).isEqualTo(1);
        List<String> console = executionResult.getStandardConsole();
        assertThat(console).anyMatch(item -> item.contains("usage: com.buschmais.jqassistant.commandline.Main <task> [options]"));
    }

}
