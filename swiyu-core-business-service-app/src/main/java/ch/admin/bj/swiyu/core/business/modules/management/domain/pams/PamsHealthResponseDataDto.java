package ch.admin.bj.swiyu.core.business.modules.management.domain.pams;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PamsHealthResponseDataDto(@JsonProperty("api_running") Boolean isApiRunning) {}
