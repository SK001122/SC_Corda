package com.example.utils;

import net.corda.core.utilities.ProgressTracker;

public class Constants {

    public static final ProgressTracker.Step PROCESSING_TRANSACTION = new ProgressTracker.Step("Processing transaction.");
    public static final ProgressTracker.Step SHARING_TRANSACTION = new ProgressTracker.Step("Sharing with counterparties.");
    public static final ProgressTracker.Step CONFIRMING_TRANSACTION = new ProgressTracker.Step("Counterparty confirmation.");

}
