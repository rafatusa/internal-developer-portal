package com.enterprise.idp.domain.deployment;

/**
 * Status of a deployment pipeline run.
 */
public enum DeploymentStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED,
    ROLLED_BACK,
    CANCELLED
}
