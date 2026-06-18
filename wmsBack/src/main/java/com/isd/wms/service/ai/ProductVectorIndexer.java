package com.isd.wms.service.ai;

import com.isd.wms.entity.Product;
import com.isd.wms.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVectorIndexer {

    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    @EventListener(ApplicationReadyEvent.class)
    public void indexProducts() {
        log.info("Starting Product Vector Indexing...");
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            log.info("No products found to index.");
            return;
        }

        List<Document> documents = products.stream().map(product -> {
            String searchableContent = "Product Name: " + product.getName() +
                ". Description: " + (product.getDescription() != null ? product.getDescription() : "Warehouse inventory item");

            Map<String, Object> metadata = Map.of(
                "productId", product.getId(),
                "barcode", product.getBarcode(),
                "name", product.getName()
            );

            return new Document(searchableContent, metadata);
        }).toList();

        vectorStore.add(documents);

        log.info("Successfully indexed {} products into PGVector.", documents.size());
    }
}
