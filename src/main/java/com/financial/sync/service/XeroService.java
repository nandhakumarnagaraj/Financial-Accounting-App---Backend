package com.financial.sync.service;

import com.financial.sync.dto.SyncResponseDTO;
import com.financial.sync.dto.XeroAccountDTO;
import com.financial.sync.dto.XeroInvoiceDTO;
import com.financial.sync.dto.XeroTransactionDTO;
import com.financial.sync.entity.User;
import java.util.List;
import java.util.Map;

public interface XeroService {
	
	String getAuthorizationUrl(String state);

	Map<String, String> exchangeCodeForToken(String code);

	SyncResponseDTO syncInvoices(User user);

	SyncResponseDTO syncAccounts(User user);

	SyncResponseDTO syncTransactions(User user);

	List<XeroInvoiceDTO> getInvoices(User user);

	List<XeroAccountDTO> getAccounts(User user);
	
	List<XeroTransactionDTO> getTransactions(User user);

	void refreshAccessToken(User user);
	
	String fetchTenantId(String accessToken);

}