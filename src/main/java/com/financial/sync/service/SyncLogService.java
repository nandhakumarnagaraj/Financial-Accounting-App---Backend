package com.financial.sync.service;

import com.financial.sync.entity.SyncLog;
import com.financial.sync.entity.User;
import java.util.List;

public interface SyncLogService {

	SyncLog startSync(User user, String syncType);

	void completeSync(SyncLog syncLog, Integer recordsSynced, String status, String errorMessage);

	List<SyncLog> getUserSyncLogs(User user);

	List<SyncLog> getUserSyncLogsBySyncType(User user, String syncType);
}