package com.finanalytics.finanalytics_platform.service;

import com.finanalytics.finanalytics_platform.dto.ProductDto;
import com.finanalytics.finanalytics_platform.entity.Product;
import com.finanalytics.finanalytics_platform.exception.ResourceNotFoundException;
import com.finanalytics.finanalytics_platform.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepo;

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "#id")
    public Product getById(Long id) {
        return productRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Product> getAll() {
        return productRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Product> search(String query, int page, int size) {
        return productRepo.search(query, PageRequest.of(page, size));
    }

    @Transactional
    @CacheEvict(value = "products", key = "#result.id")
    public Product save(ProductDto dto) {
        Product product = Product.builder()
                .name(dto.name())
                .description(dto.description())
                .price(dto.price())
                .stock(dto.stock())
                .imageUrl(dto.imageUrl())
                .category(dto.category())
                .build();
        return productRepo.save(product);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public Product update(Long id, ProductDto dto) {
        Product product = getById(id);
        product.setName(dto.name());
        product.setDescription(dto.description());
        product.setPrice(dto.price());
        product.setStock(dto.stock());
        product.setImageUrl(dto.imageUrl());
        product.setCategory(dto.category());
        return productRepo.save(product);
    }

    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void delete(Long id) {
        productRepo.deleteById(id);
    }
}