package com.billingsdk.data;

import com.billingsdk.billing.BillingClientManager;
import com.billingsdk.data.mapper.ProductDetailsMapper;
import com.billingsdk.datastore.PremiumCacheStore;
import com.billingsdk.model.BillingSdkConfig;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
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
public final class BillingRepositoryImpl_Factory implements Factory<BillingRepositoryImpl> {
  private final Provider<BillingClientManager> clientProvider;

  private final Provider<PremiumCacheStore> cacheProvider;

  private final Provider<ProductDetailsMapper> mapperProvider;

  private final Provider<BillingSdkConfig> configProvider;

  private BillingRepositoryImpl_Factory(Provider<BillingClientManager> clientProvider,
      Provider<PremiumCacheStore> cacheProvider, Provider<ProductDetailsMapper> mapperProvider,
      Provider<BillingSdkConfig> configProvider) {
    this.clientProvider = clientProvider;
    this.cacheProvider = cacheProvider;
    this.mapperProvider = mapperProvider;
    this.configProvider = configProvider;
  }

  @Override
  public BillingRepositoryImpl get() {
    return newInstance(clientProvider.get(), cacheProvider.get(), mapperProvider.get(), configProvider.get());
  }

  public static BillingRepositoryImpl_Factory create(Provider<BillingClientManager> clientProvider,
      Provider<PremiumCacheStore> cacheProvider, Provider<ProductDetailsMapper> mapperProvider,
      Provider<BillingSdkConfig> configProvider) {
    return new BillingRepositoryImpl_Factory(clientProvider, cacheProvider, mapperProvider, configProvider);
  }

  public static BillingRepositoryImpl newInstance(BillingClientManager client,
      PremiumCacheStore cache, ProductDetailsMapper mapper, BillingSdkConfig config) {
    return new BillingRepositoryImpl(client, cache, mapper, config);
  }
}
