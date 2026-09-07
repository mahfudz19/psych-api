package com.psycorp.psychapi.feature.storage.service;

/**
 * Event yang dipancarkan saat user mengganti foto profil.
 * Listener akan menghapus foto lama di background thread.
 */
public record ProfilePictureChangedEvent(String oldProfilePictureUrl) {}