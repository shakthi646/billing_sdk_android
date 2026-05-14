package com.billingsdk.data.mapper;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class ProductDetailsMapper_Factory implements Factory<ProductDetailsMapper> {
  @Override
  public ProductDetailsMapper get() {
    return newInstance();
  }

  public static ProductDetailsMapper_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static ProductDetailsMapper newInstance() {
    return new ProductDetailsMapper();
  }

  private static final class InstanceHolder {
    static final ProductDetailsMapper_Factory INSTANCE = new ProductDetailsMapper_Factory();
  }
}
