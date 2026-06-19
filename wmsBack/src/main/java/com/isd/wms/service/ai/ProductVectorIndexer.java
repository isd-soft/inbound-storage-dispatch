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

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductVectorIndexer {

    private final ProductRepository productRepository;
    private final VectorStore vectorStore;

    @EventListener(ApplicationReadyEvent.class)
    public void indexAllProducts() {
        log.info("Starting Product Vector Indexing...");
        List<Product> products = productRepository.findAll();

        if (products.isEmpty()) {
            log.info("No products found to index.");
            return;
        }

        List<Document> documents = products.stream()
            .map(this::createDocument)
            .toList();

        vectorStore.add(documents);

        log.info("Successfully indexed {} products into PGVector.", documents.size());
    }

    public void indexProduct(Product product) {
        log.info("Indexing new/updated product into PGVector: {}", product.getName());
        Document doc = createDocument(product);
        vectorStore.add(List.of(doc));
    }

    public void removeProduct(Long productId) {
        log.info("Removing product ID {} from PGVector...", productId);
        String documentId = generateDocumentId(productId);
        vectorStore.delete(List.of(documentId));
    }


    private Document createDocument(Product product) {
        String searchableContent = "Product Name: " + product.getName() +
            ". Description: " + (product.getDescription() != null ? product.getDescription() : "Warehouse inventory item");

        Map<String, Object> metadata = Map.of(
            "productId", product.getId(),
            "barcode", product.getBarcode(),
            "name", product.getName()
        );

        String documentId = generateDocumentId(product.getId());

        return new Document(documentId, searchableContent, metadata);
    }

    private String generateDocumentId(Long productId) {
        return UUID.nameUUIDFromBytes(("product-" + productId).toString().getBytes(StandardCharsets.UTF_8)).toString();
    }
}
