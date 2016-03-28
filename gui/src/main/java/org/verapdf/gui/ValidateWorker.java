package org.verapdf.gui;

import org.apache.log4j.Logger;
import org.verapdf.core.ValidationException;
import org.verapdf.features.pb.PBFeatureParser;
import org.verapdf.features.tools.FeaturesCollection;
import org.verapdf.gui.config.Config;
import org.verapdf.gui.tools.GUIConstants;
import org.verapdf.gui.tools.ProcessingType;
import org.verapdf.metadata.fixer.impl.MetadataFixerImpl;
import org.verapdf.metadata.fixer.impl.pb.FixerConfigImpl;
import org.verapdf.metadata.fixer.utils.FileGenerator;
import org.verapdf.metadata.fixer.utils.FixerConfig;
import org.verapdf.model.ModelParser;
import org.verapdf.pdfa.PDFAValidator;
import org.verapdf.pdfa.results.MetadataFixerResult;
import org.verapdf.pdfa.results.MetadataFixerResult.RepairStatus;
import org.verapdf.pdfa.results.ValidationResult;
import org.verapdf.pdfa.validation.ValidationProfile;
import org.verapdf.pdfa.validators.Validators;
import org.verapdf.report.HTMLReport;
import org.verapdf.report.MachineReadableReport;

import javax.swing.*;
import javax.xml.bind.JAXBException;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Validates PDF in a new threat.
 *
 * @author Maksim Bezrukov
 */
class ValidateWorker extends SwingWorker<ValidationResult, Integer> {

    private static final Logger LOGGER = Logger.getLogger(ValidateWorker.class);

    private File pdf;
    private ValidationProfile profile;
    private CheckerPanel parent;
    private Config settings;
    private File xmlReport = null;
    private File htmlReport = null;
    private ProcessingType processingType;
    private boolean isFixMetadata;

    private long startTimeOfValidation;
    private long endTimeOfValidation;

