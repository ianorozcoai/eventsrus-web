package com.web.eventsrus.backend;

/** Mirrors eventsrus-backend's dto.SystemSettingResponse - one row for the admin module's "System Settings" page. */
public record BackendSystemSetting(String key, String label, String description, String value) {}
