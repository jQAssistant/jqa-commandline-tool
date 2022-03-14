package com.buschmais.jqassistant.commandline.task;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import com.buschmais.jqassistant.commandline.CliConfigurationException;
import com.buschmais.jqassistant.commandline.CliExecutionException;
import com.buschmais.jqassistant.commandline.CliRuleViolationException;
import com.buschmais.jqassistant.core.analysis.api.Analyzer;
import com.buschmais.jqassistant.core.analysis.api.configuration.Analyze;
import com.buschmais.jqassistant.core.analysis.impl.AnalyzerImpl;
import com.buschmais.jqassistant.core.analysis.spi.AnalyzerPluginRepository;
import com.buschmais.jqassistant.core.configuration.api.Configuration;
import com.buschmais.jqassistant.core.configuration.api.PropertiesConfigBuilder;
import com.buschmais.jqassistant.core.report.api.ReportContext;
import com.buschmais.jqassistant.core.report.api.ReportException;
import com.buschmais.jqassistant.core.report.api.ReportHelper;
import com.buschmais.jqassistant.core.report.api.ReportPlugin;
import com.buschmais.jqassistant.core.report.api.configuration.Report;
import com.buschmais.jqassistant.core.report.impl.CompositeReportPlugin;
import com.buschmais.jqassistant.core.report.impl.InMemoryReportPlugin;
import com.buschmais.jqassistant.core.report.impl.ReportContextImpl;
import com.buschmais.jqassistant.core.rule.api.model.RuleException;
import com.buschmais.jqassistant.core.rule.api.model.RuleSet;
import com.buschmais.jqassistant.core.rule.api.model.Severity;
import com.buschmais.jqassistant.core.store.api.Store;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.Collections.emptyMap;

/**
 * @author jn4, Kontext E GmbH, 24.01.14
 */
public class AnalyzeTask extends AbstractAnalyzeTask {

