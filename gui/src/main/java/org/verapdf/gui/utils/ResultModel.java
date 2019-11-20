package org.verapdf.gui.utils;

import org.verapdf.processor.reports.BatchSummary;

public class ResultModel {
	private BatchSummary batchSummary;
	private int policyNonCompliantJobCount = 0;


	public ResultModel(BatchSummary batchSummary, int policyNonCompliantJobCount) {
		this.batchSummary = batchSummary;
		this.policyNonCompliantJobCount = policyNonCompliantJobCount;
	}

	public ResultModel(BatchSummary batchSummary) {
		this.batchSummary = batchSummary;
	}

	public BatchSummary getBatchSummary() {
		return this.batchSummary;
	}

	public boolean isMultiJob() {
		return this.batchSummary.isMultiJob();
	}

	public int getPolicyNonCompliantJobCount() {
		return policyNonCompliantJobCount;
	}

	public int getFailedEncryptedJobCount() {
		return this.batchSummary.getFailedEncryptedJobs();
	}

	public int getFailedParsingJobCount() {
		return this.batchSummary.getFailedParsingJobs();
	}

	public int getNonCompliantPdfaCount() {
		return this.batchSummary.getValidationSummary().getNonCompliantPdfaCount();
	}

	public int getCompliantPdfaCount() {
		return this.batchSummary.getValidationSummary().getCompliantPdfaCount();
	}

	public int getValidationTotalJobCount() {
		return this.batchSummary.getValidationSummary().getTotalJobCount();
	}

	public int getFeatureTotalJobCount() {
		return this.batchSummary.getFeaturesSummary().getTotalJobCount();
	}

	public int getValidationFailedJobCount() {
		return this.batchSummary.getValidationSummary().getFailedJobCount();
	}

	public int getFeatureFailedJobCount() {
		return this.batchSummary.getFeaturesSummary().getFailedJobCount();
	}

	public int getFeatureSuccessfulJobCount() {
		return this.batchSummary.getFeaturesSummary().getSuccessfulJobCount();
	}

	public int getTotalJobs(){
		return this.batchSummary.getTotalJobs();
	}
}
