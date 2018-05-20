//
// Copyright 2018 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.21
//
package com.amazonaws.mobile.content;

import android.content.Context;
import android.util.Log;

import com.amazonaws.mobile.util.StringFormatUtils;
import com.amazonaws.mobile.util.ThreadUtils;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferType;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

/**
 * Manages S3 Transfers that will be placed into a local cache.
 */
/* package */ class S3TransferHelper implements TransferHelper, TransferListener {
    private static final String LOG_TAG = S3TransferHelper.class.getSimpleName();
    /** The S3 TransferUtility for managing S3 transfers. */
    private final TransferUtility transferUtility;

    /** Map from the download ID to the Transfer Observer. */
    private final HashMap<Integer, TransferObserver> transfersInProgress;
    /** Map from the S3 Key to the download ID. */
    private final HashMap<String, Integer> managedFilesToTransfers;

    private final HashMap<Integer, ContentProgressListener> progressListeners;

    /** The S3 bucket to use for transfers. */
    private final String bucket;

    /** The S3 directory prefix. Always ends with "/" accept in the case that it is an empty string. */
    private final String s3DirPrefix;

    /** The local path to content being downloaded always ending with "/". */
    private final String localTransferPath;

    private final LocalContentCache localContentCache;

    private long sizeTransferring;

    private S3TransferHelper(final Context context,
                            final AmazonS3Client s3Client,
                            final String bucket,
                            final String s3DirPrefix,
                            final String localTransferPath,
                            final LocalContentCache cache) {
        this.bucket = bucket;
        this.s3DirPrefix = s3DirPrefix == null ? "" : s3DirPrefix;
        this.localContentCache = cache;
        if (localTransferPath.endsWith(DIR_DELIMITER)) {
            this.localTransferPath = localTransferPath;
        } else {
            this.localTransferPath = localTransferPath + DIR_DELIMITER;
        }
        transferUtility = TransferUtility.builder().s3Client(s3Client).context(context).build();
        transfersInProgress = new HashMap<>();
        managedFilesToTransfers = new HashMap<>();
        progressListeners = new HashMap<>();
    }

    /* package */ static S3TransferHelper build(final Context context,
                                         final AmazonS3Client s3Client,
                                         final String bucket,
                                         final String s3DirPrefix,
                                         final String localTransferPath,
                                         final LocalContentCache cache) {
        S3TransferHelper transferHelper = new S3TransferHelper(context, s3Client, bucket,
            s3DirPrefix, localTransferPath, cache);

        transferHelper.pollAndCleanUpTransfers();
        return transferHelper;
    }

    private String getRelativeFilePath(String downloadFilePath) {
        if (!downloadFilePath.startsWith(localTransferPath)) {
            Log.e(LOG_TAG, String.format("File path '%s' does not begin with the local transfer path '%s'",
                downloadFilePath, localTransferPath));
            return downloadFilePath;
        }
        // Remove the localTransferPath from the path to get the relative path of the item.
        return downloadFilePath.substring(localTransferPath.length());
    }

    private TransferObserver startTransfer(final String filePath, final long fileSize,
                                           final ContentProgressListener listener) {
        final TransferObserver observer;
        final File localTransferFile = new File(localTransferPath + filePath);
        final String s3Key = s3DirPrefix + filePath;
        final int transferId;

        synchronized (this) {
            sizeTransferring += fileSize;
            observer = transferUtility.download(bucket, s3Key, localTransferFile);

            transferId = observer.getId();
            // Set the progress listener for the transfer
            progressListeners.put(transferId, listener);

            transfersInProgress.put(transferId, observer);
            managedFilesToTransfers.put(filePath, transferId);
        }

        // Set our self to listen for progress and state changes. This should
        // not be set until the observer has been added to the progressListeners
        // data structure.
        // (This does not stop listening onPause and resume listening onResume;
        // instead it stays listening until the download succeeds or fails.)
        observer.setTransferListener(this);

        // Transfers get created in the waiting state, which we will miss since we can't set
        // our listener until we have a transfer ID to associate it with, and even if we
        // could it appear that the initial creation of the transfer causes onStateChanged
        // to be called.
        onStateChanged(transferId, observer.getState());

        return observer;
    }

    /**
     * Download a file to be placed in the local content cache upon completion.
     * @param filePath the relative path and file name of the item to download.
     * @param fileSize file size of the object. Pass 0 if the file size is unknown.
     * @param listener the progress listener.
     */
    public synchronized void download(final String filePath, final long fileSize, final ContentProgressListener listener) {
        final Integer transferId = managedFilesToTransfers.get(filePath);
        // if this item is not in the currently managed transfers.
        if (transferId == null) {
            try {
                // download the item.
                startTransfer(filePath, fileSize, listener);
            } catch (final IllegalStateException ex) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(filePath, ex);
                    }
                });
            }
            return;
        }
        // ensure the progress listener is set for this transfer
        progressListeners.put(transferId, listener);
        // Restart transfer service to deal with any in progress downloads.
        transferUtility.resume(transferId);
    }

    @Override
    public void upload(final File file, final String filePath, final ContentProgressListener listener) {
        final String key = s3DirPrefix + filePath;
        final TransferObserver observer = transferUtility.upload(bucket, key, file);
        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(final int id, final TransferState state) {
                if (state == TransferState.COMPLETED) {
                    final S3ObjectSummary summary = new S3ObjectSummary();
                    summary.setBucketName(bucket);
                    summary.setKey(key);
                    summary.setSize(file.length());
                    summary.setLastModified(new Date());
                    listener.onSuccess(new S3ContentSummary(summary, filePath));
                }
            }

            @Override
            public void onProgressChanged(final int id, final long bytesCurrent,
                                          final long bytesTotal) {
                listener.onProgressUpdate(filePath, false, bytesCurrent, bytesTotal);
            }

            @Override
            public void onError(final int id, final Exception ex) {
                listener.onError(filePath, ex);
            }
        });
    }

    /**
     * Sets the progress listener for a given s3Key being transferred.
     * @param relativeFilePath the relative path and file name.
     * @param listener the progress listener.
     */
    public synchronized void setProgressListener(final String relativeFilePath, final ContentProgressListener listener) {
        final Integer transferId = managedFilesToTransfers.get(relativeFilePath);
        if (transferId != null) {
            if (listener == null) {
                progressListeners.remove(transferId);
                return;
            }

            final TransferObserver observer = transfersInProgress.get(transferId);
            if (observer != null) {
                final ContentProgressListener currentListener = progressListeners.get(transferId);
                progressListeners.put(transferId, listener);

                if (currentListener != listener) {
                    observer.refresh();
                    final TransferState transferState = observer.getState();

                    if (transferState == TransferState.WAITING ||
                        transferState == TransferState.WAITING_FOR_NETWORK ||
                        transferState == TransferState.RESUMED_WAITING) {
                        ThreadUtils.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                onStateChanged(transferId, transferState);
                            }
                        });
                    }
                }
            }
        } else {
            Log.w(LOG_TAG, String.format("Attempt to set progress listener for file '%s'," +
                " but no transfer is in progress for that file.", relativeFilePath));
        }
    }

    /**
     * Clears all progress listeners.
     */
    public synchronized void clearProgressListeners() {
        progressListeners.clear();
    }

    public long getSizeTransferring() {
        return sizeTransferring;
    }

    public synchronized boolean isTransferring(final String relativeFilePath) {
        final Integer transferId = managedFilesToTransfers.get(relativeFilePath);
        return transferId != null;
    }

    private static final HashSet<TransferState> WAITING_FOR_CONTENT_STATES;
    static {
        WAITING_FOR_CONTENT_STATES = new HashSet<>();
        WAITING_FOR_CONTENT_STATES.add(TransferState.WAITING);
        WAITING_FOR_CONTENT_STATES.add(TransferState.RESUMED_WAITING);
        WAITING_FOR_CONTENT_STATES.add(TransferState.WAITING_FOR_NETWORK);
    }

    private synchronized TransferState getTransferState(final String relativeFilePath) {
        final Integer transferId = managedFilesToTransfers.get(relativeFilePath);
        if (transferId == null) {
            return null;
        }
        final TransferObserver observer = transfersInProgress.get(transferId);
        observer.refresh();
        return observer.getState();
    }

    public boolean isTransferWaiting(final String relativeFilePath) {
        final TransferState xferState = getTransferState(relativeFilePath);
        return WAITING_FOR_CONTENT_STATES.contains(xferState);
    }


    /**
     * Polls for all download transfers from the transfer utility and handles each appropraitely.
     * This is called on initialization to resume any currently in progress transfers and handle
     * any completed transfers.
     */
    private void pollAndCleanUpTransfers() {
        // Get all transfers being downloaded.
        final List<TransferObserver> observers =
            transferUtility.getTransfersWithType(TransferType.DOWNLOAD);
        boolean transferServiceStarted = false;

        sizeTransferring = 0;

        for (final TransferObserver observer : observers) {
            switch (observer.getState()) {
                case COMPLETED: {
                    final String absolutePath = observer.getAbsoluteFilePath();
                    final File completedFile = new File(absolutePath);
                    // Add the completed item into our cache.
                    final String relativePath = getRelativeFilePath(absolutePath);

                    if (!completedFile.exists()) {
                        Log.w(LOG_TAG, String.format("Completed file '%s' didn't exist.",
                            relativePath));
                        transferUtility.deleteTransferRecord(observer.getId());
                        break;
                    }

                    try {
                        localContentCache.addByMoving(relativePath, completedFile);
                    } catch (IOException ex) {
                        Log.e(LOG_TAG, ex.getMessage());
                    }
                    transferUtility.deleteTransferRecord(observer.getId());
                    break;
                }
                case CANCELED:
                    // Should not happen since we aren't currently supporting cancelling
                    // transfers. For now we just remove the transfer.
                    Log.w(LOG_TAG, "Removing canceled transfer.");
                    transferUtility.deleteTransferRecord(observer.getId());
                    break;
                case FAILED:
                    Log.e(LOG_TAG, "Removing failed transfer.");
                    transferUtility.deleteTransferRecord(observer.getId());
                    break;
                case PAUSED:
                    // Resume paused transfers, and register ourselves to listen to progress.
                    transferUtility.resume(observer.getId());
                    synchronized (this) {
                        transfersInProgress.put(observer.getId(), observer);
                    }
                    transferServiceStarted = true;
                    observer.setTransferListener(this);
                    break;
                case WAITING:
                case IN_PROGRESS:
                case RESUMED_WAITING:
                case WAITING_FOR_NETWORK: {
                    final int transferId = observer.getId();
                    synchronized (this) {
                        // Download is pending. We manage these transfers if not already doing so.
                        if (!transfersInProgress.containsKey(transferId)) {
                            final String absolutePath = observer.getAbsoluteFilePath();
                            // if this transfer begins with our localTransferPath.
                            if (absolutePath.startsWith(localTransferPath)) {
                                // Get the relative file path.
                                final String filePath = getRelativeFilePath(absolutePath);

                                transfersInProgress.put(transferId, observer);
                                if (managedFilesToTransfers.containsKey(filePath)) {
                                    // if we are already downloading this file, it is a duplicate transfer,
                                    // this should never happen.
                                    Log.e(LOG_TAG, String.format("Detected duplicate transfer for file '%s'",
                                        observer.getAbsoluteFilePath()));
                                    transferUtility.cancel(transferId);
                                    transferUtility.deleteTransferRecord(transferId);
                                } else {
                                    sizeTransferring += observer.getBytesTotal();
                                    managedFilesToTransfers.put(filePath, observer.getId());
                                    observer.setTransferListener(this);
                                    if (!transferServiceStarted) {
                                        // This is not required since the transfer is not paused, however
                                        // we should call it to start the transfer service running if it
                                        // is not already started.
                                        transferUtility.resume(transferId);
                                        transferServiceStarted = true;
                                    }
                                }
                            }
                        }
                    }
                    break;
                }
                case PART_COMPLETED:
                case PENDING_CANCEL:
                case PENDING_PAUSE:
                case PENDING_NETWORK_DISCONNECT:
                case UNKNOWN:
                    break;
            }
        }
    }

    /**
     * Cleans up all accounting data structures related to a transfer. Should be called from a
     * context that is synchronized on this object.
     * @param observer the transfer observer.
     */
    private synchronized void cleanUpTransfer(final TransferObserver observer) {
        final int transferId = observer.getId();
        final String relativePath = getRelativeFilePath(observer.getAbsoluteFilePath());
        observer.cleanTransferListener();
        transfersInProgress.remove(transferId);
        managedFilesToTransfers.remove(relativePath);
        transferUtility.deleteTransferRecord(transferId);
        progressListeners.remove(transferId);
    }

    @Override
    public synchronized void onStateChanged(final int id, final TransferState state) {
        final TransferObserver observer;
        observer = transfersInProgress.get(id);
        if (observer == null) {
            Log.w(LOG_TAG, String.format(
                "Transfer with id(%d) state changed to %s, but was not known to be in progress.",
                id, state.toString()));
            return;
        }
        if (state == TransferState.COMPLETED) {
            final String absolutePath = observer.getAbsoluteFilePath();
            final File completedFile = new File(absolutePath);

            // Adjust the size currently transferring to account for the completed item.
            sizeTransferring -= observer.getBytesTotal();

            final ContentProgressListener listener = progressListeners.get(id);
            // This removes the progress listener, so it is important that the listener has already
            // been obtained above to call back onSuccess or onError below.
            cleanUpTransfer(observer);

            final String relativeFilePath = getRelativeFilePath(absolutePath);
            // Add the completed item to our cache.
            final File cachedFile;
            try {
                cachedFile = localContentCache.addByMoving(relativeFilePath, completedFile);
            } catch (final IOException ex) {
                Log.d(LOG_TAG, String.format("Can't add file(%s) into the local cache.",
                    relativeFilePath), ex);
                if (listener != null) {
                    ThreadUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onError(getRelativeFilePath(completedFile.getAbsolutePath()), ex);
                        }
                    });
                }
                return;
            }

            if (listener != null) {
                listener.onSuccess(new FileContent(cachedFile, relativeFilePath));
            }
        } else if (state == TransferState.FAILED) {
            final ContentProgressListener listener = progressListeners.get(id);
            sizeTransferring -= observer.getBytesTotal();
            final String filePath = getRelativeFilePath(observer.getAbsoluteFilePath());
            final Exception exception = new RuntimeException(String.format("Transfer failed for '%s'." +
                " Perhaps this remote item no longer exists.", filePath));
            Log.d(LOG_TAG, exception.getMessage(), exception);
            localContentCache.unPinFile(filePath);
            cleanUpTransfer(observer);

            if (listener != null) {
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onError(filePath, exception);
                    }
                });
            }
        } else if (state == TransferState.WAITING || state == TransferState.WAITING_FOR_NETWORK ||
            state == TransferState.RESUMED_WAITING) {
            final ContentProgressListener listener = progressListeners.get(id);
            if (listener != null) {
                observer.refresh();
                final String filePath = getRelativeFilePath(observer.getAbsoluteFilePath());
                // Ensure this happens from the UI thread.
                ThreadUtils.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onProgressUpdate(filePath, true, observer.getBytesTransferred(),
                            observer.getBytesTotal());
                    }
                });
            }
        }
    }

    @Override
    public synchronized void onProgressChanged(final int id, final long bytesCurrent, final long bytesTotal) {
        final TransferObserver observer = transfersInProgress.get(id);
        if (observer == null) {
            // Logging at debug level since the on progress changed update frequently happens
            // after the state has already changed to complete.
            Log.d(LOG_TAG, String.format("Received progress update for id(%d), but transfer not in progress.", id));
            return;
        }
        final String filePath = getRelativeFilePath(observer.getAbsoluteFilePath());
        final ContentProgressListener listener = progressListeners.get(id);
        final long maxCacheSize = localContentCache.getMaxCacheSize();

        final boolean isPinned = localContentCache.shouldPinFile(filePath);
        if (!isPinned && bytesTotal > maxCacheSize) {
            // cancel the transfer
            transferUtility.cancel(id);
            cleanUpTransfer(observer);
        }
        if (listener != null) {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isPinned && bytesTotal > maxCacheSize) {
                        listener.onError(filePath, new IllegalStateException(String.format(
                            "Cancelled due to transfer size %s exceeds max cache size of %s",
                            StringFormatUtils.getBytesString(bytesTotal, true),
                            StringFormatUtils.getBytesString(maxCacheSize, true))));
                    } else {
                        listener.onProgressUpdate(filePath, false, bytesCurrent,
                            bytesTotal);
                    }
                }
            });
        }
    }

    @Override
    public synchronized void onError(final int id, final Exception ex) {
        final ContentProgressListener listener = progressListeners.get(id);
        final TransferObserver observer = transfersInProgress.get(id);
        Log.d(LOG_TAG, ex.getMessage(), ex);

        if (listener != null) {
            ThreadUtils.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listener.onError(getRelativeFilePath(observer.getAbsoluteFilePath()), ex);
                }
            });
        }

        // The transfer is not cleaned up here, since it is handled when
        // the state changes to failed above in onStateChanged().
    }

    private synchronized void deRegisterObservers() {
        for (final TransferObserver observer : transfersInProgress.values()) {
            observer.cleanTransferListener();
        }
    }

    @Override
    public synchronized void destroy() {
        clearProgressListeners();
        deRegisterObservers();
        transfersInProgress.clear();
        managedFilesToTransfers.clear();
    }
}