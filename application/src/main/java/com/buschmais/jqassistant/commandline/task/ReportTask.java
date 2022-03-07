package com.buschmais.jqassistant.commandline.task;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import com.buschmais.jqassistant.commandline.CliExecutionException;
import com.buschmais.jqassistant.core.configuration.api.Configuration;
import com.buschmais.jqassistant.core.configuration.api.PropertiesConfigBuilder;
import com.buschmais.jqassistant.core.report.api.ReportTransformer;
import com.buschmais.jqassistant.core.report.api.ReportTransformerException;
import com.buschmais.jqassistant.core.report.impl.HtmlReportTransformer;

import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReportTask extends AbstractTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportTask.class);

    public static final String REPORT_FILE_HTML = "jqassistant-report.html";

    private String reportDirectory;

    @Override
    public void withOptions(CommandLine options, PropertiesConfigBuilder propertiesConfigBuilder) {
        reportDirectory = getOptionValue(options, CMDLINE_OPTION_REPORTDIR, DEFAULT_REPORT_DIRECTORY);
    }

    @Override
    public void run(Configuration configuration) throws CliExecutionException {
        File xmlReportFile = new File(reportDirectory, REPORT_FILE_XML);
        if (!xmlReportFile.exists()) {
            LOGGER.error(xmlReportFile.getName() + " does not exist.");
        } else {
            LOGGER.info("Transforming " + xmlReportFile.getAbsolutePath() + ".");
            File htmlReportFile = new File(reportDirectory, REPORT_FILE_HTML);
            Source xmlSource = new StreamSource(xmlReportFile);
            FileWriter writer;
            try {
                writer = new FileWriter(htmlReportFile);
            } catch (IOException e) {
                throw new CliExecutionException("Cannot create HTML report file.", e);
            }
            Result htmlTarget = new StreamResult(writer);
            ReportTransformer transformer = new HtmlReportTransformer();
            try {
                transformer.toStandalone(xmlSource, htmlTarget);
            } catch (ReportTransformerException e) {
                throw new CliExecutionException("Cannot transform report.", e);
            }
        }
    }

}
