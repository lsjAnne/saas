package backend.servicecase.infrastructure.persistence;

import backend.servicecase.domain.repository.CustomerServiceTicketRepository;
import backend.servicecase.model.CustomerServiceTicket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
@ConditionalOnProperty(name = "app.persistence.mode", havingValue = "memory")
public class InMemoryCustomerServiceTicketRepository implements CustomerServiceTicketRepository {

    private final Map<String, CustomerServiceTicket> storage = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(9500);

    @Override
    public CustomerServiceTicket save(CustomerServiceTicket customerServiceTicket) {
        String ticketId = customerServiceTicket.ticketId();
        if (ticketId == null || ticketId.isBlank()) {
            ticketId = "ticket-" + sequence.incrementAndGet();
        }
        CustomerServiceTicket saved = new CustomerServiceTicket(
                ticketId,
                customerServiceTicket.storeId(),
                customerServiceTicket.orderId(),
                customerServiceTicket.customerId(),
                customerServiceTicket.ticketStatus(),
                customerServiceTicket.riskFlag(),
                customerServiceTicket.aiReplySuggestion(),
                customerServiceTicket.createdAt()
        );
        storage.put(saved.ticketId(), saved);
        return saved;
    }

    @Override
    public List<CustomerServiceTicket> findByStoreIds(List<String> storeIds) {
        return storage.values().stream()
                .filter(ticket -> storeIds.contains(ticket.storeId()))
                .sorted(Comparator.comparing(CustomerServiceTicket::createdAt).reversed())
                .toList();
    }

    @Override
    public Optional<CustomerServiceTicket> findByTicketId(String ticketId) {
        return Optional.ofNullable(storage.get(ticketId));
    }

    @Override
    public void deleteAll() {
        storage.clear();
        sequence.set(9500);
    }
}

