package com.buschmais.jqassistant.commandline.test;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

/**
 * Verifies command line listing of effective rules.
 */
@ExtendWith(Neo4JTestTemplateInvocationContextProvider.class)
public class EffectiveRulesIT extends AbstractCLIIT {

    @TestTemplate
    public void defaultGroup() throws IOException, InterruptedException {
        String rulesDirectory = EffectiveRulesIT.class.getResource("/rules").getFile();
        String[] args = new String[] { "effective-rules", "-r", rulesDirectory };
        ExecutionResult executionResult = execute(args);
        assertThat(executionResult.getExitCode(), equalTo(0));
        List<String> console = executionResult.getErrorConsole();
        assertThat(console, hasItem(containsString(TEST_CONCEPT)));
        assertThat(console, hasItem(containsString(TEST_CONSTRAINT)));
        assertThat(console, not(hasItem(containsString(CUSTOM_TEST_CONCEPT))));
        assertThat(console, not(hasItem(containsString("junit4:TestMethod"))));
    }

    @TestTemplate
    public void customGroup() throws IOException, InterruptedException {
        String rulesDirectory = EffectiveRulesIT.class.getResource("/rules").getFile();
        String[] args = new String[] { "effective-rules", "-r", rulesDirectory, "-groups", CUSTOM_GROUP };
        ExecutionResult executionResult = execute(args);
        assertThat(executionResult.getExitCode(), equalTo(0));
        List<String> console = executionResult.getErrorConsole();
        assertThat(console, hasItem(containsString(TEST_CONCEPT)));
        assertThat(console, hasItem(containsString(TEST_CONSTRAINT)));
        assertThat(console, hasItem(containsString(CUSTOM_TEST_CONCEPT)));
        assertThat(console, not(hasItem(containsString("junit4:TestMethod"))));
    }

    @TestTemplate
    public void concept() throws IOException, InterruptedException {
        String rulesDirectory = EffectiveRulesIT.class.getResource("/rules").getFile();
        String[] args = new String[] { "effective-rules", "-r", rulesDirectory, "-concepts", "junit4:TestMethod" };
        ExecutionResult executionResult = execute(args);
        assertThat(executionResult.getExitCode(), equalTo(0));
        List<String> console = executionResult.getErrorConsole();
        assertThat(console, hasItem(containsString("junit4:TestMethod")));
    }
}
