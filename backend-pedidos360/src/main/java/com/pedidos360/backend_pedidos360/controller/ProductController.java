package com.pedidos360.backend_pedidos360.controller;

import com.pedidos360.backend_pedidos360.model.Product;
import com.pedidos360.backend_pedidos360.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductRepository productRepository;

    // Obtener todos los productos
    @GetMapping
    public List<Product> getProducts() {
        return productRepository.findAll();
    }

    // Registrar un nuevo producto
    @PostMapping
    public Product createProduct(@RequestBody Product product) {
        return productRepository.save(product);
    }

    // Actualizar un producto
    @PutMapping("/{id}")
    public ResponseEntity<Product> updateProduct(@PathVariable Long id, @RequestBody Product productDetails) {
        return productRepository.findById(id).map(product -> {
            if (productDetails.getNombre() != null) product.setNombre(productDetails.getNombre());
            if (productDetails.getPrecio() != null) product.setPrecio(productDetails.getPrecio());
            if (productDetails.getStock() != null) product.setStock(productDetails.getStock());
            if (productDetails.getCategoria() != null) product.setCategoria(productDetails.getCategoria());
            Product updatedProduct = productRepository.save(product);
            return ResponseEntity.ok(updatedProduct);
        }).orElse(ResponseEntity.notFound().build());
    }

    // Eliminar un producto
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        if (productRepository.existsById(id)) {
            productRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}