package com.financial.sync.service;

import com.financial.sync.entity.SyncLog;
import com.financial.sync.entity.User;
import com.financial.sync.repository.SyncLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SyncLogServiceImpl implements SyncLogService {

	private final SyncLogRepository syncLogRepository;

	@Override
	@Transactional
	public SyncLog startSync(User user, String syncType) {
		SyncLog syncLog = new SyncLog();
		syncLog.setUser(user);
		syncLog.setSyncType(syncType);
		syncLog.setStartTime(LocalDateTime.now());
		syncLog.setStatus("IN_PROGRESS");
		return syncLogRepository.save(syncLog);
	}

	@Override
	@Transactional
	public void completeSync(SyncLog syncLog, Integer recordsSynced, String status, String errorMessage) {
		syncLog.setEndTime(LocalDateTime.now());
		syncLog.setRecordsSynced(recordsSynced);
		syncLog.setStatus(status);
		syncLog.setErrorMessage(errorMessage);
		syncLogRepository.save(syncLog);
	}

	@Override
	public List<SyncLog> getUserSyncLogs(User user) {
		return syncLogRepository.findRecentSyncsByUser(user);
	}

	@Override
	public List<SyncLog> getUserSyncLogsBySyncType(User user, String syncType) {
		return syncLogRepository.findByUserAndSyncType(user, syncType);
	}
}