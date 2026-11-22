package com.psycorp.psychapi.feature.storage.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class StorageEventListener {

    @Inject
    Logger log;

    @Inject
    StorageService storageService;

    public void onProfilePictureChanged(@ObservesAsync ProfilePictureChangedEvent event) {
        try {
            log.infof("Background cleanup: menghapus foto lama %s", event.oldProfilePictureUrl());
            storageService.deletePublicFile(event.oldProfilePictureUrl());
        } catch (Exception e) {
            log.errorf(e, "Gagal menghapus foto lama: %s", event.oldProfilePictureUrl());
        }
    }

    public void onFileCleanup(@ObservesAsync FileCleanupEvent event) {
        if (event.fileUrls() == null) return;
        for (String url : event.fileUrls()) {
            try {
                log.infof("Background cleanup: menghapus file %s", url);
                storageService.deletePublicFile(url);
            } catch (Exception e) {
                log.errorf(e, "Gagal menghapus file: %s", url);
            }
        }
    }
}