    ValidateWorker(CheckerPanel parent, File pdf, ValidationProfile profile,
            Config settings, ProcessingType processingType,
            boolean isFixMetadata) {
        if (pdf == null || !pdf.isFile() || !pdf.canRead()) {
            throw new IllegalArgumentException(
                    "PDF file doesn't exist or it can not be read");
        }
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Profile doesn't exist or it can not be read");
        }
        this.parent = parent;
        this.pdf = pdf;
        this.profile = profile;
        this.settings = settings;
        this.processingType = processingType;
        this.isFixMetadata = isFixMetadata;
    }

    @Override
    protected ValidationResult doInBackground() {
        this.xmlReport = null;
        this.htmlReport = null;
        ValidationResult validationResult = null;
        MetadataFixerResult fixerResult = null;
        FeaturesCollection collection = null;

        this.startTimeOfValidation = System.currentTimeMillis();

        try (ModelParser parser = new ModelParser(new FileInputStream(
                this.pdf.getPath()), this.profile.getPDFAFlavour())) {

            if (this.processingType.isValidating()) {
                validationResult = runValidator(parser);
                if (this.isFixMetadata) {
                    fixerResult = this.fixMetadata(validationResult, parser);
                }
            }
            if (this.processingType.isFeatures()) {
                try {
                    collection = PBFeatureParser.getFeaturesCollection(parser
                            .getPDDocument());
                } catch (Exception e) {
                    JOptionPane.showMessageDialog(this.parent,
                            "Some error in creating features collection.",
                            GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
                    LOGGER.error("Exception in creating features collection: ",
                            e);
                }
            }
            this.endTimeOfValidation = System.currentTimeMillis();
            writeReports(validationResult, fixerResult, collection);
        } catch (IOException e) {
            this.parent
                    .errorInValidatingOccur(GUIConstants.ERROR_IN_PARSING, e);
        }

        return validationResult;
    }

    private MetadataFixerResult fixMetadata(ValidationResult info,
            ModelParser parser) throws IOException {
        FixerConfig fixerConfig = FixerConfigImpl.getFixerConfig(
                parser.getPDDocument(), info);
        Path path = this.settings.getFixMetadataPathFolder();
        File tempFile = File.createTempFile("fixedTempFile", ".pdf");
        tempFile.deleteOnExit();
        try (OutputStream tempOutput = new BufferedOutputStream(
                new FileOutputStream(tempFile))) {
            MetadataFixerResult fixerResult = MetadataFixerImpl.fixMetadata(
                    tempOutput, fixerConfig);
            MetadataFixerResult.RepairStatus repairStatus = fixerResult
                    .getRepairStatus();
            if (repairStatus == RepairStatus.SUCCESS || repairStatus == RepairStatus.ID_REMOVED) {
                File resFile;
                boolean flag = true;
                while (flag) {
                    if (!path.toString().trim().isEmpty()) {
                        resFile = FileGenerator.createOutputFile(this.settings
                                .getFixMetadataPathFolder().toFile(), this.pdf
                                .getName(), this.settings
                                .getMetadataFixerPrefix());
                    } else {
                        resFile = FileGenerator.createOutputFile(this.pdf,
                                this.settings.getMetadataFixerPrefix());
                    }

                    try {
                        Files.copy(tempFile.toPath(), resFile.toPath());
                        flag = false;
                    } catch (FileAlreadyExistsException e) {
                        LOGGER.error(e);
                    }
                }
            }
            return fixerResult;
        }
    }

    private ValidationResult runValidator(ModelParser toValidate)
            throws IOException {
        try {
            int max = settings.getMaxNumberOfFailedChecks();
            PDFAValidator validator;
            if (max > 0) {
                validator = Validators.createValidator(this.profile, true, max);
            } else {
                validator = Validators.createValidator(this.profile, true);
            }
            return validator.validate(toValidate);
        } catch (ValidationException e) {

            this.parent.errorInValidatingOccur(
                    GUIConstants.ERROR_IN_VALIDATING, e);
        }
        return null;
    }

    @Override
    protected void done() {
        this.parent.validationEnded(this.xmlReport, this.htmlReport);
    }

    private void writeReports(ValidationResult result,
            MetadataFixerResult fixerResult, FeaturesCollection collection) {
        try {
            this.xmlReport = File.createTempFile("veraPDF-tempXMLReport",
                    ".xml");
            this.xmlReport.deleteOnExit();
            MachineReadableReport report = MachineReadableReport.fromValues(this.pdf,
                    this.profile, result, this.settings.isShowPassedRules(),
                    this.settings.getMaxNumberOfDisplayedFailedChecks(), fixerResult, collection,
                    this.endTimeOfValidation - this.startTimeOfValidation);
            try (OutputStream xmlReportOs = new FileOutputStream(this.xmlReport)) {
                MachineReadableReport.toXml(report, xmlReportOs, Boolean.TRUE);
            }
            if (result != null) {
                this.htmlReport = File.createTempFile("veraPDF-tempHTMLReport",
                        ".html");
                this.htmlReport.deleteOnExit();
                try (InputStream xmlStream = new FileInputStream(this.xmlReport);
                        OutputStream htmlStream = new FileOutputStream(
                                this.htmlReport)) {
                    HTMLReport.writeHTMLReport(xmlStream, htmlStream, settings.getProfileWikiPath());

                } catch (IOException | TransformerException e) {
                    JOptionPane.showMessageDialog(this.parent,
                            GUIConstants.ERROR_IN_SAVING_HTML_REPORT,
                            GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
                    LOGGER.error("Exception saving the HTML report", e);
                    this.htmlReport = null;
                }
            }
        } catch (IOException | JAXBException e) {
            JOptionPane.showMessageDialog(this.parent,
                    GUIConstants.ERROR_IN_SAVING_XML_REPORT,
                    GUIConstants.ERROR, JOptionPane.ERROR_MESSAGE);
            LOGGER.error("Exception saving the XML report", e);
            this.xmlReport = null;
        }
    }
}
