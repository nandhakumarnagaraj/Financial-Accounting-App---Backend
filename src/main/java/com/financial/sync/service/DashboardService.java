package com.financial.sync.service;

import com.financial.sync.dto.DashboardStatsResponseDTO;
import com.financial.sync.dto.XeroAccountDTO;
import com.financial.sync.dto.XeroInvoiceDTO;
import com.financial.sync.dto.XeroTransactionDTO;
import com.financial.sync.entity.User;
import com.financial.sync.entity.XeroAccount;
import com.financial.sync.entity.XeroInvoice;
import com.financial.sync.entity.XeroTransaction;
import com.financial.sync.repository.XeroAccountRepository;
import com.financial.sync.repository.XeroInvoiceRepository;
import com.financial.sync.repository.XeroTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

	private final XeroInvoiceRepository invoiceRepository;
	private final XeroAccountRepository accountRepository;
	private final XeroTransactionRepository transactionRepository;

	// ========== GET COMPLETE DASHBOARD STATS WITH ALL DATA ==========
	public DashboardStatsResponseDTO getCompleteDashboardStats(User user) {
		// Get invoices
		List<XeroInvoice> invoices = invoiceRepository.findByUser(user);
		List<XeroInvoiceDTO> invoiceDTOs = invoices.stream()
				.map(this::convertToInvoiceDTO)
				.collect(Collectors.toList());

		// Get accounts
		List<XeroAccount> accounts = accountRepository.findByUser(user);
		List<XeroAccountDTO> accountDTOs = accounts.stream()
				.map(this::convertToAccountDTO)
				.collect(Collectors.toList());

		// Get transactions
		List<XeroTransaction> transactions = transactionRepository.findByUser(user);
		List<XeroTransactionDTO> transactionDTOs = transactions.stream()
				.map(this::convertToTransactionDTO)
				.collect(Collectors.toList());

		// Calculate stats
		BigDecimal totalInvoiceAmount = invoices.stream()
				.map(inv -> inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalOutstandingAmount = invoices.stream()
				.filter(inv -> "SUBMITTED".equals(inv.getStatus()))
				.map(inv -> inv.getAmountDue() != null ? inv.getAmountDue() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Build response
		return DashboardStatsResponseDTO.builder()
				.totalInvoices((long) invoices.size())
				.totalAccounts((long) accounts.size())
				.totalTransactions((long) transactions.size())
				.totalInvoiceAmount(totalInvoiceAmount)
				.totalOutstandingAmount(totalOutstandingAmount)
				.xeroConnected(user.getXeroAccessToken() != null)
				.username(user.getUsername())
				.invoices(invoiceDTOs)
				.accounts(accountDTOs)
				.transactions(transactionDTOs)
				.build();
	}

	// ========== CONVERSION HELPER METHODS ==========

	private XeroInvoiceDTO convertToInvoiceDTO(XeroInvoice invoice) {
		XeroInvoiceDTO dto = new XeroInvoiceDTO();
		dto.setId(invoice.getId());
		dto.setInvoiceNumber(invoice.getInvoiceNumber());
		dto.setContactName(invoice.getContactName());
		dto.setInvoiceDate(invoice.getInvoiceDate());
		dto.setDueDate(invoice.getDueDate());
		dto.setStatus(invoice.getStatus());
		dto.setTotal(invoice.getTotal());
		dto.setAmountDue(invoice.getAmountDue());
		return dto;
	}

	private XeroAccountDTO convertToAccountDTO(XeroAccount account) {
		XeroAccountDTO dto = new XeroAccountDTO();
		dto.setId(account.getId());
		dto.setAccountCode(account.getAccountCode());
		dto.setAccountName(account.getAccountName());
		dto.setAccountType(account.getAccountType());
		dto.setStatus(account.getStatus());
		return dto;
	}

	private XeroTransactionDTO convertToTransactionDTO(XeroTransaction transaction) {
		XeroTransactionDTO dto = new XeroTransactionDTO();
		dto.setId(transaction.getId());
		dto.setTransactionType(transaction.getTransactionType());
		dto.setContactName(transaction.getContactName());
		dto.setTransactionDate(transaction.getTransactionDate());
		dto.setAmount(transaction.getAmount());
		dto.setAccountCode(transaction.getAccountCode());
		dto.setAccountName(transaction.getAccountName());
		dto.setDescription(transaction.getDescription());
		dto.setReference(transaction.getReference());
		dto.setStatus(transaction.getStatus());
		return dto;
	}

	// ========== ADDITIONAL STATS METHODS ==========

	public BigDecimal getMonthlyRevenueTotal(User user, YearMonth month) {
		LocalDate startDate = month.atDay(1);
		LocalDate endDate = month.atEndOfMonth();

		return invoiceRepository.findByUserAndDateRange(user, startDate, endDate).stream()
				.map(inv -> inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	public BigDecimal getTotalExpenses(User user) {
		List<XeroTransaction> transactions = transactionRepository.findByUser(user);
		return transactions.stream()
				.map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}