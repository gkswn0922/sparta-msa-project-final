package com.example.productservice.service;

import com.example.productservice.dto.ProductDto;
import com.example.productservice.entity.Product;
import com.example.productservice.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock
  ProductRepository productRepository;

  @InjectMocks
  ProductService productService;

  private Product product(int stock, Product.Status status) {
    return Product.builder()
        .id(1L)
        .name("키보드")
        .price(10000)
        .discountRate(10)
        .category("전자기기")
        .stock(stock)
        .status(status)
        .build();
  }

  @Nested
  @DisplayName("재고 차감")
  class DecreaseStock {

    @Test
    @DisplayName("재고가 충분하면 요청 수량만큼 차감된다")
    void success() {
      Product product = product(10, Product.Status.ON_SALE);
      given(productRepository.findById(1L)).willReturn(Optional.of(product));

      productService.decreaseStock(1L, 3);

      assertThat(product.getStock()).isEqualTo(7);
      assertThat(product.getStatus()).isEqualTo(Product.Status.ON_SALE);
    }

    @Test
    @DisplayName("재고가 정확히 0이 되면 품절 상태로 바뀐다")
    void soldOut() {
      Product product = product(3, Product.Status.ON_SALE);
      given(productRepository.findById(1L)).willReturn(Optional.of(product));

      productService.decreaseStock(1L, 3);

      assertThat(product.getStock()).isZero();
      assertThat(product.getStatus()).isEqualTo(Product.Status.SOLD_OUT);
    }

    @Test
    @DisplayName("재고보다 많은 수량을 요청하면 예외가 발생하고 재고는 그대로다")
    void insufficientStock() {
      Product product = product(2, Product.Status.ON_SALE);
      given(productRepository.findById(1L)).willReturn(Optional.of(product));

      assertThatThrownBy(() -> productService.decreaseStock(1L, 3))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("재고가 부족합니다");
      assertThat(product.getStock()).isEqualTo(2);
    }

    @Test
    @DisplayName("존재하지 않는 상품이면 예외가 발생한다")
    void notFound() {
      given(productRepository.findById(99L)).willReturn(Optional.empty());

      assertThatThrownBy(() -> productService.decreaseStock(99L, 1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("존재하지 않는 상품입니다");
    }
  }

  @Nested
  @DisplayName("상품 조회")
  class GetProduct {

    @Test
    @DisplayName("판매중인 상품은 할인가와 함께 조회된다")
    void success() {
      given(productRepository.findById(1L)).willReturn(Optional.of(product(5, Product.Status.ON_SALE)));

      ProductDto.Response response = productService.getProduct(1L);

      assertThat(response.getName()).isEqualTo("키보드");
      assertThat(response.getDiscountedPrice()).isEqualTo(9000);
    }

    @Test
    @DisplayName("삭제된 상품은 조회할 수 없다")
    void deleted() {
      given(productRepository.findById(1L)).willReturn(Optional.of(product(5, Product.Status.DELETED)));

      assertThatThrownBy(() -> productService.getProduct(1L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("삭제된 상품입니다");
    }

    @Test
    @DisplayName("카테고리를 지정하면 카테고리 조건으로 조회한다")
    void byCategory() {
      given(productRepository.findByCategoryAndStatusNot("전자기기", Product.Status.DELETED))
          .willReturn(List.of(product(5, Product.Status.ON_SALE)));

      List<ProductDto.Response> result = productService.getProducts("전자기기");

      assertThat(result).hasSize(1);
      verify(productRepository, never()).findByStatusNot(Product.Status.DELETED);
    }

    @Test
    @DisplayName("카테고리가 비어있으면 삭제되지 않은 전체 상품을 조회한다")
    void all() {
      given(productRepository.findByStatusNot(Product.Status.DELETED))
          .willReturn(List.of(product(5, Product.Status.ON_SALE), product(0, Product.Status.SOLD_OUT)));

      List<ProductDto.Response> result = productService.getProducts("");

      assertThat(result).hasSize(2);
    }
  }

  @Nested
  @DisplayName("상품 수정/삭제")
  class UpdateAndDelete {

    @Test
    @DisplayName("null이 아닌 필드만 수정된다")
    void partialUpdate() {
      Product product = product(5, Product.Status.ON_SALE);
      given(productRepository.findById(1L)).willReturn(Optional.of(product));
      ProductDto.UpdateRequest request = new ProductDto.UpdateRequest(null, 20000, null, null, null, null);

      productService.updateProduct(1L, request);

      assertThat(product.getPrice()).isEqualTo(20000);
      assertThat(product.getName()).isEqualTo("키보드");
      assertThat(product.getStock()).isEqualTo(5);
    }

    @Test
    @DisplayName("삭제된 상품은 수정할 수 없다")
    void updateDeleted() {
      given(productRepository.findById(1L)).willReturn(Optional.of(product(5, Product.Status.DELETED)));
      ProductDto.UpdateRequest request = new ProductDto.UpdateRequest("새이름", null, null, null, null, null);

      assertThatThrownBy(() -> productService.updateProduct(1L, request))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("삭제된 상품입니다");
    }

    @Test
    @DisplayName("삭제는 실제로 지우지 않고 DELETED 상태로 바꾼다 (soft delete)")
    void softDelete() {
      Product product = product(5, Product.Status.ON_SALE);
      given(productRepository.findById(1L)).willReturn(Optional.of(product));

      productService.deleteProduct(1L);

      assertThat(product.getStatus()).isEqualTo(Product.Status.DELETED);
      verify(productRepository, never()).delete(product);
    }
  }
}
