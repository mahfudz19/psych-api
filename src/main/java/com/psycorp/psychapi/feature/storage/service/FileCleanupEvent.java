package com.psycorp.psychapi.feature.storage.service;

import java.util.List;

/**
 * Event untuk menghapus satu atau lebih file dari GCS secara async.
 * Digunakan saat update/delete entity yang memiliki file terkait.
 */
public record FileCleanupEvent(List<String> fileUrls) {}
