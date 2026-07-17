package backend.product.domain.repository;

import backend.product.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepository {

    Product save(Product product);

    List<Product> findByStoreIds(List<String> storeIds);

    Optional<Product> findByProductId(String productId);

    void deleteAll();
}

