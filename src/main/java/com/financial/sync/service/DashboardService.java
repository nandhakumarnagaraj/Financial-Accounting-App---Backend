package com.financial.sync.service;

import com.financial.sync.dto.*;
import com.financial.sync.entity.*;
import com.financial.sync.repository.*;
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
	private final ZerodhaHoldingRepository holdingRepository;
	private final ZerodhaPositionRepository positionRepository;
	private final ZerodhaOrderRepository orderRepository;

	// ========== GET COMPLETE DASHBOARD STATS WITH ALL DATA ==========
	public DashboardStatsResponseDTO getCompleteDashboardStats(User user) {
		// Get Xero data
		List<XeroInvoice> invoices = invoiceRepository.findByUser(user);
		List<XeroInvoiceDTO> invoiceDTOs = invoices.stream()
				.map(this::convertToInvoiceDTO)
				.collect(Collectors.toList());

		List<XeroAccount> accounts = accountRepository.findByUser(user);
		List<XeroAccountDTO> accountDTOs = accounts.stream()
				.map(this::convertToAccountDTO)
				.collect(Collectors.toList());

		List<XeroTransaction> transactions = transactionRepository.findByUser(user);
		List<XeroTransactionDTO> transactionDTOs = transactions.stream()
				.map(this::convertToTransactionDTO)
				.collect(Collectors.toList());

		// Get Zerodha data
		List<ZerodhaHolding> holdings = holdingRepository.findByUser(user);
		List<ZerodhaHoldingDTO> holdingDTOs = holdings.stream()
				.map(this::convertToHoldingDTO)
				.collect(Collectors.toList());

		List<ZerodhaPosition> positions = positionRepository.findByUser(user);
		List<ZerodhaPositionDTO> positionDTOs = positions.stream()
				.map(this::convertToPositionDTO)
				.collect(Collectors.toList());

		List<ZerodhaOrder> orders = orderRepository.findByUser(user);
		List<ZerodhaOrderDTO> orderDTOs = orders.stream()
				.map(this::convertToOrderDTO)
				.collect(Collectors.toList());

		// Calculate Xero stats
		BigDecimal totalInvoiceAmount = invoices.stream()
				.map(inv -> inv.getTotal() != null ? inv.getTotal() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalOutstandingAmount = invoices.stream()
				.filter(inv -> "SUBMITTED".equals(inv.getStatus()) || "AUTHORISED".equals(inv.getStatus()))
				.map(inv -> inv.getAmountDue() != null ? inv.getAmountDue() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Calculate Zerodha stats
		BigDecimal portfolioValue = holdings.stream()
				.map(h -> {
					if (h.getLastPrice() != null && h.getQuantity() != null) {
						return h.getLastPrice().multiply(BigDecimal.valueOf(h.getQuantity()));
					}
					return BigDecimal.ZERO;
				})
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		BigDecimal totalPnl = holdings.stream()
				.map(h -> h.getPnl() != null ? h.getPnl() : BigDecimal.ZERO)
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		// Build response
		return DashboardStatsResponseDTO.builder()
				.username(user.getUsername())
				// Xero Stats
				.totalInvoices((long) invoices.size())
				.totalAccounts((long) accounts.size())
				.totalTransactions((long) transactions.size())
				.totalInvoiceAmount(totalInvoiceAmount)
				.totalOutstandingAmount(totalOutstandingAmount)
				.xeroConnected(user.getXeroAccessToken() != null)
				// Zerodha Stats
				.totalHoldings((long) holdings.size())
				.totalPositions((long) positions.size())
				.totalOrders((long) orders.size())
				.portfolioValue(portfolioValue)
				.totalPnl(totalPnl)
				.zerodhaConnected(user.getZerodhaAccessToken() != null)
				// Data Lists
				.invoices(invoiceDTOs)
				.accounts(accountDTOs)
				.transactions(transactionDTOs)
				.holdings(holdingDTOs)
				.positions(positionDTOs)
				.orders(orderDTOs)
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

	private ZerodhaHoldingDTO convertToHoldingDTO(ZerodhaHolding holding) {
		ZerodhaHoldingDTO dto = new ZerodhaHoldingDTO();
		dto.setId(holding.getId());
		dto.setTradingSymbol(holding.getTradingSymbol());
		dto.setExchange(holding.getExchange());
		dto.setIsin(holding.getIsin());
		dto.setQuantity(holding.getQuantity());
		dto.setAveragePrice(holding.getAveragePrice());
		dto.setLastPrice(holding.getLastPrice());
		dto.setPnl(holding.getPnl());
		dto.setProduct(holding.getProduct());
		return dto;
	}

	private ZerodhaPositionDTO convertToPositionDTO(ZerodhaPosition position) {
		ZerodhaPositionDTO dto = new ZerodhaPositionDTO();
		dto.setId(position.getId());
		dto.setTradingSymbol(position.getTradingSymbol());
		dto.setExchange(position.getExchange());
		dto.setProduct(position.getProduct());
		dto.setQuantity(position.getQuantity());
		dto.setBuyQuantity(position.getBuyQuantity());
		dto.setSellQuantity(position.getSellQuantquantity());
		dto.setAveragePrice(position.getAveragePrice());
		dto.setLastPrice(position.getLastPrice());
		dto.setPnl(position.getPnl());
		dto.setUnrealisedPnl(position.getUnrealisedPnl());
		dto.setRealisedPnl(position.getRealisedPnl());
		return dto;
	}

	private ZerodhaOrderDTO convertToOrderDTO(ZerodhaOrder order) {
		ZerodhaOrderDTO dto = new ZerodhaOrderDTO();
		dto.setId(order.getId());
		dto.setOrderId(order.getOrderId());
		dto.setTradingSymbol(order.getTradingSymbol());
		dto.setExchange(order.getExchange());
		dto.setTransactionType(order.getTransactionType());
		dto.setOrderType(order.getOrderType());
		dto.setProduct(order.getProduct());
		dto.setQuantity(order.getQuantity());
		dto.setPrice(order.getPrice());
		dto.setStatus(order.getStatus());
		dto.setOrderTimestamp(order.getOrderTimestamp());
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