    private static final String CMDLINE_OPTION_FAIL_ON_SEVERITY = "failOnSeverity";
    private static final String CMDLINE_OPTION_WARN_ON_SEVERITY = "warnOnSeverity";
    private static final String CMDLINE_OPTION_RULEPARAMETERS = "ruleParameters";
    private static final String CMDLINE_OPTION_EXECUTEAPPLIEDCONCEPTS = "executeAppliedConcepts";
    private static final String CMDLINE_OPTION_CREATE_REPORT_ARCHIVE = "createReportArchive";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractAnalyzeTask.class);

    private File reportDirectory;
    private boolean createReportArchive;

    @Override
    protected void executeTask(Configuration configuration, final Store store) throws CliExecutionException {
        Analyze analyze = configuration.analyze();
        Severity warnOnSeverity = analyze.report()
            .warnOnSeverity();
        Severity failOnSeverity = analyze.report()
            .failOnSeverity();
        LOGGER.info("Will warn on violations starting form severity '" + warnOnSeverity + "'");
        LOGGER.info("Will fail on violations starting from severity '" + failOnSeverity + "'.");
        LOGGER.info("Executing analysis.");

        ReportContext reportContext = new ReportContextImpl(store, reportDirectory, reportDirectory);
        Map<String, ReportPlugin> reportPlugins = getReportPlugins(analyze
            .report(), reportContext);
        InMemoryReportPlugin inMemoryReportPlugin = new InMemoryReportPlugin(new CompositeReportPlugin(reportPlugins));
        try {
            Analyzer analyzer = new AnalyzerImpl(analyze, store, pluginRepository.getAnalyzerPluginRepository()
                .getRuleInterpreterPlugins(emptyMap()),
                    inMemoryReportPlugin, LOGGER);
            RuleSet availableRules = getAvailableRules(analyze
                .rule());
            analyzer.execute(availableRules, getRuleSelection(availableRules));
        } catch (RuleException e) {
            throw new CliExecutionException("Analysis failed.", e);
        }
        if (createReportArchive) {
            createReportArchive(reportContext);
        }
        store.beginTransaction();
        LOGGER.info("Verifying results: failOnSeverity=" + failOnSeverity + ", warnOnSeverity=" + warnOnSeverity);
        try {
            final ReportHelper reportHelper = new ReportHelper(configuration.analyze().report(), LOGGER);
            final int conceptViolations = reportHelper.verifyConceptResults(inMemoryReportPlugin);
            final int constraintViolations = reportHelper.verifyConstraintResults(inMemoryReportPlugin);
            if (conceptViolations > 0 || constraintViolations > 0) {
                throw new CliRuleViolationException("Failed rules detected: " + conceptViolations + " concepts, " + constraintViolations + " constraints");
            }
        } finally {
            store.commitTransaction();
        }
    }

    private void createReportArchive(ReportContext reportContext) throws CliConfigurationException {
        File reportArchive = null;
        try {
            reportArchive = reportContext.createReportArchive();
        } catch (ReportException e) {
            throw new CliConfigurationException("Cannot create report archive.", e);
        }
        LOGGER.info("Created report archive '{}'.", reportArchive.getAbsolutePath());
    }

    /**
     * Reads the given rule parameters file.
     *
     * @param propertiesConfigBuilder
     *     The {@link PropertiesConfigBuilder}.
     * @throws CliExecutionException
     *     If the file cannot be read.
     */
    private void loadRuleParameters(String ruleParametersFile, PropertiesConfigBuilder propertiesConfigBuilder) throws CliConfigurationException {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(ruleParametersFile));
        } catch (IOException e) {
            throw new CliConfigurationException("Cannot read rule parameters file '" + ruleParametersFile + "'.", e);
        }
        propertiesConfigBuilder.with(Analyze.PREFIX, Analyze.RULE_PARAMETERS, properties);
    }

    /**
     * Get all configured report plugins.
     *
     * @param configuration
     *     The {@link Report} configuration.
     * @param reportContext
     *     The ReportContext.
     * @return The list of report plugins.
     */
    private Map<String, ReportPlugin> getReportPlugins(Report configuration, ReportContext reportContext) {
        AnalyzerPluginRepository analyzerPluginRepository;
        analyzerPluginRepository = pluginRepository.getAnalyzerPluginRepository();
        return analyzerPluginRepository.getReportPlugins(configuration, reportContext);
    }

    @Override
    public void withOptions(final CommandLine options, PropertiesConfigBuilder propertiesConfigBuilder) throws CliConfigurationException {
        super.withOptions(options, propertiesConfigBuilder);
        String ruleParametersFileName = getOptionValue(options, CMDLINE_OPTION_RULEPARAMETERS, null);
        if (ruleParametersFileName != null) {
            loadRuleParameters(ruleParametersFileName, propertiesConfigBuilder);
        }
        String reportDirectoryValue = getOptionValue(options, CMDLINE_OPTION_REPORTDIR, DEFAULT_REPORT_DIRECTORY);
        reportDirectory = new File(reportDirectoryValue);
        reportDirectory.mkdirs();
        createReportArchive = options.hasOption(CMDLINE_OPTION_CREATE_REPORT_ARCHIVE);
        propertiesConfigBuilder.with(Analyze.PREFIX, Analyze.EXECUTE_APPLIED_CONCEPTS, options.hasOption(CMDLINE_OPTION_EXECUTEAPPLIEDCONCEPTS));
        propertiesConfigBuilder.with(Report.PREFIX, Report.PROPERTIES, pluginProperties);
        propertiesConfigBuilder.with(Report.PREFIX, Report.FAIL_ON_SEVERITY, getSeverity(getOptionValue(options, CMDLINE_OPTION_FAIL_ON_SEVERITY)));
        propertiesConfigBuilder.with(Report.PREFIX, Report.WARN_ON_SEVERITY, getSeverity(getOptionValue(options, CMDLINE_OPTION_WARN_ON_SEVERITY)));
    }

    @Override
    public void addTaskOptions(final List<Option> options) {
        super.addTaskOptions(options);
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_RULEPARAMETERS).withDescription("The name of a properties file providing rule parameters.")
                .hasArgs().create(CMDLINE_OPTION_RULEPARAMETERS));
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_REPORTDIR).withDescription("The directory for writing reports.").hasArgs()
                .create(CMDLINE_OPTION_REPORTDIR));
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_FAIL_ON_SEVERITY)
                .withDescription("The severity threshold to fail on rule violations, i.e. to exit with an error code.").hasArgs()
                .create(CMDLINE_OPTION_FAIL_ON_SEVERITY));
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_WARN_ON_SEVERITY).withDescription("The severity threshold to warn on rule violations.").hasArgs()
                .create(CMDLINE_OPTION_WARN_ON_SEVERITY));
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_EXECUTEAPPLIEDCONCEPTS)
                .withDescription("If set also execute concepts which have already been applied.").create(CMDLINE_OPTION_EXECUTEAPPLIEDCONCEPTS));
        options.add(OptionBuilder.withArgName(CMDLINE_OPTION_CREATE_REPORT_ARCHIVE)
                .withDescription("If set a ZIP archive named 'jqassistant-report.zip' is created containing all generated reports.")
                .create(CMDLINE_OPTION_CREATE_REPORT_ARCHIVE));
    }
}
