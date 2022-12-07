package com.buschmais.jqassistant.commandline.test;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies, that commandline supports the list-plugins command
 */
@ExtendWith(Neo4JTestTemplateInvocationContextProvider.class)
class ListPluginsIT extends AbstractCLIIT {

    @TestTemplate
    void supportsListingOfPlugins() throws IOException, InterruptedException {
        verify(new String[] { "list-plugins" });
    }

    private void verify(String[] args) throws IOException, InterruptedException {
        ExecutionResult executionResult = execute(args);
        assertThat(executionResult.getExitCode()).isEqualTo(0);
        List<String> console = executionResult.getStandardConsole();
        assertThat(console).hasSize(9);
        assertThat(console).anyMatch(item -> item.contains("jQAssistant Common Plugin (jqa.plugin.common)"));
        assertThat(console).anyMatch(item -> item.contains("jQAssistant Core Analysis Plugin (jqa.core.analysis.plugin)"));
        assertThat(console).anyMatch(item -> item.contains("jQAssistant Core Report Plugin (jqa.core.report.plugin)"));
        assertThat(console).anyMatch(item -> item.contains("jQAssistant Java Plugin (jqa.plugin.java)"));
        assertThat(console).anyMatch(item -> item.contains("jQAssistant JSON Plugin (jqa.plugin.json)"));
        assertThat(console).anyMatch(item -> item.contains("jQAssistant Maven 3 Plugin (jqa.plugin.maven3)"));
        assertThat(console).anyMatch(item -> item.contains("jQAssistant XML Plugin (jqa.plugin.xml)"));
        assertThat(console).anyMatch(item -> item.contains("jQAssistant YAML 2 Plugin (jqa.plugin.yaml2)"));
    }
}
