package backend.servicecase.domain.repository;

import backend.servicecase.model.CustomerServiceTicket;

import java.util.List;
import java.util.Optional;

public interface CustomerServiceTicketRepository {

    CustomerServiceTicket save(CustomerServiceTicket customerServiceTicket);

    List<CustomerServiceTicket> findByStoreIds(List<String> storeIds);

    Optional<CustomerServiceTicket> findByTicketId(String ticketId);

    void deleteAll();
}

