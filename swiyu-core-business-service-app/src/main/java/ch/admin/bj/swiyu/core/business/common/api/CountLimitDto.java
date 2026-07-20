package ch.admin.bj.swiyu.core.business.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CountLimit")
public record CountLimitDto(ApiObjectDto relatesTo, long current, long max) {}
